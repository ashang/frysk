// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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


package frysk.gui.monitor.filters;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.proc.Proc;

/**
 * @author mcvet
 */
public class ProcCommandLineFilter
    extends ProcFilter
{
  private String[] argsList;

  public ProcCommandLineFilter ()
  {
    super("Command line args of", "command line args");
  }

  public ProcCommandLineFilter (ProcCommandLineFilter other)
  {
    super(other);
    this.argsList = other.argsList;
  }

  public boolean filter (Proc proc)
  {
    String[] temp = proc.getCmdLine();
    int count = 0;
    
    if (temp.length != argsList.length + 1)
      return false;
    
    for (int i = 0; i < argsList.length; i++)
      {
        for (int j = 1; j < temp.length; j++)
          {
            System.out.println(argsList[i] + temp[j]);
            if (argsList[i].equals(temp[j]))
              {
                count++;
                break;
              }
          }
      }
    
    if (count == argsList.length)
      return true;
    else
      return false;
  }

  public GuiObject getCopy ()
  {
    return new ProcCommandLineFilter(this);
  }

  public boolean setArgument (String argument)
  {
    this.argsList = argument.split(" ");
    return true;
  }
  
  public boolean setArgument (String[] argument)
  {
    this.argsList = argument;
    return true;
  }

  public String getArgument ()
  {
    String temp = argsList[0];
    for (int i = 1; i < argsList.length; i++)
      temp = temp + " " + argsList[i];

    return temp;
  }

  public ObservableLinkedList getArgumentCompletionList ()
  {
    return null;
  }
}
