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

package frysk.hpd;

import inua.elf.AT;
import java.util.Iterator;
import java.util.List;
import frysk.proc.Auxv;
import frysk.proc.Proc;

public class AuxvCommand extends ParameterizedCommand {
  
  boolean verbose = false;
  
  public AuxvCommand() {
    super("Print process auxiliary", "auxv [-verbose]", 
	  "Print out the process auxiliary data for this "
	  + "process.");
    
    add(new CommandOption("verbose", "Print out known auxv descriptions ") {
	void parse(String argument, Object options) {
	  verbose = true;
	}
      });
    
  }
  
  void interpret(CLI cli, Input cmd, Object options) {
    PTSet ptset = cli.getCommandPTSet(cmd);
    Iterator taskDataIterator = ptset.getTaskData();
    if (taskDataIterator.hasNext() == false)
      cli.addMessage("Cannot find main task. Cannot print out auxv", Message.TYPE_ERROR);
    Proc mainProc = ((TaskData) taskDataIterator.next()).getTask().getProc();
    Auxv[] liveAux = mainProc.getAuxv();
    
    class BuildAuxv extends AuxvStringBuilder {
      
      public StringBuffer auxvData = new StringBuffer();
      public void buildLine(String type, String desc, String value) {
	if (verbose)
	  auxvData.append(type+" (" + desc+") : " + value+"\n");
	else
	  auxvData.append(type+" : " + value+"\n");	
      }
    }
    
    BuildAuxv buildAuxv = new BuildAuxv();
    buildAuxv.construct(liveAux);
    
    cli.outWriter.println(buildAuxv.auxvData.toString());
  }
  
  int completer(CLI cli, Input input, int cursor, List completions) {
    return -1;
  }
  
  abstract class AuxvStringBuilder
  {
    protected AuxvStringBuilder() {
    }
    
    public final void construct (Auxv[] rawAuxv) {
      String  value;
      for (int i=0; i < rawAuxv.length; i++) {
	switch (rawAuxv[i].type) {
	case 33:
	case 16:
	case 3:
	case 9:
	case 15: 
	  value = "0x"+Long.toHexString(rawAuxv[i].val);
	  break;
	default: 
	  value = ""+rawAuxv[i].val;
	}    		  
	buildLine(AT.toString(rawAuxv[i].type), AT.toPrintString(rawAuxv[i].type), value);
      }
    }
    
    abstract public void buildLine(String type, String desc, String value);
  }
}
