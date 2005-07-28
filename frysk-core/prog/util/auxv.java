package prog.util;

/** Print the auxilary vector found in the specified file.
 *
 */

import util.PrintWriter;
import util.eio.*;
import com.redhat.fedora.frysk.proc.Auxv;
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
