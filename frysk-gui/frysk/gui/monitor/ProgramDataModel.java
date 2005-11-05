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

import java.util.HashMap;
import java.util.logging.Logger;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.pango.Weight;

import frysk.gui.FryskGui;
import frysk.gui.monitor.ProcDataModel.FilterType;

/**
 * The data model that stores data about currently watched
 * programs.
 * */
public class ProgramDataModel {
	

	public static ProgramDataModel theManager = new ProgramDataModel();

	private ListStore listStore;
	private TreeModelFilter filteredStore;
	
	DataColumn[] columns;
	
	private DataColumnBoolean enabledDC;
	private DataColumnString programEventNameDC;
	private DataColumnString colorDC;
	private DataColumnBoolean visibleDC;
	private DataColumnObject programEventDataDC;
	private DataColumnInt weightDC;
	
	private HashMap iterHash;
	
	private int currentFilter;
	
	/** stores filter argument if it is of type int */ 
	private int intFilterArgument;
	/** stores filter argument if it is of type String */
	private String stringFilterArgument;

	private boolean filterON;
			
	private Logger errorLog = Logger.getLogger(FryskGui.ERROR_LOG_ID);

	
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
		
		this.iterHash = new HashMap();
		this.currentFilter = FilterType.NONE;		
		this.filterON = true;
		
	}
	
	public DataColumnBoolean getEnabledDC() {
		return this.enabledDC;
	}

	public DataColumnString getEventNameDC() {
		return this.programEventNameDC;
	}

	public DataColumnString getColorDC() {
		return this.colorDC;
	}

	
	public DataColumnInt getWeightDC() {
		return this.weightDC;
	}
	
	
	public void add(ProgramData data)
	{
		TreeIter it = null;
		it = listStore.appendRow();
		listStore.setValue(it, (DataColumnBoolean) enabledDC, data.isEnabled());
		listStore.setValue(it, (DataColumnString) programEventNameDC,data.getExecutable());
		listStore.setValue(it, programEventDataDC, data);
		listStore.setValue(it, weightDC, Weight.NORMAL.getValue());

	}
	
	public TreeModel getModel() {
				return this.listStore;
			}
	
		
	
}
