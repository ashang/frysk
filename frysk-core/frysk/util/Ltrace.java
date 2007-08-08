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

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

/*import frysk.stack.Frame;
import frysk.stack.StackFactory;*/

import frysk.sys.Sig;
import frysk.sys.proc.MapsBuilder;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfDynamic;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfRel;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfSectionHeader;
import lib.dwfl.ElfSymbol;

class MappingInfo
{
  public String path;
  public long addressLow;
  public long addressHigh;
  public boolean permRead;
  public boolean permWrite;
  public boolean permExecute;

  public MappingInfo(String path, long addressLow, long addressHigh,
		     boolean permRead, boolean permWrite, boolean permExecute)
  {
    this.path = path;
    this.addressLow = addressLow;
    this.addressHigh = addressHigh;
    this.permRead = permRead;
    this.permWrite = permWrite;
    this.permExecute = permExecute;
  }
}

class MemMappedFiles
  extends MapsBuilder
{
  private byte[] buf;
  private Map mappedFiles = new HashMap();

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
    String path = new String(this.buf, pathnameOffset, pathnameLength);
    if (path.length () > 0 && path.charAt(0) != '[')
      {
	if (path.charAt(0) != '/')
	  throw new AssertionError ("Unexpected: first character of path in map is neither '[', nor '/'.");
	MappingInfo info = (MappingInfo)mappedFiles.get(path);
	if (info != null)
	  {
	    if (info.addressLow == addressHigh
		|| info.addressHigh == addressLow)
	      {
		if (addressHigh > info.addressHigh)
		  info.addressHigh = addressHigh;
		if (addressLow < info.addressLow)
		  info.addressLow = addressLow;
		if (permRead) info.permRead = true;
		if (permWrite) info.permWrite = true;
		if (permExecute) info.permExecute = true;
	      }
	    else
	      throw new AssertionError("Non-continuous mapping.");
	  }
	else
	  mappedFiles.put(path, new MappingInfo(path, addressLow, addressHigh,
						permRead, permWrite, permExecute));
      }
  }

  public static Map forPid (int pid)
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

  public long entryAddress;
  public long pltAddress;

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

  public void setEntryAddress(long address) {
    this.entryAddress = address;
  }

  public void setPltAddress(long address) {
    this.pltAddress = address;
  }
}

class LtraceObjectFile
{
  public HashMap symbolMap = new HashMap();
  private String fileName;
  private String soname = null;
  private long baseAddress = 0;
  private static HashMap cachedFiles = new HashMap();

  public interface SymbolIterator {
    void symbol(LtraceSymbol symbol);
  }

  private LtraceObjectFile (String name)
  {
    this.fileName = name;
  }

  public void addSymbol(LtraceSymbol symbol)
  {
    symbolMap.put(symbol.name, symbol);
    symbol.addedTo(this);
  }

  /*
  public LtraceSymbol symbolAt(final long address)
  {
    // XXX: Huh, this is ugly.  Eventually either invent some observer
    // mechanism where ObjectFile can cache observed symbol addresses,
    // or, if it turns out that address almost never changes, make it
    // final or something.
    final LinkedList list = new LinkedList();
    this.eachSymbol(new SymbolIterator(){
	public void symbol(LtraceSymbol symbol) {
	  if (symbol.entryAddress == address)
	    list.add(symbol);
	}
      });

    if (list.isEmpty())
      return null;
    if (list.size() > 1)
      System.err.println("Strange: symbolAt(0x" + Long.toHexString(address) + ") has more than one symbol...");
    return (LtraceSymbol)list.getFirst();
  }
  */

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

  public void setSoname(String soname)
  {
    this.soname = soname;
  }

  /** Either answer preset soname, or construct soname from filename. */
  public String getSoname()
  {
    if (this.soname != null)
      return this.soname;
    else
      return new File(this.fileName).getName();
  }

  public void setBaseAddress(long baseAddress)
  {
    this.baseAddress = baseAddress;
  }

  public long getBaseAddress()
  {
    return this.baseAddress;
  }

  private static ElfSection getElfSectionWithAddr(Elf elfFile, long addr)
  {
    for (ElfSection section = elfFile.getSection(0);
	 section != null;
	 section = elfFile.getNextSection(section))
      {
	ElfSectionHeader sheader = section.getSectionHeader();
	if (sheader.addr == addr)
	  return section;
      }
    return null;
  }

  public static LtraceObjectFile buildFromFile(String fileName)
  {
    {
      LtraceObjectFile objFile = (LtraceObjectFile)cachedFiles.get(fileName);
      if (objFile != null)
	return objFile;
    }

    try {
	final Elf elfFile = new Elf(fileName, ElfCommand.ELF_C_READ);
	if (elfFile == null)
	  return null;

	final ElfEHeader eh = elfFile.getEHeader();
	if (eh == null)
	  return null;

	boolean haveDynamic = false;
	boolean haveLoadable = false;
	boolean havePlt = false;
	boolean haveRelPlt = false;
	long offDynamic = 0;
	long baseAddress = 0;

	for (int i = 0; i < eh.phnum; ++i)
	  {
	    ElfPHeader ph = elfFile.getPHeader(i);
	    if (ph.type == ElfPHeader.PTYPE_DYNAMIC)
	      {
		haveDynamic = true;
		offDynamic = ph.offset;
	      }
	    else if (ph.type == ElfPHeader.PTYPE_LOAD
		     && ph.offset == 0)
	      {
		haveLoadable = true;
		baseAddress = ph.vaddr;
	      }
	  }

	if (!haveDynamic)
	  {
	    System.err.println("This file doesn't participate in dynamic linking.");
	    return null;
	  }
	if (!haveLoadable)
	  {
	    System.err.println("This file doesn't contain any loadable segments.");
	    return null;
	  }

	final LtraceObjectFile objFile = new LtraceObjectFile(fileName);
	objFile.setBaseAddress(baseAddress);

	if (eh.type == ElfEHeader.PHEADER_ET_EXEC)
	  System.err.println("Executable");
	else if (eh.type == ElfEHeader.PHEADER_ET_DYN)
	  System.err.println("DSO/PIE");
	else
	  System.err.println("Unknown ELF type " + eh.type + "!");

	class Locals {
	  public ElfSection dynamicStrtab = null;
	  public ElfSection dynamicSymtab = null;
	  public int dynamicSonameIdx = -1;
	  public long pltAddr = 0;
	  public long pltSize = 0;
	  public ElfRel[] pltRelocs = null;
	}
	final Locals locals = new Locals();
	haveDynamic = false;

	// Find & interpret DYNAMIC section.
	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section))
	  {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (sheader.offset == offDynamic)
	      {
		haveDynamic = true;
		ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
		    public void entry (int tag, long value)
		    {
		      if (tag == ElfDynamic.ELF_DT_STRTAB)
			locals.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_SONAME)
			locals.dynamicSonameIdx = (int)value;
		      else if (tag == ElfDynamic.ELF_DT_SYMTAB)
			locals.dynamicSymtab = getElfSectionWithAddr(elfFile, value);
		    }
		});
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_PROGBITS
		      || sheader.type == ElfSectionHeader.ELF_SHT_NOBITS)
		     && sheader.name.equals(".plt"))
	      {
		havePlt = true;
		locals.pltAddr = sheader.addr;
		locals.pltSize = sheader.size;
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_REL
		      && sheader.name.equals(".rel.plt"))
		     || (sheader.type == ElfSectionHeader.ELF_SHT_RELA
			 && sheader.name.equals(".rela.plt")))
	      {
		haveRelPlt = true;
		locals.pltRelocs = ElfRel.loadFrom(section);
	      }
	  }

	if (!haveDynamic)
	  throw new lib.dwfl.ElfFileException("DYNAMIC section not found in ELF file.");
	if (!havePlt)
	  throw new lib.dwfl.ElfFileException("No (suitable) .plt found in ELF file.");
	if (!haveRelPlt)
	  throw new lib.dwfl.ElfFileException("No (suitable) .rel.plt found in ELF file.");
	if (locals.dynamicSymtab == null)
	  throw new lib.dwfl.ElfFileException("Couldn't get SYMTAB from DYNAMIC section.");
	if (locals.dynamicStrtab == null)
	  throw new lib.dwfl.ElfFileException("Couldn't get STRTAB from DYNAMIC section.");

	// Load DT_SYMTAB.
	{
	  ElfSection section = locals.dynamicSymtab;
	  ElfSectionHeader sheader = section.getSectionHeader();
	  if (sheader.type != ElfSectionHeader.ELF_SHT_DYNSYM)
	    throw new lib.dwfl.ElfFileException("Section denoted by DT_SYMTAB isn't SHT_DYNSYM.");

	  final ArrayList symbolList = new ArrayList();
	  ElfSymbol.loadFrom(section, new ElfSymbol.Builder() {
	      public void symbol (String name, long value, long size,
				  int type, int bind, int visibility,
				  long shndx)
	      {
		LtraceSymbol sym = new LtraceSymbol(name, type, value, size, shndx);
		symbolList.add(sym);
		if (type == ElfSymbol.ELF_STT_FUNC)
		  {
		    sym.setEntryAddress(value);
		    objFile.addSymbol(sym);
		  }
	      }
	    });

	  long pltEntrySize = locals.pltSize / (locals.pltRelocs.length + 1);
	  for (int i = 0; i < locals.pltRelocs.length; ++i)
	    /* XXX HACK: 386 specific.  In general we want
	     * platform-independent way of asking whether it's
	     * JMP_SLOT relocation. */
	    if (locals.pltRelocs[i].type == 7)
	      {
		long pltAddress = locals.pltAddr + pltEntrySize * (i + 1);
		long symbolIndex = locals.pltRelocs[i].symbolIndex;
		LtraceSymbol symbol = (LtraceSymbol)symbolList.get((int)symbolIndex - 1);
		symbol.setPltAddress(pltAddress);
	      }
	}

	// Read SONAME if there was one.
	if (locals.dynamicSonameIdx != -1)
	  {
	    ElfData data = locals.dynamicStrtab.getData();
	    byte[] bytes = data.getBytes();
	    int startIndex = locals.dynamicSonameIdx;
	    int endIndex = startIndex;
	    while (bytes[endIndex] != 0)
	      ++endIndex;
	    String name = new String(bytes, startIndex, endIndex - startIndex);
	    objFile.setSoname(name);
	  }

	cachedFiles.put(fileName, objFile);
	return objFile;
      }
    catch (lib.dwfl.ElfFileException efe)
      {
	efe.printStackTrace();
	System.err.println("load error: " + efe);
      }
    catch (lib.dwfl.ElfException eexp)
      {
	eexp.printStackTrace();
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
   * Do a per-process init.  Called only when process task(s) exist(s)
   * and are attached to.
   */
  private void perProcInit(Proc proc)
  {
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
	  System.err.print("[" + task.getTaskId().intValue() + "] ");
	  System.err.println("syscall enter " + syscall.getName());
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
	    {
	      this.checkMapUnmapUpdates(task, false);
	      task.requestUnblock(this);
	      return Action.BLOCK;
	    }

	  if (traceSignals)
	    {
	      System.err.print ("[" + task.getTaskId().intValue() + "] ");
	      System.err.println ("syscall leave " + name);
	    }
	}

      return Action.CONTINUE;
    }



    // ------------------------------------------
    // --- code observer, breakpoint handling ---
    // ------------------------------------------

    // XXX: Following fields will have to be made task-aware.
    private HashMap breakpointMap = new HashMap();
    private HashMap returnBreakpoints = new HashMap();
    private int level = 0;

    public Action libcallEnter(Task task, LtraceSymbol symbol, long address)
    {
      /*
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
	      callerLibrary = LtraceObjectFile.buildFromFile(callerLibrary).getSoname();
	  }
	  catch (java.lang.Exception ex) {
	    callerLibrary = "(Exception)";
	  }
	}
      */

      String symbolName = symbol.name;
      String callerLibrary = symbol.parent.getSoname();

      StringBuffer spaces = new StringBuffer();
      for (int i = 0; i < level; ++i)
	spaces.append(' ');
      ++level;
      System.err.print("[" + task.getTaskId().intValue() + "] " + spaces + "call enter ");
      System.err.print(callerLibrary + "->" + /*libraryName + ":" +*/ symbolName + "(");

      ByteBuffer buf = task.getMemory();

      // i386 only at this time
      Register espRegister = task.getIsa().getRegisterByName("esp");
      long esp = espRegister.get(task);
      long retAddr = buf.getInt(esp);
      esp += 4;

      // Poor man's call formatter... both traditional ltrace-style
      // and dwarf-enhanced formatters will be implemented in future,
      // this is just to test some stuff...
      if (symbolName.equals("puts"))
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
	for (long i = 0; i < 4; ++i)
	  {
	    int value = buf.getInt(esp);
	    System.err.print("0x" + Long.toHexString(value) + ", ");
	    esp += 4;
	  }
      System.err.println(")");

      // Install breakpoint to return address.
      task.getProc().getMainTask().requestAddCodeObserver(this, retAddr);
      returnBreakpoints.put(new Long(retAddr), symbol);

      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public Action libcallLeave(Task task, LtraceSymbol symbol, long address)
    {
      StringBuffer spaces = new StringBuffer();
      --level;
      for (int i = 0; i < level; ++i)
	spaces.append(' ');
      System.err.println("[" + task.getTaskId().intValue() + "] " + spaces + "call leave " + symbol.name);
      task.getProc().getMainTask().requestDeleteCodeObserver(this, address);
      returnBreakpoints.remove(new Long(address));

      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public Action updateHit (Task task, long address)
    {
      Long laddress = new Long(address);
      LtraceSymbol symbol = (LtraceSymbol)breakpointMap.get(laddress);
      if (symbol != null)
	return this.libcallEnter(task, symbol, address);

      // XXX: This will have to take into account the fact that all
      // tasks will hit breakpoint, not only the original.
      symbol = (LtraceSymbol)returnBreakpoints.get(laddress);
      if (symbol != null)
        return this.libcallLeave(task, symbol, address);

      System.err.println("[" + task.getTaskId().intValue() + "] UNKNOWN BREAKPOINT 0x" + Long.toHexString(address));
      returnBreakpoints.remove(laddress);
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

      this.mapsForTask.put(task, new HashMap());
      this.checkMapUnmapUpdates(task, false);

      return Action.CONTINUE;
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
	: MemMappedFiles.forPid(task.getTid());

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
		  MappingInfo info = (MappingInfo)newMappedFiles.get(path);
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
		  MappingInfo info = (MappingInfo)mappedFiles.get(path);
		  this.updateUnmappedFile(task, info);
		}
	    }
	}

      this.mapsForTask.put(task, newMappedFiles);
    }

    private void reportMapUnmap(Task task, String filename, String method)
    {
      int pid = task.getTid();
      System.err.println ("[" + pid + "]"
			  + " " + method
			  + " " + filename);
    }

    public void updateMappedFile (final Task task, MappingInfo mapping)
    {
      this.reportMapUnmap(task, mapping.path, "map");

      // Try to load the mappped file as ELF.  Assume all
      // executable-mmapped ELF files are libraries.
      if (!mapping.permExecute)
	return;
      LtraceObjectFile objf = LtraceObjectFile.buildFromFile(mapping.path);
      if (objf == null)
	{
	  int pid = task.getTid();
	  System.err.println("[" + pid + "] note mmap non-elf " + mapping.path);
	}
      else
	{
	  // XXX: frysk doesn't currently handle the "once per Task"
    	  // part right, and triggers each breakpoint for each task,
    	  // so the tracing get's somewhat useless for multithreading.
	  final long relocation = mapping.addressLow - objf.getBaseAddress();
	  objf.eachSymbol(new LtraceObjectFile.SymbolIterator() {
	      public void symbol(LtraceSymbol sym)
	      {
		if (sym.pltAddress == 0)
		  return;

		Long laddr = new Long(sym.pltAddress + relocation);
		if (breakpointMap.containsKey(laddr))
		  {
		    // We got an alias.  Put the symbol with the
    		    // shorter name into the map.
		    //
		    // XXX: In future, for trace pruning/symbol
		    // matching purposes, all aliases should be
		    // available.  By default the shortest one should
		    // get printed, but any of them should match.
		    LtraceSymbol original = (LtraceSymbol)breakpointMap.get(laddr);
		    if (sym.name.length() < original.name.length())
		      breakpointMap.put(laddr, sym);
		  }
		else
		  {
		    task.requestAddCodeObserver(ltraceTaskObserver, laddr.longValue());
		    breakpointMap.put(laddr, sym);
		  }
	      }
	    });
	}
    }

    public void updateUnmappedFile (Task task, MappingInfo mapping)
    {
      this.reportMapUnmap(task, mapping.path, "unmap");
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
