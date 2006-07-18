package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.swing.BoundedRangeModel;
import javax.swing.JLabel;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

	private final InputStream in;
	private final BoundedRangeModel progress;
	private final JLabel currentAction;
	private int readSoFar = 0;
	private int lastDialogUpdate = 0;
	private final URLConnection connection;

	public ProgressInputStream(URLConnection con, BoundedRangeModel progress, JLabel currentAction) throws IOException {
		this.connection = con;
		this.in = con.getInputStream();
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

	@Override public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read != -1)
			advanceTicker(read);
		return read;
	}

	@Override public int read() throws IOException {
		int read = in.read();
		if (read != -1)
			advanceTicker(1);
		return read;
	}

	/**
	 * Increase ticker (progress counter and displayed text) by the given amount.
	 * @param amount
	 */
	private void advanceTicker(int amount) {
		if (progress.getMaximum() == 0 && connection.getContentLength() != -1)
			progress.setMaximum(connection.getContentLength());

		readSoFar += amount;

		if (readSoFar / 1024 != lastDialogUpdate) {
			lastDialogUpdate++;
			String progStr = " "+readSoFar/1024+"/";
			progStr += (progress.getMaximum()==0) ? "??? KB" : (progress.getMaximum()/1024)+" KB";
			progress.setValue(readSoFar);

			String cur = currentAction.getText();
			int i = cur.indexOf(' ');
			if (i != -1)
				cur = cur.substring(0, i) + progStr;
			else
				cur += progStr;
			currentAction.setText(cur);
		}
	}
}
