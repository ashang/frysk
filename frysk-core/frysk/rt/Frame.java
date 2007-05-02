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

import java.io.File;

import frysk.proc.Task;

import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.unwind.Cursor;
import lib.unwind.ProcInfo;
import lib.unwind.ProcName;

public class Frame
{  
  protected Cursor cursor;
  private Task task;
  
  private Symbol symbol;
  private Line[] lines;
  
  private FrameIdentifier frameIdentifier;
  private Subprogram subprogram;
  
  Frame inner = null;
  Frame outer = null;
   
  Frame (Cursor cursor, Task task)
  {
    this.cursor = cursor;
    this.task = task;
  }
  
  public Frame getOuter()
  {
    Cursor newCursor = this.cursor.unwind();
    if (newCursor != null) 
      {
      outer = new Frame(newCursor, task);
      outer.inner = this;
      }
    return outer;
  }
  
  public String getProcName()
  {
    return cursor.getProcName().getName();
  }
  
  public String getProcName(int maxNameSize)
  {
    return cursor.getProcName(maxNameSize).getName();
  }
  
  public ProcInfo getProcInfo()
  {
	  return cursor.getProcInfo();
  }
  
  public long getAddress()
  {
    ProcInfo myInfo = cursor.getProcInfo();
    ProcName myName = cursor.getProcName(0);
    
    if (myInfo.getError() != 0 || myName.getError() != 0)
      	return 0;
    
    System.err.println("StartIP: " + Long.toHexString(myInfo.getStartIP()));
    System.err.println("Offset: " + Long.toHexString(myName.getOffset()));
    return myInfo.getStartIP() + myName.getOffset();
  }
  
  public long getAdjustedAddress()
  {
    if (this.inner != null && !this.cursor.isSignalFrame())
      return getAddress() - 1;
    else
      return getAddress();
  }
  
  public long getRegister(int regNum)
  {
    byte[] word = new byte[task.getIsa().getWordSize()];
    if (cursor.getRegister(regNum, word) < 0)
      return 0;
    return byteArrayToLong(word);
  }
  
  public long getRegister (long regNum)
  {
    return getRegister((int) regNum);
  }
  
  public Task getTask()
  {
    return task;
  }
  public long byteArrayToLong(byte[] word)

  {
    long val = 0;
    for (int i = 0; i < word.length; i++)
      val = val << 8 | (word[i] & 0xff);
    return val;
  }
  
  public long getCFA()
  {
    byte[] word = new byte[task.getIsa().getWordSize()];
    if (cursor.getSP(word) < 0)
      return 0;
    return byteArrayToLong(word);
  }
  
  /**
   * Return this frame's FrameIdentifier.
   */
  public FrameIdentifier getFrameIdentifier ()
  {
    if (this.frameIdentifier != null)
      {
        ProcInfo myInfo = getProcInfo();
        this.frameIdentifier = new FrameIdentifier(myInfo.getStartIP(),
                                          getCFA());
      }
    return this.frameIdentifier;
  }
  
  public int setRegister(int regNum, long word)
  {
    return cursor.setRegister(regNum, word);
  }
  
  public long setRegister (long regNum, long word)
  {
    return (long) setRegister ((int) regNum, word);
  }
  
  public boolean isSignalFrame()
  {
    return cursor.isSignalFrame();
  }
  
  /**
   * Return a simple string representation of this stack frame.
   * The returned string is suitable for display to the user.
   */
  public String toPrint (boolean name)
  {
     // XXX: There is always an inner cursor.
      if (this.cursor == null)
	  return "Empty stack trace";
    
     // Pad the address based on the task's word size.
      StringBuffer builder = new StringBuffer ("0x");
      String addr = Long.toHexString (getAddress());
      int padding = 2 * task.getIsa().getWordSize() - addr.length();
      for (int i = 0; i < padding; ++i)
	  builder.append('0');
      builder.append(addr);

       // Print the symbol, if known append ().
      Symbol symbol = getSymbol ();
      builder.append(" in ");
      builder.append (symbol.getDemangledName ());
      if (symbol != Symbol.UNKNOWN)
	  builder.append (" ()");

       // If there's line number information append that.
      Line[] lines = getLines ();
      for (int i = 0; i < lines.length; i++) {
	  Line line = lines[i];
	  builder.append (" from: ");
	  if (name) {
	      builder.append (line.getFile ().getName ());
	      builder.append (": line #");
	      builder.append (line.getLine ());
	  }
	  else {
	      builder.append(line.getFile ().getPath ());
	      builder.append ("#");
	      builder.append (line.getLine ());
	  }
      }
      System.err.println("Printed line info");
      return builder.toString();
  }
  
  /**
   * Return this frame's symbol; UNKNOWN if there is no symbol.
   */
public Symbol getSymbol ()
{
  if (this.symbol == null)
    {
      String mangledName = cursor.getProcName().getName();
      if (mangledName == null)
        this.symbol = Symbol.UNKNOWN;
      else
        {
          long address = getAddress() - cursor.getProcName().getOffset();
          this.symbol = new Symbol(address, mangledName);
        }
    }
  return this.symbol;
}

/**
 * Return this frame's list of lines as an array; returns an empty array if
 * there is no line number information available. The lack of line-number
 * information can be determined with the test: <<tt>>.getLines().length == 0</tt>.
 * XXX: When there are multiple lines, it isn't clear if there is a well
 * defined ordering of the information; for instance: outer-to-inner or
 * inner-to-outer.
 */
public Line[] getLines ()
{
if (this.lines == null)
  {
    if (this.cursor != null)
      {
        Dwfl dwfl = new Dwfl(this.task.getTid());
        // The innermost frame and frames which were
        // interrupted during execution use their PC to get
        // the line in source. All other frames have their PC
        // set to the line after the inner frame call and must
        // be decremented by one.
        DwflLine dwflLine = dwfl.getSourceLine(getAdjustedAddress());
        if (dwflLine != null)
          {
            File f = new File (dwflLine.getSourceFile());
            if (!f.isAbsolute())
              {
                /* The file refers to a path relative to the compilation
                 * directory; so prepend the path to that directory in
                 * front of it. */
                File parent = new File(dwflLine.getCompilationDir());
                f = new File (parent, dwflLine.getSourceFile());
              }
            
            this.lines = new Line[] { new Line(f, dwflLine.getLineNum(),
                                                  dwflLine.getColumn(),
                                                  this.task.getProc()) };
          }
        
      }
    // If the fetch failed, mark it as unknown.
    if (this.lines == null)
      this.lines = new Line[0];
  }
return this.lines;
}

public Subprogram getSubprogram ()
{
  return subprogram;
}

public void setSubprogram (Subprogram subprogram)
{
  this.subprogram = subprogram;
}
}
