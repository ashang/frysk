// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.bindir;

import java.util.logging.Logger;
import frysk.event.ProcEvent;

import frysk.proc.Proc;
import frysk.proc.Task;

import frysk.util.ProcStopUtil;
import frysk.isa.corefiles.LinuxElfCorefile;
import frysk.isa.corefiles.LinuxElfCorefileFactory;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;


public class fcore
{
    private static String matchingRegEx = "";
    private static String filename = "core";  
    private static boolean writeAllMaps = false;

    private static int mapOptionCount = 0;
    protected static final Logger logger = Logger.getLogger("frysk");
  
    /**
     * Entry function. Starts the fcore dump process. Belongs in bindir/fcore. But
     * here for now.
     * 
     * @param args - pid of the process to core dump
     */
    public static void main (String[] args)
    {
	ProcStopUtil fcore = new ProcStopUtil("fcore", args, 
		                              new createCoreEvent());
	fcore.setUsage("Usage: fcore <PID>"); 
	addOptions (fcore);
	fcore.execute();
    }
  
    /**
     * Add options to fcore.
     */
    private static void addOptions (ProcStopUtil fcore)
    {

      fcore.addOption(new Option( "allmaps", 'a',
	                          " Writes all readable maps. Does not elide"
	                        + " or omit any readable map. Caution: could"
	                        + " take considerable amount of time to"
	                        + " construct core file.")
	  {
	      public void parsed (String mapsValue) throws OptionException {
		  try {
		      writeAllMaps = true;
		      mapOptionCount++;
		  } catch (IllegalArgumentException e) {
		      throw new OptionException("Invalid maps parameter " + mapsValue);
		  }
		  
	      }
	  });
      
      fcore.addOption(new Option("segments", 's',
				 "Define what segments to include via regex.",
				 "RegEx") {
	      public void parsed(String regEx) throws OptionException {
		  try {
		      mapOptionCount++;
		      matchingRegEx = regEx;
                    } catch (IllegalArgumentException e) {
		      throw new OptionException("Invalid match parameter "
						+ matchingRegEx);
		  }
	      }
	  });
      
      

      fcore.addOption(new Option( "outputfile", 'o',
	                          " Sets the name (not extension) of the core"
	                        + " file. Default is core.{pid}. The extension"
				  + " will always be the pid.", "<filename>")
	  {
	      public void parsed (String filenameValue) throws OptionException {
		  try {
		  filename = filenameValue;
		  }
		  catch (IllegalArgumentException e) {
		      throw new OptionException(  "Invalid output filename: "
						  + filenameValue);
		  }
	      }
	  });
    }
    
    /**
     * Implements a ProcEvent for core file creation.
     */
    private static class createCoreEvent implements ProcEvent
    {
	public void executeLive(Proc proc) {
	    
	    
	  
	    if (mapOptionCount > 1)
		System.err.println("Please either speciy -stackonly,"+
		" -allmaps, or -match <pattern> for map writing.");
	    else {
		Task[] tasks = (Task[]) proc.getTasks().toArray
	                   (new Task[proc.getTasks().size()]);
		LinuxElfCorefile coreFile = LinuxElfCorefileFactory.
		    getCorefile(proc, tasks);
		
		if (coreFile == null) {
		    System.err.println (  "Architecture not supported or "
					  + "LinuxElfCorefileFactory returned null");
		} else {
		    coreFile.setName(filename);
		    coreFile.setWriteAllMaps(writeAllMaps);
		    coreFile.setPatternMatch(matchingRegEx);
		    
		    try {
		    coreFile.constructCorefile();
		    } catch (RuntimeException e) {
			System.err.println (  "Architecture not supported or "
					      + "LinuxElfCorefileFactory returned null");		    
		    }
		}
	    }
	}
	
	public void executeDead(Proc proc) {
	    System.err.println ("Cannot create core file from dead process");
	}
    }
}
