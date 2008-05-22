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

public abstract class PipePair {
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
     * this.out > [ out.out | out.in > child > in.out | in.in ] > this.in
     *
     * Spawn is parameterized with the Object o, allowing custom
     * behavior.  The child must close to.out and from.in.
     */
    protected PipePair(String[] args) {
	Pipe out = new Pipe ();
	Pipe in = new Pipe ();
	// Wire: this.out > out.out
	this.out = out.out;
	// Wire: in.in > this.in
	this.in = in.in;
	// Wire: out.in > child > to.out
	pid = ProcessIdentifierFactory.create(spawn(args, in, out));
	// Close the child's end of the FDs.
	out.in.close();
	in.out.close();
    }

    /**
     * Called from the context of the child process.
     */
    abstract int spawn(String[] args, Pipe in, Pipe out);

    /**
     * Shut down the pipes.
     */
    public void close() {
	in.close ();
	out.close ();
    }

    public static PipePair daemon(String[] args) {
	return new PipePair(args) {
	    int spawn(String[] args, Pipe in, Pipe out) {
		return PipePair.daemon(args[0], args,
				       in.in.getFd(), in.out.getFd(),
				       out.in.getFd(), out.out.getFd());
	    }
	};
    }
    private static native int daemon(String exe, String[] args,
				     int in_in, int in_out,
				     int out_in, int out_out);

    public static PipePair child(String[] args) {
	return new PipePair(args) {
	    int spawn(String[] args, Pipe in, Pipe out) {
		return PipePair.child(args[0], args,
				      in.in.getFd(), in.out.getFd(),
				      out.in.getFd(), out.out.getFd());
	    }
	};
    }
    private static native int child(String exe, String[] args,
				     int in_in, int in_out,
				     int out_in, int out_out);
}
