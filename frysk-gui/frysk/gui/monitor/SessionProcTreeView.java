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
/*
 * Created on 8-Jul-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */


package frysk.gui.monitor;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade; 
import org.gnu.glib.GObject;
import org.gnu.glib.PropertyNotificationListener;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeSelection;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.VPaned;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.TreeModelEvent;
import org.gnu.gtk.event.TreeModelListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;

import frysk.gui.sessions.Session;

public class SessionProcTreeView
    extends Widget
    implements Saveable
{

  private TreeView procTreeView;

  private TreeView threadTreeView;

  private SessionProcDataModel procDataModel;

  private VPaned vPane;

  private TreeModelFilter procFilter;

  private TreeModelFilter threadFilter;

  private VBox statusWidget;
  
  private InfoWidget infoWidget;
  
  private PIDColumnDialog pidColumnDialog;
  
  private ProcMenu procMenu;
  
  private String[] colNames = {"Command", "VSZ", "RSS", "TIME", "PPID", "STAT", "NICE"}; 
  
  private boolean[] colVisible = {true, true, true, true, false, false, false};
  
  private Preferences prefs;
  
  private TreeStore treeStore;

  private LibGlade glade;
  
  private TreeViewColumn[] tvc;

  public SessionProcTreeView (LibGlade libGlade) throws IOException
  {
    super((libGlade.getWidget("allProcVBox")).getHandle());

    this.glade = libGlade;
    this.procTreeView = (TreeView) glade.getWidget("procTreeView");
    this.threadTreeView = (TreeView) glade.getWidget("threadTreeView");
    
    this.pidColumnDialog = new PIDColumnDialog(this.glade, this);
    
    this.tvc = new TreeViewColumn[8];
    
    this.procMenu = new ProcMenu(this.pidColumnDialog, this);

    this.vPane = (VPaned) glade.getWidget("vPane");

    this.statusWidget = (VBox) glade.getWidget("statusWidget");

    this.infoWidget = new InfoWidget();
    this.statusWidget.add(infoWidget);

    this.procDataModel = new SessionProcDataModel();

    this.mountProcModel(this.procDataModel);
    this.threadViewInit(procDataModel);

    // Called when the user selects a different process in the procTreeView.
    this.procTreeView.getSelection().addListener(new TreeSelectionListener()
    {
      
      public void selectionChangedEvent (TreeSelectionEvent event)
      {
        if (procTreeView.getSelection().getSelectedRows().length > 0)
          {
            TreePath selected = procTreeView.getSelection().getSelectedRows()[0];
            mountThreadModel(procDataModel, selected);

            GuiProc data = (GuiProc) procFilter.getValue(
                                                         procFilter.getIter(selected),
                                                         procDataModel.getProcDataDC());
            if (! data.hasWidget())
              {
                StatusWidget statusWidget = new StatusWidget(
                                                             data,
                                                             data.getProc().getCommand());
                data.setWidget(statusWidget, statusWidget.getTrace0());
              }
            infoWidget.setSelectedProc(data);

            // Sets the status bar to read the full executable path of the given
            // process.
            WindowManager.theManager.mainWindowStatusBar.push(
                                                              0,
                                                              data.getFullExecutablePath());

            // Sets the selected thread to the first thread.
            if (threadTreeView.getModel().getFirstIter() != null)
              {
                threadTreeView.getSelection().select(
                                                     threadTreeView.getModel().getFirstIter());
              }
          }
        else
          {
            infoWidget.setSelectedProc(null);
          }
      }
    });

    // called when a user selects a different thread from the threadTreeView.
    this.threadTreeView.getSelection().addListener(new TreeSelectionListener()
    {
      public void selectionChangedEvent (TreeSelectionEvent event)
      {
        if (procTreeView.getSelection().getSelectedRows().length > 0
            && threadTreeView.getSelection().getSelectedRows().length > 0)
          {
            TreePath selected = threadTreeView.getSelection().getSelectedRows()[0];

            GuiTask data = (GuiTask) threadFilter.getValue(
                                                           threadFilter.getIter(selected),
                                                           procDataModel.getProcDataDC());
            if (! data.hasWidget())
              {
                // GuiProc pdata = (GuiProc) procFilter.getValue(
                // procFilter.getIter(selected),
                // procDataModel.getProcDataDC());
                // GuiProc pdata =
                // GuiProcFactory.getGuiProc(data.getTask().getProc());
                // StatusWidget sw = (StatusWidget) pdata.getWidget();
                // int trace = sw.newTrace(data.getTask().getName(),
                // "Other useful per-trace information.");
                // data.setWidget(sw, trace);
              }

          }
        else
          {
            infoWidget.setSelectedTask(null);
          }
      }
    });

    this.procTreeView.setHeadersClickable(true);

    // Called when a user "right clicks" on a process.
    this.procTreeView.addListener(new MouseListener()
    {

      public boolean mouseEvent (MouseEvent event)
      {
        if (event.getType() == MouseEvent.Type.BUTTON_PRESS
            & event.getButtonPressed() == MouseEvent.BUTTON3)
          {

            GuiProc data = getSelectedProc();
            if (data != null)
              procMenu.popup(data);

            // System.out.println("click : " + data); //$NON-NLS-1$
            return true;
          }
        return false;
      }
    });

    this.threadTreeView.addListener(new MouseListener()
    {

      public boolean mouseEvent (MouseEvent event)
      {
        if (event.getType() == MouseEvent.Type.BUTTON_PRESS
            & event.getButtonPressed() == MouseEvent.BUTTON3)
          {

            GuiTask data = getSelectedThread();
            if (data != null)
              ThreadMenu.getMenu().popup(data);

            // System.out.println("click : " + data); //$NON-NLS-1$
            return true;
          }
        return false;
      }
    });
    
    this.pidColumnDialog.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          SessionProcTreeView.this.setCols();
      }
    });

  }

  public void mountProcModel (final SessionProcDataModel dataModel)
  {

    // this.procTreeView.setModel(psDataModel.getModel());

    this.procFilter = new TreeModelFilter(dataModel.getModel());

    procFilter.setVisibleMethod(new TreeModelFilterVisibleMethod()
    {

      public boolean filter (TreeModel model, TreeIter iter)
      {

        if (model.getValue(iter, dataModel.getSensitiveDC()) == false)
          {
            return false;
          }

        if (model.getValue(iter, dataModel.getHasParentDC()) == false)
          {
            return true;
          }
        else
          {
            return false;
          }
      }

    });

    this.procTreeView.setModel(procFilter);
    this.procTreeView.setSearchDataColumn(dataModel.getCommandDC());
    this.treeStore = procDataModel.getTreeStore();

    CellRendererText cellRendererText3 = new CellRendererText();
    tvc[0] = new TreeViewColumn();
    tvc[0].packStart(cellRendererText3, false);
    tvc[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.TEXT,
                               dataModel.getPidDC());
    tvc[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.FOREGROUND,
                               dataModel.getColorDC());
    tvc[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.WEIGHT,
                               dataModel.getWeightDC());
    // tvc[0].addAttributeMapping(cellRendererText3,
    // CellRendererText.Attribute.STRIKETHROUGH,psDataModel.getSensitiveDC());

    CellRendererText cellRendererText4 = new CellRendererText();
    tvc[1] = new TreeViewColumn();
    tvc[1].packStart(cellRendererText4, false);
    tvc[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getCommandDC());
    tvc[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    // tvc[1].addAttributeMapping(cellRendererText4,
    // CellRendererText.Attribute.STRIKETHROUGH ,psDataModel.getSensitiveDC());
    
    CellRendererText cellRendererText5 = new CellRendererText();
    tvc[2] = new TreeViewColumn();
    tvc[2].packStart(cellRendererText5, false);
    tvc[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getVszDC());
    tvc[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText6 = new CellRendererText();
    tvc[3] = new TreeViewColumn();
    tvc[3].packStart(cellRendererText6, false);
    tvc[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getRssDC());
    tvc[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText7 = new CellRendererText();
    tvc[4] = new TreeViewColumn();
    tvc[4].packStart(cellRendererText7, false);
    tvc[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getTimeDC());
    tvc[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());

    CellRendererText cellRendererText8 = new CellRendererText();
    tvc[5] = new TreeViewColumn();
    tvc[5].packStart(cellRendererText8, false);
    tvc[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getPPIDDC());
    tvc[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText9 = new CellRendererText();
    tvc[6] = new TreeViewColumn();
    tvc[6].packStart(cellRendererText9, false);
    tvc[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getStateDC());
    tvc[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText10 = new CellRendererText();
    tvc[7] = new TreeViewColumn();
    tvc[7].packStart(cellRendererText10, false);
    tvc[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getNiceDC());
    tvc[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    tvc[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    
    
    tvc[0].setTitle("PID"); //$NON-NLS-1$
    tvc[0].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getPidDC());
        
        if (tvc[0].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getPidDC(), SortType.DESCENDING);
            tvc[0].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getPidDC(), SortType.ASCENDING);
            tvc[0].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[1].setTitle("Command"); //$NON-NLS-1$
    tvc[1].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getCommandDC());
        
        if (tvc[1].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getCommandDC(), SortType.DESCENDING);
            tvc[1].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getCommandDC(), SortType.ASCENDING);
            tvc[1].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[2].setTitle("VSZ"); //$NON-NLS-1$
    tvc[2].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getVszDC());
        
        if (tvc[2].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getVszDC(), SortType.DESCENDING);
            tvc[2].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getVszDC(), SortType.ASCENDING);
            tvc[2].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[3].setTitle("RSS"); //$NON-NLS-1$
    tvc[3].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getRssDC());
        
        if (tvc[3].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getRssDC(), SortType.DESCENDING);
            tvc[3].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getRssDC(), SortType.ASCENDING);
            tvc[3].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[4].setTitle("TIME"); //$NON-NLS-1$
    tvc[4].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (tvc[4].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(), SortType.DESCENDING);
            tvc[4].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(), SortType.ASCENDING);
            tvc[4].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[5].setTitle("PPID"); //$NON-NLS-1$
    tvc[5].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (tvc[5].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(), SortType.DESCENDING);
            tvc[5].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(), SortType.ASCENDING);
            tvc[5].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[6].setTitle("STAT"); //$NON-NLS-1$
    tvc[6].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (tvc[6].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getStateDC(), SortType.DESCENDING);
            tvc[6].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getStateDC(), SortType.ASCENDING);
            tvc[6].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    tvc[7].setTitle("NICE"); //$NON-NLS-1$
    tvc[7].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (tvc[7].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(), SortType.DESCENDING);
            tvc[7].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(), SortType.ASCENDING);
            tvc[7].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    tvc[0].setVisible(true);
    tvc[1].setVisible(true);
    tvc[2].setVisible(true);
    tvc[3].setVisible(true);
    tvc[4].setVisible(true);
    tvc[5].setVisible(true);
    tvc[6].setVisible(true);
    tvc[7].setVisible(true);

    this.procTreeView.appendColumn(tvc[0]);
    this.procTreeView.appendColumn(tvc[1]);
    this.procTreeView.appendColumn(tvc[2]);
    this.procTreeView.appendColumn(tvc[3]);
    this.procTreeView.appendColumn(tvc[4]);
    this.procTreeView.appendColumn(tvc[5]);
    this.procTreeView.appendColumn(tvc[6]);
    this.procTreeView.appendColumn(tvc[7]);

    dataModel.getModel().addListener(new PropertyNotificationListener()
    {
      public void notify (GObject arg0, String arg1)
      {
        // System.out.println("Notification : " + arg1); //$NON-NLS-1$
      }
    });

    dataModel.getModel().addListener(new TreeModelListener()
    {

      public void treeModelEvent (TreeModelEvent event)
      {
        procTreeView.expandAll();
      }

    });

    this.procTreeView.expandAll();
  }

  public void mountThreadModel (final SessionProcDataModel dataModel,
                                final TreePath relativeRoot)
  {
    final TreePath root = this.procFilter.convertPathToChildPath(relativeRoot);
    this.threadFilter = new TreeModelFilter(dataModel.getModel(), root);

    threadFilter.setVisibleMethod(new TreeModelFilterVisibleMethod()
    {

      public boolean filter (TreeModel model, TreeIter iter)
      {

        if (relativeRoot == null)
          {
            return false;
          }
        if (model.getValue(iter, dataModel.getThreadParentDC()) == procFilter.getValue(
                                                                                       procFilter.getIter(relativeRoot),
                                                                                       dataModel.getPidDC()))
          {
            return true;
          }
        else
          {
            return false;
          }

        // if(model.getValue(iter, psDataModel.getSensitiveDC()) == false){
        // return false;
        // }

      }
    });

    this.threadTreeView.setModel(threadFilter);
  }

  private void threadViewInit (SessionProcDataModel procDataModel)
  {
    TreeViewColumn pidCol = new TreeViewColumn();
    TreeViewColumn commandCol = new TreeViewColumn();

    CellRendererText cellRendererText3 = new CellRendererText();
    pidCol.packStart(cellRendererText3, false);
    pidCol.addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.TEXT,
                               procDataModel.getPidDC());
    pidCol.addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.FOREGROUND,
                               procDataModel.getColorDC());
    pidCol.addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.WEIGHT,
                               procDataModel.getWeightDC());

    CellRendererText cellRendererText4 = new CellRendererText();
    commandCol.packStart(cellRendererText4, false);
    commandCol.addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getCommandDC());
    commandCol.addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    commandCol.addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());

    pidCol.setTitle("PID"); //$NON-NLS-1$
    commandCol.setTitle("Entry Functions"); //$NON-NLS-1$

    pidCol.setVisible(true);
    commandCol.setVisible(true);

    this.threadTreeView.appendColumn(pidCol);
    this.threadTreeView.appendColumn(commandCol);

    procDataModel.getModel().addListener(new TreeModelListener()
    {

      public void treeModelEvent (TreeModelEvent event)
      {
        threadTreeView.expandAll();
      }

    });

    this.threadTreeView.expandAll();
  }
  
  public void refreshTree()
  {
    this.procDataModel.refreshRead();
    setCols();
  }
  
  public void setCols()
  {
    for (int i = 0; i < colNames.length; i++)
      this.tvc[i + 1].setVisible(prefs.getBoolean( colNames[i],
                                                       this.colVisible[i]));
  }
  
  public String[] getColNames()
  {
    return this.colNames;
  }

  private GuiProc getSelectedProc ()
  {
    TreeSelection ts = this.procTreeView.getSelection();
    TreePath[] tp = ts.getSelectedRows();

    if (tp.length == 0)
      {
        return null;
      }

    TreeModel model = this.procFilter;
    GuiProc data = (GuiProc) model.getValue(model.getIter(tp[0]),
                                            this.procDataModel.getProcDataDC());
    model.getValue(model.getIter(tp[0]), this.procDataModel.getPidDC());

    return data;
  }

  private GuiTask getSelectedThread ()
  {
    TreeSelection ts = this.threadTreeView.getSelection();
    TreePath[] tp = ts.getSelectedRows();

    if (tp.length == 0)
      {
        return null;
      }

    TreeModel model = this.threadFilter;
    GuiTask data = (GuiTask) model.getValue(model.getIter(tp[0]),
                                            this.procDataModel.getProcDataDC());
    model.getValue(model.getIter(tp[0]), this.procDataModel.getPidDC());

    return data;
  }

  public void save (Preferences prefs)
  {
    prefs.putInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
    this.pidColumnDialog.save(prefs);
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    int position = prefs.getInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
    this.vPane.setPosition(position);
    this.pidColumnDialog.load(prefs);
    setCols();
  }

  public void setSession (Session session)
  {
    this.procDataModel.setSession(session);
  }

}
