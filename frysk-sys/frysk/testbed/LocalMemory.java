// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

/**
 * Provide access to known local areas of memory.
 *
 * Most useful by doing a fork to create a mirror image of the address
 * space that can then be manipulated using ptrace calls.
 */

public class LocalMemory {
    /**
     * Returns the address of a bunch of data.
     */
    public static native long getDataAddr();

    /**
     * Returns a copy of SIZE data bytes starting at getDataAddr.
     */
    public static native byte[] getDataBytes();

    /**
     * Returns the name of the code function.
     */
    public static native String getCodeName();

    /**
     * Returns the address of a function.
     */
    public static native long getCodeAddr();

    /*
     * Returns a copy of SIZE instruction bytes starting at
     * getCodeAddr().
     */
    public static native byte[] getCodeBytes();

    /**
     * Returns the line number of a function.
     */
    public static native int getCodeLine ();

    /**
     * Returns the file-name of a function.
     */
    public static native String getCodeFile ();

    /**
     * Callback or builder describing the constructed stack.
     */
    public interface StackBuilder {
	void stack(long addr, byte[] contents);
    }
    /**
     * Allocate a bit of stack, with known content, and pass it back
     * to the client.  Since stack is in "high" memory, this provides
     * an address in higher memory.  For instance, on the i386, ADDR
     * will be so large that it is negative.
     */
    public static native void constructStack(StackBuilder builder);
}
