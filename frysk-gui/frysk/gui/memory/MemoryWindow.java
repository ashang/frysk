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
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.Saveable;
import frysk.proc.Task;

/**
 * The MemoryWindow displays the information stored at various locations in
 * memory, starting from the current program counter to a user-defined region.
 * The information can be displayed from 4-bit to 64-bit values, and in binary,
 * octal, decimal or hexadecimal format.
 */
public class MemoryWindow
    extends Window
    implements Saveable
{
  
  private final int FOUR_BIT = 0;
  private final int EIGHT_BIT = 1;
  private final int SIXTEEN_BIT = 2;
  private final int THIRTYTWO_BIT = 3;
  private final int SIXTYFOUR_BIT = 4;
  
  private final int LOC = 0;    /* Memory address */
  private final int OBJ = 9;    /* Object stored in above address */

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  private DataColumn[][] cols = {
  { 
    new DataColumnString(), /* memory location */
    new DataColumnString(), /* 4-bit binary little endian*/
    new DataColumnString(), /* 4-bit binary big endian */
    new DataColumnString(), /* 4-bit octal little endian */
    new DataColumnString(), /* 4-bit octal big endian */
    new DataColumnString(), /* 4-bit decimal little endian */
    new DataColumnString(), /* 4-bit decimal big endian */
    new DataColumnString(), /* 4-bit hexadecimal little endian */
    new DataColumnString(), /* 4-bit hexadecimal big endian */
    new DataColumnObject()  /* memory object */
  },
  { 
    new DataColumnString(), /* memory location */
    new DataColumnString(), /* 8-bit binary little endian*/
    new DataColumnString(), /* 8-bit binary big endian */
    new DataColumnString(), /* 8-bit octal little endian */
    new DataColumnString(), /* 8-bit octal big endian */
    new DataColumnString(), /* 8-bit decimal little endian */
    new DataColumnString(), /* 8-bit decimal big endian */
    new DataColumnString(), /* 8-bit hexadecimal little endian */
    new DataColumnString(), /* 8-bit hexadecimal big endian */
    new DataColumnObject()  /* memory object */
  },
  {
    new DataColumnString(), /* memory location */
    new DataColumnString(), /* 16-bit binary little endian*/
    new DataColumnString(), /* 16-bit binary big endian */
    new DataColumnString(), /* 16-bit octal little endian */
    new DataColumnString(), /* 16-bit octal big endian */
    new DataColumnString(), /* 16-bit decimal little endian */
    new DataColumnString(), /* 16-bit decimal big endian */
    new DataColumnString(), /* 16-bit hexadecimal little endian */
    new DataColumnString(), /* 16-bit hexadecimal big endian */
    new DataColumnObject()  /* memory object */
  },
  {
    new DataColumnString(), /* memory location */
    new DataColumnString(), /* 32-bit binary little endian*/
    new DataColumnString(), /* 32-bit binary big endian */
    new DataColumnString(), /* 32-bit octal little endian */
    new DataColumnString(), /* 32-bit octal big endian */
    new DataColumnString(), /* 32-bit decimal little endian */
    new DataColumnString(), /* 32-bit decimal big endian */
    new DataColumnString(), /* 32-bit hexadecimal little endian */
    new DataColumnString(), /* 32-bit hexadecimal big endian */
    new DataColumnObject()  /* memory object */
  },
  {
    new DataColumnString(), /* memory location */
    new DataColumnString(), /* 64-bit binary little endian*/
    new DataColumnString(), /* 64-bit binary big endian */
    new DataColumnString(), /* 64-bit octal little endian */
    new DataColumnString(), /* 64-bit octal big endian */
    new DataColumnString(), /* 64-bit decimal little endian */
    new DataColumnString(), /* 64-bit decimal big endian */
    new DataColumnString(), /* 64-bit hexadecimal little endian */
    new DataColumnString(), /* 64-bit hexadecimal big endian */
    new DataColumnObject()  /* memory object */
  }
  };

  protected static String[][] colNames = {
  {
    "4-bit Binary (LE)", "4-bit Binary (BE)", "4-bit Octal (LE)", 
    "4-bit Octal (BE)", "4-bit Decimal (LE)", "4-bit Decimal (BE)",
    "4-bit Hexadecimal (LE)", "4-bit Hexadecimal (BE)"
  },
  {
    "8-bit Binary (LE)", "8-bit Binary (BE)", "8-bit Octal (LE)", 
    "8-bit Octal (BE)", "8-bit Decimal (LE)", "8-bit Decimal (BE)",
    "8-bit Hexadecimal (LE)", "8-bit Hexadecimal (BE)"
  },
  {
    "16-bit Binary (LE)", "16-bit Binary (BE)", "16-bit Octal (LE)", 
    "16-bit Octal (BE)", "16-bit Decimal (LE)", "16-bit Decimal (BE)",
    "16-bit Hexadecimal (LE)", "16-bit Hexadecimal (BE)"
  },
  {
    "32-bit Binary (LE)", "32-bit Binary (BE)", "32-bit Octal (LE)", 
    "32-bit Octal (BE)", "32-bit Decimal (LE)", "32-bit Decimal (BE)",
    "32-bit Hexadecimal (LE)", "32-bit Hexadecimal (BE)"
  },
  {
    "64-bit Binary (LE)", "64-bit Binary (BE)", "64-bit Octal (LE)", 
    "64-bit Octal (BE)", "64-bit Decimal (LE)", "64-bit Decimal (BE)",
    "64-bit Hexadecimal (LE)", "64-bit Hexadecimal (BE)"
  }
  };
  
  protected boolean[] colVisible = { true, false, false, false, false, 
                                     false, false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[41]; // 8 * 5 + location

  private MemoryFormatDialog formatDialog;

  private TreeView memoryView;
  
  private SpinButton fromSpin;
  private SpinButton toSpin;
  
  protected static int currentFormat = 0;

  public MemoryWindow (LibGlade glade)
  {
    super(glade.getWidget("memoryWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new MemoryFormatDialog(this.glade);
    currentFormat = currentFormat + THIRTYTWO_BIT; /* Seems like a good default */

    this.setIcon(IconManager.windowIcon);
  }

  public void setTask (Task myTask)
  {
    this.myTask = myTask;

    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.memoryView = (TreeView) this.glade.getWidget("memoryView");

    ListStore[] model = new ListStore[5];
    model[0] = new ListStore(cols[FOUR_BIT]);
    model[1] = new ListStore(cols[EIGHT_BIT]);
    model[2] = new ListStore(cols[SIXTEEN_BIT]);
    model[3] = new ListStore(cols[THIRTYTWO_BIT]);
    model[4] = new ListStore(cols[SIXTYFOUR_BIT]);
    memoryView.setModel(model[4]);

    long pc_inc = myTask.getIsa().pc(myTask);
    long end = (long) toSpin.getValue();
    
    this.fromSpin.setValue((double) pc_inc);

    for (int i = 0; i < 5; i++) {
    for (pc_inc = pc_inc + 1; pc_inc < end; pc_inc++)
      {
        TreeIter iter = model[i].appendRow();

        model[i].setValue(iter, (DataColumnString) cols[i][LOC], "0x"
                                                         + Long.toHexString(pc_inc));
        model[i].setValue(iter, (DataColumnObject) cols[i][OBJ],
                       "" + myTask.getMemory().getULong(pc_inc));
      }
    }

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[FOUR_BIT][LOC]);
    memoryView.appendColumn(col);

    int colCount = 0;
    for (int j = 0; j < 5; j++) 
      {
    for (int i = 0; i < 8; i++)
      {
        col = new TreeViewColumn();
        col.setTitle(colNames[j][i]);
        col.setReorderable(true);
        renderer = new CellRendererText();
        ((CellRendererText) renderer).setEditable(true);
        
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
                                cols[j][i + 1]);

        memoryView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[j][i], colVisible[i]));

        columns[colCount] = col;
        colCount++;
      }
    }

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
    
    ((SpinButton) this.glade.getWidget("fromSpinButton")).addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          MemoryWindow.this.formatDialog.showAll();
      }
    });
    
    ((SpinButton) this.glade.getWidget("toSpinButton")).addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          MemoryWindow.this.formatDialog.showAll();
        }
     });

    this.refreshList();
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
    while (iter != null)
      {
        String val = (String) model.getValue(iter, (DataColumnObject) cols[LOC][OBJ]);
        model.setValue(iter, (DataColumnString) cols[currentFormat][1],
                       Long.toBinaryString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[currentFormat][2],
                       Long.toOctalString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[currentFormat][3],
                       Long.toString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[currentFormat][4],
                       "0x" + Long.toHexString(new Long(val).longValue()));
        iter = iter.getNextIter();
      }
    for (int i = 0; i < MemoryWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       MemoryWindow.colNames[currentFormat][i],
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
    for (int i = 0; i < tmp.length; i++)
      tmp[i] = toReverse.charAt(toReverse.length() - i - 1);
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

    binaryString = signExtend(binaryString, (int) Math.pow(2, currentFormat + 1), 1);
    model.setValue(iter, (DataColumnString) cols[currentFormat][7], binaryString);
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
  
}
