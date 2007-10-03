// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import java.io.StringReader;
import java.text.ParseException;
import java.util.Vector;

import antlr.CommonAST;
import frysk.lang.Variable;
import frysk.expr.CppParser;
import frysk.expr.CppLexer;
import frysk.expr.CppTreeParser;


public class PrintHandler implements CommandHandler
{
    private static final int DECIMAL = 10;
    private static final int HEX = 16;
    private static final int OCTAL = 8;
    public void handle(Command cmd) throws ParseException
        {
	Vector params = cmd.getParameters();
	Variable result;
    boolean haveFormat = false;
    int outputFormat = DECIMAL;
	String sInput = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

	for (int i = 0; i < params.size(); i++)
	    {
          System.out.println("cmd=" + params.elementAt(i));
		if (((String)params.elementAt(i)).equals("-format"))
		    {
		    haveFormat = true;
			i += 1;
			String arg = ((String)params.elementAt(i));
			if (arg.compareTo("d") == 0)
			    outputFormat = DECIMAL;
			else if (arg.compareTo("o") == 0)
			    outputFormat = OCTAL;
			else if (arg.compareTo("x") == 0)
			    outputFormat = HEX;
            System.out.println("arg=" + arg + " " + outputFormat);
		    }
	    }
    if (haveFormat)
      sInput = sInput.substring(0,sInput.indexOf("-format"));

    System.out.println(sInput + " " + outputFormat);
	if (sInput.length() == 0) {
	    cmd.getOut().println ("Usage " + cmd.getAction() + " Expression [-format d|x|o]");
	    return;
	}
	if (cmd.getAction().compareTo("assign") == 0) {
	    int i = sInput.indexOf(' ');
	    if (i == -1) {
		cmd.getOut().println ("Usage: assign Lhs Expression");
		return;
	    }
	    sInput = sInput.substring(0, i) + "=" + sInput.substring(i);
	}
	sInput += (char)3;
	CppLexer lexer = new CppLexer(new StringReader (sInput));
	CppParser parser = new CppParser(lexer);
	try {
	    parser.start();
	}
	catch (antlr.RecognitionException r)
	{}
	catch (antlr.TokenStreamException t)
	    {cmd.getOut().println ("Token");}
	catch (frysk.expr.TabException t)
	    {cmd.getOut().println ("Tab");}

	CommonAST t = (CommonAST)parser.getAST();
	CppTreeParser treeParser = new CppTreeParser(4, 2, SymTab.symTab);

	try {
        Integer intResult;
	    result = treeParser.expr(t);
        switch (outputFormat)
        {
          case HEX: 
            cmd.getOut().print("0x");
            break;
          case OCTAL: 
            cmd.getOut().print("0");
            break;
        }
        
        
        cmd.getOut().println(Integer.toString(result.getInt(),outputFormat));
	}   catch (ArithmeticException ae)  {
	    System.err.println("Arithmetic Exception occurred:  " + ae);
	}
	catch (antlr.RecognitionException r)
	{}
	catch (frysk.lang.InvalidOperatorException i)
	{}
	catch (frysk.lang.OperationNotDefinedException o)
	{}
    }
}
