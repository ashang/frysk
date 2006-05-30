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

package frysk.gui.test;

import java.util.Observable;
import java.util.Observer;

import junit.framework.TestCase;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TasksObserver;
import frysk.proc.ProcObserver.Tasks;
import frysk.sys.Fork;
import frysk.sys.Sig;
import frysk.sys.Signal;

public class GuiTestLib extends TestCase{
	
	public GuiTestLib(){
	}

	public void setUp(){
		Thread backendStarter = new Thread(new Runnable() {
			public void run() {
				Manager.eventLoop.run();
			}
		});
		backendStarter.start();
	}
	
	public void testTestProc() {
		TestProc testProc = new TestProc();
		Proc proc = testProc.getProc();
		assertNotNull("Value of created testProc", proc);
	}
	
	public void testTestProcFork(){
		TestProc testProc = new TestProc();
		testProc.fork();
		Proc proc = testProc.getProc();
		// if fork returns then confermation signale must have been received
		assertNotNull("Retrieved process",proc);
		assertTrue("Call to fork returned", true);
	}
	
	static protected class TestProc implements Observer, TaskObserver.Signaled  {
		
		static final Sig childAck = Sig.USR1;
		static final Sig parentAck = Sig.USR2;
		static final Sig addCloneSig = Sig.USR1;
		static final Sig delCloneSig = Sig.USR2;
		static final Sig addForkSig = Sig.HUP;
		static final Sig delForkSig = Sig.INT;
		static final Sig zombieForkSig = Sig.URG;
		static final Sig execSig = Sig.PWR;
		
		int pid;
		private Proc proc;
		
		public TestProc(){
			Manager.host.observableProcAddedXXX.addObserver(this);
			pid = Fork.exec(new String[]{".././frysk-core/prog/kill/child", "0"});
			this.getProc();
		}
		
		public synchronized Proc getProc(){
			if(proc == null){
				Manager.host.requestRefreshXXX(true);
			    try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			this.listenForSignals();
			return proc;
		}

		public synchronized void update(Observable arg0, Object arg1) {
			Proc newProc = (Proc)arg1;
			if(newProc.getPid() == this.pid){
				this.proc = newProc;
				notifyAll();
			}
		}
		
//		protected void finalize() throws Throwable {
//			System.out.println("GuiTestLib.finaliz()");
//			Signal.tkill (this.pid, Sig.ALRM);
//			super.finalize();
//		}
		
		
		public synchronized void fork(){
			Signal.tkill (this.pid, addForkSig);
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		private void listenForSignals(){
			
		    new TasksObserver (proc, new Tasks()
			{
			    public void deletedFrom(Object observable){}
			    public void addFailed(Object observable, Throwable w){}
			    public void addedTo(Object observable){}

			    public void existingTask(Task task) {
				task.requestDeleteSignaledObserver(TestProc.this);
			    }
			
			    public void taskRemoved(Task task) {
				task.requestAddSignaledObserver(TestProc.this);
			    }
			
			    public void taskAdded(Task task) {
				task.requestAddSignaledObserver(TestProc.this);
			    }
			});
		}
		
		public void tearDown(){
			//System.out.println("GuiTestLib.tearDown()");
		}

		public Action updateSignaled(Task task, int signal) {
// 			System.out.println("TestProc.updateSignaled()");
// 			System.out.println("\n===========================================");
			if(signal == Sig.USR2_){
				notifyAll();
			}
			return Action.CONTINUE;
		}

		public void addedTo(Object observable) {}
		public void addFailed(Object observable, Throwable w) {}
		public void deletedFrom(Object observable){}
		
	
	}
	
}
