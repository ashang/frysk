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
/*
 * Created on Oct 20, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Iterator;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.pango.Weight;

/**
 * 
 * The data model that stores data about currently watched
 * programs.
 * */
public class ProgramDataModel {
	

	public static ProgramDataModel theManager = new ProgramDataModel();

	private ListStore listStore;
	private TreeModelFilter filteredStore;
	
	private DataColumn[] columns;
	
	private DataColumnBoolean enabledDC;
	private DataColumnString programEventNameDC;
	private DataColumnString colorDC;
	private DataColumnBoolean visibleDC;
	private DataColumnObject programEventDataDC;
	private DataColumnInt weightDC;
	

	public ProgramDataModel() {
		
		this.enabledDC     = new DataColumnBoolean();
		this.programEventNameDC = new DataColumnString();
		this.programEventDataDC= new DataColumnObject();
		this.colorDC   = new DataColumnString();
		this.visibleDC = new DataColumnBoolean();
		this.weightDC = new DataColumnInt();
		
		columns = new DataColumn[6];
		columns[0] = this.getEnabledDC(); 
		columns[1] = this.getEventNameDC(); 
		columns[2] = this.getColorDC(); 
		columns[3] = this.visibleDC;
		columns[4] = this.weightDC;
		columns[5] = this.programEventDataDC;
		
		this.listStore = new ListStore(columns);	
		this.filteredStore = new TreeModelFilter(this.listStore);
		this.filteredStore.setVisibleColumn(visibleDC);
			
	}
	
	/**
	 * Returns the boolean data column type for this model.
	 * 
	 * @return - Boolean. 
	 */
	public DataColumnBoolean getEnabledDC() {
		return this.enabledDC;
	}

	/**
	 * Returns the string data column type for this model.
	 * 
	 * @return - String. 
	 */
	public DataColumnString getEventNameDC() {
		return this.programEventNameDC;
	}

	/**
	 * Returns the string data column type for this model.
	 * 
	 * @return - String. 
	 */
	public DataColumnString getColorDC() {
		return this.colorDC;
	}

	/**
	 * Returns the object data column type for this model.
	 * 
	 * @return - DataColumnObject. 
	 */
	public DataColumnObject getObjectDataDC() {
		return this.programEventDataDC;
	}
	
	/**
	 * Returns the weight data column type for this model.
	 * 
	 * @return - DataColumnInt. 
	 */
	public DataColumnInt getWeightDC() {
		return this.weightDC;
	}
	
	/**
	 * Toggles the enabled flag from it's current state to it's opposite state at
	 * the given row. Saves the model to disk.
	 * 
	 * @param row - String. Source row number  in tree.
	 */
	public void toggle(String row) 
	{
		TreeIter it = null;
		it = listStore.getIter(row);
		
		ProgramData selected = (ProgramData) listStore.getValue(it,programEventDataDC);
		
		if (selected.isEnabled())
			selected.setEnabled(false);
		else
			selected.setEnabled(true);
					
		listStore.setValue(it, (DataColumnBoolean) enabledDC, selected.isEnabled());
		selected.save();
	}
	
	/**
	 * Returns a preformatted tip of the given TreePath element in the
	 * tree that can be used in ToolTips.
	 * 
	 * @param path - TreePath. Source element in the tree
	 * @return - String.  Pre-formatted tip.
	 */
	public String getTip(TreePath path)
	{
		String tip = "";
		TreeIter item = listStore.getIter(path.toString());
		ProgramData tipStore = (ProgramData) listStore.getValue(item,this.programEventDataDC);
		
		
		tip = "* Name: " + tipStore.getName();
		tip+="\n" + "* Executable: " + tipStore.getExecutable();
		tip+="\n" + "* Enabled: " + tipStore.isEnabled(); 
		tip+="\n" + "* Watched Processes: ";
			 
		Iterator i = tipStore.getProcessList().iterator();
		while (i.hasNext())
		{
			tip+= ((String)i.next());
				   if (i.hasNext())
					   tip+=", ";
		}
			   
	    tip+="\n* Observers: ";

	    i = tipStore.getObserverList().iterator();
		while (i.hasNext())
			   {
				   tip+=((String)i.next());
				   if (i.hasNext())
					   tip+=", ";
				   
			   }
		return tip;
		
	}
	
	/**
	 * Returns the underlying ProgramData class instance
	 * that forms the DataColumnObject element of the tree
	 * 
	 * @param  - TreePath. Source element in the tree
	 * @return - ProgramData. 
	 */
	public ProgramData interrogate(TreePath path)
	{
		TreeIter it = listStore.getIter(path.toString());
		ProgramData value = (ProgramData) listStore.getValue(it,programEventDataDC);
		return value;
	}
	
	/**
	 * Adds the parameter ProgramData object to thre tree. Construct the other column
	 * elements from the passed ProgramData parameter.
	 * 
	 * @param data - ProgramData. Adds the passed ProgramData object to the tree.
	 * 
	 */
	public void add(ProgramData data)
	{
		TreeIter it = null;
		it = listStore.appendRow();
		listStore.setValue(it, (DataColumnBoolean) enabledDC, data.isEnabled());
		listStore.setValue(it, (DataColumnString) programEventNameDC,data.getExecutable());
		listStore.setValue(it, programEventDataDC, data);
		listStore.setValue(it, weightDC, Weight.NORMAL.getValue());

	}
	
	/**
	 * Returns the model used in ProgramDataModel.
	 *
	 * @return TreeModel
	 */
	public TreeModel getModel() {
		return this.listStore;
	}
}
