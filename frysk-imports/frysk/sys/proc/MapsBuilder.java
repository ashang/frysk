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

package frysk.sys.proc;

/**
 * The build the list of maps from the contents of the file
 * <tt>/proc/PID/maps</tt>.
 *
 * While this isn't a pure builder pattern, it is close enough.
 */
public abstract class MapsBuilder
{
    /**
     * Create a MapsBuilder; can only extend.
     */
    protected MapsBuilder ()
    {
    }

    /**
     * Scan the maps file found in <tt>/proc/PID/auxv</tt> building up
     * a list of memory maps.  Return true if the scan was successful.
     */
    public final native boolean construct (int pid);

    /**
     * Scan the maps MAPS byte array building corresponding map.  It
     * is assumed that the byte array contains ASCII characters, and
     * includes a terminating NUL.  {@link #buildMap} is called with
     * each mapping.
     */
    public final native boolean construct (byte[] maps);

    /**
     * Called with the raw byte buffer slurped by {@link
     * #construct(int)}.
     */
    abstract public void buildBuffer (byte[] maps);

    /**
     * Build an address map covering [addressLow,addressHigh) with
     * permissions {permR, permW, permX, permP }, device devMajor
     * devMinor, inode, and the pathname's offset/length within the
     * buf.
     *
     * !shared implies private, they are mutually exclusive.
     */
    abstract public void buildMap (long addressLow, long addressHigh,
				   boolean permRead, boolean permWrite,
				   boolean permExecute, boolean shared,
				   long offset,
				   int devMajor, int devMinor,
				   int inode,
				   int pathnameOffset, int pathnameLength);
}
