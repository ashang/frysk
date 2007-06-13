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
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import frysk.proc.Task;
import frysk.stack.FrameIdentifier;

/**
 * An UpdatingDisplayValue is nearly identical to a DisplayValue, except that
 * an UpdatingDisplayValue is provided with additional information that
 * enables it to automatically refresh itself whenever the task given to it
 * changes state.
 * 
 * Objects can be notified of updates by using the addObserver method.
 *
 */
public class UpdatingDisplayValue
    extends DisplayValue
{

  private SteppingEngine engine;
  private List observers;
  
  /**
   * Crate a new UpdatingDisplayValue
   * @param name The name of the variable to track
   * @param task The task from which to track the variable
   * @param fIdent The frame identifier corresponding to the scope from
   *            which the variable should be read
   * @param eng The stepping engine to monitor for changes in execution
   *            state
   */
  public UpdatingDisplayValue (String name, Task task, 
                               FrameIdentifier fIdent, SteppingEngine eng)
  {
    super(name, task, fIdent);
    engine = eng;
    
    if(engine.getSteppingObserver() == null)
      engine.addProc(task.getProc());
    
    engine.addObserver(new LockObserver());
    
    observers = new LinkedList();
  }
  
  public void refresh()
  { 
    // Do nothing if the task isn't stopped
    if(myTask.getBlockers().length == 0)
      return;
    
    super.refresh();
    
    // hear ye! hear ye!
    if(observers != null) // (but only if there's someone to listen)
      notifyObservers();
  }
  
  /**
   * Adds an observer to listen for changes to this DisplayValue
   * @param obs The new observer to be notified
   */
  public void addObserver(DisplayValueObserver obs)
  {
    observers.add(obs);
  }
  
  /**
   * Removes the given observer from the list of observers
   * @param obs The observer to remove
   * @return True if obs was found and removed, false otherwise
   */
  public boolean removeObserver(DisplayValueObserver obs)
  {
    return observers.remove(obs);
  }
  
  /*
   * Called when we wish to notify the observers that there has been a
   * change in the watched value, should be called automatically at the
   * end of a refresh()
   */
  protected void notifyObservers()
  {
    Iterator iter = observers.iterator();
    while(iter.hasNext())
      ((DisplayValueObserver) iter.next()).updateDisplayValueChanged(this);
  }
  
  private class LockObserver implements Observer
  {

    public void update (Observable observable, Object arg)
    {
      /*
       * arg will be null the first time this is fired: we don't need
       * any task-specific information from this observer, we have had
       * it all provided for us by the user
       */
      if(arg != null)
        return;
      
      /*
       * arg was null, that means that we've recieved notification of a change
       * in the RunState of the task we're watching. trigger the update
       */
      refresh();
    }
  }
  
}
