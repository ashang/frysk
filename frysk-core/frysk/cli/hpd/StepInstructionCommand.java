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

package frysk.cli.hpd;

import java.text.ParseException;
import java.util.ArrayList;

import frysk.rt.RemoteFrame;

public class StepInstructionCommand implements CommandHandler
{
  private CLI cli;
  
  StepInstructionCommand (CLI cli)
  {
    this.cli = cli;
  }
  
  public void handle(Command cmd) throws ParseException
  {
    ArrayList params = cmd.getParameters();
    if (params.size() == 1 && params.get(0).equals("-help"))
      {
        this.cli.printUsage(cmd);
        return;
      }
    
    this.cli.refreshSymtab();
    if (this.cli.steppingObserver != null)
      {
        if (!this.cli.isRunning())
          {
            this.cli.getSteppingEngine().stepInstruction(cli.proc.getTasks());
            
            synchronized (this.cli.steppingObserver.getMonitor())
              {
                try
                {
                  this.cli.steppingObserver.getMonitor().wait();
                }
                catch (InterruptedException ie) {}
              }
            
            RemoteFrame rf = (RemoteFrame) this.cli.debugInfo.getCurrentFrame();
            
            if (rf.getLines().length == 0)
              this.cli.addMessage("Task stopped at address 0x" + rf.getAdjustedAddress(), Message.TYPE_NORMAL);
            else
              this.cli.addMessage("Task stopped at line " + rf.getLines()[0].getLine() + " in file " + rf.getLines()[0].getFile(), Message.TYPE_NORMAL);
          }
        else
          this.cli.addMessage("Process is already running", Message.TYPE_ERROR);
      }
    else
      this.cli.addMessage("Not attached to any process", Message.TYPE_ERROR);
  }
}
