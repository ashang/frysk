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
import java.math.BigInteger;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererTextEvent;
import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.SimpleComboBox;
import frysk.proc.Task;

/**
 * The MemoryWindow displays the information stored at various locations in
 * memory, starting from the current program counter to a user-defined region.
 * The information can be displayed from 4-bit to 64-bit values, and in binary,
 * octal, decimal or hexadecimal format.
 * 
 * @author mcvet, ajocksch
 */
public class MemoryWindow
    extends Window
    implements Saveable
{

  private final int EIGHT_BIT = 0;
  private final int SIXTEEN_BIT = 1;
  private final int THIRTYTWO_BIT = 2;
  private final int SIXTYFOUR_BIT = 3;
  private final int LOC = 0; /* Memory address */
  private final int OBJ = 9; /* Object stored in above address */

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;
  
  public static String gladePath;

  private DataColumn[] cols = { 
  new DataColumnString(), /* memory location */
  new DataColumnString(), /* binary little endian */
  new DataColumnString(), /* binary big endian */
  new DataColumnString(), /* octal little endian */
  new DataColumnString(), /* octal big endian */
  new DataColumnString(), /* decimal little endian */
  new DataColumnString(), /* decimal big endian */
  new DataColumnString(), /* hexadecimal little endian */
  new DataColumnString(), /* hexadecimal big endian */
  new DataColumnObject()  /* memory object */
  };

  protected static String[] colNames = { "X-bit Binary (LE)",
                                        "X-bit Binary (BE)",
                                        "X-bit Octal (LE)", "X-bit Octal (BE)",
                                        "X-bit Decimal (LE)",
                                        "X-bit Decimal (BE)",
                                        "X-bit Hexadecimal (LE)",
                                        "X-bit Hexadecimal (BE)" };

  protected boolean[] colVisible = { true, false, false, false, false, false,
                                    false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[9];

  private MemoryFormatDialog formatDialog;

  private TreeView memoryView;

  private SpinButton fromSpin;
  private SpinButton toSpin;

  private Entry pcEntry;

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

  public MemoryWindow (LibGlade glade)
  {
    super(glade.getWidget("memoryWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new MemoryFormatDialog(this.glade);
    currentFormat = currentFormat + THIRTYTWO_BIT; /* Seems like a good default */
    this.fromSpin = (SpinButton) this.glade.getWidget("fromSpin");
    this.toSpin = (SpinButton) this.glade.getWidget("toSpin");
    this.pcEntry = (Entry) this.glade.getWidget("PCEntry");
    this.bitsCombo = new SimpleComboBox(
                                        (this.glade.getWidget("bitsCombo")).getHandle());
    this.model = new ListStore(cols);
    this.bitsList = new ObservableLinkedList();

    this.setIcon(IconManager.windowIcon);
  }
  
  public boolean hasTaskSet ()
  {
    return myTask != null;
  }

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
   * Assign this MemoryWindow to a task
   */
  public void setTask (Task myTask)
  {
    this.myTask = myTask;
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

    long pc_inc = myTask.getIsa().pc(myTask);
    long end = pc_inc + 20;
    this.fromSpin.setValue((double) pc_inc);
    this.toSpin.setValue((double) end);
    this.pcEntry.setText("" + pc_inc);

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
            if (bitsList.indexOf(bitsCombo.getSelectedObject()) == -1) {
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
          MemoryWindow.this.hideAll();
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
  
  /**
   * Recalculate the memory information based on a new bitsize and/or radix
   */
  public void recalculate ()
  {
    long start = (long) this.fromSpin.getValue();
    long end = (long) this.toSpin.getValue();
    this.lastKnownFrom = (double) start;
    this.lastKnownTo = (double) end;
    this.model.clear();

    memoryView.setModel(model);

    TreeViewColumn[] tvc = memoryView.getColumns();
    for (int i = 0; i < tvc.length; i++)
      {
        memoryView.removeColumn(tvc[i]);
      }

    for (long i = start; i < end + 1; i++)
      rowAppend(i);

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[LOC]);
    memoryView.appendColumn(col);

    for (int i = 0; i < 8; i++)
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

        boolean littleEndian = false;
        switch (i)
          {
          case 0:
            littleEndian = true; // fall through
          case 1:
            ((CellRendererText) renderer).addListener(new BinCellListener(
                                                                          littleEndian));
            break;
          case 2:
            littleEndian = true; // fall through
          case 3:
            ((CellRendererText) renderer).addListener(new OctCellListener(
                                                                          littleEndian));
            break;
          case 4:
            littleEndian = true; // fall through
          case 5:
            ((CellRendererText) renderer).addListener(new DecCellListener(
                                                                          littleEndian));
            break;
          case 6:
            littleEndian = true; // fall through
          case 7:
            ((CellRendererText) renderer).addListener(new HexCellListener(
                                                                          littleEndian));
            break;
          }

        col.packStart(renderer, false);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                                cols[i + 1]);

        memoryView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }
    this.refreshList();
  }

  /**
   * Refresh and update the display of addresses and values
   */
  private void refreshList ()
  {
    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    // update values in the columns if one of them has been edited
    ListStore model = (ListStore) this.memoryView.getModel();
    TreeIter iter = model.getFirstIter();

        while (iter != null)
          {

            BigInteger bi = new BigInteger(
                            (String) model.getValue(iter, 
                            (DataColumnObject) cols[OBJ]), 10);
            
            byte[] b = bi.toByteArray();
            String bin = "";
            String oct = "";
            String hex = "";
            
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

            /* Big endian first */
            model.setValue(iter, (DataColumnString) cols[2], bin);
            model.setValue(iter, (DataColumnString) cols[4], oct);
            model.setValue(iter, (DataColumnString) cols[6], bi.toString(10));
            model.setValue(iter, (DataColumnString) cols[8], "0x" + hex);
           
            /* Little endian second */
            bin = switchEndianness(bin, true);
            BigInteger bii = new BigInteger(bin, 2);
            
            b = bii.toByteArray();
            
            bin = bii.toString(2);
            oct = bii.toString(8);
            hex = bii.toString(16);
            
            model.setValue(iter, (DataColumnString) cols[1], bin);
            model.setValue(iter, (DataColumnString) cols[3], oct);
            model.setValue(iter, (DataColumnString) cols[5], bii.toString(10));
            model.setValue(iter, (DataColumnString) cols[7], "0x" + hex);

            iter = iter.getNextIter();
          }

    for (int i = 0; i < MemoryWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       MemoryWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
  }
  
  /**
   * Helper function for calculating memory information and putting it into
   * rows to be displayed
   */
  public void rowAppend (long i)
  {
    TreeIter iter = model.appendRow();
    model.setValue(iter, (DataColumnString) cols[LOC], "0x"
                                                       + Long.toHexString(i));
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

  /**
   * Switch the endianness of a binary string
   */
  private String switchEndianness (String toReverse, boolean littleEndian)
  {
    boolean neg = false;
    if (toReverse.charAt(0) == '-') {
      toReverse = toReverse.substring(1);
      neg = true;
    }
    
    int diff = toReverse.length() % 8;
    if (diff != 0)
      if (littleEndian)
        for (int i = 0; i < 8 - diff; i++)
          toReverse = toReverse + "0";
      else
        for (int i = 0; i < 8 - diff; i++)
          toReverse = "0" + toReverse;
    
    char[] tmp = new char[toReverse.length()];
    for (int i = 0; i < tmp.length; i+=8)
      for(int bitOffset = 0; bitOffset < 8; bitOffset++)
        tmp[i + bitOffset] = toReverse.charAt(toReverse.length() - i - (8 - bitOffset));

    if (neg)
      return ("-" +  new String(tmp));
    else
      return new String(tmp);
  }

  private void saveBinaryValue (String rawString, int radix,
                                boolean littleEndian, TreePath path)
  {
    long value = 0;
    String binaryString = "";
    // convert the data to little endian binary
    try
      {
        value = Long.parseLong(rawString, radix);
        binaryString = Long.toBinaryString(value);
      }
    // Invalid format, do nothing
    catch (NumberFormatException e)
      {
        return;
      }
    if (! littleEndian)
      binaryString = switchEndianness(binaryString, littleEndian);

    ListStore model = (ListStore) this.memoryView.getModel();
    TreeIter iter = model.getIter(path);

    binaryString = signExtend(binaryString,
                              (int) Math.pow(2, currentFormat + 3), 1);
    model.setValue(iter, (DataColumnString) cols[1],
                   binaryString);
  }

  private String signExtend (String unextended, int bitlength, int bitsPerChar)
  {
    int fullDigits = (bitlength / bitsPerChar);
    int digitsToAdd = fullDigits + (bitlength - fullDigits * bitsPerChar)
                      - unextended.length();

    for (int i = 0; i < digitsToAdd; i++)
      unextended = '0' + unextended;

    return unextended;
  }
  
  /****************************************************************************
   * SpinBox callback methods
   ***************************************************************************/

  public void handleFromSpin (double val)
  {

    if (val > this.lastKnownTo)
      {
        this.fromSpin.setValue(this.lastKnownTo);
        this.lastKnownFrom = this.lastKnownTo;
        return;
      }

    if (val > this.lastKnownFrom)
      {
        TreeIter iter = model.getFirstIter();

        for (int i = (int) lastKnownFrom; i < (int) val + 1; i++)
          {
            model.removeRow(iter);
            iter = iter.getNextIter();
          }
      }
    else
      {
        for (long i = (long) val; i < lastKnownTo + 1; i++)
          rowAppend(i);
      }
    refreshList();
    this.lastKnownFrom = val;
  }

  public void handleToSpin (double val)
  {

    if (val < this.lastKnownFrom)
      {
        this.toSpin.setValue(lastKnownFrom);
        this.lastKnownTo = this.lastKnownFrom;
        return;
      }

    if (val > this.lastKnownTo)
      {
        for (long i = (long) lastKnownTo + 1; i < val + 1; i++)
          rowAppend(i);
      }
    else
      {
        TreeIter iter = model.getFirstIter();
        for (int i = (int) this.fromSpin.getValue(); i < (int) val + 1; i++)
          iter = iter.getNextIter();

        while (iter != null)
          {
            model.removeRow(iter);
            iter = iter.getNextIter();
          }
      }

    this.lastKnownTo = val;
    refreshList();
  }
  
  /****************************************************************************
   * Save and Load
   ***************************************************************************/
  
  public void save (Preferences prefs)
  {
    this.formatDialog.save(prefs);
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    this.formatDialog.load(prefs);
    this.refreshList();
  }
 
  /****************************************************************************
   * Cell Listener Classes
   ***************************************************************************/
  
  class DecCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    public DecCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      MemoryWindow.this.saveBinaryValue(text, 10, littleEndian,
                                        new TreePath(arg0.getIndex()));
      MemoryWindow.this.refreshList();
    }

  }

  class HexCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    public HexCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      if (text.indexOf("0x") != - 1)
        text = text.substring(2);
      MemoryWindow.this.saveBinaryValue(text, 16, littleEndian,
                                        new TreePath(arg0.getIndex()));
      MemoryWindow.this.refreshList();
    }

  }

  class OctCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    public OctCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      MemoryWindow.this.saveBinaryValue(text, 8, littleEndian,
                                        new TreePath(arg0.getIndex()));
      MemoryWindow.this.refreshList();
    }

  }

  class BinCellListener
      implements CellRendererTextListener
  {

    boolean littleEndian;

    public BinCellListener (boolean littleEndian)
    {
      this.littleEndian = littleEndian;
    }

    public void cellRendererTextEvent (CellRendererTextEvent arg0)
    {
      String text = arg0.getText();

      MemoryWindow.this.saveBinaryValue(text, 2, littleEndian,
                                        new TreePath(arg0.getIndex()));
      MemoryWindow.this.refreshList();
    }

  }
  
}
