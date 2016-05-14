// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Downloads a file and caches it on disk in order to reduce network load.
 *
 * Supports URLs, local files, and a custom scheme (<code>resource:</code>) to get
 * resources from the current *.jar file. (Local caching is only done for URLs.)
 * <p>
 * The mirrored file is only downloaded if it has been more than 7 days since
 * last download. (Time can be configured.)
 * <p>
 * The file content is normally accessed with {@link #getInputStream()}, but
 * you can also get the mirrored copy with {@link #getFile()}.
 */
public class CachedFile implements Closeable {

    /**
     * Caching strategy.
     */
    public enum CachingStrategy {
        /**
         * If cached file on disk is older than a certain time (7 days by default),
         * consider the cache stale and try to download the file again.
         */
        MaxAge,
        /**
         * Similar to MaxAge, considers the cache stale when a certain age is
         * exceeded. In addition, a If-Modified-Since HTTP header is added.
         * When the server replies "304 Not Modified", this is considered the same
         * as a full download.
         */
        IfModifiedSince
    }

    protected String name;
    protected long maxAge;
    protected String destDir;
    protected String httpAccept;
    protected CachingStrategy cachingStrategy;

    private boolean fastFail;
    private HttpClient activeConnection;
    protected File cacheFile;
    protected boolean initialized;

    public static final long DEFAULT_MAXTIME = -1L;
    public static final long DAYS = 24L*60L*60L; // factor to get caching time in days

    private final Map<String, String> httpHeaders = new ConcurrentHashMap<>();

    /**
     * Constructs a CachedFile object from a given filename, URL or internal resource.
     *
     * @param name can be:<ul>
     *  <li>relative or absolute file name</li>
     *  <li>{@code file:///SOME/FILE} the same as above</li>
     *  <li>{@code http://...} a URL. It will be cached on disk.</li>
     *  <li>{@code resource://SOME/FILE} file from the classpath (usually in the current *.jar)</li>
     *  <li>{@code josmdir://SOME/FILE} file inside josm user data directory (since r7058)</li>
     *  <li>{@code josmplugindir://SOME/FILE} file inside josm plugin directory (since r7834)</li></ul>
     */
    public CachedFile(String name) {
        this.name = name;
    }

    /**
     * Set the name of the resource.
     * @param name can be:<ul>
     *  <li>relative or absolute file name</li>
     *  <li>{@code file:///SOME/FILE} the same as above</li>
     *  <li>{@code http://...} a URL. It will be cached on disk.</li>
     *  <li>{@code resource://SOME/FILE} file from the classpath (usually in the current *.jar)</li>
     *  <li>{@code josmdir://SOME/FILE} file inside josm user data directory (since r7058)</li>
     *  <li>{@code josmplugindir://SOME/FILE} file inside josm plugin directory (since r7834)</li></ul>
     * @return this object
     */
    public CachedFile setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set maximum age of cache file. Only applies to URLs.
     * When this time has passed after the last download of the file, the
     * cache is considered stale and a new download will be attempted.
     * @param maxAge the maximum cache age in seconds
     * @return this object
     */
    public CachedFile setMaxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Set the destination directory for the cache file. Only applies to URLs.
     * @param destDir the destination directory
     * @return this object
     */
    public CachedFile setDestDir(String destDir) {
        this.destDir = destDir;
        return this;
    }

    /**
     * Set the accepted MIME types sent in the HTTP Accept header. Only applies to URLs.
     * @param httpAccept the accepted MIME types
     * @return this object
     */
    public CachedFile setHttpAccept(String httpAccept) {
        this.httpAccept = httpAccept;
        return this;
    }

    /**
     * Set the caching strategy. Only applies to URLs.
     * @param cachingStrategy caching strategy
     * @return this object
     */
    public CachedFile setCachingStrategy(CachingStrategy cachingStrategy) {
        this.cachingStrategy = cachingStrategy;
        return this;
    }

    /**
     * Sets the http headers. Only applies to URL pointing to http or https resources
     * @param headers that should be sent together with request
     * @return this object
     */
    public CachedFile setHttpHeaders(Map<String, String> headers) {
        this.httpHeaders.putAll(headers);
        return this;
    }

    /**
     * Sets whether opening HTTP connections should fail fast, i.e., whether a
     * {@link HttpClient#setConnectTimeout(int) low connect timeout} should be used.
     * @param fastFail whether opening HTTP connections should fail fast
     */
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns maximum age of cache file. Only applies to URLs.
     * When this time has passed after the last download of the file, the
     * cache is considered stale and a new download will be attempted.
     * @return the maximum cache age in seconds
     */
    public long getMaxAge() {
        return maxAge;
    }

    public String getDestDir() {
        return destDir;
    }

    public String getHttpAccept() {
        return httpAccept;
    }

    public CachingStrategy getCachingStrategy() {
        return cachingStrategy;
    }

    /**
     * Get InputStream to the requested resource.
     * @return the InputStream
     * @throws IOException when the resource with the given name could not be retrieved
     */
    public InputStream getInputStream() throws IOException {
        File file = getFile();
        if (file == null) {
            if (name.startsWith("resource://")) {
                InputStream is = getClass().getResourceAsStream(
                        name.substring("resource:/".length()));
                if (is == null)
                    throw new IOException(tr("Failed to open input stream for resource ''{0}''", name));
                return is;
            } else {
                throw new IOException("No file found for: "+name);
            }
        }
        return new FileInputStream(file);
    }

    /**
     * Get the full content of the requested resource as a byte array.
     * @return the full content of the requested resource as byte array
     * @throws IOException in case of an I/O error
     */
    public byte[] getByteContent() throws IOException {
        try (InputStream is = getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8192];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }

    /**
     * Returns {@link #getInputStream()} wrapped in a buffered reader.
     * <p>
     * Detects Unicode charset in use utilizing {@link UTFInputStreamReader}.
     *
     * @return buffered reader
     * @throws IOException if any I/O error occurs
     * @since 9411
     */
    public BufferedReader getContentReader() throws IOException {
        return new BufferedReader(UTFInputStreamReader.create(getInputStream()));
    }

    /**
     * Get local file for the requested resource.
     * @return The local cache file for URLs. If the resource is a local file,
     * returns just that file.
     * @throws IOException when the resource with the given name could not be retrieved
     */
    public synchronized File getFile() throws IOException {
        if (initialized)
            return cacheFile;
        initialized = true;
        URL url;
        try {
            url = new URL(name);
            if ("file".equals(url.getProtocol())) {
                cacheFile = new File(name.substring("file:/".length() - 1));
                if (!cacheFile.exists()) {
                    cacheFile = new File(name.substring("file://".length() - 1));
                }
            } else {
                cacheFile = checkLocal(url);
            }
        } catch (MalformedURLException e) {
            if (name.startsWith("resource://")) {
                return null;
            } else if (name.startsWith("josmdir://")) {
                cacheFile = new File(Main.pref.getUserDataDirectory(), name.substring("josmdir://".length()));
            } else if (name.startsWith("josmplugindir://")) {
                cacheFile = new File(Main.pref.getPluginsDirectory(), name.substring("josmplugindir://".length()));
            } else {
                cacheFile = new File(name);
            }
        }
        if (cacheFile == null)
            throw new IOException("Unable to get cache file for "+name);
        return cacheFile;
    }

    /**
     * Looks for a certain entry inside a zip file and returns the entry path.
     *
     * Replies a file in the top level directory of the ZIP file which has an
     * extension <code>extension</code>. If more than one files have this
     * extension, the last file whose name includes <code>namepart</code>
     * is opened.
     *
     * @param extension  the extension of the file we're looking for
     * @param namepart the name part
     * @return The zip entry path of the matching file. Null if this cached file
     * doesn't represent a zip file or if there was no matching
     * file in the ZIP file.
     */
    public String findZipEntryPath(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.a;
    }

    /**
     * Like {@link #findZipEntryPath}, but returns the corresponding InputStream.
     * @param extension  the extension of the file we're looking for
     * @param namepart the name part
     * @return InputStream to the matching file. Null if this cached file
     * doesn't represent a zip file or if there was no matching
     * file in the ZIP file.
     * @since 6148
     */
    public InputStream findZipEntryInputStream(String extension, String namepart) {
        Pair<String, InputStream> ze = findZipEntryImpl(extension, namepart);
        if (ze == null) return null;
        return ze.b;
    }

    private Pair<String, InputStream> findZipEntryImpl(String extension, String namepart) {
        File file = null;
        try {
            file = getFile();
        } catch (IOException ex) {
            Main.warn(ex, false);
        }
        if (file == null)
            return null;
        Pair<String, InputStream> res = null;
        try {
            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            ZipEntry resentry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith('.' + extension)) {
                    /* choose any file with correct extension. When more than
                        one file, prefer the one which matches namepart */
                    if (resentry == null || entry.getName().indexOf(namepart) >= 0) {
                        resentry = entry;
                    }
                }
            }
            if (resentry != null) {
                InputStream is = zipFile.getInputStream(resentry);
                res = Pair.create(resentry.getName(), is);
            } else {
                Utils.close(zipFile);
            }
        } catch (IOException e) {
            if (file.getName().endsWith(".zip")) {
                Main.warn(tr("Failed to open file with extension ''{2}'' and namepart ''{3}'' in zip file ''{0}''. Exception was: {1}",
                        file.getName(), e.toString(), extension, namepart));
            }
        }
        return res;
    }

    /**
     * Clear the cache for the given resource.
     * This forces a fresh download.
     * @param name the URL
     */
    public static void cleanup(String name) {
        cleanup(name, null);
    }

    /**
     * Clear the cache for the given resource.
     * This forces a fresh download.
     * @param name the URL
     * @param destDir the destination directory (see {@link #setDestDir(java.lang.String)})
     */
    public static void cleanup(String name, String destDir) {
        URL url;
        try {
            url = new URL(name);
            if (!"file".equals(url.getProtocol())) {
                String prefKey = getPrefKey(url, destDir);
                List<String> localPath = new ArrayList<>(Main.pref.getCollection(prefKey));
                if (localPath.size() == 2) {
                    File lfile = new File(localPath.get(1));
                    if (lfile.exists()) {
                        Utils.deleteFile(lfile);
                    }
                }
                Main.pref.putCollection(prefKey, null);
            }
        } catch (MalformedURLException e) {
            Main.warn(e);
        }
    }

    /**
     * Get preference key to store the location and age of the cached file.
     * 2 resources that point to the same url, but that are to be stored in different
     * directories will not share a cache file.
     * @param url URL
     * @param destDir destination directory
     * @return Preference key
     */
    private static String getPrefKey(URL url, String destDir) {
        StringBuilder prefKey = new StringBuilder("mirror.");
        if (destDir != null) {
            prefKey.append(destDir).append('.');
        }
        prefKey.append(url.toString());
        return prefKey.toString().replaceAll("=", "_");
    }

    private File checkLocal(URL url) throws IOException {
        String prefKey = getPrefKey(url, destDir);
        String urlStr = url.toExternalForm();
        long age = 0L;
        long lMaxAge = maxAge;
        Long ifModifiedSince = null;
        File localFile = null;
        List<String> localPathEntry = new ArrayList<>(Main.pref.getCollection(prefKey));
        boolean offline = false;
        try {
            checkOfflineAccess(urlStr);
        } catch (OfflineAccessException e) {
            offline = true;
        }
        if (localPathEntry.size() == 2) {
            localFile = new File(localPathEntry.get(1));
            if (!localFile.exists()) {
                localFile = null;
            } else {
                if (maxAge == DEFAULT_MAXTIME
                        || maxAge <= 0 // arbitrary value <= 0 is deprecated
                ) {
                    lMaxAge = Main.pref.getInteger("mirror.maxtime", 7*24*60*60); // one week
                }
                age = System.currentTimeMillis() - Long.parseLong(localPathEntry.get(0));
                if (offline || age < lMaxAge*1000) {
                    return localFile;
                }
                if (cachingStrategy == CachingStrategy.IfModifiedSince) {
                    ifModifiedSince = Long.valueOf(localPathEntry.get(0));
                }
            }
        }
        if (destDir == null) {
            destDir = Main.pref.getCacheDirectory().getPath();
        }

        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            Utils.mkDirs(destDirFile);
        }

        // No local file + offline => nothing to do
        if (offline) {
            return null;
        }

        String a = urlStr.replaceAll("[^A-Za-z0-9_.-]", "_");
        String localPath = "mirror_" + a;
        destDirFile = new File(destDir, localPath + ".tmp");
        try {
            activeConnection = HttpClient.create(url)
                    .setAccept(httpAccept)
                    .setIfModifiedSince(ifModifiedSince == null ? 0L : ifModifiedSince)
                    .setHeaders(httpHeaders);
            if (fastFail) {
                activeConnection.setReadTimeout(1000);
            }
            final HttpClient.Response con = activeConnection.connect();
            if (ifModifiedSince != null && con.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (Main.isDebugEnabled()) {
                    Main.debug("304 Not Modified ("+urlStr+')');
                }
                if (localFile == null)
                    throw new AssertionError();
                Main.pref.putCollection(prefKey,
                        Arrays.asList(Long.toString(System.currentTimeMillis()), localPathEntry.get(1)));
                return localFile;
            }
            try (InputStream bis = new BufferedInputStream(con.getContent())) {
                Files.copy(bis, destDirFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            activeConnection = null;
            localFile = new File(destDir, localPath);
            if (Main.platform.rename(destDirFile, localFile)) {
                Main.pref.putCollection(prefKey,
                        Arrays.asList(Long.toString(System.currentTimeMillis()), localFile.toString()));
            } else {
                Main.warn(tr("Failed to rename file {0} to {1}.",
                destDirFile.getPath(), localFile.getPath()));
            }
        } catch (IOException e) {
            if (age >= lMaxAge*1000 && age < lMaxAge*1000*2) {
                Main.warn(tr("Failed to load {0}, use cached file and retry next time: {1}", urlStr, e));
                return localFile;
            } else {
                throw e;
            }
        }

        return localFile;
    }

    private static void checkOfflineAccess(String urlString) {
        OnlineResource.JOSM_WEBSITE.checkOfflineAccess(urlString, Main.getJOSMWebsite());
        OnlineResource.OSM_API.checkOfflineAccess(urlString, OsmApi.getOsmApi().getServerUrl());
    }

    /**
     * Attempts to disconnect an URL connection.
     * @see HttpClient#disconnect()
     * @since 9411
     */
    @Override
    public void close() {
        if (activeConnection != null) {
            activeConnection.disconnect();
        }
    }
}
