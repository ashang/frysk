/**
 * 
 */
package frysk.gui.monitor.datamodels;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author pmuldoon
 *
 */
public class CoreDebugHandler extends Handler {

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
	public void publish(LogRecord record) {	
		// Ensure that this log record should be logged by this Handler

		if (!isLoggable(record))
			return;
		
		DataModelManager.theManager.coreDebugDataModel.write(record);
	}

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#flush()
	 */
	public void flush() {

	}

	/* (non-Javadoc)
	 * @see java.util.logging.Handler#close()
	 */
	public void close() throws SecurityException {

	}
	
}
