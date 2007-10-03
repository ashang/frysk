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

import frysk.gui.monitor.DynamicWidget;
import frysk.gui.monitor.GuiObject;
import frysk.proc.Task;

/**
 * @author swagiaal
 * Filter passes if the name of the parent process of 
 * the given Task matches the stored process name.
 */
public class TaskProcNameFilter extends TaskFilter {
	
	private ProcNameFilter procNamefilter;
	
	public TaskProcNameFilter(){
		super();
		this.procNamefilter = new ProcNameFilter();
		
		this.initWidget();
	}
	
	public TaskProcNameFilter(String procName){
		super("Name Filter", "Checks the process name of the parent of the given task");
		this.procNamefilter = new ProcNameFilter(procName);
		
		this.initWidget();
	}
	
	public TaskProcNameFilter(TaskProcNameFilter other){
		super(other);
		this.procNamefilter = new ProcNameFilter(other.procNamefilter);
		
		this.initWidget();
	}
	
	private void initWidget(){
		this.widget.addString(new GuiObject("Name", "name of the process"), this.getProcName(),
				new DynamicWidget.StringCallback() {
			public void stringChanged(String string) {
				setProcName(string);
			}
		});
	}
	
	public boolean filter(Task task) {
		return this.procNamefilter.filter(task.getProc());
	}
	
	public Filter getCopy() {
		return new TaskProcNameFilter(this);
	}

	/**
	 * Set the name of the process to be filtered for.
	 * @param procName the name of the process to be filtered for.
	 */
	public void setProcName(String procName) {
		this.procNamefilter.setName(procName);
	}

	/**
	 * Get the name of the process that is being filtered for.
	 * @return the name of the process that is being filtered for.
	 */
	public String getProcName() {
		return this.procNamefilter.getProcName();
	}
}
