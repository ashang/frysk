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
import org.gnu.gtk.AttachOptions;
import org.gnu.gtk.Button;
import org.gnu.gtk.Entry;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.StateType;
import org.gnu.gtk.Table;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.FocusEvent;
import org.gnu.gtk.event.FocusListener;

import frysk.gui.monitor.filters.FilterCombo;
import frysk.gui.monitor.filters.FilterComboFactory;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 * @author swagiaal
 *
 */
public class FiltersTable extends Table {

	int row;
	private ObserverRoot observer;
	
	private LinkedList applyList;
	private LinkedList allList;
	
	ObservableLinkedList booleanList;
	
	public FiltersTable(Handle handle) {
		super(handle);
		
		this.applyList = new LinkedList();
		this.allList = new LinkedList();
		
		this.booleanList = new ObservableLinkedList();
		
		booleanList.add(new GuiObject("is", ""));
		booleanList.add(new GuiObject("is not", ""));
		
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
	
	private void addRow(FilterCombo combo){
		FilterRow filterRow = new FilterRow(combo);
		filterRow.addToTable();

		if(combo == null){
			this.applyList.add(filterRow);
		}
		this.allList.add(filterRow);
		
		this.row++;
		this.showAll();
	}

	public void setObserver(ObserverRoot observer){
		System.out.println("FiltersTable.setObserver() " + observer);
		this.clear();
		this.observer = observer;
		Iterator iterator = observer.getCurrentFilterCombos().iterator();
		while (iterator.hasNext()) {
			FilterCombo combo = (FilterCombo) iterator.next();
			this.addRow(combo);
		}
		
		if(this.row == 0){
			this.addRow(null);
		}
	}
	
	public void apply(){
		Iterator iterator = this.applyList.iterator();
		while (iterator.hasNext()) {
			FilterRow filterRow = (FilterRow) iterator.next();
			filterRow.apply();
		}
		this.applyList.clear();
	}
	
	public void clear(){
		Iterator iterator = this.allList.iterator();
		while (iterator.hasNext()) {
			FilterRow filterRow = (FilterRow) iterator.next();
			filterRow.removeFromTable();
		}
		
		this.allList.clear();
		this.applyList.clear();
	}
	
	public void removeRow(FilterRow row){
		this.allList.remove(row);
		this.applyList.remove(row);
		row.removeFromTable();
	}
	
	private class FilterRow {
		FilterCombo combo;
		
		SimpleComboBox filtersComboBox;
		SimpleComboBox booleanComboBox;
		Entry argumentEntry;
		Button addButton;
		Button removeButton;
		
		FilterRow(FilterCombo myCombo){
			this.combo = myCombo;
			
			filtersComboBox = new SimpleComboBox();
			ObservableLinkedList comboList = FilterComboFactory.theFactory.getFilterCombos(observer);
			filtersComboBox.watchLinkedList(comboList);
			
			if(combo != null){
				filtersComboBox.setSelectedText(combo.getName());
				comboList.swap(filtersComboBox.getSelectedObject(), combo);
				filtersComboBox.setSelectedObject(combo);
			}
			
			booleanComboBox = new SimpleComboBox();
			booleanComboBox.watchLinkedList(FiltersTable.this.booleanList);

			argumentEntry = new Entry();
			if(combo != null){
				argumentEntry.setText(combo.getFilter().getArgument());
			}else{
				argumentEntry.setText("");
			}
			argumentEntry.addListener(new FocusListener() {
				public boolean focusEvent(FocusEvent event) {
					if(event.isOfType(FocusEvent.Type.FOCUS_OUT)){
						apply();
					}
					return false;
				}
			});

			addButton = new Button("");
			addButton.setImage(new Image(GtkStockItem.ADD, IconSize.BUTTON));
			addButton.addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						addRow(null);
					}
				}
			});
			
			removeButton = new Button("");
			removeButton.setImage(new Image(GtkStockItem.REMOVE, IconSize.BUTTON));
			removeButton.addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						System.out.println(".buttonEvent() row         : " + row);
						System.out.println(".buttonEvent() combo       : " + combo);
						System.out.println(".buttonEvent() comboApplied: " + combo.isApplied());
						
						if(row == 1){
							if(combo != null && combo.isApplied()){
								combo.unApply();
							}
						}else{
							if(combo != null && combo.isApplied()){
								combo.unApply();
							}
							FiltersTable.this.removeRow(FilterRow.this);
						}
					}
				}
			});
		}
		
		public void apply() {
			if(combo == null){
				// this FilterRow represents and unapplied filter
				combo = (FilterCombo) filtersComboBox.getSelectedObject();
			}

			if(combo == null){// nothing was selected by user
				return;
			}
			if(!combo.isApplied()){
				combo.apply();
			}
			
			combo.getFilter().setArgument(argumentEntry.getText());
		}

		public void removeFromTable(){
			FiltersTable.this.remove(filtersComboBox);
			FiltersTable.this.remove(booleanComboBox);
			FiltersTable.this.remove(argumentEntry);
			FiltersTable.this.remove(addButton);
			FiltersTable.this.remove(removeButton);
			
			FiltersTable.this.row--;
			FiltersTable.this.showAll();
		}
		
		public void addToTable(){
			AttachOptions EXPAND_AND_FILL = AttachOptions.EXPAND.or(AttachOptions.FILL);
			
			int count = 0;
			FiltersTable.this.attach(filtersComboBox, count,++count,FiltersTable.this.row,FiltersTable.this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
			FiltersTable.this.attach(booleanComboBox, count,++count,FiltersTable.this.row,FiltersTable.this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
			FiltersTable.this.attach(argumentEntry,   count,++count,FiltersTable.this.row,FiltersTable.this.row+1, EXPAND_AND_FILL, AttachOptions.SHRINK, 0, 0);
			FiltersTable.this.attach(addButton,       count,++count,FiltersTable.this.row,FiltersTable.this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
			FiltersTable.this.attach(removeButton,    count,++count,FiltersTable.this.row,FiltersTable.this.row+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
		}
	}
}
