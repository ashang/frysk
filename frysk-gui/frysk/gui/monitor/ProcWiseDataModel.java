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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;

import frysk.event.TimerEvent;
import frysk.gui.Gui;
import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * @author swagiaal, pmuldoon
 *
 * A data model that groups PID's by executable
 * name. Also has a selected component that allows
 * the druid to define whether a process is selected
 */
public class ProcWiseDataModel {

	private TreeStore treeStore;

	private DataColumnString nameDC;
	private DataColumnObject objectDC;
	private DataColumnBoolean selectedDC;
	
	private ProcCreatedObserver procCreatedObserver;
	private ProcDestroyedObserver procDestroyedObserver;

	private TimerEvent refreshTimer;
	
	private Hashtable iterHash;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	public ProcWiseDataModel(){
		this.iterHash = new Hashtable();
		
		this.nameDC = new DataColumnString();
		this.objectDC = new DataColumnObject();
		this.selectedDC = new DataColumnBoolean();
		
		this.treeStore = new TreeStore(new DataColumn[] {this.nameDC, this.objectDC, this.selectedDC});

		this.refreshTimer = new TimerEvent(0, 5000){
			public void execute() {
				Manager.host.requestRefreshXXX (true);
			}
		};
		
		Manager.eventLoop.add (this.refreshTimer);
		
		this.procCreatedObserver = new ProcCreatedObserver();
		this.procDestroyedObserver = new ProcDestroyedObserver();
		
		Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
		Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);
	}

	private void setRow(TreeIter row, String name, ProcData data, boolean selected){
		treeStore.setValue(row, nameDC, name);
		treeStore.setValue(row, objectDC, data);
		treeStore.setValue(row, selectedDC, selected);
	}

	public TreePath searchName(String name)
	{
		TreeIter iter = treeStore.getFirstIter();		
		while (iter != null)
		{
			String split[] = treeStore.getValue(iter,getNameDC()).split("\t");
			
			split[0] = split[0].trim();
			if (split.length > 0)
				if (split[0].equalsIgnoreCase(name))
					return iter.getPath();
				
			iter = iter.getNextIter();
		}
		return null;	
	}
	
	public DataColumnString getNameDC() {
		return nameDC;
	}

	public DataColumnObject getPathDC() {
		return objectDC;
	}
	
	public DataColumnBoolean getSelectedDC() {
		return selectedDC;
	}
	
	public void setSelected(TreeIter iter, boolean type, boolean setChildren)
	{
		if (treeStore.isIterValid(iter))
		{
			treeStore.setValue(iter,getSelectedDC(), type);
			if (setChildren)
			{
				int children = iter.getChildCount();
				for (int count=0; count<children; count++)
					if (treeStore.isIterValid(iter.getChild(count)))
						treeStore.setValue(iter.getChild(count),getSelectedDC(), type);
			}
		}
	}
	
	public ArrayList dumpSelectedProcesses()
	{
		
		ArrayList processData = new ArrayList();
		// TODO: Very unsafe (process might be deleted by observers
		// behind the scenes. Rewrite
		for(int i=0;true;i++)
		{
			TreeIter item = treeStore.getIter(new Integer(i).toString());
			if (item == null) break;
			
			if (treeStore.isIterValid(item))
			{
				// We only care about process groups, so top level run only.
				if (treeStore.getValue(item,selectedDC) == true)
					processData.add(treeStore.getValue(item,nameDC));
			}
		}
		
		return  processData;
	}
	
	class ProcCreatedObserver implements Observer {
		public void update(Observable o, Object obj) {
			final Proc proc = (Proc) obj;
			org.gnu.glib.CustomEvents.addEvent(new Runnable() {
				public void run() {
					// get an iterator pointing to the parent
					try {
						
	
						TreeIter parent = (TreeIter) iterHash.get(proc
								.getCommand());
						if (parent != null)
							if (!treeStore.isIterValid(parent))
								throw new RuntimeException(
										"TreeIter has parent, but isIterValid returns false."); //$NON-NLS-1$

						if (parent == null) {
							parent = treeStore.appendRow(null);
							if (parent == null)
								throw new RuntimeException(
										"parent = treeStore.appendRow(null) returns a null."); //$NON-NLS-1$
							if (!treeStore.isIterValid(parent))
								throw new RuntimeException(
										"parent = treeStore.appendRow(null) fails isIterValid test."); //$NON-NLS-1$
							iterHash.put(proc.getCommand(), parent);
							setRow(parent, proc.getCommand() + "\t"
									+ proc.getPid(), new ProcData(proc), false);
						} else {
							TreeIter iter = treeStore.appendRow(parent);
							if (iter == null)
								throw new RuntimeException(
										"iter = treeStore.appendRow(null) returns a null."); //$NON-NLS-1$
							if (!treeStore.isIterValid(iter))
								throw new RuntimeException(
										"iter = treeStore.appendRow(parent) fails isIterValid test."); //$NON-NLS-1$

							if (((ProcData) treeStore
									.getValue(parent, objectDC)).getProc() != null) {
								Proc oldProc = ((ProcData) treeStore.getValue(
										parent, objectDC)).getProc();
								setRow(parent, proc.getCommand(), new ProcData(
										null), false);
								setRow(iter, "" + oldProc.getPid(),
										new ProcData(oldProc), false);
								iter = treeStore.appendRow(parent);
								if (iter == null)
									throw new RuntimeException(
											"iter = treeStore.appendRow(null) returns a null."); //$NON-NLS-1$
								if (!treeStore.isIterValid(iter))
									throw new RuntimeException(
											"iter = treeStore.appendRow(parent) fails isIterValid test."); //$NON-NLS-1$
							}

							// setRow(iter, "", ""+proc.getPid(),
							// proc.getExe());
							setRow(iter, "" + proc.getPid(),
									new ProcData(proc), false);
						}
					} catch (Exception e) {
						errorLog.log(Level.WARNING,
						"ProcWiseDataModel.ProcCreatedObserver reported thiis error",e.getMessage());
					}
				}

			});
		}
	}
    

	class ProcDestroyedObserver implements Observer {
		public void update(Observable o, Object obj) {
			final Proc proc = (Proc) obj;

			org.gnu.glib.CustomEvents.addEvent(new Runnable() {
				public void run() {
					TreeIter parent = (TreeIter) iterHash
							.get(proc.getCommand());
					try {

						if (parent == null)
							throw new NullPointerException("proc "	+ proc + 
								"Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
						else if (!treeStore.isIterValid(parent))
							throw new RuntimeException(
									"TreeIter has parent, but isIterValid returns false.");

						int n = parent.getChildCount();

						if (n == 0) {
							treeStore.removeRow(parent);
							iterHash.remove(proc.getCommand());
							return;
						}

						if (n > 1) {
							for (int i = 0; i < n; i++) {
								TreeIter iter = parent.getChild(i);
								if (!treeStore.isIterValid(iter))
									throw new RuntimeException(
											"TreeIter child of parent "
													+ proc.getCommand()
													+ " isIterValid reports false");

								if (((ProcData) treeStore.getValue(iter,
										objectDC)).getProc().getPid() == proc
										.getPid()) {
									treeStore.removeRow(iter);
									break;
								}
							}
						}

						n = parent.getChildCount();
						if (n == 1) {
							TreeIter iter = parent.getChild(0);
							if (!treeStore.isIterValid(iter))
								throw new RuntimeException(
										"TreeIter child of parent "
												+ proc.getCommand()
												+ " isIterValid reports false");

							Proc oldProc = ((ProcData) treeStore.getValue(iter,
									objectDC)).getProc();
							setRow(parent, oldProc.getCommand() + "\t"
									+ oldProc.getPid(), new ProcData(oldProc),
									treeStore.getValue(iter, selectedDC));

							treeStore.removeRow(iter);
						}

					} catch (Exception e) {
						errorLog.log(Level.WARNING,
										"ProcWiseDataModel.ProcDestroyedObserver reported this error", e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			});
		}
	}


	public TreeStore getModel() {
		return this.treeStore;
	}
	
}
