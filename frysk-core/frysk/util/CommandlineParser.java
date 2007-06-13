// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;

import gnu.classpath.tools.getopt.Parser;
import frysk.Config;
import frysk.EventLogger;
import frysk.proc.ProcId;

/**
 * CommandlineParser extends the getopt {@link Parser} class with common
 * options for Frysk command-line applications. It adds the
 * {@link EventLogger} options.
 */
public class CommandlineParser
  extends Parser
{
  public CommandlineParser(String name)
  {
    super(name, Config.getVersion(), true);
    EventLogger.addConsoleOptions(this);
  }
  
  /**
   * Callback function. Gives an array of pids if pids were detected on the 
   * command line.
   * @param pids The array of pids passed on the command line.
   */
  public void parsePids(ProcId[] pids)
  {
    throw new RuntimeException("pids not supported.");
  }
  
  /**
   * Callback function. Gives an array of core files if core files were 
   * detected on the command line.
   * @param coreFiles The array of core files passed on the command line.
   */
  public void parseCores(File[] coreFiles)
  {
    throw new RuntimeException("cores not supported.");
  }
  
  /**
   * Callback function. Gives a string array represented a parsed command.
   * @param command The parsed command.
   */
  public void parseCommand(String[] command)
  {
    throw new RuntimeException("commands not supported.");
  }
  
  public String[] parse(String[] args)
  {
    String[] result = super.parse(args);
    
    //XXX: Should parseCommand be called with an empty array here?
    if (result == null)
      return null;
    if (result.length == 0)
      return result;
    
    //Check if arguments are all pids.
    try 
    {
      ProcId[] pids = new ProcId[result.length];
      pids[0] = new ProcId(Integer.parseInt(result[0]));
      
      for (int i = 1; i < result.length; i++)
        {
          try
          {
          pids[i] = new ProcId(Integer.parseInt(result[i]));
          }
          catch (NumberFormatException e)
          {
            throw new RuntimeException("Please don't mix pids with core files or executables");
          }
        }
      
      parsePids(pids);
      return result;
    }
    catch (NumberFormatException e)
    {
      //Not a pid, continue on.
    }        
    
    // Check if arguments are all core files.
    if (isCoreFile(result[0]))
      {
        File[] coreFiles = new File[result.length];
        
        for (int file = 0; file < result.length; file++)
          if (isCoreFile(result[file]))
            coreFiles[file] = new File(result[file]);
          else
            throw new RuntimeException("Please don't mix core files with pids or executables.");
          
        
        parseCores(coreFiles);
        return result;
      }
      
    
    // If not above, then this is an executable command.
    parseCommand(result);
    return result;
  }
  
  /**
   * Check if the given file is a coreFile.
   * @param fileName A file to check.
   * @return if fileName is a corefile returns true, otherwise false.
   */
  private boolean isCoreFile (String fileName)
  {
    Elf elf;
    try
      {
        elf = new Elf(fileName, ElfCommand.ELF_C_READ);
      }
    catch (Exception e)
      {
        return false;
      }
    
    boolean ret = elf.getEHeader().type == ElfEHeader.PHEADER_ET_CORE;
    elf.close();
    return ret;

  }
}
    
