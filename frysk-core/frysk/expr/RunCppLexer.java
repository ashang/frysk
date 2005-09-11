// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

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
