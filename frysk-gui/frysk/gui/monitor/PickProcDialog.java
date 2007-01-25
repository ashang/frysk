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
// type filter text
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


package frysk.gui.monitor;

import java.io.File;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HButtonBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.common.dialogs.FryskDialog;
import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * A Dialog that displays a list of procs matching the given path.
 */
public class PickProcDialog
    extends FryskDialog
{

  TreeView ListView;

  private TreeStore treeStore;

  private Hashtable iterHash;

  private DataColumnString nameDC;

  private DataColumnString locationDC;

  private DataColumnInt pidDC;

  private DataColumnObject objectDC;

  private Button OpenButton;

  private final TreeViewColumn nameColumn = new TreeViewColumn();

  private final TreeViewColumn pidColumn = new TreeViewColumn();

  private final TreeViewColumn dirColumn = new TreeViewColumn();

  private ProcCreatedObserver procCreatedObserver;

  private ProcDestroyedObserver procDestroyedObserver;

  public PickProcDialog (String path)
  {
    super();
    this.setTitle("Debug Process List");

    this.iterHash = new Hashtable();

    this.nameDC = new DataColumnString();
    this.locationDC = new DataColumnString();
    this.pidDC = new DataColumnInt();
    this.objectDC = new DataColumnObject();

    this.ListView = new TreeView();
    this.treeStore = new TreeStore(
                                   new DataColumn[] { this.nameDC,
                                                     this.locationDC,
                                                     this.pidDC, this.objectDC });

    this.procCreatedObserver = new ProcCreatedObserver();
    this.procDestroyedObserver = new ProcDestroyedObserver();

    Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
    Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);

    setupNameColumn();
    setupPidColumn();
    setupLocationColumn();

    this.ListView.appendColumn(pidColumn);
    this.ListView.appendColumn(nameColumn);
    this.ListView.appendColumn(dirColumn);
    this.ListView.setEnableSearch(true);
    this.ListView.setModel(this.treeStore);
    this.treeStore.setSortColumn(nameDC, SortType.ASCENDING);

    this.ListView.addListener(new TreeViewListener()
    {
      public void treeViewEvent (TreeViewEvent event)
      {
        if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
          {
            // Subtle .. it is not.
            // On double click, simulate OK click
            HButtonBox actionArea = getActionArea();
            Widget[] buttons = actionArea.getChildren();
            if (buttons.length == 2)
              if (buttons[1] instanceof Button)
                ((Button) buttons[1]).click();
          }
      }
    });

    this.setHasSeparator(false);

    VBox mainBox = new VBox(false, 2);
    mainBox.setBorderWidth(12);
    this.getDialogLayout().add(mainBox);

    mainBox.packStart(new Label(
                                "Please select a process to inspect in the source window"));
    ScrolledWindow sWindow = new ScrolledWindow(null, null);
    sWindow.setMinimumSize(500, 500);
    sWindow.setBorderWidth(10);
    sWindow.setPolicy(PolicyType.NEVER, PolicyType.AUTOMATIC);

    sWindow.add(ListView);
    mainBox.packEnd(sWindow);

    this.addButton(GtkStockItem.OPEN, ResponseType.OK.getValue());
    this.addButton(GtkStockItem.CANCEL, ResponseType.CANCEL.getValue());

    HButtonBox actionArea = getActionArea();
    Widget[] buttons = actionArea.getChildren();
    if (buttons.length == 2)
      if (buttons[1] instanceof Button)
        OpenButton = (Button) buttons[1];

    OpenButton.setSensitive(false);

    this.ListView.getSelection().addListener(new TreeSelectionListener()
    {

      public void selectionChangedEvent (TreeSelectionEvent event)
      {
        if (event.getType() == TreeSelectionEvent.Type.CHANGED)
          {
            OpenButton.setSensitive(true);
          }
      }
    });
  }

  public Proc getChoosenProc ()
  {
    if (this.ListView.getSelection().getSelectedRows().length < 1)
      return null;

    if (this.ListView.getSelection().getSelectedRows()[0] == null)
      return null;

    TreePath selected = this.ListView.getSelection().getSelectedRows()[0];
    return ((GuiProc) this.treeStore.getValue(this.treeStore.getIter(selected),
                                              objectDC)).getProc();

  }

  private void setupNameColumn ()
  {
    nameColumn.setClickable(true);
    nameColumn.addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (nameColumn.getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(nameDC, SortType.DESCENDING);
            nameColumn.setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(nameDC, SortType.ASCENDING);
            nameColumn.setSortOrder(SortType.ASCENDING);
          }
        pidColumn.setSortIndicator(false);
        nameColumn.setSortIndicator(true);
        dirColumn.setSortIndicator(false);

      }
    });

    CellRendererText renderNameText = new CellRendererText();
    nameColumn.packStart(renderNameText, true);
    nameColumn.setTitle("Process Name");
    nameColumn.addAttributeMapping(renderNameText,
                                   CellRendererText.Attribute.TEXT, this.nameDC);
    nameColumn.setReorderable(true);
    nameColumn.setSortOrder(SortType.ASCENDING);
    nameColumn.setSortIndicator(true);
  }

  private void setupPidColumn ()
  {

    pidColumn.setClickable(true);
    pidColumn.setReorderable(true);
    CellRendererText renderPIDText = new CellRendererText();
    pidColumn.packStart(renderPIDText, true);
    pidColumn.setTitle("PID");
    pidColumn.addAttributeMapping(renderPIDText,
                                  CellRendererText.Attribute.TEXT, this.pidDC);

    pidColumn.addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pidColumn.getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(pidDC, SortType.DESCENDING);
            pidColumn.setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(pidDC, SortType.ASCENDING);
            pidColumn.setSortOrder(SortType.ASCENDING);

          }
        pidColumn.setSortIndicator(true);
        nameColumn.setSortIndicator(false);
        dirColumn.setSortIndicator(false);
      }
    });
  }

  private void setupLocationColumn ()
  {

    CellRendererText renderDirText = new CellRendererText();
    dirColumn.packStart(renderDirText, true);
    dirColumn.setTitle("Location");
    dirColumn.addAttributeMapping(renderDirText,
                                  CellRendererText.Attribute.TEXT,
                                  this.locationDC);

    dirColumn.addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (dirColumn.getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(locationDC, SortType.DESCENDING);
            dirColumn.setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(locationDC, SortType.ASCENDING);
            dirColumn.setSortOrder(SortType.ASCENDING);

          }
        pidColumn.setSortIndicator(false);
        nameColumn.setSortIndicator(false);
        dirColumn.setSortIndicator(true);

      }
    });
    dirColumn.setReorderable(true);
    dirColumn.setClickable(true);
  }

  class ProcCreatedObserver
      implements Observer
  {
    public void update (Observable o, Object obj)
    {
      final Proc proc = (Proc) obj;

      org.gnu.glib.CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          GuiProc guiProc = null;
          try
            {
              guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
            }
          catch (Exception e)
            {
              return;
            }

          if (! guiProc.isOwned())
            return;
          // new process name
          TreeIter process = treeStore.appendRow(null);
          if (process != null)
            {
              iterHash.put(guiProc.getExecutableName(), process);
              treeStore.setValue(process, nameDC, guiProc.getExecutableName());
              File path = new File(guiProc.getNiceExecutablePath());
              if (path != null)
                treeStore.setValue(process, locationDC,
                                   justPath(path.getPath(), path.getName()));
              else
                treeStore.setValue(process, locationDC, "");
              treeStore.setValue(process, objectDC, guiProc);
              treeStore.setValue(process, pidDC, guiProc.getProc().getPid());
            }
        }

      });
    }
  }

  class ProcDestroyedObserver
      implements Observer
  {
    public void update (Observable o, Object obj)
    {
      final Proc proc = (Proc) obj;

      org.gnu.glib.CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          GuiProc guiProc = null;
          TreeIter parent = null;
          try
            {
              guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
            }
          catch (Exception e)
            {
              return;
            }
          if (guiProc != null)
            {
              try
                {
                  parent = (TreeIter) iterHash.get(guiProc.getExecutableName());
                }
              catch (Exception e)
                {

                  return;
                }
            }
          else
            {
              return;
            }

          if (parent == null)
            {
              return;
            }

          if (! treeStore.isIterValid(parent))
            {
              return;
            }

          treeStore.removeRow(parent);
          iterHash.remove(guiProc.getExecutableName());

        }
      });
    }
  }

  private String justPath (String path, String name)
  {
    try
      {
        return (path.substring(0, path.length() - name.length()));
      }
    catch (Exception e)
      {
        return "";
      }
  }

}
