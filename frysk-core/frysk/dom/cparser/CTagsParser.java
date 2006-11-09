// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
/**
 * The CTagsParser attemps to do a basic parser of a C/C++ source file by using
 * ctags to generate the information
 */
package frysk.dom.cparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.dom.StaticParser;
import frysk.dom.DOMFrysk;


/**
 * @author ajocksch
 *
 */
public class CTagsParser implements StaticParser {
	
	/* (non-Javadoc)
	 * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String, com.redhat.fedora.frysk.gui.srcwin.SourceBuffer)
	 */
	public void parse(DOMFrysk dom, DOMSource source, DOMImage image) 
      throws IOException {
		String[] command = new String[7];
		command[0] = "ctags";
		command[1] = "--fields=+KSn";
		command[2] = "-uV";
		command[3] = "--c-kinds=+lxp";
		command[4] = "--file-scope=yes";
		command[5] = "-f "+new File(".").getCanonicalPath()+"/tags";
		command[6] = source.getFilePath() + "/" + source.getFileName();
		
		Runtime run = Runtime.getRuntime();
		
		Process proc = run.exec(command);
		
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		BufferedReader reader = null;
		try {
			File f = new File(".");
			reader = new BufferedReader(new FileReader(new File(f.getCanonicalPath()+"/tags")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		String line = reader.readLine();
		
		while(line != null && line.charAt(0) == '!')
			line = reader.readLine();
		
		while(line != null){
			// Get information from tags file
//			String[] parts = line.split("\t");
//			String name = parts[0];
//			String type = parts[3];
//			int lineNum = Integer.parseInt(parts[4].split(":")[1]);
//			
//			// Get line of code this appeared on
//			TextIter iter1 = buffer.getIter(lineNum-1, 0);
//			TextIter iter2 = buffer.getIter(lineNum-1, buffer.getText(iter1, buffer.getEndIter(), true).indexOf("\n"));
//			String lineText = buffer.getText(iter1, iter2, false);
//			
//			int start = lineText.indexOf(name);
//			
//			if(type.equals("member") || type.equals("local") || type.equals("variable")){
//				if(start != 0)
//					while(!Character.isWhitespace(lineText.charAt(start-1)))
//						start += lineText.substring(start+1).indexOf(name)+1;
//				
//				if(!type.equals("variable"))
//					buffer.addVariable(lineNum, start, name.length());
//				else
//					buffer.addVariable(lineNum, start, name.length());
//			}
//			
//			else if(type.equals("function")){
//				buffer.addFunction(name, lineNum-1, start, true);
//			}
			
			line = reader.readLine();
		}
		
		reader.close();
	}
}
