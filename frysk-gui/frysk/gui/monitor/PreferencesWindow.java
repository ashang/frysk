/*
 * Created on Oct 6, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.HashMap;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Frame;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

/**
 * The preference window. This also provides a place
 * to extend the perference window.
 * */
public class PreferencesWindow extends Window {
	
	private TreeView treeView;
	
	private TreeStore treeStore;
	
	private DataColumnString nameDC;
	private DataColumnObject widgetDC;
	
	private HashMap iterHashMap;
	
	private VBox prefsWidget;
	
	public PreferencesWindow(LibGlade glade){
		super(((Window)glade.getWidget("preferencesWindow")).getHandle());
		this.treeView  = (TreeView)glade.getWidget("prefsTreeView");
		this.prefsWidget = (VBox)glade.getWidget("prefsWidget");
		
		this.iterHashMap = new HashMap();
		
		DataColumn[] columns = new DataColumn[2];
		
		this.nameDC = new DataColumnString();
		this.widgetDC = new DataColumnObject();
		
		columns[0] = nameDC; 
		columns[1] = widgetDC;
		
		this.treeStore = new TreeStore(columns);
		TreeViewColumn nameCol = new TreeViewColumn();
		CellRendererText cellRendererText = new CellRendererText();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT ,this.nameDC);
		this.treeView.setModel(treeStore);
		this.treeView.appendColumn(nameCol);
		
		this.treeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent event) {
				if(treeView.getSelection().getSelectedRows().length > 0){
					TreePath selected = treeView.getSelection().getSelectedRows()[0];
					
					Frame widget = (Frame) treeStore.getValue(treeStore.getIter(selected), widgetDC);
					
					Widget widgets[] = prefsWidget.getChildren();
					for (int i = 0; i < widgets.length; i++) {
						prefsWidget.remove(widgets[i]);
					}
					
					prefsWidget.add(widget);
				}
			}
		});
		
		
		Button button = (Button) glade.getWidget("prefsOkButton");
		button.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.prefsWindow.hideAll();
				}
			}
		});
		
		this.hideAll();
	}
	
	/**
	 * Adds a new catagory page in the preference window
	 * @param name the name of the page to create.
	 */
	public void addPage(String path, PreferenceWidget page){
		TreeIter iter = this.treeStore.appendRow(null);
		String name = page.getLabel();
		
		if(this.iterHashMap.get(path) == null){
			this.iterHashMap.put(path, iter);
		}else{
			throw new IllegalArgumentException("Catagory " + name + " already exists");
		}
		
		this.treeStore.setValue(iter, this.nameDC, name);
		this.treeStore.setValue(iter, this.widgetDC, page);
	}
	
}
