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
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.dom.DOMFunction;
import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.proc.MachineType;
import frysk.rt.StackFrame;

public class CurrentStackView
    extends TreeView
    implements TreeSelectionListener
{

  public interface StackViewListener
  {
    void currentStackChanged (StackFrame newFrame);
  }

  private DataColumn[] stackColumns;

  private StackFrame currentFrame;

  private Vector observers;

  public CurrentStackView (StackFrame frame)
  {
    super();

    this.setName("currentStackView");
    this.getAccessible().setName("currentStackView_showsCurrentStack");
    this.getAccessible().setDescription(
                                        "Displays the current stack frame as well as the current stack");

    this.setHeadersVisible(false);

    this.observers = new Vector();

    stackColumns = new DataColumn[] { new DataColumnString(),
                                     new DataColumnObject() };
    ListStore listModel = new ListStore(stackColumns);

    TreeIter iter = null;
    TreeIter last = null;

    boolean hasInlinedCode = false;
    
    DOMSource source = frame.getData();
    DOMFunction func = frame.getFunction();
    String row = "";
    
    if (source == null || func == null || MachineType.getMachineType() != MachineType.IA32)
      {
        iter = listModel.appendRow();
        row = "Unknown file : Unknown function";
        listModel.setValue(iter, (DataColumnString) stackColumns[0], row);
        listModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);
      }

    else
      {
        int level = 0;
        while (frame != null)
          {
            hasInlinedCode = false;
            
            // Go through each segment of the current line, but once we've found
            // one stop checking
            // StackFrame curr =
            // while (current != null && ! hasInlinedCode)
            // {
            // // Go through each line of the segment
            // for (int i = current.getStartLine(); i <
            // current.getEndLine(); i++)
            // {
            // Check for inlined code
            
            DOMLine line = frame.getData().getLine(frame.getLineNumber());
            if (line != null && line.hasInlinedCode())
              {
                hasInlinedCode = true;
              }

            // current = current.getNextSection();

            iter = listModel.appendRow();

            if (frame.getMethodName() != "")
              {
                row = "#" + (++level) + " 0x"
                      + Long.toHexString(frame.getAddress()) + " in "
                      + frame.getMethodName() + " (): line #"
                      + frame.getLineNumber();
              }
            else
              {
                row = "#" + (++level) + " 0x"
                      + Long.toHexString(frame.getAddress()) + " in (null) ()";
              }

            if (hasInlinedCode)
              row += " (i)";

            listModel.setValue(iter, (DataColumnString) stackColumns[0], row);
            listModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);

            frame = frame.getOuter();
            row = "";
          }
      }
      

    //CurrentLineSection current = topLevel.getCurrentLine();
    //hasInlinedCode = false;
    
    /* Current frame is the top one */
    
    
    this.currentFrame = frame;
    
    


    last = iter;
    /* For now - its always null anyway */
    //currentLevel = topLevel; // remove

    this.setModel(listModel);

    TreeViewColumn column = new TreeViewColumn();
    CellRenderer renderer = new CellRendererText();
    column.packStart(renderer, true);
    column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
                               stackColumns[0]);
    this.appendColumn(column);

    this.getSelection().setMode(SelectionMode.SINGLE);
    this.getSelection().select(last);

    this.getSelection().addListener(this);
  }

  /**
   * @return The currently selected stack frame
   */
  public StackFrame getCurrentFrame ()
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

  public void selectionChangedEvent (TreeSelectionEvent arg0)
  {
    TreeModel model = this.getModel();

    TreePath[] paths = this.getSelection().getSelectedRows();
    if (paths.length == 0)
      return;

     StackFrame selected = (StackFrame) model.getValue(
                                                      model.getIter(paths[0]),
                                                      (DataColumnObject) stackColumns[1]);

    this.notifyObservers(selected);
  }

}
