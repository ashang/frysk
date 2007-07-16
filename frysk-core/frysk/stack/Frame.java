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

package frysk.stack;

import lib.dwfl.DwTagEncodings;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import lib.unwind.Cursor;
import frysk.debuginfo.DebugInfo;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.rt.Line;
import frysk.rt.Subprogram;
import frysk.rt.Symbol;

public abstract class Frame
{
  protected Task task;
  protected Cursor cursor;
  
  private Subprogram subprogram;
  
  /**
   * Returns the program counter for this StackFrame.
   * 
   * @return The program counter for this StackFrame.
   */
  public abstract long getAddress ();
  
  /**
   * Returns the pre-call PC for this non-interrupted StackFrame.
   * 
   * @return The pre-call program counter for this StackFrame.
   */
  public abstract long getAdjustedAddress ();

  /**
   * Returns the Task this StackFrame belongs to.
   * 
   * @return The Task this StackFrame belongs to.
   */
  public final Task getTask ()
  {
    return task;
  }

  /**
   * Returns this StackFrame's inner frame.
   * 
   * @return This StackFrame's inner frame.
   */
  public abstract Frame getInner ();

  /**
   * Returns this StackFrame's outer frame.
   * 
   * @return This StackFrame's outer frame.
   */
  public abstract Frame getOuter ();
  
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
    StringBuffer builder = new StringBuffer("0x");
    String addr = Long.toHexString(getAddress());
    int padding = 2 * task.getIsa().getWordSize() - addr.length();
    for (int i = 0; i < padding; ++i)
      builder.append('0');
    builder.append(addr);

    // Print the symbol, if known append ().
    Symbol symbol = getSymbol();
    builder.append(" in ");
    builder.append(symbol.getDemangledName());
    if (symbol != Symbol.UNKNOWN)
      builder.append(" ()");
    
    return builder.toString();
  }
  public abstract long getReg(long reg);
  
  /**
   * Returns the Cannonical Frame Address of this StackFrame. Used in
   * conjunction with methodName to provide a unique identifier.
   * 
   * @return cfa    The cannonical frame address of this StackFrame
   */
  public abstract long getCFA();
  
  public abstract long setReg(long reg, long val);
  
    /**
     * Return this frame's FrameIdentifier.
     */
  public abstract FrameIdentifier getFrameIdentifier ();

    /**
     * Return this frame's symbol; UNKNOWN if there is no symbol.
     */
  public abstract Symbol getSymbol ();

    /**
     * Return this frame's list of lines as an array; returns an empty array if
     * there is no line number information available. The lack of line-number
     * information can be determined with the test: <<tt>>.getLines().length == 0</tt>.
     * XXX: When there are multiple lines, it isn't clear if there is a well
     * defined ordering of the information; for instance: outer-to-inner or
     * inner-to-outer.
     */
  public abstract Line[] getLines ();

  public final Subprogram getSubprogram ()
  {
    if (subprogram == null) {
      DebugInfo debugInfo = new DebugInfo(this);
      
      Dwfl dwfl = DwflCache.getDwfl(this.getTask());
      DwflDieBias bias = dwfl.getDie(getAdjustedAddress());

      if (bias != null) {

	DwarfDie[] scopes = bias.die.getScopes(getAdjustedAddress());
	
	for (int i = 0; i < scopes.length; i++) {
	  if (scopes[i].getTag() == DwTagEncodings.DW_TAG_subprogram_) {
	    subprogram = new Subprogram(scopes[i], debugInfo);
	    break;
	  }

	}
      }
      this.setSubprogram(subprogram);
    }

    return subprogram;
  }

    public final void setSubprogram (Subprogram subprogram)
    {
      this.subprogram = subprogram;
    }
    
}
