package frysk.gui.monitor.datamodels;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import frysk.gui.monitor.GuiObject;


public class CoreDebugLogRecord extends GuiObject {
	
	long sequence;
	String sourceClass;
	String sourceMethod;
	Level level;
	String message;
	long millis;
	LogRecord rawLog;
	
	
	public CoreDebugLogRecord()
	{
		
	}
	/**
	 * @param sourceClass
	 * @param sourceMethod
	 * @param level
	 * @param message
	 * @param millis
	 * @param rawLog
	 */
	public CoreDebugLogRecord(long sequence, String sourceClass, String sourceMethod, Level level, String message, long millis, LogRecord rawLog) {
		// TODO Auto-generated constructor stub
		this.sequence = sequence;
		this.sourceClass = sourceClass;
		this.sourceMethod = sourceMethod;
		this.level = level;
		this.message = message;
		this.millis = millis;
		this.rawLog = rawLog;
		super.setName(this.sequence+ " " + this.sourceClass+"::"+this.sourceMethod);
		super.setToolTip(this.message);
	}
	
	/**
	 * @param record
	 */
	public CoreDebugLogRecord(LogRecord record) {
		this.sequence = record.getSequenceNumber();
		this.sourceClass = record.getSourceClassName();
		this.sourceMethod = record.getSourceMethodName();
		this.level = record.getLevel();
		this.message = record.getMessage();
		this.millis = record.getMillis();
		this.rawLog = record;
		super.setName(this.sequence+ " " + this.sourceClass+"::"+this.sourceMethod);
		super.setToolTip(this.message);
	}
	/**
	 * @return Returns the level.
	 */
	public Level getLevel() {
		return level;
	}
	/**
	 * @param level The level to set.
	 */
	public void setLevel(Level level) {
		this.level = level;
	}
	/**
	 * @return Returns the message.
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message The message to set.
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return Returns the millis.
	 */
	public long getMillis() {
		return millis;
	}
	/**
	 * @param millis The millis to set.
	 */
	public void setMillis(long millis) {
		this.millis = millis;
	}
	/**
	 * @return Returns the rawLog.
	 */
	public LogRecord getRawLog() {
		return rawLog;
	}
	/**
	 * @param rawLog The rawLog to set.
	 */
	public void setRawLog(LogRecord rawLog) {
		this.rawLog = rawLog;
	}
	/**
	 * @return Returns the sourceClass.
	 */
	public String getSourceClass() {
		return sourceClass;
	}
	/**
	 * @param sourceClass The sourceClass to set.
	 */
	public void setSourceClass(String sourceClass) {
		this.sourceClass = sourceClass;
	}
	/**
	 * @return Returns the sourceMethod.
	 */
	public String getSourceMethod() {
		return sourceMethod;
	}
	/**
	 * @param sourceMethod The sourceMethod to set.
	 */
	public void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	/**
	 * @return Returns the sequence.
	 */
	public long getSequence() {
		return sequence;
	}

	/**
	 * @param sequence The sequence to set.
	 */
	public void setSequence(long sequence) {
		this.sequence = sequence;
	}


}
