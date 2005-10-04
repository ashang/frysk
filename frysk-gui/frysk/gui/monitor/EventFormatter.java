package frysk.gui.monitor;

import java.util.logging.LogRecord;
import java.util.logging.Formatter;

public class EventFormatter extends Formatter  {

	
	public String format(LogRecord arg0) {
		return  arg0.getSequenceNumber()+ " " + arg0.getMillis() + " " + arg0.getMessage() + "\n";
	}

}
