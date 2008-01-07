//This file is part of the program FRYSK.
//
//Copyright 2007 Oracle Corporation.
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
import java.util.List;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.math.BigInteger;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.pango.FontDescription;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnDouble;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ScrolledWindow;
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
import org.gnu.gtk.event.ContainerListener;
import org.gnu.gtk.event.ContainerEvent;

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
import frysk.symtab.SymbolFactory;
import frysk.proc.MemoryMap;

import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;

public class MemoryWindow
    extends Window
    implements Saveable
{

  private final int LOC = 0; /* Memory address */

  private final int OBJ = 11; /* Object stored in above address */

  private final int BYTE_BITS = 8;

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  public static String gladePath;

  private DataColumn[] cols = { new DataColumnString(), /* memory location */
  new DataColumnString(), /* 8-bit hexadecimal little endian */
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

  protected static String[] colNames = {"X-bit Binary (LE)",
                                        "X-bit Binary (BE)",
                                        "X-bit Octal (LE)", "X-bit Octal (BE)",
                                        "X-bit Decimal (LE)",
                                        "X-bit Decimal (BE)",
                                        "X-bit Hexadecimal (LE)",
                                        "X-bit Hexadecimal (BE)", "Instruction" };

  protected boolean[] colVisible = { true, true, false, false, false, false, false,
                                    false, false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[10];

  private MemoryFormatDialog formatDialog;

  private TreeView memoryView;

  private Disassembler diss;

  private SpinButton fromSpin;
  
  private Entry fromBox;
  
  private SimpleComboBox segmentCombo;
  
  private ObservableLinkedList segmentList;

  private ListStore model;

  private double lastKnownFrom;

  private double lastKnownTo;

  protected static int currentFormat = 2;
  
  private boolean MW_active = false;
  
  private Observable observable;
  
  private LockObserver lock;
  
  private TreePath lastPath;
  
  private boolean toggle = true;
  
  private boolean closed = false;
  
  private MemoryMap[] mmaps;
  
  private int segmentIndex = 0;
  
  private ScrolledWindow swin;

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

    this.fromSpin = (SpinButton) this.glade.getWidget("fromSpin");
    this.fromBox = (Entry) this.glade.getWidget("fromBox");
    //this.pcLabelDec = (Label) this.glade.getWidget("PCLabelDec");
    //this.pcLabelHex = (Label) this.glade.getWidget("PCLabelHex");
    this.segmentCombo = new SimpleComboBox(
            (this.glade.getWidget("segmentCombo")).getHandle());
    this.model = new ListStore(cols);
    this.segmentList = new ObservableLinkedList();

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
    final double highestAddress = Math.pow(2.0, (double)(8 * myTask.getISA().wordSize())) - 1.0;
    
    this.mmaps = this.myTask.getProc().getMaps();
    
    this.diss = new Disassembler(myTask.getMemory());
    pc_inc = myTask.getPC();
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

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
    

    this.memoryView = (TreeView) this.glade.getWidget("memoryView");
    FontDescription fontDesc = new FontDescription("monospace 10");
    memoryView.setFont(fontDesc);
    
    swin = (ScrolledWindow)this.glade.getWidget("scrolledwindowMemory");
    swin.setPolicy(PolicyType.NEVER, PolicyType.NEVER);	
	

    this.segmentCombo.showAll();
    this.diss = new Disassembler(myTask.getMemory());
    this.fromSpin.setRange(0.0, highestAddress);
    this.fromSpin.setValue((double) pc_inc);
    this.fromBox.setText("0x" + Long.toHexString(pc_inc));
    
    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[LOC]);
    memoryView.appendColumn(col);
    
    col = new TreeViewColumn();
    col.setTitle("Raw-bytes (Hexadecimal)");
    renderer = new CellRendererText();
    ((CellRendererText) renderer).setEditable(false);
    col.packStart(renderer, false);
    col.setReorderable(true);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                            cols[1]);
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
                                cols[i + 2]);

        memoryView.appendColumn(col);

        col.addAttributeMapping(renderer, CellRendererText.Attribute.XALIGN,
                                cols[12]);
        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
      }
    
    recalculate();

    memoryView.setAlternateRowColor(true);
    this.addListener(new ContainerListener()
    {
	public void containerEvent(ContainerEvent event)
	{
	    if(event.isOfType(ContainerEvent.Type.CHECK_RESIZE))
	    {
		double value = (double)(lastKnownFrom + 
			((MemoryWindow.this.getSize().getHeight()-180)/25)*8);
        	if (addressAccessible((long)value))
        	    handleToSpin(value);
        	else
        	{
        	    WarnDialog dialog = new WarnDialog(
        		    "Cannot access memory at address 0x" + Long.toHexString((long)value));
        	    dialog.showAll();
        	    dialog.run();        	    
        	}        	
	    }	    
	}
    });

    this.formatDialog.addListener(new LifeCycleListener()
    {

      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        refreshList();
      }

    });

  
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
		fromSpin.setValue((double)startAddress);
		segmentIndex = temp;
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
        		    "Cannot access memory at address 0x" + Long.toHexString((long)value));
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
        		      "Cannot access memory at address 0x" + Long.toHexString((long)d));
        	      dialog.showAll();
        	      dialog.run();
                  }
                  else
                  {
                      fromSpin.setValue(d);
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
  
  private boolean refreshLock = false;
  
  public void resetTask (Task task)
  {
    this.refreshLock = true;
    this.myTask = task;
    long pc_inc;
    double highestAddress = Math.pow(2.0, (double)(8 * myTask.getISA().wordSize())) - 1.0;
    
    this.diss = new Disassembler(myTask.getMemory());

    pc_inc = myTask.getPC();

    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());
    this.model.clear();
    this.fromSpin.setRange(0.0, highestAddress);
    this.fromSpin.setValue((double) pc_inc);

    recalculate();
    this.refreshLock = false;
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
    long end = start + ((MemoryWindow.this.getSize().getHeight()-180)/25)*8;
    this.lastKnownFrom = (double) start;
    this.lastKnownTo = (double) end;
    this.model.clear();

    memoryView.setModel(model);

    for (int i = 0; i <= end - start; i = i + 8)
      rowAppend((long)(start + i), null);

    this.refreshList();
  }
  
  private void resetPCAndList ()
  {
    this.refreshLock = true;
    long pc_inc = 0;
    pc_inc = myTask.getPC();
    
    this.lastKnownFrom = pc_inc;
    this.lastKnownTo = (double) pc_inc + ((MemoryWindow.this.getSize().getHeight()-180)/25)*8;
    
    this.fromSpin.setValue((double) pc_inc);
    
    this.model.clear();
    for (int i = 0; i <= this.lastKnownTo - this.lastKnownFrom; i = i + 8)
      rowAppend((long)(this.lastKnownFrom + i), null);
    
    refreshList();
    this.refreshLock = false;
  }

  /**
   * Refresh and update the display of addresses and values from what is 
   * re-calculated and turned into BigIntegers.
   */
  private synchronized void refreshList ()
  {
    // If there's no task, no point in refreshing
    if (this.myTask == null)
      return;

    List instructionsList
	= diss.disassembleInstructions((long) this.lastKnownFrom,
				       (long) (this.lastKnownTo
					       - this.lastKnownFrom + 1));
    Iterator li = instructionsList.listIterator(0);
    Instruction ins = (Instruction) li.next();

    // update values in the columns if one of them has been edited
    TreeIter iter = this.model.getFirstIter();

    while (iter != null)
      {
        BigInteger bi = new BigInteger(
                                       (String) this.model.getValue(
                                                               iter,
                                                               (DataColumnObject) cols[OBJ]),
                                       10);
        String addr = new String((String) this.model.getValue(
                                        iter, (DataColumnString) cols[LOC])).substring(2);

        byte[] b = bi.toByteArray();
        String bin = "";
        //String oct = "";
        String hex = "";
        //String dec = "";

        if (bi.signum() < 0)
            bi = new BigInteger(1, b);
        bin = bi.toString(2);
        //oct = bi.toString(8);
        hex = bi.toString(16);
        //dec = bi.toString(10);

        int diff = bin.length() % BYTE_BITS;
        if (diff != 0)
          bin = padBytes(bin, false, diff);
            
        while (bin.length() < Long.SIZE)
        {
            bin = "0" + bin;            
        }
 
        while (hex.length() < Long.SIZE/4 )
        {
            hex = "0" + hex;
        }            

        /* Little endian first */
        String newbin = "";
        String newoct = "";
        String newdec = "";
        String newhex = "";
        String bytehex = "";
        int bit = Byte.SIZE;        
        for (int i = 0; i < Math.round((double)bin.length()/bit); i++)
        {
            bytehex = "0x" + hex.substring(bit/4*i, bit/4*(i+1)) + " " + bytehex;    	    
        }
        bit = (int) Math.pow(2, currentFormat + 3);
        long len = Math.round((double)bin.length()/bit);
        String[] binArray = new String[(int)len];
        String[] octArray = new String[(int)len];
        String[] hexArray = new String[(int)len];        
        for (int i=0; i< len; i++)
        {
    	    if (bit*(i+1) < bin.length())
    	    {
    		binArray[i] = bin.substring(bit*i, bit*(i+1));
    		hexArray[i] = hex.substring(bit/4*i, bit/4*(i+1));
    	    }
    	    else
    	    {
    		binArray[i] = bin.substring(bit*i);
    		hexArray[i] = hex.substring(bit/4*i, bit/4*(i+1));
    	    }    	    
    	    int in = (int)Long.parseLong(binArray[i], 2);
    	    newdec = newdec + in + " ";
    	    octArray[i] = Integer.toOctalString(in);
    	    	    
    	    newbin = binArray[i] + " " + newbin;
    	    newoct = "0"+octArray[i] + " " + newoct;    	    
    	    newhex = "0x"+hexArray[i]+ " " + newhex;
        }
        this.model.setValue(iter, (DataColumnString) cols[1], bytehex);
        this.model.setValue(iter, (DataColumnString) cols[2], newbin);
        this.model.setValue(iter, (DataColumnString) cols[4], newoct);
        this.model.setValue(iter, (DataColumnString) cols[6], newdec);
        this.model.setValue(iter, (DataColumnString) cols[8], newhex);

        /* Big endian second */
        String bin2 = switchEndianness(bin, true);
        BigInteger bii = new BigInteger(bin2, 2);

        hex = bii.toString(16);        
        newbin = "";
        newoct = "";
        newdec = "";
        newhex = "";
        /* Bad hack to get around weird BigInteger endian bug */
        /*if (bin2.equals(bin))
          dec = bi.toString(10);
        else
          dec = bii.toString(10);*/
        
        while (bin2.length() < Long.SIZE)
        {
            bin2 = "0" + bin2;            
        }      
        while (hex.length() < Long.SIZE/4 )
        {
            hex = "0" + hex;
        }
        
        for (int i=0; i< len; i++)
        {
    	    if (bit*(i+1) < bin2.length())
    	    {
    		binArray[i] = bin2.substring(bit*i, bit*(i+1));
    		hexArray[i] = hex.substring(bit/4*i, bit/4*(i+1));
    	    }
    	    else
    	    {
    		binArray[i] = bin2.substring(bit*i);
    		hexArray[i] = hex.substring(bit/4*i, bit/4*(i+1));
    	    }    	    
    	    int in = (int)Long.parseLong(binArray[i], 2);
    	    newdec = newdec + in + " ";
    	    octArray[i] = Integer.toOctalString(in);
    	    
    	    newbin = binArray[i] + " " + newbin;
    	    newoct = "0"+octArray[i] + " " + newoct;    	    
    	    newhex = "0x"+hexArray[i]+ " " + newhex;
        }
        this.model.setValue(iter, (DataColumnString) cols[3], newbin);
        this.model.setValue(iter, (DataColumnString) cols[5], newoct);
        this.model.setValue(iter, (DataColumnString) cols[7], newdec);
        this.model.setValue(iter, (DataColumnString) cols[9], newhex);

        if (ins != null && Long.toHexString(ins.address).equals(addr))
          {
            this.model.setValue(iter, (DataColumnString) cols[10], ins.instruction);
            if (li.hasNext())
              ins = (Instruction) li.next();
            else
              ins = null;
          }
        else
          this.model.setValue(iter, (DataColumnString) cols[10], "");

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
      {
        iter = model.appendRow();
        this.lastPath = iter.getPath();
      }
    else
      this.lastPath.next();

    model.setValue(iter, (DataColumnString) cols[LOC], "0x"
                                                       + Long.toHexString(i));
    model.setValue(iter, (DataColumnDouble) cols[12], 1.0);
    
    try
    {
      model.setValue(iter, (DataColumnObject) cols[OBJ],
                     "" + myTask.getMemory().getLong(i));
    }
    catch (Exception e)
    {
      return;
    }    
  }
  
  private void desensitize ()
  {
    this.memoryView.setSensitive(false);
    this.segmentCombo.setSensitive(false);
    this.fromSpin.setSensitive(false);    
    this.fromBox.setSensitive(false);
  }
  
  private void resensitize ()
  {
    this.memoryView.setSensitive(true);
    this.segmentCombo.setSensitive(true);
    this.fromSpin.setSensitive(true);
    this.fromBox.setSensitive(true);
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

    TreeIter iter = model.getFirstIter();
    this.lastPath = iter.getPath();
    for (long i = 0; i <= (MemoryWindow.this.getSize().getHeight()-180)/25; i++)
    {
	if (iter == null)
	    break;
	rowAppend((long)val+i*8, iter);
	lastKnownTo = (double)(val+i*8);
	iter.getNextIter();
    }
    this.lastPath.previous();
    
    this.lastKnownFrom = val;
    this.fromBox.setText("0x" + Long.toHexString((long) val));
    refreshList();

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

    if (val > this.lastKnownTo)
      {
        for (long i = (long) lastKnownTo + 8; i < val + 1; i = i+8)
          //rowAppend((long)(i+8*(i-(lastKnownTo+8))), null);
            rowAppend((long)i, null);
      }
    else
      {
        for (; this.lastKnownTo > val; this.lastKnownTo = this.lastKnownTo-8)
          {
            this.model.removeRow(this.model.getIter(this.lastPath));
            this.lastPath.previous();
          }
      }

    this.lastKnownTo = val;
    refreshList();
  }
  
  /**
   * When the box is inputed a symbol, update the displayed information to the symbol.
   * @param symbolName
   */
  private synchronized void handleSymbol(String symbolName)
  {
      LinkedList addressList = SymbolFactory.getSymbol(this.myTask, symbolName);
      long startAddress = ((Long)addressList.getFirst()).longValue();
      fromSpin.setValue((double)startAddress);
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
      TaskStepEngine tse = (TaskStepEngine) arg;
      if (!tse.getState().isStopped())
        {
          if (! MW_active)
            {
              MemoryWindow.this.observable = o;
              finishMemWin(tse.getTask().getProc());
            }
          else
            return;
        }
      else
        {
          /* The argument is not stopped. its used here as a toggle. If the toggle is
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
