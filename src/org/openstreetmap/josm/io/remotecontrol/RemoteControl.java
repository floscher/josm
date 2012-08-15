// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;

/**
 * Manager class for remote control operations.
 *
 * IMPORTANT! increment the minor version on compatible API extensions
 * and increment the major version and set minor to 0 on incompatible changes.
 */
public class RemoteControl
{
    /**
     * If the remote cotrol feature is enabled or disabled. If disabled,
     * it should not start the server.
     */
    public static final BooleanProperty PROP_REMOTECONTROL_ENABLED = new BooleanProperty("remotecontrol.enabled", false);

    /**
     * RemoteControl HTTP protocol version. Change minor number for compatible
     * interface extensions. Change major number in case of incompatible
     * changes.
     */
    static final int protocolMajorVersion = 1;
    static final int protocolMinorVersion = 4;

    /**
     * Starts the remote control server
     */
    public static void start() {
        RemoteControlHttpServer.restartRemoteControlHttpServer();
    }

    /**
     * Adds external request handler.
     * Can be used by plugins that want to use remote control.
     * 
     * @param command The command name.
     * @param handlerClass The additional request handler.
     */
    public void addRequestHandler(String command, Class<? extends RequestHandler> handlerClass)
    {
        RequestProcessor.addRequestHandlerClass(command, handlerClass);
    }
}
