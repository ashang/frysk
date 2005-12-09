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

import org.gnu.gtk.HBox;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;

import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.filters.FilterManager;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.observers.ObserverRoot;

/*
 * I would like to apologize to anyone after me who needs to debug this widget
 * or extend its functionality. Although not clear it has two modes one where
 * it displays what the user asks it to and make those changes concrete (ie
 * add filter to observer). The second mode displays what has already been 
 * done to the observer. So be ware of infinite loops.
 * Sincerely,
 *   Sami Wagiaalla
 */

/**
 * 
 * @author swagiaal
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FilterLineWidget extends HBox{

	ObserverRoot observer;
	FilterPoint  filterPoint;
	Filter       filter;
	
	private SimpleComboBox filterPointsComboBox;
	private SimpleComboBox filtersComboBox;
	private VBox filterWidgetVBox;

	private GuiObject spaceHolder;
	
	public FilterLineWidget(ObserverRoot observer) {
		super(false, 3);
		
		this.observer = observer;

		this.filtersComboBox = new SimpleComboBox();
		
		this.spaceHolder = new GuiObject("----------","");
		
		this.filtersComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {System.out.println("filtersComboBox.ComboBoxListener()");
		
				if(filtersComboBox.getSelectedObject() != spaceHolder){System.out.println("filtersComboBox.ComboBoxListener() getActive()!=0");
					if(filter == (Filter)filtersComboBox.getSelectedObject()){System.out.println("filtersComboBox.ComboBoxListener() filter == (Filter)filtersComboBox.getSelectedObject()");
						// we now know its not a user event because public setFilter
						// sets this.filter to selected filter
						privateSetFilter((Filter)filtersComboBox.getSelectedObject());
					}else{System.out.println("filtersComboBox.ComboBoxListener() filter == (Filter)filtersComboBox.getSelectedObject()");
						// here we know the user has selected a new prototype an 
						// would like to add that to the observers
						Filter myConcreteFilter = FilterManager.theManager.getFilterCopy((Filter)filtersComboBox.getSelectedObject());
						filterPoint.addFilter(myConcreteFilter);
						setFilter(myConcreteFilter);
					}
				}
			}
		});
		
		this.filterPointsComboBox = new SimpleComboBox();
		this.filterPointsComboBox.watchLinkedList(this.observer.getFilterPoints());
		this.filterPointsComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				privateSetFilterPoint((FilterPoint)filterPointsComboBox.getSelectedObject());
			}
		});
	
		this.filterWidgetVBox = new VBox(false,0);
		this.packStart(filterPointsComboBox);
		this.packStart(filtersComboBox);
		this.packEnd(this.filterWidgetVBox);
	}
	
	
	private void privateSetFilterPoint(FilterPoint filterPoint){
		this.filterPoint = filterPoint;
		this.filtersComboBox.clear();
		this.filtersComboBox.watchLinkedList(filterPoint.getApplicableFilters());
		this.filtersComboBox.add(this.spaceHolder, 0);
	}
	
	public void setFilterPoint(FilterPoint filterPoint){
		this.filterPointsComboBox.setSelectedObject(filterPoint);
	}
	
	private void privateSetFilter(Filter filter){System.out.println("FilterLineWidget.privateSetFilter()");
//		if(this.filter == filter) return;
		
		Widget[] widgets = this.filterWidgetVBox.getChildren();
		for (int i = 0; i < widgets.length; i++) {
			this.filterWidgetVBox.remove(widgets[i]);
		}
		
		if(this.filter.getWidget().getParent() == null){
			this.filterWidgetVBox.packStart(this.filter.getWidget());
		}else{
			this.filter.getWidget().reparent(filterWidgetVBox);
		}
		this.showAll();
	}
	
	/**
	 * Sets the current filter to the given one.
	 * This function expects to receive a real filter not a prototype
	 * from FilterManager.
	 * @param filter
	 */
	public void setFilter(Filter filter){System.out.println("FilterLineWidget.setFilter()");
		if(this.filter != spaceHolder && this.filter != null){
			System.out.println("filtersComboBox.ComboBoxListener() selection has changed removing old filter since it was not ap");
			filterPoint.removeFilter(this.filter);
		}
	
		this.filter = filter;
		
//		filter.setName(">"+ filter.getName() +"<");
		
		this.filtersComboBox.setActive(0);
		this.filtersComboBox.remove(this.filtersComboBox.getSelectedObject());
		this.filtersComboBox.add(filter, 0);

		this.filtersComboBox.setSelectedObject(filter);
	}

	public Filter addToObserver(){
//		Filter filter = FilterManager.theManager.getFilterCopy(this.filter);
		this.filterPoint.addFilter(filter);
		return filter;
	}
	
	public void removeFromObserver() {
		this.filterPoint.removeFilter(filter);
	}
	
}
