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

import java.util.LinkedList;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;

public class ProcWiseTreeView
    extends TreeView
{

  public ProcWiseDataModel dataModel;

  private TreeModelFilter removedProcFilter;
  
  private TreeStore treeStore;
  
  private final TreeViewColumn pwtvTVC[] = new TreeViewColumn[6];

  public ProcWiseTreeView (Handle handle, ProcWiseDataModel model)
  {
    super(handle);
    this.treeStore = model.getModel();
    this.dataModel = model;
  }
  
  private boolean shown;
  
  public void setFilter (boolean shown)
  {
    this.shown = shown;
    this.removedProcFilter = new TreeModelFilter(dataModel.getModel());

    this.removedProcFilter.setVisibleMethod(new TreeModelFilterVisibleMethod()
    {

      public boolean filter (TreeModel model, TreeIter iter)
      {
        if (model.getValue(iter, dataModel.getSelectedDC()) == false)
          {
            return ProcWiseTreeView.this.shown;
          }
        else
          {
            return !ProcWiseTreeView.this.shown;
          }
      }
    });
    
    mountDataModel(this.dataModel);
  }

  public void mountDataModel (final ProcWiseDataModel dataModel)
  {
    setUpColumns();
    
    this.setHeadersClickable(true);
    this.setHeadersVisible(true);

    this.appendColumn(this.pwtvTVC[0]);
    this.appendColumn(this.pwtvTVC[1]);
    this.appendColumn(this.pwtvTVC[2]);
    this.appendColumn(this.pwtvTVC[3]);
    this.appendColumn(this.pwtvTVC[4]);
    this.appendColumn(this.pwtvTVC[5]);

    this.setEnableSearch(true);
    this.treeStore.setSortColumn(dataModel.getNameDC(), SortType.ASCENDING);

    this.setModel(removedProcFilter);
    this.pwtvTVC[0].setVisible(true);
    this.pwtvTVC[1].setVisible(true);
    this.pwtvTVC[2].setVisible(true);
    this.pwtvTVC[3].setVisible(true);
    this.pwtvTVC[4].setVisible(true);
    this.pwtvTVC[5].setVisible(true);
    this.expandAll();
    
  }
  
  private void setUpColumns ()
  {
    this.pwtvTVC[0] = new TreeViewColumn();
    this.pwtvTVC[0].setClickable(true);
    this.pwtvTVC[0].setReorderable(true);
    CellRendererText renderNameText = new CellRendererText();
    this.pwtvTVC[0].packStart(renderNameText, true);
    this.pwtvTVC[0].setTitle("Process Name");
    this.pwtvTVC[0].addAttributeMapping(renderNameText,
                                   CellRendererText.Attribute.TEXT, this.dataModel.getNameDC());
    this.pwtvTVC[0].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[0].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getNameDC(), SortType.DESCENDING);
            pwtvTVC[0].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getNameDC(), SortType.ASCENDING);
            pwtvTVC[0].setSortOrder(SortType.ASCENDING);
          }
        pwtvTVC[0].setSortIndicator(true);
        pwtvTVC[1].setSortIndicator(false);
        pwtvTVC[2].setSortIndicator(false);
        pwtvTVC[3].setSortIndicator(false);
        pwtvTVC[4].setSortIndicator(false);
        pwtvTVC[5].setSortIndicator(false);

      }
    });
    this.pwtvTVC[0].setSortOrder(SortType.ASCENDING);
    this.pwtvTVC[0].setSortIndicator(true);
    
    this.pwtvTVC[1] = new TreeViewColumn();
    this.pwtvTVC[1].setClickable(true);
    this.pwtvTVC[1].setReorderable(true);
    CellRendererText renderPIDText = new CellRendererText();
    this.pwtvTVC[1].packStart(renderPIDText, true);
    this.pwtvTVC[1].setTitle("PID");
    this.pwtvTVC[1].addAttributeMapping(renderPIDText,
                                  CellRendererText.Attribute.TEXT, this.dataModel.getPIDDC());

    this.pwtvTVC[1].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[1].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getPIDDC(), SortType.DESCENDING);
            pwtvTVC[1].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getPIDDC(), SortType.ASCENDING);
            pwtvTVC[1].setSortOrder(SortType.ASCENDING);

          }
        pwtvTVC[0].setSortIndicator(false);
        pwtvTVC[1].setSortIndicator(true);
        pwtvTVC[2].setSortIndicator(false);
        pwtvTVC[3].setSortIndicator(false);
        pwtvTVC[4].setSortIndicator(false);
        pwtvTVC[5].setSortIndicator(false);
      }
    });
    this.pwtvTVC[2] = new TreeViewColumn();
    CellRendererText renderDirText = new CellRendererText();
    this.pwtvTVC[2].packStart(renderDirText, true);
    this.pwtvTVC[2].setTitle("Location");
    this.pwtvTVC[2].addAttributeMapping(renderDirText,
                                  CellRendererText.Attribute.TEXT,
                                  this.dataModel.getLocationDC());

    this.pwtvTVC[2].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[2].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getLocationDC(), SortType.DESCENDING);
            pwtvTVC[2].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getLocationDC(), SortType.ASCENDING);
            pwtvTVC[2].setSortOrder(SortType.ASCENDING);

          }
        pwtvTVC[0].setSortIndicator(false);
        pwtvTVC[1].setSortIndicator(false);
        pwtvTVC[2].setSortIndicator(true);
        pwtvTVC[3].setSortIndicator(false);
        pwtvTVC[4].setSortIndicator(false);
        pwtvTVC[5].setSortIndicator(false);

      }
    });
    this.pwtvTVC[2].setReorderable(true);
    this.pwtvTVC[2].setClickable(true);
    
    this.pwtvTVC[3] = new TreeViewColumn();
    CellRendererText renderVSZText = new CellRendererText();
    this.pwtvTVC[3].packStart(renderVSZText, true);
    this.pwtvTVC[3].setTitle("VSZ (kb)");
    this.pwtvTVC[3].addAttributeMapping(renderVSZText,
                                  CellRendererText.Attribute.TEXT,
                                  this.dataModel.getVszDC());

    this.pwtvTVC[3].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[3].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getVszDC(), SortType.DESCENDING);
            pwtvTVC[3].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getVszDC(), SortType.ASCENDING);
            pwtvTVC[3].setSortOrder(SortType.ASCENDING);

          }
        pwtvTVC[0].setSortIndicator(false);
        pwtvTVC[1].setSortIndicator(false);
        pwtvTVC[2].setSortIndicator(false);
        pwtvTVC[3].setSortIndicator(true);
        pwtvTVC[4].setSortIndicator(false);
        pwtvTVC[5].setSortIndicator(false);

      }
    });
    this.pwtvTVC[3].setReorderable(true);
    this.pwtvTVC[3].setClickable(true);
    
    this.pwtvTVC[4] = new TreeViewColumn();
    CellRendererText renderRSSText = new CellRendererText();
    this.pwtvTVC[4].packStart(renderRSSText, true);
    this.pwtvTVC[4].setTitle("RSS (kb)");
    this.pwtvTVC[4].addAttributeMapping(renderRSSText,
                                  CellRendererText.Attribute.TEXT,
                                  this.dataModel.getRssDC());

    this.pwtvTVC[4].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[4].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getRssDC(), SortType.DESCENDING);
            pwtvTVC[4].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getRssDC(), SortType.ASCENDING);
            pwtvTVC[4].setSortOrder(SortType.ASCENDING);

          }
        pwtvTVC[0].setSortIndicator(false);
        pwtvTVC[1].setSortIndicator(false);
        pwtvTVC[2].setSortIndicator(false);
        pwtvTVC[3].setSortIndicator(false);
        pwtvTVC[4].setSortIndicator(true);
        pwtvTVC[5].setSortIndicator(false);

      }
    });
    this.pwtvTVC[4].setReorderable(true);
    this.pwtvTVC[4].setClickable(true);
    
    this.pwtvTVC[5] = new TreeViewColumn();
    CellRendererText renderTimeText = new CellRendererText();
    this.pwtvTVC[5].packStart(renderTimeText, true);
    this.pwtvTVC[5].setTitle("TIME");
    this.pwtvTVC[5].addAttributeMapping(renderTimeText,
                                  CellRendererText.Attribute.TEXT,
                                  this.dataModel.getTimeDC());

    this.pwtvTVC[5].addListener(new TreeViewColumnListener()
    {
      public void columnClickedEvent (TreeViewColumnEvent event)
      {
        if (pwtvTVC[5].getSortOrder() == SortType.ASCENDING)
          {
            treeStore.setSortColumn(dataModel.getTimeDC(), SortType.DESCENDING);
            pwtvTVC[5].setSortOrder(SortType.DESCENDING);
          }
        else
          {
            treeStore.setSortColumn(dataModel.getTimeDC(), SortType.ASCENDING);
            pwtvTVC[5].setSortOrder(SortType.ASCENDING);

          }
        pwtvTVC[0].setSortIndicator(false);
        pwtvTVC[1].setSortIndicator(false);
        pwtvTVC[2].setSortIndicator(false);
        pwtvTVC[3].setSortIndicator(false);
        pwtvTVC[4].setSortIndicator(false);
        pwtvTVC[5].setSortIndicator(true);

      }
    });
    this.pwtvTVC[5].setReorderable(true);
    this.pwtvTVC[5].setClickable(true);
  }
  
  public LinkedList getSelectedObjects ()
  {

    LinkedList selecteds = new LinkedList();
    TreePath[] selectedPaths = this.getSelection().getSelectedRows();

    /* Check for no selected rows */
    if (selectedPaths.length > 0)
      {

        for (int i = 0; i < selectedPaths.length; i++)
          {
            
            TreePath realPath = this.removedProcFilter.convertPathToChildPath(selectedPaths[i]);
            selecteds.add((GuiObject) this.treeStore.getValue(
                                                              this.treeStore.getIter(realPath),
                                                              this.dataModel.getObjectDC()));
          }
        return selecteds;
      }
    else
      {
        return null;
      }
  }
  
  public GuiObject getSelectedObject ()
  {

    GuiObject selected = null;

    /* Check for no selected rows */
    if (this.getSelection().getSelectedRows().length > 0)
      {
        TreePath realPath = this.removedProcFilter.convertPathToChildPath(this.getSelection().getSelectedRows()[0]);
        selected = (GuiObject) this.treeStore.getValue(
                                                       this.treeStore.getIter(realPath),
                                                       this.dataModel.getObjectDC());
      }
    return selected;
  }
}
