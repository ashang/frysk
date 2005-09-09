// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
package prog.util;

/** Print the auxilary vector found in the specified file.
 *
 */

import util.PrintWriter;
import util.eio.*;
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
