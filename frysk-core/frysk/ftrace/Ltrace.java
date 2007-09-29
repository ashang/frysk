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
import java.util.logging.*;

import frysk.proc.Action;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

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

  // List of observers.
  protected List observers = new ArrayList();

  // The controller that decides what should be traced.
  protected final LtraceController ltraceController;

  public Ltrace(LtraceController ltraceController)
  {
    this.ltraceController = ltraceController;
  }

  public interface Driver
  {
    void tracePoint(Task task, TracePoint tp);
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
   * Trace new process.  Processes all existing tasks of a process,
   * and monitors the process for new tasks.
   *
   * @param blocker An observer that blocks current process until
   * the tasks are processed.  Unblock is requested here.
   */
  private void addProc(Proc proc, TaskObserver blocker)
  {
    Task mainTask = proc.getMainTask();

    // If a task appears after the getTasks() list is generated, but
    // before ProcTasks iteration below, it will be reported as
    // existing at ProcTasks iteration.  This set exists to
    // differentiate between yet-unprocessed and already processed
    // "existing" tasks.
    final Set processedTasks = new HashSet();

    for (Iterator it = proc.getTasks().iterator(); it.hasNext(); )
      {
	Task task = (Task)it.next();
	processedTasks.add(task);
	if (task.getTid() == mainTask.getTid())
	  perProcInit(task.getProc());
	addTask(task);
      }

    new ProcTasksObserver(proc, new ProcObserver.ProcTasks() {
	public void existingTask(Task task)
	{
	  if (!processedTasks.contains(task))
	    addTask(task);
	}

	public void taskAdded(Task task)
	{
	  addTask(task);
	}

	public void taskRemoved(Task task)
	{
	  removeTask(task);
	}

	public void addFailed(Object arg0, Throwable w) {}
	public void addedTo(Object arg0) {}
	public void deletedFrom(Object arg0) {}
      });

    if (blocker != null)
      mainTask.requestUnblock(blocker);
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

    if (numTasks == 0)
      Manager.eventLoop.requestStop();
  }

  /**
   * Start tracing a set of PIDs defined on commandline.
   */
  public void trace()
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
	    addProc(proc, null);
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
  public void trace(String[] command)
  {
    if (!pidsToTrace.isEmpty())
      throw new AssertionError("Unexpected: tracing both pids and command.");

    Manager.host.requestCreateAttachedProc(command, new TaskObserver.Attached() {
	public Action updateAttached (Task task)
	{
	  addProc(task.getProc(), this);
	  ltraceTaskObserver.updateAttached(task);
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
    /** Remembers which files are currently mapped in which task. */
    private HashMap mapsForTask = new HashMap();

    /** Remembers which tracepoint is associated with which breakpoint..
	Map&lt;task, Map&lt;address, TracePoint&gt;&gt; */
    private HashMap breakpointsForTask = new HashMap();

    /** Remembers return from which tracepoint is associated with
	which breakpoint.
        Map&lt;task, Map&lt;address, List&lt;TracePoint&gt;&gt;&gt; */
    private HashMap retBreakpointsForTask = new HashMap();

    // ---------------------
    // --- ltrace driver ---
    // ---------------------

    private class DriverImpl
      implements Ltrace.Driver
    {
      final long relocation;

      public DriverImpl(long relocation)
      {
	this.relocation = relocation;
      }

      public void tracePoint(Task task, TracePoint tp)
      {
	long addr = tp.address + this.relocation;
	Long laddr = new Long(addr);
	logger.log(Level.CONFIG, "Will trace `" + tp.symbol.name
		   + "' at 0x" + Long.toHexString(addr), this);

	// FIXME: probably handle aliases at a lower
	// lever.  Each tracepoint should point to a list
	// of symbols that alias it, and should be present
	// only once in an ObjectFile.
	synchronized (LtraceTaskObserver.this)
	  {
	    HashMap breakpoints = (HashMap)breakpointsForTask.get(task);
	    if (breakpoints.containsKey(laddr))
	      {
		// We got an alias.  Put the symbol with the
		// shorter name into the map.
		TracePoint original = (TracePoint)breakpoints.get(laddr);
		if (tp.symbol.name.length() < original.symbol.name.length())
		  breakpoints.put(laddr, tp);
	      }
	    else
	      {
		task.requestAddCodeObserver(ltraceTaskObserver, laddr.longValue());
		breakpoints.put(laddr, tp);
	      }
	  }
      }
    }

    // ------------------------
    // --- syscall observer ---
    // ------------------------

    /// Remembers which syscall is currently handled in which task.
    private HashMap syscallCache = new HashMap();

    public Action updateSyscallEnter (Task task)
    {
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

      return Action.CONTINUE;
    }



    // ------------------------------------------
    // --- code observer, breakpoint handling ---
    // ------------------------------------------

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
      logger.log(Level.FINE, "Breakpoint at 0x" + Long.toHexString(address), this);
      Long laddress = new Long(address);

      TracePoint enter = null;
      TracePoint leave = null;
      Arch arch = (Arch)taskArchHandlers.get(task);

      // Obtain breakpoint maps for this task.
      HashMap breakpoints;
      HashMap breakpointsRet;
      synchronized (this)
	{
	  breakpoints = (HashMap)breakpointsForTask.get(task);
	  breakpointsRet = (HashMap)retBreakpointsForTask.get(task);
	}

      // See if we enter somewhere.
      TracePoint tp = (TracePoint)breakpoints.get(laddress);
      if (tp != null)
	{
	  if (address != tp.symbol.getParent().getEntryPoint())
	    {
	      // Install breakpoint to return address.
	      long retAddr = arch.getReturnAddress(task, tp.symbol);
	      logger.log(Level.FINER,
			 "It's enter tracepoint, return address 0x"
			 + Long.toHexString(retAddr) + ".", this);
	      Long retAddrL = new Long(retAddr);
	      List tpList = (List)breakpointsRet.get(retAddrL);
	      if (tpList == null)
		{
		  task.requestAddCodeObserver(this, retAddr);
		  tpList = new LinkedList();
		  breakpointsRet.put(retAddrL, tpList);
		}
	      tpList.add(tp);
	    }
	  else
	    logger.log(Level.FINEST,
		       "It's _start, no return breakpoint established..", this);

	  enter = tp;
	}

      // See if we returned from somewhere.
      List tpList = (List)breakpointsRet.get(laddress);
      if (tpList != null)
	{
	  logger.log(Level.FINER, "It's leave tracepoint.", this);
	  leave = (TracePoint)tpList.remove(tpList.size() - 1);
	  if (tpList.isEmpty())
	    {
	      logger.log(Level.FINEST, "Removing leave breakpoint.", this);
	      breakpointsRet.remove(new Long(address));
	      task.requestDeleteCodeObserver(this, address);
	    }
	}

      if (enter != null || leave != null)
	{
	  Object[] args = null;
	  if (enter != null)
	    {
	      logger.log(Level.FINEST, "Building arglist.", this);
	      args = arch.getCallArguments(task, enter.symbol);
	    }

	  Object ret = null;
	  if (leave != null)
	    {
	      logger.log(Level.FINEST, "Fetching retval.", this);
	      ret = arch.getReturnValue(task, leave.symbol);
	    }

	  synchronized(observers)
	    {
	      for (Iterator it = observers.iterator(); it.hasNext(); )
		{
		  LtraceObserver o = (LtraceObserver)it.next();
		  if (enter != null)
		    o.funcallEnter(task, enter.symbol, args);
		  if (leave != null)
		    o.funcallLeave(task, leave.symbol, ret);
		}
	    }
	}
      else
	System.err.println("[" + task.getTaskId().intValue() + "] UNKNOWN BREAKPOINT 0x" + Long.toHexString(address));

      logger.log(Level.FINE, "Breakpoint handled.", this);
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
      logger.log(Level.FINE,
		 "new task attached at 0x" + Long.toHexString(pc)
		 + ", pid=" + task.getTaskId().intValue(), this);

      this.mapsForTask.put(task, java.util.Collections.EMPTY_SET);

      // Can't use the EMPTY_MAPs here, cause these get modified directly.
      this.breakpointsForTask.put(task, new HashMap());
      this.retBreakpointsForTask.put(task, new HashMap());

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

    public Action updateTerminated(Task task, boolean signal, int value)
    {
      synchronized(observers)
	{
	  for (Iterator it = observers.iterator(); it.hasNext(); )
	    {
	      LtraceObserver o = (LtraceObserver)it.next();
	      o.taskTerminated(task, signal, value);
	    }
	}

      return Action.CONTINUE;
    }



    // -----------------------
    // --- forked observer ---
    // -----------------------

    public Action updateForkedOffspring (Task parent, Task offspring)
    {
      if(traceChildren)
	{
	  addProc(offspring.getProc(), null);
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

    private void checkMapUnmapUpdates(Task task, boolean terminating)
    {
      Set mappedFiles = (Set)this.mapsForTask.get(task);
      Set newMappedFiles = terminating ?
	java.util.Collections.EMPTY_SET : MemoryMapping.buildForPid(task.getTid());

      // Assume that files get EITHER mapped, OR unmapped.  Because
      // under normal conditions, each map/unmap will get spotted,
      // this is a reasonable assumption.
      if (newMappedFiles.size() != mappedFiles.size())
	{
	  if (newMappedFiles.size() > mappedFiles.size())
	    {
	      Set diff = new HashSet(newMappedFiles);
	      diff.removeAll(mappedFiles);
	      for (Iterator it = diff.iterator(); it.hasNext(); )
		{
		  MemoryMapping info = (MemoryMapping)it.next();
		  this.updateMappedFile(task, info);
		}
	    }
	  else
	    {
	      // We can avoid artificial `diff' set here, to gain a
	      // little performance.
	      mappedFiles.removeAll(newMappedFiles);
	      for (Iterator it = mappedFiles.iterator(); it.hasNext(); )
		{
		  MemoryMapping info = (MemoryMapping)it.next();
		  this.updateUnmappedFile(task, info);
		}
	    }
	}

      this.mapsForTask.put(task, newMappedFiles);
    }

    private void updateMappedFile(final Task task, MemoryMapping mapping)
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
      if (objf == null)
	return;

      // It's actually cheaper to create one driver per task and file
      // loaded, than to look up relocation for each TracePoint
      // processed by the driver.
      long relocation = mapping.addressLow - objf.getBaseAddress();
      DriverImpl driver = new DriverImpl(relocation);
      ltraceController.fileMapped(task, objf, driver);
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
