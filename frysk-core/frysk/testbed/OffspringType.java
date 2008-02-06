// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.testbed;

import frysk.sys.Fork;
import frysk.proc.Manager;
import frysk.proc.TaskObserver;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

public abstract class OffspringType {
    private OffspringType() { }
    public abstract ProcessIdentifier startOffspring(String stdin,
						     String stdout,
						     String stderr,
						     String[] argv);
    /**
     * Create a daemon offspring.
     */
    static public final OffspringType DAEMON = new OffspringType() {
	    public ProcessIdentifier startOffspring(String stdin,
						    String stdout,
						    String stderr,
						    String[] argv) {
		return Fork.daemon(stdin, stdout, stderr, argv);
	    }
	};
    /**
     * Create a child offspring.
     */
    static public final OffspringType CHILD = new OffspringType() {
	    public ProcessIdentifier startOffspring(String stdin,
						    String stdout,
						    String stderr,
						    String[] argv) {
		return Fork.exec(stdin, stdout, stderr, argv);
	    }
	};
    /**
     * Create a running attached child offspring.
     */
    static protected final OffspringType ATTACHED_CHILD = new OffspringType() {
	    public ProcessIdentifier startOffspring(String stdin,
						    String stdout,
						    String stderr,
						    String[] argv) {
		// Capture the child process id as it flys past.
		class TidObserver
		    extends TaskObserverBase
		    implements TaskObserver.Attached
		{
		    int tid;
		    public Action updateAttached (Task task) {
			tid = task.getTid();
			Manager.eventLoop.requestStop();
			return Action.CONTINUE;
		    }
		}
		TidObserver tidObserver = new TidObserver();
		// Start the child process, run the event loop until
		// the tid is known.
		Manager.host.requestCreateAttachedProc(stdin, stdout,
						       stderr, argv,
						       tidObserver);
		TestLib.assertRunUntilStop("starting attached child");
		// Return that captured TID.
		return ProcessIdentifierFactory.createFIXME(tidObserver.tid);
	    }
	};
}
