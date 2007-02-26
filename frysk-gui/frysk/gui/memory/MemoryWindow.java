//This file is part of the program FRYSK.
//
//Copyright 2005, Red Hat Inc.
//
//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.
//
//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.


package frysk.gui.memory;

import java.util.prefs.Preferences;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
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
import org.gnu.gtk.Entry;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.gui.common.IconManager;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.SimpleComboBox;
import frysk.proc.Proc;
import frysk.proc.Task;

import lib.opcodes.Disassembler;
import lib.opcodes.OpcodesException;
import lib.opcodes.Instruction;

public class MemoryWindow
    extends Window
    implements Saveable
{

  private final int EIGHT_BIT = 0;

  private final int SIXTEEN_BIT = 1;

  private final int THIRTYTWO_BIT = 2;

  private final int SIXTYFOUR_BIT = 3;

  private final int LOC = 0; /* Memory address */

  private final int OBJ = 10; /* Object stored in above address */

  private final int BYTE_BITS = 8;

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  public static String gladePath;

  TreeIter lastIter = null;

  private DataColumn[] cols = { new DataColumnString(), /* memory location */
  new DataColumnString(), /* binary little endian */
  new DataColumnString(), /* binary big endian */
  new DataColumnString(), /* octal little endian */
  new DataColumnString(), /* octal big endian */
  new DataColumnString(), /* decimal little endian */
  new DataColumnString(), /* decimal big endian */
  new DataColumnString(), /* hexadecimal little endian */
  new DataColumnString(), /* hexadecimal big endian */
  new DataColumnString(), /* instruction */
  new DataColumnObject(), /* memory object */
  new DataColumnDouble() /* alignment field */
  };

  protected static String[] colNames = { "X-bit Binary (LE)",
                                        "X-bit Binary (BE)",
                                        "X-bit Octal (LE)", "X-bit Octal (BE)",
                                        "X-bit Decimal (LE)",
                                        "X-bit Decimal (BE)",
                                        "X-bit Hexadecimal (LE)",
                                        "X-bit Hexadecimal (BE)", "Instruction" };

  protected boolean[] colVisible = { true, false, false, false, false, false,
                                    false, false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[10];

  private MemoryFormatDialog formatDialog;

  private TreeView memoryView;

  private Disassembler diss;

  private SpinButton fromSpin;

  private SpinButton toSpin;

  private Entry pcEntryDec;

  private Entry pcEntryHex;

  private SimpleComboBox bitsCombo;

  private GuiObject eight;

  private GuiObject sixteen;

  private GuiObject thirtytwo;

  private GuiObject sixtyfour;

  private ObservableLinkedList bitsList;

  private ListStore model;

  private double lastKnownFrom;

  private double lastKnownTo;

  protected static int currentFormat = 0;
  
  private boolean MW_active = false;
  
  private Observable observable;
  
  private LockObserver lock;
  
  private boolean toggle = true;
  
  private boolean closed = false;

  /**
   * The MemoryWindow displays the information stored at various locations in
   * memory, starting from the current program counter to a user-defined region.
   * The information can be displayed from 4-bit to 64-bit values, and in binary,
   * octal, decimal or hexadecimal format.
   * 
   * The MemoryWindow is dynamically created through the MemoryWindowFactory.
   * 
   * @param glade   The glade file containing MemoryWindow widgets.
   */
  public MemoryWindow (LibGlade glade)
  {
    super(glade.getWidget("memoryWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new MemoryFormatDialog(this.glade);
    if (currentFormat == 0)
      currentFormat = currentFormat + THIRTYTWO_BIT; /* Seems like a good default */

    this.fromSpin = (SpinButton) this.glade.getWidget("fromSpin");
    this.toSpin = (SpinButton) this.glade.getWidget("toSpin");
    this.pcEntryDec = (Entry) this.glade.getWidget("PCEntryDec");
    this.pcEntryHex = (Entry) this.glade.getWidget("PCEntryHex");
    this.bitsCombo = new SimpleComboBox(
                                        (this.glade.getWidget("bitsCombo")).getHandle());
    this.model = new ListStore(cols);
    this.bitsList = new ObservableLinkedList();

    this.setIcon(IconManager.windowIcon);
    this.lock = new LockObserver();
  }
  
  /**
   * Initializes the Glade file, the MemoryWindow itself, adds listeners and
   * Assigns the Proc.
   * 
   * @param proc  The Proc to be examined by mw.
   */
  public void finishMemWin (Proc proc)
  {
    Preferences prefs = PreferenceManager.getPrefs();
    load(prefs.node(prefs.absolutePath() + "/memory"));

    if (!hasTaskSet())
      {
        setIsRunning(false);
        setTask(proc.getMainTask());
      }
    else
      showAll();
    
    MW_active = true;
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
   * Set the sensitivity of this window and the formatting window.
   *  
   * @param running True if the window is running, false otherwise.
   */
  public void setIsRunning (boolean running)
  {
    if (running)
      {
        this.glade.getWidget("memoryView").setSensitive(false);
        this.glade.getWidget("formatSelector").setSensitive(false);
      }
    else
      {
        this.glade.getWidget("memoryView").setSensitive(true);
        this.glade.getWidget("formatSelector").setSensitive(true);
      }
  }

  /**
   * Set the task for this MemoryWindow to examine. Initializes the 
   * Disassembler we use to grab instructions from memory, as well as
   * initialize values for critical members of this class. Also performs
   * the first refresh and display of information.
   * 
   * @param myTask The task to be examined.
   */
  public void setTask (Task myTask)
  {
    this.myTask = myTask;
    long pc_inc;
    
    this.diss = new Disassembler(myTask.getMemory());
    pc_inc = myTask.getIsa().pc(myTask);
    long end = pc_inc + 50;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.eight = new GuiObject("8", "");
    this.sixteen = new GuiObject("16", "");
    this.thirtytwo = new GuiObject("32", "");
    this.sixtyfour = new GuiObject("64", "");

    bitsList.add(EIGHT_BIT, eight);
    bitsList.add(SIXTEEN_BIT, sixteen);
    bitsList.add(THIRTYTWO_BIT, thirtytwo);
    bitsList.add(SIXTYFOUR_BIT, sixtyfour);

    this.bitsCombo.watchLinkedList(bitsList);
    this.bitsCombo.setSelectedObject((GuiObject) bitsList.get(currentFormat));

    this.bitsCombo.setActive(currentFormat + 1);

    this.memoryView = (TreeView) this.glade.getWidget("memoryView");

    this.bitsCombo.showAll();
    this.diss = new Disassembler(myTask.getMemory());
    this.fromSpin.setValue((double) pc_inc);
    this.toSpin.setValue((double) end);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[LOC]);
    memoryView.appendColumn(col);

    /* Replace the X in the title of the column with the bitsize to be displayed,
     * and then append the columns into the view. */
    for (int i = 0; i < this.columns.length - 1; i++)
      {
        col = new TreeViewColumn();
        col.setTitle(colNames[i].replaceFirst(
                                              "X",
                                              ""
                                                  + (int) Math.pow(
                                                                   2,
                                                                   currentFormat + 3)));

        col.setReorderable(true);
        renderer = new CellRendererText();
        ((CellRendererText) renderer).setEditable(false);

        col.packStart(renderer, false);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                                cols[i + 1]);

        memoryView.appendColumn(col);

        col.addAttributeMapping(renderer, CellRendererText.Attribute.XALIGN,
                                cols[11]);
        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }
    
    recalculate();

    memoryView.setAlternateRowColor(true);

    this.formatDialog.addListener(new LifeCycleListener()
    {

      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          MemoryWindow.this.refreshList();
      }

    });

    bitsCombo.addListener(new ComboBoxListener()
    {
      public void comboBoxEvent (ComboBoxEvent arg0)
      {
        if (arg0.isOfType(ComboBoxEvent.Type.CHANGED))
          {
            if (bitsList.indexOf(bitsCombo.getSelectedObject()) == - 1)
              {
                return;
              }
            currentFormat = bitsList.indexOf(bitsCombo.getSelectedObject());
            recalculate();
          }
      }
    });

    ((Button) this.glade.getWidget("closeButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            MemoryWindow.this.observable.deleteObserver(lock);
            MemoryWindow.this.closed = true;
            MemoryWindow.this.hideAll();
          }
      }
    });

    ((Button) this.glade.getWidget("formatButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          MemoryWindow.this.formatDialog.showAll();
      }
    });

    ((SpinButton) this.glade.getWidget("fromSpin")).addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          handleFromSpin(fromSpin.getValue());
      }
    });

    ((SpinButton) this.glade.getWidget("toSpin")).addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          handleToSpin(toSpin.getValue());
      }
    });

  }
  
  public void resetTask (Task task)
  {
    this.myTask = task;
    long pc_inc;
    
    this.diss = new Disassembler(myTask.getMemory());

    pc_inc = myTask.getIsa().pc(myTask);
    long end = pc_inc + 20;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());
    this.model.clear();
    this.fromSpin.setValue((double) pc_inc);
    this.toSpin.setValue((double) end);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));

    recalculate();

  }

  /*****************************************************************************
   * Calculation, memory reading, and information display methods
   ****************************************************************************/

  /**
   * Recalculate the memory information based on a new bitsize and/or radix,
   * and then update the display.
   */
  public void recalculate ()
  {
    long start = (long) this.fromSpin.getValue();
    long end = (long) this.toSpin.getValue();
    this.lastKnownFrom = (double) start;
    this.lastKnownTo = (double) end;
    this.model.clear();

    memoryView.setModel(model);

    /* Remove the existing information because we have to change it anyway. */
//    TreeViewColumn[] tvc = memoryView.getColumns();
//    for (int i = 0; i < tvc.length; i++)
//      {
//        memoryView.removeColumn(tvc[i]);
//      }

    for (long i = start; i < end + 1; i++)
      rowAppend(i, null);

    this.refreshList();
  }
  
  private void resetPCAndList ()
  {
    long pc_inc = 0;
    pc_inc = myTask.getIsa().pc(myTask);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));
    
    long diff = (long) this.toSpin.getValue() - (long) this.fromSpin.getValue();

    this.lastKnownFrom = pc_inc;
    this.lastKnownTo = (double) pc_inc + diff;
    
    this.fromSpin.setValue((double) pc_inc);
    this.toSpin.setValue(this.lastKnownTo);
    
    this.model.clear();
    
    for (long i = (long) lastKnownFrom; i < this.lastKnownTo + 1; i++)
      rowAppend(i, null);
    
    refreshList();
  }

  /**
   * Refresh and update the display of addresses and values from what is 
   * re-calculated and turned into BigIntegers.
   */
  private void refreshList ()
  {
    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    LinkedList instructionsList = null;

    try
      {
        instructionsList = diss.disassembleInstructions(
                                                        (long) this.lastKnownFrom,
                                                        (long) (this.lastKnownTo
                                                                - this.lastKnownFrom + 1));
      }
    catch (OpcodesException oe)
      {
        System.out.println(oe.getMessage());
      }

    Iterator li = instructionsList.listIterator(0);
    Instruction ins = (Instruction) li.next();

    // update values in the columns if one of them has been edited
    ListStore model = (ListStore) this.memoryView.getModel();
    TreeIter iter = model.getFirstIter();

    while (iter != null)
      {
        BigInteger bi = new BigInteger(
                                       (String) model.getValue(
                                                               iter,
                                                               (DataColumnObject) cols[OBJ]),
                                       10);
        String addr = new String((String) model.getValue(
                                        iter, (DataColumnString) cols[LOC])).substring(2);

        byte[] b = bi.toByteArray();
        String bin = "";
        String oct = "";
        String hex = "";
        String dec = "";

        if (bi.signum() < 0)
          {
            for (int i = 0; i < b.length; i++)
              {
                bin = bin + Integer.toBinaryString(b[i] & 0xff);
                oct = oct + Integer.toOctalString(b[i] & 0xff);
                hex = hex + Integer.toHexString(b[i] & 0xff);
              }
          }
        else
          {
            bin = bi.toString(2);
            oct = bi.toString(8);
            hex = bi.toString(16);
          }
        dec = bi.toString(10);

        int diff = bin.length() % BYTE_BITS;
        if (diff != 0)
          bin = padBytes(bin, false, diff);

        /* Little endian first */
        model.setValue(iter, (DataColumnString) cols[1], bin);
        model.setValue(iter, (DataColumnString) cols[3], oct);
        model.setValue(iter, (DataColumnString) cols[5], dec);
        model.setValue(iter, (DataColumnString) cols[7], "0x" + hex);

        /* Big endian second */
        String bin2 = switchEndianness(bin, true);
        BigInteger bii = new BigInteger(bin2, 2);

        oct = bii.toString(8);
        hex = bii.toString(16);

        /* Bad hack to get around weird BigInteger endian bug */
        if (bin2.equals(bin))
          dec = bi.toString(10);
        else
          dec = bii.toString(10);

        model.setValue(iter, (DataColumnString) cols[2], bin2);
        model.setValue(iter, (DataColumnString) cols[4], oct);
        model.setValue(iter, (DataColumnString) cols[6], dec);
        model.setValue(iter, (DataColumnString) cols[8], "0x" + hex);

        if (ins != null && Long.toHexString(ins.address).equals(addr))
          {
            model.setValue(iter, (DataColumnString) cols[9], ins.instruction);
            if (li.hasNext())
              ins = (Instruction) li.next();
            else
              ins = null;
          }
        else
          model.setValue(iter, (DataColumnString) cols[9], "");

        iter = iter.getNextIter();
      }

    for (int i = 0; i < MemoryWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       MemoryWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
  }

  /**
   * Helper function for calculating memory information and putting it into rows
   * to be displayed. By default append rows to the end; occasionally prepend rows
   * to the front.
   * 
   * @param i   The memory address from which information is read from and stored
   * in the model as an Object.
   * @param iter    The row in the TreeView.
   */
  public void rowAppend (long i, TreeIter iter)
  {
    if (iter == null)
      iter = model.appendRow();

    model.setValue(iter, (DataColumnString) cols[LOC], "0x"
                                                       + Long.toHexString(i));
    model.setValue(iter, (DataColumnDouble) cols[11], 1.0);

    switch (currentFormat)
      {
      case EIGHT_BIT:
        try
          {
            model.setValue(iter, (DataColumnObject) cols[OBJ],
                           "" + myTask.getMemory().getByte(i));
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }
        break;

      case SIXTEEN_BIT:
        try
          {
            model.setValue(iter, (DataColumnObject) cols[OBJ],
                           "" + myTask.getMemory().getShort(i));
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }
        break;

      case THIRTYTWO_BIT:
        try
          {
            model.setValue(iter, (DataColumnObject) cols[OBJ],
                           "" + myTask.getMemory().getInt(i));
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }
        break;

      case SIXTYFOUR_BIT:
        try
          {
            model.setValue(iter, (DataColumnObject) cols[OBJ],
                           "" + myTask.getMemory().getLong(i));
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }
        break;
      }

  }
  
  private void desensitize ()
  {
    this.memoryView.setSensitive(false);
    this.bitsCombo.setSensitive(false);
    this.fromSpin.setSensitive(false);
    this.toSpin.setSensitive(false);
  }
  
  private void resensitize ()
  {
    this.memoryView.setSensitive(true);
    this.bitsCombo.setSensitive(true);
    this.fromSpin.setSensitive(true);
    this.toSpin.setSensitive(true);
  }

  /*****************************************************************************
   * Endianness methods
   ****************************************************************************/

  /**
   * Pad this byte string with zeroes so that it is of proper size.
   * 
   * @param s   The String to pad zeroes to.
   * @param littleEndian    The endianness of the binary string.
   * @param diff    Eight minus this many bits need to be added.
   */
  private String padBytes (String s, boolean littleEndian, int diff)
  {

    if (littleEndian)
      for (int i = 0; i < BYTE_BITS - diff; i++)
        s = s + "0";
    else
      for (int i = 0; i < BYTE_BITS - diff; i++)
        s = "0" + s;

    return s;
  }

  /**
   * Switch the endianness of a binary string
   * 
   * @param toReverse   The String to converted
   * @param littleEndian    The endianness of this binary string.
   */
  private String switchEndianness (String toReverse, boolean littleEndian)
  {
    int diff = toReverse.length() % BYTE_BITS;

    /* The string isn't properly composed of bits yet */
    if (diff != 0)
      toReverse = padBytes(toReverse, littleEndian, diff);

    /* No need to switch this string, it'll be identical either way */
    if (toReverse.length() == BYTE_BITS)
      return toReverse;

    char[] tmp = new char[toReverse.length()];
    for (int i = 0; i < tmp.length; i += BYTE_BITS)
      for (int bitOffset = 0; bitOffset < BYTE_BITS; bitOffset++)
        tmp[i + bitOffset] = toReverse.charAt(toReverse.length() - i
                                              - (BYTE_BITS - bitOffset));

    return new String(tmp);
  }

  /*****************************************************************************
   * SpinBox callback methods
   ****************************************************************************/

  /**
   * When the 'From' SpinBox is changed, update the displayed information 
   * accordingly.
   * 
   * @param val The new value of the SpinBox.
   */
  public void handleFromSpin (double val)
  {

    if (this.model.getFirstIter() == null)
      return;
    
    if (val > this.lastKnownTo)
      {
        this.fromSpin.setValue(this.lastKnownTo);
        this.lastKnownFrom = this.lastKnownTo;
        return;
      }

    if (val > this.lastKnownFrom)
      {
        TreeIter iter = model.getFirstIter();

        for (int i = (int) lastKnownFrom; i < (int) val; i++)
          {
            model.removeRow(iter);
            iter = iter.getNextIter();
          }
      }
    else
      {
        for (long i = (long) val; i < lastKnownFrom; i++)
          {
            TreeIter newRow = model.prependRow();
            rowAppend(i, newRow);
          }
      }
    refreshList();
    this.lastKnownFrom = val;
  }

  /**
   * When the 'To' SpinBox value is changed, update the displayed information
   * accordingly. 
   * 
   * @param val The new value of the SpinBox.
   */
  public void handleToSpin (double val)
  {
    
    if (this.model.getFirstIter() == null)
      return;

    if (val < this.lastKnownFrom)
      {
        this.toSpin.setValue(lastKnownFrom);
        this.lastKnownTo = this.lastKnownFrom;
        return;
      }

    if (val > this.lastKnownTo)
      {
        for (long i = (long) lastKnownTo + 1; i < val + 1; i++)
          rowAppend(i, null);
      }
    else
      {
        TreeIter i = model.getFirstIter();
        while (i != null)
          i = i.getNextIter();

        TreeIter ii = model.getFirstIter();
        long j;
        for (j = (long) lastKnownFrom; j < (long) val; j++)
          ii = ii.getNextIter();

        for (; j < lastKnownTo; j++)
          {
            model.removeRow(ii);
            ii = ii.getNextIter();
          }
      }

    this.lastKnownTo = val;
    refreshList();
  }

  /****************************************************************************
   * Save and Load
   ***************************************************************************/

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
   * Returns this MemoryWindow's LockObserver.
   * 
   * @return lock This MemoryWindow's LockObserver
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
   * the Register, Source, and Disassembly windows.
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
      /* The argument is not null. We're only concerned with it here the very
       * first time we see it, because its used for this window's
       * initialization. Otherwise, ignore it. */
      if (arg != null)
        {
          if (! MW_active)
            {
              Task t = (Task) arg;
              MemoryWindow.this.observable = o;
              finishMemWin(t.getProc());
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
                  //refreshList();
                  resetPCAndList();
                  resensitize();
                }
              });
            }
        }
    }
  }

}
