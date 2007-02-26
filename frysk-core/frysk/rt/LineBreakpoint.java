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


package frysk.rt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import lib.dw.Dwfl;
import lib.dw.DwflLine;

import frysk.proc.Task;

public class LineBreakpoint
{
  private String fileName;
  private int lineNumber;
  private int column;
  private LinkedList dwflAddrs;
  private LinkedList breakpoints;
  static private Logger logger;
    
  public LineBreakpoint () 
  {
    breakpoints = new LinkedList();
  }

  public LineBreakpoint(Task task, String fileName, int lineNumber, int column) 
  {
    this();
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.column = column;
    Dwfl dwfl = new Dwfl(task.getTid());
    dwflAddrs = dwfl.getLineAddresses(fileName, lineNumber, column);
    if (logger == null)
      logger = LogManager.getLogManager().getLogger("frysk");
    if (logger != null && logger.isLoggable(Level.FINEST))
      {
	Iterator iterator = dwflAddrs.iterator();
	int i;
	for (i = 0; iterator.hasNext(); i++)
	  {
	    logger.logp(Level.FINEST, "LineBreakpoint", "LineBreakpoint",
			"dwfl[" + i + "]: {0}", iterator.next());
	  }
      }
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
    
  public void addBreakpoint(RunState runState, Task task)
  {
    DwflLine line = (DwflLine)dwflAddrs.getFirst();
    long address = line.getAddress();
	  
    RunState.PersistentBreakpoint breakpoint
      = runState.new PersistentBreakpoint(address);
    breakpoints.add(breakpoint);
    runState.addPersistentBreakpoint(task, breakpoint);
  }

  public void deleteBreakpoint(RunState runState, Task task)
  {
    RunState.PersistentBreakpoint bpt
      = (RunState.PersistentBreakpoint)breakpoints.getFirst();
    breakpoints.removeFirst();
    runState.deletePersistentBreakpoint(task, bpt);
  }

  public static LineBreakpoint addLineBreakpoint(RunState runState, Task task,
						 String filename,
						 int lineNumber)
  {
    LineBreakpoint bpt = new LineBreakpoint(task, filename, lineNumber, 0);
    bpt.addBreakpoint(runState, task);
    return bpt;
  }    
}
