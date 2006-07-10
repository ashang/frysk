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

  private DataColumn[][] cols = { { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 4-bit binary little endian */
  new DataColumnString(), /* 4-bit binary big endian */
  new DataColumnString(), /* 4-bit octal little endian */
  new DataColumnString(), /* 4-bit octal big endian */
  new DataColumnString(), /* 4-bit decimal little endian */
  new DataColumnString(), /* 4-bit decimal big endian */
  new DataColumnString(), /* 4-bit hexadecimal little endian */
  new DataColumnString(), /* 4-bit hexadecimal big endian */
  new DataColumnObject() /* memory object */
  }, { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 8-bit binary little endian */
  new DataColumnString(), /* 8-bit binary big endian */
  new DataColumnString(), /* 8-bit octal little endian */
  new DataColumnString(), /* 8-bit octal big endian */
  new DataColumnString(), /* 8-bit decimal little endian */
  new DataColumnString(), /* 8-bit decimal big endian */
  new DataColumnString(), /* 8-bit hexadecimal little endian */
  new DataColumnString(), /* 8-bit hexadecimal big endian */
  new DataColumnObject() /* memory object */
  }, { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 16-bit binary little endian */
  new DataColumnString(), /* 16-bit binary big endian */
  new DataColumnString(), /* 16-bit octal little endian */
  new DataColumnString(), /* 16-bit octal big endian */
  new DataColumnString(), /* 16-bit decimal little endian */
  new DataColumnString(), /* 16-bit decimal big endian */
  new DataColumnString(), /* 16-bit hexadecimal little endian */
  new DataColumnString(), /* 16-bit hexadecimal big endian */
  new DataColumnObject() /* memory object */
  }, { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 32-bit binary little endian */
  new DataColumnString(), /* 32-bit binary big endian */
  new DataColumnString(), /* 32-bit octal little endian */
  new DataColumnString(), /* 32-bit octal big endian */
  new DataColumnString(), /* 32-bit decimal little endian */
  new DataColumnString(), /* 32-bit decimal big endian */
  new DataColumnString(), /* 32-bit hexadecimal little endian */
  new DataColumnString(), /* 32-bit hexadecimal big endian */
  new DataColumnObject() /* memory object */
  }, { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 64-bit binary little endian */
  new DataColumnString(), /* 64-bit binary big endian */
  new DataColumnString(), /* 64-bit octal little endian */
  new DataColumnString(), /* 64-bit octal big endian */
  new DataColumnString(), /* 64-bit decimal little endian */
  new DataColumnString(), /* 64-bit decimal big endian */
  new DataColumnString(), /* 64-bit hexadecimal little endian */
  new DataColumnString(), /* 64-bit hexadecimal big endian */
  new DataColumnObject() /* memory object */
  } };

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

  private ListStore[] model;

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

    this.bitsList = new ObservableLinkedList();
    this.model = new ListStore[4];

    this.setIcon(IconManager.windowIcon);
  }

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

    this.bitsCombo.setActive(currentFormat);

    this.memoryView = (TreeView) this.glade.getWidget("memoryView");

    model[EIGHT_BIT] = new ListStore(cols[EIGHT_BIT]);
    model[SIXTEEN_BIT] = new ListStore(cols[SIXTEEN_BIT]);
    model[THIRTYTWO_BIT] = new ListStore(cols[THIRTYTWO_BIT]);
    model[SIXTYFOUR_BIT] = new ListStore(cols[SIXTYFOUR_BIT]);
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
            currentFormat = bitsList.indexOf(bitsCombo.getSelectedObject());
            recalculate();
            //refreshList();
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

  private void refreshList ()
  {

    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    // update values in the columns if one of them has been edited
    ListStore model = (ListStore) this.memoryView.getModel();
    TreeIter iter = model.getFirstIter();

    switch (currentFormat)
      {
      case EIGHT_BIT:
        while (iter != null)
          {
            String val = (String) model.getValue(
                                                 iter,
                                                 (DataColumnObject) cols[currentFormat][OBJ]);
            model.setValue(iter, (DataColumnString) cols[currentFormat][1],
                           Long.toBinaryString(new Long(val).byteValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][3],
                           Long.toOctalString(new Long(val).byteValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][5],
                           Long.toString(new Long(val).byteValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][7],
                           "0x" + Long.toHexString(new Long(val).byteValue()));

            // val = reverse(val);
            // model.setValue(iter, (DataColumnString) cols[currentFormat][2],
            // Long.toBinaryString(new Long(val).byteValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][4],
            // Long.toOctalString(new Long(val).byteValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][6],
            // Long.toString(new Long(val).byteValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][8],
            // "0x" + Long.toHexString(new Long(val).byteValue()));
            //            
            iter = iter.getNextIter();
          }
        break;

      case SIXTEEN_BIT:
        while (iter != null)
          {
            String val = (String) model.getValue(
                                                 iter,
                                                 (DataColumnObject) cols[currentFormat][OBJ]);
            model.setValue(iter, (DataColumnString) cols[currentFormat][1],
                           Long.toBinaryString(new Long(val).shortValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][3],
                           Long.toOctalString(new Long(val).shortValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][5],
                           Long.toString(new Long(val).shortValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][7],
                           "0x" + Long.toHexString(new Long(val).shortValue()));

            // val = reverse(val);
            // model.setValue(iter, (DataColumnString) cols[currentFormat][2],
            // Long.toBinaryString(new Long(val).shortValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][4],
            // Long.toOctalString(new Long(val).shortValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][6],
            // Long.toString(new Long(val).shortValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][8],
            // "0x" + Long.toHexString(new Long(val).shortValue()));
            //            
            iter = iter.getNextIter();
          }
        break;

      case THIRTYTWO_BIT:
        while (iter != null)
          {
            String val = (String) model.getValue(
                                                 iter,
                                                 (DataColumnObject) cols[currentFormat][OBJ]);
            model.setValue(iter, (DataColumnString) cols[currentFormat][1],
                           Long.toBinaryString(new Long(val).intValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][3],
                           Long.toOctalString(new Long(val).intValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][5],
                           Long.toString(new Long(val).intValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][7],
                           "0x" + Long.toHexString(new Long(val).intValue()));

            // val = reverse(val);
            // model.setValue(iter, (DataColumnString) cols[currentFormat][2],
            // Long.toBinaryString(new Long(val).intValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][4],
            // Long.toOctalString(new Long(val).intValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][6],
            // Long.toString(new Long(val).intValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][8],
            // "0x" + Long.toHexString(new Long(val).intValue()));

            iter = iter.getNextIter();
          }
        break;

      case SIXTYFOUR_BIT:
        while (iter != null)
          {
            String val = "";

            val = (String) model.getValue(
                                          iter,
                                          (DataColumnObject) cols[currentFormat][OBJ]);

            model.setValue(iter, (DataColumnString) cols[currentFormat][1],
                           Long.toBinaryString(new Long(val).longValue()));

            model.setValue(iter, (DataColumnString) cols[currentFormat][3],
                           Long.toOctalString(new Long(val).longValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][5],
                           Long.toString(new Long(val).longValue()));
            model.setValue(iter, (DataColumnString) cols[currentFormat][7],
                           "0x" + Long.toHexString(new Long(val).longValue()));
            // System.out.println("Before rev");
            // val = reverse(val);
            // System.out.println("After rev: " + val);
            //            
            // try {
            // model.setValue(iter, (DataColumnString) cols[currentFormat][2],
            // Long.toBinaryString(new Long(val).longValue()));
            // } catch (Exception e) { System.out.println(e.getMessage()); }
            //            
            // model.setValue(iter, (DataColumnString) cols[currentFormat][4],
            // Long.toOctalString(new Long(val).longValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][6],
            // Long.toString(new Long(val).longValue()));
            // model.setValue(iter, (DataColumnString) cols[currentFormat][8],
            // "0x" + Long.toHexString(new Long(val).longValue()));

            iter = iter.getNextIter();
          }
        break;
      }

    for (int i = 0; i < MemoryWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       MemoryWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
  }

  public void save (Preferences prefs)
  {
    this.formatDialog.save(prefs);
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    this.refreshList();
    this.formatDialog.load(prefs);
  }

  public boolean hasTaskSet ()
  {
    return myTask != null;
  }

  private String reverse (String toReverse)
  {
    char[] tmp = new char[toReverse.length()];
    int start = 0;
    if (toReverse.charAt(0) == '-')
      {
        tmp[0] = '-';
        start = 1;
      }
    for (int i = start; i < tmp.length; i+= 8)
      for(int bitOffset = 0; bitOffset < 8; bitOffset++)
        tmp[i+bitOffset] = toReverse.charAt(toReverse.length() + start - i - (8 - bitOffset));

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
      binaryString = reverse(binaryString);

    ListStore model = (ListStore) this.memoryView.getModel();
    TreeIter iter = model.getIter(path);

    binaryString = signExtend(binaryString,
                              (int) Math.pow(2, currentFormat + 3), 1);
    model.setValue(iter, (DataColumnString) cols[currentFormat][7],
                   binaryString);
  }

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

  private String signExtend (String unextended, int bitlength, int bitsPerChar)
  {
    int fullDigits = (bitlength / bitsPerChar);
    int digitsToAdd = fullDigits + (bitlength - fullDigits * bitsPerChar)
                      - unextended.length();

    for (int i = 0; i < digitsToAdd; i++)
      unextended = '0' + unextended;

    return unextended;
  }

  public void recalculate ()
  {
    long start = (long) this.fromSpin.getValue();
    long end = (long) this.toSpin.getValue();
    this.lastKnownFrom = (double) start;
    this.lastKnownTo = (double) end;

    memoryView.setModel(model[currentFormat]);

    TreeViewColumn[] tvc = memoryView.getColumns();
    for (int i = 0; i < tvc.length; i++)
      {
        memoryView.removeColumn(tvc[i]);
      }

    for (long i = start; i < end + 1; i++)
      {
        TreeIter iter = model[currentFormat].appendRow();

        model[currentFormat].setValue(
                                      iter,
                                      (DataColumnString) cols[currentFormat][LOC],
                                      "0x" + Long.toHexString(i));
        switch (currentFormat)
          {
          case EIGHT_BIT:
            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnObject) cols[EIGHT_BIT][OBJ],
                                          "" + myTask.getMemory().getUByte(i));
            break;

          case SIXTEEN_BIT:
            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnObject) cols[SIXTEEN_BIT][OBJ],
                                          "" + myTask.getMemory().getUShort(i));
            break;

          case THIRTYTWO_BIT:
            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnObject) cols[THIRTYTWO_BIT][OBJ],
                                          "" + myTask.getMemory().getUInt(i));
            break;

          case SIXTYFOUR_BIT:
            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnObject) cols[SIXTYFOUR_BIT][OBJ],
                                          "" + myTask.getMemory().getULong(i));
            break;
          }
      }

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[currentFormat][LOC]);
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
                                cols[currentFormat][i + 1]);

        memoryView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }
    this.refreshList();
  }

  public void handleFromSpin (double val)
  {

    if (val > this.lastKnownFrom)
      {
        TreeIter iter = model[currentFormat].getFirstIter();

        for (int i = (int) lastKnownFrom; i < (int) val + 1; i++)
          {
            model[currentFormat].removeRow(iter);
            iter = iter.getNextIter();
          }
      }
    else
      {

        for (int i = (int) val; i < lastKnownTo + 1; i++)
          {
            TreeIter iter = model[currentFormat].appendRow();

            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnString) cols[currentFormat][LOC],
                                          "0x" + Long.toHexString(i));
            switch (currentFormat)
              {
              case EIGHT_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[EIGHT_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getUByte(
                                                                                i));
                break;

              case SIXTEEN_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[SIXTEEN_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getUShort(
                                                                                 i));
                break;

              case THIRTYTWO_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[THIRTYTWO_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getUInt(
                                                                               i));
                break;

              case SIXTYFOUR_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[SIXTYFOUR_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getULong(
                                                                                i));
                break;
              }
          }

      }
    refreshList();
    this.lastKnownFrom = val;
  }

  public void handleToSpin (double val)
  {

    if (val > this.lastKnownTo)
      {

        for (int i = (int) lastKnownTo + 1; i < val + 1; i++)
          {

            TreeIter iter = model[currentFormat].appendRow();

            model[currentFormat].setValue(
                                          iter,
                                          (DataColumnString) cols[currentFormat][LOC],
                                          "0x" + Long.toHexString(i));
            switch (currentFormat)
              {
              case EIGHT_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[EIGHT_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getByte(
                                                                               i));
                break;

              case SIXTEEN_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[SIXTEEN_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getShort(
                                                                                i));
                break;

              case THIRTYTWO_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[THIRTYTWO_BIT][OBJ],
                                              "" + myTask.getMemory().getInt(i));
                break;

              case SIXTYFOUR_BIT:
                model[currentFormat].setValue(
                                              iter,
                                              (DataColumnObject) cols[SIXTYFOUR_BIT][OBJ],
                                              ""
                                                  + myTask.getMemory().getLong(
                                                                               i));
                break;
              }
          }
      }
    else
      {
        TreeIter iter = model[currentFormat].getFirstIter();
        for (int i = (int) this.fromSpin.getValue(); i < (int) val + 1; i++)
          iter = iter.getNextIter();

        while (iter != null)
          {
            model[currentFormat].removeRow(iter);
            iter = iter.getNextIter();
          }
      }

    this.lastKnownTo = val;
    refreshList();
  }

}
