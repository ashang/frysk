// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

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
