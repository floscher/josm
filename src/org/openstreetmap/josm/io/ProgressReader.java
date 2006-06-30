package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;

import javax.swing.BoundedRangeModel;
import javax.swing.JLabel;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressReader extends InputStream {

	private final Reader in;
	private final BoundedRangeModel progress;
	private final JLabel currentAction;
	private int readSoFar = 0;

	public ProgressReader(URLConnection con, BoundedRangeModel progress, JLabel currentAction) throws IOException {
		this.in = new InputStreamReader(con.getInputStream());
		this.progress = progress;
		this.currentAction = currentAction;
		int contentLength = con.getContentLength();
		if (contentLength > 0)
			progress.setMaximum(contentLength);
		else
			progress.setMaximum(0);
		progress.setValue(0);
    }

	@Override public void close() throws IOException {
		in.close();
	}

	@Override public int read() throws IOException {
		int read = in.read();
		readSoFar++;

		String progStr = " ("+readSoFar+"/";
		if (progress.getMaximum() == 0)
			progStr += "???)";
		else
			progStr += progress.getMaximum()+")";
		
		String cur = currentAction.getText();
		int i = cur.indexOf(' ');
		if (i != -1)
			cur = cur.substring(0, i) + progStr;
		else
			cur += progStr;
		currentAction.setText(cur);
		progress.setValue(readSoFar);
		return read;
    }
}
