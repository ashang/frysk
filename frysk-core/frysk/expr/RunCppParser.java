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

package frysk.expr;

import java.io.*;
import antlr.collections.AST;
import jline.*;
import java.util.*;
import antlr.*;

/**
 * A Test framework for the C++ expression parser with tab auto completion.
 */

public class RunCppParser 
{
    /**
     * The function was used earlier to parse files containing
     * C++ expressions. This was done since a proper CLI was not developed
     * at that time, so the input was derived from a file. However now 
     * the main function initializes a CLI and reads input keyed in by the
     * user.
     */
    public static void doFile(File f)
	throws Exception 
    {
	if (f.isDirectory()) {
	    String files[] = f.list();
	    for(int i=0; i < files.length; i++)
		doFile(new File(f, files[i]));
	}
	else {
	    System.out.println("-------------------------------------------");
	    System.out.println(f.getAbsolutePath());
	    parseFile(new FileInputStream(f));
	}
    }

    /**
     * A helper function for doFile(File f). It parses a file given to it as
     * an input
     */
    public static void parseFile(InputStream s)
	throws Exception 
    {
	try {
	    int iSize = s.available();
	    byte byteBuf[] = new byte[iSize];
	    s.read(byteBuf, 0, iSize);
	    String sInput = new String(byteBuf);
	    sInput += (char)3;

	    // Create a scanner that reads from the input stream passed to us
	    CppLexer lexer = new CppLexer(s);

	    // Create a parser that reads from the scanner
	    CppParser parser = new CppParser(lexer);

	    parser.start();
	    AST t = parser.getAST();
	    if (t != null)
		System.out.println(t.toStringTree());
	    System.out.println("\n");
	}
	catch (TabException exTab) {
	    System.out.println("tab expression: "+ exTab);
	}
	catch (Exception e) {
	    System.err.println("parser exception: "+e);
	    e.printStackTrace();   // so we can get stack trace
	}
    }

    
    /**
     * This class identifies the delimiters that can be used for
     * argument separation
     */
    /*public static class ParserArgumentDelimiter
	extends ArgumentCompletor.AbstractArgumentDelimiter
    {
	public boolean isDelimiterChar (String buffer, int pos)
	{
	    return false;
	}
    }*/

    
    /**
     * This class defines the "complete()" function needed to handle
     * TAB presses.
     */

    public static class ParserCompletor implements Completor
    {
	public String[] TabCompletion(AST astExpr, String sPartialExpr)
	{
	    String arrCandidates[] = (new String []{"print", "prind", "hello"});

	    if (sPartialExpr.equals(""))
		return arrCandidates;

	    LinkedList lCandidates = new LinkedList();

	    for(int i=0; i<arrCandidates.length;i++) {
		if (arrCandidates[i].startsWith(sPartialExpr)) {
		    lCandidates.add(arrCandidates[i]);
		}
	    }

	    arrCandidates = (String [])lCandidates.toArray(new String[lCandidates.size()]);
	    return arrCandidates;
	}

	/**
	 * This is the function that first gets called when a user
	 * hits the TAB key on the CLI.
	 *
	 * The function is a callback wich is called by the
	 * ConsoleReader.complete() function.  It receives as input the
	 * text that the user enters on the console. The secod
	 * argument gives the position of the cursor within the
	 * text. The function should return the candidates for tab
	 * completion as elements of the List "candidates" which is the
	 * third argument to the function.
	 *
	 * Within this function @CppParser (the class that implements
	 * parser functions) and
	 * @CppLexer (the class that implements Lexer functions duhhh!) 
	 * are instantiated and the text entered by the user is parsed.
	 * 
	 */
	public int complete(String buffer, int cursor, List candidates)
	{
	    // Create a scanner that reads from the input stream
	    // passed to us
	    String sInput, sCompletionArray[] = (new String[]{"a", "b"});
	    int iTabExprLen = 0;

	    sInput = (buffer == null || buffer.equals(""))? "\t" :
	      (buffer.substring(0, cursor) + '\t' + 
		((cursor < buffer.length()) ? buffer.substring(cursor, buffer.length()) : ""));
	    
	    CppLexer lexer = new CppLexer(new StringReader(sInput));
	    CppParser parser = new CppParser(lexer);
	    
	    try {
	      parser.start();
	      AST t = parser.getAST();
	    }
	    catch (TabException exTab) {
	      sCompletionArray = TabCompletion(exTab.getAst(), exTab.getTabExpression().trim());
	      iTabExprLen = exTab.getTabExpression().trim().length();
	    }
	    catch (RecognitionException re) {
	      iTabExprLen = 0;
	      Writer out = new PrintWriter(System.out);
	      try {
		out.write(System.getProperty("line.separator"));
		out.write(System.getProperty("line.separator"));
		out.write(re.getMessage());
		out.write(System.getProperty("line.separator"));
		out.write(System.getProperty("line.separator"));
		out.write("$" + buffer);
		char arrBackSpace[] = new char[buffer.length() - cursor];
		Arrays.fill(arrBackSpace, '\b');
		out.write(arrBackSpace);
		out.flush();
	      }	catch(Exception e) {
		System.err.println("caught exception in writing the output");
	      }
	      return 1;
	    }
	    catch (TokenStreamException tse) {
	      System.err.println("Token Stream Exception");
	      return 0;
	    }

	    List sCompletionList = Arrays.asList(sCompletionArray);
	    
	    if (buffer != null) {
		for(Iterator i=sCompletionList.iterator();i.hasNext();) {
		    String sNext = (String)i.next();
		    candidates.add(sNext);
		}
	    }
	    
	    if (candidates.size() == 1)
		candidates.set(0, ((String)candidates.get(0)) + " ");

	    return	(cursor-iTabExprLen+1);
	}
    }
    
    public static void main(String[] args)
	throws Exception
    {
	try {
	  PrintWriter pw = new PrintWriter(System.out);
	  pw.write("hello");
	  System.out.println(pw.checkError());
	  ConsoleReader consReader;
	  try{
	    consReader = new ConsoleReader();
	  }
	  catch (IOException ioe) {
	    ioe.printStackTrace(System.err);
	    throw (new IOException(ioe.getMessage() + 
		  "I/O exception when creating new instance of Console Reader"));
	  }
	    /*LinkedList listCompletor = new LinkedList();
	    listCompletor.add(new ParserCompletor());
	    Completor [] arrCompletor = (Completor[])listCompletor.toArray(new Completor[listCompletor.size()]);

	    ArgumentCompletor argCompletor = 
		new ArgumentCompletor(arrCompletor, (new ParserArgumentDelimiter()));

	    argCompletor.setStrict(false);
	    //consReader.addCompletor(argCompletor);*/
	    consReader.addCompletor(new ParserCompletor());
	
	    String sInput;
	    try {
	      sInput = consReader.readLine("$");
	    }
	    catch (IOException ioe) {
	      throw (new IOException(ioe.getMessage() + 
		    "I/O exception in readLine"));

	    }
	    //sInput += (char)3;

	    System.out.println(sInput);
	}
	catch(IOException ioe){
	  System.err.println("IO Exception: " + ioe);
	  ioe.printStackTrace(System.err);
	}
	catch (Exception e) {
	    System.err.println("exception: "+e);
	    e.printStackTrace(System.err);   // so we can get stack trace
	}
    }
}
