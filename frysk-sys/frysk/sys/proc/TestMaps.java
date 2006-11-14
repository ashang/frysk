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

import frysk.junit.TestCase;

/**
 * Test the MapsBuilder against a predefined set of <tt>maps</tt>
 * buffers.
 */
public class TestMaps
    extends TestCase
{
    static final boolean T = true;
    static final boolean F = false;
    /**
     * An unpacked map.
     */
    private class Map
    {
	long addressLow;
	long addressHigh;
	boolean permRead;
	boolean permWrite;
	boolean permExecute;
	boolean permPrivate;
	long offset;
	int devMajor;
	int devMinor;
	int inode;
	String pathname;
	Map (long addressLow, long addressHigh,
	     boolean permRead, boolean permWrite,
	     boolean permExecute, boolean permPrivate,
	     long offset,
	     int devMajor, int devMinor,
	     int inode,
	     String pathname)
	{
	    this.addressLow = addressLow;
	    this.addressHigh = addressHigh;
	    this.permRead = permRead;
	    this.permWrite = permWrite;
	    this.permExecute = permExecute;
	    this.permPrivate = permPrivate;
	    this.offset = offset;
	    this.devMajor = devMajor;
	    this.devMinor = devMinor;
	    this.inode = inode;
	    this.pathname = pathname;
	}
    }
    private void check (String[] strings, Map[] map)
    {
	class Verify
	    extends MapsBuilder
	{
	    private byte[] buf;
	    private Map[] map;
	    Verify (byte[] buf, Map[] map)
	    {
		this.buf = buf;
		this.map = map;
	    }
	    public void buildBuffer (byte[] buf)
	    {
		// Discard.
	    }
	    int index;
	    public void buildMap (long addressLow, long addressHigh,
				  boolean permRead, boolean permWrite,
				  boolean permExecute, boolean permPrivate,
				  long offset,
				  int devMajor, int devMinor,
				  int inode,
				  int pathnameOffset, int pathnameLength)
	    {
		assertTrue ("map within bounds", index < map.length);
		assertEquals (index + ".addressLow", map[index].addressLow, addressLow);
		assertEquals (index + ".addressHigh", map[index].addressHigh, addressHigh);
		assertEquals (index + ".permRead", map[index].permRead, permRead);
		assertEquals (index + ".permWrite", map[index].permWrite, permWrite);
		assertEquals (index + ".permExecute", map[index].permExecute, permExecute);
		assertEquals (index + ".permPrivate", map[index].permPrivate, permPrivate);
		assertEquals (index + ".offset", map[index].offset, offset);
		assertEquals (index + ".devMajor", map[index].devMajor, devMajor);
		assertEquals (index + ".devMinor", map[index].devMinor, devMinor);
		assertEquals (index + ".inode", map[index].inode, inode);
		String pathname;
		if (pathnameLength == 0)
		    pathname = null;
		else
		    pathname = new String (buf, pathnameOffset, pathnameLength);
		assertEquals (index + ".pathname", map[index].pathname, pathname);
		index++;
	    }
	}
	byte[] buf = TestLib.stringsToBytes (strings);
	Verify verify = new Verify (buf, map);
	assertTrue ("construct's return value", verify.construct (buf));
	assertEquals ("number of entries", verify.index, map.length);
    }
    /**
     * Check that an IA-32 map can be parsed.
     */
    public void testIA32 ()
    {
	check (new String[] {
		   "00113000-0012d000 r-xp 00000000 03:08 163689     /lib/ld-2.3.5.so\n",
		   "0012d000-0012e000 r-xp 00019000 03:08 163689     /lib/ld-2.3.5.so\n",
		   "0012e000-0012f000 rwxp 0001a000 03:08 163689     /lib/ld-2.3.5.so\n",
		   "00131000-00254000 r-xp 00000000 03:08 163690     /lib/libc-2.3.5.so\n",
		   "00254000-00256000 r-xp 00123000 03:08 163690     /lib/libc-2.3.5.so\n",
		   "00256000-00258000 rwxp 00125000 03:08 163690     /lib/libc-2.3.5.so\n",
		   "00258000-0025a000 rwxp 00258000 00:00 0\n",
		   "00283000-00285000 r-xp 00000000 03:08 163692     /lib/libdl-2.3.5.so\n",
		   "00285000-00286000 r-xp 00001000 03:08 163692     /lib/libdl-2.3.5.so\n",
		   "00286000-00287000 rwxp 00002000 03:08 163692     /lib/libdl-2.3.5.so\n",
		   "0029e000-002a1000 r-xp 00000000 03:08 161263     /lib/libtermcap.so.2.0.8\n",
		   "002a1000-002a2000 rwxp 00002000 03:08 161263     /lib/libtermcap.so.2.0.8\n",
		   "006a4000-006ad000 r-xp 00000000 03:08 159573     /lib/libnss_files-2.3.5.so\n",
		   "006ad000-006ae000 r-xp 00008000 03:08 159573     /lib/libnss_files-2.3.5.so\n",
		   "006ae000-006af000 rwxp 00009000 03:08 159573     /lib/libnss_files-2.3.5.so\n",
		   "00c7b000-00c7c000 r-xp 00c7b000 00:00 0\n",
		   "08047000-080e9000 r-xp 00000000 03:08 127707     /bin/bash\n",
		   "080e9000-080ef000 rw-p 000a1000 03:08 127707     /bin/bash\n",
		   "080ef000-080f4000 rw-p 080ef000 00:00 0\n",
		   "094c4000-094e5000 rw-p 094c4000 00:00 0          [heap]\n",
		   "b7d17000-b7d19000 rw-p b7d17000 00:00 0\n",
		   "b7d19000-b7d1f000 r--s 00000000 03:05 1847915    /usr/lib/gconv/gconv-modules.cache\n",
		   "b7d1f000-b7f1f000 r--p 00000000 03:05 1785869    /usr/lib/locale/locale-archive\n",
		   "b7f1f000-b7f20000 rw-p b7f1f000 00:00 0\n",
		   "b7f30000-b7f31000 rw-p b7f30000 00:00 0\n",
		   "bfc1b000-bfc31000 rw-p bfc1b000 00:00 0          [stack]\n"
	       },
	       new Map[] {
		   new Map (0x00113000L,0x0012d000L, T,F,T,T, 0x00000000L, 0x03,0x08, 163689, "/lib/ld-2.3.5.so"),
		   new Map (0x0012d000L,0x0012e000L, T,F,T,T, 0x00019000L, 0x03,0x08, 163689, "/lib/ld-2.3.5.so"),
		   new Map (0x0012e000L,0x0012f000L, T,T,T,T, 0x0001a000L, 0x03,0x08, 163689, "/lib/ld-2.3.5.so"),
		   new Map (0x00131000L,0x00254000L, T,F,T,T, 0x00000000L, 0x03,0x08, 163690, "/lib/libc-2.3.5.so"),
		   new Map (0x00254000L,0x00256000L, T,F,T,T, 0x00123000L, 0x03,0x08, 163690, "/lib/libc-2.3.5.so"),
		   new Map (0x00256000L,0x00258000L, T,T,T,T, 0x00125000L, 0x03,0x08, 163690, "/lib/libc-2.3.5.so"),
		   new Map (0x00258000L,0x0025a000L, T,T,T,T, 0x00258000L, 0x00,0x00, 0x0, null),
		   new Map (0x00283000L,0x00285000L, T,F,T,T, 0x00000000L, 0x03,0x08, 163692, "/lib/libdl-2.3.5.so"),
		   new Map (0x00285000L,0x00286000L, T,F,T,T, 0x00001000L, 0x03,0x08, 163692, "/lib/libdl-2.3.5.so"),
		   new Map (0x00286000L,0x00287000L, T,T,T,T, 0x00002000L, 0x03,0x08, 163692, "/lib/libdl-2.3.5.so"),
		   new Map (0x0029e000L,0x002a1000L, T,F,T,T, 0x00000000L, 0x03,0x08, 161263, "/lib/libtermcap.so.2.0.8"),
		   new Map (0x002a1000L,0x002a2000L, T,T,T,T, 0x00002000L, 0x03,0x08, 161263, "/lib/libtermcap.so.2.0.8"),
		   new Map (0x006a4000L,0x006ad000L, T,F,T,T, 0x00000000L, 0x03,0x08, 159573, "/lib/libnss_files-2.3.5.so"),
		   new Map (0x006ad000L,0x006ae000L, T,F,T,T, 0x00008000L, 0x03,0x08, 159573, "/lib/libnss_files-2.3.5.so"),
		   new Map (0x006ae000L,0x006af000L, T,T,T,T, 0x00009000L, 0x03,0x08, 159573, "/lib/libnss_files-2.3.5.so"),
		   new Map (0x00c7b000L,0x00c7c000L, T,F,T,T, 0x00c7b000L, 0x00,0x00, 0x0, null),
		   new Map (0x08047000L,0x080e9000L, T,F,T,T, 0x00000000L, 0x03,0x08, 127707, "/bin/bash"),
		   new Map (0x080e9000L,0x080ef000L, T,T,F,T, 0x000a1000L, 0x03,0x08, 127707, "/bin/bash"),
		   new Map (0x080ef000L,0x080f4000L, T,T,F,T, 0x080ef000L, 0x00,0x00, 0x0, null),
		   new Map (0x094c4000L,0x094e5000L, T,T,F,T, 0x094c4000L, 0x00,0x00, 0x0, "[heap]"),
		   new Map (0xb7d17000L,0xb7d19000L, T,T,F,T, 0xb7d17000L, 0x00,0x00, 0x0, null),
		   new Map (0xb7d19000L,0xb7d1f000L, T,F,F,F, 0x00000000L, 0x03,0x05, 1847915, "/usr/lib/gconv/gconv-modules.cache"),
		   new Map (0xb7d1f000L,0xb7f1f000L, T,F,F,T, 0x00000000L, 0x03,0x05, 1785869, "/usr/lib/locale/locale-archive"),
		   new Map (0xb7f1f000L,0xb7f20000L, T,T,F,T, 0xb7f1f000L, 0x00,0x00, 0x0, null),
		   new Map (0xb7f30000L,0xb7f31000L, T,T,F,T, 0xb7f30000L, 0x00,0x00, 0x0 , null),
		   new Map (0xbfc1b000L,0xbfc31000L, T,T,F,T, 0xbfc1b000L, 0x00,0x00, 0x0, "[stack]")
	       });
    }
    /**
     * Check that an IA-64 map can be parsed.
     */
    public void testIA64 ()
    {
	check (new String[] {
		   "00000000-00004000 r--p 00000000 00:00 0 \n",
		   "2000000000000000-2000000000030000 r-xp 00000000 08:02 5406726            /lib/ld-2.3.4.so\n",
		   "200000000003c000-2000000000044000 rw-p 0002c000 08:02 5406726            /lib/ld-2.3.4.so\n",
		   "2000000000058000-20000000002a8000 r-xp 00000000 08:02 5406767            /lib/tls/libc-2.3.4.so\n",
		   "20000000002a8000-20000000002b4000 ---p 00250000 08:02 5406767            /lib/tls/libc-2.3.4.so\n",
		   "20000000002b4000-20000000002c0000 rw-p 0024c000 08:02 5406767            /lib/tls/libc-2.3.4.so\n",
		   "20000000002c0000-20000000002cc000 rw-p 20000000002c0000 00:00 0 \n",
		   "4000000000000000-4000000000008000 r-xp 00000000 08:02 4636706            /bin/cat\n",
		   "6000000000004000-6000000000008000 rw-p 00004000 08:02 4636706            /bin/cat\n",
		   "6000000000008000-6000000000030000 rw-p 6000000000008000 00:00 0 \n",
		   "60000fff7fffc000-60000fff80000000 rw-p 60000fff7fffc000 00:00 0 \n",
		   "60000fffffff8000-60000fffffffc000 rw-p 60000fffffff8000 00:00 0 \n",
		   "a000000000000000-a000000000020000 ---p 00000000 00:00 0 \n"
	       },
	       new Map[] {
		   new Map (0x00000000L,0x00004000L, T,F,F,T, 0x00000000L, 0x00,0x00, 0, null),
		   new Map (0x2000000000000000L,0x2000000000030000L, T,F,T,T, 0x00000000L, 0x08,0x02, 5406726, "/lib/ld-2.3.4.so"),
		   new Map (0x200000000003c000L,0x2000000000044000L, T,T,F,T, 0x0002c000L, 0x08,0x02, 5406726, "/lib/ld-2.3.4.so"),
		   new Map (0x2000000000058000L,0x20000000002a8000L, T,F,T,T, 0x00000000L, 0x08,0x02, 5406767, "/lib/tls/libc-2.3.4.so"),
		   new Map (0x20000000002a8000L,0x20000000002b4000L, F,F,F,T, 0x00250000L, 0x08,0x02, 5406767, "/lib/tls/libc-2.3.4.so"),
		   new Map (0x20000000002b4000L,0x20000000002c0000L, T,T,F,T, 0x0024c000L, 0x08,0x02, 5406767, "/lib/tls/libc-2.3.4.so"),
		   new Map (0x20000000002c0000L,0x20000000002cc000L, T,T,F,T, 0x20000000002c0000L, 0x00,0x00, 0, null),
		   new Map (0x4000000000000000L,0x4000000000008000L, T,F,T,T, 0x00000000L, 0x08,0x02, 4636706, "/bin/cat"),
		   new Map (0x6000000000004000L,0x6000000000008000L, T,T,F,T, 0x00004000L, 0x08,0x02, 4636706, "/bin/cat"),
		   new Map (0x6000000000008000L,0x6000000000030000L, T,T,F,T, 0x6000000000008000L, 0x00,0x00, 0, null),
		   new Map (0x60000fff7fffc000L,0x60000fff80000000L, T,T,F,T, 0x60000fff7fffc000L, 0x00,0x00, 0, null),
		   new Map (0x60000fffffff8000L,0x60000fffffffc000L, T,T,F,T, 0x60000fffffff8000L, 0x00,0x00, 0, null),
		   new Map (0xa000000000000000L,0xa000000000020000L, F,F,F,T, 0x00000000L, 0x00,0x00, 0, null),
	       });
    }
    /**
     * Verify an amd64 memory map.
     */
    public void testAMD64 ()
    {
	check (new String[] {
		   "00400000-00405000 r-xp 00000000 03:03 8683581                            /bin/cat\n",
		   "00504000-00505000 rw-p 00004000 03:03 8683581                            /bin/cat\n",
		   "00505000-00526000 rw-p 00505000 00:00 0                                  [heap]\n",
		   "3369700000-336971a000 r-xp 00000000 03:03 3178727                        /lib64/ld-2.3.5.so\n",
		   "3369819000-336981a000 r--p 00019000 03:03 3178727                        /lib64/ld-2.3.5.so\n",
		   "336981a000-336981b000 rw-p 0001a000 03:03 3178727                        /lib64/ld-2.3.5.so\n",
		   "3369900000-3369a2e000 r-xp 00000000 03:03 3178728                        /lib64/libc-2.3.5.so\n",
		   "3369a2e000-3369b2d000 ---p 0012e000 03:03 3178728                        /lib64/libc-2.3.5.so\n",
		   "3369b2d000-3369b31000 r--p 0012d000 03:03 3178728                        /lib64/libc-2.3.5.so\n",
		   "3369b31000-3369b33000 rw-p 00131000 03:03 3178728                        /lib64/libc-2.3.5.so\n",
		   "3369b33000-3369b37000 rw-p 3369b33000 00:00 0 \n",
		   "2aaaaaaab000-2aaaaaaac000 rw-p 2aaaaaaab000 00:00 0 \n",
		   "2aaaaaac7000-2aaaaaac9000 rw-p 2aaaaaac7000 00:00 0 \n",
		   "7fffff8e2000-7fffff8f8000 rw-p 7fffff8e2000 00:00 0                      [stack]\n",
		   "ffffffffff600000-ffffffffffe00000 ---p 00000000 00:00 0                  [vdso]\n"
	       },
	       new Map[] {
		   new Map (0x00400000L,0x00405000L, T,F,T,T, 0x00000000L, 0x03,0x03, 8683581, "/bin/cat"),
		   new Map (0x00504000L,0x00505000L, T,T,F,T, 0x00004000L, 0x03,0x03, 8683581, "/bin/cat"),
		   new Map (0x00505000L,0x00526000L, T,T,F,T, 0x00505000L, 0x00,0x00, 0, "[heap]"),
		   new Map (0x3369700000L,0x336971a000L, T,F,T,T, 0x00000000L, 0x03,0x03, 3178727, "/lib64/ld-2.3.5.so"),
		   new Map (0x3369819000L,0x336981a000L, T,F,F,T, 0x00019000L, 0x03,0x03, 3178727, "/lib64/ld-2.3.5.so"),
		   new Map (0x336981a000L,0x336981b000L, T,T,F,T, 0x0001a000L, 0x03,0x03, 3178727, "/lib64/ld-2.3.5.so"),
		   new Map (0x3369900000L,0x3369a2e000L, T,F,T,T, 0x00000000L, 0x03,0x03, 3178728, "/lib64/libc-2.3.5.so"),
		   new Map (0x3369a2e000L,0x3369b2d000L, F,F,F,T, 0x0012e000L, 0x03,0x03, 3178728, "/lib64/libc-2.3.5.so"),
		   new Map (0x3369b2d000L,0x3369b31000L, T,F,F,T, 0x0012d000L, 0x03,0x03, 3178728, "/lib64/libc-2.3.5.so"),
		   new Map (0x3369b31000L,0x3369b33000L, T,T,F,T, 0x00131000L, 0x03,0x03, 3178728, "/lib64/libc-2.3.5.so"),
		   new Map (0x3369b33000L,0x3369b37000L, T,T,F,T, 0x3369b33000L, 0x00,0x00, 0, null),
		   new Map (0x2aaaaaaab000L,0x2aaaaaaac000L, T,T,F,T, 0x2aaaaaaab000L, 0x00,0x00, 0, null),
		   new Map (0x2aaaaaac7000L,0x2aaaaaac9000L, T,T,F,T, 0x2aaaaaac7000L, 0x00,0x00, 0, null),
		   new Map (0x7fffff8e2000L,0x7fffff8f8000L, T,T,F,T, 0x7fffff8e2000L, 0x00,0x00, 0, "[stack]"),
		   new Map (0xffffffffff600000L,0xffffffffffe00000L, F,F,F,T, 0x00000000L, 0x00,0x00, 0, "[vdso]")
	       });
    }
    /**
     * Verify a 32-bit PowerPC map.
     */
    public void testPPC32 ()
    {
	check (new String[] {
		   "00100000-00102000 r-xp 00100000 00:00 0 \n",
		   "0fe50000-0ff94000 r-xp 00000000 fd:00 38862877                           /lib/libc-2.3.5.so\n",
		   "0ff94000-0ffa3000 ---p 00144000 fd:00 38862877                           /lib/libc-2.3.5.so\n",
		   "0ffa3000-0ffa5000 r--p 00143000 fd:00 38862877                           /lib/libc-2.3.5.so\n",
		   "0ffa5000-0ffa9000 rwxp 00145000 fd:00 38862877                           /lib/libc-2.3.5.so\n",
		   "0ffa9000-0ffab000 rwxp 0ffa9000 00:00 0 \n",
		   "0ffd0000-0ffed000 r-xp 00000000 fd:00 38862875                           /lib/ld-2.3.5.so\n",
		   "0fffc000-0fffd000 r--p 0001c000 fd:00 38862875                           /lib/ld-2.3.5.so\n",
		   "0fffd000-0fffe000 rwxp 0001d000 fd:00 38862875                           /lib/ld-2.3.5.so\n",
		   "10000000-10005000 r-xp 00000000 fd:00 9338908                            /bin/cat\n",
		   "10014000-10015000 rwxp 00004000 fd:00 9338908                            /bin/cat\n",
		   "10015000-10036000 rwxp 10015000 00:00 0                                  [heap]\n",
		   "f7fcc000-f7fcd000 rw-p f7fcc000 00:00 0 \n",
		   "f7ffe000-f7fff000 rw-p f7ffe000 00:00 0 \n",
		   "ff87f000-ff894000 rw-p ff87f000 00:00 0                                  [stack]\n"
	       },
	       new Map[] {
		   new Map (0x00100000L,0x00102000L, T,F,T,T, 0x00100000L, 0x00,0x00, 0, null),
		   new Map (0x0fe50000L,0x0ff94000L, T,F,T,T, 0x00000000L, 0xfd,0x00, 38862877, "/lib/libc-2.3.5.so"),
		   new Map (0x0ff94000L,0x0ffa3000L, F,F,F,T, 0x00144000L, 0xfd,0x00, 38862877, "/lib/libc-2.3.5.so"),
		   new Map (0x0ffa3000L,0x0ffa5000L, T,F,F,T, 0x00143000L, 0xfd,0x00, 38862877, "/lib/libc-2.3.5.so"),
		   new Map (0x0ffa5000L,0x0ffa9000L, T,T,T,T, 0x00145000L, 0xfd,0x00, 38862877, "/lib/libc-2.3.5.so"),
		   new Map (0x0ffa9000L,0x0ffab000L, T,T,T,T, 0x0ffa9000L, 0x00,0x00, 0, null),
		   new Map (0x0ffd0000L,0x0ffed000L, T,F,T,T, 0x00000000L, 0xfd,0x00, 38862875, "/lib/ld-2.3.5.so"),
		   new Map (0x0fffc000L,0x0fffd000L, T,F,F,T, 0x0001c000L, 0xfd,0x00, 38862875, "/lib/ld-2.3.5.so"),
		   new Map (0x0fffd000L,0x0fffe000L, T,T,T,T, 0x0001d000L, 0xfd,0x00, 38862875, "/lib/ld-2.3.5.so"),
		   new Map (0x10000000L,0x10005000L, T,F,T,T, 0x00000000L, 0xfd,0x00, 9338908, "/bin/cat"),
		   new Map (0x10014000L,0x10015000L, T,T,T,T, 0x00004000L, 0xfd,0x00, 9338908, "/bin/cat"),
		   new Map (0x10015000L,0x10036000L, T,T,T,T, 0x10015000L, 0x00,0x00, 0, "[heap]"),
		   new Map (0xf7fcc000L,0xf7fcd000L, T,T,F,T, 0xf7fcc000L, 0x00,0x00, 0, null),
		   new Map (0xf7ffe000L,0xf7fff000L, T,T,F,T, 0xf7ffe000L, 0x00,0x00, 0, null),
		   new Map (0xff87f000L,0xff894000L, T,T,F,T, 0xff87f000L, 0x00,0x00, 0, "[stack]"),
	       });
    }
    /**
     * Verify that a 64-bit PowerPC map works.
     */
    public void testPPC64 ()
    {
	check (new String[] {
		   "00100000-00102000 r-xp 00100000 00:00 0 \n",
		   "10000000-10001000 r-xp 00000000 00:14 27689030                           /home/cagney/tmp/a.out\n",
		   "10010000-10011000 rw-p 00000000 00:14 27689030                           /home/cagney/tmp/a.out\n",
		   "8000000000-8000001000 rw-p 8000000000 00:00 0 \n",
		   "8000032000-8000034000 rw-p 8000032000 00:00 0 \n",
		   "80b4840000-80b4868000 r-xp 00000000 fd:00 121962513                      /lib64/ld-2.3.5.so\n",
		   "80b4877000-80b4878000 r--p 00027000 fd:00 121962513                      /lib64/ld-2.3.5.so\n",
		   "80b4878000-80b487b000 rw-p 00028000 fd:00 121962513                      /lib64/ld-2.3.5.so\n",
		   "80b4880000-80b49e7000 r-xp 00000000 fd:00 121962523                      /lib64/libc-2.3.5.so\n",
		   "80b49e7000-80b49f7000 ---p 00167000 fd:00 121962523                      /lib64/libc-2.3.5.so\n",
		   "80b49f7000-80b49fa000 r--p 00167000 fd:00 121962523                      /lib64/libc-2.3.5.so\n",
		   "80b49fa000-80b4a0a000 rw-p 0016a000 fd:00 121962523                      /lib64/libc-2.3.5.so\n",
		   "80b4a0a000-80b4a0e000 rw-p 80b4a0a000 00:00 0 \n",
		   "1fffffb7000-1fffffcc000 rw-p 1fffffb7000 00:00 0                         [stack]\n"
	       },
	       new Map[] {
		   new Map (0x00100000L,0x00102000L, T,F,T,T, 0x00100000L, 0x00,0x00, 0, null),
		   new Map (0x10000000L,0x10001000L, T,F,T,T, 0x00000000L, 0x00,0x14, 27689030, "/home/cagney/tmp/a.out"),
		   new Map (0x10010000L,0x10011000L, T,T,F,T, 0x00000000L, 0x00,0x14, 27689030, "/home/cagney/tmp/a.out"),
		   new Map (0x8000000000L,0x8000001000L, T,T,F,T, 0x8000000000L, 0x00,0x00, 0, null),
		   new Map (0x8000032000L,0x8000034000L, T,T,F,T, 0x8000032000L, 0x00,0x00, 0, null),
		   new Map (0x80b4840000L,0x80b4868000L, T,F,T,T, 0x00000000L, 0xfd,0x00, 121962513, "/lib64/ld-2.3.5.so"),
		   new Map (0x80b4877000L,0x80b4878000L, T,F,F,T, 0x00027000L, 0xfd,0x00, 121962513, "/lib64/ld-2.3.5.so"),
		   new Map (0x80b4878000L,0x80b487b000L, T,T,F,T, 0x00028000L, 0xfd,0x00, 121962513, "/lib64/ld-2.3.5.so"),
		   new Map (0x80b4880000L,0x80b49e7000L, T,F,T,T, 0x00000000L, 0xfd,0x00, 121962523, "/lib64/libc-2.3.5.so"),
		   new Map (0x80b49e7000L,0x80b49f7000L, F,F,F,T, 0x00167000L, 0xfd,0x00, 121962523, "/lib64/libc-2.3.5.so"),
		   new Map (0x80b49f7000L,0x80b49fa000L, T,F,F,T, 0x00167000L, 0xfd,0x00, 121962523, "/lib64/libc-2.3.5.so"),
		   new Map (0x80b49fa000L,0x80b4a0a000L, T,T,F,T, 0x0016a000L, 0xfd,0x00, 121962523, "/lib64/libc-2.3.5.so"),
		   new Map (0x80b4a0a000L,0x80b4a0e000L, T,T,F,T, 0x80b4a0a000L, 0x00,0x00, 0, null),
		   new Map (0x1fffffb7000L,0x1fffffcc000L, T,T,F,T, 0x1fffffb7000L, 0x00,0x00, 0, "[stack]"),
	       });
    }
}
