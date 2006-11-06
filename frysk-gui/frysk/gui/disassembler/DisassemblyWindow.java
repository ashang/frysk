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


package frysk.gui.disassembler;

import java.util.prefs.Preferences;
import java.util.LinkedList;
import java.util.Iterator;

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
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.Saveable;
import frysk.proc.Task;
import frysk.proc.TaskException;

import lib.opcodes.Disassembler;
import lib.opcodes.OpcodesException;
import lib.opcodes.Instruction;

/**
 * @author mcvet
 */
public class DisassemblyWindow
    extends Window
    implements Saveable
{

  private final int LOC = 0; /* Memory address */

  private final int OBJ = 3; /* Object stored in above address */

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  public static String gladePath;

  TreeIter lastIter = null;

  private DataColumn[] cols = { new DataColumnString(), /* memory location */
  new DataColumnString(), /* function offset */
  new DataColumnString(), /* instruction */
  new DataColumnObject(), /* memory object */
  };

  protected static String[] colNames = { "Location", "PC offset", "Instruction" };

  protected boolean[] colVisible = { true, true };

  private TreeViewColumn[] columns = new TreeViewColumn[3];

  private DisassemblyFormatDialog formatDialog;

  private TreeView disassemblerView;

  private Disassembler diss;

  private SpinButton fromSpin;

  private SpinButton toSpin;

  private Entry pcEntryDec;

  private Entry pcEntryHex;

  private ListStore model;

  private double lastKnownFrom;

  private double lastKnownTo;
  
  private int numInstructions;
  
  private long pcOffset = 0;
  
  private long pc;
  
  /**
   * The DisassmblyWindow, given a Task, will disassemble the instructions
   * and parameters for that task in memory and display them, as well as their
   * absolute address in memory and relative distance from the program counter.
   * 
   * @param glade   The glade file containing the widgets for this window.
   */
  public DisassemblyWindow (LibGlade glade)
  {
    super(glade.getWidget("disassemblyWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new DisassemblyFormatDialog(this.glade);

    this.fromSpin = (SpinButton) this.glade.getWidget("fromSpin");
    this.toSpin = (SpinButton) this.glade.getWidget("toSpin");
    this.pcEntryDec = (Entry) this.glade.getWidget("PCEntryDec");
    this.pcEntryHex = (Entry) this.glade.getWidget("PCEntryHex");
    this.model = new ListStore(cols);

    this.setIcon(IconManager.windowIcon);
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
        this.glade.getWidget("disassemblerView").setSensitive(false);
        this.glade.getWidget("formatSelector").setSensitive(false);
      }
    else
      {
        this.glade.getWidget("disassemblerView").setSensitive(true);
        this.glade.getWidget("formatSelector").setSensitive(true);
      }
  }

  /**
   * Set the Task for this window to examine. Initializes the Disassembler
   * used as the backend for this window, and assigns initial values to 
   * members of this class. Also refreshes the window itself, displaying 
   * information for the first time.
   * 
   * @param myTask  The task to be examined.
   */
  public void setTask (Task myTask)
  {
    this.myTask = myTask;
    long pc_inc;
    try
      {
        this.diss = new Disassembler(myTask.getMemory());

        pc_inc = myTask.getIsa().pc(myTask);
        this.pc = pc_inc;
      }
    catch (TaskException e)
      {
        // XXX What to do if there's an error?
        e.printStackTrace();
        return;
      }
    //long end = pc_inc + 20;
    this.numInstructions = 20;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.disassemblerView = (TreeView) this.glade.getWidget("disassemblerView");

    this.diss = new Disassembler(myTask.getMemory());
    this.fromSpin.setValue((double) pc_inc);
    this.lastKnownFrom = pc_inc;
    //this.toSpin.setValue((double) end);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));

    setUpColumns();

    disassemblerView.setAlternateRowColor(true);

    this.formatDialog.addListener(new LifeCycleListener()
    {

      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          DisassemblyWindow.this.refreshList();
      }

    });

    ((Button) this.glade.getWidget("closeButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          DisassemblyWindow.this.hideAll();
      }
    });

    ((Button) this.glade.getWidget("formatButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          DisassemblyWindow.this.formatDialog.showAll();
      }
    });
    
    ((Button) this.glade.getWidget("formatButton")).setSensitive(false);

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

  /*****************************************************************************
   * Calculation, memory reading, and information display methods
   ****************************************************************************/

  /**
   * Sets up the columns in the TreeView between the addresses given by the
   * SpinBoxes.
   */
  public void setUpColumns ()
  {
    this.model.clear();

    disassemblerView.setModel(model);

    TreeViewColumn[] tvc = disassemblerView.getColumns();
    for (int i = 0; i < tvc.length; i++)
      {
        disassemblerView.removeColumn(tvc[i]);
      }

    for (long i = 0; i < this.numInstructions; i++)
      this.model.appendRow();
      //rowAppend(i, null);

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle(colNames[LOC]);
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[LOC]);
    disassemblerView.appendColumn(col);

    for (int i = 0; i < this.columns.length - 1; i++)
      {
        col = new TreeViewColumn();
        col.setTitle(colNames[i + 1]);

        col.setReorderable(true);
        renderer = new CellRendererText();
        ((CellRendererText) renderer).setEditable(false);

        col.packStart(renderer, false);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                                cols[i + 1]);

        disassemblerView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }
    this.refreshList();
  }

  /**
   * Grabs information out of the Disassembler and updates the display
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
                                                        numInstructions);
      }
    catch (OpcodesException oe)
      {
        System.out.println(oe.getMessage());
      }

    Iterator li = instructionsList.listIterator(0);
    Instruction ins = (Instruction) li.next();

    // update values in the columns if one of them has been edited
    ListStore model = (ListStore) this.disassemblerView.getModel();
    TreeIter iter = model.getFirstIter();

    while (iter != null)
      {
        if (ins != null)
          {
            model.setValue(iter, (DataColumnString) cols[1],
                           "<pc+" + (ins.address - this.pc) + ">: ");
            model.setValue(iter, (DataColumnString) cols[LOC],
                           "0x" + Long.toHexString(ins.address));
            model.setValue(iter, (DataColumnString) cols[2], ins.instruction);
            
            this.pcOffset += ins.address;
            
            if (li.hasNext())
              ins = (Instruction) li.next();
            else
              {
                this.toSpin.setValue((double) ins.address);
                this.lastKnownTo = ins.address;
                ins = null;
              }
          }
        else
          model.setValue(iter, (DataColumnString) cols[1], "");

        model.setValue(iter, (DataColumnObject) cols[OBJ], ins);
        
        iter = iter.getNextIter();
      }

    for (int i = 0; i < DisassemblyWindow.colNames.length - 1; i++)
      this.columns[i].setVisible(this.prefs.getBoolean(
                                                       DisassemblyWindow.colNames[i],
                                                       this.colVisible[i]));

    this.showAll();
  }

  /**
   * Helper function for calculating memory information and putting it into rows
   * to be displayed.
   * By default append rows to the end; occasionally prepend rows to the front.
   * 
   * @param i   The address to be displayed
   * @param iter    The TreeIter representing the row to be added.
   */
  public void rowAppend (long i, TreeIter iter)
  {
//    if (iter == null)
//      iter = model.appendRow();
    
    LinkedList instructionsList = null;

    try
      {
        instructionsList = diss.disassembleInstructions(
                                                        (long) this.lastKnownTo,
                                                        numInstructions);
      }
    catch (OpcodesException oe)
      {
        System.out.println(oe.getMessage());
      }

    Iterator li = instructionsList.listIterator(0);
    Instruction ins = (Instruction) li.next();
    
    for (int j = 0; j < i; j++)
      {
        iter = model.appendRow();
        if (ins != null)
          {
            model.setValue(iter, (DataColumnString) cols[1],
                           "<pc+" + (ins.address - this.pc) + ">: ");
            model.setValue(iter, (DataColumnString) cols[LOC],
                           "0x" + Long.toHexString(ins.address));
            model.setValue(iter, (DataColumnString) cols[2], ins.instruction);
            
            if (li.hasNext())
              ins = (Instruction) li.next();
            else
              {
                this.toSpin.setValue((double) ins.address);
                this.lastKnownTo = ins.address;
                ins = null;
              }
          }
        else
          model.setValue(iter, (DataColumnString) cols[1], "");
      }
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
  public synchronized void handleFromSpin (double val)
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

        for (int i = (int) lastKnownFrom; i < (int) val; i++)
          {
            this.numInstructions--;
            model.removeRow(iter);
            iter = iter.getNextIter();
          }
      }
    else
      {
        for (long i = (long) val; i < lastKnownFrom; i++)
          {
            //TreeIter newRow = model.prependRow();
            //rowAppend(i, newRow);
            this.numInstructions++;
            model.prependRow();
            //model.clear();
            refreshList();
          }
      }
    //refreshList();
    this.lastKnownFrom = val;
  }

  /**
   * When the 'To' SpinBox is changed, update the displayed information 
   * accordingly.
   * 
   * @param val The new value of the SpinBox.
   */
  public synchronized void handleToSpin (double val)
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
          {
            //this.model.appendRow();
            this.numInstructions++;
          }
        rowAppend((long) (val - lastKnownTo), null);
        return;
      }
    else
      {
        return;
        /* We have a problem here because all the instructions are of a 
         * different size and we won't know exactly how many rows to take off simply
         * by the offset from the PC. */
        
//        TreeIter i = model.getFirstIter();
//        while (i != null)
//          i = i.getNextIter();
        
//        this.numInstructions--;
//
//        TreeIter i = this.model.getFirstIter();
//        long j;
//        int end = (int) ((val - 2) - this.lastKnownFrom);
//        for (j = 0; j < end; j++)
//          {
//            System.out.println("iterating " + j);
//            System.out.println("i is " + i);
//            i = i.getNextIter();
//          }
//
//        end = (int) (this.lastKnownTo - (val - 2));
//        for (j = 0; j < end; j++)
//          {
//            System.out.println("de-iterating " + j);
//            model.removeRow(i);
//            i = i.getNextIter();
//          }
//        
//        this.lastKnownTo = val;
        //refreshList();
      }
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

}
