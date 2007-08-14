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

package frysk.value;

import java.io.PrintWriter;

/**
 * Formats a base type sending it to the printer.
 */

public class Format
{
    /**
     * Report that formattting for Type is unknown.
     */
    public void printUnknown(PrintWriter writer, Type type) {
	writer.print("<<format for ");
	type.toPrint(writer);
	writer.print(" unknown>>");
    }

    /**
     * Report that formatting of TYPE of SIZE is unknown.
     */
    protected void printUnknown(PrintWriter writer, String type,
				long size) {
	writer.print("<<format for ");
	writer.print(size);
	writer.print(" byte ");
	writer.print(type);
	writer.print(" unknown>>");
    }
    /**
     * Print VAL as a signed integer.
     *
     * FIXME: Should take a BigInt.
     */
    public void printInteger(PrintWriter writer, Location location) {
	long value;
	long size = location.getByteBuffer().capacity();
	switch ((int)size) {
	case 1:
	    value = location.getByte();
	    break;
	case 2:
 	    value = location.getShort();
	    break;
	case 4:
	    value = location.getInt();
	    break;
	case 8:
	    value = location.getLong();
	    break;
	default:
	    printUnknown(writer, "integer", size);
	    return;
	}
	writer.print(value);
    }

    /**
     * Print VAL as a floating-point.
     *
     * FIXME: Should take a BigFloat.
     */
    public void printFloatingPoint(PrintWriter writer, Location location) {
	long size = location.getByteBuffer().capacity();
	switch((int) size) {
	case 4:
	    writer.print(location.getFloat());
	    break;
	case 8:
	    writer.print(location.getDouble());
	    break;
	default:
	    printUnknown(writer, "floating-point", size);
	    break;
	}
    }

    public void printPointer(PrintWriter writer, Location location) {
	long value;
	long size = location.getByteBuffer().capacity();
	switch ((int)size) {
	case 1:
	    value = location.getByte();
	    break;
	case 2:
	    value = location.getShort();
	    break;
	case 4:
	    value = location.getInt();
	    break;
	case 8:
	    value = location.getLong();
	    break;
	default:
	    printUnknown(writer, "integer", size);
	    return;
	}
	writer.print("0x");
	writer.print(Long.toHexString(value));
    }

    public static final Format NATURAL = new Format();
    public static final Format HEXADECIMAL = new Format();
    public static final Format OCTAL = new Format();
    public static final Format DECIMAL = new Format();
    public static final Format BINARY = new Format();
}
