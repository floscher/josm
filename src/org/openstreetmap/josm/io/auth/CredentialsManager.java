// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;

import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.Utils;

/**
 * CredentialManager is a factory for the single credential agent used.
 *
 * Currently, it defaults to replying an instance of {@link JosmPreferencesCredentialAgent}.
 *
 */
public class CredentialsManager implements CredentialsAgent {

    private static CredentialsManager instance;

    /**
     * Replies the single credential agent used in JOSM
     *
     * @return the single credential agent used in JOSM
     */
    static public CredentialsManager getInstance() {
        if (instance == null) {
            CredentialsAgent delegate;
            if (agentFactory == null) {
                delegate = new JosmPreferencesCredentialAgent();
            } else {
                delegate = agentFactory.getCredentialsAgent();
            }
            instance = new CredentialsManager(delegate);
        }
        return instance;
    }

    private static CredentialsAgentFactory agentFactory;

    public interface CredentialsAgentFactory {
        CredentialsAgent getCredentialsAgent();
    }

    /**
     * Plugins can register a CredentialsAgentFactory, thereby overriding
     * JOSM's default credentials agent.
     * @param agentFactory The Factory that provides the custom CredentialsAgent.
     * Can be null to clear the factory and switch back to default behavior.
     */
    public static void registerCredentialsAgentFactory(CredentialsAgentFactory agentFactory) {
        CredentialsManager.agentFactory = agentFactory;
        CredentialsManager.instance = null;
    }

    /*****
     * non-static fields and methods
     */

    private CredentialsAgent delegate;

    public CredentialsManager(CredentialsAgent delegate) {
        this.delegate = delegate;
    }

    public String getUsername() {
        return getUsername(OsmApi.getOsmApi().getHost());
    }

    public String getUsername(String host) {
        String username = null;
        try {
            PasswordAuthentication auth = lookup(RequestorType.SERVER, host);
            if (auth != null) {
                username = auth.getUserName();
            }
        } catch (CredentialsAgentException ex) {
            return null;
        }
        if (username == null) return null;
        username = username.trim();
        return Utils.equal(username, "") ? null : username;
    }

    @Override
    public PasswordAuthentication lookup(RequestorType requestorType, String host) throws CredentialsAgentException {
        return delegate.lookup(requestorType, host);
    }

    @Override
    public void store(RequestorType requestorType, String host, PasswordAuthentication credentials) throws CredentialsAgentException {
        if (requestorType == RequestorType.SERVER && Utils.equal(OsmApi.getOsmApi().getHost(), host)) {
            String username = credentials.getUserName();
            if(username != null && !username.trim().isEmpty()) {
                JosmUserIdentityManager.getInstance().setPartiallyIdentified(username);
            }
        }
        delegate.store(requestorType, host, credentials);
    }

    @Override
    public CredentialsAgentResponse getCredentials(RequestorType requestorType, String host, boolean noSuccessWithLastResponse) throws CredentialsAgentException {
        return delegate.getCredentials(requestorType, host, noSuccessWithLastResponse);
    }

    @Override
    public OAuthToken lookupOAuthAccessToken() throws CredentialsAgentException {
        return delegate.lookupOAuthAccessToken();
    }

    @Override
    public void storeOAuthAccessToken(OAuthToken accessToken) throws CredentialsAgentException {
        delegate.storeOAuthAccessToken(accessToken);
    }

    @Override
    public Component getPreferencesDecorationPanel() {
        return delegate.getPreferencesDecorationPanel();
    }
}
