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

import antlr.TokenStream;
import antlr.Token;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import antlr.TokenStreamException;

class RunCppLexer
    implements CppParserTokenTypes
{
    static class TestTokens
	implements TokenStream
    {
	protected TokenStream input;
	static String sTokens[] = {
	    "<0>", "EOF", "<2>", "NULL_TREE_LOOKAHEAd", "apply", "arg_list",
	    "expr_list", "cond_expr", "array_ref", "eTX", "COMMA",
	    "ASSIGNEQUAL", "TIMESEQUAL", "DIVIDEEQUAL", "MINUSEQUAL", "PLUSEQUAL",
	    "MODEQUAL", "SHIFTLEFTEQUAL", "SHIFTRIGHTEQUAL", "BITWISEANDEQUAL",
	    "BITWISEXOREQUAL", "BITWISEOREQUAL", "SEMICOLON", "RPAREN", "QUESTIONMARK",
	    "COLON", "OR", "AND", "BITWISEOR", "BITWISEXOR", "AMPERSAND", "NOTEQUAL",
	    "EQUAL", "LESSTHAN", "GREATERTHAN", "LESSTHANOREQUALTO",
	    "GREATERTHANOREQUALTO", "SHIFTLEFT", "SHIFTRIGHT", "PLUS", "MINUS",
	    "STAR", "DIVIDE", "MOD", "DOTMBR", "POINTERTOMBR", "LSQUARE", "RSQUARE",
	    "LPAREN", "DOT", "TAB", "POINTERTO", "PLUSPLUS", "MINUSMINUS", "\"this\"",
	    "OCTALINT", "DECIMALINT", "HEXADECIMALINT", "CharLiteral", "StringLiteral",
	    "FLOATONE", "FLOATTWO", "\"true\"", "\"false\"", "IDENT", "\"operator\"",
	    "LCURLY", "RCURLY", "NOT", "TILDE", "ELLIPSIS", "SCOPE", "TAB_IDENT",
	    "NL", "WS", "Escape", "Digit", "Decimal", "LongSuffix", "UnsignedSuffix",
	    "FloatSuffix", "Exponent", "Vocabulary", "NUM"
	};

	public TestTokens(TokenStream in)
	{
	    input = in;
	}

	public Token nextToken()
	    throws TokenStreamException //throws IOException
	{
	    return input.nextToken();
	}
    }

    public static void main (String args[])
    {
	if (args.length<1) {
	    System.out.println("Usage: java RunCppLexer <input_file_name>");
	    return;
	}

	try {
	    Token tok;
	    TestTokens testTokens = new TestTokens(new CppLexer(new FileInputStream(args[0])));
	    System.out.println("\n\t");
	    while((tok = testTokens.nextToken()).getType() != EOF) {
		System.out.print(TestTokens.sTokens[tok.getType()] + " ");
		if (tok.getType() == TAB)
		    System.out.print("(" + tok.getText() + ")");
	    }
	}
	catch (TokenStreamException ex) {
	    System.out.println("Token Stream Exception: " + ex);
	}
	catch (FileNotFoundException ex) {
	    System.out.println("File not found: " + ex);
	}
	catch (Exception ex) {
	    System.out.println("General Exception: " + ex);
	}
    }
}
