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
import java.util.LinkedList;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.Saveable;
import frysk.proc.Proc;
import frysk.proc.Task;

public class StepDialog
		extends Dialog
		implements Saveable
{

	private LibGlade glade;

	private DataColumn[] cols = { new DataColumnBoolean(),
					new DataColumnString() };

	private TreeView formatList;

	private Proc proc;

	private LinkedList tasks;

	private LinkedList stepTasks;

	private SourceWindow sw;

	private ListStore model;

	private int state;

	/**
	 * On initialization, set the glade file, the titles for CheckBox options,
	 * the columns in this window, and add all the Listeners.
	 * 
	 * @param glade   The glade file containing the needed widgets.
	 */
	public StepDialog (LibGlade glade, SourceWindow sw)
	{
		super(glade.getWidget("stepDialog").getHandle());
		this.glade = glade;
		this.proc = sw.getSwProc();
		this.sw = sw;
		this.tasks = this.proc.getTasks();
		this.stepTasks = new LinkedList();

		this.setIcon(IconManager.windowIcon);

		this.formatList = (TreeView) this.glade.getWidget("stepView");
		formatList.setHeadersVisible(false);

		this.model = new ListStore(cols);

		Iterator i = this.tasks.iterator();
		while (i.hasNext())
			{
				Task t = (Task) i.next();
				TreeIter iter = model.appendRow();
				model.setValue(iter,
						(DataColumnBoolean) cols[0],
						false);
				model.setValue(iter,
						(DataColumnString) cols[1],
						"Thread ID: " + t.getTid());
			}

		TreeViewColumn col = new TreeViewColumn();
		CellRenderer renderer = new CellRendererToggle();
		col.packStart(renderer, false);
		col.addAttributeMapping(renderer,
					CellRendererToggle.Attribute.ACTIVE,
					cols[0]);
		formatList.appendColumn(col);

		((CellRendererToggle) renderer).addListener(new CellRendererToggleListener()
		{
			public void cellRendererToggleEvent (
								CellRendererToggleEvent arg0)
			{
				TreePath path = new TreePath(arg0.getPath());

				TreeIter iter = model.getIter(path);

				boolean prev = model.getValue(
								iter,
								(DataColumnBoolean) cols[0]);
				model.setValue(iter,
						(DataColumnBoolean) cols[0],
						! prev);
			}
		});

		col = new TreeViewColumn();
		renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer,
					CellRendererText.Attribute.TEXT,
					cols[1]);
		formatList.appendColumn(col);

		formatList.setModel(model);

		((Button) this.glade.getWidget("stepDialogOkButton")).addListener(new ButtonListener()
		{
			public void buttonEvent (ButtonEvent arg0)
			{
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
					{
						grabTasks();
						StepDialog.this.hideAll();
					}
			}
		});

		((Button) this.glade.getWidget("stepDialogStepButton")).addListener(new ButtonListener()
		{
			public void buttonEvent (ButtonEvent arg0)
			{
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
					{
						grabTasks();
						switch (StepDialog.this.state)
							{
							case 0:
								StepDialog.this.sw.doStep(stepTasks);
								break;
							case 1:
								StepDialog.this.sw.doNext(stepTasks);
								break;
							case 2:
								StepDialog.this.sw.doFinish(stepTasks);
								break;
							case 3:
								StepDialog.this.sw.doStepAsm(stepTasks);
								break;
							case 4:
								StepDialog.this.sw.doAsmNext(stepTasks);
								break;
							default:
								break;
							}
					}
			}
		});

		((Button) this.glade.getWidget("stepDialogCancelButton")).addListener(new ButtonListener()
		{
			public void buttonEvent (ButtonEvent arg0)
			{
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
					{
						StepDialog.this.hideAll();
					}
			}
		});

		this.addListener(new LifeCycleListener()
		{
			public boolean lifeCycleQuery (LifeCycleEvent arg0)
			{
				if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
					{
						StepDialog.this.hideAll();
						return true;
					}
				return false;
			}

			public void lifeCycleEvent (LifeCycleEvent arg0)
			{
			}
		});
	}

	public void setType (int type)
	{
		if (this.sw.getSwProc().getPid() != this.proc.getPid())
			{
				this.model.clear();
				this.proc = this.sw.getSwProc();
				this.tasks = this.proc.getTasks();
				Iterator i = this.tasks.iterator();
				while (i.hasNext())
					{
						Task t = (Task) i.next();
						TreeIter iter = model.appendRow();
						model.setValue(
								iter,
								(DataColumnBoolean) cols[0],
								false);
						model.setValue(
								iter,
								(DataColumnString) cols[1],
								"Thread ID: "
										+ t.getTid());
					}
			}

		this.state = type;
		StringBuffer buffer = new StringBuffer();
		buffer.append("Frysk / Threaded ");
		switch (type)
			{
			case 0:
				buffer.append("step into");
				break;
			case 1:
				buffer.append("step over");
				break;
			case 2:
				buffer.append("step out");
				break;
			case 3:
				buffer.append("step instruction");
				break;
			case 4:
				buffer.append("step next instruction");
				break;
			default:
				break;
			}

		this.setTitle(buffer.toString());
	}

	public void grabTasks ()
	{
		int length = this.tasks.size();
		ListStore model = (ListStore) this.formatList.getModel();
		this.stepTasks.clear();

		TreeIter iter = model.getFirstIter();
		Iterator taskIter = this.tasks.iterator();

		for (int i = 0; i < length; i++)
			{
				Task task = (Task) taskIter.next();
				boolean val = model.getValue(
								iter,
								(DataColumnBoolean) cols[0]);
				if (val)
					{
						this.stepTasks.add(task);
					}
				iter = iter.getNextIter();
			}
	}

	public LinkedList getStepTasks ()
	{
		return this.stepTasks;
	}

	public void desensitize ()
	{
		this.setSensitive(false);
	}

	public void resensitize ()
	{
		this.setSensitive(true);
	}

	/**
	 * Save the preferences contained in this Preferences node, apply to all 
	 * options represented by the TreeIters in this dialog.
	 * 
	 * @param prefs   The Preferences node to save.
	 */
	public void save (Preferences prefs)
	{

	}

	/**
	 * Load the options contained in this Preferences node, apply the changes
	 * to each of the options represented by the TreeIters in this dialog.
	 * 
	 * @param prefs   The Preferences node to load from.
	 */
	public void load (Preferences prefs)
	{
	}
}
