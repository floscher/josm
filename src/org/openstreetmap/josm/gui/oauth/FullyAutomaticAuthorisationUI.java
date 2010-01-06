// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.CredentialsManagerException;
import org.openstreetmap.josm.io.auth.CredentialsManagerFactory;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * This is an UI which supports a JOSM user to get an OAuth Access Token in a fully
 * automatic process.
 * 
 */
public class FullyAutomaticAuthorisationUI extends AbstractAuthorisationUI {

    private JTextField tfUserName;
    private JPasswordField tfPassword;
    private UserNameValidator valUserName;
    private PasswordValidator valPassword;
    private AccessTokenInfoPanel pnlAccessTokenInfo;
    private OsmPrivilegesPanel pnlOsmPrivileges;
    private JPanel pnlPropertiesPanel;
    private JPanel pnlActionButtonsPanel;
    private JPanel pnlResult;

    /**
     * Builds the panel with the three privileges the user can grant JOSM
     * 
     * @return
     */
    protected VerticallyScrollablePanel buildGrantsPanel() {
        pnlOsmPrivileges = new OsmPrivilegesPanel();
        return pnlOsmPrivileges;
    }

    /**
     * Builds the panel for entering the username and password
     * 
     * @return
     */
    protected VerticallyScrollablePanel buildUserNamePasswordPanel() {
        VerticallyScrollablePanel pnl = new VerticallyScrollablePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        HtmlPanel pnlMessage = new HtmlPanel();
        HTMLEditorKit kit = (HTMLEditorKit)pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(".warning-body {background-color:rgb(253,255,221);padding: 10pt; border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        kit.getStyleSheet().addRule("ol {margin-left: 1cm}");
        pnlMessage.setText("<html><body>"
                + "Please enter your OSM user name and password. The password will <strong>not</strong> be saved "
                + "in clear text in the JOSM preferences and it will be submitted to the OSM server <strong>only once</strong>. "
                + "Subsequent data upload requests don't use your password any more. "
                + "</p>"
                + "</body></html>"
        );
        pnl.add(pnlMessage, gc);

        // the user name input field
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,3,3);
        pnl.add(new JLabel(tr("Username: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfUserName = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfUserName);
        valUserName = new UserNameValidator(tfUserName);
        valUserName.validate();

        // the password input field
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Password: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfPassword = new JPasswordField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfPassword);
        valPassword = new PasswordValidator(tfPassword);
        valPassword.validate();

        gc.gridy = 3;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        pnlMessage = new HtmlPanel();
        kit = (HTMLEditorKit)pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(".warning-body {background-color:rgb(253,255,221);padding: 10pt; border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        kit.getStyleSheet().addRule("ol {margin-left: 1cm}");
        pnlMessage.setText("<html><body>"
                + "<p class=\"warning-body\">"
                + "<strong>Warning:</strong> The password is transferred <strong>once</strong> in clear text "
                + "to the OSM website. <strong>Do not</strong> use a sensitive "
                + "password until the OSM server provides an encrypted communication channel (HTTPS)."
                + "</p>"
                + "</body></html>"
        );
        pnl.add(pnlMessage, gc);

        // filler - grab remaining space
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected JPanel buildPropertiesPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        JTabbedPane tpProperties = new JTabbedPane();
        tpProperties.add(VerticallyScrollablePanel.embed(buildUserNamePasswordPanel()));
        tpProperties.add(VerticallyScrollablePanel.embed(buildGrantsPanel()));
        tpProperties.add(VerticallyScrollablePanel.embed(getAdvancedPropertiesPanel()));
        tpProperties.setTitleAt(0, tr("Basic"));
        tpProperties.setTitleAt(1, tr("Granted rights"));
        tpProperties.setTitleAt(2, tr("Advanced OAuth properties"));

        pnl.add(tpProperties, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Initializes the panel with values from the preferences
     */
    @Override
    public void initFromPreferences(Preferences pref) {
        super.initFromPreferences(pref);
        CredentialsManager cm = CredentialsManagerFactory.getCredentialManager();
        try {
            PasswordAuthentication pa = cm.lookup(RequestorType.SERVER);
            if (pa == null) {
                tfUserName.setText("");
                tfPassword.setText("");
            } else {
                tfUserName.setText(pa.getUserName() == null ? "" : pa.getUserName());
                tfPassword.setText(pa.getPassword() == null ? "" : String.valueOf(pa.getPassword()));
            }
        } catch(CredentialsManagerException e) {
            e.printStackTrace();
            tfUserName.setText("");
            tfPassword.setText("");
        }
    }

    /**
     * Builds the panel with the action button  for starting the authorisation
     * 
     * @return
     */
    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        RunAuthorisationAction runAuthorisationAction= new RunAuthorisationAction();
        tfPassword.getDocument().addDocumentListener(runAuthorisationAction);
        tfUserName.getDocument().addDocumentListener(runAuthorisationAction);
        pnl.add(new SideButton(runAuthorisationAction));
        return pnl;
    }

    /**
     * Builds the panel which displays the generated Access Token.
     * 
     * @return
     */
    protected JPanel buildResultsPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());;
        GridBagConstraints gc = new GridBagConstraints();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // the message panel
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        JMultilineLabel msg = new JMultilineLabel("");
        msg.setFont(msg.getFont().deriveFont(Font.PLAIN));
        String lbl = tr("Accept Access Token");
        msg.setText(tr("<html>"
                + "You''ve sucessfully retrieved an OAuth Access Token from the OSM website. "
                + "Click on <strong>{0}</strong> to accept the token. JOSM will use it in "
                + "subsequent requests to gain access to the OSM API."
                + "</html",
                lbl
        ));
        pnl.add(msg, gc);

        // infos about the access token
        gc.gridy = 1;
        gc.insets = new Insets(5,0,0,0);
        pnl.add(pnlAccessTokenInfo = new AccessTokenInfoPanel(), gc);

        // the actions
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl1.add(new SideButton(new BackAction()));
        pnl1.add(new SideButton(new TestAccessTokenAction()));
        gc.gridy = 2;
        pnl.add(pnl1, gc);

        // filler - grab the remaining space
        gc.gridy = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        pnlPropertiesPanel = buildPropertiesPanel();
        pnlActionButtonsPanel = buildActionButtonPanel();
        pnlResult = buildResultsPanel();

        prepareUIForEnteringRequest();
    }

    /**
     * Prepares the UI for the first step in the automatic process: entering the authentication
     * and authorisation parameters.
     * 
     */
    protected void prepareUIForEnteringRequest() {
        removeAll();
        add(pnlPropertiesPanel, BorderLayout.CENTER);
        add(pnlActionButtonsPanel, BorderLayout.SOUTH);
        pnlPropertiesPanel.revalidate();
        pnlActionButtonsPanel.revalidate();
        validate();
        repaint();

        setAccessToken(null);
    }

    /**
     * Prepares the UI for the second step in the automatic process: displaying the access token
     * 
     */
    protected void prepareUIForResultDisplay() {
        removeAll();
        add(pnlResult, BorderLayout.CENTER);
        validate();
        repaint();
    }

    protected String getOsmUserName() {
        return tfUserName.getText();
    }

    protected String getOsmPassword() {
        return String.valueOf(tfPassword.getPassword());
    }

    public FullyAutomaticAuthorisationUI() {
        build();
    }

    @Override
    public boolean isSaveAccessTokenToPreferences() {
        return pnlAccessTokenInfo.isSaveToPreferences();
    }

    @Override
    protected void setAccessToken(OAuthToken accessToken) {
        super.setAccessToken(accessToken);
        pnlAccessTokenInfo.setAccessToken(accessToken);
    }

    /**
     * Starts the authorisation process
     */
    class RunAuthorisationAction extends AbstractAction implements DocumentListener{
        public RunAuthorisationAction() {
            putValue(NAME, tr("Authorise now"));
            putValue(SMALL_ICON, ImageProvider.get("oauth", "oauth"));
            putValue(SHORT_DESCRIPTION, tr("Click to redirect you to the authorisation form on the JOSM web site"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent evt) {
            Main.worker.submit(new FullyAutomaticAuthorisationTask(FullyAutomaticAuthorisationUI.this));
        }

        protected void updateEnabledState() {
            setEnabled(valPassword.isValid() && valUserName.isValid());
        }

        public void changedUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        public void insertUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        public void removeUpdate(DocumentEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Action to go back to step 1 in the process
     */
    class BackAction extends AbstractAction {
        public BackAction() {
            putValue(NAME, tr("Back"));
            putValue(SHORT_DESCRIPTION, tr("Run the automatic authorisation steps again"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "previous"));
        }

        public void actionPerformed(ActionEvent arg0) {
            prepareUIForEnteringRequest();
        }
    }

    /**
     * Action to test an access token.
     */
    class TestAccessTokenAction extends AbstractAction {
        public TestAccessTokenAction() {
            putValue(NAME, tr("Test Access Token"));
            putValue(SHORT_DESCRIPTION, tr(""));
            putValue(SMALL_ICON, ImageProvider.get("about"));
        }

        public void actionPerformed(ActionEvent arg0) {
            Main.worker.submit(new TestAccessTokenTask(
                    FullyAutomaticAuthorisationUI.this,
                    getApiUrl(),
                    getAdvancedPropertiesPanel().getAdvancedParameters(),
                    getAccessToken()
            ));
        }
    }


    static private class UserNameValidator extends AbstractTextComponentValidator {
        public UserNameValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getComponent().getText().trim().length() > 0;
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter your OSM user name"));
            } else {
                feedbackInvalid(tr("The user name can't be empty. Please enter your OSM user name"));
            }
        }
    }

    static private class PasswordValidator extends AbstractTextComponentValidator {

        public PasswordValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getComponent().getText().trim().length() > 0;
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter your OSM password"));
            } else {
                feedbackInvalid(tr("The password can't be empty. Please enter your OSM password"));
            }
        }
    }

    class FullyAutomaticAuthorisationTask extends PleaseWaitRunnable {
        private boolean canceled;
        private OsmOAuthAuthorisationClient authClient;

        public FullyAutomaticAuthorisationTask(Component parent) {
            super(parent, tr("Authorise JOSM to access the OSM API"), false /* don't ignore exceptions */);
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void finish() {}

        protected void alertAutorisationFailed(OsmOAuthAuthorisationException e) {
            HelpAwareOptionPane.showOptionDialog(
                    FullyAutomaticAuthorisationUI.this,
                    tr("<html>"
                            + "The automatic process for retrieving an OAuth Access Token<br>"
                            + "from the OSM server failed.<br><br>"
                            + "Please try again or choose another kind of authorisation process,<br>"
                            + "i.e. semi-automatic or manual authorisation."
                            +"</html>"
                    ),
                    tr("OAuth authorisation failed"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/OAuthAutorisationWizard#FullyAutomaticProcessFailed")
            );
        }

        protected void alertInvalidLoginUrl() {
            HelpAwareOptionPane.showOptionDialog(
                    FullyAutomaticAuthorisationUI.this,
                    tr("<html>"
                            + "The automatic process for retrieving an OAuth Access Token<br>"
                            + "from the OSM server failed because JOSM wasn't able to build<br>"
                            + "a valid login URL from the OAuth Autorise Endpoint URL ''{0}''.<br><br>"
                            + "Please check your advanced setting and try again."
                            +"</html>",
                            getAdvancedPropertiesPanel().getAdvancedParameters().getAuthoriseUrl()
                    ),
                    tr("OAuth authorisation failed"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/OAuthAutorisationWizard#FullyAutomaticProcessFailed")
            );
        }

        protected void alertLoginFailed(OsmLoginFailedException e) {
            String loginUrl = null;
            try {
                loginUrl = authClient.buildOsmLoginUrl();
            } catch(OsmOAuthAuthorisationException e1) {
                alertInvalidLoginUrl();
                return;
            }
            HelpAwareOptionPane.showOptionDialog(
                    FullyAutomaticAuthorisationUI.this,
                    tr("<html>"
                            + "The automatic process for retrieving an OAuth Access Token<br>"
                            + "from the OSM server failed. JOSM failed to log into {0}<br>"
                            + "for user {1}.<br><br>"
                            + "Please check username and password and try again."
                            +"</html>",
                            loginUrl,
                            getOsmUserName()
                    ),
                    tr("OAuth authorisation failed"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/OAuthAutorisationWizard#FullyAutomaticProcessFailed")
            );
        }

        protected void handleException(final OsmOAuthAuthorisationException e) {
            Runnable r = new Runnable() {
                public void run() {
                    if (e instanceof OsmLoginFailedException) {
                        alertLoginFailed((OsmLoginFailedException)e);
                    } else {
                        alertAutorisationFailed(e);
                    }
                }
            };
            e.printStackTrace();
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeLater(r);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                getProgressMonitor().setTicksCount(3);
                authClient = new OsmOAuthAuthorisationClient(
                        getAdvancedPropertiesPanel().getAdvancedParameters()
                );
                OAuthToken requestToken = authClient.getRequestToken(
                        getProgressMonitor().createSubTaskMonitor(1, false)
                );
                getProgressMonitor().worked(1);
                if (canceled)return;
                authClient.authorise(
                        requestToken,
                        getOsmUserName(),
                        getOsmPassword(),
                        pnlOsmPrivileges.getPrivileges(),
                        getProgressMonitor().createSubTaskMonitor(1, false)
                );
                getProgressMonitor().worked(1);
                if (canceled)return;
                final OAuthToken accessToken = authClient.getAccessToken(
                        getProgressMonitor().createSubTaskMonitor(1,false)
                );
                getProgressMonitor().worked(1);
                if (canceled)return;
                Runnable r = new Runnable() {
                    public void run() {
                        prepareUIForResultDisplay();
                        setAccessToken(accessToken);
                    }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                    r.run();
                } else {
                    SwingUtilities.invokeLater(r);
                }
            } catch(final OsmOAuthAuthorisationException e) {
                handleException(e);
            }
        }
    }
}