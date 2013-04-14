// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;

/**
 * Simple HTTP server that spawns a {@link RequestProcessor} for every
 * connection.
 *
 * Taken from YWMS plugin by frsantos.
 */
public class RemoteControlHttpServer extends Thread {

    /** Default port for the HTTP server */
    public static final int DEFAULT_PORT = 8111;

    /** The server socket */
    private ServerSocket server;

    private static RemoteControlHttpServer instance;

    /**
     * Starts or restarts the HTTP server
     */
    public static void restartRemoteControlHttpServer() {
        try {
            stopRemoteControlHttpServer();

            instance = new RemoteControlHttpServer(DEFAULT_PORT);
            instance.start();
        } catch (BindException ex) {
            System.err.println(tr("Warning: Cannot start remotecontrol server on port {0}: {1}",
                    Integer.toString(DEFAULT_PORT), ex.getLocalizedMessage()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Stops the HTTP server
     * @since 5861
     */
    public static void stopRemoteControlHttpServer() {
        if (instance != null) {
            try {
                instance.stopServer();
                instance = null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Constructor
     * @param port The port this server will listen on
     * @throws IOException when connection errors
     */
    public RemoteControlHttpServer(int port)
        throws IOException
    {
        super("RemoteControl HTTP Server");
        this.setDaemon(true);
        // Start the server socket with only 1 connection.
        // Also make sure we only listen
        // on the local interface so nobody from the outside can connect!
        this.server = new ServerSocket(port, 1,
            InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection
     */
    public void run()
    {
        System.out.println("RemoteControl::Accepting connections on port " + server.getLocalPort());
        while (true)
        {
            try
            {
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            }
            catch( SocketException se)
            {
                if( !server.isClosed() )
                    se.printStackTrace();
                }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Stops the HTTP server
     *
     * @throws IOException
     */
    public void stopServer() throws IOException
    {
        server.close();
        System.out.println("RemoteControl::Server stopped.");
    }
}
