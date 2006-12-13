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
import frysk.proc.TaskException;
import gnu.gcj.RawDataManaged;
import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.stdcpp.Demangler;
import lib.unwind.FrameCursor;

public class StackFrame
{

  private String methodName;

  private String sourceFile;

  private int lineNum;
  
  private int startLine;

  private int endLine;

  private int startOffset;

  private int endOffset;

  private int column;

  private long address;

  protected StackFrame inner;

  protected StackFrame outer;

  private RawDataManaged unwind_data;
  
  private DOMFunction func;
  
  private DOMSource data;
  
  private DwflLine dwflLine;

  private Task task;
  
  protected long cfa;
  
  private boolean isSignalFrame = false;
  
  /**
   * Create a new StackFrame without knowing the inner frame ahead of time.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param task  The Task this StackFrame belongs to.
   */
  public StackFrame (FrameCursor current, Task myTask)
  {
    this(current, myTask, null);
  }

  /**
   * Create a new StackFrame, knowing the inner frame ahead of time. Should
   * be called to create all StackFrames except the innermost. Based on whether
   * or not the inner frame is null tells us that this frame is the innermost 
   * frame. With this as a condition, or having this frame a 'signal frame' via
   * an interrupt, determines whether or not the address sent to the Dwfl
   * object is decremented.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param task  The Task this StackFrame belongs to.
   * @param inner   This StackFrame's inner StackFrame.
   */
  public StackFrame (FrameCursor current, Task myTask, StackFrame inner)
  {
    this.task = myTask;
    unwind_data = current.getNativeCursor();
    initialize();
    this.inner = inner;
    this.methodName = Demangler.demangle(this.methodName);

    if (address != 0) /* We were able to pull information from this cursor */
      {
        Dwfl dwfl = new Dwfl(myTask.getTid());
        DwflLine line = null;
        
        /* The innermost frame and frames which were interrupted during
         * execution use their PC to get the line in source. All other 
         * frames have their PC set to the line after the inner frame call
         * and must be decremented by one. */
        if (inner == null || isSignalFrame)
          line = dwfl.getSourceLine(address);
        else
          line = dwfl.getSourceLine(address - 1);
        
        if (line != null)
          {
            this.lineNum = line.getLineNum();
            this.startLine = this.lineNum;
            this.endLine = this.lineNum;
            this.startOffset = 0;
            this.endOffset = -1;
            this.sourceFile = line.getSourceFile();
            this.column = line.getColumn();
          }
        this.dwflLine = line;
      }
  }
  
  /**
   * Native function. Sets methodName and address via the cursor obtained
   * from unwind_data.
   */
  private native void initialize ();
  
  /**
   * Sets the DOMFunction representing the function which is in turn
   * represented by this StackFrame.  This method also calls
   * {@link setData} using the function's source as an argument; if
   * the function does not have a source, then null is used.
   * 
   * @param f   The DOMFunction for this StackFrame.
   */
  public void setDOMFunction(DOMFunction f)
  {
    this.func = f;
    if (f != null)
        setData(f.getSource());
    else
        setData(null);
  }
  
  /**
   * Sets the DOMSource, representing the source code of the
   * executable.
   * 
   * @param s   The DOMSource for the executable this StackFrame belongs to.
   */
  public void setData(DOMSource s)
  {
    this.data = s;
  }
  
  /**
   * Returns this StackFrame's function.  If no function was
   * previously set using {@link setFunction}, this will return null.
   * 
   * @return This StackFrame's function.
   */
  public DOMFunction getDOMFunction()
  {
    return this.func;
  }
  
  /**
   * Returns this StackFrame's source code.  If no source was
   * previously set using {@link setData}, null is returned.
   * 
   * @return This StackFrame's source code.
   */
  public DOMSource getData()
  {
    return this.data;
  }

  /**
   * Return the name of the function associated with this stack frame.
   * This will return null if the function's name is not known.
   */
  public String getMethodName ()
  {
    return methodName;
  }

  /**
   * Return the name of the source file associated with this stack
   * frame.  If the source file is not known, this will return null.
   */
  public String getSourceFile ()
  {
    return sourceFile;
  }

  /**
   * Return the line number of the source code associated with this
   * stack frame.  Line numbers begin at 1.  If the source line number
   * is not known, this will return 0.
   */
  public int getLineNumber ()
  {
    return lineNum;
  }

  /**
   * Returns the column in the currently executing line in the source file.
   * 
   * @return The column in the currently executing source file line. 
   */
  public int getColumn ()
  {
    return column;
  }

  /**
   * Returns the program counter for this StackFrame.
   * 
   * @return The program counter for this StackFrame.
   */
  public long getAddress ()
  {
    return address;
  }

  /**
   * Returns the Task this StackFrame belongs to.
   * 
   * @return The Task this StackFrame belongs to.
   */
  public Task getTask ()
  {
    return task;
  }

  /**
   * Returns the RawDataManaged  which represents this StackFrame's cursor.
   * 
   * @return This StackFrame's cursor.
   */
  protected RawDataManaged getUnwindData ()
  {
    return unwind_data;
  }

  /**
   * Returns this StackFrame's inner frame.
   * 
   * @return This StackFrame's inner frame.
   */
  public StackFrame getInner ()
  {
    return inner;
  }

  /**
   * Returns this StackFrame's outer frame.
   * 
   * @return This StackFrame's outer frame.
   */
  public StackFrame getOuter ()
  {
    return outer;
  }

  /**
   * Return a string representation of this stack frame.
   * The returned string is suitable for display to the user.
   */
  public String frameToString ()
  {
    System.out.println("In Custom tostring");
    StringBuffer builder = new StringBuffer("0x");
    String addr = Long.toHexString(getAddress());
    
    // Pad the address based on the task's word size.
    try
      {
        int padding = 2 * task.getIsa().getWordSize() - addr.length();
        for (int i = 0; i < padding; ++i)
          builder.append('0');
      }
    catch (TaskException _)
      {
        // We couldn't get the task's ISA. But, we don't care, since
        // all it means is that we can't properly pad the address.
      }
    
    builder.append(addr);
    String mn = getMethodName();
    
    if (mn != null && ! "".equals(mn))
      {
        builder.append(" in function: ");
        builder.append(getMethodName());
      }
    
    String sf = getSourceFile();
    int line = getLineNumber();
    
    if (sf != null || line != 0)
      {
        builder.append(" (");
        if (sf != null)
          builder.append(sf);
        else
          builder.append("Unknown source");
        
        if (line != 0)
          {
            builder.append(":");
            builder.append(line);
          }
        
        builder.append(")");
      }
    
    return builder.toString();
  }
  
  /**
   * Return a simple string representation of this stack frame.
   */
  public String toPrint (boolean isSourceWindow)
  {
    String ret = "";
    if (this.dwflLine != null)
      {
        if (! isSourceWindow)
          {
            ret = "0x"
              + Long.toHexString(this.address) + " in "
              + this.methodName + " () from: "
              + this.sourceFile + "#" + this.lineNum;
          }
        else
          {
            String[] fileName = this.sourceFile.split("/");
            ret = "0x" + Long.toHexString(this.address) + " in "
                  + fileName[fileName.length - 1] + " " + this.methodName 
                  + " (): line #" + this.lineNum;
          }
      }
    else
      {
        ret = "0x"
          + Long.toHexString(this.address) + " in "
          + this.methodName + " ()";
      }
    
    return ret;
  }
  
  public int getEndLine ()
  {
    return endLine;
  }
  
  public void setEndLine (int i)
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

  public int getStartOffset ()
  {
    return startOffset;
  }
  
  /**
   * Return the DwflLine Object for this StackFrame. If this is null, it is
   * an excellent indication that this frame has no debuginfo.
   * 
   * @return    dwflLine    The DwflLine Object for this StackFrame
   */
  public DwflLine getDwflLine ()
  {
    return this.dwflLine;
  }
  
  /**
   * Sets whether or not this frame is a signal frame - meaning it has been
   * sent an interrupt, and we should not decrement its address when trying
   * to get its line number from the Dwfl object.
   * 
   * @param sigFrame    Whether or not this frame is a 'signal frame.'
   */
  public void setIsSignalFrame(boolean sigFrame)
  {
    this.isSignalFrame = sigFrame;
  }
  
  /**
   * Returns whether or not this frame is a signal frame.
   * 
   * @return Whether or not this frame is a signal frame.
   */
  public boolean getIsSignalFrame()
  {
    return this.isSignalFrame;
  }
  
  public long getReg(long reg)
  {
    // ??? Use something akin to Register interface?
    return get_reg(reg);
  }
  
  /**
   * Returns the Cannonical Frame Address of this StackFrame. Used in
   * conjunction with methodName to provide a unique identifier.
   * 
   * @return cfa    The cannonical frame address of this StackFrame
   */
  public long getCFA()
  {
    return this.cfa;
  }
  
  public long setReg(long reg, long val)
  {
    // ??? Use something akin to Register interface?
    return set_reg(reg, val);
  }
  
  private native long get_reg (long reg);
  private native long set_reg (long reg, long val);
}

