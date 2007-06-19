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

import inua.eio.ByteBuffer;

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
  
  private byte[] oldValue;
  
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
  
  /*
   * Basically the same as the refresh in the superclass, but we do
   * some extra observer-related items.
   * 
   * (non-Javadoc)
   * @see frysk.rt.DisplayValue#refresh()
   */
  public void refresh()
  {
    // If the task isn't running, notify our observers to that extent
    if(myTask.getBlockers().length == 0)
      {
        /*
         * TODO: right now we don't get notified when the task is resumed,
         * should we?
         */
        //notifyObserversUnavailableTaskResumed();
        return;
      }
    
    super.refresh();    

    // hear ye! hear ye!
    if(observers != null) // (but only if there's someone to listen)
      notifyObserversAvailable();
    
    ByteBuffer newBuffer = myVar.getLocation().getByteBuffer();
//  TODO: is this kosher?
    byte[] newValue = new byte[(int) newBuffer.capacity()];
    newBuffer.get(newValue);
    
    /*
     * On the first call to refresh, lastValue will be null, so 
     * we don't need to send out an event, just update the value.
     * 
     * On subsequent calls, lastValue will not be null so we compare
     * it's value to the one that we just got. If they're the same,
     * fire off an event
     */
    if(oldValue != null && arrayChanged(newValue))
      notifyObserversValueChanged();
    oldValue = newValue;
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
  protected void notifyObserversAvailable()
  {
    Iterator iter = observers.iterator();
    while(iter.hasNext())
      ((DisplayValueObserver) iter.next()).updateAvailableTaskStopped(this);
  }
  
  /*
   * Called when we wish to notify the observers that the watched value
   * is no longer available due to the the task resuming execution
   */
  protected void notifyObserversUnavailableTaskResumed()
  {
    Iterator iter = observers.iterator();
    while(iter.hasNext())
      ((DisplayValueObserver) iter.next()).updateUnavailbeResumedExecution(this);
  }
  
  /*
   * Called whenever the value that we are watching has changed value
   */
  protected void notifyObserversValueChanged()
  {
    Iterator iter = observers.iterator();
    while(iter.hasNext())
      ((DisplayValueObserver) iter.next()).updateValueChanged(this);
  }
  
  /*
   * Returns true if newArray is different than the value contained in
   * oldValue
   */
  protected boolean arrayChanged(byte[] newArray)
  {
    if(oldValue.length != newArray.length)
      return true;
    
    for(int i = 0; i < newArray.length; i++)
      if(newArray[i] != oldValue[i])
        return true;
    
    return false;
  }
  
  
  /*
   * An observer to notify us when the program execution state changes
   */
  private class LockObserver implements Observer
  {

    public void update (Observable observable, Object arg)
    {
      /*
       * When we have received a task, that should mean that the task has stopped.
       * Ergo, Update.
       */
      if(arg != null)
        {
          if(arg.equals(myTask))
            refresh();
        }
     
      /*
       * If arg was null, then the stepping engine has resumed. don't refresh
       * TODO: plug the "resuming" event here?
       */
    }
  }
  
}