// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import java.util.Iterator;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
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
import frysk.gui.monitor.Saveable;
import frysk.proc.Isa;
import frysk.proc.Register;
import frysk.proc.Task;

/**
 * The RegisterWindow allows the display and editing of the names and values of
 * system registers. The values of the registers can be displayed in decimal,
 * hexadecimal, octal, or binary. Manipulating the registers is only possible
 * when the task is stopped, otherwise all functionality is disabled
 * 
 * @author ajocksch
 */
public class RegisterWindow
    extends Window
    implements Saveable
{

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  private DataColumn[] cols = { new DataColumnString(), // name
                               new DataColumnString(), // decimal le
                               new DataColumnString(), // decimal be
                               new DataColumnString(), // hex le
                               new DataColumnString(), // hex be
                               new DataColumnString(), // octal le
                               new DataColumnString(), // octal be
                               new DataColumnString(), // binary le
                               new DataColumnString(), // binary be
                               new DataColumnObject() }; // the Register object

  protected static String[] colNames = { "Decimal (LE)", "Decimal (BE)",
                                        "Hexadecimal (LE)", "Hexadecimal (BE)",
                                        "Octal (LE)", "Octal (BE)",
                                        "Binary (LE)", "Binary (BE)" };

  protected boolean[] colVisible = { true, false, false, false, false, false,
                                    false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[8];

  private RegisterFormatDialog formatDialog;

  private TreeView registerView;

  /**
   * Creates a new RegistryWindow
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
  }

  public boolean hasTaskSet ()
  {
    return myTask != null;
  }

  public void setTask (Task myTask)
  {
    
    this.myTask = myTask;

    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.registerView = (TreeView) this.glade.getWidget("registerView");

    ListStore model = new ListStore(cols);
    registerView.setModel(model);

    Isa isa = this.myTask.getIsa();
    Iterator registers = isa.RegisterIterator();

    while (registers.hasNext())
      {
        Register register = (Register) registers.next();
        TreeIter iter = model.appendRow();

        model.setValue(iter, (DataColumnString) cols[0], register.getName());
        model.setValue(iter, (DataColumnObject) cols[9], register);
        saveBinaryValue("" + register.get(this.myTask), 10, true,
                        iter.getPath());
      }

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Name");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[0]);
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
          RegisterWindow.this.hideAll();
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

  /**
   * Sign extends the given string to 32 bits.
   * 
   * @param unextended The unextended string
   * @param bitsPerChar The number of bits per character (i.e. for hex this is
   *          4, for binary it is 1)
   * @return The extended string TODO: Make this generic, so it no longer
   *         assumes 32 bit little-endian
   */
  private String signExtend (String unextended, int bitlength, int bitsPerChar)
  {
    int fullDigits = (bitlength / bitsPerChar);
    int digitsToAdd = fullDigits + (bitlength - fullDigits * bitsPerChar)
                      - unextended.length();

    for (int i = 0; i < digitsToAdd; i++)
      unextended = '0' + unextended;

    return unextended;
  }

  /*
   * Refreshes the view of the items in the list
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
        Register register = (Register) model.getValue(
                                                      iter,
                                                      (DataColumnObject) cols[9]);

        // reference everything from the little endian binary value
        String value = model.getValue(iter, (DataColumnString) cols[7]);
        long parsedValue = Long.parseLong(value, 2);

        // Decimal little endian
        model.setValue(iter, (DataColumnString) cols[1], "" + parsedValue);

        // Hex little endian
        model.setValue(iter, (DataColumnString) cols[3],
                       "0x"
                           + signExtend(Long.toHexString(parsedValue),
                                        register.getLength() * 4, 4));

        // Octal little endian
        model.setValue(iter, (DataColumnString) cols[5],
                       signExtend(Long.toOctalString(parsedValue),
                                  register.getLength() * 4, 3));

        // flip the binary string to do big-endian
        value = reverse(value);
        parsedValue = Long.parseLong(value, 2);

        // Binary big endian
        model.setValue(iter, (DataColumnString) cols[8],
                       signExtend(value, register.getLength() * 4, 1));

        // Decimal big-endian
        model.setValue(iter, (DataColumnString) cols[2], "" + parsedValue);

        // Hex big-endian
        model.setValue(iter, (DataColumnString) cols[4],
                       "0x"
                           + signExtend(Long.toHexString(parsedValue),
                                        register.getLength() * 4, 4));

        // Octal big-endian
        model.setValue(iter, (DataColumnString) cols[6],
                       signExtend(Long.toOctalString(parsedValue),
                                  register.getLength() * 4, 3));

        iter = iter.getNextIter();
      }

    // update which columns are shown
    for (int i = 0; i < RegisterWindow.colNames.length; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       RegisterWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
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

    ListStore model = (ListStore) this.registerView.getModel();
    TreeIter iter = model.getIter(path);
    Register register = (Register) model.getValue(iter,
                                                  (DataColumnObject) cols[9]);

    // TODO: Set the value of the register here

    binaryString = signExtend(binaryString, register.getLength() * 4, 1);
    model.setValue(iter, (DataColumnString) cols[7], binaryString);
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

      RegisterWindow.this.saveBinaryValue(text, 10, littleEndian,
                                          new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
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
      RegisterWindow.this.saveBinaryValue(text, 16, littleEndian,
                                          new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
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

      RegisterWindow.this.saveBinaryValue(text, 8, littleEndian,
                                          new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
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

      RegisterWindow.this.saveBinaryValue(text, 2, littleEndian,
                                          new TreePath(arg0.getIndex()));
      RegisterWindow.this.refreshList();
    }

  }
}
