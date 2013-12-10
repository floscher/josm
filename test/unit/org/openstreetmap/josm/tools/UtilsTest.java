// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;

import java.io.BufferedReader;
import java.net.URL;

/**
 * Unit tests of {@link Utils} class.
 */
public class UtilsTest {

    /**
     * Test of {@link Utils#strip} method.
     */
    @Test
    public void testStrip() {
        final String someWhite = 
            "\u00A0"+ // SPACE_SEPARATOR
            "\u2007"+ // LINE_SEPARATOR
            "\u202F"+ // PARAGRAPH_SEPARATOR
            "\u0009"+ // HORIZONTAL TABULATION
            "\n"    + // LINE FEED (U+000A, cannot be put as it in Java)
            "\u000B"+ // VERTICAL TABULATION
            "\u000C"+ // FORM FEED
            "\r"    + // CARRIAGE RETURN (U+000D, cannot be put as it in Java)
            "\u001C"+ // FILE SEPARATOR
            "\u001D"+ // GROUP SEPARATOR
            "\u001E"+ // RECORD SEPARATOR
            "\u001F"+ // UNIT SEPARATOR
            "\u2003"+ // EM SPACE
            "\u2007"+ // FIGURE SPACE
            "\u200B"+ // ZERO WIDTH SPACE
            "\uFEFF"+ // ZERO WIDTH NO-BREAK SPACE
            "\u3000"; // IDEOGRAPHIC SPACE
        Assert.assertNull(Utils.strip(null));
        Assert.assertEquals("", Utils.strip(""));
        Assert.assertEquals("", Utils.strip(" "));
        Assert.assertEquals("", Utils.strip("  "));
        Assert.assertEquals("", Utils.strip("   "));
        Assert.assertEquals("", Utils.strip(someWhite));
        Assert.assertEquals("a", Utils.strip("a"));
        Assert.assertEquals("ab", Utils.strip("ab"));
        Assert.assertEquals("abc", Utils.strip("abc"));
        Assert.assertEquals("a", Utils.strip(" a"));
        Assert.assertEquals("ab", Utils.strip(" ab"));
        Assert.assertEquals("abc", Utils.strip(" abc"));
        Assert.assertEquals("a", Utils.strip("a "));
        Assert.assertEquals("ab", Utils.strip("ab "));
        Assert.assertEquals("abc", Utils.strip("abc "));
        Assert.assertEquals("a", Utils.strip(someWhite+"a"+someWhite));
        Assert.assertEquals("ab", Utils.strip(someWhite+"ab"+someWhite));
        Assert.assertEquals("abc", Utils.strip(someWhite+"abc"+someWhite));
    }

    /**
     * Test of {@link Utils#toHexString} method.
     */
    @Test
    public void testToHexString(){
        Assert.assertEquals("", Utils.toHexString(null));
        Assert.assertEquals("", Utils.toHexString(new byte[0]));
        Assert.assertEquals("01", Utils.toHexString(new byte[]{0x1}));
        Assert.assertEquals("0102", Utils.toHexString(new byte[]{0x1,0x2}));
        Assert.assertEquals("12", Utils.toHexString(new byte[]{0x12}));
        Assert.assertEquals("127f", Utils.toHexString(new byte[]{0x12, 0x7f}));
        Assert.assertEquals("fedc", Utils.toHexString(new byte[]{(byte) 0xfe, (byte) 0xdc}));
    }

    @Test
    public void testOpenUrlGzip() throws Exception {
        Main.initApplicationPreferences();
        final BufferedReader x = Utils.openURLReaderAndDecompress(new URL("http://www.openstreetmap.org/trace/1613906/data"), true);
        Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        x.close();
    }

    @Test
    public void testOpenUrlBzip() throws Exception {
        Main.initApplicationPreferences();
        final BufferedReader x = Utils.openURLReaderAndDecompress(new URL("http://www.openstreetmap.org/trace/785544/data"), true);
        Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        x.close();
    }
}
