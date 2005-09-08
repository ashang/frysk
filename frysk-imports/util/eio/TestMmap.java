// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import junit.framework.TestCase;

public class TestMmap
    extends TestCase
{
    public void testMmap ()
    {
	File file;
	PrintWriter tmpFile;
	try {
	    file = File.createTempFile ("TestMmap.", ".tmp");
	    file.deleteOnExit ();
	    tmpFile = new PrintWriter (new FileWriter (file));
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
	String line = "A really long line that goes for at least ten (i.e., one-zero) characters";
	tmpFile.println (line);
	tmpFile.flush ();

	ByteBuffer map = new MmapByteBuffer (file.getPath ());
	assertNotNull ("Opening " + file.getPath (), map);

	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test get(): Character " + i + " wrong",
			  line.charAt (i), map.get ());
	}
	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test get(i):Character " + i + " wrong",
			  line.charAt (i),
			  map.get (0 + i));
	}

	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test 10+get(): Character " + i + " wrong",
			  line.charAt (10 + i), map.get ());
	}
	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test get(10+i): Character " + i + " wrong",
			  line.charAt (10 + i), map.get (10 + i));

	}

	ByteBuffer slice = map.slice (20, 30);
	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test slice 20..30: Character " + i + " wrong",
			  line.charAt (20 + i), slice.get ());
	}
	for (int i = 0; i < 10; i++) {
	    assertEquals ("Test slice 20..30 (i): Character " + i + " wrong",
			  line.charAt (20 + i), slice.get (i));
	}
    }
}

