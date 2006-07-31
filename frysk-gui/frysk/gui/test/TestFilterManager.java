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

package frysk.gui.test;

import junit.framework.TestCase;
import frysk.gui.monitor.filters.FilterManager;
import frysk.gui.monitor.filters.IntFilter;
import frysk.gui.monitor.filters.ProcFilter;
import frysk.gui.monitor.filters.ProcNameFilter;
import frysk.gui.monitor.filters.ProcParentNameFilter;
import frysk.gui.monitor.filters.ProcCommandLineFilter;
import frysk.gui.monitor.filters.ProcPathFilter;
import frysk.gui.monitor.filters.TaskFilter;
import frysk.gui.monitor.filters.TaskProcNameFilter;
import frysk.gui.monitor.filters.TaskProcParentNameFilter;
import frysk.gui.monitor.filters.TaskProcCommandLineFilter;
import frysk.gui.monitor.filters.TaskProcPathFilter;

public class TestFilterManager extends TestCase{

	public void testAddingRemovingFilters(){
      
      IntFilter intFilter = new IntFilter();
      TaskFilter taskFilter = new TaskProcNameFilter();
      ProcFilter procFilter = new ProcNameFilter();
      ProcParentNameFilter procParentNameFilter = new ProcParentNameFilter();
      ProcCommandLineFilter procCommandLineFilter = new ProcCommandLineFilter();
      ProcPathFilter procPathFilter = new ProcPathFilter();
      TaskProcParentNameFilter taskProcParentNameFilter = new TaskProcParentNameFilter();
      TaskProcCommandLineFilter taskProcCommandLineFilter = new TaskProcCommandLineFilter();
      TaskProcPathFilter taskProcPathFilter = new TaskProcPathFilter();
      
      FilterManager.theManager.addIntFilterPrototype(intFilter);
      FilterManager.theManager.addTaskFilterPrototype(taskFilter);
      FilterManager.theManager.addTaskFilterPrototype(taskProcParentNameFilter);
      FilterManager.theManager.addTaskFilterPrototype(taskProcCommandLineFilter);
      FilterManager.theManager.addTaskFilterPrototype(taskProcPathFilter);
      FilterManager.theManager.addProcFilterPrototype(procFilter);
      FilterManager.theManager.addProcFilterPrototype(procParentNameFilter);
      FilterManager.theManager.addProcFilterPrototype(procCommandLineFilter);
      FilterManager.theManager.addProcFilterPrototype(procPathFilter);

      assertTrue("Filter has been added", FilterManager.theManager.getIntFilters().contains(intFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getTaskFilters().contains(taskFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getTaskFilters().contains(taskProcParentNameFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getTaskFilters().contains(taskProcCommandLineFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getTaskFilters().contains(taskProcPathFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getProcFilters().contains(procFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getProcFilters().contains(procParentNameFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getProcFilters().contains(procCommandLineFilter));
      assertTrue("Filter has been added", FilterManager.theManager.getProcFilters().contains(procPathFilter));
      
      FilterManager.theManager.removeGenericFilterPrototype(intFilter);
      FilterManager.theManager.removeTaskFilterPrototype(taskFilter);
      FilterManager.theManager.removeTaskFilterPrototype(taskProcParentNameFilter);
      FilterManager.theManager.removeTaskFilterPrototype(taskProcParentNameFilter);
      FilterManager.theManager.removeTaskFilterPrototype(taskProcCommandLineFilter);
      FilterManager.theManager.removeProcFilterPrototype(procFilter);
      FilterManager.theManager.removeProcFilterPrototype(procParentNameFilter);
      FilterManager.theManager.removeProcFilterPrototype(procCommandLineFilter);
      FilterManager.theManager.removeProcFilterPrototype(procPathFilter);

    }
    
}
