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

package frysk.stack;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * A dewey decimal like notation to identify frame and sub-levels.
 */

public class FrameLevel {
    private final int[] decimals;

    /**
     * Inner most level.
     */
    public FrameLevel() {
	this.decimals = new int[1];
    }

    /**
     * Construct a frame level from the string.
     */
    public FrameLevel(String string) {
	LinkedList positions = new LinkedList();
	// convert to an array.
	decimals = new int[positions.size()];
	int p = 0;
	for (Iterator i = positions.iterator(); i.hasNext(); ) {
	    Integer d = (Integer)i.next();
	    decimals[p++] = d.intValue();
	}
    }

    /**
     * New sub-level.
     */
    private FrameLevel(int[] decimals) {
	this.decimals = decimals;
    }

    /**
     * Increment POSITION by one (discarding any less significant
     * positions).
     */
    public FrameLevel increment(int position) {
	int[] levels = new int[position + 1];
	for (int i = 0; i < levels.length; i++) {
	    if (i < decimals.length)
		levels[i] = decimals[i];
	}
	levels[position]++;
	return new FrameLevel(levels);
    }

    /**
     * Truncate the level to SIZE fields.  If SIZE is larger than the
     * current size, zero extend (yea, ok that isn' exactly a normal
     * truncate).
     */
    public FrameLevel truncate(int size) {
	int[] levels = new int[size];
	for (int i = 0; i < levels.length && i < decimals.length; i++) {
	    levels[i] = decimals[i];
	}
	return new FrameLevel(levels);
    }

    /**
     * Return the N't position.
     */
    public int position(int p) {
	if (p < decimals.length)
	    return decimals[p];
	else
	    return 0;
    }

    /**
     * Return the "size"
     */
    public int size() {
	return decimals.length;
    }

    /**
     * Convert the level to an array.
     */
    public int[] toArray() {
	return (int[]) (decimals.clone());
    }

    public void toPrint(PrintWriter writer) {
	writer.print(decimals[0]);
	for (int i = 1; i < decimals.length; i++) {
	    writer.print(".");
	    writer.print(decimals[i]);
	}
    }

    public String toString() {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	toPrint(writer);
	return stringWriter.toString();
    }
}
