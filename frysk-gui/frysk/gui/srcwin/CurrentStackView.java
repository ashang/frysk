// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

import frysk.debuginfo.PrintDebugInfoStackOptions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeRowReference;
import org.gnu.gtk.TreeSelection;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.debuginfo.DebugInfoFrame;
import frysk.dom.DOMLine;
import frysk.proc.Task;
import frysk.scopes.SourceLocation;

public class CurrentStackView
	extends TreeView
	implements TreeSelectionListener
{

  public interface StackViewListener
  {
	void currentStackChanged (DebugInfoFrame newFrame, int current);
  }

  private DataColumn[] stackColumns = new DataColumn[] {
														new DataColumnString(),
														new DataColumnObject() };

    private static final PrintDebugInfoStackOptions STACK_OPTIONS;
    static {
	STACK_OPTIONS = new PrintDebugInfoStackOptions();
	STACK_OPTIONS.setPrintParameters(true);
    }


  private static DebugInfoFrame currentFrame;

  private LinkedList observers;

  private DebugInfoFrame head = null;

  private TreeStore treeModel = new TreeStore(stackColumns);

  private Object[] stackArray;

  public CurrentStackView (DebugInfoFrame[][] frames)
  {
	super();
	this.setName("currentStackView");
	this.getAccessible().setName("currentStackView_showsCurrentStack");
	this.getAccessible().setDescription(
										"Displays the current stack frame as well as the current stack");

	this.setHeadersVisible(false);

	this.observers = new LinkedList();
	this.stackArray = new Object[frames.length];

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

  public void refreshProc (DebugInfoFrame[] frames, int current) {
    TreeIter iter = null;
    TreePath path = ((TreeRowReference) this.stackArray[current]).getPath();

    path.down();
    TreeIter taskIter = treeModel.getIter(path);

    Task task = frames[0].getTask();

    for (int j = frames.length - 1; j >= 0; j--)
      {
	DebugInfoFrame frame = frames[j];
	task = frames[j].getTask();
	boolean hasInlinedCode = false;
	String row = "";
	int level = 0;

	if (taskIter == null || ! treeModel.isIterValid(taskIter))
	  {
	    taskIter = treeModel.appendRow(treeModel.getIter(((TreeRowReference) this.stackArray[current]).getPath()));
	  }

	treeModel.setValue(taskIter, (DataColumnString) stackColumns[0],
			   "thread ID: " + task.getTid());
	treeModel.setValue(taskIter, (DataColumnObject) stackColumns[1], null);
	path.down();
	iter = taskIter.getFirstChild();

	if (task.getTid() == task.getProc().getMainTask().getTid())
	  {
	    DebugInfoFrame out = frame.getOuterDebugInfoFrame();
	    if (out != null)
	      {
		currentFrame = out;
		this.head = out;
	      }
	    else
	      {
		currentFrame = frame;
		head = frame;
	      }
	  }

	while (frame != null)
	  {
	    hasInlinedCode = false;

	    if (iter == null || ! treeModel.isIterValid(iter))
	      iter = treeModel.appendRow(taskIter);

	    // Check for inlined code
	    if (frame.getLine() != SourceLocation.UNKNOWN
		&& frame.getLineXXX().getDOMSource() != null)
	      {
		DOMLine line = frame.getLineXXX().getDOMSource().getLine(
									  frame.getLine().getLine());
		if (line != null && line.hasInlinedCode())
		  {
		    hasInlinedCode = true;
		  }
	      }

	    StringWriter stringWriter = new StringWriter();
	    stringWriter.write("# " + (++level) + " ");
	    frame.toPrint(new PrintWriter(stringWriter), STACK_OPTIONS);
	    row = stringWriter.toString();
	    
	    if (hasInlinedCode)
	      row += " (i)";

	    treeModel.setValue(iter, (DataColumnString) stackColumns[0], row);
	    treeModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);

	    frame = frame.getOuterDebugInfoFrame();
	    iter = iter.getNextIter();
	    row = "";
	  }

	taskIter = taskIter.getNextIter();

	while (iter != null && treeModel.isIterValid(iter))
	  {
	    TreeIter del = iter;
	    iter = iter.getNextIter();
	    treeModel.removeRow(del);
	  }
      }
  }

  private void buildTree (DebugInfoFrame[][] frames)
  {
    TreeIter procIter = null;
    TreeIter taskIter = null;

    for (int i = 0; i < frames.length; i++)
      {
	procIter = treeModel.appendRow(null);
	Task t = frames[i][0].getTask();

	this.stackArray[i] = new TreeRowReference(treeModel, procIter.getPath());

	treeModel.setValue(procIter, (DataColumnString) stackColumns[0],
			   "process: " + t.getProc().getCommand() + " PID: "
			       + t.getProc().getPid());

	treeModel.setValue(procIter, (DataColumnObject) stackColumns[1],
			   new Integer(i));

	Task task = frames[i][0].getTask();

	for (int j = frames[i].length - 1; j >= 0; j--)
	  {
	    DebugInfoFrame frame = frames[i][j];
	    task = frames[i][j].getTask();

	    taskIter = treeModel.appendRow(procIter);

	    treeModel.setValue(taskIter, (DataColumnString) stackColumns[0],
			       "thread ID: " + task.getTid());
	    treeModel.setValue(taskIter, (DataColumnObject) stackColumns[1],
			       null);

	    if (i == 0
		&& task.getTid() == task.getProc().getMainTask().getTid())
	      {
		DebugInfoFrame in = frame.getInnerDebugInfoFrame();
		if (in != null)
		  {
		    currentFrame = in;
		    this.head = in;
		  }
		else
		  {
		    currentFrame = frame;
		    head = frame;
		  }
	      }
	    appendRows(frame, taskIter);
	  }
      }
  }

  public void appendRows (DebugInfoFrame frame, TreeIter taskIter)
  {
	boolean hasInlinedCode;
	TreeIter iter;
	String row = "";
	int level = 0;

	while (frame != null)
	  {
		hasInlinedCode = false;

		// Check for inlined code
		if (frame.getLine() != SourceLocation.UNKNOWN
		    && frame.getLineXXX().getDOMSource() != null)
		  {
			DOMLine line = frame.getLineXXX().getDOMSource().getLine(frame.getLine().getLine());
			if (line != null && line.hasInlinedCode())
			  {
				hasInlinedCode = true;
			  }
		  }

		iter = treeModel.appendRow(taskIter);

		StringWriter stringWriter = new StringWriter();
		stringWriter.write(row = "# " + (++level) + " ");
		frame.toPrint(new PrintWriter(stringWriter), STACK_OPTIONS);
		row = stringWriter.toString();
		
		if (hasInlinedCode)
		  row += " (i)";

		treeModel.setValue(iter, (DataColumnString) stackColumns[0], row);
		treeModel.setValue(iter, (DataColumnObject) stackColumns[1], frame);

		frame = frame.getOuterDebugInfoFrame();
		row = "";
	  }
  }

  public void addProc (DebugInfoFrame[] frames, int current)
  {
	int len = this.stackArray.length;
	Object[] tempArray = new Object[len + 1];
	System.arraycopy(this.stackArray, 0, tempArray, 0, len);
	this.stackArray = tempArray;

	TreeIter procIter = this.treeModel.appendRow(null);
	TreeIter taskIter;
	Task t = frames[0].getTask();

	this.stackArray[current] = new TreeRowReference(treeModel,
													procIter.getPath());

	treeModel.setValue(procIter, (DataColumnString) stackColumns[0],
					   "process: " + t.getProc().getCommand() + " PID: "
						   + t.getProc().getPid());

	treeModel.setValue(procIter, (DataColumnObject) stackColumns[1],
					   new Integer(current));

	Task task = frames[0].getTask();

	for (int j = frames.length - 1; j >= 0; j--)
	  {
		DebugInfoFrame frame = frames[j];

		taskIter = treeModel.appendRow(procIter);

		treeModel.setValue(taskIter, (DataColumnString) stackColumns[0],
						   "thread ID: " + task.getTid());
		treeModel.setValue(taskIter, (DataColumnObject) stackColumns[1], null);

		appendRows(frame, taskIter);
	  }
  }
  
  public void removeProc (int current)
  {
	TreePath path = ((TreeRowReference) this.stackArray[current]).getPath();
	this.treeModel.removeRow(this.treeModel.getIter(path));
	
	int len = this.stackArray.length - 1;
	Object[] tempArray = new Object[len];
	
	int j = 0;
	for (int i = 0; i < this.stackArray.length; i++)
	  {
		if (i != current)
		  tempArray[j++] = this.stackArray[i];
	  }
	
	int loc = 0;
	TreeIter iter = this.treeModel.getFirstIter();
	
	while (iter != null && this.treeModel.isIterValid(iter))
	  {
		treeModel.setValue(iter, (DataColumnObject) stackColumns[1],
				   new Integer(loc));
		++loc;
		iter = iter.getNextIter();
	  }
	
	this.stackArray = tempArray;
  }

  public void clear ()
  {
    this.treeModel.clear();
  }
  
  /**
   * @return The currently selected stack frame
   */
  public static DebugInfoFrame getCurrentFrame ()
  {
	return currentFrame;
  }

  public void addListener (StackViewListener listener)
  {
	this.observers.add(listener);
  }

  private void notifyObservers (DebugInfoFrame newStack, int current)
  {
	Iterator iter = this.observers.iterator();

	while (iter.hasNext())
	  {
		((StackViewListener) iter.next()).currentStackChanged(newStack, current);
	  }
  }

  public DebugInfoFrame getFirstFrameSelection ()
  {
	return this.head;
  }

  /**
         * Scan the available stack frames and find the frame which matches the
         * parameter. Then highlight the row containing that frame.
         * 
         * @param frame The StackFrame whose row to highlight.
         */
  public void selectRow (DebugInfoFrame frame)
  {
    TreeSelection selection = this.getSelection();
    TreeIter first = this.treeModel.getFirstIter();
    first = first.getFirstChild();

    while (first != null && this.treeModel.isIterValid(first))
      {
	TreeIter iter = first.getFirstChild();
	while (iter != null && this.treeModel.isIterValid(iter))
	  {
	    DebugInfoFrame rowFrame = (DebugInfoFrame) this.treeModel.getValue(
								       iter,
								       (DataColumnObject) stackColumns[1]);

	    if (frame.getFrameIdentifier().equals(rowFrame.getFrameIdentifier()))
	      {
		selection.select(iter);
		return;
	      }
	    iter = iter.getNextIter();
	  }
	first = first.getNextIter();
      }
  }

  public void selectionChangedEvent (TreeSelectionEvent arg0)
  {

	TreePath[] paths = this.getSelection().getSelectedRows();
	if (paths.length == 0)
	  return;

	DebugInfoFrame selected = null;
	Integer current = null;

	Object o = treeModel.getValue(treeModel.getIter(paths[0]),
								  (DataColumnObject) stackColumns[1]);

	if (o != null)
	  {

		if (paths[0].up())
		  {
			if (paths[0].up())
			  {
				selected = (DebugInfoFrame) o;

				current = (Integer) treeModel.getValue(
													   treeModel.getIter(paths[0]),
													   (DataColumnObject) stackColumns[1]);
			  }
			else
			  {
				return;
				// current = (Integer) treeModel.getValue(
				// treeModel.getIter(paths[0]),
				// (DataColumnObject) stackColumns[1]);
				// paths[0].down();
				// paths[0].down();
				// selected = (StackFrame) treeModel.getValue(
				// treeModel.getIter(paths[0]),
				// (DataColumnObject) stackColumns[1]);
			  }

		  }
		else
		  {
			return;
			// paths[0].down();
			// paths[0].down();
			// selected = (StackFrame) treeModel.getValue(
			// treeModel.getIter(paths[0]),
			// (DataColumnObject) stackColumns[1]);
			// current = (Integer) o;
		  }

		this.notifyObservers(selected, current.intValue());
		currentFrame = selected;
		return;
	  }
	else
	  return;
  }

}
