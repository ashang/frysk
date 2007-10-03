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

package frysk;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;


/**
 * The event looger for internal state.
 */

public class EventLogger
{
    protected EventLogger () {}
    static private Logger logger = null;
    
    static public Logger get (String log_subdir, String log_entry)
    {
	if (logger != null) {
	    return logger;
	}
	LogManager manager = LogManager.getLogManager();

	// Read $HOME/.frysk/logging.properties, if present.
	try {
	    FileInputStream properties
		= new FileInputStream (Config.FRYSK_DIR
				       + "logging.properties");
	    manager.readConfiguration (properties);
	}
	catch (FileNotFoundException e) {
	    // toss, don't care
	}
	catch (IOException e) {
	    e.printStackTrace ();
	    System.exit (1);
	}
	catch (SecurityException e) {
	    e.printStackTrace ();
	    System.exit (1);
	}

	logger = Logger.getLogger (Config.FRYSK_LOG_ID);
	try {
	    FileHandler handler = new FileHandler
		(Config.FRYSK_DIR + log_subdir + log_entry,
		 1024 * 128, 1);
	    handler.setFormatter (new Formatter ()
		{
		    public String format(LogRecord record)
		    {
			return formatMessage(record);
		    }
		});
	    logger.addHandler (handler);
	}
	catch (IOException e) {
	    e.printStackTrace ();
	}

	logger.setUseParentHandlers(false);
	return logger;
    }
}
