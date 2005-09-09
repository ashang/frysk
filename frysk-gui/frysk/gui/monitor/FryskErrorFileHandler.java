package frysk.gui.monitor;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;

public class FryskErrorFileHandler extends FileHandler {

	public FryskErrorFileHandler(String arg0, boolean arg1) throws IOException, SecurityException {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}
	public synchronized void publish(LogRecord arg) {
		
		// As this is going to log exceptions, and as frysk might be kill -9'd
		// I've not found a way to let normal FileHandlers do an explicit flush
		// after each log event. So we found logfiles that were incomplete.
		// So will cause and explicit flush after each publish, until we figure out
		// otherwise.
		
		super.publish(arg);
		super.flush();
	}


}
