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

package frysk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import inua.eio.ByteBuffer;

import frysk.proc.Action;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Register;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

import frysk.sys.Sig;
import frysk.sys.proc.MapsBuilder;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfRel;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfSectionHeader;
import lib.dwfl.ElfSymbol;
import lib.dwfl.ElfSymbolBuilder;

class MemMappedFiles
  extends MapsBuilder
{
  private byte[] buf;
  private Set mappedFiles = new HashSet ();

  public void buildBuffer (byte[] buf)
  {
    this.buf = buf;
  }

  public void buildMap (long addressLow, long addressHigh,
			boolean permRead, boolean permWrite,
			boolean permExecute, boolean shared,
			long offset,
			int devMajor, int devMinor,
			int inode,
			int pathnameOffset, int pathnameLength)
  {
    String path = new String (this.buf, pathnameOffset, pathnameLength);
    if (path.length () > 0 && path.charAt(0) != '[')
      {
	if (path.charAt(0) != '/')
	  throw new AssertionError ("Unexpected: first character of path in map is neither '[', nor '/'.");
	mappedFiles.add(path);
      }
  }

  public static Set forPid (int pid)
  {
    MemMappedFiles mappings = new MemMappedFiles();
    mappings.construct(pid);
    return mappings.mappedFiles;
  }
}

class LtraceSymbol
{
  public final String name;
  public final long value;
  public final long size;
  public final int type;
  public final long shndx;
  protected LtraceObjectFile parent = null;

  /**
   * Address associated with symbol.  Most often this is address of
   * PLT entry, but it can be any address suitable for breakpoint
   * tracking.
   */
  public long address;

  /**
   * Build ltrace symbol.
   *
   * @param name Name of the symbol.
   * @param type Type of the symbol, as in ElfSymbol.ELF_STT_* fields.
   * @param value Value of the symbol.
   * @param size Size of the symbol.
   * @param shndx Associated section index, or one of the special
   *   values in ElfSectionHeader.ELF_SHN_*.
   */
  public LtraceSymbol(String name, int type, long value,
		      long size, long shndx)
  {
    this.name = name;
    this.type = type;
    this.value = value;
    this.size = size;
    this.shndx = shndx;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append(this.name);
    return buf.toString();
  }

  public void addedTo(LtraceObjectFile of) {
    this.parent = of;
  }

  public LtraceObjectFile getParent() {
    return this.parent;
  }

  public void setAddress(long address) {
    this.address = address;
  }
}

class LtraceObjectFile
{
  public HashMap symbolMap = new HashMap();
  public String name;

  public interface SymbolIterator {
    void symbol(LtraceSymbol symbol);
  }

  private LtraceObjectFile (String name)
  {
    this.name = name;
  }

  public void addSymbol(LtraceSymbol symbol)
  {
    symbolMap.put(symbol.name, symbol);
    symbol.addedTo(this);
  }

  public LtraceSymbol symbolAt(final long address)
  {
    // XXX: Huh, this is ugly.  Eventually either invent some observer
    // mechanism where ObjectFile can cache observed symbol addresses,
    // or, if it turns out that address almost never changes, make it
    // final or something.
    final LinkedList list = new LinkedList();
    this.eachSymbol(new SymbolIterator(){
	public void symbol(LtraceSymbol symbol) {
	  if (symbol.address == address)
	    list.add(symbol);
	}
      });

    if (list.isEmpty())
      return null;
    if (list.size() > 1)
      System.err.println("Strange: symbolAt(0x" + Long.toHexString(address) + ") has more than one symbol...");
    return (LtraceSymbol)list.getFirst();
  }

  public void eachSymbol(SymbolIterator client)
  {
    int mapsize = symbolMap.size();
    Iterator it = symbolMap.entrySet().iterator();
    for (int i = 0; i < mapsize; i++)
      {
	Map.Entry entry = (Map.Entry)it.next();
	LtraceSymbol sym = (LtraceSymbol)entry.getValue();
	client.symbol(sym);
      }
  }

  public static LtraceObjectFile buildForProc(Proc proc)
  {
    try {
	Elf elfFile = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
	ElfEHeader eh = elfFile.getEHeader();
	LtraceObjectFile objFile = new LtraceObjectFile(proc.getExe());

	if (eh.type == ElfEHeader.PHEADER_ET_EXEC)
	  System.err.println("Executable");
	else if (eh.type == ElfEHeader.PHEADER_ET_DYN)
	  System.err.println("PIE executable");
	else
	  System.err.println("Unknown ELF type " + eh.type + "!");

	long pltAddr = 0;
	long pltSize = 0;
	ElfRel[] pltRelocs = null;
	final ArrayList symbols = new ArrayList();

	boolean haveDynsym = false;
	boolean havePlt = false;
	boolean haveRelPlt = false;

	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section))
	  {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (sheader.type == ElfSectionHeader.ELF_SHT_DYNSYM)
	      {
		haveDynsym = true;

		ElfSymbol.loadFrom(section, new ElfSymbolBuilder() {
		    public void symbol (String name, long value, long size,
					int type, int bind, int visibility,
					long shndx)
		    {
		      LtraceSymbol sym = new LtraceSymbol(name, type, value, size, shndx);
		      sym.setAddress(value);
		      symbols.add(sym);
		    }
		  });

		for (Iterator it = symbols.iterator(); it.hasNext(); )
		  {
		    LtraceSymbol sym = (LtraceSymbol)it.next();
		    if (sym.type == ElfSymbol.ELF_STT_FUNC
			&& (sym.shndx == ElfSectionHeader.ELF_SHN_UNDEF
			    || sym.value == 0))
		      {
			System.err.println("Got library function symbol `" + sym.name + "'.");
			objFile.addSymbol(sym);
		      }
		  }
	      }
	    else if (sheader.type == ElfSectionHeader.ELF_SHT_PROGBITS
		     && sheader.name.equals(".plt"))
	      {
		havePlt = true;
		pltAddr = sheader.addr;
		pltSize = sheader.size;
	      }
	    else if (sheader.type == ElfSectionHeader.ELF_SHT_REL
		     && sheader.name.equals(".rel.plt"))
	      {
		haveRelPlt = true;
		pltRelocs = ElfRel.loadFrom(section);
	      }
	  }

	if (!havePlt)
	  throw new lib.dwfl.ElfFileException("No .plt found in ELF file.");
	if (!haveRelPlt)
	  throw new lib.dwfl.ElfFileException("No .rel.plt found in ELF file.");
	if (!haveDynsym)
	  throw new lib.dwfl.ElfFileException("No .dynsym found in ELF file.");

	long pltEntrySize = pltSize / (pltRelocs.length + 1);
	for (int i = 0; i < pltRelocs.length; ++i)
	  {
	    long entryAddr = pltAddr + pltEntrySize * (i + 1);
	    LtraceSymbol symbol = (LtraceSymbol)symbols.get((int)pltRelocs[i].symbolIndex - 1);
	    System.out.println("PLT entry for `" + symbol.name + "' is at 0x" + Long.toHexString(entryAddr) + ".");
	    symbol.setAddress(entryAddr);
	  }

	return objFile;
      }
    catch (lib.dwfl.ElfFileException efe)
      {
	efe.printStackTrace ();
	System.err.println("load error: " + efe);
      }
    catch (lib.dwfl.ElfException eexp)
      {
	eexp.printStackTrace ();
	System.err.println("load error: " + eexp);
      }

    return null;
  }
}

public class Ltrace
{
  // Set of pids to trace initially.
  HashSet pidsToTrace = new HashSet();

  // True if we're tracing children as well.
  boolean traceChildren = false;

  // True if we're tracing signals as well.
  boolean traceSignals = false;

  // Task counter.
  int numTasks = 0;

  // Mapping between processes and their LtraceObjectFile descriptors.
  HashMap objectFiles = new HashMap();

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
	  System.err.println("=== existing " + task.getTid() + "===");
	  addTask(task);
	  if (task.getTid() == task.getProc().getMainTask().getTid())
	    perProcInit(task.getProc());
	}

	public void taskAdded (Task task)
	{
	  System.err.println("=== added " + task.getTid() + " ===");
	  addTask(task);
	}

	public void taskRemoved (Task task)
	{
	  System.err.println("=== removed " + task.getTid() + " ===");
	  removeTask(task);
	}

	public void addFailed (Object arg0, Throwable w) {}
	public void addedTo (Object arg0) {}
	public void deletedFrom (Object arg0) {}
      });
  }

  /**
   * Do a per-process init.  Call this only when process task(s)
   * exist(s) and are attached to.
   */
  private void perProcInit(Proc proc)
  {
    LtraceObjectFile objf = (LtraceObjectFile)objectFiles.get(proc);
    if (objf == null)
      objf = LtraceObjectFile.buildForProc(proc);

    if (objf == null)
      {
	System.err.println("Error in loading executable or libraries. The process will not be traced.");
	return;
      }
    else
      objectFiles.put(proc, objf);

    final Task mainTask = proc.getMainTask();
    objf.eachSymbol(new LtraceObjectFile.SymbolIterator() {
	public void symbol(LtraceSymbol sym) {
	  System.out.println("Add breakpoint to 0x" + Long.toHexString(sym.address)
			     + " for " + sym.name);
	  mainTask.requestAddCodeObserver(ltraceTaskObserver, sym.address);
	}
      });
  }

  /**
   * Trace new task of existing process.
   */
  private void addTask (Task task)
  {
    synchronized (this) {
      numTasks++;
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
	  return ltraceTaskObserver.updateAttached(task);
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
      syscallCache.put(task, syscall);

      if (traceSignals)
	{
	  System.out.print("[" + task.getTaskId().intValue() + "] ");
	  System.out.println("syscall enter " + syscall.getName());
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

	  if (name.indexOf("mmap") != -1
	      || name.indexOf("munmap") != -1)
	    return this.checkMapUnmapUpdates(task, false);

	  if (traceSignals)
	    {
	      System.out.print ("[" + task.getTaskId().intValue() + "] ");
	      System.out.println ("syscall leave " + name);
	    }
	}

      return Action.CONTINUE;
    }



    // ------------------------------------------
    // --- code observer, breakpoint handling ---
    // ------------------------------------------

    public Action updateHit (Task task, long address)
    {
      //long pc = task.getIsa().pc(task);
      System.err.print("[" + task.getTaskId().intValue() + "] ");
      LtraceObjectFile objf = (LtraceObjectFile)objectFiles.get(task.getProc());
      LtraceSymbol symbol = objf.symbolAt(address);
      System.err.print("call " + symbol.name + "(");

      ByteBuffer buf = task.getMemory();

      // i386 only at this time
      Register espRegister = task.getIsa().getRegisterByName("esp");
      long esp = espRegister.get(task);
      esp += 4;

      // Poor man's call formatter... both traditional ltrace-style
      // and dwarf-enhanced formatters will be implemented in future,
      // this is just to test some stuff...
      if (symbol.name.equals("puts"))
	{
	  long pointer = buf.getInt(esp);
	  System.err.print('"');
	  while(true)
	    {
	      byte value = buf.getByte(pointer);
	      if (value == 0)
		break;
	      ++pointer;
	      if (value < 32)
		{
		  long val = value < 0 ? 255 + value : value;
		  System.err.print("\\x" + Long.toHexString(val));
		}
	      else
		System.err.print((char)value);
	    }
	  System.err.print('"');
	}
      else
	for (long i = 0; i < 8; ++i)
	  {
	    int value = buf.getInt(esp);
	    System.err.print("0x" + Long.toHexString(value) + ", ");
	    esp += 4;
	  }
      System.err.println(")");
      return Action.CONTINUE;
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

      this.mapsForTask.put(task, new HashSet());
      this.checkMapUnmapUpdates(task, false);

      return Action.CONTINUE;
    }

    public Action updateTerminating(Task task, boolean signal, int value)
    {
      return this.checkMapUnmapUpdates(task, true);
    }

    public Action updateTerminated (Task task, boolean signal, int value)
    {
      System.out.print("[" + task.getTaskId().intValue() + "] ");
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

    private Action checkMapUnmapUpdates(Task task, boolean terminating)
    {
      // Note that when several files get mapped or unmapped (which
      // should generally happen only on program start or end), and
      // handler blocks in the middle of the list, you lose the rest of
      // the list.  This may be considered a bug.

      int pid = task.getTid();
      Set mappedFiles = (Set)this.mapsForTask.get(task);
      Set newMappedFiles = terminating
	? new HashSet ()
	: MemMappedFiles.forPid(pid);

      if (!newMappedFiles.equals(mappedFiles))
	{
	  // Assume that files get EITHER mapped, OR unmapped.
	  // Because under normal conditions, each map/unmap will
	  // get spotted, this is a reasonable assumption.
	  if (newMappedFiles.containsAll(mappedFiles))
	    {
	      Set diff = new HashSet(newMappedFiles);
	      diff.removeAll(mappedFiles);
	      for (Iterator it = diff.iterator(); it.hasNext(); )
		{
		  Action a = this.updateMappedFile (task, it.next().toString());
		  if (a != Action.CONTINUE)
		    return a;
		}
	    }
	  else
	    {
	      // We can avoid artificial `diff' set here, to gain a
	      // little performance.
	      mappedFiles.removeAll(newMappedFiles);
	      for (Iterator it = mappedFiles.iterator(); it.hasNext(); )
		{
		  Action a = this.updateUnmappedFile (task, it.next().toString());
		  if (a != Action.CONTINUE)
		    return a;
		}
	    }
	}

      this.mapsForTask.put(task, newMappedFiles);
      return Action.CONTINUE;
    }

    private void reportMapUnmap(Task task, String filename, String method)
    {
      int pid = task.getTid();
      System.out.println ("[" + pid + "]"
			  + " " + method
			  + " " + filename);
    }

    public Action updateMappedFile (Task task, String filename)
    {
      this.reportMapUnmap(task, filename, "map");
      return Action.CONTINUE;
    }

    public Action updateUnmappedFile (Task task, String filename)
    {
      this.reportMapUnmap(task, filename, "unmap");
      return Action.CONTINUE;
    }



    // ----------------------------------------
    // --- Higher level observer interfaces ---
    // ----------------------------------------

    public void addedTo (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to add an observer to the process", w);
    }

    public void deletedFrom (Object observable)
    {
      throw new RuntimeException("This has not yet been implemented");
    }
  }

  LtraceTaskObserver ltraceTaskObserver = new LtraceTaskObserver();
}
