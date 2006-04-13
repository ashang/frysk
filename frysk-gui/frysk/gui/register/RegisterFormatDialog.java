package frysk.gui.register;

import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.Saveable;

public class RegisterFormatDialog extends Dialog implements Saveable{
	
	private LibGlade glade;
	
	private DataColumn[] cols = {new DataColumnBoolean(), new DataColumnString()};
	
	private TreeView formatList;
	
	private Preferences prefs;
	
	public RegisterFormatDialog(LibGlade glade){
		super(glade.getWidget("formatDialog").getHandle());
		
		this.glade = glade;
		
		this.setIcon(IconManager.windowIcon);
		
		SizeGroup group = new SizeGroup(SizeGroupMode.HORIZONTAL);
		group.addWidget(this.glade.getWidget("formatUpButton"));
		group.addWidget(this.glade.getWidget("formatDownButton"));
		
		this.formatList = (TreeView) this.glade.getWidget("formatSelector");
		formatList.setHeadersVisible(false);
		
		final ListStore model = new ListStore(cols);
		
		for(int i = 0; i < RegisterWindow.colNames.length; i++){
			TreeIter iter = model.appendRow();
			
			model.setValue(iter, (DataColumnBoolean) cols[0], false);
			String text = RegisterWindow.colNames[i].replaceFirst("LE", "Little Endian");
			text = text.replaceFirst("BE", "Big Endian");
			model.setValue(iter, (DataColumnString) cols[1], text);
		}
		
		TreeViewColumn col = new TreeViewColumn();
		CellRenderer renderer = new CellRendererToggle();
		col.packStart(renderer, false);
		col.addAttributeMapping(renderer, CellRendererToggle.Attribute.ACTIVE, cols[0]);
		formatList.appendColumn(col);
		
		((CellRendererToggle) renderer).addListener(new CellRendererToggleListener() {
			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {
				TreePath path = new TreePath(arg0.getPath());
				
				TreeIter iter = model.getIter(path);
				
				boolean prev = model.getValue(iter, (DataColumnBoolean) cols[0]);
				model.setValue(iter, (DataColumnBoolean) cols[0], !prev);
			}
		});
		
		col = new TreeViewColumn();
		renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[1]);
		formatList.appendColumn(col);

		formatList.setModel(model);
		
		((Button) this.glade.getWidget("formatCloseButton")).addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if(arg0.isOfType(ButtonEvent.Type.CLICK)){
					RegisterFormatDialog.this.save(RegisterFormatDialog.this.prefs);
					RegisterFormatDialog.this.hideAll();
				}
			}
		});
		
		this.addListener(new LifeCycleListener() {
			public boolean lifeCycleQuery(LifeCycleEvent arg0) {
				if(arg0.isOfType(LifeCycleEvent.Type.DELETE)){
					RegisterFormatDialog.this.save(RegisterFormatDialog.this.prefs);
					RegisterFormatDialog.this.hideAll();
					return true;
				}
				return false;
			}
		
			public void lifeCycleEvent(LifeCycleEvent arg0) {}
		});
		
		((Button) this.glade.getWidget("formatUpButton")).addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if(arg0.isOfType(ButtonEvent.Type.CLICK))
					RegisterFormatDialog.this.moveColumnUp();
			}
		});
		
		((Button) this.glade.getWidget("formatDownButton")).addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if(arg0.isOfType(ButtonEvent.Type.CLICK))
					RegisterFormatDialog.this.moveColumnDown();
			}
		});
	}

	public void save(Preferences prefs) {
		ListStore model = (ListStore) this.formatList.getModel();
		
		TreeIter iter = model.getFirstIter();
		
		for(int i = 0; i < RegisterWindow.colNames.length; i++){
			boolean val = model.getValue(iter, (DataColumnBoolean) cols[0]);
			
			prefs.putBoolean(RegisterWindow.colNames[i], val);
			iter = iter.getNextIter();
		}
	}

	public void load(Preferences prefs) {
		this.prefs = prefs;
		ListStore model = (ListStore) this.formatList.getModel();
		
		TreeIter iter = model.getFirstIter();
		
		for(int i = 0; i < RegisterWindow.colNames.length; i++){
			boolean val = prefs.getBoolean(RegisterWindow.colNames[i], i == 0);
			
			model.setValue(iter, (DataColumnBoolean) cols[0], val);
			iter = iter.getNextIter();
		}
	}

	
	private void moveColumnUp(){
		
	}
	
	private void moveColumnDown(){
		
	}
}
