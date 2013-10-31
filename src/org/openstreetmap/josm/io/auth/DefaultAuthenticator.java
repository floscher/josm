// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.io.OsmApi;

/**
 * This is the default authenticator used in JOSM. It delegates lookup of credentials
 * for the OSM API and an optional proxy server to the currently configured
 * {@link CredentialsManager}.
 *
 */
public  class DefaultAuthenticator extends Authenticator {
    private static DefaultAuthenticator instance;

    public static DefaultAuthenticator getInstance() {
        return instance;
    }

    public static void createInstance() {
        instance = new DefaultAuthenticator();
    }

    private final Map<RequestorType, Boolean> credentialsTried = new HashMap<RequestorType, Boolean>();
    private boolean enabled = true;

    private DefaultAuthenticator() {
    }

    /**
     * Called by the Java http stack when either the OSM API server or a proxy requires
     * authentication.
     *
     */
    @Override protected PasswordAuthentication getPasswordAuthentication() {
        if (!enabled)
            return null;
        try {
            if (getRequestorType().equals(Authenticator.RequestorType.SERVER)) {
                // if we are working with OAuth we don't prompt for a password
                if (OsmApi.isUsingOAuth())
                    return null;
            }
            boolean tried = credentialsTried.get(getRequestorType()) != null;
            CredentialsAgentResponse response = CredentialsManager.getInstance().getCredentials(getRequestorType(), getRequestingHost(), tried);
            if (response == null || response.isCanceled())
                return null;
            credentialsTried.put(getRequestorType(), true);
            return new PasswordAuthentication(response.getUsername(), response.getPassword());
        } catch(CredentialsAgentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
