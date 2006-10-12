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


package frysk.gui.srcwin;

import java.util.Iterator;
import java.util.Vector;

import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.lang.Variable;

public class VariableWatchView
    extends TreeView
    implements TreeSelectionListener
{

  public interface WatchViewListener
  {
    void variableSelected (Variable var);
  }

  private DataColumn[] traceColumns;

  private Vector observers;
  
  private SourceWindow parent;
  
  private VariableWatchListener listener;
  
  private ListStore model;
  
  private int treeSize = 0;

  public VariableWatchView (SourceWindow sw)
  {
    super();

    this.parent = sw;
    this.setName("varWatchView");
    this.getAccessible().setName("varWatchView_variableWatchList");
    this.getAccessible().setDescription(
                                        "A list of all the variables that are being watched");

    this.observers = new Vector();

    traceColumns = new DataColumn[] { new DataColumnString(),
                                     new DataColumnString(),
                                     new DataColumnObject() };

    this.model = new ListStore(traceColumns);

    this.setModel(model);

    TreeViewColumn column = new TreeViewColumn();
    column.setTitle("Name");
    CellRenderer renderer = new CellRendererText();
    column.packStart(renderer, true);
    column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                               traceColumns[0]);
    this.appendColumn(column);

    column = new TreeViewColumn();
    column.setTitle("Value");
    renderer = new CellRendererText();
    column.packStart(renderer, true);
    column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                               traceColumns[1]);
    this.appendColumn(column);

    this.getSelection().setMode(SelectionMode.SINGLE);

    this.getSelection().addListener(this);
    listener = new VariableWatchListener();
    this.addListener(listener);
  }

  /**
   * Addes a Variable row to this TreeView.
   * 
   * @param var
   */
  public void addTrace (Variable var)
  {
    TreeIter iter = this.model.appendRow();
    this.treeSize++;
    
    this.model.setValue(iter, (DataColumnString) this.traceColumns[0], var.getText());
    this.model.setValue(iter, (DataColumnString) this.traceColumns[1],
                       "" + var.toString());
    this.model.setValue(iter, (DataColumnObject) this.traceColumns[2], var);
    this.showAll();
  }
  
  /**
   * Removes the incoming Variable from this TreeView.
   * 
   * @param var The Variable to be removed.
   */
  public void removeTrace (Variable var)
  {
    TreeIter iter = this.model.getFirstIter();

    while (iter != null)
      {
        if (model.isIterValid(iter))
          {
            Variable v = (Variable) model.getValue(iter,
                         (DataColumnObject) traceColumns[2]);

            if (v.getText().equals(var.getText()))
              {
                this.model.removeRow(iter);
                this.treeSize--;
              }
          }
        if (this.treeSize != 0)
          iter = iter.getNextIter();
        else
          break;
      }
  }

  /**
   * Adds a listener to this list of observers.
   * 
   * @param listener    The Listener to be added.
   */
  public void addObserver (WatchViewListener listener)
  {
    this.observers.add(listener);
  }

  /**
   * Notifies all observers of the selected Variable
   * 
   * @param var The selected Variable.
   */
  private void notifyListeners (Variable var)
  {
    Iterator iter = this.observers.iterator();

    while (iter.hasNext())
      ((WatchViewListener) iter.next()).variableSelected(var);
  }

  /**
   * Called when the selection in this TreeView has changed.
   */
  public void selectionChangedEvent (TreeSelectionEvent arg0)
  {
    TreeIter selected = null;
    
    try
    {
      selected = this.model.getIter(this.getSelection().getSelectedRows()[0]);
    }
    catch (ArrayIndexOutOfBoundsException aoobe)
    {
      return;
    }
    
    this.notifyListeners((Variable) this.model.getValue(
                    selected, (DataColumnObject) this.traceColumns[2]));
  }
  
  /**
   * Generates a new right-click menu when a row with a Variable
   * is clicked.
   * 
   * @param event   The click event
   */
  public void clickedOnVariable(MouseEvent event)
  {
    Menu m = new Menu();
    MenuItem removeItem = new MenuItem("Remove from Variable Watches", false);
    m.append(removeItem);
    removeItem.setSensitive(true);
    removeItem.addListener(new MenuItemListener()
    {
      public void menuItemEvent (MenuItemEvent arg0)
      {
        handleClick();
      }
    });
    
    m.showAll();
    m.popup();
  }
   
  /**
   * Finds the selected Variable and tells the View parent
   * to remove it, which will then update this TreeView.
   */
  private void handleClick()
  {
    TreePath[] paths = this.getSelection().getSelectedRows();
    
    Variable selected = (Variable) model.getValue(model.getIter(paths[0]),
                             (DataColumnObject) traceColumns[2]);
    ((SourceView)parent.getView()).removeVar(selected);
  }
  
  /**
   * Checks for right-clicks.
   */
  private class VariableWatchListener
      implements MouseListener
  {

    public boolean mouseEvent (MouseEvent event)
    {
      if (! event.isOfType(MouseEvent.Type.BUTTON_PRESS))
        return false;
      
      if (event.getButtonPressed() == MouseEvent.BUTTON3)
        {
          clickedOnVariable(event);
        }

      return false;
    }
  }

}
