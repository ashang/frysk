package frysk.gui.register;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.CellRendererTextEvent;
import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;

import frysk.proc.Isa;
import frysk.proc.Register;
import frysk.proc.Task;

public class RegisterWindow extends Window implements ComboBoxListener, CellRendererTextListener{
	
	private Task myTask;
	
	private LibGlade glade;
	
	private DataColumn[] cols = {new DataColumnString(), new DataColumnString(), new DataColumnString()};
	
	private static final int DECIMAL = 0;
	private static final int HEX = 1;
	private static final int OCTAL = 2;
	private static final int BINARY = 3;
	
	private int mode = DECIMAL;
	
	public RegisterWindow(Task task, LibGlade glade){
		super(glade.getWidget("registerWindow").getHandle());
		this.glade = glade;
		this.myTask = task;
		
		TreeView registerView = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = new ListStore(cols);
		
		Isa isa = this.myTask.getIsa();
		Iterator registers = isa.RegisterIterator();
		while(registers.hasNext()){
			Register register = (Register) registers.next();
			TreeIter iter = model.appendRow();
			
			model.setValue(iter, (DataColumnString) cols[0], register.getName());
			model.setValue(iter, (DataColumnString) cols[1], ""+1234567);
			model.setValue(iter, (DataColumnString) cols[2], ""+1234567);
		}
		
		registerView.setModel(model);
		
		TreeViewColumn col = new TreeViewColumn();
		col.setTitle("Name");
		CellRenderer renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[0]);
		registerView.appendColumn(col);
		
		col = new TreeViewColumn();
		col.setTitle("Value");
		renderer = new CellRendererText();
		((CellRendererText) renderer).setEditable(true);
		((CellRendererText) renderer).addListener(this);
		col.packStart(renderer, false);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[1]);
		registerView.appendColumn(col);
		
		registerView.setAlternateRowColor(true);
		
		this.showAll();
		
		ComboBox formatSelector = (ComboBox) this.glade.getWidget("formatSelector");
		formatSelector.setActive(0);
		formatSelector.addListener(this);
	}

	public void comboBoxEvent(ComboBoxEvent arg0) {
		ComboBox formatSelector = (ComboBox) this.glade.getWidget("formatSelector");
		
		switch(formatSelector.getActive()){
		case 0:
			setFormatToDecimal();
			break;
		case 1:
			setFormatToHex();
			break;
		case 2: 
			setFormatToOctal();
			break;
		case 3:
			setFormatToBinary();
			break;
		}
	}
	
	private void setFormatToDecimal(){
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();

		TreeIter iter = model.getFirstIter();
		while(iter != null){
			long value = Long.parseLong(model.getValue(iter, (DataColumnString) cols[2]));
			
			model.setValue(iter, (DataColumnString) cols[1], ""+value);
			iter = iter.getNextIter();
		}
		
		this.mode = DECIMAL;
		view.showAll();
	}
	
	private void setFormatToHex(){
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();

		TreeIter iter = model.getFirstIter();
		while(iter != null){
			long value = Long.parseLong(model.getValue(iter, (DataColumnString) cols[2]));
			
			model.setValue(iter, (DataColumnString) cols[1], "0x"+Long.toHexString(value));
			iter = iter.getNextIter();
		}
		
		this.mode = HEX;
		view.showAll();
	}
	
	private void setFormatToOctal(){
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();

		TreeIter iter = model.getFirstIter();
		while(iter != null){
			long value = Long.parseLong(model.getValue(iter, (DataColumnString) cols[2]));
			
			model.setValue(iter, (DataColumnString) cols[1], Long.toOctalString(value));
			iter = iter.getNextIter();
		}
		
		this.mode = OCTAL;
		view.showAll();
	}
	
	private void setFormatToBinary(){
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();

		TreeIter iter = model.getFirstIter();
		while(iter != null){
			long value = Long.parseLong(model.getValue(iter, (DataColumnString) cols[2]));
			
			model.setValue(iter, (DataColumnString) cols[1], Long.toBinaryString(value));
			iter = iter.getNextIter();
		}
		
		this.mode = BINARY;
		view.showAll();
	}
	
	public void setIsRunning(boolean running){
		if(running){
			this.glade.getWidget("registerView").setSensitive(false);
			this.glade.getWidget("formatSelector").setSensitive(false);
		}
		else{
			this.glade.getWidget("registerView").setSensitive(true);
			this.glade.getWidget("formatSelector").setSensitive(true);
		}
	}

	public void cellRendererTextEvent(CellRendererTextEvent arg0) {
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();
		
		TreeIter edited = model.getIter(arg0.getIndex());
		
		String newText = arg0.getText();
		long actualValue = 0;
		
		// perform input validation
		if(this.mode == DECIMAL){
			long value = 0;
			try{
				value = Long.parseLong(newText);
			}
			catch (Exception e){
				// invalid entry, do nothing
				return;
			}
			
			model.setValue(edited, (DataColumnString) cols[1], ""+value);
			actualValue = value;
		}
		else if(this.mode == HEX){
			long value = 0;
			try{
				if(newText.indexOf("0x") == 0)
					value = Long.parseLong(newText.substring(2), 16);
				else
					value = Long.parseLong(newText, 16);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
			
			model.setValue(edited, (DataColumnString) cols[1], "0x"+Long.toHexString(value));
			actualValue = value;
		}
		else if(this.mode == OCTAL){
			long value = 0;
			
			try{
				value = Long.parseLong(newText, 8);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
			
			model.setValue(edited, (DataColumnString) cols[1], Long.toOctalString(value));
			actualValue = value;
		}
		else if(this.mode == BINARY){
			long value = 0;
			
			try{
				value = Long.parseLong(newText, 2);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
			
			model.setValue(edited, (DataColumnString) cols[1], Long.toBinaryString(value));
			actualValue = value;
		}
		
		model.setValue(edited, (DataColumnString) cols[2], ""+actualValue);
	}
}
