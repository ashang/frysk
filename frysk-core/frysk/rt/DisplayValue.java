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

import java.text.ParseException;

import javax.naming.NameNotFoundException;

import frysk.debuginfo.DebugInfo;
import frysk.proc.Task;
import frysk.stack.Frame;
import frysk.stack.FrameIdentifier;
import frysk.stack.StackFactory;
import frysk.value.Value;

/**
 * The DisplayValue class is an intermediary between a Variable object and anything that
 * wishes to keep track of Variables. A Display is responsible for keeping track
 * of when a Variable is in or out of scope, and reloading the variable if it is
 * available upon a process becoming blocked. It is also responsible for keeping
 * track of the Variable should it switch from being in memory to being in a 
 * register
 *
 */
public class DisplayValue
{
  
  protected String varLabel;
  protected Task myTask;
  protected FrameIdentifier frameIdentifier;
  protected Value myVar;
  
  protected int num;
  
  /**
   * Creates a new DisplayValue object encompassing a variable from the
   * provided Task
   * @param name The name of the value to encapsulate
   * @param task The task to fetch updates from
   * @param fIdent The FrameIdentifier corresponding to the frame that
   *    the variable should be looked for in. 
   */
  public DisplayValue(String name, Task task, FrameIdentifier fIdent)
  {
    varLabel = name;
    myTask = task;
    frameIdentifier = fIdent;
    refresh();
  }
  
  /**
   * @return true iff the variable represented by this Display is in scope
   */
  public boolean isAvailable ()
  {
    return myVar != null;
  }
  
  /**
   * Updates the display to refect the new variable value
   */
  public void refresh()
  {
    
    Frame current = StackFactory.createFrame(myTask);
    // Work backwards through the frames
    // trying to find the one the value came from
    while(current != null)
      {
        if(current.getFrameIdentifier().equals(frameIdentifier))
          break;
        current = current.getOuter();
      }
    
    // If we couldn't find a matching frame, our variable is no longer available
    if(current == null)
      {
        myVar = null;
        return;
      }
    
    // We found the correct frame, now refresh the variable
    DebugInfo info = new DebugInfo(current);
    info.refresh(current);
    try
      {
        myVar = info.print(varLabel);
      }
    catch (NameNotFoundException e)
      {
//        e.printStackTrace();
        myVar = null;
      }
    catch (ParseException e)
      {
//        e.printStackTrace();
        myVar = null;
      }
  }
  
  /**
   * 
   * @return The Value object encapsulated by this Display.
   */
  public Value getValue()
  {
    return myVar;
  }

  public FrameIdentifier getFrameIdentifier ()
  {
    return frameIdentifier;
  }

  public Task getTask ()
  {
    return myTask;
  }

  public String getName ()
  {
    return varLabel;
  }
}
