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
import java.util.Observable;
import java.util.Observer;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
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
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.gui.common.IconManager;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.monitor.Saveable;
import frysk.proc.Proc;
import frysk.proc.Task;

import lib.opcodes.Disassembler;
import lib.opcodes.OpcodesException;
import lib.opcodes.Instruction;

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
  
  private Entry fromBox;
    
  private Entry toBox;

  private ListStore model;

  private double lastKnownFrom;

  private double lastKnownTo;
  
  private int numInstructions;
  
  private long pcOffset = 0;
  
  private long pc;
  
  private boolean DW_active = false;
  
  private Observable observable;
  
  private LockObserver lock;
  
  private TreePath lastPath;
  
  private boolean toggle = true;
  
  private boolean closed = false;
  
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
    this.fromBox = (Entry) this.glade.getWidget("fromBox");
    this.toBox = (Entry) this.glade.getWidget ("toBox");
    this.pcEntryDec = (Entry) this.glade.getWidget("PCEntryDec");
    this.pcEntryHex = (Entry) this.glade.getWidget("PCEntryHex");
    this.model = new ListStore(cols);
    
    this.lock = new LockObserver();
    this.DW_active = true;

    this.setIcon(IconManager.windowIcon);
  }
  
  /**
   * Initializes the Glade file, the DisassemblyWindow itself, adds listeners and
   * Assigns the Proc.
   * 
   * @param dw  The DisassemblyWindow to be initialized.
   * @param proc    The Proc to be examined by dw.
   */
  public void finishDisWin (Proc proc)
  {
    Preferences prefs = PreferenceManager.getPrefs();
    load(prefs.node(prefs.absolutePath() + "/disassembler"));

    if (!hasTaskSet())
      {
        setIsRunning(false);
        setTask(proc.getMainTask());
      }
    else
      this.showAll();
    
    return;
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
    this.diss = new Disassembler(myTask.getMemory());

    pc_inc = myTask.getIsa().pc(myTask);
    this.pc = pc_inc;
    //long end = pc_inc + 20;
    this.numInstructions = 50;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.disassemblerView = (TreeView) this.glade.getWidget("disassemblerView");

    this.diss = new Disassembler(myTask.getMemory());
    this.fromSpin.setValue((double) pc_inc);
    this.fromBox.setText(Long.toHexString(pc_inc));
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
          {
            DisassemblyWindow.this.observable.deleteObserver(lock);
            DisassemblyWindow.this.closed = true;
            DisassemblyWindow.this.hideAll();
          }
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
    
    this.fromSpin.addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          handleFromSpin(fromSpin.getValue());
      }
    });

    this.toSpin.addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
          handleToSpin(toSpin.getValue());
      }
    });
    
    this.fromBox.addListener(new EntryListener()
    {
      public void entryEvent (EntryEvent arg0)
      {
        if (arg0.getType() == EntryEvent.Type.CHANGED)
          {
            String str = arg0.getText();
            try
            {
              double d = (double) Long.parseLong(str, 16);
              //fromSpin.setValue(d);
              handleFromSpin(d);
            }
            catch (NumberFormatException nfe)
            {
              fromBox.setText(Long.toHexString((long) lastKnownFrom));
            }
          }
      }
    });

    this.toBox.addListener(new EntryListener()
    {
      public void entryEvent (EntryEvent arg0)
      {
        if (arg0.getType() == EntryEvent.Type.CHANGED)
          {
            {
              String str = arg0.getText();
              try
              {
                double d = (double) Long.parseLong(str, 16);
                //toSpin.setValue(d);
                handleToSpin(d);
              }
              catch (NumberFormatException nfe)
              {
                toBox.setText(Long.toHexString((long) lastKnownTo));
              }
            }
          }
      }
    });

  }
  
  public void resetTask (Task task)
  {
    this.myTask = task;
    long pc_inc;
    this.diss = new Disassembler(myTask.getMemory());
    pc_inc = myTask.getIsa().pc(myTask);
    this.pc = pc_inc;
    // long end = pc_inc + 20;
    this.numInstructions = 50;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());
    this.model.clear();
    this.fromSpin.setValue((double) pc_inc);
    this.lastKnownFrom = pc_inc;
    // this.toSpin.setValue((double) end);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));
    
    this.model.appendRow();
    this.lastPath = this.model.getFirstIter().getPath();
    for (long i = 1; i < this.numInstructions; i++)
      {
        this.model.appendRow();
        this.lastPath.next();
      }
    
    refreshList();
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

    this.model.appendRow();
    this.lastPath = this.model.getFirstIter().getPath();
    for (long i = 1; i < this.numInstructions; i++)
      {
        this.model.appendRow();
        this.lastPath.next();
      }
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

  private void resetPCAndList ()
  {
    long pc_inc = 0;
    pc_inc = myTask.getIsa().pc(myTask);
    this.pcEntryDec.setText("" + pc_inc);
    this.pcEntryHex.setText("0x" + Long.toHexString(pc_inc));
    
    this.lastKnownFrom = pc_inc;
    this.fromSpin.setValue((double) pc_inc);
   
    this.model.clear();
    
    this.model.appendRow();
    this.lastPath = this.model.getFirstIter().getPath();
    for (long i = 1; i < this.numInstructions; i++)
      {
        this.model.appendRow();
        this.lastPath.next();
      }
    
    refreshList();
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
            model.setValue(iter, (DataColumnObject) cols[3], ins);
            
            this.pcOffset += ins.address;
            
            if (li.hasNext())
              ins = (Instruction) li.next();
            else
              {
                this.toSpin.setValue((double) ins.address);
                this.toBox.setText(Long.toHexString(ins.address));
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
  public synchronized void rowAppend (long i, TreeIter iter)
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
    
    //for (int j = 0; j < this.numInstructions; j++)
    this.toToggle = true;
    while (ins != null && ins.address < i)
      {
        iter = model.appendRow();
        this.lastPath.next();
        if (ins != null)
          {
            model.setValue(iter, (DataColumnString) cols[1],
                           "<pc+" + (ins.address - this.pc) + ">: ");
            model.setValue(iter, (DataColumnString) cols[LOC],
                           "0x" + Long.toHexString(ins.address));
            model.setValue(iter, (DataColumnString) cols[2], ins.instruction);
            model.setValue(iter, (DataColumnObject) cols[3], ins);
            
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
    
    this.toSpin.setValue((double) ins.address);
    this.toBox.setText(Long.toHexString(ins.address));
    this.lastKnownTo = ins.address;
  }
  
  private void desensitize ()
  {
    this.disassemblerView.setSensitive(false);
    this.fromSpin.setSensitive(false);
    this.toSpin.setSensitive(false);
    this.fromBox.setSensitive(false);
    this.toBox.setSensitive(false);
  }
  
  private void resensitize ()
  {
    this.disassemblerView.setSensitive(true);
    this.fromSpin.setSensitive(true);
    this.toSpin.setSensitive(true);
    this.fromBox.setSensitive(true);
    this.toBox.setSensitive(true);
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
    
    if (this.model.getFirstIter() == null)
      return;

    if (val > this.lastKnownTo)
      {
        this.fromSpin.setValue(this.lastKnownTo);
        this.fromBox.setText(Long.toHexString((long) this.lastKnownTo));
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
            this.numInstructions++;
            model.prependRow();
          }
      }
    
    refreshList();
    this.lastKnownFrom = val;
    this.fromBox.setText(Long.toHexString((long) val));
  }

  boolean toToggle = false;
  
  /**
   * When the 'To' SpinBox is changed, update the displayed information 
   * accordingly.
   * 
   * @param val The new value of the SpinBox.
   */
  public synchronized void handleToSpin (double val)
  {

    if (toToggle == true)
      {
        this.toToggle = false;
        return;
      }

    if (this.model.getFirstIter() == null)
      return;

    if (val < this.lastKnownFrom)
      {
        this.toSpin.setValue(lastKnownFrom);
        this.toBox.setText(Long.toHexString((long) this.lastKnownFrom));
        this.lastKnownTo = this.lastKnownFrom;
        return;
      }

    if (val > this.lastKnownTo)
      {
        for (long i = (long) lastKnownTo + 1; i < val + 1; i++)
          ++this.numInstructions;
          
        rowAppend((long) val, null);

        return;
      }
    else
      {
        if (this.numInstructions < 1)
          return;

        this.toToggle = true;

        this.model.removeRow(this.model.getIter(this.lastPath));
        this.lastPath.previous();
        --this.numInstructions;
        
        Instruction ins = (Instruction) this.model.getValue(this.model.getIter(this.lastPath),
                                                (DataColumnObject) cols[OBJ]);
        
        this.toSpin.setValue((double) ins.address);
        this.toBox.setText(Long.toHexString(ins.address));
        
        while (ins.address > val)
          {
            model.removeRow(model.getIter(this.lastPath));
            this.lastPath.previous();
            ins = (Instruction) this.model.getValue(
                                                    this.model.getIter(this.lastPath),
                                                    (DataColumnObject) cols[OBJ]);
            --this.numInstructions;
          }
        
        refreshList();
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
  public Task getMyTask ()
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
   * Returns this DisassemblyWindow's LockObserver.
   * 
   * @return lock This DisassemblyWindow's LockObserver
   */
  public LockObserver getLockObserver ()
  {
    return this.lock;
  }
  
  /**
   * Local Observer class used to poke this window from RunState when
   * all the Tasks belonging to this window's Proc have been
   * blocked. These Tasks could have ben running, stepping, or neither
   * and were just blocked once to allow this window to finish
   * building. This observer is synchronized between this windowand
   * the Register, Source, and Memory windows.
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
          if (! DW_active)
            {
              Task t = (Task) arg;
              DisassemblyWindow.this.observable = o;
              finishDisWin(t.getProc());
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
                  resetPCAndList();
                  //refreshList();
                  resensitize();
                }
              });
            }
        }
    }
  }


}
