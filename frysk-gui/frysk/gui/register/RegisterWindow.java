package frysk.gui.register;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
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

/**
 * The RegisterWindow allows the display and editing of the names and values
 * of system registers. The values of the registers can be displayed in decimal,
 * hexadecimal, octal, or binary. Manipulating the registers is only possible
 * when the task is stopped, otherwise all functionality is disabled 
 * @author ajocksch
 *
 */
public class RegisterWindow extends Window implements CellRendererTextListener{
	
	private Task myTask;
	
	private LibGlade glade;
	
	private DataColumn[] cols = {new DataColumnString(), new DataColumnString(), new DataColumnString(), new DataColumnObject()};
	
	private static final int DECIMAL = 0;
	private static final int HEX = 1;
	private static final int OCTAL = 2;
	private static final int BINARY = 3;
	
	private int mode = DECIMAL;
	
	private boolean littleEndian = true;
	
	/**
	 * Creates a new RegistryWindow
	 * @param task The Task for which to display the registers
	 * @param glade The glade file for the register window
	 */
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
			model.setValue(iter, (DataColumnString) cols[1], ""+0);
			model.setValue(iter, (DataColumnString) cols[2], ""+0);
			model.setValue(iter, (DataColumnObject) cols[3], register);
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
		formatSelector.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent arg0) {
				switch(((ComboBox) arg0.getSource()).getActive()){
				case 0:
					mode = DECIMAL;
					break;
				case 1:
					mode = HEX;
					break;
				case 2: 
					mode = OCTAL;
					break;
				case 3:
					mode = BINARY;
					break;
				// do nothing on the default case
				default:
					return;
				}
				
				RegisterWindow.this.glade.getWidget("endianSelector").setSensitive(mode != DECIMAL);
				
				refreshList();
			}
		});
		
		ComboBox endianSelector = (ComboBox) this.glade.getWidget("endianSelector");
		endianSelector.setActive(0);
		endianSelector.setSensitive(false);
		endianSelector.addListener(new ComboBoxListener() {
			public void comboBoxEvent(ComboBoxEvent arg0) {
				littleEndian = (((ComboBox) arg0.getSource()).getActive() == 0);
				refreshList();
			}
		});
	}
	
	/**
	 * Sets whether the task is running or not, and if it is diable the
	 * widgets in the window
	 * @param running Whether the task is running
	 * TODO: Should we be listening to the Task for some sort of an event in this regard?
	 */
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

	/**
	 * When the value of a register is done being edited, save the value in the model
	 */
	public void cellRendererTextEvent(CellRendererTextEvent arg0) {
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();
		
		TreeIter edited = model.getIter(arg0.getIndex());
		
		String newText = arg0.getText();
		long actualValue = 0;
		
		if(mode != DECIMAL && !littleEndian){
			char[] tmp = new char[newText.length()];
			for(int i = 0; i < newText.length(); i++)
				tmp[newText.length() - i - 1] = newText.charAt(i);
			newText = new String(tmp);
		}
		
		// perform input validation
		if(this.mode == DECIMAL){
			try{
				actualValue = Long.parseLong(newText);
			}
			catch (Exception e){
				// invalid entry, do nothing
				return;
			}
		}
		else if(this.mode == HEX){
			try{
				if(newText.indexOf("0x") == 0)
					actualValue = Long.parseLong(newText.substring(2), 16);
				else
					actualValue = Long.parseLong(newText, 16);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
		}
		else if(this.mode == OCTAL){
			try{
				actualValue = Long.parseLong(newText, 8);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
		}
		else if(this.mode == BINARY){
			try{
				actualValue = Long.parseLong(newText, 2);
			}
			catch (Exception e){
				// bad input, do nothing
				return;
			}
		}
		
		// update the hidden column with the real value, then update the display
		model.setValue(edited, (DataColumnString) cols[2], ""+actualValue);
		this.refreshList();
	}
	
	/**
	 * Sign extends the given string to 32 bits. 
	 * @param unextended The unextended string
	 * @param bitsPerChar The number of bits per character (i.e. for hex this is 4, for binary it is 1)
	 * @return The extended string
	 * 
	 * TODO: Make this generic, so it no longer assumes 32 bit little-endian
	 */
	private String signExtend(String unextended, int bitlength, int bitsPerChar){
		int fullDigits = (bitlength / bitsPerChar);
		int digitsToAdd = fullDigits + (bitlength - fullDigits*bitsPerChar) - unextended.length();
		
		for(int i = 0; i < digitsToAdd; i++)
			unextended = '0' + unextended;
		
		return unextended;
	}
	
	/*
	 * Refreshes the view of the items in the list
	 */
	private void refreshList(){
		TreeView view = (TreeView) this.glade.getWidget("registerView");
		
		ListStore model = (ListStore) view.getModel();

		TreeIter iter = model.getFirstIter();
		while(iter != null){
			long value = Long.parseLong(model.getValue(iter, (DataColumnString) cols[2]));
		
			Register register = (Register) model.getValue(iter, (DataColumnObject) cols[3]);
			
			int bitlength = register.getLength()*4;
			
			String text = "";
			switch(this.mode){
			case BINARY:
				text = signExtend(Long.toBinaryString(value), bitlength, 1);
				break;
			case HEX:
				text = signExtend(Long.toHexString(value), bitlength, 4);
				break;
			case OCTAL:
				text = signExtend(Long.toOctalString(value), bitlength, 3);
				break;
			case DECIMAL:
				text = "" + value;
				break;
			}
			
			// flip the bits if we have to, but not in decimal mode
			if(!this.littleEndian && mode != DECIMAL){
				char[] tmp = new char[text.length()];
				for(int i = 0; i < text.length(); i++)
					tmp[tmp.length - i - 1] = text.charAt(i);
				text = new String(tmp);
			}
			
			if(this.mode == HEX)
				text = "0x" + text;
				
			
			model.setValue(iter, (DataColumnString) cols[1], text);
			iter = iter.getNextIter();
		}
		
		this.showAll();
	}
}
