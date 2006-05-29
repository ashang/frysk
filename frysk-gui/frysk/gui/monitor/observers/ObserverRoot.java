package frysk.gui.monitor.observers;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.CustomEvents;
import org.jdom.Element;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.Combo;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.SaveableXXX;
import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.actions.GenericActionPoint;
import frysk.gui.monitor.actions.LogAction;
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
			
		Runnable onAdded;
		Runnable onDeleted;
		
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
		
			this.returnAction = null;
			
			this.genericActionPoint = new GenericActionPoint("Generic Actions", "Actions that dont take any arguments" );
			this.addActionPoint(genericActionPoint);

			LogAction logAction = new LogAction();
			logAction.dontSaveObject();
			this.genericActionPoint.addAction(logAction);
		}
		
		public ObserverRoot(String name, String toolTip){
			super(name, toolTip);
			
			this.actions      = new ObservableLinkedList();
			this.info         = new String();
			this.filterPoints = new ObservableLinkedList();			
			this.actionPoints = new ObservableLinkedList();			
			this.baseName     = name;			
			
			this.returnAction = null;
			
			this.genericActionPoint = new GenericActionPoint("Generic Actions", "Actions that dont take any arguments" );
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
			
		public void addedTo (Object o) {
			if(this.onAdded != null){
				CustomEvents.addEvent(this.onAdded);
			}
		}

		public void deletedFrom (Object o) {
			if(this.onDeleted != null){
				CustomEvents.addEvent(this.onDeleted);
			}
		}

		public void addFailed (Object o, Throwable w) {
			throw new RuntimeException (w);
		}

		public void onAdded(Runnable r){
			this.onAdded = r;
		}
		
		public void onDeleted(Runnable r){
			this.onDeleted = r;
		}

		/**
		 * Could be called by an action during the update call to get
		 * print generic information about the event that just occurred
		 * format (as currently used by logger):
		 *   PID 123 did action ACTION on Host HOST
		 */
		public String getInfo() {
			return info;
		}
	
		protected String setInfo(String info) {
			return info;
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

		public void save(Element node) {
			super.save(node);
			
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
		
//		public String toString(){
//			String myString = "";
//			myString+= "Name: " + this.getName() + "\n";
//			myString+= "Base Name: " + this.getBaseName() + "\n";
//			myString+= "FilterPoints:" + "\n";
//			Iterator iter = this.filterPoints.iterator();
//			while (iter.hasNext()) {
//				FilterPoint filterPoint = (FilterPoint) iter.next();
//				myString+=  filterPoint + "\n";
//			}
//			
//			myString += "ActionPoints: \n";
//			iter = this.actionPoints.iterator();
//			while (iter.hasNext()) {
//				ActionPoint actionPoint = (ActionPoint) iter.next();
//				myString += actionPoint + "\n";
//			}
//			
//			return myString;
//		}

		public GuiObject getCopy() {
			return new ObserverRoot(this);
		}
		
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
		protected frysk.proc.Action getReturnAction(){
			
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
		
	}
