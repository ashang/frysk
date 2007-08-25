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
import java.util.Map;
import java.util.HashMap;
import frysk.sys.proc.MapsBuilder;

class MemoryMapping
{
  public File path;
  public long addressLow;
  public long addressHigh;
  public boolean permRead;
  public boolean permWrite;
  public boolean permExecute;

  public MemoryMapping(File path, long addressLow, long addressHigh,
		       boolean permRead, boolean permWrite, boolean permExecute)
  {
    this.path = path;
    this.addressLow = addressLow;
    this.addressHigh = addressHigh;
    this.permRead = permRead;
    this.permWrite = permWrite;
    this.permExecute = permExecute;
  }

  /**
   * Return a map of address space of process with given `pid'.  The
   * map is constructed in such a way that consecutive mappings of the
   * same file are merged, with their flags or-ed.
   */
  public static Map buildForPid (int pid)
  {
    class MyMapsBuilder
      extends MapsBuilder
    {
      private byte[] buf;
      private Map mappedFiles = new HashMap();
      public void buildBuffer (byte[] buf) { this.buf = buf; }

      public void buildMap (long addressLow, long addressHigh,
			    boolean permRead, boolean permWrite,
			    boolean permExecute, boolean shared,
			    long offset,
			    int devMajor, int devMinor,
			    int inode,
			    int pathnameOffset, int pathnameLength)
      {
	String path = new String(this.buf, pathnameOffset, pathnameLength);
	if (path.length () > 0 && path.charAt(0) != '[')
	  {
	    if (path.charAt(0) != '/')
	      throw new AssertionError("Unexpected: first character of path in map is neither '[', nor '/'.");
	    MemoryMapping info = (MemoryMapping)mappedFiles.get(path);
	    if (info != null)
	      {
		if (info.addressLow == addressHigh
		    || info.addressHigh == addressLow)
		  {
		    if (addressHigh > info.addressHigh)
		      info.addressHigh = addressHigh;
		    if (addressLow < info.addressLow)
		      info.addressLow = addressLow;
		    if (permRead) info.permRead = true;
		    if (permWrite) info.permWrite = true;
		    if (permExecute) info.permExecute = true;
		  }
		else
		  throw new AssertionError("Non-continuous mapping.");
	      }
	    else
	      mappedFiles.put(path, new MemoryMapping(new File(path), addressLow, addressHigh,
						      permRead, permWrite, permExecute));
	  }
      }
    }

    MyMapsBuilder mappings = new MyMapsBuilder();
    mappings.construct(pid);
    return mappings.mappedFiles;
  }
}
