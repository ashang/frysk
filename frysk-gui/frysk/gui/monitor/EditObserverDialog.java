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

import org.gnu.gdk.Color;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Frame;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.StateType;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.TextBufferEvent;
import org.gnu.gtk.event.TextBufferListener;

import frysk.gui.common.dialogs.Dialog;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.proc.Action;

public class EditObserverDialog extends Dialog {

	private ObserverRoot observer;
	
	Entry observerNameEntry;
	TextView observerDescriptionTextView;
	TextBuffer observerDescBuffer;
	SimpleComboBox observerTypeComboBox;
	
	FiltersTable filtersTable;
	ActionsTable actionsTable;
	
	RadioButton resumeRadioButton;
	RadioButton stopRadioButton;
	RadioButton askMeRadioButton;
	
	EditObserverDialog(LibGlade glade){
		super(glade.getWidget("editObserverDialog").getHandle());
		
		Button button = (Button) glade.getWidget("editObserverCancelButton");
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					EditObserverDialog.this.hideAll();
				}
			}
		});
		
		button = (Button) glade.getWidget("editObserverOkButton");
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					filtersTable.apply();
					actionsTable.apply();
					EditObserverDialog.this.hideAll();
				}
			}
		});
		
		observerNameEntry = (Entry) glade.getWidget("observerNameEntry");
		observerNameEntry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if(event.isOfType(EntryEvent.Type.CHANGED)){
					observer.setName(observerNameEntry.getText());
				}
			}
		});
		
		observerDescriptionTextView = (TextView) glade.getWidget("observerDescriptionTextView");
		observerDescBuffer = new TextBuffer();
		
		observerDescriptionTextView.setBuffer(observerDescBuffer);
		observerDescBuffer.addListener(new TextBufferListener() {
			public void textBufferEvent(TextBufferEvent event) {
				if (event.isOfType(TextBufferEvent.Type.CHANGED))
				{
					observer.setToolTip(observerDescBuffer.getText(
							observerDescBuffer.getStartIter(),
							observerDescBuffer.getEndIter(),
							false));
				}
				
			}});
		
		observerTypeComboBox = new SimpleComboBox((glade.getWidget("observerTypeComboBox")).getHandle());
		observerTypeComboBox.watchLinkedList(ObserverManager.theManager.getBaseObservers());
		observerTypeComboBox.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent event) {
				ObserverRoot selected = (ObserverRoot) observerTypeComboBox.getSelectedObject();
				if(selected != null && !selected.getClass().equals(observer.getClass())){
					ObserverRoot newObserver = ObserverManager.theManager.getObserverCopy((TaskObserverRoot) selected);
					newObserver.setName(observerNameEntry.getText());
					newObserver.setToolTip(observerDescBuffer.getText(
							observerDescBuffer.getStartIter(),
							observerDescBuffer.getEndIter(),
							false));
		
					if(observerNameEntry.getText().length() == 0){
						newObserver.setName("NewObserver");
					}
					

					setObserver(newObserver);
					setName(newObserver);
					filtersTable.setObserver(newObserver);
					actionsTable.setObserver(newObserver);
				}
			}
		});
	
		resumeRadioButton = (RadioButton) glade.getWidget("resumeRadioButton");
		resumeRadioButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
				    //System.out.println(this + ": .buttonEvent() resumeRadioButton");
					observer.setReturnAction(Action.CONTINUE);
				}
			}
		});
		
		stopRadioButton = (RadioButton) glade.getWidget("stopRadioButton");
		stopRadioButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
				    //System.out.println(this + ": .buttonEvent() stopRadioButton");
					observer.setReturnAction(Action.BLOCK);
				}
			}
		});
		
		askMeRadioButton = (RadioButton) glade.getWidget("askMeRadioButton");
		askMeRadioButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
				    //System.out.println(this + ": .buttonEvent() askMeRadioButton");
					observer.setReturnAction(null);
				}
			}
		});
		
		this.filtersTable = new FiltersTable(glade.getWidget("observerFiltersTable").getHandle());
		this.actionsTable = new ActionsTable(glade.getWidget("observerActionsTable").getHandle());
		
		Frame frame = (Frame) glade.getWidget("observerFiltersFrame");
		frame.setBackgroundColor(StateType.NORMAL, Color.WHITE);
		frame.setBackgroundColor(StateType.ACTIVE, Color.WHITE);
		frame.setBackgroundColor(StateType.INSENSITIVE, Color.WHITE);
		frame.setBackgroundColor(StateType.SELECTED, Color.WHITE);
		frame.setBackgroundColor(StateType.NORMAL, Color.WHITE);

		frame.setBaseColor(StateType.NORMAL, Color.WHITE);
		frame.setBaseColor(StateType.ACTIVE, Color.WHITE);
		frame.setBaseColor(StateType.INSENSITIVE, Color.WHITE);
		frame.setBaseColor(StateType.SELECTED, Color.WHITE);
		frame.setBaseColor(StateType.NORMAL, Color.WHITE);
		frame.showAll();
	}
	
	private void setAll(ObserverRoot observer){
		this.setObserver(observer);
		this.setName(observer);
		this.setType(observer);
		this.setDescription(observer);
		this.setReturnAction(observer);
		this.filtersTable.setObserver(observer);
		this.actionsTable.setObserver(observer);
	}

	/**
	 * This is for creating a new observer.
	 * call getObserver() to get the new observer
	 * @see getObserver()
	 */
	public void editNewObserver(){
		this.observerTypeComboBox.setSensitive(true);
		this.setAll(new ObserverRoot());
	}
	
	public void editObserver(ObserverRoot observer){
		this.setAll(observer);
		
		this.observerTypeComboBox.setSensitive(false);

		if(observer.getClass().equals(ObserverRoot.class)){
			this.observerTypeComboBox.setSensitive(true);
		}
	}
	
	public ObserverRoot getObserver(){
		return this.observer;
	}
	
	private void setObserver(ObserverRoot observer){
		this.observer = observer;
	}
	
	private void setName(ObserverRoot observer){
		this.observerNameEntry.setText(observer.getName());
	}
	
	private void setDescription(ObserverRoot observer){
		this.observerDescBuffer.setText(observer.getToolTip());
	}
	
	private void setType(ObserverRoot oserver){
		this.observerTypeComboBox.setSelectedObject(null);
		Iterator iter = ObserverManager.theManager.getBaseObservers().iterator();
		while (iter.hasNext()) {
			GuiObject obj = (GuiObject) iter.next();
			if((obj.getClass().toString()).equals(oserver.getClass().toString())){
				this.observerTypeComboBox.setSelectedObject(obj);
			}
		}
	}
	
	private void setReturnAction(ObserverRoot observer){
		Action returnAction = observer.getCurrentAction();
		
		if(returnAction == null){
			this.askMeRadioButton.activate();
			return;
		}
		
		if(returnAction == Action.BLOCK){
			this.stopRadioButton.activate();
			return;
		}
		
		if(returnAction == Action.CONTINUE){
			this.resumeRadioButton.activate();
			return;
		}
		
	}
}
