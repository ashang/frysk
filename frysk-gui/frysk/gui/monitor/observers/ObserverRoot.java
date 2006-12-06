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

package frysk.gui.monitor.observers;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import org.gnu.glib.CustomEvents;
import org.jdom.Element;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.Combo;
import frysk.gui.monitor.EventLogger;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.SaveableXXX;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.actions.GenericActionPoint;
import frysk.gui.monitor.filters.Filter;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.proc.Action;
import frysk.proc.TaskObserver;

/**
 * A more sophisticated implementer of Observer.
 * provides name and tool tip strings for GUI display purposes.
 * Takes Action objects that can be used by clients to customize
 * behaviour. 
 */
public class ObserverRoot extends GuiObject implements TaskObserver, Observer, SaveableXXX{

		private ObservableLinkedList actions;
		private ObservableLinkedList runnables;
			
//		Runnable onAdded;
//		Runnable onDeleted;
		
		private String info;
		
		private ObservableLinkedList filterPoints;
		private ObservableLinkedList actionPoints;
		
		private final String baseName;
		
		public GenericActionPoint genericActionPoint;
		
		private frysk.proc.Action returnAction;
		
		public ObserverRoot(){
			super();
			
			this.actions      = new ObservableLinkedList();
			this.info         = new String();
			this.filterPoints = new ObservableLinkedList();			
			this.actionPoints = new ObservableLinkedList();			
			this.baseName     = "";			
		
			this.returnAction = Action.CONTINUE;
			
			this.genericActionPoint = new GenericActionPoint(" ", "Actions that dont take any arguments" );
			this.addActionPoint(genericActionPoint);

//          XXX: logging is done though setInfo
//          so this is not needed for the time being
//			LogAction logAction = new LogAction();
//			logAction.dontSaveObject();
//			this.genericActionPoint.addAction(logAction);
		}
		
		public ObserverRoot(String name, String toolTip){
			super(name, toolTip);
			
			this.actions      = new ObservableLinkedList();
			this.info         = new String();
			this.filterPoints = new ObservableLinkedList();			
			this.actionPoints = new ObservableLinkedList();			
			this.baseName     = name;			
			
			this.returnAction = Action.CONTINUE;
			
			this.genericActionPoint = new GenericActionPoint(" ", "Actions that dont take any arguments" );
			this.addActionPoint(genericActionPoint);

		}
		
		public ObserverRoot(ObserverRoot other) {
			super(other);

			this.actions      = new ObservableLinkedList(other.actions);
			this.info         = new String(other.info);
			this.filterPoints = new ObservableLinkedList(); // do not copy..			
			this.actionPoints = new ObservableLinkedList();	// ... because children will readd items
			this.baseName     = other.baseName;
			
			this.returnAction = other.returnAction;
			
			this.genericActionPoint = new GenericActionPoint(other.genericActionPoint);
			this.addActionPoint(genericActionPoint);

//			LogAction logAction = new LogAction();
//			logAction.dontSaveObject();
//			this.genericActionPoint.addAction(logAction);
		}

		public void update(Observable o, Object obj) {
			final Observable myObservable = o;
			final Object     myObj = obj;
			
			CustomEvents.addEvent(new Runnable(){
				public void run() {
					ListIterator iter = actions.listIterator();
					while(iter.hasNext()){
						ObserverRunnable runnable = (ObserverRunnable) iter.next();
						runnable.run(myObservable, myObj);
					}
				}
			});
		}
		
		/**
		 * Add and action to be performed when this observers
		 * update function is called.
		 * */
		public void addRunnable(ObserverRunnable action){
			this.runnables.add(action);
		}
			
//		public void addedTo (Object o) {
//			if(this.onAdded != null){
//				CustomEvents.addEvent(this.onAdded);
//			}
//		}
//
//		public void deletedFrom (Object o) {
//			if(this.onDeleted != null){
//				CustomEvents.addEvent(this.onDeleted);
//			}
//		}

		public void addFailed (Object o, Throwable w) {
			
			EventLogger.logAddFailed("addFailed(Object o, Throwable w)", o);
			throw new RuntimeException (w);
		}

//		public void onAdded(Runnable r){
//			this.onAdded = r;
//		}
//		
//		public void onDeleted(Runnable r){
//			this.onDeleted = r;
//		}

		/**
		 * Could be called by an action during the update call to get
		 * print generic information about the event that just occurred
		 * format (as currently used by logger):
		 *   PID 123 did action ACTION on Host HOST
		 */
		public String getInfo() {
			return info;
		}
	
        /**
         * Should be called whent the observer has fired. Every time
         * this is called it sends a message to the logs, so it should
         * only be called once every time the information changes
         * @param info info string to be set
         */
		protected void setInfo(String info) {
		  EventLogger.theLogger.getEventLogger().log(Level.INFO, info);
		  WindowManager.theManager.logWindow.print(new Date() + " " + info +"\n");
		  this.info = info;
		}
	
		protected void runActions(){
			this.genericActionPoint.runActions(this);
		}
		
		public ObservableLinkedList getFilterPoints(){
			return this.filterPoints;
		}
		
		public ObservableLinkedList getActionPoints(){
			return this.actionPoints;
		}
		
		protected void addFilterPoint(FilterPoint filterPoint){
			this.filterPoints.add(filterPoint);
		}
		
		protected void addActionPoint(ActionPoint actionPoint){
			this.actionPoints.add(actionPoint);
		}

		public String getBaseName() {
			return baseName;
		}

		private void saveReturnAction(Element node){
			if(this.getCurrentAction() == null){
				node.setAttribute("returnAction", "null");
				return;
			}
			if(this.getCurrentAction() == Action.BLOCK){
				node.setAttribute("returnAction", Action.BLOCK.toString());
				return;
			}
			if(this.getCurrentAction() == Action.CONTINUE){
				node.setAttribute("returnAction",Action.CONTINUE.toString());
				return;
			}			
		}
		
		private Action loadReturnAction(Element node){
			String actionString = node.getAttributeValue("returnAction");
			
			if(actionString.equals("null")){
				return null;
			}
			if(actionString.equals(Action.BLOCK.toString())){
				return Action.BLOCK;
			}
			if(actionString.equals(Action.CONTINUE.toString())){
				return Action.CONTINUE;
			}
			
			throw new RuntimeException("Error while loading observer: unkown action");
		}
		
		public void save(Element node) {
			super.save(node);
			
			this.saveReturnAction(node);
			
			//actions
			Element actionPointsXML = new Element("actionPoints");
			
			Iterator iterator = this.getActionPoints().iterator();
			while (iterator.hasNext()) {
				ActionPoint actionPoint = (ActionPoint) iterator.next();
				Element actionPointXML = new Element("actionPoint");
				actionPoint.save(actionPointXML);
				actionPointsXML.addContent(actionPointXML);
			}
			node.addContent(actionPointsXML);
			
			//filters
			
			Element filterPointsXML = new Element("filterPoints");
			
			iterator = this.getFilterPoints().iterator();
			while (iterator.hasNext()) {
				FilterPoint filterPoint = (FilterPoint) iterator.next();
				Element filterPointXML = new Element("filterPoint");
				filterPoint.save(filterPointXML);
				filterPointsXML.addContent(filterPointXML);
			}
			node.addContent(filterPointsXML);
			
		}

		public void load(Element node) {	
			super.load(node);

			this.setReturnAction(this.loadReturnAction(node));
			
			//actions
			Element actionPointsXML = node.getChild("actionPoints");
			List list = (List) (actionPointsXML.getChildren("actionPoint"));
			Iterator i = list.iterator();
			Iterator j = this.getActionPoints().iterator();
			
			while (i.hasNext()){
				((ActionPoint)j.next()).load(((Element) i.next()));
			}

			//filters
			Element filterPointsXML = node.getChild("filterPoints");
			list = (List) filterPointsXML.getChildren("filterPoint");
			i = list.iterator();
			j = this.getFilterPoints().iterator();
			
			while (i.hasNext()){
				((FilterPoint)j.next()).load(((Element) i.next()));
			}
		}
		
		public GuiObject getCopy() {
			return new ObserverRoot(this);
		}
		
        /**
         * Returns combo objects representing the currently applied
         * filters.
         * @see Combo.
         * @return List of combos.
         */
		public ObservableLinkedList getCurrentFilterCombos(){
			ObservableLinkedList combos = new ObservableLinkedList();
			
			Iterator i = this.getFilterPoints().iterator();
			while (i.hasNext()) {
				FilterPoint filterPoint = (FilterPoint) i.next();
				Iterator j = filterPoint.getItems().iterator();
				while (j.hasNext()) {
					Filter filter = (Filter) j.next();
					combos.add(new Combo(filterPoint, filter));
				}
			}
			return combos;
		}

        /**
         * Returns combo objects representing the currently applied
         * actions.
         * @see Combo.
         * @return List of combos.
         */
		public ObservableLinkedList getCurrentActionCombos() {
			ObservableLinkedList combos = new ObservableLinkedList();
			
			Iterator i = this.getActionPoints().iterator();
			while (i.hasNext()) {
				ActionPoint actionPoint = (ActionPoint) i.next();
				Iterator j = actionPoint.getItems().iterator();
				while (j.hasNext()) {
					frysk.gui.monitor.actions.Action action = (frysk.gui.monitor.actions.Action) j.next();
                    combos.add(new Combo(actionPoint, action));
				}
			}
			return combos;
		}

		/**
		 * Used to set which action is taken by the observer
		 * with respect to resuming execution of the observed
		 * thread after all the actions are executed.
		 * acceptable values are
		 * Action.BLOCK to block the process.
		 * Action.CONTINUE to continue the process.
		 * and null to pop-up a dialog and ask the user
		 * for action.
		 */
		public void setReturnAction(frysk.proc.Action action){
			this.returnAction = action;
		}
		
		/**
		 * Should be used by inheriting observers to get the
		 * desired Action with respect to stoping/resumeing
		 * execution of the observed thread.
		 * @return
		 */
		protected frysk.proc.Action whatActionShouldBeReturned(){
			if(this.returnAction != null){
				return this.returnAction;
			}else{
				if(DialogManager.showQueryDialog(this.getName() + ": would you like to resume thread execution ?")){
					return Action.CONTINUE;
				}else{
					return Action.BLOCK;
				}
			}
		}
		
		public frysk.proc.Action getCurrentAction(){
			return this.returnAction;
		}

      
        //XXX: These should really be abstract
        // they cannot be made abstract right now because
        // ObserverRoot cannot be made abstract because dummy
        // instances of it is needed. perhaps creating a class
        // DummyObserverRoot can solve that problem... sami
	  public void addedTo (Object observable){}
      public void deletedFrom (Object observable){}

//    abstract public void addedTo (Object observable);
//    abstract public void deletedFrom (Object observable);
		
	}
