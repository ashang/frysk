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

  public SessionProcDataModel procDataModel;

  private VPaned vPane;

  private TreeModelFilter procFilter;

  private TreeModelFilter threadFilter;

  private VBox statusWidget;
  
  private InfoWidget infoWidget;
  
  private PIDColumnDialog procPidColumnDialog;
  
  private TIDColumnDialog threadTidColumnDialog;
  
  private ProcMenu procMenu;
  
  private ThreadMenu threadMenu;
  
  private String[] procColNames = {"Command", "VSZ", "RSS", "TIME", "PPID", "STAT", "NICE"}; 
  
  private String[] threadColNames = {"Entry function", "VSZ", "RSS", "TIME", "PPID", "STAT", "NICE"};
  
  private boolean[] colVisible = {true, true, true, true, false, false, false};
  
  private Preferences prefs;
  
  private TreeStore treeStore;

  private LibGlade glade;
  
  private TreeViewColumn[] procTVC;
  
  private TreeViewColumn[] threadTVC;

  public SessionProcTreeView (LibGlade libGlade) throws IOException
  {
    super((libGlade.getWidget("allProcVBox")).getHandle());

    this.glade = libGlade;
    this.procTreeView = (TreeView) glade.getWidget("procTreeView");
    this.threadTreeView = (TreeView) glade.getWidget("threadTreeView");
    
    this.procPidColumnDialog = new PIDColumnDialog(this.glade, this);
    this.threadTidColumnDialog = new TIDColumnDialog(this.glade, this);
    
    this.procTVC = new TreeViewColumn[8];
    this.threadTVC = new TreeViewColumn[8];
    
    this.procMenu = new ProcMenu(this.procPidColumnDialog, this);
    this.threadMenu = new ThreadMenu(this.threadTidColumnDialog, this);

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
            
            if (!data.getFullExecutablePath().contains("(deleted"))
            	WindowManager.theManager.mainWindowStatusBar.push(
                                                              0,
                                                              data.getFullExecutablePath());
            else
            	WindowManager.theManager.mainWindowStatusBar.push(
                        0,
                        data.getExecutableName());

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
              threadMenu.popup(data);

            // System.out.println("click : " + data); //$NON-NLS-1$
            return true;
          }
        return false;
      }
    });
    
    this.procPidColumnDialog.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          SessionProcTreeView.this.setProcCols();
      }
    });
    
    this.threadTidColumnDialog.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          SessionProcTreeView.this.setThreadCols();
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
    procTVC[0] = new TreeViewColumn();
    procTVC[0].packStart(cellRendererText3, false);
    procTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.TEXT,
                               dataModel.getTidDC());
    procTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.FOREGROUND,
                               dataModel.getColorDC());
    procTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.WEIGHT,
                               dataModel.getWeightDC());
    // procTVC[0].addAttributeMapping(cellRendererText3,
    // CellRendererText.Attribute.STRIKETHROUGH,psDataModel.getSensitiveDC());

    CellRendererText cellRendererText4 = new CellRendererText();
    procTVC[1] = new TreeViewColumn();
    procTVC[1].packStart(cellRendererText4, false);
    procTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getCommandDC());
    procTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    // procTVC[1].addAttributeMapping(cellRendererText4,
    // CellRendererText.Attribute.STRIKETHROUGH ,psDataModel.getSensitiveDC());
    
    CellRendererText cellRendererText5 = new CellRendererText();
    procTVC[2] = new TreeViewColumn();
    procTVC[2].packStart(cellRendererText5, false);
    procTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getVszDC());
    procTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText6 = new CellRendererText();
    procTVC[3] = new TreeViewColumn();
    procTVC[3].packStart(cellRendererText6, false);
    procTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getRssDC());
    procTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText7 = new CellRendererText();
    procTVC[4] = new TreeViewColumn();
    procTVC[4].packStart(cellRendererText7, false);
    procTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getTimeDC());
    procTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());

    CellRendererText cellRendererText8 = new CellRendererText();
    procTVC[5] = new TreeViewColumn();
    procTVC[5].packStart(cellRendererText8, false);
    procTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getPPIDDC());
    procTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText9 = new CellRendererText();
    procTVC[6] = new TreeViewColumn();
    procTVC[6].packStart(cellRendererText9, false);
    procTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getStateDC());
    procTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    CellRendererText cellRendererText10 = new CellRendererText();
    procTVC[7] = new TreeViewColumn();
    procTVC[7].packStart(cellRendererText10, false);
    procTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.TEXT,
                                   dataModel.getNiceDC());
    procTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.FOREGROUND,
                                   dataModel.getColorDC());
    procTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.WEIGHT,
                                   dataModel.getWeightDC());
    
    
    
    procTVC[0].setTitle("PID"); //$NON-NLS-1$
    procTVC[0].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTidDC());
        
        if (procTVC[0].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getTidDC(), SortType.DESCENDING);
            procTVC[0].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getTidDC(), SortType.ASCENDING);
            procTVC[0].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[1].setTitle("Command"); //$NON-NLS-1$
    procTVC[1].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getCommandDC());
        
        if (procTVC[1].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getCommandDC(), SortType.DESCENDING);
            procTVC[1].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getCommandDC(), SortType.ASCENDING);
            procTVC[1].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[2].setTitle("VSZ"); //$NON-NLS-1$
    procTVC[2].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getVszDC());
        
        if (procTVC[2].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getVszDC(), SortType.DESCENDING);
            procTVC[2].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getVszDC(), SortType.ASCENDING);
            procTVC[2].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[3].setTitle("RSS"); //$NON-NLS-1$
    procTVC[3].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getRssDC());
        
        if (procTVC[3].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getRssDC(), SortType.DESCENDING);
            procTVC[3].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getRssDC(), SortType.ASCENDING);
            procTVC[3].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[4].setTitle("TIME"); //$NON-NLS-1$
    procTVC[4].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (procTVC[4].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(), SortType.DESCENDING);
            procTVC[4].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(), SortType.ASCENDING);
            procTVC[4].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[5].setTitle("PPID"); //$NON-NLS-1$
    procTVC[5].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (procTVC[5].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(), SortType.DESCENDING);
            procTVC[5].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(), SortType.ASCENDING);
            procTVC[5].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[6].setTitle("STAT"); //$NON-NLS-1$
    procTVC[6].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (procTVC[6].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getStateDC(), SortType.DESCENDING);
            procTVC[6].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getStateDC(), SortType.ASCENDING);
            procTVC[6].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
    procTVC[7].setTitle("NICE"); //$NON-NLS-1$
    procTVC[7].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        procTreeView.setSearchDataColumn(dataModel.getTimeDC());
        
        if (procTVC[7].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(), SortType.DESCENDING);
            procTVC[7].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(), SortType.ASCENDING);
            procTVC[7].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    procTVC[0].setVisible(true);
    procTVC[1].setVisible(true);
    procTVC[2].setVisible(true);
    procTVC[3].setVisible(true);
    procTVC[4].setVisible(true);
    procTVC[5].setVisible(true);
    procTVC[6].setVisible(true);
    procTVC[7].setVisible(true);

    this.procTreeView.appendColumn(procTVC[0]);
    this.procTreeView.appendColumn(procTVC[1]);
    this.procTreeView.appendColumn(procTVC[2]);
    this.procTreeView.appendColumn(procTVC[3]);
    this.procTreeView.appendColumn(procTVC[4]);
    this.procTreeView.appendColumn(procTVC[5]);
    this.procTreeView.appendColumn(procTVC[6]);
    this.procTreeView.appendColumn(procTVC[7]);

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
                                                                                       dataModel.getTidDC()))
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
    
    threadTVC[0].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getTidDC());

        if (threadTVC[0].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getTidDC(),
                                    SortType.DESCENDING);
            threadTVC[0].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getTidDC(),
                                    SortType.ASCENDING);
            threadTVC[0].setSortOrder(SortType.ASCENDING);
          }
      }
    });
    
//    threadTVC[1].addListener(new TreeViewColumnListener()
//    {
//      public void columnClickedEvent (TreeViewColumnEvent arg0)
//      {
//        threadTreeView.setSearchDataColumn(dataModel.getTidDC());
//
//        if (threadTVC[1].getSortOrder() == SortType.ASCENDING)
//          {
//            treeStore.setSortColumn(procDataModel.getTidDC(),
//                                    SortType.DESCENDING);
//            threadTVC[1].setSortOrder(SortType.DESCENDING);
//          }
//        else
//          {
//            treeStore.setSortColumn(procDataModel.getTidDC(),
//                                    SortType.ASCENDING);
//            threadTVC[1].setSortOrder(SortType.ASCENDING);
//          }
//      }
//    });

    threadTVC[2].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getVszDC());

        if (threadTVC[2].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getVszDC(),
                                    SortType.DESCENDING);
            threadTVC[2].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getVszDC(),
                                    SortType.ASCENDING);
            threadTVC[2].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    threadTVC[3].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getRssDC());

        if (threadTVC[3].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getRssDC(),
                                    SortType.DESCENDING);
            threadTVC[3].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getRssDC(),
                                    SortType.ASCENDING);
            threadTVC[3].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    threadTVC[4].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getTimeDC());

        if (threadTVC[4].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(),
                                    SortType.DESCENDING);
            threadTVC[4].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getTimeDC(),
                                    SortType.ASCENDING);
            threadTVC[4].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    threadTVC[5].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getPPIDDC());

        if (threadTVC[5].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(),
                                    SortType.DESCENDING);
            threadTVC[5].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getPPIDDC(),
                                    SortType.ASCENDING);
            threadTVC[5].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    threadTVC[6].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getStateDC());

        if (threadTVC[6].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getStateDC(),
                                    SortType.DESCENDING);
            threadTVC[6].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getStateDC(),
                                    SortType.ASCENDING);
            threadTVC[6].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    threadTVC[7].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent arg0)
      {
        threadTreeView.setSearchDataColumn(dataModel.getNiceDC());

        if (threadTVC[7].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(),
                                    SortType.DESCENDING);
            threadTVC[7].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(procDataModel.getNiceDC(),
                                    SortType.ASCENDING);
            threadTVC[7].setSortOrder(SortType.ASCENDING);
          }
      }
    });

    this.threadTreeView.setModel(threadFilter);
    refreshThreadTree();
  }

  private void threadViewInit (SessionProcDataModel dataModel)
  {

    CellRendererText cellRendererText3 = new CellRendererText();
    threadTVC[0] = new TreeViewColumn();
    threadTVC[0].packStart(cellRendererText3, false);
    threadTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.TEXT,
                               procDataModel.getTidDC());
    threadTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.FOREGROUND,
                               procDataModel.getColorDC());
    threadTVC[0].addAttributeMapping(cellRendererText3,
                               CellRendererText.Attribute.WEIGHT,
                               procDataModel.getWeightDC());

    CellRendererText cellRendererText4 = new CellRendererText();
    threadTVC[1] = new TreeViewColumn();
    threadTVC[1].packStart(cellRendererText4, false);
    threadTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getCommandDC());
    threadTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[1].addAttributeMapping(cellRendererText4,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());

    CellRendererText cellRendererText5 = new CellRendererText();
    threadTVC[2] = new TreeViewColumn();
    threadTVC[2].packStart(cellRendererText5, false);
    threadTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getVszDC());
    threadTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[2].addAttributeMapping(cellRendererText5,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    CellRendererText cellRendererText6 = new CellRendererText();
    threadTVC[3] = new TreeViewColumn();
    threadTVC[3].packStart(cellRendererText6, false);
    threadTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getRssDC());
    threadTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[3].addAttributeMapping(cellRendererText6,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    CellRendererText cellRendererText7 = new CellRendererText();
    threadTVC[4] = new TreeViewColumn();
    threadTVC[4].packStart(cellRendererText7, false);
    threadTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getTimeDC());
    threadTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[4].addAttributeMapping(cellRendererText7,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    CellRendererText cellRendererText8 = new CellRendererText();
    threadTVC[5] = new TreeViewColumn();
    threadTVC[5].packStart(cellRendererText8, false);
    threadTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getPPIDDC());
    threadTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[5].addAttributeMapping(cellRendererText8,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    CellRendererText cellRendererText9 = new CellRendererText();
    threadTVC[6] = new TreeViewColumn();
    threadTVC[6].packStart(cellRendererText9, false);
    threadTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getStateDC());
    threadTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[6].addAttributeMapping(cellRendererText9,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    CellRendererText cellRendererText10 = new CellRendererText();
    threadTVC[7] = new TreeViewColumn();
    threadTVC[7].packStart(cellRendererText10, false);
    threadTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.TEXT,
                                   procDataModel.getNiceDC());
    threadTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.FOREGROUND,
                                   procDataModel.getColorDC());
    threadTVC[7].addAttributeMapping(cellRendererText10,
                                   CellRendererText.Attribute.WEIGHT,
                                   procDataModel.getWeightDC());
    
    
    threadTVC[0].setTitle("TID"); //$NON-NLS-1$
    threadTVC[1].setTitle("Entry Function"); //$NON-NLS-1$
    threadTVC[2].setTitle("VSZ"); //$NON-NLS-1$
    threadTVC[3].setTitle("RSS"); //$NON-NLS-1$
    threadTVC[4].setTitle("TIME"); //$NON-NLS-1$
    threadTVC[5].setTitle("PPID"); //$NON-NLS-1$
    threadTVC[6].setTitle("STAT"); //$NON-NLS-1$
    threadTVC[7].setTitle("NICE"); //$NON-NLS-1$

    threadTVC[0].setVisible(true);
    threadTVC[1].setVisible(true);
    threadTVC[2].setVisible(true);
    threadTVC[3].setVisible(true);
    threadTVC[4].setVisible(true);
    threadTVC[5].setVisible(true);
    threadTVC[6].setVisible(true);
    threadTVC[7].setVisible(true);

    this.threadTreeView.appendColumn(threadTVC[0]);
    this.threadTreeView.appendColumn(threadTVC[1]);
    this.threadTreeView.appendColumn(threadTVC[2]);
    this.threadTreeView.appendColumn(threadTVC[3]);
    this.threadTreeView.appendColumn(threadTVC[4]);
    this.threadTreeView.appendColumn(threadTVC[5]);
    this.threadTreeView.appendColumn(threadTVC[6]);
    this.threadTreeView.appendColumn(threadTVC[7]);

    procDataModel.getModel().addListener(new TreeModelListener()
    {

      public void treeModelEvent (TreeModelEvent event)
      {
        threadTreeView.expandAll();
      }

    });

    this.threadTreeView.expandAll();
  }
  
  public void refreshProcTree()
  {
    this.procDataModel.refreshProcRead();
    setProcCols();
  }
  
  public void refreshThreadTree()
  {
    this.procDataModel.refreshThreadRead(getSelectedProc());
    setThreadCols();
  }
  
  public void setProcCols()
  {
    for (int i = 0; i < procColNames.length; i++)
      this.procTVC[i + 1].setVisible(prefs.getBoolean( procColNames[i],
                                                       this.colVisible[i]));
  }
  
  public void setThreadCols()
  {
    for (int i = 0; i < threadColNames.length; i++)
      this.threadTVC[i + 1].setVisible(prefs.getBoolean( threadColNames[i],
                                                       this.colVisible[i]));
  }
  
  public String[] getProcColNames()
  {
    return this.procColNames;
  }
  
  public String[] getThreadColNames()
  {
    return this.threadColNames;
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
    model.getValue(model.getIter(tp[0]), this.procDataModel.getTidDC());

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
    model.getValue(model.getIter(tp[0]), this.procDataModel.getTidDC());

    return data;
  }

  public void save (Preferences prefs)
  {
    prefs.putInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
    this.procPidColumnDialog.save(prefs);
    this.threadTidColumnDialog.save(prefs);
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    int position = prefs.getInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
    this.vPane.setPosition(position);
    this.procPidColumnDialog.load(prefs);
    this.threadTidColumnDialog.load(prefs);
    setProcCols();
    setThreadCols();
  }

  public void setSession (Session session)
  {
    this.procDataModel.setSession(session);
  }

}
