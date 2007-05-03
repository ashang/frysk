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

package frysk.gui.sessions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;

import frysk.gui.monitor.SaveableXXX;
import frysk.value.Variable;

/**
 * The WatchList class represents a list of variables that are being
 * watched from a process. All of the variables in a WatchList should
 * come from the same process. A WatchList can also save the variables
 * to ~/.frysk/path/to/proc when the source window closes so that the
 * next time the program is debugged the watched can be automatically
 * retrieved.
 *
 */
public class WatchList implements SaveableXXX
{
  private boolean shouldSave = true; // Save by default
  private List listeners;
  private List vars;
  
  /**
   * Creates a new empty list of watched variables
   *
   */
  public WatchList()
  {
    vars = new LinkedList();
    listeners = new LinkedList();
  }
  
  /**
   * Create a new WatchList containing the same variables as the provided
   * list. The new watch list will not have any listeners attached to it.
   * @param other WatchList to populate variables from
   */
  public WatchList(WatchList other)
  {
    vars = new LinkedList(other.vars);
    listeners = new LinkedList();
  }
  
  /**
   * Adds a variable to the list of watched variables
   * @param var The new variable to watch
   */
  public void addVariable(Variable var)
  {
    vars.add(var);
    notifyListeners();
  }
  
  /**
   * Removes the given variable from the watched list
   * @param var The variable to remove
   * @return True if the variable was removed, false otherwise
   */
  public boolean removeVariable(Variable var)
  { 
    Iterator iter = vars.iterator();
    while(iter.hasNext())
      {
	Variable iVar = (Variable) iter.next();
	//TODO: Do we need a better way of identifying whether two variables are the same?
	if(iVar.getText().equals(var.getText()) && iVar.getType().getName().equals(var.getType().getName()))
	  {
	    iter.remove();
	    notifyListeners();
	    return true;
	  }
      }
    
    return false;
  }
  
  /**
   * Clears all of the variables currently being watched
   *
   */
  public void clearVariables()
  {
    vars.clear();
    notifyListeners();
  }
  
  /**
   *
   * @return An iterator to the variables currently being watched
   */
  public Iterator getVariableIterator()
  {
    return vars.iterator();
  }
  
  /**
   * Refreshes the variables in the list
   *
   */
  public void refreshVars(List variables)
  {
    this.vars = variables;
    notifyListeners();
  }
  
  /**
   * Adds a listener to be notified when the list of watched variables
   * changes.
   * @param obj The object to be notified
   */
  public void addListener(WatchListListener obj)
  {
    listeners.add(obj);
  }
  
  /**
   * Removes a listener from the objects to be notified
   * @param obj The listener to remove
   * @return True if the listener was removed, false otherwise
   */
  public boolean removeListener(WatchListListener obj)
  {
    return listeners.remove(obj);
  }
  
  public void doSaveObject ()
  {
    shouldSave = true;
  }

  public void dontSaveObject ()
  {
    shouldSave = false;
  }

  public void load (Element node)
  {
  }

  public void save (Element node)
  {
    Iterator iter = vars.iterator();
    while(iter.hasNext())
      {
	Variable var = (Variable) iter.next();
	Element varNode = new Element("variable");
	varNode.setAttribute("type", var.getType().toString());
	varNode.setAttribute("name", var.getText());
	varNode.setAttribute("filePath", var.getFilePath());
	varNode.setAttribute("line", ""+var.getLineNo());
	
	node.addContent(varNode);
      }
  }

  public boolean shouldSaveObject ()
  {
    return shouldSave;
  }

  protected void notifyListeners()
  {
    Iterator iter = listeners.iterator();
    while(iter.hasNext())
      ((WatchListListener) iter.next()).variableWatchChanged(vars.iterator());
  }
}
