// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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


package frysk.gui.sessions;

import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.WindowManager;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.glade.LibGlade;

import org.gnu.gtk.Button;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeViewColumn;
//import org.gnu.gtk.event.LifeCycleEvent;
//import org.gnu.gtk.event.LifeCycleListener;

/**
 * A dialog allowing the user select and de-select processes of the same name,
 * allowing customization of which processes are brought to the MainWindow.
 * 
 * @author mcvet
 */
public class ProcessPicker
    extends Dialog
{

  private TreeView procView;

  private TreeStore model;

  private LibGlade glade;

  private DataColumn[] columns = { new DataColumnBoolean(),
                                  new DataColumnString() };

  private LinkedList guiProcsList;

  private Session newSession;

  private Hashtable nameHT;

  private Hashtable pidHT;

  public ProcessPicker (LibGlade glade)
  {
    super(glade.getWidget("processPickerWindow").getHandle());
    this.glade = glade;
    this.guiProcsList = new LinkedList();
  }

  /**
   * Determine whether or not the incoming session contains multiple processes
   * with the same name. If so, build a dialog for the user so they can
   * determine specifically which processes they are interested in.
   */
  public void checkSession (Session s)
  {
    nameHT = new Hashtable();
    pidHT = new Hashtable();
    boolean flag = false;
    Iterator debugprocesses = s.getProcesses().iterator();
    LinkedList ll;

    while (debugprocesses.hasNext())
      {
        DebugProcess dp = (DebugProcess) debugprocesses.next();
        dp.addRemoveObservers();
        dp.addProcsMinusObserver();
        Iterator guiprocs = dp.getProcs().iterator();
        while (guiprocs.hasNext())
          {
            GuiProc gp = (GuiProc) guiprocs.next();
            if (nameHT.containsKey(gp.getExecutableName()))
              {
                flag = true; /* There are multiple processes with this name */
                ll = (LinkedList) nameHT.get(gp.getExecutableName());
                ll.add(gp);
                pidHT.put(new Integer(gp.getProc().getPid()), dp);
              }
            else
              {
                LinkedList nll = new LinkedList();
                nll.add(gp);
                nameHT.put(gp.getExecutableName(), nll);
                pidHT.put(new Integer(gp.getProc().getPid()), dp);
              }
          }
      }

    if (! flag) /* All singleton processes; return the session as-is */
      finish(s);
    else
      /* We've got multiple processes by the same name... */
      {
        this.procView = (TreeView) this.glade.getWidget("procView");
        this.procView.setHeadersVisible(true);
        this.procView.setHeadersClickable(true);
        this.procView.setEnableSearch(true);
        this.model = new TreeStore(columns);
        newSession = s;

        Iterator i = nameHT.keySet().iterator();
        while (i.hasNext())
          {
            ll = (LinkedList) nameHT.get(i.next());
            Iterator j = ll.iterator();
            GuiProc gp = (GuiProc) j.next();

            /* First append the name of the process */
            TreeIter parent = model.appendRow(null);
            model.setValue(parent, (DataColumnBoolean) columns[0], true);
            model.setValue(parent, (DataColumnString) columns[1],
                           gp.getExecutableName());

            /* Then append all the relevant PIDs */
            TreeIter iter = model.appendRow(parent);
            model.setValue(iter, (DataColumnBoolean) columns[0], true);
            model.setValue(iter, (DataColumnString) columns[1],
                           "" + gp.getProc().getPid());
            guiProcsList.add(gp);

            while (j.hasNext())
              {
                gp = (GuiProc) j.next();
                iter = model.appendRow(parent);
                model.setValue(iter, (DataColumnBoolean) columns[0], true);
                model.setValue(iter, (DataColumnString) columns[1],
                               "" + gp.getProc().getPid());
                guiProcsList.add(gp);
              }
          }

        setListeners();
        this.run();
      }
  }

  /**
   * Set up the listeners for the checkboxes and the close button - which
   * finalizes all the changes.
   */
  public void setListeners ()
  {
    TreeViewColumn col = new TreeViewColumn();
    CellRenderer renderer = new CellRendererToggle();
    col.packStart(renderer, false);

    col.addAttributeMapping(renderer, CellRendererToggle.Attribute.ACTIVE,
                            columns[0]);
    col.setTitle("");
    procView.appendColumn(col);

    ((CellRendererToggle) renderer).addListener(new CellRendererToggleListener()
    {
      public void cellRendererToggleEvent (CellRendererToggleEvent arg0)
      {
        TreePath path = new TreePath(arg0.getPath());
        TreeIter iter = model.getIter(path);

        boolean prev = model.getValue(iter, (DataColumnBoolean) columns[0]);
        model.setValue(iter, (DataColumnBoolean) columns[0], ! prev);

        if (iter.getChildCount() != 0)
          {
            TreeIter child = iter.getFirstChild();
            do
              {
                if (model.isIterValid(child))
                  {
                    model.setValue(child, (DataColumnBoolean) columns[0],
                                   ! prev);
                  }

                child = child.getNextIter();
              }
            while (child != null);
          }

      }
    });

    final TreeViewColumn col2 = new TreeViewColumn();
    CellRendererText textrenderer = new CellRendererText();
    col2.packStart(textrenderer, true);
    col2.addAttributeMapping(textrenderer, CellRendererText.Attribute.TEXT,
                             columns[1]);

    col2.addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (col2.getSortOrder() == SortType.ASCENDING)
          {
            model.setSortColumn(columns[1], SortType.DESCENDING);
            col2.setSortOrder(SortType.DESCENDING);
          }
        else
          {
            model.setSortColumn(columns[1], SortType.ASCENDING);
            col2.setSortOrder(SortType.ASCENDING);
          }
      }
    });
    col2.setTitle("Processes");
    col2.addAttributeMapping(textrenderer, CellRendererText.Attribute.TEXT,
                             columns[1]);
    col2.setClickable(true);
    col2.setReorderable(true);
    col2.setSortOrder(SortType.ASCENDING);
    col2.setSortIndicator(true);
    model.setSortColumn(columns[1], SortType.ASCENDING);
    procView.appendColumn(col2);

    procView.setModel(model);

    ((Button) this.glade.getWidget("launchButton")).addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            pickProcs();
            finish(newSession);
          }
      }
    });

  }

  /**
   * After the user is finished selecting PIDs from the checkboxes, iterate
   * through the boxes and remove the de-selected PIDs from the session.
   */
  public void pickProcs ()
  {

    TreeIter parent = model.getFirstIter();
    Iterator i = guiProcsList.iterator();

    while (parent != null)
      {
        TreeIter child = parent.getFirstChild();
        while (child != null)
          {
            GuiProc gp = (GuiProc) i.next();

            if (gp != null
                && ! model.getValue(child, (DataColumnBoolean) columns[0]))
              {
                DebugProcess dp = (DebugProcess) pidHT.get(new Integer(
                                                                       gp.getProc().getPid()));
                dp.removeProc(gp);
              }
            child = child.getNextIter();
          }
        parent = parent.getNextIter();
      }

    Iterator j = newSession.getProcesses().iterator();
    while (j.hasNext())
      ((DebugProcess) j.next()).addObservers();

  }

  /**
   * Finish everything up: hide this window, bring up the MainWindow, and set
   * the current session.
   */
  public void finish (Session s)
  {
    ProcessPicker.this.hideAll();
    WindowManager.theManager.mainWindow.setSession(s);
    WindowManager.theManager.mainWindow.showAll();
  }

}
