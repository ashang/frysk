/*
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.ShadowType;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.pango.FontDescription;

public class StatusWidget extends VBox{

	Label nameLabel;
	private GuiData data;
	private TextView logTextView;
	public Observable notifyUser;
	
	public StatusWidget(ProcData data){
		super(false,0);
		FontDescription font = new FontDescription();
		this.notifyUser = new Observable();
		this.data = data;
		
		//========================================
		this.nameLabel = new Label(data.getProc().getCommand());
		HBox hbox1 = new HBox(false,0);
		this.nameLabel.setBooleanProperty("expand", false);
		hbox1.packStart(this.nameLabel, false, false, 5);
		hbox1.packStart(new Label(""), true, false, 0);
		this.packStart(hbox1, false, false, 0);
		//========================================
		
		//========================================
		this.logTextView = new TextView();
		ScrolledWindow logScrolledWindow = new ScrolledWindow();
		logScrolledWindow.add(logTextView);
		this.packStart(logScrolledWindow, true, true, 0);
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
		vbox.packStart(scrolledWindow, true, true, 0);
		this.add(vbox);
		//========================================

		this.showAll();
	}
	
	private TreeView initAttacheObserversTreeView(){
		TreeView treeView = new TreeView();
		final DataColumnString observersDC = new DataColumnString();
		DataColumn[] columns = new DataColumn[1];
		columns[0] = observersDC;
		final ListStore listStore = new ListStore(columns);
		System.out.println("Adding Observers to : " + ((ProcData)this.data).getProc().getPid() );
		this.data.observerAdded.addObserver(new Observer(){
			public void update(Observable observable, Object obj) {
				ObserverRoot observer = (ObserverRoot) obj;
				TreeIter iter = listStore.appendRow();
				listStore.setValue(iter, observersDC, observer.getName());
				System.out.println("ObserverAObserver-----------------------");
			}
		});
		
		this.data.observerRemoved.addObserver(new Observer(){
			public void update(Observable arg0, Object arg1) {
				System.out.println("ObserverRemovedObserver-----------------------");
			}
		});
		
		treeView.setModel(listStore);
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn observersCol = new TreeViewColumn();
		observersCol.packStart(cellRendererText, false);
		observersCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , observersDC);
		treeView.appendColumn(observersCol);
		
		return treeView;
	}
}
