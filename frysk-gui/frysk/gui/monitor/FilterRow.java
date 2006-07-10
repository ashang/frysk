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

import org.gnu.gtk.AttachOptions;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;

import frysk.gui.monitor.filters.ComboFactory;
import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.observers.ObserverRoot;

public class FilterRow extends ObserverItemRow{

	private static ObservableLinkedList booleanList;
	private static final GuiObject  IS = new GuiObject("is", "");
	private static final GuiObject  ISNOT = new GuiObject("is not", "");
	
	static{
		booleanList = new ObservableLinkedList();
		booleanList.add(IS);
		booleanList.add(ISNOT);
	}
	
	SimpleComboBox booleanComboBox;
	Filter filter;
	
	FilterRow(ObserverItemsTable table, ObserverRoot observer, Combo myCombo) {
		super(table, observer, myCombo);
		
		ObservableLinkedList comboList = ComboFactory.theFactory.getFilterCombos(observer);
		itemsComboBox.watchLinkedList(comboList);
		
		booleanComboBox = new SimpleComboBox();
		booleanComboBox.watchLinkedList(booleanList);
		
		if(combo != null){
			filter = (Filter) combo.getFilter();
			itemsComboBox.setSelectedText(combo.getName());
			comboList.swap(itemsComboBox.getSelectedObject(), combo);
			itemsComboBox.setSelectedObject(combo);
			
			boolean currentBoolean = filter.getFilterBoolean();

			if(currentBoolean){
				booleanComboBox.setSelectedObject(IS);
			}else{
				booleanComboBox.setSelectedObject(ISNOT);			
			}

			booleanComboBox.addListener(new ComboBoxListener() {
				public void comboBoxEvent(ComboBoxEvent event) {
					GuiObject object = booleanComboBox.getSelectedObject();
					if(object == IS){
						filter.setFilterBoolean(true);
					}else{				
						filter.setFilterBoolean(false);
					}
				}
			});
		}	

        removeButton.addListener(new ButtonListener() {
            public void buttonEvent(ButtonEvent event) {
                if (event.isOfType(ButtonEvent.Type.CLICK)) {
                    if(FilterRow.this.table.getIndexOfFinalRow() == 1){
//                        if(combo != null && combo.isApplied()){
                          booleanComboBox.setSelectedObject(null);
//                        }
                    }
                }
            }
        });
        
        super.apply();		/* Keep the info contained in this row in case no event was given to the argumentEntry */
	}	
	
	public void removeFromTable(){
		super.removeFromTable();
		this.table.remove(booleanComboBox);
	}
	
	public void addToTable(){
		AttachOptions EXPAND_AND_FILL = AttachOptions.EXPAND.or(AttachOptions.FILL);
		
		int count = 0;
		table.attach(itemsComboBox,   count,++count,table.getIndexOfFinalRow(),table.getIndexOfFinalRow()+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
		table.attach(booleanComboBox, count,++count,table.getIndexOfFinalRow(),table.getIndexOfFinalRow()+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
		table.attach(argumentEntry,   count,++count,table.getIndexOfFinalRow(),table.getIndexOfFinalRow()+1, EXPAND_AND_FILL, AttachOptions.SHRINK, 0, 0);
		table.attach(addButton,       count,++count,table.getIndexOfFinalRow(),table.getIndexOfFinalRow()+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
		table.attach(removeButton,    count,++count,table.getIndexOfFinalRow(),table.getIndexOfFinalRow()+1, AttachOptions.SHRINK, AttachOptions.SHRINK, 0, 0);
	}
}