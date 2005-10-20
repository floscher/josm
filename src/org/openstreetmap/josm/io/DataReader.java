package org.openstreetmap.josm.io;

import org.openstreetmap.josm.command.DataSet;

/**
 * All Classes that subclass DataReader are capable of importing data from a source
 * to a DataSet.
 *
 * @author imi
 */
public interface DataReader {
	
	/**
	 * Thrown from the parse command in case of parsing problems.
	 * @author imi
	 */
	public class ParseException extends Exception {
		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}
		public ParseException(String message) {
			super(message);
		}
	}
	/**
	 * Thrown from the parse command in case of other problems like connection
	 * failed or a file could not be read.
	 * @author imi
	 */
	public class ConnectionException extends Exception {
		public ConnectionException(String message, Throwable cause) {
			super(message, cause);
		}
		public ConnectionException(String message) {
			super(message);
		}
	}

	/**
	 * Called to parse the source and return the dataset.
	 * @return The dataSet parsed.
	 * @throws DataParseException The data is ill-formated. The data could be
	 * 		retrieved but it contain errors that could not be handled.
	 * @throws ConnectionException A problem with the connection to the data source
	 * 		occoured. As example, the file could not be read or the server
	 * 		does not repsonse
	 */
	public DataSet parse() throws ParseException, ConnectionException; 
}
