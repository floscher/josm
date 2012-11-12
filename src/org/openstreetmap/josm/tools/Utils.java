// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Basic utils, that can be useful in different parts of the program.
 */
public class Utils {

    public static <T> boolean exists(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.evaluate(item))
                return true;
        }
        return false;
    }

    public static <T> boolean exists(Iterable<T> collection, Class<? extends T> klass) {
        for (Object item : collection) {
            if (klass.isInstance(item))
                return true;
        }
        return false;
    }

    public static <T> T find(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.evaluate(item))
                return item;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T find(Iterable<? super T> collection, Class<? extends T> klass) {
        for (Object item : collection) {
            if (klass.isInstance(item))
                return (T) item;
        }
        return null;
    }

    public static <T> Collection<T> filter(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return new FilteredCollection<T>(collection, predicate);
    }

    public static <T> T firstNonNull(T... items) {
        for (T i : items) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    /**
     * Filter a collection by (sub)class.
     * This is an efficient read-only implementation.
     */
    public static <S, T extends S> SubclassFilteredCollection<S, T> filteredCollection(Collection<S> collection, final Class<T> klass) {
        return new SubclassFilteredCollection<S, T>(collection, new Predicate<S>() {
            @Override
            public boolean evaluate(S o) {
                return klass.isInstance(o);
            }
        });
    }

    public static <T> int indexOf(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        int i = 0;
        for (T item : collection) {
            if (predicate.evaluate(item))
                return i;
            i++;
        }
        return -1;
    }

    /**
     * Get minimum of 3 values
     */
    public static int min(int a, int b, int c) {
        if (b < c) {
            if (a < b)
                return a;
            return b;
        } else {
            if (a < c)
                return a;
            return c;
        }
    }

    public static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * for convenience: test whether 2 objects are either both null or a.equals(b)
     */
    public static <T> boolean equal(T a, T b) {
        if (a == b)
            return true;
        return (a != null && a.equals(b));
    }

    public static void ensure(boolean condition, String message, Object...data) {
        if (!condition)
            throw new AssertionError(
                    MessageFormat.format(message,data)
            );
    }

    /**
     * return the modulus in the range [0, n)
     */
    public static int mod(int a, int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        int res = a % n;
        if (res < 0) {
            res += n;
        }
        return res;
    }

    /**
     * Joins a list of strings (or objects that can be converted to string via
     * Object.toString()) into a single string with fields separated by sep.
     * @param sep the separator
     * @param values collection of objects, null is converted to the
     *  empty string
     * @return null if values is null. The joined string otherwise.
     */
    public static String join(String sep, Collection<?> values) {
        if (sep == null)
            throw new IllegalArgumentException();
        if (values == null)
            return null;
        if (values.isEmpty())
            return "";
        StringBuilder s = null;
        for (Object a : values) {
            if (a == null) {
                a = "";
            }
            if (s != null) {
                s.append(sep).append(a.toString());
            } else {
                s = new StringBuilder(a.toString());
            }
        }
        return s.toString();
    }

    public static String joinAsHtmlUnorderedList(Collection<?> values) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<ul>");
        for (Object i : values) {
            sb.append("<li>").append(i).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    /**
     * convert Color to String
     * (Color.toString() omits alpha value)
     */
    public static String toString(Color c) {
        if (c == null)
            return "null";
        if (c.getAlpha() == 255)
            return String.format("#%06x", c.getRGB() & 0x00ffffff);
        else
            return String.format("#%06x(alpha=%d)", c.getRGB() & 0x00ffffff, c.getAlpha());
    }

    /**
     * convert float range 0 <= x <= 1 to integer range 0..255
     * when dealing with colors and color alpha value
     * @return null if val is null, the corresponding int if val is in the
     *         range 0...1. If val is outside that range, return 255
     */
    public static Integer color_float2int(Float val) {
        if (val == null)
            return null;
        if (val < 0 || val > 1)
            return 255;
        return (int) (255f * val + 0.5f);
    }

    /**
     * convert back
     */
    public static Float color_int2float(Integer val) {
        if (val == null)
            return null;
        if (val < 0 || val > 255)
            return 1f;
        return ((float) val) / 255f;
    }

    public static Color complement(Color clr) {
        return new Color(255 - clr.getRed(), 255 - clr.getGreen(), 255 - clr.getBlue(), clr.getAlpha());
    }

    public static int copyStream(InputStream source, OutputStream destination) throws IOException {
        int count = 0;
        byte[] b = new byte[512];
        int read;
        while ((read = source.read(b)) != -1) {
            count += read;
            destination.write(b, 0, read);
        }
        return count;
    }

    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return( path.delete() );
    }

    /**
     * <p>Utility method for closing an input stream.</p>
     *
     * @param is the input stream. May be null.
     */
    public static void close(InputStream is){
        if (is == null) return;
        try {
            is.close();
        } catch(IOException e){
            // ignore
        }
    }

    /**
     * <p>Utility method for closing an output stream.</p>
     *
     * @param os the output stream. May be null.
     */
    public static void close(OutputStream os){
        if (os == null) return;
        try {
            os.close();
        } catch(IOException e){
            // ignore
        }
    }

    /**
     * <p>Utility method for closing a reader.</p>
     *
     * @param reader the reader. May be null.
     */
    public static void close(Reader reader){
        if (reader == null) return;
        try {
            reader.close();
        } catch(IOException e){
            // ignore
        }
    }

    private final static double EPSILION = 1e-11;

    public static boolean equalsEpsilon(double a, double b) {
        return Math.abs(a - b) <= EPSILION;
    }

    /**
     * Copies the string {@code s} to system clipboard.
     * @param s string to be copied to clipboard.
     * @return true if succeeded, false otherwise.
     */
    public static boolean copyToClipboard(String s) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), new ClipboardOwner() {

                @Override
                public void lostOwnership(Clipboard clpbrd, Transferable t) {
                }
            });
            return true;
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts clipboard content as string.
     * @return string clipboard contents if available, {@code null} otherwise.
     */
    public static String getClipboardContent() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = null;
        for (int tries = 0; t == null && tries < 10; tries++) {
            try {
                t = clipboard.getContents(null);
            } catch (IllegalStateException e) { 
                // Clipboard currently unavailable. On some platforms, the system clipboard is unavailable while it is accessed by another application.
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
            }
        }
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                return text;
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Calculate MD5 hash of a string and output in hexadecimal format.
     * Output has length 32 with characters in range [0-9a-f]
     */
    public static String md5Hex(String data) {
        byte[] byteData = null;
        try {
            byteData = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        byte[] byteDigest = md.digest(byteData);
        return toHexString(byteDigest);
    }

    /**
     * Converts a byte array to a string of hexadecimal characters. Preserves leading zeros, so the
     * size of the output string is always twice the number of input bytes.
     */
    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length * 2];
        for (int j=0; j<bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v/16];
            hexChars[j*2 + 1] = hexArray[v%16];
        }
        return new String(hexChars);
    }

    /**
     * Topological sort.
     *
     * @param dependencies contains mappings (key -> value). In the final list of sorted objects, the key will come
     * after the value. (In other words, the key depends on the value(s).)
     * There must not be cyclic dependencies.
     * @return the list of sorted objects
     */
    public static <T> List<T> topologicalSort(final MultiMap<T,T> dependencies) {
        MultiMap<T,T> deps = new MultiMap<T,T>();
        for (T key : dependencies.keySet()) {
            deps.putVoid(key);
            for (T val : dependencies.get(key)) {
                deps.putVoid(val);
                deps.put(key, val);
            }
        }

        int size = deps.size();
        List<T> sorted = new ArrayList<T>();
        for (int i=0; i<size; ++i) {
            T parentless = null;
            for (T key : deps.keySet()) {
                if (deps.get(key).size() == 0) {
                    parentless = key;
                    break;
                }
            }
            if (parentless == null) throw new RuntimeException();
            sorted.add(parentless);
            deps.remove(parentless);
            for (T key : deps.keySet()) {
                deps.remove(key, parentless);
            }
        }
        if (sorted.size() != size) throw new RuntimeException();
        return sorted;
    }

    /**
     * Represents a function that can be applied to objects of {@code A} and
     * returns objects of {@code B}.
     * @param <A> class of input objects
     * @param <B> class of transformed objects
     */
    public static interface Function<A, B> {

        /**
         * Applies the function on {@code x}.
         * @param x an object of
         * @return the transformed object
         */
        B apply(A x);
    }

    /**
     * Transforms the collection {@code c} into an unmodifiable collection and
     * applies the {@link Function} {@code f} on each element upon access.
     * @param <A> class of input collection
     * @param <B> class of transformed collection
     * @param c a collection
     * @param f a function that transforms objects of {@code A} to objects of {@code B}
     * @return the transformed unmodifiable collection
     */
    public static <A, B> Collection<B> transform(final Collection<? extends A> c, final Function<A, B> f) {
        return new Collection<B>() {

            @Override
            public int size() {
                return c.size();
            }

            @Override
            public boolean isEmpty() {
                return c.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return c.contains(o);
            }

            @Override
            public Object[] toArray() {
                return c.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return c.toArray(a);
            }

            @Override
            public String toString() {
                return c.toString();
            }

            @Override
            public Iterator<B> iterator() {
                return new Iterator<B>() {

                    private Iterator<? extends A> it = c.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public B next() {
                        return f.apply(it.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public boolean add(B e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends B> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Convert Hex String to Color.
     * @param s Must be of the form "#34a300" or "#3f2", otherwise throws Exception.
     * Upper/lower case does not matter.
     * @return The corresponding color.
     */
    static public Color hexToColor(String s) {
        String clr = s.substring(1);
        if (clr.length() == 3) {
            clr = new String(new char[] {
                clr.charAt(0), clr.charAt(0), clr.charAt(1), clr.charAt(1), clr.charAt(2), clr.charAt(2)
            });
        }
        if (clr.length() != 6)
            throw new IllegalArgumentException();
        return new Color(Integer.parseInt(clr, 16));
    }

}
