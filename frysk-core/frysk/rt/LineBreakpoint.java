// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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


package frysk.rt;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.ListIterator;
import frysk.rsl.Log;
import java.io.File;
import lib.dwfl.DwflLine;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;

public class LineBreakpoint extends SourceBreakpoint {
    private static final Log fine = Log.fine(LineBreakpoint.class);

  private String fileName;
  private int lineNumber;
  private int column;
    
    public LineBreakpoint(int id, File fileName,
			  int lineNumber, int column) {
	this(id, fileName.getPath(), lineNumber, column);
    }

  public LineBreakpoint(int id, String fileName, int lineNumber, int column) 
  {
    super(id);
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.column = column;
  }

  public String getFileName() 
  {
    return fileName;
  }
    
  public int getLineNumber() 
  {
    return lineNumber;
  }
    
  public int getColumn() 
  {
    return column;
  }
    
  public String toString() 
  {
    return "breakpoint file " + getFileName() + " line " + getLineNumber() 
      + " column " + getColumn();
  }

  public long getRawAddress(Object addr)
  {
    return ((Long)addr).longValue();
  }

  public LinkedList getBreakpointRawAddresses(Task task) {
      fine.log(this, "getBreakpointRawAddresses task", task, "...");
      LinkedList dies
          = DwflCache.getDwfl(task)
	  .getLineAddresses(fileName, lineNumber, column);
      LinkedList result = new LinkedList();
      ListIterator iterator = dies.listIterator();
      while (iterator.hasNext()) {
          result.add(new Long(((DwflLine)iterator.next()).getAddress()));
      }
      
      fine.log(this, "getBreakpointRawAddresses ... returns", result);
      return result;
  }

  public PrintWriter output(PrintWriter writer)
  {
    writer.print("#");
    writer.print(getFileName());
    writer.print("#");
    writer.print(getLineNumber());
    return writer;
  }
}
