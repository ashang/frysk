// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.ftrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import frysk.proc.Action;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/*import frysk.stack.Frame;
import frysk.stack.StackFactory;*/

import frysk.sys.Sig;

public class Ltrace
{
  protected static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

  // Set of pids to trace initially.
  HashSet pidsToTrace = new HashSet();

  // True if we're tracing children as well.
  boolean traceChildren = false;

  // True if we're tracing signals as well.
  boolean traceSignals = false;

  // Task counter.
  int numTasks = 0;

  private List observers = new ArrayList();

  final SymbolFilter symbolFilter;

  /**
   *
   */
  public Ltrace (SymbolFilter symbolFilter)
  {
    this.symbolFilter = symbolFilter;
  }

  /**
   * Add new observer.
   */
  public void addObserver(LtraceObserver observer)
  {
    synchronized(observer) {
      observers.add(observer);
    }
  }

  /**
   * Remove given observer.
   */
  public boolean removeObserver(LtraceObserver observer)
  {
    synchronized(observer) {
      return observers.remove(observer);
    }
  }

  /**
   * Used for adding one or more traced pids.
   */
  public void addTracePid (ProcId id)
  {
    pidsToTrace.add(id);
  }

  /**
   * Request that the newly created children of traced processes
   * should be automatically attached to.
   */
  public void setTraceChildren ()
  {
    traceChildren = true;
  }

  /**
   * Request that signals should be traced as well.
   */
  public void setTraceSignals ()
  {
    traceSignals = true;
  }

  /**
   * Trace new process.
   */
  private void addProc(Proc proc)
  {
    new ProcTasksObserver(proc, new ProcObserver.ProcTasks() {
	public void existingTask (Task task)
	{
	  if (task.getTid() == task.getProc().getMainTask().getTid())
	    perProcInit(task.getProc());
	  addTask(task);
	}

	public void taskAdded (Task task)
	{
	  addTask(task);
	}

	public void taskRemoved (Task task)
	{
	  removeTask(task);
	}

	public void addFailed (Object arg0, Throwable w) {}
	public void addedTo (Object arg0) {}
	public void deletedFrom (Object arg0) {}
      });
  }

  /**
   * Do a per-process init.  Called only when process task(s) exist(s)
   * and are attached to.
   */
  private void perProcInit(Proc proc)
  {
  }

  private Map taskArchHandlers = new HashMap();

  /**
   * Trace new task of existing process.
   */
  private void addTask (Task task)
  {
    synchronized (this) {
      numTasks++;
      taskArchHandlers.put(task, ArchFactory.instance.getArch(task));

      for (Iterator it = observers.iterator(); it.hasNext(); )
	{
	  LtraceObserver o = (LtraceObserver)it.next();
	  o.taskAttached(task);
	}
    }

    task.requestAddAttachedObserver(ltraceTaskObserver);
    if (traceChildren)
      task.requestAddForkedObserver(ltraceTaskObserver);
    task.requestAddTerminatedObserver(ltraceTaskObserver);
    task.requestAddTerminatingObserver(ltraceTaskObserver);
    task.requestAddSyscallObserver(ltraceTaskObserver);
    // Code observers are added in perProcInit
  }

  /**
   * Notify that the task ended.
   */
  synchronized private void removeTask (Task task)
  {
    numTasks--;

    // note this method is already synchronized
    for (Iterator it = observers.iterator(); it.hasNext(); )
      {
	LtraceObserver o = (LtraceObserver)it.next();
	o.taskRemoved(task);
      }

    if (numTasks == 0)
      Manager.eventLoop.requestStop();
  }

  /**
   * Start tracing a set of PIDs defined on commandline.
   */
  public void trace ()
  {
    // This observer should only be used to pick up a proc if we are
    // tracing a process given a pid.  Otherwise use forkobserver.
    Manager.host.observableProcAddedXXX.addObserver(new Observer()
    {
      public void update (Observable observable, Object arg)
      {
	Proc proc = (Proc)arg;
	ProcId id = proc.getId();
	if (pidsToTrace.contains(id))
	  {
	    addProc(proc);
	    pidsToTrace.remove(id);
	    if (pidsToTrace.isEmpty())
	      Manager.host.observableProcAddedXXX.deleteObserver(this);
	  }
      }
    });

    // XXX: We iterate over pidsToTrace here, but remove from that
    // same set in observer installed before.  See if there's no
    // problem with that.
    for (Iterator it = pidsToTrace.iterator(); it.hasNext(); )
      Manager.host.requestFindProc(
	(ProcId)it.next(),
	new Host.FindProc() {
	  public void procFound (ProcId procId) {}
	  public void procNotFound (ProcId procId, Exception e)
	  {
	    System.err.println("No process with ID " + procId.intValue() + " found.");
	  }
	}
      );

    Manager.eventLoop.run();
  }

  /**
   * Start tracing a command given on commandline.
   */
  public void trace (String[] command)
  {
    if (!pidsToTrace.isEmpty())
      throw new AssertionError("Unexpected: tracing both pids and command.");

    Manager.host.requestCreateAttachedProc(command, new TaskObserver.Attached() {
	public Action updateAttached (Task task)
	{
	  addProc(task.getProc());
	  ltraceTaskObserver.updateAttached(task);
	  task.requestUnblock(this);
	  return Action.BLOCK;
	}
	public void addFailed (Object arg0, Throwable w) {}
	public void addedTo (Object arg0) {}
	public void deletedFrom (Object arg0) {}
      });
    Manager.eventLoop.run();
  }


  private class LtraceTaskObserver
    implements TaskObserver.Attached,
	       TaskObserver.Code,
	       TaskObserver.Forked,
	       TaskObserver.Syscall,
	       TaskObserver.Terminated,
	       TaskObserver.Terminating
  {
    // ------------------------
    // --- syscall observer ---
    // ------------------------

    /// Remembers which syscall is currently handled in which task.
    private HashMap syscallCache = new HashMap();

    public Action updateSyscallEnter (Task task)
    {
      if (syscallCache.containsKey(task))
	System.err.println("Warning: syscallCache contains "
			   + ((frysk.proc.Syscall)syscallCache.get(task)).getName()
			   + ".");

      frysk.proc.Syscall syscall = task.getSyscallEventInfo().getSyscall(task);

      /*
	Taken from glibc's sysdeps/unix/make-syscalls.sh:

	# Syscall Signature Key Letters for BP Thunks:
	#
	# a: unchecked address (e.g., 1st arg to mmap)
	# b: non-NULL buffer (e.g., 2nd arg to read; return value from mmap)
	# B: optionally-NULL buffer (e.g., 4th arg to getsockopt)
	# f: buffer of 2 ints (e.g., 4th arg to socketpair)
	# F: 3rd arg to fcntl
	# i: scalar (any signedness & size: int, long, long long, enum, whatever)
	# I: 3rd arg to ioctl
	# n: scalar buffer length (e.g., 3rd arg to read)
	# N: pointer to value/return scalar buffer length (e.g., 6th arg to recvfrom)
	# p: non-NULL pointer to typed object (e.g., any non-void* arg)
	# P: optionally-NULL pointer to typed object (e.g., 2nd argument to gettimeofday)
	# s: non-NULL string (e.g., 1st arg to open)
	# S: optionally-NULL string (e.g., 1st arg to acct)
	# v: vararg scalar (e.g., optional 3rd arg to open)
	# V: byte-per-page vector (3rd arg to mincore)
	# W: wait status, optionally-NULL pointer to int (e.g., 2nd arg of wait4)
       */

      Object[] args = new Object[syscall.numArgs];
      for (int i = 0; i < syscall.numArgs; ++i)
	{
	  char fmt = syscall.argList.charAt(i + 2);
	  switch (fmt)
	    {
	    case 's':
	    case 'S':
	      long addr = syscall.getArguments(task, i + 1);
	      if (addr == 0)
		args[i] = new Long(0);
	      else
		{
		  StringBuffer x = new StringBuffer();
		  task.getMemory().get(addr, 20, x);
		  args[i] = new String(x);
		}
	      break;

	    default:
	      long arg = syscall.getArguments(task, i);
	      args[i] = new Long(arg);
	      break;
	    }
	}

      syscallCache.put(task, syscall);

      synchronized(observers)
	{
	  for (Iterator it = observers.iterator(); it.hasNext(); )
	    {
	      LtraceObserver o = (LtraceObserver)it.next();
	      o.syscallEnter(task, syscall, args);
	    }
	}

      return Action.CONTINUE;
    }

    public Action updateSyscallExit (Task task)
    {
      frysk.proc.Syscall syscall = (frysk.proc.Syscall) syscallCache.remove(task);

      if (syscall == null)
	System.err.println("Warning: syscallCache should be set.");
      else
	{
	  // Unfortunately, I know of no reasonable, (as in platform
	  // independent) way to find whether a syscall is mmap,
	  // munmap, or anything else.  Hence this hack, which is
	  // probably still much better than rescanning the map on
	  // each syscall.
	  String name = syscall.getName();

	  Object ret = null;
	  char fmt = syscall.argList.charAt(0);
	  switch (fmt)
	    {
	    case 's':
	    case 'S':
	      long addr = syscall.getReturnCode(task);
	      if (addr == 0)
		ret = new Long(0);
	      else
		{
		  StringBuffer x = new StringBuffer();
		  task.getMemory().get(addr, 20, x);
		  ret = new String(x);
		}
	      break;

	    default:
	      long arg = syscall.getReturnCode(task);
	      ret = new Long(arg);
	      break;
	    }

	  synchronized(observers)
	    {
	      for (Iterator it = observers.iterator(); it.hasNext(); )
		{
		  LtraceObserver o = (LtraceObserver)it.next();
		  o.syscallLeave(task, syscall, ret);
		}
	    }

	  if (name.indexOf("mmap") != -1
	      || name.indexOf("munmap") != -1)
	    {
	      this.checkMapUnmapUpdates(task, false);
	      task.requestUnblock(this);
	      return Action.BLOCK;
	    }
	}

      return Action.CONTINUE;
    }



    // ------------------------------------------
    // --- code observer, breakpoint handling ---
    // ------------------------------------------

    // XXX: Following fields will have to be made task-aware.
    private HashMap pltBreakpoints = new HashMap();
    private HashMap dynamicBreakpoints = new HashMap();
    private HashMap staticBreakpoints = new HashMap();
    private HashMap pltBreakpointsRet = new HashMap();

    /*
    public Action libcallEnter(Task task, Symbol symbol, long address)
    {
	// XXX: this makes no sense for PLT tracing.
      Frame frame = StackFactory.createFrame(task);
      if (frame != null)
	{
	  try { frame = frame.getOuter(); }
	  catch (java.lang.NullPointerException ex) {}
	}
      String symbolName = symbol.name;
      String libraryName = symbol.getParent().getSoname();

      String callerLibrary = "(toplevel)";
      if (frame != null)
	{
	  try {
	    callerLibrary = frame.getLibraryName();
	    if (!callerLibrary.equals("Unknown"))
	      callerLibrary = ObjectFile.buildFromFile(callerLibrary).getSoname();
	  }
	  catch (java.lang.Exception ex) {
	    callerLibrary = "(Exception)";
	  }
	}
    }
    */

    public Action updateHit (Task task, long address)
    {
      Long laddress = new Long(address);

      Symbol pltEnter = null;
      Symbol pltLeave = null;
      Arch arch = (Arch)taskArchHandlers.get(task);

      Symbol symbol = (Symbol)pltBreakpoints.get(laddress);
      if (symbol != null)
	{
	  // Install breakpoint to return address.
	  long retAddr = arch.getReturnAddress(task, symbol);
	  Long retAddrL = new Long(retAddr);
	  List symList = (List)pltBreakpointsRet.get(retAddrL);
	  if (symList == null)
	    {
	      task.requestAddCodeObserver(this, retAddr);
	      symList = new LinkedList();
	      pltBreakpointsRet.put(retAddrL, symList);
	    }
	  symList.add(symbol);

	  pltEnter = symbol;
	}

      List symList = (List)pltBreakpointsRet.get(laddress);
      if (symList != null)
	{
	  pltLeave = (Symbol)symList.remove(symList.size() - 1);
	  if (symList.isEmpty())
	    {
	      pltBreakpointsRet.remove(new Long(address));
	      task.requestDeleteCodeObserver(this, address);
	    }
	}

      Object[] args = null;
      if (pltEnter != null)
	args = arch.getCallArguments(task, pltEnter);

      Object ret = null;
      if (pltLeave != null)
	ret = arch.getReturnValue(task, pltLeave);

      if (pltEnter != null || pltLeave != null)
	synchronized(observers)
	  {
	    for (Iterator it = observers.iterator(); it.hasNext(); )
	      {
		LtraceObserver o = (LtraceObserver)it.next();
		if (pltEnter != null)
		  o.pltEntryEnter(task, pltEnter, args);
		if (pltLeave != null)
		  o.pltEntryLeave(task, pltLeave, ret);
	      }
	  }
      else
	System.err.println("[" + task.getTaskId().intValue() + "] UNKNOWN BREAKPOINT 0x" + Long.toHexString(address));

      task.requestUnblock(this);
      return Action.BLOCK;
    }



    // -------------------------------------------------
    // --- attached/terminated/terminating observers ---
    // -------------------------------------------------

    public Action updateAttached (Task task)
    {
      // Per-task initialization.
      long pc = task.getIsa().pc(task);
      System.err.print("[" + task.getTaskId().intValue() + "] ");
      System.err.println("new task attached at 0x" + Long.toHexString(pc));

      this.mapsForTask.put(task, new HashMap());
      this.checkMapUnmapUpdates(task, false);

      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public Action updateTerminating(Task task, boolean signal, int value)
    {
      this.checkMapUnmapUpdates(task, true);
      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public Action updateTerminated (Task task, boolean signal, int value)
    {
      System.err.print("[" + task.getTaskId().intValue() + "] ");
      if (signal)
	System.err.println("+++ killed by " + Sig.toPrintString(value) + " +++");
      else
	System.err.println("+++ exited (status " + value + ") +++");

      return Action.CONTINUE;
    }



    // -----------------------
    // --- forked observer ---
    // -----------------------

    public Action updateForkedOffspring (Task parent, Task offspring)
    {
      if(traceChildren)
	{
	  addProc(offspring.getProc());
	  offspring.requestUnblock(this);
	  return Action.BLOCK;
	}
      return Action.CONTINUE;
    }

    public Action updateForkedParent (Task parent, Task offspring)
    {
      return Action.CONTINUE;
    }



    // ----------------------------
    // --- mmap/munmap handling ---
    // ----------------------------

    /// Remembers which files are currently mapped in which task.
    HashMap mapsForTask = new HashMap();

    private void checkMapUnmapUpdates(Task task, boolean terminating)
    {
      Map mappedFiles = (Map)this.mapsForTask.get(task);
      Map newMappedFiles = terminating
	? new HashMap ()
	: MemoryMapping.buildForPid(task.getTid());

      Set mappedFilesSet = mappedFiles.keySet();
      Set newMappedFilesSet = newMappedFiles.keySet();

      if (!newMappedFilesSet.equals(mappedFilesSet))
	{
	  // Assume that files get EITHER mapped, OR unmapped.
	  // Because under normal conditions, each map/unmap will
	  // get spotted, this is a reasonable assumption.
	  if (newMappedFilesSet.containsAll(mappedFilesSet))
	    {
	      Set diff = new HashSet(newMappedFilesSet);
	      diff.removeAll(mappedFilesSet);
	      for (Iterator it = diff.iterator(); it.hasNext(); )
		{
		  String path = (String)it.next();
		  MemoryMapping info = (MemoryMapping)newMappedFiles.get(path);
		  this.updateMappedFile(task, info);
		}
	    }
	  else
	    {
	      // We can avoid artificial `diff' set here, to gain a
	      // little performance.
	      mappedFilesSet.removeAll(newMappedFilesSet);
	      for (Iterator it = mappedFilesSet.iterator(); it.hasNext(); )
		{
		  String path = (String)it.next();
		  MemoryMapping info = (MemoryMapping)mappedFiles.get(path);
		  this.updateUnmappedFile(task, info);
		}
	    }
	}

      this.mapsForTask.put(task, newMappedFiles);
    }

    private void updateMappedFile (final Task task, MemoryMapping mapping)
    {
      synchronized(observers)
	{
	  for (Iterator it = observers.iterator(); it.hasNext(); )
	    {
	      LtraceObserver o = (LtraceObserver)it.next();
	      o.fileMapped(task, mapping.path);
	    }
	}

      // Try to load the mappped file as ELF.  Assume all
      // executable-mmapped ELF files are libraries.
      if (!mapping.permExecute)
	return;
      ObjectFile objf = ObjectFile.buildFromFile(mapping.path);
      if (objf != null)
	{
	  final long relocation = mapping.addressLow - objf.getBaseAddress();
	  final Long tracepoints[] = {null, null, null};
	  objf.eachSymbol(new ObjectFile.SymbolIterator() {
	      public void symbol(Symbol sym)
	      {
    		if (sym.pltAddress != 0
		    && symbolFilter.matchPltEntry(task, sym))
		  {
		    Long laddr = new Long(sym.pltAddress + relocation);
		    tracepoints[0] = laddr;
		    logger.log(Level.CONFIG, "Will trace PLT for " + sym.name + "\n", this);
		  }
		else
		  tracepoints[0] = null;

		if (sym.entryAddress != 0
		    && symbolFilter.matchDynamic(task, sym))
		  {
		    Long laddr = new Long(sym.entryAddress + relocation);
		    tracepoints[1] = laddr;
		    logger.log(Level.CONFIG, "Will trace (dynamic) entry for " + sym.name + "\n", this);
		  }
		else
		  tracepoints[1] = null;

		tracepoints[2] = null;

		Map[] breakpointMaps = {
		  pltBreakpoints, dynamicBreakpoints, staticBreakpoints
		};

		for (int i = 0; i < 3; ++i)
		  {
		    Long laddr = tracepoints[i];
		    if (laddr != null)
		      {
			if (breakpointMaps[i].containsKey(laddr))
			  {
			    // We got an alias.  Put the symbol with the
			    // shorter name into the map.
			    Symbol original = (Symbol)breakpointMaps[i].get(laddr);
			    if (sym.name.length() < original.name.length())
			      breakpointMaps[i].put(laddr, sym);
			  }
			else
			  {
			    task.requestAddCodeObserver(ltraceTaskObserver, laddr.longValue());
			    breakpointMaps[i].put(laddr, sym);
			  }
		      }
		  }
	      }
	    });
	}
    }

    private void updateUnmappedFile (Task task, MemoryMapping mapping)
    {
      synchronized(observers)
	{
	  for (Iterator it = observers.iterator(); it.hasNext(); )
	    {
	      LtraceObserver o = (LtraceObserver)it.next();
	      o.fileUnmapped(task, mapping.path);
	    }
	}
    }



    // ----------------------------------------
    // --- Higher level observer interfaces ---
    // ----------------------------------------

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      System.err.print("[" + ((Task)observable).getTid() + "] ");
      w.printStackTrace();
      //throw new RuntimeException("Failed to add an observer to the process", w);
    }

  }

  LtraceTaskObserver ltraceTaskObserver = new LtraceTaskObserver();
}
