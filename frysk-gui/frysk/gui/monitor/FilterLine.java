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
import org.gnu.gtk.Label;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;

import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.filters.FilterManager;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.observers.ObserverRoot;

public class FilterLine extends HBox{

	private VBox filterWidgetVBox;
	private ObserverRoot observer;
	private SimpleComboBox filterPointComboBox;
	private SimpleComboBox filterComboBox;
	
	private FilterPoint selectedFilterPoint;
	private Filter      selectedFilter;
	
	public FilterLine(ObserverRoot observer) {
		super(false, 3);
		
		this.observer = observer;
		
		//========================================
		// add an item for each filter point
		filterPointComboBox = new SimpleComboBox();
		Iterator iter = observer.getFilterPoints().iterator();
		while(iter.hasNext()){
			filterPointComboBox.add((FilterPoint)iter.next());
		}
		this.packStart(filterPointComboBox, false, true, 0);
		//========================================
		
		//========================================
		// populate a drop-down menu for selected filterPoint
		filterComboBox = new SimpleComboBox();
		
		filterPointComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				filterComboBox.clear();
				filterComboBox.setActive(-1);
				selectedFilterPoint = (FilterPoint)filterPointComboBox.getSelectedObject();
				Iterator iter = selectedFilterPoint.getApplicableFilters().iterator();
				while(iter.hasNext()){
					filterComboBox.add((Filter)iter.next());
				}
			}
		});
		this.packStart(filterComboBox, false, true, 0);
		//========================================

		//========================================
		//get the selected filter's widget
		filterWidgetVBox = new VBox(false, 0);
		filterWidgetVBox.packStart(new Label(""), true, true, 0); // spacer
		filterComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				System.out.println(".comboBoxEvent()" + filterComboBox.getSelectedObject());
				Widget[] widgets = filterWidgetVBox.getChildren();
				for (int i = 0; i < widgets.length; i++) {
					filterWidgetVBox.remove(widgets[i]);
				}
				
				Filter filter = (Filter)filterComboBox.getSelectedObject();
				selectedFilter = filter;
				if(filter != null){
					filterWidgetVBox.packStart(filter.getWidget(), true, true, 0);
				}else{
					filterWidgetVBox.packStart(new Label(""), true, true, 0); // spacer
				}
				
				filterWidgetVBox.showAll();System.out.println("[3]");
			}
		});
		this.packStart(filterWidgetVBox, true, true, 0);
		//========================================
		
		
		this.showAll();
	}
	
	/**
	 * Change the selections in the filter line to represent
	 * the given FilterPoint and filter.
	 * */
	public void setSelection(FilterPoint filterPoint, final Filter filter){
		// assertions
		if(!this.observer.getFilterPoints().contains(filterPoint)){
			throw new IllegalArgumentException("The given FilterPoint is not a member of the observer represented by this filter line");
		}
		
		if(!filterPoint.getFilters().contains(filter)){
			throw new IllegalArgumentException("The given filter is not a member of the given filterPoint");
		}

		this.selectedFilterPoint = filterPoint;
		this.selectedFilter = filter;
		
//		ComboBoxListener listener = new ComboBoxListener() {
//			public void comboBoxEvent(ComboBoxEvent arg0) {
//			}
//		};
//		this.filterComboBox.addListener(listener);
		
		System.out.println("[0]");
		filterPointComboBox.setSelectedObject(filterPoint);System.out.println("[1]");
		filterComboBox.setSelectedText(filter.getName());System.out.println("[2]");
		
//   	this.filterComboBox.removeListener(listener);
		
	}
	
	/**
	 * Remove the current filter line from the observer
	 * ie remove the filter represented by this line from
	 * the filter point represented by this line from the
	 * observer represented by this line.
	 * This is how updates are done (remove old then add new).
	 * */
	public void removeFromObserver(){
		this.selectedFilterPoint.removeFilter(selectedFilter);
	}
	
	/**
	 * Add the current filter line to the observer
	 * ie add the filter represented by this line to
	 * the filter point represented by this line in the
	 * observer represented by this line.
	 * This is how updates are done (remove old then add new).
	 * */
	public void addToObserver(){
		this.selectedFilterPoint.addFilter(FilterManager.theManager.getFilterCopy(selectedFilter));
	}

}
