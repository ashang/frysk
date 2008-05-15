// This file is part of the program FRYSK.
// 
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
// Copyright 2007 Oracle Corporation.
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


package frysk.gui.disassembler;

import java.util.prefs.Preferences;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
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
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;

import frysk.dwfl.DwflCache;
import frysk.gui.common.IconManager;
import frysk.gui.dialogs.WarnDialog;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.SimpleComboBox;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.stepping.TaskStepEngine;
import frysk.proc.MemoryMap;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;

import lib.dwfl.Disassembler;
import lib.dwfl.Instruction;

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

  private Entry fromBox;
    
  private Entry toBox;

  private ListStore model;

  private double lastKnownFrom;

  private double lastKnownTo;
  
  private int numInstructions = 50;
  
  private long pcOffset = 0;
  
  private long pc;
  
  private boolean DW_active = false;
  
  private Observable observable;
  
  private LockObserver lock;
  
  private TreePath lastPath;
  
  private boolean toggle = true;
  
  private boolean closed = false;
  
  private MemoryMap[] mmaps;
  
  private SimpleComboBox segmentCombo;
  
  private ObservableLinkedList segmentList;
  
  private int segmentIndex = 0;
  
  private int row = numInstructions*3;
  
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
    this.segmentCombo = new SimpleComboBox(
	    (this.glade.getWidget("segmentCombo")).getHandle());
    this.model = new ListStore(cols);
    this.segmentList = new ObservableLinkedList();
    
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
    final double highestAddress = Math.pow(2.0, (double)(8 * myTask.getISA().wordSize())) - 1.0;

    this.diss = new Disassembler(DwflCache.getDwfl(myTask), myTask.getMemory());

    pc_inc = myTask.getPC();
    this.pc = pc_inc;
    //long end = pc_inc + 20;
    this.numInstructions = 50;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.disassemblerView = (TreeView) this.glade.getWidget("disassemblerView");

    this.mmaps = this.myTask.getProc().getMaps();
    
    for (int i = 0; i < this.mmaps.length; i++)
    {
	GuiObject segment = new GuiObject(Long.toHexString(mmaps[i].addressLow)
		+ " - " + Long.toHexString(mmaps[i].addressHigh), "");
	segmentList.add(i, segment);
	if (mmaps[i].addressLow <= pc_inc && pc_inc < mmaps[i].addressHigh)
	    this.segmentIndex = i;
    }
    
    this.segmentCombo.watchLinkedList(segmentList);
    
    this.segmentCombo.setSelectedObject((GuiObject) segmentList.get(segmentIndex));

    this.segmentCombo.setActive(segmentIndex + 1);
    this.segmentCombo.showAll();
    
    this.diss = new Disassembler(DwflCache.getDwfl(myTask), myTask.getMemory());
    this.fromSpin.setRange(0.0, highestAddress);
    this.fromSpin.setValue((double) pc_inc);
    this.fromBox.setText("0x" + Long.toHexString(pc_inc));
    this.lastKnownFrom = pc_inc;
    this.toSpin.setRange(0.0, highestAddress);
    //this.toSpin.setValue((double) end);
    
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
    
    ((Button) this.glade.getWidget("formatButton")).hideAll();
    
    segmentCombo.addListener(new ComboBoxListener()
    {
	public void comboBoxEvent (ComboBoxEvent arg0)
	{
	    if(arg0.isOfType(ComboBoxEvent.Type.CHANGED))
	    {
		if (segmentList.indexOf(segmentCombo.getSelectedObject()) == -1)
		    return;
		int temp = segmentList.indexOf(segmentCombo.getSelectedObject());
		long startAddress = mmaps[temp].addressLow;
		long endAddress = mmaps[temp].addressHigh;
		if (endAddress - startAddress > row)
		    handleSegment (startAddress, startAddress+20*8);
		else
		    handleSegment (startAddress, endAddress);
		segmentIndex = temp;		
	    }
	}
    });
    
    this.fromSpin.addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (refreshLock)
          return;
        
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
        {
            double value = fromSpin.getValue();
            if (value <= 0.0 || value >= highestAddress)
        	fromSpin.setValue(lastKnownFrom);
            else
            {
        	if (addressAccessible((long)value))
        	    handleFromSpin(value);
        	else
        	{        	
        	    fromSpin.setValue(lastKnownFrom);
        	    WarnDialog dialog = new WarnDialog(
        		    " No function contains specified address");
        	    dialog.showAll();
        	    dialog.run();
        	}
            }
        }
      }
    });

    this.toSpin.addListener(new SpinListener()
    {
      public void spinEvent (SpinEvent arg0)
      {
        if (refreshLock)
          return;
        
        if (arg0.getType() == SpinEvent.Type.VALUE_CHANGED)
        {
            double value = toSpin.getValue();
            if (value <= 0.0 || value >= highestAddress)
        	toSpin.setValue(lastKnownTo);
            else
            {
        	if (addressAccessible((long)value))
        	    handleToSpin(value);
        	else
        	{
        	    toSpin.setValue(lastKnownTo);
        	    WarnDialog dialog = new WarnDialog(
        		    " No function contains specified address");
        	    dialog.showAll();
        	    dialog.run();
        	}
            }
        }          
      }
    });
    
    this.fromBox.addListener(new EntryListener()
    {
      public void entryEvent (EntryEvent arg0)
      {
        if (arg0.getType() == EntryEvent.Type.ACTIVATE)
          {
            if (refreshLock)
              return;
          
            String str = fromBox.getText();
            
            if (str.startsWith("0x"))
            {
                str = str.substring(2);            
                try
                {
                  double d = (double) Long.parseLong(str, 16);
                  if (!addressAccessible((long)d))
                  {
                      fromBox.setText("0x" + Long.toHexString((long) lastKnownFrom));
        	      WarnDialog dialog = new WarnDialog(
        		      " No function contains specified address");
        	      dialog.showAll();
        	      dialog.run();
                  }
                  else
                  {
                      if (d > lastKnownTo)
                      {
                          if (lastKnownTo == lastKnownFrom)
                    	  handleFromSpin(lastKnownTo);
                          else
                    	  fromSpin.setValue(lastKnownTo);
                      }
                      else
                      {
                	  if ( (d < lastKnownFrom) && (lastKnownFrom - d > row*8))
                              handleSegment((long)d, (long)(d + row));
                          else
                              fromSpin.setValue(d);
                      }
                  }
                     
                }
                catch (NumberFormatException nfe)
                {
                  fromBox.setText("0x" + Long.toHexString((long) lastKnownFrom));
                }
            }
            else
            {        	   	
        	try
        	{
        	    handleSymbol(str);
        	}
        	catch (RuntimeException e){
        	    fromBox.setText("0x" + Long.toHexString((long) lastKnownFrom));
        	    WarnDialog dialog = new WarnDialog(
        		    " No Symbol \"" + str + "\" in current context");
        	    dialog.showAll();
        	    dialog.run();
        	}       
            }
          }
      }
    });

    this.toBox.addListener(new EntryListener()
    {
      public void entryEvent (EntryEvent arg0)
      {
        if (arg0.getType() == EntryEvent.Type.ACTIVATE)
          {
            if (refreshLock)
              return;
            
              String str = toBox.getText();
              if (str.startsWith("0x"))
              {
        	  str = str.substring(2);        	  
                  try
                  {
                    double d = (double) Long.parseLong(str, 16);
                    if (!(addressAccessible((long)d)))
                    {
                	toBox.setText("0x" + Long.toHexString((long) lastKnownTo));
                	WarnDialog dialog = new WarnDialog(
                		" No function contains specified address");
                	dialog.showAll();
                	dialog.run();
                    }
                    else 
                    {
                        if (d < lastKnownFrom) 
                        {
                            if (lastKnownFrom == lastKnownTo)
                        	handleToSpin(lastKnownFrom);
                            else
                        	toSpin.setValue(lastKnownFrom);
                        }
                        else
                        {
                            if ((d > lastKnownTo) && (d - lastKnownTo > row))                            
                        	handleSegment((long)(d - row), (long)d);
                            else
                        	toSpin.setValue(d);
                        }
                    }
                  }
                  catch (NumberFormatException nfe)
                  {
                    toBox.setText("0x" + Long.toHexString((long) lastKnownTo));
                  }
              }
              else
              {        	   	
          	try
          	{
          	    handleSymbol(str);
          	}
          	catch (RuntimeException e){
          	    toBox.setText("0x" + Long.toHexString((long) lastKnownTo));
          	    WarnDialog dialog = new WarnDialog(
          		    " No Symbol \"" + str + "\" in current context");
          	    dialog.showAll();
          	    dialog.run();
          	}       
              }
          }
      }
    });
  }
  
  private boolean refreshLock = false;
  
  public void resetTask (Task task)
  {
    this.refreshLock = true;
    this.myTask = task;
    long pc_inc;
    double highestAddress = Math.pow(2.0, (double)(8 * myTask.getISA().wordSize())) - 1.0;

    this.diss = new Disassembler(DwflCache.getDwfl(myTask), myTask.getMemory());
    pc_inc = myTask.getPC();
    this.pc = pc_inc;
    // long end = pc_inc + 20;
    this.numInstructions = 50;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());
    this.fromSpin.setRange(0.0, highestAddress);
    this.fromSpin.setValue((double) pc_inc);
    this.lastKnownFrom = pc_inc;
    this.toSpin.setRange(0.0, highestAddress);
    // this.toSpin.setValue((double) end);
    
    this.model.clear();
    this.model.appendRow();
    this.lastPath = this.model.getFirstIter().getPath();
    for (long i = 1; i < this.numInstructions; i++)
      {
        this.model.appendRow();
        this.lastPath.next();
      }
    
    refreshList();
    this.refreshLock = false;
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
   * return a boolean indicating whether or not this address is accessible.
   * 
   * @return whether or not this address is accessible
   */
  private boolean addressAccessible(long address)
  {
      for (int i=0; i< this.mmaps.length; i++)
	  if (mmaps[i].addressLow <= address && address < mmaps[i].addressHigh)
	      return true;	  
      return false;
  }

  protected void resetPCAndList ()
  {
    this.refreshLock = true;
    long pc_inc = 0;
    pc_inc = myTask.getPC();
    
    this.lastKnownFrom = pc_inc;
    this.fromSpin.setValue((double) pc_inc);
   
    this.model.clear();
    
    this.lastPath = this.model.appendRow().getPath();
    for (long i = 1; i < this.numInstructions; i++)
      {
        this.model.appendRow();
        this.lastPath.next();
      }
    
    refreshList();
    this.refreshLock = false;
  }
  
  /**
   * Grabs information out of the Disassembler and updates the display
   */
  private synchronized void refreshList ()
  {
    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    List instructionsList
	= diss.disassembleInstructions((long) this.lastKnownFrom,
				       this.numInstructions);
    Iterator li = instructionsList.listIterator(0);
    Instruction ins = (Instruction) li.next();

    // update values in the columns if one of them has been edited
    TreeIter iter = this.model.getFirstIter();
    
    while (iter != null && this.model.isIterValid(iter))
      {
        if (ins != null)
          {
            this.model.setValue(iter, (DataColumnString) cols[1],
                           "<pc+" + (ins.address - this.pc) + ">: ");
            this.model.setValue(iter, (DataColumnString) cols[LOC],
                           "0x" + Long.toHexString(ins.address));
            this.model.setValue(iter, (DataColumnString) cols[2], ins.instruction);
            
            this.pcOffset += ins.address;
          }
        else
          this.model.setValue(iter, (DataColumnString) cols[1], "");

        this.model.setValue(iter, (DataColumnObject) cols[OBJ], ins);
        
        
        if (li.hasNext())
            ins = (Instruction) li.next();
        else
        {
            this.toSpin.setValue((double) ins.address);
            this.toBox.setText("0x" + Long.toHexString(ins.address));
            this.lastKnownTo = ins.address;
            ins = null;        
        }
        
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
   * By default append rows to the end.
   * 
   * @param i   The address to be displayed
   * @param numIns  The Instructions that maybe be added
   * @param iter    The TreeIter representing the row to be added.
   */
  public synchronized void rowAppend (long i, int numIns, TreeIter iter)
  {
//    if (iter == null)
//      iter = model.appendRow();
    
    List instructionsList
	= diss.disassembleInstructions((long) this.lastKnownTo,
				       numInstructions+numIns);
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
            if (li.hasNext()){
                ins = (Instruction) li.next();
                this.numInstructions++;
            }
              else
                {
                  this.toSpin.setValue((double) ins.address);
                  this.lastKnownTo = ins.address;
                  ins = null;
                }
            model.setValue(iter, (DataColumnString) cols[1],
                           "<pc+" + (ins.address - this.pc) + ">: ");
            model.setValue(iter, (DataColumnString) cols[LOC],
                           "0x" + Long.toHexString(ins.address));
            model.setValue(iter, (DataColumnString) cols[2], ins.instruction);
            model.setValue(iter, (DataColumnObject) cols[3], ins);
            
          }
        else
          model.setValue(iter, (DataColumnString) cols[1], "");
      }
    
    this.toSpin.setValue((double) ins.address);
    this.toBox.setText("0x" + Long.toHexString(ins.address));
    this.lastKnownTo = ins.address;
  }
  
  /**
   * Helper function for calculating memory information and putting it into rows
   * to be displayed.
   * By default prepend rows to the front.
   * 
   * @param val   The address to be displayed
   * @param nums   The numbers of rows to be prepend.
   */
  private synchronized void rowPrepend(long val, int addressAdded)
  {
      TreeIter iter = model.getFirstIter();
      TreePath path = iter.getPath();
      List instructionsList = diss.disassembleInstructions((long)(val-20), addressAdded+20);
      ListIterator li = instructionsList.listIterator(0);
      Instruction ins = (Instruction) li.next();
      while (li.hasNext() && ins.address < lastKnownFrom)
      {
	  ins = (Instruction) li.next();
	  if (ins.address == lastKnownFrom)
	      break;
	  
      }
      while (li.hasPrevious())
      {
	  if (ins.address < (long) val) {
	      ins = (Instruction) li.next();
	      break;
	  }
	  ins = (Instruction) li.previous();	 
      }
      if (addressAdded > 1) // if num==1, it should fetch the previous instruction
	  ins = (Instruction) li.next();
      
      long newlastFrom = ins.address;
      
      while (ins != null && ins.address < lastKnownFrom) {
	  iter = model.insertRowBefore(model.getIter(path));
	  this.lastPath.next();
	  if (ins != null) {
	      model.setValue(iter, (DataColumnString) cols[1], "<pc+"
		      + (ins.address - this.pc) + ">: ");
	      model.setValue(iter, (DataColumnString) cols[LOC], "0x"
		      + Long.toHexString(ins.address));
	      model.setValue(iter, (DataColumnString) cols[2],
		      ins.instruction);
	      model.setValue(iter, (DataColumnObject) cols[3], ins);
	      this.numInstructions++;		  
	      if (li.hasNext()) {
		  ins = (Instruction) li.next();
		  path.next();
	      }
	      else
		  ins = null;
	    }
	  else
	      model.setValue(iter, (DataColumnString) cols[1], "");
	  
      }

      this.lastKnownFrom = newlastFrom;
      this.fromSpin.setValue((double) newlastFrom);
      this.fromBox.setText("0x" + Long.toHexString(newlastFrom));
  }
  
  private void desensitize ()
  {
    this.disassemblerView.setSensitive(false);
    this.segmentCombo.setSensitive(false);
    this.fromSpin.setSensitive(false);
    this.toSpin.setSensitive(false);
    this.fromBox.setSensitive(false);
    this.toBox.setSensitive(false);
  }
  
  private void resensitize ()
  {
    this.disassemblerView.setSensitive(true);
    this.segmentCombo.setSensitive(true);
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
        this.fromBox.setText("0x" + Long.toHexString((long) this.lastKnownTo));
        this.lastKnownFrom = this.lastKnownTo;
        return;
      }

    if (val > this.lastKnownFrom)
      {
	if (this.numInstructions < 1)
            return;
        
	TreeIter iter = model.getFirstIter();
	Instruction ins = (Instruction) this.model.getValue(iter, (DataColumnObject) cols[OBJ]);
	//--this.numInstructions;
        
	while (ins != null && ins.address < val)
	{
	    this.model.removeRow(iter);
	    this.lastPath.previous();
	    ins = (Instruction) this.model.getValue(iter,(DataColumnObject) cols[OBJ]);
	    --this.numInstructions;
	}
	if (ins == null)
	    return;

	this.lastKnownFrom = ins.address;
	this.fromBox.setText("0x" + Long.toHexString(ins.address));
	this.fromSpin.setValue((double)ins.address);
	refreshList();
	return;
      }
    else
      {
	int addressAdded = 0;
	for (long i = (long)lastKnownFrom; i > (long)val; i--)
	    addressAdded++;
	if (addressAdded == 0)
	    return;
	rowPrepend((long)val, addressAdded);
      }
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
        this.toBox.setText("0x" + Long.toHexString((long) this.lastKnownFrom));
        this.lastKnownTo = this.lastKnownFrom;
        return;
      }

    if (val > this.lastKnownTo)
      {
        int numIns = 0;
	for (long i = (long) lastKnownTo + 1; i < val + 1; i++)
          ++numIns;
          
        rowAppend((long) val, numIns, null);

        return;
      }
    else
      {
        if (this.numInstructions < 1)
          return;

        this.toToggle = true;
  
        Instruction ins = (Instruction) this.model.getValue(this.model.getIter(this.lastPath),
                                                (DataColumnObject) cols[OBJ]);
        
        while (ins != null && ins.address > val)
          {
            model.removeRow(model.getIter(this.lastPath));
            this.lastPath.previous();
            ins = (Instruction) this.model.getValue(
                                                    this.model.getIter(this.lastPath),
                                                    (DataColumnObject) cols[OBJ]);
            --this.numInstructions;
          }
        
        if (ins == null)
          return;
        
        this.toSpin.setValue((double) ins.address);
        this.lastKnownTo = ins.address;
        this.toBox.setText("0x" + Long.toHexString(ins.address));
        
        refreshList();
      }
  }
  
  /**
   * When the box is inputed a symbol, update the displayed information to the symbol.
   * @param symbolName
   */
  private synchronized void handleSymbol(String symbolName)
  {
      LinkedList addressList = SymbolFactory.getAddresses(this.myTask, symbolName);
      long startAddress = ((Long)addressList.getFirst()).longValue();
      Symbol symbol = SymbolFactory.getSymbol(this.myTask, startAddress);
      long endAddress = symbol.getAddress() + symbol.getSize();
      handleSegment(startAddress, endAddress);      
  }
  
  /**
   * Display the whole segment
   * @param startAddress
   * @param endAddress
   */
  private synchronized void handleSegment(long startAddress, long endAddress)
  {
      	List instructionsList
    	= diss.disassembleInstructionsStartEnd((long)startAddress, (long)endAddress);
        Iterator li = instructionsList.listIterator(0);
        int insnum = 1;
        Instruction ins = (Instruction)li.next();
        this.lastKnownFrom = (double)ins.address;
        while (li.hasNext()){
    	  ins = (Instruction)li.next();
    	  insnum++;
        }
        this.lastKnownTo = (double)ins.address;
    
        TreeIter iter = this.model.getFirstIter();
        while (insnum < numInstructions)
        {
    	  this.model.removeRow(iter);
    	  this.lastPath.previous();
    	  numInstructions--;	  
        }
        while(insnum > numInstructions)
        {
    	  this.model.appendRow();
    	  this.lastPath.next();
    	  numInstructions++;	  
        }
    
        refreshList();
        fromBox.setText("0x" + Long.toHexString((long)lastKnownFrom));
        fromSpin.setValue(lastKnownFrom);
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
      TaskStepEngine tse = (TaskStepEngine) arg;
      if (!tse.getState().isStopped())
        {
          if (! DW_active)
            {
              DisassemblyWindow.this.observable = o;
              finishDisWin(tse.getTask().getProc());
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
                  resensitize();
                }
              });
            }
        }
    }
  }


}
