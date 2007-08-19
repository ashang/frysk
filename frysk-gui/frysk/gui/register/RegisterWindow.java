// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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


package frysk.gui.register;

import inua.eio.ByteOrder;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import java.math.BigInteger;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnDouble;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererTextEvent;
import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.common.IconManager;
import frysk.gui.common.UBigInteger;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.monitor.Saveable;

import frysk.proc.Isa;
import frysk.proc.Proc;
import frysk.proc.Task;

import frysk.stack.Register;
import frysk.stack.RegisterFactory;
import frysk.stack.StackFactory;
import frysk.stepping.TaskStepEngine;
import frysk.value.ArithmeticType;
import frysk.value.IntegerType;
import frysk.value.UnsignedType;
import frysk.value.FloatingPointType;
import frysk.value.Format;
import frysk.value.Value;


public class RegisterWindow
    extends Window
    implements Saveable
{
  public static String gladePath;
  
  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;
  
  private DataColumnString registerNameColumn = new DataColumnString();
  private DataColumnString decimalLittleEndianColumn = new DataColumnString();
  private DataColumnString decimalBigEndianColumn = new DataColumnString();
  private DataColumnString hexidecimalLittleEndianColumn = new DataColumnString();
  private DataColumnString hexidecimalBigEndianColumn = new DataColumnString();
  private DataColumnString octalLittleEndianColumn = new DataColumnString();
  private DataColumnString octalBigEndianColumn = new DataColumnString();
  private DataColumnString binaryLittleEndianColumn = new DataColumnString();
  private DataColumnString binaryBigEndianColumn = new DataColumnString();
  private DataColumnObject registerColumn = new DataColumnObject();
  private DataColumnDouble alignmentColumn = new DataColumnDouble();
  private DataColumnObject valueColumn = new DataColumnObject();
  private DataColumnObject typeColumn = new DataColumnObject();
  
  private DataColumn[] cols = new DataColumn[] {registerNameColumn,
				decimalLittleEndianColumn,
				decimalBigEndianColumn,
				hexidecimalLittleEndianColumn,
				hexidecimalBigEndianColumn,
				octalLittleEndianColumn,
				octalBigEndianColumn,
				binaryLittleEndianColumn,
				binaryBigEndianColumn,
				registerColumn,
				alignmentColumn,
				valueColumn,
				typeColumn }; 
  
  protected static String[] colNames = { "Decimal (LE)", "Decimal (BE)",
                                        "Hexadecimal (LE)", "Hexadecimal (BE)",
                                        "Octal (LE)", "Octal (BE)",
                                        "Binary (LE)", "Binary (BE)" };

  protected boolean[] colVisible = new boolean[cols.length];
  {
    java.util.Arrays.fill(colVisible, false);
    colVisible[2] = true;
  }

  private TreeViewColumn[] columns = new TreeViewColumn[8];

  private RegisterFormatDialog formatDialog;

  private TreeView registerView;
  
  private boolean RW_active = false;
  
  private Observable observable;
  
  private LockObserver lock;
  
  private boolean toggle = true;
  
  private boolean closed = false;

  /**
   * The RegisterWindow allows the display and editing of the names and values of
   * system registers. The values of the registers can be displayed in decimal,
   * hexadecimal, octal, or binary. Manipulating the registers is only possible
   * when the task is stopped, otherwise all functionality is disabled.
   * 
   * The RegisterWindow is created dynamically from the RegisterWindowFactory.
   * 
   * @param task The Task for which to display the registers
   * @param glade The glade file for the register window
   */
  public RegisterWindow (LibGlade glade)
  {
    super(glade.getWidget("registerWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new RegisterFormatDialog(this.glade);

    this.setIcon(IconManager.windowIcon);
    this.lock = new LockObserver();
  }
  
  /**
   * Initializes the Glade file, the RegisterWindow itself, adds listeners and
   * Assigns the Proc.
   * 
   * @param proc  The Proc to be examined by rw.
   */
  public void finishRegWin (Proc proc)
  {
    Preferences prefs = PreferenceManager.getPrefs();
    load(prefs.node(prefs.absolutePath() + "/register"));

    if (!hasTaskSet())
      {
        setIsRunning(false);
        setTask(proc.getMainTask());
      }
    else
      this.showAll();
    
    this.RW_active = true;
  }
  
  public void setObservable (Observable o)
  {
    this.observable = o;
  }

  /**
   * Check to see if the task to be examined has already been set.
   * 
   * @return    False if myTask is null, True otherwise.
   */
  public boolean hasTaskSet ()
  {
    return myTask != null;
  }

  /**
   * Sets the task to be examined by this RegisterWindow. Also initializes 
   * the ISA used and does most of the work setting up the initial members
   * used by this class.
   * 
   * @param myTask  The Task to be examined by this RegisterWindow.
   */
  public void setTask (Task myTask)
  {
    this.myTask = myTask;
    Isa isa;
    isa = this.myTask.getIsa();
    this.setTitle(this.getTitle() + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.registerView = (TreeView) this.glade.getWidget("registerView");

    ListStore model = new ListStore(cols);
    registerView.setModel(model);

    setValues(myTask, isa, model);

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Name");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, registerNameColumn);
    registerView.appendColumn(col);
    
    for (int i = 0; i < colNames.length; i++)
      {
        col = new TreeViewColumn();
        col.setTitle(colNames[i]);
        col.setReorderable(true);
        renderer = new CellRendererText();
        ((CellRendererText) renderer).setEditable(true);
        boolean littleEndian = false;
        switch (i)
          {
          case 0:
            littleEndian = true; // fall through
          case 1:
            ((CellRendererText) renderer).addListener(new DecCellListener(
                                                                          littleEndian));
            break;
          case 2:
            littleEndian = true; // fall through
          case 3:
            ((CellRendererText) renderer).addListener(new HexCellListener(
                                                                          littleEndian));
            break;
          case 4:
            littleEndian = true; // fall through
          case 5:
            ((CellRendererText) renderer).addListener(new OctCellListener(
                                                                          littleEndian));
            break;
          case 6:
            littleEndian = true; // fall through
          case 7:
            ((CellRendererText) renderer).addListener(new BinCellListener(
                                                                          littleEndian));
            break;
          }
        col.packStart(renderer, false);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                                cols[i + 1]);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.XALIGN, alignmentColumn);
        registerView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }

    registerView.setAlternateRowColor(true);

    this.formatDialog.addListener(new LifeCycleListener()
    {

      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          RegisterWindow.this.refreshList();
      }

    });

    ((Button) this.glade.getWidget("closeButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            RegisterWindow.this.observable.deleteObserver(lock);
            RegisterWindow.this.closed = true;
            RegisterWindow.this.hideAll();
          }
      }
    });

    ((Button) this.glade.getWidget("formatButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          RegisterWindow.this.formatDialog.showAll();
      }
    });

    this.refreshList();
  }

private void setValues(Task myTask, Isa isa, ListStore model) {
    
    Register[] registers = RegisterFactory.getRegisters(isa);
    
    for (int i = 0; i < registers.length; i++)
      {
        Register register = registers[i];
        TreeIter iter = model.appendRow();

        model.setValue(iter, registerNameColumn, register.name);
        model.setValue(iter, registerColumn, register);
        model.setValue(iter, alignmentColumn, 1.0);
        model.setValue(iter, typeColumn,
                       register.type);

        Value value = StackFactory.createFrame(myTask).getRegisterValue(register);
        saveBinaryValue(value, iter.getPath());
      }
}
  
  public void resetTask (Task task)
  {
    this.myTask = task;
    Isa isa;
    isa = this.myTask.getIsa();
    this.setTitle(this.getTitle() + this.myTask.getProc().getCommand() + " "
                  + this.myTask.getName());

    ListStore model = (ListStore) this.registerView.getModel();
    model.clear();

    setValues(task, isa, model);
    
    refreshList();
  }

  /**
   * Sets whether the task is running or not, and if it is diable the widgets in
   * the window
   * 
   * @param running Whether the task is running TODO: Should we be listening to
   *          the Task for some sort of an event in this regard?
   */
  public void setIsRunning (boolean running)
  {
    if (running)
      {
        this.glade.getWidget("registerView").setSensitive(false);
        this.glade.getWidget("formatSelector").setSensitive(false);
      }
    else
      {
        this.glade.getWidget("registerView").setSensitive(true);
        this.glade.getWidget("formatSelector").setSensitive(true);
      }
  }

  /**
   * Saves the new preferences of this window.
   * 
   * @param prefs   The preference node to be saved.
   */
  public void save (Preferences prefs)
  {
    this.formatDialog.save(prefs);
  }

  /**
   * Loads the saved preferences of this window.
   * 
   * @param prefs   The preference node used to load preferences.
   */
  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    this.formatDialog.load(prefs);
    this.refreshList();
  }

  private String stringUsingValue(Value value, ArithmeticType type, Format format)
  {
      if (value == null)
	  return null;
      return new Value(type, value.getLocation()).toPrint(format);
  }

  private void resetList ()
  {
    Isa isa;
    isa = this.myTask.getIsa();
    ListStore model = (ListStore) this.registerView.getModel();
    model.clear();
    
    setValues(myTask, isa, model);
    
    refreshList();
  }
  
  /**
   * Refreshes the TreeView displayed to the user with values grabbed from
   * the Register Objects in the model.
   */
  private void refreshList ()
  {
    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    // update values in the columns if one of them has been edited
    ListStore model = (ListStore) this.registerView.getModel();
    TreeIter iter = model.getFirstIter();

    while (iter != null)
      {
        // get the register
        Register register 
	  = (Register)model.getValue(iter,
				     registerColumn);
	Value value
	  = (Value)model.getValue(iter, valueColumn);
	
	ArithmeticType type;

	// XXX: This big/little re-formatting of the Register's type
	// could be pushed into Format.  XXX: Registers can be more
	// complex types such as union; this use of instanceof to
	// re-create the type with a specific byte-order isn't going
	// to scale.
	if (register.type instanceof IntegerType)
	    type = new UnsignedType(register.type.getSize(), ByteOrder.LITTLE_ENDIAN, register.type.getTypeIdFIXME(), register.type.toPrint(), false);
	else if (register.type instanceof FloatingPointType)
	    type = new FloatingPointType(register.type.getSize(), ByteOrder.LITTLE_ENDIAN, register.type.getTypeIdFIXME(), register.type.toPrint(), false);
	else
	    throw new RuntimeException("type botch");

	// Binary little endian
	model.setValue(iter, binaryLittleEndianColumn, 
		       stringUsingValue(value, type, Format.BINARY));
	// Decimal little endian
	model.setValue(iter, decimalLittleEndianColumn, 
		       stringUsingValue(value, type, Format.DECIMAL));
	// Hex little endian
	model.setValue(iter, hexidecimalLittleEndianColumn, 
		       stringUsingValue(value, type, Format.HEXADECIMAL));
	// Octal little endian
	model.setValue(iter, octalLittleEndianColumn, 
		       stringUsingValue(value, type, Format.OCTAL));

	//Switch endian-ness.	

	// XXX: This big/little re-formatting of the Register's type
	// could be pushed into Format.  XXX: Registers can be more
	// complex types such as union; this use of instanceof to
	// re-create the type with a specific byte-order isn't going
	// to scale.
	if (register.type instanceof IntegerType)
	    type = new UnsignedType(register.type.getSize(), ByteOrder.BIG_ENDIAN, register.type.getTypeIdFIXME(), register.type.toPrint(), false);
	else if (register.type instanceof FloatingPointType)
	    type = new FloatingPointType(register.type.getSize(), ByteOrder.BIG_ENDIAN, register.type.getTypeIdFIXME(), register.type.toPrint(), false);
	else
	    throw new RuntimeException("type botch");

	// Binary big endian
	model.setValue(iter, binaryBigEndianColumn, 
		       stringUsingValue(value, type, Format.BINARY));
        // Decimal big-endian
	model.setValue(iter, decimalBigEndianColumn,
		       stringUsingValue(value, type, Format.DECIMAL));
        // Hex big-endian
	model.setValue(iter, hexidecimalBigEndianColumn, 
		       stringUsingValue(value, type, Format.HEXADECIMAL));
        // Octal big-endian
	model.setValue(iter, octalBigEndianColumn,
		       stringUsingValue(value, type, Format.OCTAL));

        iter = iter.getNextIter();
      }

    // update which columns are shown
    for (int i = 0; i < RegisterWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       RegisterWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
  }

  /**
   * Reverse the byte order of an integer.
   * 
   * @param val The value to be reversed
   * @param bitLength   The number of bits in this value.
   */
  private static BigInteger swizzleByteOrder(BigInteger val, int bitLength)
  {
    int byteLength = bitLength / 8;
    byte[] valBytes = val.toByteArray();
    byte[] newbytes = new byte[byteLength];

    for (int i = 0; i < valBytes.length; i++)
      {
	newbytes[byteLength - 1 - i] = valBytes[i];
      }
    return new BigInteger(newbytes);
  }

  /**
   * Saves the incoming binary value to the model.
   * 
   * @param val The binary value to save
   * @param path    The path used to get the TreeIter from the model.
   */
  private void saveBinaryValue (Value val, TreePath path)
  {
    ListStore model = (ListStore) this.registerView.getModel();
    TreeIter iter = model.getIter(path);
    model.setValue (iter, valueColumn, val);
  }

  /**
   * Writes the incoming binary value to the Register Object for this Task.
   * 
   * @param val The binary value to write
   * @param path    The TreePath used to get the TreeIter from the model.
   */
  private void writeBinaryValue (BigInteger val, TreePath path)
  {
      //XXX: Implement frame.setRegister(Register reg, Value value)
//    ListStore model = (ListStore) this.registerView.getModel();
//    TreeIter iter = model.getIter(path);
//    Register register = (Register) model.getValue(iter,
//                                                  (DataColumnObject) cols[9]);
//    register.putBigInteger (myTask, val);
//    model.setValue (iter, (DataColumnObject)cols[11], val);
  }

  /**
   * Writes the incoming binary value to the Register Object for this Task.
   * 
   * If the string is not negative but the sign bit of the register
   * will be set, ensure that the BigInteger value is negative.
   * 
   * @param rawString   The String containing the value to write.
   * @param radix   The radix to convert rawString to.
   * @param littleEndian    The endianness of the incoming value.
   * @param path    The path used to get the TreeIter from this model.
   */
  private void writeBinaryValue (String rawString, int radix,
                                boolean littleEndian, TreePath path)
  {
    BigInteger value;
    ListStore model = (ListStore)registerView.getModel();
    TreeIter iter = model.getIter(path);
    Register register = (Register)model.getValue(iter,
						 registerColumn);
    int bitLength = register.type.getSize() * 8;

    try
      {
	value = new BigInteger(rawString, radix);
      }
    // Invalid format, do nothing XXX probably should throw some kind
    // of error
    catch (NumberFormatException e)
      {
        return;
      }
    if (!littleEndian)
      value = swizzleByteOrder(value, bitLength);
    value = UBigInteger.signExtend(value, bitLength);
    writeBinaryValue(value, path);
  }
  
  private void desensitize ()
  {
    this.registerView.setSensitive(false);
  }
  
  private void resensitize ()
  {
    this.registerView.setSensitive(true);
  }

  class DecCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    /**
     * Listens to the Decimal cells for changes to write to the Register Object.
     * @param littleEndian
     */
    public DecCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    /**
     * Calls to write the new value to the Register and refreshes the TreeView.
     * 
     * @param arg0  The argument to write.
     */
    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      RegisterWindow.this.writeBinaryValue(text, 10, littleEndian,
					   new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
    }

  }

  class HexCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    /**
     * Listens to the Hexadecimal cells for changes to write to the Register Object.
     * @param littleEndian
     */
    public HexCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    /**
     * Calls to write the new value to the Register and refreshes the TreeView.
     * 
     * @param arg0  The argument to write.
     */
    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      if (text.indexOf("0x") != - 1)
        text = text.substring(2);
      RegisterWindow.this.writeBinaryValue(text, 16, littleEndian,
					   new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
    }

  }

  class OctCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    /**
     * Listens to the Octal cells for changes to write to the Register Object.
     * @param littleEndian
     */
    public OctCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    /**
     * Calls to write the new value to the Register and refreshes the TreeView.
     * 
     * @param arg0  The argument to write.
     */
    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      RegisterWindow.this.writeBinaryValue(text, 8, littleEndian,
					   new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
    }

  }

  class BinCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    /**
     * Listens to the Binary cells for changes to write to the Register Object.
     * @param littleEndian
     */
    public BinCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    /**
     * Calls to write the new value to the Register and refreshes the TreeView.
     * 
     * @param arg0  The argument to write.
     */
    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      RegisterWindow.this.writeBinaryValue(text, 2, littleEndian,
					   new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
    }

  }
  
  /**
   * Returns the Task being examined by this Window.
   * 
   * @return myTask The Task being examined.
   */
  public Task getMyTask()
  {
    return this.myTask;
  }
  
  public boolean getClosed ()
  {
    return this.closed;
  }
  
  public void setClosed (boolean closed)
  {
    this.closed = closed;
  }
  
  /**
   * Returns this RegisterWindow's LockObserver.
   * 
   * @return lock This RegisterWindow's LockObserver
   */
  public LockObserver getLockObserver()
  {
    return this.lock;
  }
  
  /**
   * Local Observer class used to poke this window from RunState when
   * all the Tasks belonging to this window's Proc have been
   * blocked. These Tasks could have ben running, stepping, or neither
   * and were just blocked once to allow this window to finish
   * building. This observer is synchronized between this windowand
   * the Memory, Source, and Disassembly windows.
   */
  class LockObserver implements Observer
  {
    
    /**
     * Builtin Observer method - called whenever the Observable we're concerned
     * with - in this case the RunState - has changed.
     * 
     * @param o The Observable we're watching
     * @param arg An Object argument
     */
    public synchronized void update (Observable o, Object arg)
    {
      TaskStepEngine tse = (TaskStepEngine) arg;
      if (!tse.getState().isStopped())
        {
          if (! RW_active)
            {
              RegisterWindow.this.observable = o;
              finishRegWin(tse.getTask().getProc());
            }
          else
            return;
        }
      else
        {
          /* The argument is null; its used here as a toggle. If the toggle is
           * true, the window is sensitive and we set the toggle to false and 
           * desensitize the important widgets. Otherwise, set the toggle 
           * back to true, refresh the window information and resensitize it. */
          if (toggle)
            {
              CustomEvents.addEvent(new Runnable()
              {
                public void run ()
                {
                  toggle = false;
                  desensitize();
                }
              });
            }
          else
            {
              CustomEvents.addEvent(new Runnable()
              {
                public void run ()
                {
                  toggle = true;
                  resetList();
                  resensitize();
                }
              });
            }
        }
    }
    }
  }
  

