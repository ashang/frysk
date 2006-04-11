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

import org.gnu.gtk.Button;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.FocusEvent;
import org.gnu.gtk.event.FocusListener;

import frysk.gui.monitor.observers.ObserverRoot;

public abstract class ObserverItemRow {
	
		Combo combo;
		
		SimpleComboBox itemsComboBox;

		CompletingEntry argumentEntry;
		Button addButton;
		Button removeButton;

		ObserverRoot observer;

		protected ObserverItemsTable table;
		
		ObserverItemRow(ObserverItemsTable table, ObserverRoot observer, Combo myCombo){
		
			this.table = table;
			this.combo = myCombo;
			this.observer = observer;
			
			argumentEntry = new CompletingEntry();
			if(combo != null){
				String argument = ((LiaisonItem)combo.getFilter()).getArgument();
				if(argument == null){
					argumentEntry.setSensitive(false);
				}else{
					argumentEntry.setText(argument);
				}
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

			itemsComboBox = new SimpleComboBox();
			itemsComboBox.addListener(new ComboBoxListener() {
				public void comboBoxEvent(ComboBoxEvent event) {
					if(event.isOfType(ComboBoxEvent.Type.CHANGED)){
						if(combo != null && combo.isApplied()){
							combo.unApply();
							combo = (Combo) itemsComboBox.getSelectedObject();
							ObservableLinkedList list = combo.getFilter().getArgumentCompletionList();
							if(list!= null){
								argumentEntry.watchList(list);
							}
							combo.apply();
						}
					}
				}
			});
			
			addButton = new Button("");
			addButton.setImage(new Image(GtkStockItem.ADD, IconSize.BUTTON));
			addButton.addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						ObserverItemRow.this.table.addRow(null);
					}
				}
			});
			
			removeButton = new Button("");
			removeButton.setImage(new Image(GtkStockItem.REMOVE, IconSize.BUTTON));
			removeButton.addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						
						if(ObserverItemRow.this.table.getRow() == 1){
							if(combo != null && combo.isApplied()){
								combo.unApply();
							}
						}else{
							if(combo != null && combo.isApplied()){
								combo.unApply();
							}
							ObserverItemRow.this.table.removeRow(ObserverItemRow.this);
						}
					}
				}
			});
		}
		
		public void apply() {
			if(combo == null){
				// this FilterRow represents and unapplied filter
				combo = (Combo) itemsComboBox.getSelectedObject();
			}

			if(combo == null){// nothing was selected by user
				return;
			}
			if(!combo.isApplied()){
				combo.apply();
			}
			
			((LiaisonItem)combo.getFilter()).setArgument(argumentEntry.getText());
		}

		public void removeFromTable(){
			ObserverItemRow.this.table.remove(itemsComboBox);
			ObserverItemRow.this.table.remove(argumentEntry);
			ObserverItemRow.this.table.remove(addButton);
			ObserverItemRow.this.table.remove(removeButton);
			
			ObserverItemRow.this.table.showAll();
		}
		
		public abstract void addToTable();
		
}

