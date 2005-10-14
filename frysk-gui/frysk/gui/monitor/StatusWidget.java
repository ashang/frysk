/*
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Frame;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.ShadowType;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.ObserverRunnable;

public class StatusWidget extends VBox{

	Label nameLabel;
	private GuiData data;
	private TextView logTextView;
	private Frame frame;
	
	public  Observable notifyUser;
	
	public StatusWidget(GuiData data){
		super(false,0);
		//FontDescription font = new FontDescription();
		this.notifyUser = new Observable();
		this.data = data;
		
		VBox mainVbox = new VBox(false, 0);
		
		//========================================
		frame = new Frame("");
		frame.add(mainVbox);
		this.add(frame);
//		Label label = (Label) frame.getLabelWidget();
//		label.setFont(new FontDescription());
		//========================================
		
		//========================================
		initLogTextView();
		ScrolledWindow logScrolledWindow = new ScrolledWindow();
		logScrolledWindow.add(logTextView);
		logScrolledWindow.setShadowType(ShadowType.IN);
		logScrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
		mainVbox.packStart(logScrolledWindow, true, true, 0);
		//========================================
		
		//========================================
		HBox hbox = new HBox(false, 0);
		VBox vbox = new VBox(false, 0);
		hbox.packStart(new Label("Attached Observers: "), false, false, 0);
		hbox.packStart(new Label(""), true, false, 0);
		vbox.packStart(hbox, false, false, 0);
		ScrolledWindow scrolledWindow = new ScrolledWindow();
		scrolledWindow.add(initAttacheObserversTreeView());
		scrolledWindow.setShadowType(ShadowType.IN);
		scrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
		vbox.packStart(scrolledWindow, true, true, 0);
		mainVbox.add(vbox);
		//========================================

		this.showAll();
	}
	
	private TreeView initAttacheObserversTreeView(){
		final TreeView treeView = new TreeView();
		final DataColumnString nameDC = new DataColumnString();
		final DataColumnObject observersDC = new DataColumnObject();
		DataColumn[] columns = new DataColumn[2];
		columns[0] = nameDC;
		columns[1] = observersDC;
		final ListStore listStore = new ListStore(columns);
		treeView.setHeadersVisible(false);
		
		// handle add evets
		this.data.observerAdded.addObserver(new Observer(){
			public void update(Observable observable, Object obj) {
				ObserverRoot observer = (ObserverRoot) obj;
				TreeIter iter = listStore.appendRow();
				listStore.setValue(iter, nameDC, observer.getName());
				listStore.setValue(iter, observersDC, observer);
			}
		});
		
		// handle remove evets
		this.data.observerRemoved.addObserver(new Observer(){
			public void update(Observable o, Object obj) {
				TreeIter iter = listStore.getFirstIter();
				ObserverRoot observer = (ObserverRoot)obj;
				ObserverRoot myObserver;
				while(iter != null){
					myObserver = (ObserverRoot) listStore.getValue(iter, observersDC);
					if(myObserver == observer){
						listStore.removeRow(iter);
						break;
					}
					iter = iter.getNextIter();
				}
			}
		});
		
		treeView.setModel(listStore);
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn observersCol = new TreeViewColumn();
		observersCol.packStart(cellRendererText, false);
		observersCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		treeView.appendColumn(observersCol);
		
		final Menu menu = new Menu();
		MenuItem item = new MenuItem("Remove", false);
		item.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent event) {
				TreePath path = (treeView.getSelection().getSelectedRows())[0];
				data.remove((ObserverRoot) listStore.getValue(listStore.getIter(path), observersDC));
			}
		});
		menu.add(item);
		menu.showAll();
		
		treeView.addListener(new MouseListener(){

			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.BUTTON_PRESS 
						& event.getButtonPressed() == MouseEvent.BUTTON3){
					if((treeView.getSelection().getSelectedRows()).length > 0){
						menu.popup();						
					}
                    return true;
				}
				return false;
			}
		});
		
		
		return treeView;
	}


	private void initLogTextView(){
		this.logTextView = new TextView();
		LinkedList observers = this.data.getObservers();
		ListIterator iter = observers.listIterator();
		while(iter.hasNext()){
			final ObserverRoot observer = (ObserverRoot) iter.next();
			observer.addRunnable(new ObserverRunnable(){
				public void run(Observable o, Object obj) {
					logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");					
				}
			});
		}
		
		this.data.observerAdded.addObserver(new Observer(){

			public void update(Observable arg0, Object obj) {
				final ObserverRoot observer = (ObserverRoot)obj;
				logTextView.getBuffer().insertText("Event: " + observer.getName() + " added\n");
				observer.addRunnable(new ObserverRunnable(){
					public void run(Observable o, Object obj) {
						logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");
					}
				});
			}
			
		});
		
		this.data.observerRemoved.addObserver(new Observer(){

			public void update(Observable arg0, Object obj) {
				ObserverRoot observer = (ObserverRoot)obj;
				logTextView.getBuffer().insertText("Event: " + observer.getName() + " removed\n");
			}
			
		});
		
	}
	
	public void setName(String name){
		this.frame.setLabel(name);
	}
}
