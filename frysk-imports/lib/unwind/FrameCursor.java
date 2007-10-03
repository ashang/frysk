// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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


package lib.unwind;

import lib.stdcpp.Demangler;

import gnu.gcj.RawDataManaged;

public class FrameCursor
{

  private RawDataManaged nativeCursor;

  protected FrameCursor inner;

  protected FrameCursor outer;
  
  protected int signal_frame;
  
  protected String demangledMethodName;
  protected String procName;
  private long procOffset;
  private long procInfoStart;
  private long procInfoEnd;
  
  private long cfa;
  
  private long address;

  protected FrameCursor (long cursor)
  {
    create_frame_cursor(cursor);
    this.demangledMethodName = Demangler.demangle(this.procName);
  }

  /**
   * 
   * @return The stack frame cursor from the next inner stack
   */
  public FrameCursor getInner ()
  {
    return inner;
  }

  /**
   * 
   * @return The stack frame cursor from the previous outer stack
   */
  public FrameCursor getOuter ()
  {
    return outer;
  }

  /**
   * 
   * @return The raw pointer to this frame's unwind cursor
   */
  public RawDataManaged getNativeCursor ()
  {
    return nativeCursor;
  }
  
  public int getIsSignalFrame()
  {
    return signal_frame;
  }

  private native void create_frame_cursor (long cursor);

  public long getAddress ()
  {
    return address;
  }

  public long getCfa ()
  {
    return cfa;
  }

  public boolean isSignalFrame()
  {
    // ??? Is this right?
    return signal_frame == 1;
  }
  
  public void setIsSignalFrame(boolean isSignalFrame)
  {
    signal_frame = isSignalFrame? 1 : 0;
  }

  public String getMethodName ()
  {
    return demangledMethodName;
  }
  
  public String getProcName ()
  {
    return procName;
  }
  public long getProcOffset ()
  {
    return procOffset;
  }
  public long getProcInfoStart ()
  {
    return procInfoStart;
  }
  public long getProcInfoEnd ()
  {
    return procInfoEnd;
  }

  
  public native long get_reg (long reg);
  public native long set_reg (long reg, long val);
}
