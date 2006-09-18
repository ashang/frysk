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


package frysk.rt;

import frysk.dom.DOMFunction;
import frysk.dom.DOMSource;
import frysk.proc.Task;
import gnu.gcj.RawDataManaged;
import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.unwind.FrameCursor;

public class StackFrame
{

  private String methodName;

  private String sourceFile;

  private int lineNum;
  
  private int startLine;

  private int endLine;

  private int startOffsset;

  private int endOffset;

  private int column;

  private long address;

  protected StackFrame inner;

  protected StackFrame outer;

  private RawDataManaged unwind_data;
  
  private DOMFunction func;
  
  private DOMSource data;

  private Task myTask;

  public StackFrame (FrameCursor current, Task myTask)
  {
    this.myTask = myTask;
    unwind_data = current.getNativeCursor();
    initialize();

    if (address != 0)
      {
        Dwfl dwfl = new Dwfl(myTask.getTid());
        DwflLine line = dwfl.getSourceLine(address);
        if (line != null)
          {
            this.lineNum = line.getLineNum();
            this.startLine = this.lineNum;
            this.endLine = this.lineNum;
            this.startOffsset = 0;
            this.endOffset = -1;
            this.sourceFile = line.getSourceFile();
            this.column = line.getColumn();
          }
      }
    else
      {
        this.sourceFile = "<Unknown file>";
      }
  }
  
  public void setFunction(DOMFunction f)
  {
    this.func = f;
    if (f != null)
        setData(f.getSource());
    else
        setData(null);
  }
  
  public void setData(DOMSource s)
  {
    this.data = s;
  }
  
  public DOMFunction getFunction()
  {
    return this.func;
  }
  
  public DOMSource getData()
  {
    return this.data;
  }

  public String getMethodName ()
  {
    return methodName;
  }

  public String getSourceFile ()
  {
    return sourceFile;
  }

  public int getLineNumber ()
  {
    return lineNum;
  }

  public int getColumn ()
  {
    return column;
  }

  public long getAddress ()
  {
    return address;
  }

  public Task getMyTask ()
  {
    return myTask;
  }

  protected RawDataManaged getUnwindData ()
  {
    return unwind_data;
  }

  private native void initialize ();

  public StackFrame getInner ()
  {
    return inner;
  }

  public StackFrame getOuter ()
  {
    return outer;
  }
  
  public int getEndLine ()
  {
    return endLine;
  }
  
  public void setEndLine (int i )
  {
    this.endLine = i;
  }

  public int getEndOffset ()
  {
    return endOffset;
  }
  
  public int getStartLine ()
  {
    return startLine;
  }
  
  public void setStartLine (int i)
  {
    this.startLine = i;
  }

  public int getStartOffsset ()
  {
    return startOffsset;
  }
}
