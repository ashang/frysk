// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import frysk.sys.FileDescriptor;
import frysk.junit.TestCase;
import frysk.testbed.TearDownFile;


public class TestStatelessFile
    extends TestCase
{
    private final String st =
	new String ("Some sample datammmmmmmmmmmmmmmmmmmmmmmm");
    private String noSuchFile = "/no/such/file";
    private File fakefile = new File (noSuchFile);
    private long stlen = -1;
    private final byte[] stb = st.getBytes();
    
    public void testNoSuchFile ()
    {
	assertFalse ("noSuchFile exists", fakefile.exists());
    }

    public void testNullTermination ()
    {
	StatelessFile file = new StatelessFile (noSuchFile);
	assertEquals ("length fits NUL", noSuchFile.length() + 1,
		      file.unixPath.length);
	for (int i = 0; i < noSuchFile.length(); i++) {
	    assertEquals ("unixPath[" + i + "]", (byte) noSuchFile.charAt(i),
			  file.unixPath[i]);
	}
	assertEquals ("trailing NUL", 0,
		      file.unixPath[file.unixPath.length-1]);
    }

    public void testPreadValidity()
    {
	TearDownFile tmp = TearDownFile.create ();
	StatelessFile sf = new StatelessFile(tmp);
	StatelessFile sfbad = new StatelessFile(fakefile);
	final int BYTE_BUFFER_SIZE = 128;
	byte[] bytes = new byte[BYTE_BUFFER_SIZE];

	try {
	    sfbad.pread(0,		// long fileOffset,
		     bytes,	// byte[] bytes,
		     0,		// long start,
		     10);	// long length
	    assertTrue ("invalid file pread", false);
	} catch (Errno ioe) {
	    //	    System.out.println ("invalid file name");
	    assertTrue (true);
	}

	try {
	    sf.pread(-1,	// long fileOffset,
		     bytes,	// byte[] bytes,
		     0,		// long start,
		     10);	// long length
	    assertTrue ("invalid file offset pread", false);
	} catch (Errno ioe) {
	    //	    System.out.println ("invalid file offset");
	    assertTrue (true);
	}

	try {
	    sf.pread(0,		// long fileOffset,
		     bytes,	// byte[] bytes,
		     -1,	// long start,
		     10);	// long length
	    assertTrue ("invalid buffer start pread", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    //	    System.out.println ("invalid buffer start");
	    assertTrue (true);
	}

	try {
	    sf.pread(0,		// long fileOffset,
		     bytes,	// byte[] bytes,
		     0,		// long start,
		     -1);	// long length
	    assertTrue ("invalid length pread", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    //	    System.out.println ("invalid length");
	    assertTrue (true);
	}

	try {
	    sf.pread(0,		// long fileOffset,
		     bytes,	// byte[] bytes,
		     0,		// long start,
		     BYTE_BUFFER_SIZE+1);	// long length
	    assertTrue ("read overflow pread", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    //	    System.out.println ("read overflow");
	    assertTrue (true);
	}
	tmp.delete();
    }
    
    public void testPread()
    {
	long ilen;
	final byte[] stb = st.getBytes();
	TearDownFile tmp = TearDownFile.create ();
	try {
	    FileWriter fw = new FileWriter (tmp);
	    fw.write (st);
	    fw.close();
	    stlen = tmp.length();
	} catch (IOException ioe) {
	    System.out.println ("TestStatelessFile() "  + ioe);
	}

	byte[] bytes = new byte[(int)stlen];
	StatelessFile sf = new StatelessFile(tmp);

	// Read the entirety of the temp file created above and compare
	// the results with the expected string.
	try {
	    ilen = sf.pread(0,		// long fileOffset,
			    bytes,	// byte[] bytes,
			    0,		// long start,
			    tmp.length());	// long length
	    assertEquals ("initial length pread", stlen, ilen);
	    for (int i = 0; i < stlen; i++)
		assertEquals ("initial bytes", stb[i], bytes[i]);
	} catch (Errno ioe) {
	    System.out.println (">>>>>>>>>pread()<<<<<<<"  + ioe);
	}

	// Read 3 bytes from offset 8 in the file to offset 6 in the byte array,
	// overlaying the original characters.  Do the same operation to a copy
	// of the original string used to create the file.  Compare the results.
	// (There's probably a more erfficient way to extract bytes from a
	// StringBuffer than my way below, but I don't know what it is.  I'm a
	// C guy.
	final long fileOffset = 8;
	final long start      = 6;
	final long length     = 3;
	StringBuffer sb = new StringBuffer (st);
	sb.replace ((int)start, (int) (start + length),
		    st.substring ((int)fileOffset,
				  (int) (fileOffset + length)));
	String sbc = new String (sb);
	byte[] sbb = sbc.getBytes();
	try {
	    ilen = sf.pread(fileOffset,	// long fileOffset,
			    bytes,		// byte[] bytes,
			    start,		// long start,
			    length);	// long length
	    assertEquals ("sub-read length", length, ilen);
	    for (int i = 0; i < stlen; i++)
		assertEquals ("sub-read bytes", sbb[i], bytes[i]);
	} catch (Errno ioe) {
	    System.out.println (">>>>>>>>>pread()<<<<<<<"  + ioe);
	}
	
	tmp.delete();
    }
    
    public void testPwriteValidity()
    {
	File fakefile = new File (".", "*****");
	StatelessFile sfbad = new StatelessFile(fakefile);
	TearDownFile tmp = TearDownFile.create ();
	StatelessFile sf = new StatelessFile(tmp);

	final int BYTE_BUFFER_SIZE = 128;
	byte[] bytes = new byte[BYTE_BUFFER_SIZE];

	try {
	    sfbad.pwrite(0,	// long fileOffset,
			 bytes,	// byte[] bytes,
			 0,	// long start,
			 10);	// long length
	    assertTrue ("invalid file pread", false);
	} catch (Errno ioe) {
	    // System.out.println ("invalid file name");
	    assertTrue (true);
	}

	try {
	    sf.pwrite(-1,	// long fileOffset,
		      bytes,	// byte[] bytes,
		      0,	// long start,
		      10);	// long length
	    assertTrue ("invalid file offset pwrite", false);
	} catch (Errno ioe) {
	    // System.out.println ("invalid file offset");
	    assertTrue (true);
	}

	try {
	    sf.pwrite(0,	// long fileOffset,
		      bytes,	// byte[] bytes,
		      -1,	// long start,
		      10);	// long length
	    assertTrue ("invalid buffer start pwrite", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    //System.out.println ("invalid buffer start");
	    assertTrue (true);
	}

	try {
	    sf.pwrite(0,	// long fileOffset,
		      bytes,	// byte[] bytes,
		      0,	// long start,
		      -1);	// long length
	    assertTrue ("invalid length pwrite", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    // System.out.println ("invalid length");
	    assertTrue (true);
	}

	try {
	    sf.pwrite(0,	// long fileOffset,
		      bytes,	// byte[] bytes,
		      0,	// long start,
		      BYTE_BUFFER_SIZE+1);	// long length
	    assertTrue ("read overflow pwrite", false);
	} catch (ArrayIndexOutOfBoundsException ioe) {
	    // System.out.println ("read overflow");
	    assertTrue (true);
	}
	tmp.delete();
    }
    
    public void testPwriteHuge()
    {
	try {
	    long ilen;
	    //                        0123456789abcdef
	    final long HugeOffset = 0x000000007fffffffL;
	    File tmp = File.createTempFile("sftest", null);
	    FileDescriptor fd = new FileDescriptor(tmp, FileDescriptor.RDWR);
	    fd.lseek (HugeOffset);
	    StatelessFile sf = new StatelessFile(tmp);

	    // pwrite to the temp file created above
	    try {
		ilen = sf.pwrite(HugeOffset,		// long fileOffset,
				 stb,		// byte[] bytes,
				 0,		// long start,
				 stb.length);	// long length
		stlen = tmp.length();
		assertEquals ("initial length pwrite", stb.length, ilen);
	    } catch (Errno ioe) {
		System.out.println ("huge offsety pwrite()"  + ioe);
	    }
	    
	    tmp.delete();
	} catch (IOException ioe) {
	    System.out.println ("TestStatelessFile() "  + ioe);
	}
    }
    
    public void testPwrite()
    {
	//	final int BYTE_BUFFER_SIZE = 128;
	//byte[] bytes = new byte[BYTE_BUFFER_SIZE];

	long ilen;
	long stlen = -1;
	TearDownFile tmp = TearDownFile.create ();
	StatelessFile sf = new StatelessFile(tmp);
	char[] chars = new char[stb.length];

	// pwrite to the temp file created above
	try {
	    ilen = sf.pwrite(0,			// long fileOffset,
			     stb,		// byte[] bytes,
			     0,			// long start,
			     stb.length);	// long length
	    stlen = tmp.length();
	    assertEquals ("initial length pwrite", stb.length, ilen);
	} catch (Errno ioe) {
	    System.out.println (">>>>>>>>>pwrite()<<<<<<<"  + ioe);
	}
	
	try {
	    FileReader fr = new FileReader (tmp);
	    ilen = fr.read(chars, 0, (int)stlen);
	    fr.close();
	    assertEquals ("read length pwrite", stlen, ilen);
	    for (int i = 0; i < stlen; i++)
		assertEquals ("initial chars", st.charAt (i), chars[i]);
	} catch (IOException ioe) {
	    System.out.println ("TestStatelessFile() "  + ioe);
	}

	// Write 3 bytes from offset 8 in the source bytes to offset 6 in the 
	// file overlaying the original characters.  Do the same operation to a
	// copy of the original string used to create the file.  Compare the
	// results.
	final long fileOffset = 8;
	final long start      = 6;
	final long length     = 3;
	StringBuffer sb = new StringBuffer (st);
	sb.replace ((int)fileOffset, (int) (fileOffset + length),
		    st.substring ((int)start,
				  (int) (start + length)));
	byte[] scb = st.getBytes();
	String sbc = new String (sb);

	try {
	    ilen = sf.pwrite(fileOffset,	// long fileOffset,
			     scb,		// byte[] bytes,
			     start,		// long start,
			     length);	// long length
	    assertEquals ("sub-write length", length, ilen);
	    //	    for (int i = 0; i < stlen; i++)
	    //	assertEquals ("sub-read bytes", sbb[i], bytes[i]);
	} catch (Errno ioe) {
	    System.out.println (">>>>>>>>>pread()<<<<<<<"  + ioe);
	}
	
	try {
	    FileReader fr = new FileReader (tmp);
	    ilen = fr.read(chars, 0, (int)stlen);
	    fr.close();
	    assertEquals ("read length pwrite", stlen, ilen);
	    for (int i = 0; i < stlen; i++)
		assertEquals ("initial chars", sbc.charAt (i), chars[i]);
	} catch (IOException ioe) {
	    System.out.println ("TestStatelessFile() "  + ioe);
	}

	
	tmp.delete();
    }
}
