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

package frysk.ftrace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import frysk.sys.proc.MapsBuilder;

class MemoryMapping
{
    public File path;
    public List parts; //List<Part>

    static class Part
    {
	public long addressLow;
	public long addressHigh;
	public long offset;
	public boolean permRead;
	public boolean permWrite;
	public boolean permExecute;

	public Part(long addressLow, long addressHigh, long offset,
		    boolean permRead, boolean permWrite, boolean permExecute)
	{
	    this.addressLow = addressLow;
	    this.addressHigh = addressHigh;
	    this.offset = offset;
	    this.permRead = permRead;
	    this.permWrite = permWrite;
	    this.permExecute = permExecute;
	}

	public boolean equals(Object other)
	{
	    if (!(other instanceof Part))
		return false;
	    Part p = (Part)other;
	    return addressLow == p.addressLow
		&& addressHigh == p.addressHigh
		&& permRead == p.permRead
		&& permWrite == p.permWrite
		&& permExecute == p.permExecute;
	}

	public String toString()
	{
	    return "<0x" + Long.toHexString(addressLow)
		+ "..0x" + Long.toHexString(addressHigh)
		+ ": offset=0x" + Long.toHexString(offset) + "; perm=`"
		+ (permRead ? 'r' : '-')
		+ (permWrite ? 'w' : '-')
		+ (permExecute ? 'x' : '-')
		+ "'>";
	}
    }

    public MemoryMapping(File path)
    {
	this.path = path;
	this.parts = new ArrayList();
    }

    public void addPart(Part part)
    {
	this.parts.add(part);
    }

    public List lookupParts(long offset)
    {
	List ret = new ArrayList();
	for (Iterator it = this.parts.iterator(); it.hasNext(); ) {
	    Part part = (Part)it.next();
	    if (offset >= part.offset
		&& offset < (part.offset + part.addressHigh - part.addressLow))
		    ret.add(part);
	}
	return ret;
    }

  public boolean equals(Object other)
  {
    if (!(other instanceof MemoryMapping))
      return false;
    MemoryMapping m = (MemoryMapping)other;
    return path.equals(m.path)
	&& parts.equals(m.parts);
  }

  public int hashCode()
  {
    return path.hashCode();
  }

  /**
   * Return a map of address space of process with given `pid'.  The
   * returned map is Map&lt;Path, MemoryMapping&gt;.
   */
  public static Map buildForPid (int pid)
  {
    class MyMapsBuilder
      extends MapsBuilder
    {
      private byte[] buf;
      public Map mappings = new HashMap();

      public void buildBuffer(byte[] buf)
      {
	this.buf = buf;
      }

      public void buildMap(long addressLow, long addressHigh,
			   boolean permRead, boolean permWrite,
			   boolean permExecute, boolean shared,
			   long offset,
			   int devMajor, int devMinor,
			   int inode,
			   int pathnameOffset, int pathnameLength)
      {
	  // Ignore empty mappings.  Probably a VDSO or something.
	  if (pathnameLength == 0
	      || this.buf[pathnameOffset] == '[')
	      return;

	  String path = new String(this.buf, pathnameOffset, pathnameLength);
	  if (path.charAt(0) != '/')
	      throw new AssertionError("Unexpected: first character of path in map is neither '[', nor '/'.");

	  File file = new File(path);
	  MemoryMapping mapping = (MemoryMapping)mappings.get(file);
	  if (mapping == null) {
	      mapping = new MemoryMapping(file);
	      mappings.put(file, mapping);
	  }
	  Part part = new Part(addressLow, addressHigh, offset,
			       permRead, permWrite, permExecute);
	  mapping.addPart(part);
      }
    }

    MyMapsBuilder builder = new MyMapsBuilder();
    builder.construct(pid);
    return builder.mappings;
  }
}
