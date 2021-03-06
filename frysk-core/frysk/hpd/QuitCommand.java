// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

package frysk.hpd;

import java.util.Iterator;
import frysk.event.Event;
import frysk.event.Request;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.sys.Signal;
import frysk.util.CountDownLatch;
import java.util.List;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

class QuitCommand extends ParameterizedCommand {
    // Do the killing in the event loop in order to not invalidate
    // operations that are already in flight in the event loop. This
    // avoids a race seen in testing where a "quit" command is sent as
    // soon as the message from a breakpoint hit is output.
    private static class KillRequest extends Request {
	private final CLI cli;
	private final CountDownLatch quitLatch;
        KillRequest(CLI cli, CountDownLatch quitLatch) {
            super(Manager.eventLoop);
	    this.cli = cli;
	    this.quitLatch = quitLatch;
        }

        public final void execute() {
            for (Iterator iterator = cli.runningProcs.iterator();
		 iterator.hasNext();) {
                Proc p = (Proc) iterator.next();
		// FIXME: Should be sending the kill request to the
		// host; which then passes it down to back-end code.
		ProcessIdentifier pid = ProcessIdentifierFactory.create(p.getPid());
                Signal.KILL.kill(pid);
            }
            // Throw the countDown on the queue so that the command
            // thread will wait until events provoked by Signal.kill()
            // are handled.
            Manager.eventLoop.add(new Event() {
                    public void execute() {
                        quitLatch.countDown();
                    }
                });
        }

        public void request () {
	    if (isEventLoopThread())
		execute();
	    else {
                synchronized (this) {
                    super.request();
                }
            }
	}
    }

    QuitCommand() {
	super("Terminate the debugging session.", "quit");
    }

    void interpret(CLI cli, Input cmd, Object options) {
        CountDownLatch quitLatch = new CountDownLatch(1);
        new KillRequest(cli, quitLatch).request();
        while (true) {
            try {
                quitLatch.await();
                break;
            }
            catch (InterruptedException e) {
            }
        }
	cli.addMessage("Quitting...", Message.TYPE_NORMAL);
	DetachCommand detachCommand = new DetachCommand();
	Input command = new Input("detach").accept();
	detachCommand.interpret(cli, command);
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return -1;
    }
}
