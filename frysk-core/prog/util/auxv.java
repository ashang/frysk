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

/**
 * Print the auxilary vector found in the specified file.
 */

package prog.util;

import inua.eio.ByteBuffer;
import inua.eio.ArrayByteBuffer;
import inua.PrintWriter;
import frysk.proc.Auxv;
import java.io.File;
import java.io.FileInputStream;

class auxv
{
    public static void main (String[] args)
    {
	PrintWriter out = new PrintWriter (System.out, true);

	if (args.length != 1) {
	    out.println ("Usage: accu/auxv <auxv>");
	    return;
	}


	ByteBuffer b;
	try {
	    byte[] buf = new byte[1024];
	    FileInputStream auxvFile = new FileInputStream (new File (args[0]));
	    int len = auxvFile.read (buf);
	    b = new ArrayByteBuffer (buf, 0, len);
	}
	catch (java.io.FileNotFoundException e) {
	    out.println ("File: " + args[0] + " not found");
	    return;
	}
	catch (java.io.IOException e) {
	    out.println ("Error reading file " + args[0]);
	    return;
	}

	Auxv[] vec = Auxv.parse (b);
	for (int i = 0; i < vec.length; i++) {
	    vec[i].print (out);
	}
    }
}
