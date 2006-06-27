/**
 * 
 */
package frysk.gui.monitor.datamodels;

import java.util.logging.LogRecord;

import org.gnu.glib.CustomEvents;

import frysk.gui.monitor.ObservableLinkedList;

/**
 * @author pmuldoon
 *
 */
public class CoreDebugDataModel extends ObservableLinkedList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		
	public void write(final LogRecord record)
	{
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				add(new CoreDebugLogRecord(record));
			}
		});
	}
}
