package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.openstreetmap.josm.Main;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

	private final InputStream in;
	private int readSoFar = 0;
	private int lastDialogUpdate = 0;
	private final URLConnection connection;

	public ProgressInputStream(URLConnection con) throws IOException {
		this.connection = con;
		this.in = con.getInputStream();
		int contentLength = con.getContentLength();
		if (contentLength > 0)
			Main.pleaseWaitDlg.progress.setMaximum(contentLength);
		else
			Main.pleaseWaitDlg.progress.setMaximum(0);
		Main.pleaseWaitDlg.progress.setValue(0);
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
		if (Main.pleaseWaitDlg.progress.getMaximum() == 0 && connection.getContentLength() != -1)
			Main.pleaseWaitDlg.progress.setMaximum(connection.getContentLength());

		readSoFar += amount;

		if (readSoFar / 1024 != lastDialogUpdate) {
			lastDialogUpdate++;
			String progStr = " "+readSoFar/1024+"/";
			progStr += (Main.pleaseWaitDlg.progress.getMaximum()==0) ? "??? KB" : (Main.pleaseWaitDlg.progress.getMaximum()/1024)+" KB";
			Main.pleaseWaitDlg.progress.setValue(readSoFar);

			String cur = Main.pleaseWaitDlg.currentAction.getText();
			int i = cur.indexOf(' ');
			if (i != -1)
				cur = cur.substring(0, i) + progStr;
			else
				cur += progStr;
			Main.pleaseWaitDlg.currentAction.setText(cur);
		}
	}
}
