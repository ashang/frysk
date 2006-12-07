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
//import org.gnu.gtk.ListStore;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
//import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

//import frysk.dom.DOMFunction;
import frysk.dom.DOMLine;
//import frysk.dom.DOMSource;
import frysk.proc.MachineType;
import frysk.proc.Task;
import frysk.rt.StackFrame;

public class CurrentStackView
    extends TreeView
    implements TreeSelectionListener
{

  public interface StackViewListener
  {
    void currentStackChanged (StackFrame newFrame);
  }

  private DataColumn[] stackColumns = new DataColumn[] { new DataColumnString(),
                                                        new DataColumnObject() };

  private static StackFrame currentFrame;

  private Vector observers;
  
  private StackFrame head = null;
  
  private TreeStore treeModel = new TreeStore(stackColumns);

  public CurrentStackView (StackFrame[] frames)
  {
    super();
    this.setName("currentStackView");
    this.getAccessible().setName("currentStackView_showsCurrentStack");
    this.getAccessible().setDescription(
                                        "Displays the current stack frame as well as the current stack");

    this.setHeadersVisible(false);

    this.observers = new Vector();

    buildTree(frames);
    
    this.setModel(treeModel);

    TreeViewColumn column = new TreeViewColumn();
    CellRenderer renderer = new CellRendererText();
    column.packStart(renderer, true);
    column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                               stackColumns[0]);
    this.appendColumn(column);

    this.getSelection().setMode(SelectionMode.SINGLE);

    this.getSelection().addListener(this);
  }

  public void resetView (StackFrame[] frames)
  {
    treeModel.clear();
    buildTree(frames);
  }
  
  private void buildTree (StackFrame[] frames)
  {
    TreeIter iter = null;
    TreeIter parent = null;
    int nullCount = 0;
    
    for (int j = frames.length - 1; j >= 0; j--)
      {
        StackFrame frame = frames[j];

        if (frame == null)
          {
            nullCount++;
            continue;
          }
        
        parent = null;
        iter = null;
        
        boolean hasInlinedCode = false;

        String row = "";

        if (MachineType.getMachineType() == MachineType.PPC
            || MachineType.getMachineType() == MachineType.PPC64)
          {
            iter = treeModel.appendRow(null);
            row = "Unknown file : Unknown function";
            treeModel.setValue(iter, (DataColumnString) stackColumns[0], row);
            treeModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);
          }

        else
          {
            int level = 0;
            parent = treeModel.appendRow(null);
            Task task = frame.getTask();

            treeModel.setValue(parent, (DataColumnString) stackColumns[0],
                               "tid: " + task.getTid());
            treeModel.setValue(parent, (DataColumnObject) stackColumns[1], frame);
            
            if (task.equals(task.getProc().getMainTask()))
              {
                currentFrame = frame;
                head = frame;
              }

            while (frame != null)
              {
                hasInlinedCode = false;

                // Check for inlined code
                if (frame.getData() != null)
                  {
                    DOMLine line = frame.getData().getLine(frame.getLineNumber());
                    if (line != null && line.hasInlinedCode())
                      {
                        hasInlinedCode = true;
                      }
                  }

                iter = treeModel.appendRow(parent);

                if (frame.getLineNumber() != 0)
                  row = "# " + (++level) + " " + frame.toPrint();
                else
                  row = "# " + (++level) + " " + frame.toPrint(); 

                if (hasInlinedCode)
                  row += " (i)";

                treeModel.setValue(iter, (DataColumnString) stackColumns[0], row);
                treeModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);

                frame = frame.getOuter();
                row = "";
              }
          }
      }
    
    if (nullCount == frames.length)
      {
        iter = treeModel.appendRow(null);
        treeModel.setValue(iter, (DataColumnString) stackColumns[0], "Broken stack trace");
        treeModel.setValue(iter, (DataColumnString) stackColumns[1], null);
      }
  }
  
  /**
   * @return The currently selected stack frame
   */
  public static StackFrame getCurrentFrame ()
  {
    return currentFrame;
  }

  public void addListener (StackViewListener listener)
  {
    this.observers.add(listener);
  }

  private void notifyObservers (StackFrame newStack)
  {
    Iterator iter = this.observers.iterator();

    while (iter.hasNext())
      {
        ((StackViewListener) iter.next()).currentStackChanged(newStack);
      }
  }
  
  public StackFrame getFirstFrameSelection()
  {
    return this.head;
  }

  public void selectionChangedEvent (TreeSelectionEvent arg0)
  {
    
    TreePath[] paths = this.getSelection().getSelectedRows();
    if (paths.length == 0)
      return;

     StackFrame selected = (StackFrame) treeModel.getValue(
                                                      treeModel.getIter(paths[0]),
                                                      (DataColumnObject) stackColumns[1]);

    this.notifyObservers(selected);
    currentFrame = selected;
  }

}
