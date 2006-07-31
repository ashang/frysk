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
// type filter text
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


package frysk.gui.monitor.filters;

import java.util.Observable;

import frysk.gui.monitor.ObservableLinkedList;

/**
 * Only once instance. Keeps a list of available filters. Provides an interface
 * for instantiating those actions.
 */
public class FilterManager
    extends Observable
{

  public static FilterManager theManager = new FilterManager();

  private ObservableLinkedList procFilters;

  private ObservableLinkedList taskFilters;

  private ObservableLinkedList intFilters;

  public FilterManager ()
  {
    this.taskFilters = new ObservableLinkedList();
    this.procFilters = new ObservableLinkedList();
    this.intFilters = new ObservableLinkedList();
    this.initFilterList();
  }

  private void initFilterList ()
  {
    this.addProcFilterPrototype(new ProcNameFilter());
    this.addProcFilterPrototype(new ProcParentNameFilter());
    this.addProcFilterPrototype(new ProcPathFilter());
    this.addProcFilterPrototype(new ProcCommandLineFilter());
    this.addTaskFilterPrototype(new TaskProcNameFilter());
    this.addTaskFilterPrototype(new TaskProcParentNameFilter());
    this.addTaskFilterPrototype(new TaskProcPathFilter());
    this.addTaskFilterPrototype(new TaskProcCommandLineFilter());
    this.addIntFilterPrototype(new IntFilter());
  }

  public void addIntFilterPrototype (IntFilter filter)
  {
    this.intFilters.add(filter);
  }

  /**
   * Returns a copy of the prototype given. A list of available prototypes can
   * be
   * 
   * @param prototype a prototype of the observer to be instantiated.
   */
  public Filter getFilterCopy (Filter prototype)
  {
    return (Filter) prototype.getCopy();
  }

  /**
   * add an observer to the list of available observers.
   */
  public void addProcFilterPrototype (ProcFilter filter)
  {
    this.procFilters.add(filter);
    this.hasChanged();
    this.notifyObservers();
  }

  public void addTaskFilterPrototype (TaskFilter filter)
  {
    this.taskFilters.add(filter);
    this.hasChanged();
    this.notifyObservers();
  }

  public ObservableLinkedList getProcFilters ()
  {
    return this.procFilters;
  }

  public ObservableLinkedList getIntFilters ()
  {
    return this.intFilters;
  }

  public ObservableLinkedList getTaskFilters ()
  {
    return this.taskFilters;
  }

  public void removeGenericFilterPrototype (IntFilter intFilter)
  {
    this.intFilters.remove(intFilter);
  }

  public void removeTaskFilterPrototype (TaskFilter taskFilter)
  {
    this.taskFilters.remove(taskFilter);
  }

  public void removeProcFilterPrototype (ProcFilter procFilter)
  {
    this.procFilters.remove(procFilter);
  }
  
}
