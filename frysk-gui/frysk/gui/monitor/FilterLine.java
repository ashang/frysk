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

import org.gnu.gtk.HBox;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;

import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.observers.ObserverRoot;

public class FilterLine extends HBox{

	private VBox filterWidgetVBox;
	
	public FilterLine(ObserverRoot observer) {
		super(false, 3);
		
		//========================================
		// add an item for each filter point
		final SimpleComboBox comboBox = new SimpleComboBox();
		Iterator iter = observer.getFilterPoints().iterator();
		while(iter.hasNext()){
			comboBox.add((FilterPoint)iter.next());
		}
		this.packStart(comboBox, false, false, 0);
		//========================================
		
		//========================================
		// populate a drop-down menu for selected filterPoint
		final SimpleComboBox filtersComboBox = new SimpleComboBox();
		
		comboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				filtersComboBox.clear();
				filtersComboBox.setActive(-1);
				Iterator iter = ((FilterPoint)comboBox.getSelectedObject()).getApplicableFilters().iterator();
				while(iter.hasNext()){
					filtersComboBox.add((Filter)iter.next());
				}
			}
		});
		this.packStart(filtersComboBox, false, false, 0);
		//========================================

		//========================================
		//get the selected filter's widget
		filterWidgetVBox = new VBox(false, 0);
		filtersComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				System.out.println(".comboBoxEvent()" + filtersComboBox.getSelectedObject());
				Widget[] widgets = filterWidgetVBox.getChildren();
				for (int i = 0; i < widgets.length; i++) {
					filterWidgetVBox.remove(widgets[i]);
				}
				
				Filter filter = (Filter)filtersComboBox.getSelectedObject();
				if(filter != null){ filterWidgetVBox.add(filter.getWidget()); }
				filterWidgetVBox.showAll();
			}
		});
		this.packStart(filterWidgetVBox, false, false, 0);
		//========================================
		
		
		this.showAll();
	}

}
