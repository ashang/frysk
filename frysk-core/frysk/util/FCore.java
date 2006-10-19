//This file is part of the program FRYSK.

//Copyright 2006, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.util;



//import java.nio.ByteBuffer;
import inua.eio.ByteOrder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfData;
import lib.elf.ElfEHeader;
import lib.elf.ElfEMachine;
import lib.elf.ElfException;
import lib.elf.ElfFileException;
import lib.elf.ElfNhdr;
import lib.elf.ElfNhdrType;
import lib.elf.ElfPHeader;
import lib.elf.ElfPrpsinfo;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;
import lib.elf.ElfSectionHeaderTypes;
import frysk.EventLogger;
import frysk.event.RequestStopEvent;
import frysk.proc.Isa;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.proc.Stat;
import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

/**
 * @author pmuldoon
 *
 * XXX: WORK IN PROGRESS. NOT YET COMPLETED
 * 
 * FCore - Utility class to take a pid. Derive from that a Proc that is modelled in the core, stop all the tasks,
 * get the main tasks's maps, and each threads register/status and construct an elf core file. Caveats associated
 * at this time:
 * 
 * fcore ELF core file will be:
 * 
 * ELF Header
 * Program Segment Header
 * Segments * n.
 * 
 * The NOTES section of the core file, will contain several structures as defined as follows:
 * 
 * Structure: 
 * 
 * elf_prpsinfo (one per process)
 * elf_prstatus (one per thread, main task first)
 * elf_fpregset_t (one per thread, regardless of whether fp used)
 * elf_xfpregset_t (as above, per 64 bit).
 * 
 */
public class FCore {
	private Proc proc;

	private Elf local_elf = null;

	public ProcAttachedObserver procAttachedObserver;

	int i = 1;

	Task[] taskArray;

	
	private static Parser parser;

	private static String levelValue;
	private static Level level;
	protected static final Logger logger = EventLogger.get("logs/",
	"frysk_core_event.log");
	private static int pid = 0;
	
	public void run(final int pid) {

		// Start refreshing
		Manager.host.requestRefreshXXX(true);

		// Wait here until refresh is done. It will take n amount of time to
		// finish.The amount of time is undetermined.
		Manager.eventLoop.runPending();

		// Get the requested pid, and return a Proc object.
		proc = Manager.host.getProc(new ProcId(pid));

		if (proc == null) {
			System.err.println("Couldn't get the process " + pid
					+ ". It might have disappeared.");
			System.exit(-1);
		}

		// Attach to the proc, and when tasks stopped, do 
		// core dump.
		procAttachedObserver = new ProcAttachedObserver(proc,
				new CoreDumpTasksObserver(proc));

		// Start the event loop, not pending events.
		Manager.eventLoop.start();
	}

	private class CoreDumpTasksObserver implements ProcObserver.ProcTasks {
		private LinkedList taskList;

		public CoreDumpTasksObserver(final Proc proc) {
			taskList = proc.getTasks();
			taskArray = new Task[taskList.size()];
		}

		public void existingTask(final Task task) {
			if (proc.getMainTask() == task) {
				taskArray[0] = task;
			} else {
				taskArray[i] = task;
				i++;
			}

			// Remove this task from the list of tasks.
			if (taskList.contains(task)) {
				taskList.remove(task);
			}

			if (taskList.size() == 0) {

				try {
					write_elf_file(taskArray, proc);
				} catch (final ElfFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final ElfException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final TaskException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Have all the tasks.
				removeObservers(task.getProc());
			}
		}

		public void taskAdded(final Task task) {
		}

		public void taskRemoved(final Task task) {

		}

		public void addFailed(final Object observable, final Throwable w) {
			System.err.println(w);
			Manager.eventLoop.requestStop();
			System.exit(2);
		}

		public void addedTo(final Object observable) {
		}

		public void deletedFrom(final Object observable) {
		}

	}

	private final void requestDeletes(final Proc proc) {
		final Iterator iter = proc.getTasks().iterator();
		while (iter.hasNext()) {
			((Task) iter.next())
					.requestDeleteAttachedObserver(procAttachedObserver);
		}
	}

	private final void removeObservers(final Proc proc) {
		requestDeletes(proc);
		proc.observableDetached.addObserver(new Observer() {

			public void update(final Observable o, final Object arg) {
				Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
			}
		});
	}


    /**
     * Fill the ElfNhdr object according to Proc object.
     * 
     * @param nhdrEntry
     * @param proc
     * @return less than zero when error occurs, or return one value 
     *         that is equal to zero or more than zero.
     */
    protected int fillENotePrpsinfo(ElfNhdr nhdrEntry, Proc proc)
    {
      //XXX: fill the elf_prpsinfo here.
      int pid = proc.getPid();
      
      ElfPrpsinfo prpsInfo = new ElfPrpsinfo();
      
      Stat processStat = new Stat();
      
      processStat.refresh();
      
      prpsInfo.setPrState(processStat.state);
      prpsInfo.setPrSname(processStat.state);
      
      //prpsInfo.setPrZomb(processStat.zero);
      //prpsInfo.setPrNice(processStat.nice);
      
      prpsInfo.setPrFlag(processStat.flags);
      //prpsInfo.setPrUid(Status.getUID(pid));
      //prpsInfo.setPrGid(Status.getGID(pid));
      
      // For some reason Stat does not work here in all
      // cases, but proc.getUID/GID does.
      
      prpsInfo.setPrUid(proc.getUID());
      prpsInfo.setPrGid(proc.getGID());
      
      prpsInfo.setPrPid(pid);
      
      //prpsInfo.setPrPpid(processStat.ppid);
      
      // Stat problem same here. Stat produces 0 here. Ask proc
      // for it's parent.
      prpsInfo.setPrPpid(proc.getParent().getPid());
      prpsInfo.setPrPgrp(processStat.pgrp);
      
      prpsInfo.setPrSid(processStat.session);
      
      // Stat problem same here. Stat seems broken :/
      // Use proc.getCommand for now.
      prpsInfo.setPrFname(proc.getCommand());

      //prpsInfo.setPrFname(processStat.comm);
      
      class BuildCmdLine extends CmdLineBuilder
      {
          byte[] args;
          public void buildBuffer (byte[] buf)
          {
            args = buf;  
          }
          public void buildArgv (String[] argv)
          {
            //Do nothing.
          }
      }
      BuildCmdLine cmdLine = new BuildCmdLine();
      cmdLine.construct(pid);
      prpsInfo.setPrPsargs(new String(cmdLine.args).toString());
      
      nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRPSINFO, prpsInfo);
      return 0;
    }
    
    /**
     * Transform all information carried by list into ElfData object.
     * 
     * @param noteSectionData
     * @param list ElfNhdr list.
     * @return the number of invalid ElfNhdr objects.
     */
    protected int constructSectionData(ElfData noteSectionData, List nhdrList)
    {
      int size = 0;
      
      long secSize = 0;
      long entrySize = 0;
      
      size = nhdrList.size();
      if (size <= 0)
        return 0;
      
      // Count the size of the whole PT_NOTE section.
      for (int index = 0; index < size; index++)
        {
          ElfNhdr entry = (ElfNhdr)nhdrList.get(index);
          
          entrySize = entry.getNhdrEntrySize();
          if (entrySize <= 0)
            {
              //One invalid entry, ignore it.
              nhdrList.remove(index);
              size--;
              index--;
              continue;
            }
          
          secSize += entrySize;
        }
      //XXX: in the operation "new byte[count]", count must be "int". 
      // If secSize is bigger than the max of "int' type, how can we do?
      byte[] noteSecBuffer = new byte[(int)secSize];
      long startAddress = 0;
      
      // Begin to fill the noteSection memory region.
      size = nhdrList.size();
      for (int index = 0; index < size; index++)
        {
          ElfNhdr entry = (ElfNhdr)nhdrList.get(index);
          
          entry.fillMemRegion(noteSecBuffer, startAddress);
          
          startAddress += entry.getNhdrEntrySize();
        }
      
      noteSectionData.setBuffer(noteSecBuffer);
      noteSectionData.setSize(noteSecBuffer.length);
      
      return size;
    }
    
    public void fillENoteSection(ElfSection noteSection)
    {
      int ret = -1;
      int entryCount = 0;
      
      ArrayList list = new ArrayList();
      
      ElfNhdr prpsinfoNhdr = new ElfNhdr();
      ret = this.fillENotePrpsinfo(prpsinfoNhdr, this.proc);
      if (ret >= 0)
        {
          // Fill PRPSINFO correctly.
          list.add(entryCount, prpsinfoNhdr);
          entryCount++;
        }
      
      //XXX: Continue to fill other ElfNhdr object, such as NT_PRSTATUS info.
      // ElfNhdr psstatusNhdr = new ...
      
      if (list.size() <= 0)
        return;
      
      ElfData sectionData = noteSection.createNewElfData();
      constructSectionData(sectionData, list);
      sectionData.setType(0);
    }
    
  private void write_elf_file (final Task[] tasks, final Proc proc)
      throws ElfFileException, ElfException, TaskException
  {

    // Start new elf file
    local_elf = new Elf(System.getProperty("user.dir") + "/fcore."
                        + proc.getPid(), ElfCommand.ELF_C_WRITE, true);

    Isa arch = proc.getMainTask().getIsa();
    ByteOrder order = arch.getByteOrder();

    // Create the elf header
    local_elf.createNewEHeader(arch.getWordSize());
    ElfEHeader elf_header = local_elf.getEHeader();

    if (order == inua.eio.ByteOrder.BIG_ENDIAN)
      elf_header.ident[5] = ElfEHeader.PHEADER_ELFDATA2MSB;
    else
      elf_header.ident[5] = ElfEHeader.PHEADER_ELFDATA2LSB;

    // Version
    elf_header.ident[6] = (byte) local_elf.getElfVersion();

    // EXEC for now, ET_CORE later
    elf_header.type = ElfEHeader.PHEADER_ET_EXEC;

    // Version
    elf_header.version = local_elf.getElfVersion();

    // String Index
    elf_header.shstrndx = 1;

    // XXX: I hate this, there must be a better way to get architecture than
    // this ugly, ugly hack
    String arch_test = arch.toString();
    String type = arch_test.substring(0, arch_test.lastIndexOf("@"));

    if (type.equals("frysk.proc.LinuxIa32"))
      {
        elf_header.machine = ElfEMachine.EM_386;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS32;
      }
    if (type.equals("frysk.proc.LinuxPPC64"))
      {
        elf_header.machine = ElfEMachine.EM_PPC64;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS64;
      }
    if (type.equals("frysk.proc.LinuxX8664"))
      {
        elf_header.machine = ElfEMachine.EM_X86_64;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS64;
      }

    local_elf.updateEHeader(elf_header);

    // Dump out PT_NOTE
    ElfSection noteSection = local_elf.createNewSection();
    ElfSectionHeader noteSectHeader = noteSection.getSectionHeader();
    ElfPHeader noteProgramHeader = null;

    // Count maps
    final MapsCounter counter = new MapsCounter();
    counter.construct(proc.getMainTask().getTid());

    // Build initial Program segment header including PT_NOTE program header.
    local_elf.createNewPHeader(counter.numOfMaps + 1);
    System.out.println("XXX: UNCOMPLETED, maps not written. Number of writeable maps: "
                       + counter.numOfMaps);

    this.fillENoteSection(noteSection);
    // Modify PT_NOTE section header
    noteSectHeader.type = ElfSectionHeaderTypes.SHTYPE_PROGBITS;
    noteSectHeader.flags = ElfSectionHeaderTypes.SHFLAG_ALLOC;
    noteSectHeader.nameAsNum = 16;
    // XXX: Must fix the value of offset! Maybe we neednot set it. when dumping
    // out,
    // the elfutils will modify its value according the ELF object.
    noteSectHeader.offset = 0;
    noteSectHeader.size = noteSection.getData().getSize();
    noteSection.update(noteSectHeader);

    // Must first ask libelf to construct offset location before
    // adding offset back to program header. Otherwise program offset
    // will be 0.
    if (local_elf.update(ElfCommand.ELF_C_NULL) < 0)
      {
        throw new ElfException("Cannot calculate note section offset");
      }

    // Then re-fetch the elf modified header from section. Now offset
    // is calculated and correct.
    noteSectHeader = noteSection.getSectionHeader();

    // Modify PT_NOTE program header
    noteProgramHeader = local_elf.getPHeader(0);
    noteProgramHeader.type = ElfPHeader.PTYPE_NOTE;

    noteProgramHeader.offset = noteSectHeader.offset;
    noteProgramHeader.filesz = noteSectHeader.size;
    local_elf.updatePHeader(0, noteProgramHeader);

    final CoreMapsBuilder builder = new CoreMapsBuilder();
    builder.construct(proc.getMainTask().getTid());

    // Make a static string table. XXX: Should be dynamically generated in the
    // future?
    // even though we know that core string tables are always the same thing.
    String a = "\0" + "load" + "\0" + ".shstrtab" + "\0" + "note" + "\0";
    byte[] bytes = a.getBytes();

    // Create a very small static string section. This is needed as the actual
    // program segment data
    // needs to be placed into Elf_Data, and that is section function, and
    // therefore needs a section table and
    // a section string table. This is how gcore does it (and the only way using
    // libelf).
    //
    // The kernel just dumps the segments right after the segment table. We
    // *might* do
    // that in the future, but for right now, let's do it as much as possible
    // within libelf.

    // Sections need a string lookup table
    ElfSection stringSection = local_elf.createNewSection();
    ElfData data = stringSection.createNewElfData();
    ElfSectionHeader stringSectionHeader = stringSection.getSectionHeader();
    stringSectionHeader.type = ElfSectionHeaderTypes.SHTYPE_STRTAB;
    stringSectionHeader.nameAsNum = 6; // offset of .shrstrtab;

    // Set elf data
    data.setBuffer(bytes);
    data.setSize(bytes.length);
    // Update the section table back to elf structures.
    stringSection.update(stringSectionHeader);

    // Repoint shstrndx to string segment number
    elf_header = local_elf.getEHeader();
    elf_header.shstrndx = (int) stringSection.getIndex();
    local_elf.updateEHeader(elf_header);

    final long i = local_elf.update(ElfCommand.ELF_C_WRITE);
    if (i < 0)
      {
        throw new ElfException("LibElf elf_update failed with "
                               + local_elf.getLastErrorMsg());
      }
    local_elf.close();
    local_elf = null;

    // Ugly post process. Libelf will not let us write program segment when
    // class type is
    // set to ET_CORE. Bit confusing. SeeSee bz sw 3373.

    // Open the file
    boolean postProcess = postProcessElfFile(System.getProperty("user.dir")
                                             + "/fcore." + proc.getPid());
    if (! postProcess)
      {
        throw new ElfException(
                               "Could not post process elf core file. Stucj as ET_EXEC");
      }

  }

  private boolean postProcessElfFile (String file_string)
  {

    // temporary
    final int EI_NIDENT = 16;

    File elf_file = new File(file_string);
    if (! elf_file.canRead())
      return false;

    RandomAccessFile raf = null;
    try
      {
        raf = new RandomAccessFile(elf_file, "rw");
        raf.seek(EI_NIDENT);
        raf.write(ElfEHeader.PHEADER_ET_CORE);
        raf.close();
      }
    catch (IOException ec)
      {
        ec.printStackTrace();
        return false;
      }

    return true;
  }


	/**
   * Very basic Builder that forward counts the number of maps we have to
   * construct later, and dump to core.
   */
	class MapsCounter extends MapsBuilder {

		int numOfMaps = 0;

		public void buildBuffer(final byte[] maps) {
			maps[maps.length - 1] = 0;
		}

		public void buildMap(final long addressLow, final long addressHigh,
				final boolean permRead, final boolean permWrite, final boolean permExecute,
				final boolean permPrivate, final long offset, final int devMajor, final int devMinor,
				final int inode, final int pathnameOffset, final int pathnameLength) {
				if (permRead == true)
					numOfMaps++;
		}
	}

	/**
	 * 
	 * Core map file builder. Parse each map, build the section header.
	 * 
	 * Once section header is completed, copy the data from the mapped
	 * task memory according to the parameter from the builder:
	 * 
	 * addressLow -> addressHigh
	 * 
	 * and place in an Elf_Data section.
	 * 
	 */
	class CoreMapsBuilder extends MapsBuilder {

		int numOfMaps = 0;
		Elf elf;

		public void buildBuffer(final byte[] maps) {
			maps[maps.length - 1] = 0;
		}

		public void buildMap(final long addressLow, final long addressHigh,
				final boolean permRead, final boolean permWrite, final boolean permExecute,
				final boolean permPrivate, final long offset, final int devMajor, final int devMinor,
				final int inode, final int pathnameOffset, final int pathnameLength) {
			
			if (permRead == true) {
								
				// Get empty progam segment header corresponding to this entry.
                // PT_NOTE's program header entry takes the index: 0. So we should begin from 1.
				final ElfPHeader pheader = local_elf.getPHeader(numOfMaps + 1);
                pheader.offset = offset;
				pheader.type = ElfPHeader.PTYPE_LOAD;
				pheader.vaddr = addressLow;
				pheader.memsz = addressHigh - addressLow;
				pheader.flags = ElfPHeader.PHFLAG_NONE;
				
				// Set initial section flags (always ALLOC).
				long sectionFlags = ElfSectionHeaderTypes.SHFLAG_ALLOC; // SHF_ALLOC;

				// Build flags
				if (permRead == true)
					pheader.flags = pheader.flags | ElfPHeader.PHFLAG_READABLE;
            
				if (permWrite == true)
				{
					pheader.flags = pheader.flags | ElfPHeader.PHFLAG_WRITABLE;
					sectionFlags = sectionFlags |  ElfSectionHeaderTypes.SHFLAG_WRITE; 
				}
                
				if (permExecute == true)
				{
					pheader.flags = pheader.flags | ElfPHeader.PHFLAG_EXECUTABLE;
					sectionFlags = sectionFlags |  ElfSectionHeaderTypes.SHFLAG_EXECINSTR; 
				}

				// Construct file size, if any
				pheader.filesz = 0;
				if (ElfPHeader.PHFLAG_WRITABLE == (pheader.flags & ElfPHeader.PHFLAG_WRITABLE))
					pheader.filesz = pheader.memsz;
			
				// Update section header
				ElfSection section = local_elf.createNewSection();
				ElfSectionHeader sectionHeader = section.getSectionHeader();

				// sectionHeader.Name holds the string value. We also need to store the offset into the 
				// string table when we write data back;
				sectionHeader.nameAsNum = 1; // String offset of load string
				
				// Set the rest of the header
				sectionHeader.type = ElfSectionHeaderTypes.SHTYPE_PROGBITS;
				sectionHeader.flags = sectionFlags;
				sectionHeader.addr = pheader.vaddr;
				sectionHeader.offset = pheader.offset;
				sectionHeader.size = pheader.memsz;
				sectionHeader.link = 0;
				sectionHeader.info = 0;
				sectionHeader.addralign = 0;
				sectionHeader.entsize = 0;		

				// New data section
				ElfData data = section.createNewElfData();
				
				// Load data. How to fail here?
				byte[] memory = new byte[(int) (addressHigh-addressLow)];
				proc.getMainTask().getMemory().get(addressLow, memory, 0, (int) (addressHigh-addressLow));
				
				// Set and update back to native elf section
				data.setBuffer(memory);
				data.setSize(memory.length);
				
				// Fix this
				data.setType(0);
				section.update(sectionHeader);
				
				
				// inefficient to do this for each map, but alternative is to rerun another builder
				// so for right now, less of two evil. Needs a rethinks.
				final long i = local_elf.update(ElfCommand.ELF_C_NULL);
				if (i < 0)
					System.err.println("update in memory failed with message " + local_elf.getLastErrorMsg());
				sectionHeader = section.getSectionHeader();
				pheader.offset = sectionHeader.offset;
				pheader.align = sectionHeader.addralign;
				// Write back Segment header to elf structure
				local_elf.updatePHeader(numOfMaps + 1, pheader);
				numOfMaps++;
			}
			

		}
	}
	
	/**
	 * Entry function. Starts the fcore dump process.
	 * 
	 * Belongs in bindir/fcore. But here for now.
	 * 
	 * @param args - pid of the process to core dump
	 * 
	 */
	public static void main(String[] args) {

		System.out.println("XXX: Experimental do not use for core file generation.");
		// Parse command line. Check pid provided.
		parser = new Parser("fcore", "1.0", true) {
			protected void validate() throws OptionException {
				if (pid == 0) {
					throw new OptionException("no pid provided");
				}
			}
		};
		addOptions(parser);

		parser.setHeader("Usage: fcore <PID>");

		// XXX: should support > 1 pid, but for now, just one pid.
		parser.parse(args, new FileArgumentCallback() {
			public void notifyFile(String arg) throws OptionException {
				try {
					if (pid == 0) {
						pid = Integer.parseInt(arg);
					} else {
						throw new OptionException("too many pids");
					}

				} catch (Exception _) {
					throw new OptionException("couldn't parse pid");
				}
			}
		});

		// Set log level.
		if (levelValue != null) {
			logger.setLevel(level);
		}

		// Do core dump.
		FCore core_dumper = new FCore();
		core_dumper.run(pid);

	}

	/**
	 * Add options ot the the option parser.
	 * 
	 * Belongs in bindir/fcore but here for now
	 * 
	 * @param parser - the parser that is to be worked on.
	 * 
	 */
	private static void addOptions(Parser parser) {
		parser.add(new Option(
				"console",
				'c',
				"Set the console level. The console-level can be "
				+ "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
				"<console-level>") {
			public void parsed(String consoleValue)
			throws OptionException {
				try {
					Level consoleLevel = Level.parse(consoleValue);
					// Need to set both the console and the main logger
					// as
					// otherwize the console won't see the log messages.

					Handler consoleHandler = new ConsoleHandler();
					consoleHandler.setLevel(consoleLevel);
					logger.addHandler(consoleHandler);
					logger.setLevel(consoleLevel);

				} catch (IllegalArgumentException e) {
					throw new OptionException("Invalid log console: " + consoleValue);
				}

			}
		});
		parser.add(new Option(
				"level",
				'l',
				"Set the log level. The log-level can be "
				+ "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
				"<log-level>") {
			
			public void parsed(String logLevel) throws OptionException {
				levelValue = logLevel;
				try {
					level = Level.parse(levelValue);
					logger.setLevel(level);
				} catch (IllegalArgumentException e) {
					throw new OptionException("Invalid log level: "	+ levelValue);
				}
			}
		});
	}
	
}
