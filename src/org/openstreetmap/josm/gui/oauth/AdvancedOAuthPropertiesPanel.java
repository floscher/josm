// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

public class AdvancedOAuthPropertiesPanel extends VerticallyScrollablePanel {

    private JCheckBox cbUseDefaults;
    private JTextField tfConsumerKey;
    private JTextField tfConsumerSecret;
    private JTextField tfRequestTokenURL;
    private JTextField tfAccessTokenURL;
    private JTextField tfAutoriseURL;
    private UseDefaultItemListener ilUseDefault;

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0, 3, 3);
        gc.gridwidth = 2;
        cbUseDefaults = new JCheckBox(tr("Use default settings"));
        add(cbUseDefaults, gc);

        // -- consumer key
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        add(new JLabel(tr("Consumer Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerKey = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerKey);

        // -- consumer secret
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Consumer Secret:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerSecret = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerSecret);

        // -- request token URL
        gc.gridy = 3;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Request Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfRequestTokenURL = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfRequestTokenURL);

        // -- access token URL
        gc.gridy = 4;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Access Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenURL = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenURL);


        // -- autorise URL
        gc.gridy = 5;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Autorise URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAutoriseURL = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAutoriseURL);

        cbUseDefaults.addItemListener(ilUseDefault = new UseDefaultItemListener());
    }

    protected boolean hasCustomSettings() {
        return
        ! tfConsumerKey.getText().equals( OAuthParameters.DEFAULT_JOSM_CONSUMER_KEY)
        || ! tfConsumerSecret.getText().equals( OAuthParameters.DEFAULT_JOSM_CONSUMER_SECRET)
        || ! tfRequestTokenURL.getText().equals( OAuthParameters.DEFAULT_REQUEST_TOKEN_URL)
        || ! tfAccessTokenURL.getText().equals( OAuthParameters.DEFAULT_ACCESS_TOKEN_URL)
        || ! tfAutoriseURL.getText().equals( OAuthParameters.DEFAULT_AUTHORISE_URL);
    }

    protected boolean confirmOverwriteCustomSettings() {
        ButtonSpec[] buttons = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Continue"),
                        ImageProvider.get("ok"),
                        tr("Click to reset the OAuth settings to default values"),
                        null /* no dedicated help topic */
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to abort resetting to the OAuth default values"),
                        null /* no dedicated help topic */
                )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                AdvancedOAuthPropertiesPanel.this,
                tr(
                        "<html>JOSM is about to reset the OAuth settings to default values.<br>"
                        + "The current custom settings are not saved.</html>"
                ),
                tr("Overwrite custom OAuth settings?"),
                JOptionPane.WARNING_MESSAGE,
                null, /* no dedicated icon */
                buttons,
                buttons[0],
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard")
        );

        return ret == 0; // OK button clicked
    }

    protected void resetToDefaultSettings() {
        cbUseDefaults.setSelected(true);
        tfConsumerKey.setText( OAuthParameters.DEFAULT_JOSM_CONSUMER_KEY);
        tfConsumerSecret.setText( OAuthParameters.DEFAULT_JOSM_CONSUMER_SECRET);
        tfRequestTokenURL.setText(OAuthParameters.DEFAULT_REQUEST_TOKEN_URL);
        tfAccessTokenURL.setText(OAuthParameters.DEFAULT_ACCESS_TOKEN_URL);
        tfAutoriseURL.setText(OAuthParameters.DEFAULT_AUTHORISE_URL);

        setChildComponentsEnabled(false);
    }

    protected void setChildComponentsEnabled(boolean enabled){
        for (Component c: getComponents()) {
            if (c instanceof JTextField || c instanceof JLabel) {
                c.setEnabled(enabled);
            }
        }
    }

    /**
     * Replies the OAuth parameters currently edited in this properties panel.
     * 
     * @return the OAuth parameters
     */
    public OAuthParameters getAdvancedParameters() {
        if (cbUseDefaults.isSelected())
            return OAuthParameters.createDefault();
        OAuthParameters parameters = new OAuthParameters();
        parameters.setConsumerKey(tfConsumerKey.getText());
        parameters.setConsumerSecret(tfConsumerSecret.getText());
        parameters.setRequestTokenUrl(tfRequestTokenURL.getText());
        parameters.setAccessTokenUrl(tfAccessTokenURL.getText());
        parameters.setAuthoriseUrl(tfAutoriseURL.getText());
        return parameters;
    }

    /**
     * Sets the advanced parameters to be displayed
     * 
     * @param parameters the advanced parameters. Must not be null.
     * @throws IllegalArgumentException thrown if parameters is null.
     */
    public void setAdvancedParameters(OAuthParameters parameters) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        if (parameters.equals(OAuthParameters.createDefault())) {
            cbUseDefaults.setSelected(true);
            setChildComponentsEnabled(false);
        } else {
            cbUseDefaults.setSelected(false);
            setChildComponentsEnabled(true);
            tfConsumerKey.setText( parameters.getConsumerKey() == null ? "" : parameters.getConsumerKey());
            tfConsumerSecret.setText( parameters.getConsumerSecret() == null ? "" : parameters.getConsumerSecret());
            tfRequestTokenURL.setText(parameters.getRequestTokenUrl() == null ? "" : parameters.getRequestTokenUrl());
            tfAccessTokenURL.setText(parameters.getAccessTokenUrl() == null ? "" : parameters.getAccessTokenUrl());
            tfAutoriseURL.setText(parameters.getAuthoriseUrl() == null ? "" : parameters.getAuthoriseUrl());
        }
    }

    public AdvancedOAuthPropertiesPanel() {
        build();
    }

    /**
     * Initializes the panel from the values in the preferences <code>preferences</code>.
     * 
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException thrown if pref is null
     */
    public void initFromPreferences(Preferences pref) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        boolean useDefault = pref.getBoolean("oauth.settings.use-default", true);
        ilUseDefault.setEnabled(false);
        if (useDefault) {
            resetToDefaultSettings();
        } else {
            cbUseDefaults.setSelected(false);
            tfConsumerKey.setText(pref.get("oauth.settings.consumer-key", ""));
            tfConsumerSecret.setText(pref.get("oauth.settings.consumer-secret", ""));
            tfRequestTokenURL.setText(pref.get("oauth.settings.request-token-url", ""));
            tfAccessTokenURL.setText(pref.get("oauth.settings.access-token-url", ""));
            tfAutoriseURL.setText(pref.get("oauth.settings.authorise-url", ""));
            setChildComponentsEnabled(true);
        }
        ilUseDefault.setEnabled(true);
    }

    /**
     * Remembers the current values in the preferences <code>pref</code>.
     * 
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException thrown if pref is null.
     */
    public void rememberPreferences(Preferences pref) throws IllegalArgumentException  {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        pref.put("oauth.settings.use-default", cbUseDefaults.isSelected());
        if (cbUseDefaults.isSelected()) {
            pref.put("oauth.settings.consumer-key", null);
            pref.put("oauth.settings.consumer-secret", null);
            pref.put("oauth.settings.request-token-url", null);
            pref.put("oauth.settings.access-token-url", null);
            pref.put("oauth.settings.authorise-url", null);
        } else {
            pref.put("oauth.settings.consumer-key", tfConsumerKey.getText().trim());
            pref.put("oauth.settings.consumer-secret", tfConsumerSecret.getText().trim());
            pref.put("oauth.settings.request-token-url", tfRequestTokenURL.getText().trim());
            pref.put("oauth.settings.access-token-url", tfAccessTokenURL.getText().trim());
            pref.put("oauth.settings.authorise-url", tfAutoriseURL.getText().trim());
        }
    }

    class UseDefaultItemListener implements ItemListener {
        private boolean enabled;

        public void itemStateChanged(ItemEvent e) {
            if (!enabled) return;
            switch(e.getStateChange()) {
            case ItemEvent.SELECTED:
                if (hasCustomSettings()) {
                    if (!confirmOverwriteCustomSettings()) {
                        cbUseDefaults.setSelected(false);
                        return;
                    }
                }
                resetToDefaultSettings();
                break;
            case ItemEvent.DESELECTED:
                setChildComponentsEnabled(true);
                break;
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
