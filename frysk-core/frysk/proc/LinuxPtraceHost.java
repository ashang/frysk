// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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


package frysk.proc;

import frysk.event.Event;
import frysk.event.EventLoop;
import frysk.sys.Fork;
import frysk.sys.Pid;
import frysk.sys.Tid;
import frysk.sys.proc.Stat;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import frysk.sys.proc.ProcBuilder;
import java.util.logging.Level;

/**
 * A Linux Host.
 */

public class LinuxPtraceHost
    extends Host
{
  final EventLoop eventLoop;

  /**
   * Construct an instance of the LinuxPtraceHost that uses the specified eventLoop.
   */
  LinuxPtraceHost (EventLoop eventLoop)
  {
    this.eventLoop = eventLoop;
    eventLoop.add(new LinuxPtraceWaitBuilder(LinuxPtraceHost.this));
  }

  /**
   * Either add or update a process, however, before doing that determine the
   * parent and ensure that it has been updated.
   */
  private class ProcChanges
  {
    /**
     * ADDED accumulates all the tasks added as things are updated.
     */
    List added = new LinkedList();

    /**
     * REMOVED starts with the full list of processes and then works backwards
     * removing any that are processed, by the end it contains processes that no
     * longer exist.
     */
    HashMap removed = (HashMap) ((HashMap) procPool).clone();

    /**
     * Update PROCID, either adding it
     */
    Proc update (int pid)
    {
      ProcId procId = new ProcId(pid);
      Proc proc = (Proc) procPool.get(procId);
      if (proc == null)
        {
          // New, unknown process. Try to find both the process
          // and its parent. In the case of a daemon process, a
          // second attempt may be needed.
          Stat stat = new Stat();
          Proc parent = null;
          int attempt = 0;
          while (true)
            {
              // Should take no more than two attempts - one for
              // a normal process, and one for a daemon.
              if (attempt++ >= 2)
                return null;
              // Scan in the process's stat file. Of course, if
              // the stat file disappeared indicating that the
              // process exited, return NULL.
              if (! stat.refresh(procId.id))
                return null;
              // Find the parent, every process, except process
              // 1, has a parent.
              if (pid <= 1)
                break;
              parent = update(stat.ppid);
              if (parent != null)
                break;
            }
          // .. and then add this process.
          proc = new LinuxPtraceProc(LinuxPtraceHost.this, parent, procId, stat);
          added.add(proc);
        }
      else if (removed.get(procId) != null)
        {
          // Process 1 never gets a [new] parent.
          if (pid > 1)
            {
              Stat stat = ((LinuxPtraceProc) proc).getStat();
              // An existing process that hasn't yet been
              // updated. Still need check that its parent
              // didn't change (assuming there is one).
              if (! stat.refresh(pid))
                // Oops, just disappeared.
                return null;
              Proc oldParent = proc.getParent();
              if (oldParent.getPid() != stat.ppid)
                {
                  // Transfer ownership
                  Proc newParent = update(stat.ppid);
                  oldParent.remove(proc);
                  proc.parent = newParent;
                  newParent.add(proc);
                }
            }
          removed.remove(procId);
        }
      return proc;
    }
  }

  protected void sendRefresh (boolean refreshAll)
  {
    // Iterate (build) the /proc tree, passing each found PID to
    // procChanges where it can update the /proc tree.
    final ProcChanges procChanges = new ProcChanges();
    ProcBuilder pidBuilder = new ProcBuilder()
    {
      public void buildId (int pid)
      {
        procChanges.update(pid);
      }
    };
    pidBuilder.construct();
    // If requested, tell each process that it too should refresh.
    if (refreshAll)
      {
        // Changes individual process.
        for (Iterator i = procPool.values().iterator(); i.hasNext();)
          {
            LinuxPtraceProc proc = (LinuxPtraceProc) i.next();
            proc.sendRefresh();
          }
      }
    // Tell each process that no longer exists that it has been
    // destroyed.
    for (Iterator i = procChanges.removed.values().iterator(); i.hasNext();)
      {
        Proc proc = (Proc) i.next();
        // XXX: Should there be a ProcEvent.schedule(), instead of
        // Manager .eventLoop .appendEvent for injecting the event
        // into the event loop?
        proc.performRemoval();
        remove(proc);
      }
  }
  
  protected void sendRefresh (final ProcId procId, final FindProc finder)
  {

    // Iterate (build) the /proc tree starting with the given procId.
    final ProcChanges procChanges = new ProcChanges();
    ProcBuilder pidBuilder = new ProcBuilder()
    {
      public void buildId (int pid)
      {
        procChanges.update(pid);
      }
    };
    pidBuilder.construct(procId.id);

    if (!(procPool.containsKey(procId)))
      {

        Manager.eventLoop.add(new Event()
        {

          public void execute ()
          {
            finder.procNotFound(procId, new RuntimeException(
                                                             "Couldn't find the proc"
                                                                 + procId));
          }
        });
        return;
      }

    
    LinuxPtraceProc proc = (LinuxPtraceProc) Manager.host.getProc(procId);
    proc.sendRefresh();
    
    Manager.eventLoop.add(new Event()
    {

      public void execute ()
      {
        finder.procFound(procId);
      }
    });

  } 

  /**
   * Create an attached process that is a child of this process (and
   * this task).
   */
  protected void sendCreateAttachedProc (String in, String out, String err,
					 String[] args,
					 TaskObserver.Attached attached)
  {
    logger.log(Level.FINE, "{0} sendCreateAttachedProc\n", this);
    int pid = Fork.ptrace(in, out, err, args);
    // See if the Host knows about this task.
    TaskId myTaskId = new TaskId(Tid.get());
    Task myTask = get(myTaskId);
    if (myTask == null) {
	// If not, find this process and add this task to it.
	Proc myProc = getSelf();
	myTask = new LinuxPtraceTask (myProc, myTaskId);
    }
    Proc proc = new LinuxPtraceProc (myTask, new ProcId(pid));
    new LinuxPtraceTask (proc, attached);
  }

  /**
   * Return a pointer to this <em>frysk</em> instance.
   */
  protected Proc sendrecSelf ()
  {
    ProcChanges procChanges = new ProcChanges();
    return procChanges.update(Pid.get());
  }  

  void sendGetProc (final ProcId procId, final FindProc finder)
  {
    ProcBuilder pidBuilder = new ProcBuilder()
    {
      public void buildId (int pid)
      {
        Proc proc = (Proc) procPool.get(procId);
        if (proc == null)
          {
            // New, unknown process. Try to find both the process
            // and its parent. In the case of a daemon process, a
            // second attempt may be needed.
            Stat stat = new Stat();
            int attempt = 0;
            while (true)
              {
                // Should take no more than two attempts - one for
                // a normal process, and one for a daemon.
                if (attempt++ >= 2)
                  break;
                // Scan in the process's stat file. Of course, if
                // the stat file disappeared indicating that the
                // process exited, return NULL.
                if (! stat.refresh(procId.id))
                  return;                
              }
            // .. and then add this process.
            new LinuxPtraceProc(LinuxPtraceHost.this, null, procId, stat);
          }
      }
    };
    pidBuilder.construct(procId.id);

    if (!(procPool.containsKey(procId)))
      {

        Manager.eventLoop.add(new Event()
        {

          public void execute ()
          {
            finder.procNotFound(procId, new RuntimeException(
                                                             "Couldn't find the"
                                                             + "proc with id: "
                                                                 + procId.id));
          }
        });
        return;
      }
    
    LinuxPtraceProc proc = (LinuxPtraceProc) Manager.host.getProc(procId);
    proc.sendRefresh();
    
    Manager.eventLoop.add(new Event()
    {

      public void execute ()
      {
        finder.procFound(procId);
      }
    });
  }
}
