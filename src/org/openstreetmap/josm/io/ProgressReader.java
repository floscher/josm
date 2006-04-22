package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;

import javax.swing.BoundedRangeModel;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressReader extends Reader {

	private final Reader in;
	private final BoundedRangeModel progress;

	public ProgressReader(URLConnection con, BoundedRangeModel progress) throws IOException {
		this.in = new InputStreamReader(con.getInputStream());
		this.progress = progress;
		int contentLength = con.getContentLength();
		if (contentLength > 0)
			progress.setMaximum(contentLength);
		progress.setValue(0);
    }

	@Override public void close() throws IOException {
		in.close();
	}

	@Override public int read(char[] cbuf, int off, int len) throws IOException {
		int read = in.read(cbuf, off, len);
		progress.setValue(progress.getValue()+read);
		return read;
	}

}
