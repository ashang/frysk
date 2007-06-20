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

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import frysk.proc.Task;
import frysk.rt.DisplayValue;
import frysk.rt.DisplayValueObserver;
import frysk.rt.SteppingEngine;
import frysk.rt.UpdatingDisplayValue;
import frysk.stack.FrameIdentifier;
import frysk.value.Value;

/**
 * The display command is used to create a display on an expression. 
 * The syntax for this command should be "display <expression>". Whenever
 * the value of the expression changes, the user will be notified.
 *
 */
public class DisplayCommand
    extends CLIHandler
{

  private List displays;
  
  private static final String desc = "creates a display on an expression";
  
  public DisplayCommand(String name, CLI cli)
  {
    super(name, cli, new CommandHelp(name, desc, "display expr", desc));
    displays = new LinkedList();
  }
  
  public DisplayCommand(CLI cli)
  {
    this("display", cli);
  }
  
  public void handle (Command cmd) throws ParseException
  {
    final PrintWriter output = cli.getPrintWriter();
    cli.refreshSymtab();
    
    ArrayList args = cmd.getParameters();
    if(args.size() > 1)
      throw new ParseException("Too many arguments to display", 0);
    if(args.size() == 0)
      throw new ParseException("Too few arguments to display", 0);
    
    /* if cli.debugInfo is null, that probably means that we're not attached
     * to anything
     */
    if(cli.debugInfo == null)
      {
        output.write("Not attached to any task, nothing to display\n");
        output.flush();
        return;
      }
    
    SteppingEngine engine = cli.getSteppingEngine();
    Task myTask = cli.getTask();
    FrameIdentifier fIdent = cli.debugInfo.getCurrentFrame().getFrameIdentifier();
    
    UpdatingDisplayValue uDisp = new UpdatingDisplayValue((String) args.get(0),
                                                          myTask,
                                                          fIdent,
                                                          engine);
    if(!displays.contains(uDisp))
      {
        displays.add(uDisp);
        uDisp.addObserver(new DisplayValueObserver()
        {
          public void updateValueChanged (DisplayValue value)
          {
            output.println(value.getName() + " = " + value.getValue());
            output.flush();
          }
    
          public void updateUnavailbeResumedExecution (DisplayValue value) {}
          public void updateAvailableTaskStopped (DisplayValue value) {}
        });
      }
    
    Value v = uDisp.getValue();
    if(v == null)
      output.println(args.get(0) + " is unavailable");

    else
      output.println(v.getText() + " = " + v);
    output.flush();
  }

}