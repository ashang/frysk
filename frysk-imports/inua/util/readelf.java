// Copyright 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
package inua.util;

import java.util.Iterator;
import java.util.LinkedList;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

import inua.dwarf.Elf;
import inua.elf.PrintEhdr;
import inua.elf.PrintShdr;
import inua.elf.PrintPhdr;
import inua.elf.PrintSym;
import inua.elf.PrintNote;
import inua.dwarf.PrintDebugLine;
import inua.dwarf.PrintDebugRanges;
import inua.dwarf.PrintDebugMacinfo;
import inua.dwarf.PrintDebugFrame;
import inua.dwarf.PrintDebugStr;
import inua.dwarf.PrintDebugLoc;
import inua.dwarf.PrintDebugInfo;
import inua.dwarf.PrintDebugPubs;
import inua.dwarf.PrintDebugAranges;
import inua.dwarf.PrintDebugAbbrev;
import inua.util.PrintWriter;



class readelf
{
  
  private static Parser parser;
  protected static boolean help;
  protected static boolean fileHeader;
  protected static boolean programHeaders;
  protected static boolean segments;
  protected static boolean sectionHeaders;
  protected static boolean section;
  protected static boolean sectionGroups;
  protected static boolean headers;
  protected static boolean syms;
  protected static boolean symbols;
  protected static boolean notes;
  protected static boolean wide;
  protected static String arg;
  
  protected static LinkedList otherArgs;

  
  private static void addOptions (Parser parser)
  {
    parser.add(new Option ("help", 'H', "Print a help message") {
      public void parsed (String arg0) throws OptionException {
        help = true;
      }
    });
    
    parser.add(new Option ("file-header", 'h', "File header message") {
      public void parsed (String arg0) throws OptionException {
        fileHeader = true;
      }
    });
    
    parser.add(new Option ("program-headers", 'l', "Program header message") {
      public void parsed (String arg0) throws OptionException {
        programHeaders = true;
      }
    });
    
    parser.add(new Option ("segments", "segments message") {
      public void parsed (String arg0) throws OptionException {
        segments = true;
        programHeaders = true;
      }
    });
    
    parser.add(new Option ("section-headers", 'S', "Section headers message") {
      public void parsed (String arg0) throws OptionException {
        sectionHeaders = true;
      }
    });
    
    parser.add(new Option ("section-headers", 'S', "Sections message") {
      public void parsed (String arg0) throws OptionException {
        section = true;
        sectionHeaders = true;
      }
    });
    
    parser.add(new Option ("section-groups", 'g', "Section groups message") {
      public void parsed (String arg0) throws OptionException {
        sectionGroups = true;
      }
    });
    
    parser.add(new Option ("headers", 'e', "Headers message") {
      public void parsed (String arg0) throws OptionException {
        headers = true;
      }
    });
    
    parser.add(new Option ("syms", 's', "Syms message") {
      public void parsed (String arg0) throws OptionException {
        syms = true;
      }
    });

    parser.add(new Option ("symbols", "Symbols message") {
      public void parsed (String arg0) throws OptionException {
        symbols = true;
        syms = true;
      }
    });

    parser.add(new Option ("notes", 'n', "Notes message") {
      public void parsed (String arg0) throws OptionException {
        notes = true;
      }
    });

    parser.add(new Option ("wide", 'W', "Wide message") {
      public void parsed (String arg0) throws OptionException {
        wide = true;
      }
    });
    
    parser.add(new Option ("debug-dump", "Debug Dump Message", "<dump variable>") {
      public void parsed (String arg0) throws OptionException {
        arg = arg0;
      }
    });
    
  }
    static void usage (PrintWriter o)
    {
	o.print ("Usage");
	o.println ();
    }

    public static void main (String[] argv)
    {
	PrintWriter o = new PrintWriter (System.out);

	if (argv.length == 0) {
	    usage (o);
	    return;
	}

	parser = new Parser("readelf", "1.0", true);
    addOptions(parser);
	

    parser.parse (argv, new FileArgumentCallback() {
        public void notifyFile(String arg) throws OptionException
        {           
            otherArgs.add(arg);
        }
    });     
         
	
	boolean debugDumpLine = arg.equals ("line");
	boolean debugDumpInfo = arg.equals ("info");
	boolean debugDumpAbbrev = arg.equals ("abbrev");
	boolean debugDumpPubnames = arg.equals ("pubnames");
	boolean debugDumpRanges = arg.equals ("Ranges");
	boolean debugDumpAranges = arg.equals ("ranges") || arg.equals ("aranges");
	boolean debugDumpMacinfo = arg.equals ("macro");
	boolean debugDumpFrames = arg.equals ("frames");
	boolean debugDumpFramesInterp = arg.equals ("frames-interp");
	boolean debugDumpStr = arg.equals ("str");
	boolean debugDumpLoc = arg.equals ("loc");
	// o.print ("Unrecognized debug-dump option " + arg);
  

	if (otherArgs.size() == 0) {
	    o.print  ("readelf: Warning: Nothing to do.");
	    o.println ();
	    usage (o);
	    return;
	}

	try {
      Iterator iter = otherArgs.listIterator();
      while (iter.hasNext()) {
	    String tempArg = (String) iter.next();
		Elf elf = new Elf (tempArg);
		if (headers || fileHeader)
		    new PrintEhdr (elf).print (o);
		if (headers || sectionHeaders)
		    new PrintShdr (elf).print (o, headers);
		if (headers || programHeaders)
		    new PrintPhdr (elf).print (o, headers);
		if (syms)
		    new PrintSym (elf).print (o);

		if (debugDumpLine)
		    new PrintDebugLine (elf).print (o);
		if (debugDumpInfo)
		    new PrintDebugInfo (elf).print (o);
		if (debugDumpAbbrev)
		    new PrintDebugAbbrev (elf).print (o);
		if (debugDumpPubnames)
		    new PrintDebugPubs (elf).print (o);
		if (debugDumpAranges)
		    new PrintDebugAranges (elf).print (o);
		if (debugDumpRanges)
		    new PrintDebugRanges (elf).print (o);
		if (debugDumpMacinfo)
		    new PrintDebugMacinfo (elf).print (o);
		if (debugDumpFrames)
		    new PrintDebugFrame (elf).print (o);
		if (debugDumpFramesInterp)
		    new PrintDebugFrame (elf).printInterp (o);
		if (debugDumpStr)
		    new PrintDebugStr (elf).print (o);
 		if (debugDumpLoc)
		    new PrintDebugLoc (elf).print (o);
		if (notes)
		    new PrintNote (elf).print (o);
		if (sectionGroups) {
		    o.println ();
		    o.println ("There are no section groups in this file.");
		}
		    
	    }
	} catch (Exception e) {
	    throw new RuntimeException (e);
	}
	o.flush ();
    }
}
