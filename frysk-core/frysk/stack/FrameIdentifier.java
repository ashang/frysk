// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

public class FrameIdentifier
{
  private long functionAddress;
  private long cfa;
  
  /**
   * Create a new FrameIdentifier. Identifies a stack frame based on 
   * its canonical frame address and function starting address.
   * 
   * @param functionAddress The address of the start of the frame
   * @param cfa The canonical frame address of the stack frame
   */
  public FrameIdentifier (long functionAddress, long cfa)
  {
    this.functionAddress = functionAddress;
    this.cfa = cfa;
  }
  
  /**
   * Returns true if this FrameIdentifier is inner to the parameter.
   * 
   * @param fi  The FrameIdentifier to compare to
   * @return   true If this is inner to fi
   */
  public boolean innerTo (FrameIdentifier fi)
  {
    return this.cfa < fi.getCfa();
  }
  
  /**
   * Returns true if this FrameIdentifier is outer to the parameter.
   * 
   * @param fi  The FrameIdentifier to compare to
   * @return   true If this is outer to fi
   */
  public boolean outerTo (FrameIdentifier fi)
  {
    return this.cfa > fi.getCfa();
  }
  
  /**
   * Compares this FrameIdentifier to the parameter and returns true if 
   * both the cfa and function address match.
   * 
   * It is important to use the function address instead of the stack frame's
   * current address because that address may change in the lifetime of
   * a stack frame, and thus this method could potentially return false if
   * one of the frames had been stepped and the other was not. Using the
   * frame start address guarantees that if two StackFrames are compared,
   * and they both represent the same frame on the stack, this method will
   * return true regardless of the state of either frame.
   * 
   * @param fi  The FrameIdentifier to compare to.
   * @return true   If the cfa and function address belonging to this
   * FrameIdentifier match the cfa and function address in the parameter.
   */
  public boolean equals (Object fi)
  {
    if (! (fi instanceof FrameIdentifier))
      return false;
    
    FrameIdentifier rhs = (FrameIdentifier) fi;
    return (this.cfa == rhs.cfa && this.functionAddress == rhs.functionAddress);
  }

  /**
   * Returns the canonical frame address from this FrameIdentifier
   * 
   * @return cfa    The canonical frame address of the StackFrame
   * represented by this FrameIdentifier
   */
  public long getCfa ()
  {
    return this.cfa;
  }

  /**
   * Returns the function address from this FrameIdentifier
   * 
   * @return functionAddress    The start address of the frame
   * represented by this FrameIdentifier
   */
  public long getFunctionAddress ()
  {
    return this.functionAddress;
  }
  
  /**
   * Returns a hashCode for this object
   */
  public int hashCode ()
  {
    return (int) (this.cfa ^ this.functionAddress);
  }
  
  /**
   * Displays customized String output.
   */
  public String toString ()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("{");
    buffer.append(super.toString());
    buffer.append(",functionAddress=");
    buffer.append(this.functionAddress);
    buffer.append(",cfa=");
    buffer.append(this.cfa);
    buffer.append("}");
    
    return buffer.toString();
  }
  
}
