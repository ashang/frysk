// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

package frysk.sys;

import java.io.File;

/**
 * Open a pty
 */

public class PseudoTerminal extends FileDescriptor {
    /**
     * Returns the pathname of corrsponding to the fd
     */
    private static native String getName(int fd);
    /**
     * Returns an open master fd for a pseudo-terminal.
     */
    private static native int open (boolean controllingTerminal);

    private final String name;
    private final File file;

    /**
     * Open a pseudo-terminal, a.k.a. pty, not wired to anything.
     */
    public PseudoTerminal(boolean controllingTerminal) {
	super(open(controllingTerminal));
	name = getName(getFd());
	file = new File (name);
    }
    public PseudoTerminal() {
	this(false);
    }

    public String toString() {
	return name;
    }

    /**
     * Return the path of the pseudo-terminal's slave.
     */
    public File getFile ()
    {
	return file;
    }

    /**
     * Redirect stdin, stdout, and stderr to this PseudoTerminal.
     *
     * The file descriptors are created in the parent process so that
     * child does not need to run the risk of running out of
     * descriptors et.al. and recover from that.
     */
    static private class RedirectStdio
	extends Redirect
    {
	protected final String name;
	RedirectStdio (String name)
	{
	    this.name = name;
	}
	/**
	 * Execute in context of child.
	 */
	public native void reopen ();
	/**
	 * Executed in context of parent.
	 */
	public void close ()
	{
	}
    }

    /**
     * Convenience method, adds a child process bound to this
     * pseudo-terminal.
     */
    public ProcessIdentifier addChild(String[] args) {
	return ChildFactory.create(new RedirectStdio (name), new Exec (args));
    }

    /**
     * Convenience method, adds a daemon process bound to this
     * pseudo-terminal.
     */
    public ProcessIdentifier addDaemon(String[] args) {
	return DaemonFactory.create(new RedirectStdio (name), new Exec (args));
    }
}
