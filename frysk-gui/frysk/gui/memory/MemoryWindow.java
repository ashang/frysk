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
import org.gnu.gtk.TreeIter;
//import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
//import org.gnu.gtk.event.CellRendererTextEvent;
//import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

//import frysk.sys.PtraceByteBuffer;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.Saveable;
import frysk.proc.Task;

/**
 * @author mcvet
 */
public class MemoryWindow
    extends Window
    implements Saveable
{

  private Task myTask;

  private LibGlade glade;

  private Preferences prefs;

  private DataColumn[] cols = { new DataColumnString(), /* memory location */
  new DataColumnString(), /* binary */
  new DataColumnString(), /* octal */
  new DataColumnString(), /* decimal */
  new DataColumnString(), /* hexadecimal */
  new DataColumnObject() /* memory object */
  };

  protected static String[] colNames = { "Binary", "Octal", "Decimal",
                                        "Hexadecimal" };

  protected boolean[] colVisible = { true, false, false, false };

  private TreeViewColumn[] columns = new TreeViewColumn[5];

  private MemoryFormatDialog formatDialog;

  private TreeView memoryView;

  public MemoryWindow (LibGlade glade)
  {
    super(glade.getWidget("memoryWindow").getHandle());
    this.glade = glade;
    this.formatDialog = new MemoryFormatDialog(this.glade);

    this.setIcon(IconManager.windowIcon);
  }

  public void setTask (Task myTask)
  {
    this.myTask = myTask;

    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());

    this.memoryView = (TreeView) this.glade.getWidget("memoryView");

    ListStore model = new ListStore(cols);
    memoryView.setModel(model);

    long j = myTask.getIsa().pc(myTask);
    long k = j + 300;

    for (j = j + 1; j < k; j++)
      {
        TreeIter iter = model.appendRow();

        model.setValue(iter, (DataColumnString) cols[0], "0x"
                                                         + Long.toHexString(j));
        model.setValue(iter, (DataColumnObject) cols[5],
                       "" + myTask.getMemory().getULong(j));
      }

    TreeViewColumn col = new TreeViewColumn();
    col.setTitle("Location");
    CellRenderer renderer = new CellRendererText();
    col.packStart(renderer, true);
    col.setReorderable(false);
    col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, cols[0]);
    memoryView.appendColumn(col);

    for (int i = 0; i < colNames.length; i++)
      {
        col = new TreeViewColumn();
        col.setTitle(colNames[i]);
        col.setReorderable(true);
        renderer = new CellRendererText();
        ((CellRendererText) renderer).setEditable(true);
        col.packStart(renderer, false);
        col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                                cols[i + 1]);

        memoryView.appendColumn(col);

        col.setVisible(this.prefs.getBoolean(colNames[i], colVisible[i]));

        columns[i] = col;
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
        String val = (String) model.getValue(iter, (DataColumnObject) cols[5]);
        model.setValue(iter, (DataColumnString) cols[1],
                       Long.toBinaryString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[2],
                       Long.toOctalString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[3],
                       Long.toString(new Long(val).longValue()));
        model.setValue(iter, (DataColumnString) cols[4],
                       "0x" + Long.toHexString(new Long(val).longValue()));
        iter = iter.getNextIter();
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

}
