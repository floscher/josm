package org.openstreetmap.josm.gui.io;



// License: GPL. For details, see LICENSE file.

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.net.URLConnection;
import java.util.Enumeration;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;


/**
 * Asynchronous task for downloading andnd unpacking arbitrary file lists
 * Shows progress bar when donloading
 */
public class DownloadFileTask extends PleaseWaitRunnable{
    private final String address;
    private final File file;
    private final boolean mkdir;
    private final boolean unpack;

    /**
     * Creates the download task
     *
     * @param parent the parent component relative to which the {@see PleaseWaitDialog} is displayed
     * @param title the title to display in the {@see PleaseWaitDialog}
     * @throws IllegalArgumentException thrown if toUpdate is null
     */
    public DownloadFileTask(Component parent, String address, File file, boolean mkdir, boolean unpack) {
        super(parent, tr("Downloading file"), false);
        this.address = address;
        this.file = file;
        this.mkdir = mkdir;
        this.unpack = unpack;
                
    }    
    
    private static class DownloadException extends Exception {
        public DownloadException(String msg) {
            super(msg);
        }
    }

    private boolean canceled;
    private URLConnection downloadConnection;

    private synchronized void closeConnectionIfNeeded() {
        if (downloadConnection != null && downloadConnection instanceof HttpURLConnection) {
            HttpURLConnection conn = ((HttpURLConnection) downloadConnection);
            conn.disconnect();
        }
        downloadConnection = null;
    }


    @Override 
    protected void cancel() {
        this.canceled = true;
        closeConnectionIfNeeded();
    }

    @Override 
    protected void finish() {}

    public void download() throws DownloadException {
        OutputStream out = null;
        InputStream in = null;
        try {
            if (mkdir) {
                File newDir = file.getParentFile();
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
            }
            
            URL url = new URL(address);
            int size;
            synchronized(this) {
                downloadConnection = url.openConnection();
                downloadConnection.setRequestProperty("Cache-Control", "no-cache");
                downloadConnection.setRequestProperty("User-Agent",Version.getInstance().getAgentString());
                downloadConnection.setRequestProperty("Host", url.getHost());
                downloadConnection.connect();
                size = downloadConnection.getContentLength();
            }
            
            progressMonitor.setTicksCount(100);
            progressMonitor.subTask(tr("Downloading File {0}: {1} bytes...", file.getName(),size));
            
            in = downloadConnection.getInputStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[32768];
            int count=0;
            int p1=0, p2=0;
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
                count+=read;
                if (canceled) return;                            
                p2 = 100 * count / size;
                if (p2!=p1) {
                    progressMonitor.setTicks(p2);
                    p1=p2;
                }
            }
            out.close();
            System.out.println(tr("Download finished"));
            if (unpack) {
                System.out.println(tr("Unpacking {0} into {1}", file.getAbsolutePath(), file.getParent()));
                unzipFileRecursively(file, file.getParent());
                file.delete();
            }
        } catch(MalformedURLException e) {
            String msg = tr("Warning: Cannot download file ''{0}''. Its download link ''{1}'' is not a valid URL. Skipping download.", file.getName(), address);
            System.err.println(msg);
            throw new DownloadException(msg);
        } catch (IOException e) {
            if (canceled)
                return;
            throw new DownloadException(e.getMessage());
        } finally {
            closeConnectionIfNeeded();
            Utils.close(out);
        }
    }

    @Override 
    protected void realRun() throws SAXException, IOException {
        if (canceled) return;
        try {
            download();
        } catch(DownloadException e) {
            e.printStackTrace();
        }
    }

    /**
     * Replies true if the task was canceled by the user
     *
     * @return
     */
    public boolean isCanceled() {
        return canceled;
    }
    
    /**
     * Recursive unzipping function
     * TODO: May be placed somewhere else - Tools.Utils?
     * @param file
     * @param dir
     * @throws IOException 
     */
    public static void unzipFileRecursively(File file, String dir) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            Enumeration es = zf.entries();
            ZipEntry ze;
            while (es.hasMoreElements()) {
                ze = (ZipEntry) es.nextElement();
                File newFile = new File(dir, ze.getName());
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    is = zf.getInputStream(ze);
                    os = new BufferedOutputStream(new FileOutputStream(newFile));
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.close();
                    is.close();
                }
            }
            zf.close();
        } finally {
            if (zf!=null) zf.close();
        }
    }
}

