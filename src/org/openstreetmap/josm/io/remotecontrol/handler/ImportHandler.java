// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handler for import request
 */
public class ImportHandler extends RequestHandler {

    public static final String command = "import";
    public static final String permissionKey = "remotecontrol.permission.import";
    public static final boolean permissionDefault = true;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            DownloadTask osmTask = new DownloadOsmTask();
            osmTask.loadUrl(false, args.get("url"), null);
        } catch (Exception ex) {
            System.out.println("RemoteControl: Error parsing import remote control request:");
            ex.printStackTrace();
            throw new RequestHandlerErrorException();
        }
    }

    @Override
    public String[] getMandatoryParams()
    {
        return new String[] { "url" };
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to import data from the following URL:") +
        "<br>" + request;
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref()
    {
        return new PermissionPrefWithDefault(permissionKey, permissionDefault,
                "RemoteControl: import forbidden by preferences");
    }

    @Override
    protected void parseArgs() {
        HashMap<String, String> args = new HashMap<String, String>();
        if (request.indexOf('?') != -1) {
            String query = request.substring(request.indexOf('?') + 1);
            if (query.indexOf("url=") == 0) {
                args.put("url", decodeURL(query.substring(4)));
            } else {
                int urlIdx = query.indexOf("&url=");
                if (urlIdx != -1) {
                    String url = query.substring(urlIdx + 1);
                    args.put("url", decodeURL(query.substring(urlIdx + 5)));
                    query = query.substring(0, urlIdx);
                } else {
                    if (query.indexOf('#') != -1) {
                        query = query.substring(0, query.indexOf('#'));
                    }
                }
                String[] params = query.split("&", -1);
                for (String param : params) {
                    int eq = param.indexOf('=');
                    if (eq != -1) {
                        args.put(param.substring(0, eq), param.substring(eq + 1));
                    }
                }
            }
        }
        this.args = args;
    }

    private String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
    }
}