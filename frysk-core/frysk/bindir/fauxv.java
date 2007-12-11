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

package frysk.bindir;

import frysk.util.CommandlineParser;
import frysk.util.Util;
import frysk.util.AuxvStringBuilder;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Auxv;
import java.io.File;
import frysk.util.CoreExePair;

public class fauxv {
    
    public static void main (String[] args) {
	// Parse command line. Check pid provided.
	
	CommandlineParser parser = new CommandlineParser("fauxv") {
		
		public void parseCores (CoreExePair[] corePairs) {
		    for (int i = 0; i < corePairs.length; i++) {
			File coreFile = corePairs[i].coreFile;
			File exeFile = corePairs[i].exeFile;
			Proc proc;
			
			if (exeFile == null)
			    proc = Util.getProcFromCoreFile(coreFile);
			else
			    proc = Util.getProcFromCoreFile(coreFile, exeFile);
			printAuxv(proc.getAuxv());
		    }
		    
		    System.exit(0);
		}
		
		public void parsePids (ProcId[] pids) {
		    for (int i= 0; i< pids.length; i++) {
			ProcId id = pids[i];
			Proc proc = Util.getProcFromPid(id);
			printAuxv(proc.getAuxv());
		    }
		    System.exit(0);
		}
		
		private void printAuxv(Auxv[] auxv)
		{
		    
		    class BuildAuxv extends AuxvStringBuilder {
			
			public StringBuffer auxvData = new StringBuffer();
			public void buildLine(String type, String desc, String value) {
			    auxvData.append(type+" (" + desc+") : " + value+"\n");
			}
		    }
		    
		    BuildAuxv buildAuxv = new BuildAuxv();
		    buildAuxv.construct(auxv);
		    
		    System.out.println(buildAuxv.auxvData.toString());
		}
		
	    };
	
	parser.setHeader("Usage: fauxv <PID>  || fmaps <COREFILE> [<EXEFILE>]");
	parser.parse(args);
	
	//If we got here, we didn't find a pid.
	System.err.println("Error: No PID or COREFILE.");
	parser.printHelp();
	System.exit(1);
    }
}
