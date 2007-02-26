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
import org.gnu.gtk.HBox;
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
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.TreeModelEvent;
import org.gnu.gtk.event.TreeModelListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;

import frysk.gui.monitor.eventviewer.EventViewer2;
import frysk.gui.sessions.Session;

public class SessionProcTreeView
    extends Widget
    implements Saveable
{

  private TreeView procTreeView;

  public SessionProcDataModel procDataModel;

  private TreeModelFilter procFilter;

  private PIDColumnDialog procPidColumnDialog;
  
  private ProcMenu procMenu;
  
  private String[] procColNames = {"Command", "VSZ", "RSS", "TIME", "PPID", "STAT", "NICE"}; 
  
  private boolean[] colVisible = {true, true, true, true, false, false, false};
  
  private Preferences prefs;
  
  private TreeStore treeStore;

  private LibGlade glade;
  
  private TreeViewColumn[] procTVC;
  
  private final EventViewer2 eventViewer;
  
  public SessionProcTreeView (LibGlade libGlade) throws IOException
  {
    super((libGlade.getWidget("allProcVBox")).getHandle());

    this.glade = libGlade;
    this.procTreeView = (TreeView) glade.getWidget("procTreeView");
    
    this.procPidColumnDialog = new PIDColumnDialog(this.glade, this);
   
    this.procTVC = new TreeViewColumn[8];
    
    this.procMenu = new ProcMenu(this.procPidColumnDialog, this);
  
    eventViewer = new EventViewer2();
    HBox box = (HBox) glade.getWidget("statusWidget");
    box.add(eventViewer);
    box.showAll();
    
    this.procDataModel = new SessionProcDataModel();
    
    
    this.mountProcModel(this.procDataModel);
    
    // Called when the user selects a different process in the procTreeView.
    this.procTreeView.getSelection().addListener(new TreeSelectionListener()
    {
      
      public void selectionChangedEvent (TreeSelectionEvent event)
      {
        if (procTreeView.getSelection().getSelectedRows().length > 0)
          {
            TreePath selected = procTreeView.getSelection().getSelectedRows()[0];
            
            GuiProc data = (GuiProc) procFilter.getValue(
                                                         procFilter.getIter(selected),
                                                         procDataModel.getProcDataDC());
                        
            if (!data.getFullExecutablePath().contains("(deleted"))
            	WindowManager.theManager.mainWindowStatusBar.push(
                                                              0,
                                                              data.getFullExecutablePath());
            else
            	WindowManager.theManager.mainWindowStatusBar.push(
                        0,
                        data.getExecutableName());
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

  public void refreshProcTree()
  {
    this.procDataModel.refreshProcRead();
    setProcCols();
  }
  
  public void setProcCols()
  {
    for (int i = 0; i < procColNames.length; i++)
      this.procTVC[i + 1].setVisible(prefs.getBoolean( procColNames[i],
                                                       this.colVisible[i]));
  }
  
  public String[] getProcColNames()
  {
    return this.procColNames;
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

  public void save (Preferences prefs)
  {
    this.procPidColumnDialog.save(prefs);
  }

  public void load (Preferences prefs)
  {
    this.prefs = prefs;
    this.procPidColumnDialog.load(prefs);
    setProcCols();
  }

  public void setSession (Session session)
  {
    this.procDataModel.setSession(session);
    this.eventViewer.setSession(session);
  }

}
