// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.gui.ShowHideButtonListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;
import org.openstreetmap.josm.tools.WindowGeometry.WindowGeometryException;

/**
 * This class is a toggle dialog that can be turned on and off.
 *
 */
public class ToggleDialog extends JPanel implements ShowHideButtonListener, Helpful, AWTEventListener {

    /** The action to toggle this dialog */
    protected ToggleDialogAction toggleAction;
    protected String preferencePrefix;
    final protected String name;

    /** DialogsPanel that manages all ToggleDialogs */
    protected DialogsPanel dialogsPanel;

    protected TitleBar titleBar;

    /**
     * Indicates whether the dialog is showing or not.
     */
    protected boolean isShowing;
    /**
     * If isShowing is true, indicates whether the dialog is docked or not, e. g.
     * shown as part of the main window or as a separate dialog window.
     */
    protected boolean isDocked;
    /**
     * If isShowing and isDocked are true, indicates whether the dialog is
     * currently minimized or not.
     */
    protected boolean isCollapsed;
    /**
     * Indicates whether dynamic button hiding is active or not.
     */
    protected boolean isButtonHiding;

    /** the preferred height if the toggle dialog is expanded */
    private int preferredHeight;

    /** the label in the title bar which shows whether the toggle dialog is expanded or collapsed */
    private JLabel lblMinimized;

    /** the label in the title bar which shows whether buttons are dynamic or not */
    private JButton buttonsHide = null;

    /** the JDialog displaying the toggle dialog as undocked dialog */
    protected JDialog detachedDialog;

    protected JToggleButton button;
    private JPanel buttonsPanel;

    /** holds the menu entry in the windows menu. Required to properly
     * toggle the checkbox on show/hide
     */
    protected JCheckBoxMenuItem windowMenuItem;

    /**
     * Constructor
     * (see below)
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight) {
        this(name, iconName, tooltip, shortcut, preferredHeight, false);
    }
    /**
     * Constructor
     *
     * @param name  the name of the dialog
     * @param iconName the name of the icon to be displayed
     * @param tooltip  the tool tip
     * @param shortcut  the shortcut
     * @param preferredHeight the preferred height for the dialog
     * @param defShow if the dialog should be shown by default, if there is no preference
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight, boolean defShow) {
        super(new BorderLayout());
        this.preferencePrefix = iconName;
        this.name = name;

        /** Use the full width of the parent element */
        setPreferredSize(new Dimension(0, preferredHeight));
        /** Override any minimum sizes of child elements so the user can resize freely */
        setMinimumSize(new Dimension(0,0));
        this.preferredHeight = preferredHeight;
        toggleAction = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortcut, iconName);
        String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        toggleAction.putValue("help", helpId.substring(0, helpId.length()-6));

        isShowing = Main.pref.getBoolean(preferencePrefix+".visible", defShow);
        isDocked = Main.pref.getBoolean(preferencePrefix+".docked", true);
        isCollapsed = Main.pref.getBoolean(preferencePrefix+".minimized", false);
        isButtonHiding = Main.pref.getBoolean(preferencePrefix+".buttonhiding", true);

        /** show the minimize button */
        titleBar = new TitleBar(name, iconName);
        add(titleBar, BorderLayout.NORTH);

        setBorder(BorderFactory.createEtchedBorder());

        Main.redirectToMainContentPane(this);

        windowMenuItem = MainMenu.addWithCheckbox(Main.main.menu.windowMenu,
                (JosmAction) getToggleAction(),
                MainMenu.WINDOW_MENU_GROUP.TOGGLE_DIALOG);
    }

    /**
     * The action to toggle the visibility state of this toggle dialog.
     *
     * Emits {@see PropertyChangeEvent}s for the property <tt>selected</tt>:
     * <ul>
     *   <li>true, if the dialog is currently visible</li>
     *   <li>false, if the dialog is currently invisible</li>
     * </ul>
     *
     */
    public final class ToggleDialogAction extends JosmAction {

        private ToggleDialogAction(String name, String iconName, String tooltip, Shortcut shortcut, String prefname) {
            super(name, iconName, tooltip, shortcut, false);
        }

        public void actionPerformed(ActionEvent e) {
            toggleButtonHook();
            if(getValue("toolbarbutton") != null && getValue("toolbarbutton") instanceof JButton) {
                ((JButton) getValue("toolbarbutton")).setSelected(!isShowing);
            }
            if (isShowing) {
                hideDialog();
                dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                hideNotify();
            } else {
                showDialog();
                if (isDocked && isCollapsed) {
                    expand();
                }
                if (isDocked) {
                    dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                }
                showNotify();
            }
        }

        @Override
        public void destroy() {
            super.destroy();
        }
    }

    /**
     * Shows the dialog
     */
    public void showDialog() {
        setIsShowing(true);
        if (!isDocked) {
            detach();
        } else {
            dock();
            this.setVisible(true);
        }
        // toggling the selected value in order to enforce PropertyChangeEvents
        setIsShowing(true);
        windowMenuItem.setState(true);
        toggleAction.putValue("selected", false);
        toggleAction.putValue("selected", true);
    }

    /**
     * Changes the state of the dialog such that the user can see the content.
     * (takes care of the panel reconstruction)
     */
    public void unfurlDialog() {
        if (isDialogInDefaultView())
            return;
        if (isDialogInCollapsedView()) {
            expand();
            dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, this);
        } else if (!isDialogShowing()) {
            showDialog();
            if (isDocked && isCollapsed) {
                expand();
            }
            if (isDocked) {
                dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, this);
            }
            showNotify();
        }
    }

    @Override
    public void buttonHidden() {
        if ((Boolean) toggleAction.getValue("selected")) {
            toggleAction.actionPerformed(null);
        }
    }

    public void buttonShown() {
        unfurlDialog();
    }


    /**
     * Hides the dialog
     */
    public void hideDialog() {
        closeDetachedDialog();
        this.setVisible(false);
        windowMenuItem.setState(false);
        setIsShowing(false);
        toggleAction.putValue("selected", false);
    }

    /**
     * Displays the toggle dialog in the toggle dialog view on the right
     * of the main map window.
     *
     */
    protected void dock() {
        detachedDialog = null;
        titleBar.setVisible(true);
        setIsDocked(true);
    }

    /**
     * Display the dialog in a detached window.
     *
     */
    protected void detach() {
        setContentVisible(true);
        this.setVisible(true);
        titleBar.setVisible(false);
        detachedDialog = new DetachedDialog();
        detachedDialog.setVisible(true);
        setIsShowing(true);
        setIsDocked(false);
    }

    /**
     * Collapses the toggle dialog to the title bar only
     *
     */
    public void collapse() {
        if (isDialogInDefaultView()) {
            setContentVisible(false);
            setIsCollapsed(true);
            setPreferredSize(new Dimension(0,20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
            setMinimumSize(new Dimension(Integer.MAX_VALUE,20));
            lblMinimized.setIcon(ImageProvider.get("misc", "minimized"));
        }
        else throw new IllegalStateException();
    }

    /**
     * Expands the toggle dialog
     */
    protected void expand() {
        if (isDialogInCollapsedView()) {
            setContentVisible(true);
            setIsCollapsed(false);
            setPreferredSize(new Dimension(0,preferredHeight));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            lblMinimized.setIcon(ImageProvider.get("misc", "normal"));
        }
        else throw new IllegalStateException();
    }

    /**
     * Sets the visibility of all components in this toggle dialog, except the title bar
     *
     * @param visible true, if the components should be visible; false otherwise
     */
    protected void setContentVisible(boolean visible) {
        Component comps[] = getComponents();
        for(int i=0; i<comps.length; i++) {
            if(comps[i] != titleBar) {
                comps[i].setVisible(visible);
            }
        }
    }

    public void destroy() {
        closeDetachedDialog();
        hideNotify();
        Main.main.menu.windowMenu.remove(windowMenuItem);
    }

    /**
     * Closes the detached dialog if this toggle dialog is currently displayed
     * in a detached dialog.
     *
     */
    public void closeDetachedDialog() {
        if (detachedDialog != null) {
            detachedDialog.setVisible(false);
            detachedDialog.getContentPane().removeAll();
            detachedDialog.dispose();
        }
    }

    /**
     * Called when toggle dialog is shown (after it was created or expanded). Descendants may overwrite this
     * method, it's a good place to register listeners needed to keep dialog updated
     */
    public void showNotify() {

    }

    /**
     * Called when toggle dialog is hidden (collapsed, removed, MapFrame is removed, ...). Good place to unregister
     * listeners
     */
    public void hideNotify() {

    }

    /**
     * The title bar displayed in docked mode
     *
     */
    protected class TitleBar extends JPanel {
        final private JLabel lblTitle;
        final private JComponent lblTitle_weak;

        public TitleBar(String toggleDialogName, String iconName) {
            setLayout(new GridBagLayout());

            lblMinimized = new JLabel(ImageProvider.get("misc", "normal"));
            add(lblMinimized);

            // scale down the dialog icon
            ImageIcon inIcon = ImageProvider.get("dialogs", iconName);
            ImageIcon smallIcon = new ImageIcon(inIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
            lblTitle = new JLabel("",smallIcon, JLabel.TRAILING);
            lblTitle.setIconTextGap(8);

            JPanel conceal = new JPanel();
            conceal.add(lblTitle);
            conceal.setVisible(false);
            add(conceal, GBC.std());

            // Cannot add the label directly since it would displace other elements on resize
            lblTitle_weak = new JComponent() {
                @Override
                public void paintComponent(Graphics g) {
                    lblTitle.paint(g);
                }
            };
            lblTitle_weak.setPreferredSize(new Dimension(Integer.MAX_VALUE,20));
            lblTitle_weak.setMinimumSize(new Dimension(0,20));
            add(lblTitle_weak, GBC.std().fill(GBC.HORIZONTAL));

            addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (isCollapsed) {
                                expand();
                                dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, ToggleDialog.this);
                            } else {
                                collapse();
                                dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                            }
                        }
                    }
            );

            if(Main.pref.getBoolean("dialog.dynamic.buttons", true)) {
                buttonsHide = new JButton(ImageProvider.get("misc", isButtonHiding ? "buttonhide" : "buttonshow"));
                buttonsHide.setToolTipText(tr("Toggle dynamic buttons"));
                buttonsHide.setBorder(BorderFactory.createEmptyBorder());
                buttonsHide.addActionListener(
                    new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            setIsButtonHiding(!isButtonHiding);
                        }
                    }
                );
                add(buttonsHide);
            }

            // show the sticky button
            JButton sticky = new JButton(ImageProvider.get("misc", "sticky"));
            sticky.setToolTipText(tr("Undock the panel"));
            sticky.setBorder(BorderFactory.createEmptyBorder());
            sticky.addActionListener(
                    new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            detach();
                            dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                        }
                    }
            );
            add(sticky);

            // show the close button
            JButton close = new JButton(ImageProvider.get("misc", "close"));
            close.setToolTipText(tr("Close this panel. You can reopen it with the buttons in the left toolbar."));
            close.setBorder(BorderFactory.createEmptyBorder());
            close.addActionListener(
                    new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            hideDialog();
                            dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                            hideNotify();
                        }
                    }
            );
            add(close);
            setToolTipText(tr("Click to minimize/maximize the panel content"));
            setTitle(toggleDialogName);
        }

        public void setTitle(String title) {
            lblTitle.setText(title);
            lblTitle_weak.repaint();
        }

        public String getTitle() {
            return lblTitle.getText();
        }
    }

    /**
     * The dialog class used to display toggle dialogs in a detached window.
     *
     */
    private class DetachedDialog extends JDialog{
        public DetachedDialog() {
            super(JOptionPane.getFrameForComponent(Main.parent));
            getContentPane().add(ToggleDialog.this);
            addWindowListener(new WindowAdapter(){
                @Override public void windowClosing(WindowEvent e) {
                    rememberGeometry();
                    getContentPane().removeAll();
                    dispose();
                    if (dockWhenClosingDetachedDlg()) {
                        dock();
                        if (isDialogInCollapsedView()) {
                            expand();
                        }
                        dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                    } else {
                        hideDialog();
                        hideNotify();
                    }
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override public void componentMoved(ComponentEvent e) {
                    rememberGeometry();
                }
                @Override public void componentResized(ComponentEvent e) {
                    rememberGeometry();
                }
            });

            try {
                new WindowGeometry(preferencePrefix+".geometry").applySafe(this);
            } catch (WindowGeometryException e) {
                ToggleDialog.this.setPreferredSize(ToggleDialog.this.getDefaultDetachedSize());
                pack();
                setLocationRelativeTo(Main.parent);
            }
            setTitle(titleBar.getTitle());
            HelpUtil.setHelpContext(getRootPane(), helpTopic());
        }

        protected void rememberGeometry() {
            if (detachedDialog != null) {
                new WindowGeometry(detachedDialog).remember(preferencePrefix+".geometry");
            }
        }
    }

    /**
     * Replies the action to toggle the visible state of this toggle dialog
     *
     * @return the action to toggle the visible state of this toggle dialog
     */
    public AbstractAction getToggleAction() {
        return toggleAction;
    }

    /**
     * Replies the prefix for the preference settings of this dialog.
     *
     * @return the prefix for the preference settings of this dialog.
     */
    public String getPreferencePrefix() {
        return preferencePrefix;
    }

    /**
     * Sets the dialogsPanel managing all toggle dialogs
     */
    public void setDialogsPanel(DialogsPanel dialogsPanel) {
        this.dialogsPanel = dialogsPanel;
    }

    /**
     * Replies the name of this toggle dialog
     */
    @Override
    public String getName() {
        return "toggleDialog." + preferencePrefix;
    }

    /**
     * Sets the title
     */
    public void setTitle(String title) {
        titleBar.setTitle(title);
        if (detachedDialog != null) {
            detachedDialog.setTitle(title);
        }
    }

    protected void setIsShowing(boolean val) {
        isShowing = val;
        Main.pref.put(preferencePrefix+".visible", val);
        stateChanged();
    }

    protected void setIsDocked(boolean val) {
        if(buttonsPanel != null && buttonsHide != null) {
            buttonsPanel.setVisible(val ? !isButtonHiding : true);
        }
        isDocked = val;
        Main.pref.put(preferencePrefix+".docked", val);
        stateChanged();
    }

    protected void setIsCollapsed(boolean val) {
        isCollapsed = val;
        Main.pref.put(preferencePrefix+".minimized", val);
        stateChanged();
    }

    protected void setIsButtonHiding(boolean val) {
        isButtonHiding = val;
        Main.pref.put(preferencePrefix+".buttonhiding", val);
        buttonsHide.setIcon(ImageProvider.get("misc", val ? "buttonhide" : "buttonshow"));
        stateChanged();
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public String helpTopic() {
        String help = getClass().getName();
        help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
        return "Dialog/"+help;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Replies true if this dialog is showing either as docked or as detached dialog
     */
    public boolean isDialogShowing() {
        return isShowing;
    }

    /**
     * Replies true if this dialog is docked and expanded
     */
    public boolean isDialogInDefaultView() {
        return isShowing && isDocked && (! isCollapsed);
    }

    /**
     * Replies true if this dialog is docked and collapsed
     */
    public boolean isDialogInCollapsedView() {
        return isShowing && isDocked && isCollapsed;
    }

    public void setButton(JToggleButton button) {
        this.button = button;
    }

    public JToggleButton getButton() {
        return button;
    }
    
    /***
     * The following methods are intended to be overridden, in order to customize
     * the toggle dialog behavior.
     **/

    /**
     * Change the Geometry of the detached dialog to better fit the content.
     */
    protected Rectangle getDetachedGeometry(Rectangle last) {
        return last;
    }

    /**
     * Default size of the detached dialog.
     * Override this method to customize the initial dialog size.
     */
    protected Dimension getDefaultDetachedSize() {
        return new Dimension(dialogsPanel.getWidth(), preferredHeight);
    }

    /**
     * Do something when the toggleButton is pressed.
     */
    protected void toggleButtonHook() {
    }

    protected boolean dockWhenClosingDetachedDlg() {
        return true;
    }

    /**
     * primitive stateChangedListener for subclasses
     */
    protected void stateChanged() {
    }

    protected Component createLayout(Component data, boolean scroll, Collection<SideButton> buttons) {
        if(scroll)
            data = new JScrollPane(data);
        add(data, BorderLayout.CENTER);
        if(buttons != null && buttons.size() != 0) {
            buttonsPanel = new JPanel(Main.pref.getBoolean("dialog.align.left", false)
                ? new FlowLayout(FlowLayout.LEFT) : new GridLayout(1,buttons.size()));
            for(SideButton button : buttons)
                buttonsPanel.add(button);
            add(buttonsPanel, BorderLayout.SOUTH);
            if(Main.pref.getBoolean("dialog.dynamic.buttons", true)) {
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_MOTION_EVENT_MASK);
                buttonsPanel.setVisible(!isButtonHiding || !isDocked);
            }
        } else if(buttonsHide != null) {
            buttonsHide.setVisible(false);
        }
        return data;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if(isShowing() && !isCollapsed && isDocked && isButtonHiding) {
            Rectangle b = this.getBounds();
            b.setLocation(getLocationOnScreen());
            if (b.contains(((MouseEvent)event).getLocationOnScreen())) {
                if(!buttonsPanel.isVisible()) {
                    buttonsPanel.setVisible(true);
                }
            } else if (buttonsPanel.isVisible()) {
                buttonsPanel.setVisible(false);
            }
        }
    }
}
