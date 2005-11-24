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

import java.util.HashMap;
import java.util.Iterator;

import org.gnu.glib.Handle;
import org.gnu.gtk.Button;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;

import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.gui.monitor.observers.ObserverRoot;

public class FilterWidget extends VBox{

	private ObserverRoot currentObserver;
	private HashMap widgets;

	public FilterWidget() {
		super(true, 3);
		this.init();
	}

	public FilterWidget(Handle handle) {
		super(handle);
		this.init();
	}
	
	private void init(){
		this.widgets = new HashMap();
		
		Button addButton = new Button("   Add    "); // looks better with the spaces :)
		
		addButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					addFilterLine(new FilterLine(currentObserver));
				}
			}
		});
		HBox hbox = new HBox(false, 0);
		
		hbox.packStart(new Label(""), true, true, 0);
		hbox.packStart(addButton, false, false, 0);
		this.packEnd(hbox, false, false, 0);

		this.showAll();	
	}
	
	public void setObserver(ObserverRoot newObserver){
		this.clear();
		this.currentObserver = newObserver;
		this.populateList();
		//this.addFilterLine(new FilterLine(this.currentObserver));
		
	}

	/**
	 * Go through the filters that are currently added to the observer
	 * and display it.
	 * */
	private void populateList(){
		Iterator i = this.currentObserver.getFilterPoints().iterator();
		while(i.hasNext()){
			FilterPoint filterPoint = (FilterPoint)i.next();
			Iterator j = filterPoint.getFilters().iterator();
			while (j.hasNext()) {
				Filter filter = (Filter) j.next();
				FilterLine filterLine = new FilterLine(currentObserver);
				filterLine.setSelection(filterPoint, filter);
				this.addFilterLine(filterLine);
			}
		}
	}
	
	private void addFilterLine(final FilterLine filterLine){
		HBox hbox = new HBox(false, 0);

		Button deleteButton = new Button("Delete");		
		deleteButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					deleteFilterLine(filterLine);
				}
			}
		});
		
		hbox.packStart(filterLine, true, false, 0);
		hbox.packStart(new Label("|-----------------|"), true, true, 0); //spacer
		hbox.packStart(deleteButton, false, false, 0);
		
		this.packStart(hbox, false, false, 0);
		
		this.widgets.put(filterLine, hbox);
		this.showAll();
	}
	
	private void deleteFilterLine(FilterLine filterLine){
		filterLine.removeFromObserver();
		Widget widget = (Widget) this.widgets.get(filterLine);
		this.remove(widget);
	}
	
	private void clear() {
		Iterator iter = this.widgets.values().iterator();
		while (iter.hasNext()) {
			this.remove((Widget)iter.next());
		}
		this.widgets.clear();
	}

}
