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

import jargs.gnu.CmdLineParser;
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
import inua.PrintWriter;

class readelf
{
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

	CmdLineParser parser = new CmdLineParser ();
	CmdLineParser.Option helpOption = parser.addBooleanOption ('H', "help");
	CmdLineParser.Option fileHeaderOption = parser.addBooleanOption ('h', "file-header");
	CmdLineParser.Option programHeadersOption = parser.addBooleanOption ('l', "program-headers");
	CmdLineParser.Option segmentsOption = parser.addBooleanOption ("segments");
	CmdLineParser.Option sectionHeadersOption = parser.addBooleanOption ('S', "section-headers");
	CmdLineParser.Option sectionsOption = parser.addBooleanOption ('S', "sections");
	CmdLineParser.Option sectionGroupsOption = parser.addBooleanOption ('g', "section-groups");
	CmdLineParser.Option headersOption = parser.addBooleanOption ('e', "headers");
	CmdLineParser.Option symsOption = parser.addBooleanOption ('s', "syms");
	CmdLineParser.Option symbolsOption = parser.addBooleanOption ("symbols");
	CmdLineParser.Option notesOption = parser.addBooleanOption ('n', "notes");
	CmdLineParser.Option wideOption = parser.addBooleanOption ('W', "wide");
	CmdLineParser.Option debugDumpOption = parser.addStringOption ("debug-dump");

        try {
            parser.parse (argv);
        }
        catch (CmdLineParser.OptionException e) {
            System.err.println (e.getMessage ());
            usage (o);
            System.exit (2);
        }

	// boolean help =
	((Boolean)parser.getOptionValue (helpOption, Boolean.FALSE)).booleanValue ();
	boolean fileHeader = ((Boolean)parser.getOptionValue (fileHeaderOption, Boolean.FALSE)).booleanValue ();
	boolean programHeaders = (((Boolean) parser.getOptionValue (programHeadersOption, Boolean.FALSE)).booleanValue ()
				  || ((Boolean) parser.getOptionValue (segmentsOption, Boolean.FALSE)).booleanValue ());
	boolean sectionHeaders = (((Boolean) parser.getOptionValue (sectionHeadersOption, Boolean.FALSE)).booleanValue ()
				  || ((Boolean) parser.getOptionValue (sectionsOption, Boolean.FALSE)).booleanValue ());
	boolean sectionGroups = ((Boolean) parser.getOptionValue (sectionGroupsOption, Boolean.FALSE)).booleanValue ();
	boolean headers = ((Boolean) parser.getOptionValue (headersOption, Boolean.FALSE)).booleanValue ();
	boolean syms = (((Boolean) parser.getOptionValue (symsOption, Boolean.FALSE)).booleanValue ()
			   || ((Boolean) parser.getOptionValue (symbolsOption, Boolean.FALSE)).booleanValue ());
	// boolean wide = 
	((Boolean) parser.getOptionValue (wideOption, Boolean.FALSE)).booleanValue ();
	boolean notes = ((Boolean) parser.getOptionValue (notesOption, Boolean.FALSE)).booleanValue ();

	String arg = (String) parser.getOptionValue (debugDumpOption, "");
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

	String[] otherArgs = parser.getRemainingArgs();

	if (otherArgs.length == 0) {
	    o.print  ("readelf: Warning: Nothing to do.");
	    o.println ();
	    usage (o);
	    return;
	}

	try {
	    for (int j = 0; j < otherArgs.length; j++) {
		Elf elf = new Elf (otherArgs[j]);
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
