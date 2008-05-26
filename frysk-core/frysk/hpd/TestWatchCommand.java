// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.hpd;

import frysk.config.Prefix;
import frysk.isa.ISA;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtEntry;

/**
* This class tests the "watch" command.
*/
public class TestWatchCommand extends TestLib {

  public void testWatchPointSetAndHit()
  {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-watchpoint").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");
      
      e.send("watch source\n"); 
      e.expect(".*Watchpoint set: source.*");
      
      e.send("go\n"); 
      e.expect(".*Watchpoint hit: source.*Value before hit ="
  	        + ".*Value after  hit =.*");

      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();
  }

  public void testMultipleWatchPointSetAndHit()
  {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-watchpoint").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");

      e.send("watch source\n"); 
      e.expect(".*Watchpoint set: source.*");
      e.send("watch read_only -a\n"); 
      e.expect(".*Watchpoint set: read_only.*");
      
      e.send("go\n"); 
      e.expect(".*Watchpoint hit: source.*Value before hit ="
	      + ".*Value after  hit =.*");

      e.send("go\n"); 
      e.expect(".*Watchpoint hit: read_only.*Value before hit ="
	      + ".*Value after  hit =.*");

      e.send("go\n"); 
      e.expect(".*Task " + "[0-9]+" + " is exiting with status " 
	       + "[0-9]+");
      
      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();
  }  
  
  /*
   * Test to watch a variable that requires multiple
   * watchpoints on IA32.
   */
  public void testWatchLongLong()
  {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-ctypes").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");
      
      e.send("watch long_long\n"); 
      e.expect(".*Watchpoint set: long_long.*");
      
      e.send("go\n"); 
      e.expect(".*Watchpoint hit: long_long.*Value before hit ="
  	        + ".*Value after  hit =.*");

      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();
  }  
  
  /*
   * Test to watch a data type whose size is larger than
   * that can be watched by all hardware watch registers
   * put together.
   */
  public void testWatchOversized()
  {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-ctypes").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");
      
      e.send("watch bigArray\n"); 
      e.expect(".*Watch error: Available watchpoints not sufficient " +
      	       "to watch complete value..*");
      
      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();
  }   
  
  /*
   * Test to watch a variable that is smaller than the max
   * size a single watch register can watch.
   */
  public void testUndersized() {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-ctypes").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");
      
      e.send("watch char_\n"); 
      e.expect(".*Watchpoint set: char_.*");
      
      e.send("go\n"); 
      e.expect(".*Watchpoint hit: char_.*Value before hit ="
  	        + ".*Value after  hit =.*");

      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();     
  }
  
  /*
   * Test for watching expressions with no legal addresses.
   */
  public void testBadWatch() {
      e = new HpdTestbed();
      e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-ctypes").getPath(),
                                "Loaded executable file.*");
      e.sendCommandExpectPrompt("start", "Attached to process.*");

      Task task = getStoppedTask();
      ISA isa = task.getISA();

      if (isa == ISA.IA32) {
	  e.send("watch $eax\n"); 
      } else if (isa == ISA.X8664) {
	  e.send("watch $rdi\n");
      }
      e.expect("Error: Location not in contiguous memory.*");
      
      e.send("quit\n");
      e.expect("Quitting\\.\\.\\.");
      e.close();     
  }

  
  private Task getStoppedTask() {
      return this.getStoppedTask("funit-ctypes");
  }

  private Task getStoppedTask (String process) {
      DaemonBlockedAtEntry daemon = new DaemonBlockedAtEntry 
                                    (new String[] { getExecPath(process) });
      
      return daemon.getMainTask();
  }  

}