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

import inua.util.PrintWriter;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import frysk.EventLogger;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

class ftrace
{
  private static Proc proc;

  public static void main (String[] args)
  {
    final PrintWriter out = new PrintWriter(System.out, true);

//    Logger logger = EventLogger.get ("logs/", "frysk_core_event.log");
//    Handler handler = new ConsoleHandler ();
//    handler.setLevel(Level.FINEST);
//    logger.addHandler(handler);
//    logger.setLevel(Level.ALL);
//    LogManager.getLogManager().addLogger(logger);
     
    if (args.length == 0)
      {
        out.println("Usage: ftrace <command [arg ...]>");
        return;
      }
    
    
    Manager.host.requestCreateAttachedProc(args, new AttachedObserver());
    Manager.eventLoop.start();
  }

  protected static void setProc (Proc myProc)
  {
    proc = myProc;
    Manager.host.observableProcRemovedXXX.addObserver (new ProcRemovedObserver(proc));
    System.out.println("ftrace.main() Proc.getPid() " + proc.getPid());
    System.out.println("ftrace.main() Proc.getPid() " + proc.getExe());
  }
  
  /**
   * An observer to stop the eventloop when the traced process
   * exits.
   */
  private static class ProcRemovedObserver implements Observer{
    int pid;
    
    ProcRemovedObserver(Proc proc){
      this.pid = proc.getPid();
    }
    
    public void update (Observable o, Object object)
    {
      Proc proc = (Proc) object;
      if (proc.getPid() == this.pid) {
        Manager.eventLoop.requestStop ();
      }
    }
  }
  
  /**
   * An observer that sets up things once frysk has set up
   * the requested proc and attached to it.
   */
  private static class AttachedObserver implements TaskObserver.Attached{
    public Action updateAttached (Task task)
    {
      task.requestAddSyscallObserver(new SyscallObserver());
      setProc(task.getProc());
      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public void addedTo (Object observable){}

    public void addFailed (Object observable, Throwable w){
      throw new RuntimeException("Failed to attach to created proc", w);
    }

    public void deletedFrom (Object observable){}
  }
  
  /**
   * The syscallObserver added to the traced proc.
   */
  private static class SyscallObserver implements TaskObserver.Syscall{

    public Action updateSyscallEnter (Task task)
    {
      SyscallEventInfo syscallEventInfo;
      try
	{
	  syscallEventInfo = task.getSyscallEventInfo ();
	}
      catch (Task.TaskException e) 
	{
	  // XXX Abort? or what?
	  System.out.println("Got task exception " + e);
	  return Action.CONTINUE;
	}
      frysk.proc.Syscall syscall = frysk.proc.Syscall.syscallByNum(syscallEventInfo.number(task));
      PrintWriter printWriter = new PrintWriter(System.out);
      printWriter.print(task.getProc().getPid() + "." + task.getTid() + " ");
      syscall.printCall(printWriter, task, syscallEventInfo);
      printWriter.flush();
      return Action.CONTINUE;
    }

    public Action updateSyscallExit (Task task)
    {
      SyscallEventInfo syscallEventInfo;
      try
	{
	  syscallEventInfo = task.getSyscallEventInfo ();
	}
      catch (Task.TaskException e) 
	{
	  // XXX Abort? or what?
	  System.out.println("Got task exception " + e);
	  return Action.CONTINUE;
	}
      frysk.proc.Syscall syscall = frysk.proc.Syscall.syscallByNum(syscallEventInfo.number(task));
      PrintWriter printWriter = new PrintWriter(System.out);
      syscall.printReturn(printWriter, task, syscallEventInfo);
      printWriter.flush();
      return Action.CONTINUE;
    }

    public void addedTo (Object observable)
    {
      
    }

    public void addFailed (Object observable, Throwable w)  {
      throw new RuntimeException("Failed to add a Systemcall observer to the process",w);
    }

    public void deletedFrom (Object observable)
    {
      throw new RuntimeException("This has not yet been implemented");
    }
    
  }
  
}
