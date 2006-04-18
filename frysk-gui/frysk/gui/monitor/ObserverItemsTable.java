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

package frysk.gui.monitor;



import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.gdk.Color;
import org.gnu.glib.Handle;
import org.gnu.gtk.StateType;
import org.gnu.gtk.Table;

import frysk.gui.monitor.observers.ObserverRoot;

/**
 * @author swagiaal
 *
 */
public abstract class ObserverItemsTable extends Table {

	private int row;
	protected ObserverRoot observer;
	
	private LinkedList applyList;
	private LinkedList allList;
	
	public ObserverItemsTable(Handle handle) {
		super(handle);
		
		this.applyList = new LinkedList();
		this.allList = new LinkedList();
		
		this.row = 0;
		this.setBaseColor(StateType.NORMAL, Color.WHITE);
		this.setBackgroundColor(StateType.NORMAL, Color.WHITE);
		this.setBackgroundColor(StateType.ACTIVE, Color.WHITE);
		this.setBackgroundColor(StateType.INSENSITIVE, Color.WHITE);
		this.setBackgroundColor(StateType.SELECTED, Color.WHITE);
		this.setBackgroundColor(StateType.NORMAL, Color.WHITE);

		this.setBaseColor(StateType.NORMAL, Color.WHITE);
		this.setBaseColor(StateType.ACTIVE, Color.WHITE);
		this.setBaseColor(StateType.INSENSITIVE, Color.WHITE);
		this.setBaseColor(StateType.SELECTED, Color.WHITE);
		this.setBaseColor(StateType.NORMAL, Color.WHITE);
		this.showAll();
	}
	
	abstract public ObserverItemRow getNewRow(Combo combo);

	void addRow(Combo combo){
		ObserverItemRow itemRow = this.getNewRow(combo);
		itemRow.addToTable();

		if(combo == null){
			this.applyList.add(itemRow);
		}
		this.allList.add(itemRow);
		
		this.row++;
		System.out.println(this + ": ObserverItemsTable.addRow() row: " + row);
		this.showAll();
	}

	abstract public ObservableLinkedList getCombos(ObserverRoot observer);
	
	public void setObserver(ObserverRoot observer){		
		this.clear();
		this.observer = observer;
		Iterator iterator = this.getCombos(observer).iterator();
		while (iterator.hasNext()) {
			Combo combo = (Combo) iterator.next();
			this.addRow(combo);
		}
		
		System.out.println(this + ": ObserverItemsTable.setObserver() row: " + row);
		
		if(this.row == 0){
			this.addRow(null);
		}
	}
	
	public int getRow(){
		return this.row;
	}
	
	public void apply(){
		Iterator iterator = this.applyList.iterator();
		while (iterator.hasNext()) {
			ObserverItemRow itemRow = (ObserverItemRow) iterator.next();
			itemRow.apply();
		}
		this.applyList.clear();
	}
	
	public void clear(){
		Iterator iterator = this.allList.iterator();

		while (iterator.hasNext()) {
			ObserverItemRow itemRow = (ObserverItemRow) iterator.next();
			itemRow.removeFromTable();
			row--;
		}
		
		this.allList.clear();
		this.applyList.clear();
	}
	
	public void removeRow(ObserverItemRow itemRow){
		this.allList.remove(itemRow);
		this.applyList.remove(itemRow);
		itemRow.removeFromTable();
	}
	
}
