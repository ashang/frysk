// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

/**
 * An abstract class for creating a pair of pipes wired up to a child
 * process, with OUT wired to the child's STDIN, and IN wired to the
 * child's stdout.
 *
 * The child process is created using the method spawn.
 */

public abstract class PipePair
{
    /**
     * Write to this file descriptor.
     */
    public final FileDescriptor out;
    /**
     * Read from this file descriptor.
     */
    public final FileDescriptor in;
    /**
     * File descriptor of process wired to the pipe.
     */
    public final ProcessIdentifier pid;
    /**
     * Create a pipe-pair and then wire it up.
     *
     * this > out.out|out.in > child > in.out|in.in > this
     *
     * Spawn is parameterized with the Object o, allowing custom
     * behavior.  The child must close to.out and from.in.
     */
    protected PipePair (Execute execute)
    {
	final Pipe out = new Pipe ();
	final Pipe in = new Pipe ();
	// Wire: this.out > to.out
	this.out = out.out;
	// Wire: from.in > this.in
	this.in = in.in;
	// Wire: out.in > child > to.out
	pid = spawn (new RedirectPipes (out, in), execute);
    }

    private static class RedirectPipes
	extends Redirect
    {
	private Pipe out;
	private Pipe in;
	RedirectPipes (Pipe out, Pipe in)
	{
	    this.out = out;
	    this.in = in;
	}
	/**
	 * Executed by the child process - re-direct child's end of
	 * pipes to STDIN and STDOUT.
	 */
	protected void reopen ()
	{
	    FileDescriptor.in.dup (out.in);
	    FileDescriptor.out.dup (in.out);
	    in.close ();
	    out.close ();
	}
	/**
	 * Executed by the parent process (this) - close child's ends
	 * of pipes.
	 */
	protected void close ()
	{
	    out.in.close ();
	    in.out.close ();
	}
    }

    /**
     * Called from the context of the child process.
     */
    protected abstract ProcessIdentifier spawn (Redirect redirect,
						Execute exec);

    /**
     * Shut down the pipes.
     */
    public void close ()
    {
	in.close ();
	out.close ();
    }
}
