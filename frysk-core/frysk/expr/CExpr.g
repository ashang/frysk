// This file is part of the program FRYSK.
//
// Copyright 2005, 2007 Red Hat Inc.
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


header
{
// This file is part of the program FRYSK.
//
// Copyright 2005, 2007 Red Hat Inc.
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

    import java.util.regex.Pattern;
    import java.util.regex.Matcher;
    import java.io.StringReader;
}

class CExprParser extends Parser;

options {
    defaultErrorHandler=false;
    buildAST=true;
    k=2;
}

{
    private int assign_stmt_RHS_found;
    //private String sInputExpression;

    protected CExprParser(TokenStream lexer, String sInput)
    {
        this(lexer);
    }
}

/** 
  * These tokens are never returned by the Lexer. These are imaginary
  * tokens used as root nodes in the generated Abstract Syntax Tree.
  */
imaginaryTokenDefinitions
    :
	ADDRESS_OF
	ARG_LIST
	CAST
	COND_EXPR
	EXPR_LIST
	FUNC_CALL
	MEMORY
	MEMBER
	SIZEOF
	INDEX
	SLICE
	ARITHMETIC_PLUS
	ARITHMETIC_MINUS
	PREINCREMENT
	PREDECREMENT
	POSTINCREMENT
	POSTDECREMENT	
    ;

/** 
  * The TabException propagates all the way up, to the start rule,
  * which then propagates it up to the calling program.
  */
start
    :   expressionList EOF
    ;

/** 
  *  This rule looks for comma separated expressions.
  */
expressionList
    :   expression (COMMA! expression)*
        {#expressionList = #(#[EXPR_LIST,"Expr list"], expressionList);}
        ;

expression!
    :   assign_expr1: assignment_expression 
        {
/** 
  * The following syntax is used for generation of ASTs. 
  * The # before a symbol refers to the AST for that symbol. 
  * ## refers to the AST returned by this rule (expression)
  */

            ## = #assign_expr1;
        }
    ;

/**
  *  Various types of assignment operators like +=, *=, /= etc.
  */
assign_op  
    :   ASSIGNEQUAL^
    |   TIMESEQUAL^
    |   DIVIDEEQUAL^
    |   MINUSEQUAL^
    |   PLUSEQUAL^
    |   MODEQUAL^
    |   SHIFTLEFTEQUAL^
    |   SHIFTRIGHTEQUAL^
    |   BITWISEANDEQUAL^
    |   BITWISEXOREQUAL^
    |   BITWISEOREQUAL^
    ;

/**
  *  Assignment expressions of the form "expr1 = expr2 = expr3".
  *  Notice that the operator can by any assignment operator.
  */
assignment_expression! 
    :   (c:conditional_expression
            (a:assign_op   r:remainder_expression)?)
        {
            if (#a != null)
                ## = #(#a, #c, #r);
            else
                ## = #c;
        }
    ;

remainder_expression 
    :   ((conditional_expression 
                (COMMA|SEMICOLON|RPAREN))=>
            { assign_stmt_RHS_found += 1;}
        
            assignment_expression 
        {
            if (assign_stmt_RHS_found > 0)
                assign_stmt_RHS_found -= 1;
            else {
                System.out.println(LT(1).getLine() + 
                    "warning Error in assign_stmt_RHS_found = " +
                    assign_stmt_RHS_found + "\n");
                System.out.println("Press return to continue\n");
            }
        }
            |	assignment_expression
        )
    ;

/**
  *  Conditional expressions of the form
  *  (logical_expr)?expr:expr
  */
conditional_expression! 
    :   log_or_expr:logical_or_expression
        (ques:QUESTIONMARK expr:expression colon:COLON 
            cond_expr:conditional_expression)?
        {
          if (ques != null)
            ## = #([COND_EXPR, "ConditionalExpression"], #log_or_expr, #expr, #cond_expr);
          else
            ## = #log_or_expr;
        }
    ;

logical_or_expression 
    :	logical_and_expression (OR^ logical_and_expression)* 
    ;

logical_and_expression 
    :	bitwise_inclusive_or_expression (AND^ bitwise_inclusive_or_expression)* 
    ;

bitwise_inclusive_or_expression 
    :	bitwise_exclusive_or_expression (BITWISEOR^ bitwise_exclusive_or_expression)*
    ;

bitwise_exclusive_or_expression 
    :   bitwise_and_expression (BITWISEXOR^ bitwise_and_expression)*
    ;

bitwise_and_expression 
    :   equality_expression (AMPERSAND^  equality_expression)*
    ;

equality_expression 
    :   relational_expression ((NOTEQUAL^ | EQUAL^) relational_expression)*
    ;

relational_expression 
    :   shift_expression
        (options {warnWhenFollowAmbig = false;}:
	    (	LESSTHAN^
            |	GREATERTHAN^
            |	LESSTHANOREQUALTO^
            |	GREATERTHANOREQUALTO^
            )
            shift_expression
        )*
    ;

shift_expression 
    :   additive_expression ((SHIFTLEFT^ | SHIFTRIGHT^) additive_expression)*
    ;

additive_expression 
    :   multiplicative_expression
        (options {warnWhenFollowAmbig = false;}:
            (PLUS^ | MINUS^) multiplicative_expression
        )*
    ;

multiplicative_expression 
    :	member_selection_expression
        (options {warnWhenFollowAmbig = false;}:
            (STAR^ | DIVIDE^ | MOD^) member_selection_expression
        )*
    ;
    
member_selection_expression
    : prefix_expression 
        ((DOTSTAR^ | POINTERSTAR^) prefix_expression 
        )*
    ;      
 
prefix_expression 
    :   "sizeof"! expr:prefix_expression 
        { ## = #(#[SIZEOF, "Size_Of"], #expr); 
        }
        //sizeof (type)
    |   PLUSPLUS^ pp_expr: prefix_expression 
        { ## = #([PREINCREMENT, "Preincrement"], #pp_expr);    
        }
    |   MINUSMINUS^ mm_expr: prefix_expression
        { ## = #([PREDECREMENT, "Preincrement"], #mm_expr);    
        }    
    |   TILDE^ prefix_expression 
    |   NOT^ prefix_expression   
    |   MINUS^ n_expr: prefix_expression
        { ## = #([ARITHMETIC_MINUS, "Arithmetic_minus"], #n_expr);    
        }          
    |   PLUS^ p_expr: prefix_expression 
        { ## = #([ARITHMETIC_PLUS, "Arithmetic_plus"], #p_expr);    
        }
    |   AMPERSAND addr_expr: prefix_expression
        { ## = #([ADDRESS_OF, "Address_Of"], #addr_expr);    
        }         
    |   STAR dref_expr: postfix_expression
        { ## = #([MEMORY, "Memory"], #dref_expr);    
        }            
        // new operators 
        // delete operators
    |   cast_expression
    |   postfix_expression 
    ;
    
cast_expression! 
    :  LPAREN tc:typeCast RPAREN expr:prefix_expression
       { ## = #([CAST, "Cast"], #tc, #expr);}
    ;
  
postfix_expression!
    { AST astPostExpr = null; }   
    : (   sc_expr: scope_expression {  astPostExpr = #sc_expr; }
         ( DOT
             ( IDENT_TAB { throw new IncompleteMemberException(#astPostExpr,
                                                               #IDENT_TAB); }
             | id_expr1:IDENT { astPostExpr = #(#[MEMBER, "Member"],
                                                #astPostExpr, #id_expr1); }
             )
         | POINTERTO
            ( IDENT_TAB { throw new IncompleteMemberException(#astPostExpr,
                                                              #IDENT_TAB); }
            | id_expr2:IDENT { astPostExpr = #(#[MEMORY, "Memory"],
                                               #astPostExpr); 
                               astPostExpr = #(#[MEMBER, "Member"],
                                               #astPostExpr, #id_expr2); }
            )
         | LSQUARE arrExpr1:expressionList 
           ( RSQUARE  { astPostExpr = #(#[INDEX, "Index"], 
                                        #astPostExpr, #arrExpr1); }
	   | COLON arrExpr2:expressionList RSQUARE { astPostExpr = #(#[SLICE, "Slice"], 
	                                                             #astPostExpr, #arrExpr1, #arrExpr2); }
	   )    
         | LPAREN! expressionList RPAREN!  
   	 | PLUSPLUS  
   	   { astPostExpr = #(#[POSTINCREMENT, "Postincrement"], #astPostExpr); 
   	   }
   	 | MINUSMINUS
   	   { astPostExpr = #(#[POSTDECREMENT, "Postdecrement"], #astPostExpr); 
   	   }
   	   )*
   	 )      
    { ## = #astPostExpr; }       
    ;           
    
/**
 * The TAB over here is not part of the C++ grammar.
 * This enables auto-completion by allowing the user
 * to press TAB whenever auto-completion is required
 */
scope_expression 
    : IDENT
        ( SCOPE
            ( IDENT
            | IDENT_TAB { throw new IncompleteScopeException(#IDENT_TAB); }
            )
        )*
    | IDENT_TAB { throw new IncompleteIdentifierException(#IDENT_TAB); }
    | SCOPE
        ( IDENT
        | IDENT_TAB { throw new IncompleteScopeException(#IDENT_TAB); }
        )
    | LPAREN! expressionList RPAREN!
    | constant
    | "this"
    ;
    
primitiveType
    :   "boolean"
    |   "char"
    |   "byte"
    |   "short"
    |   "int"
    |   "long"
    |   "float"
    |   "double"
    ;
    
typeCast
    :   primitiveType (STAR)?
    ;   

constant
    :   OCTALINT
    |   DECIMALINT
    |   HEXADECIMALINT
    |   CharLiteral
    |   (StringLiteral)+
    |   FLOAT
    |   DOUBLE
    |   "true"
    |   "false"
    ;
    
/*---------------------------------------------------------------------------
 * The Lexer
 *---------------------------------------------------------------------------*/

class CExprLexer extends Lexer;

options {
    charVocabulary = '\0'..'\377';
    testLiterals=true;    
    k=3;
}

tokens
{
    OPERATOR = "operator";
}

{
    private String fqinit;

    private char fqLA(int i) throws CharStreamException {
        if (i >= fqinit.length())
            return LA(i - fqinit.length() + 1);
        else
            return fqinit.charAt(i);
    }

    private void fqmatch(String s) throws MismatchedCharException, CharStreamException {
        while (fqinit.length() > 0) {
            char c = s.charAt(0);
            char d = fqinit.charAt(0);
            if (c != d)
                throw new MismatchedCharException(d, c, false, this);
            s = s.substring(1);
            fqinit = fqinit.substring(1);
        }
        super.match(s);
    }

    public static class FqIdentException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FqIdentException(String s) {
            super(s);
        }
    }

    public static class FqIdentExtraGarbageException extends FqIdentException {
        private static final long serialVersionUID = 1L;
        public FqIdentExtraGarbageException(String garbage) {
            super(garbage);
        }
    }

    public static class FqIdentInvalidTokenException extends FqIdentException {
        private static final long serialVersionUID = 1L;
        public FqIdentInvalidTokenException(String token) {
            super(token);
        }
    }

    public static FqIdentToken parseFqIdent(String str)
        throws FqIdentExtraGarbageException, FqIdentInvalidTokenException
    {
        StringReader r = new StringReader(str);
        CExprLexer lexer = new CExprLexer(r);
        Token tok;

        try {
            tok = lexer.nextToken();

            if (!(tok instanceof FqIdentToken))
                throw new FqIdentInvalidTokenException(tok.getText());

            FqIdentToken fqTok = (FqIdentToken)tok;

            if ((tok = lexer.nextToken()).getType() != Token.EOF_TYPE)
                throw new FqIdentExtraGarbageException(tok.getText());

            return fqTok;

        } catch (antlr.TokenStreamException exc) {
            throw new FqIdentInvalidTokenException(str);
        }
    }
}

AMPERSAND       : '&' ;
AND		        : "&&" ;
ASSIGNEQUAL     : '=' ;
AT              : '@' ;
BITWISEANDEQUAL : "&=" ;
BITWISEOR       : '|'	  ;
BITWISEOREQUAL  : "|="  ;
BITWISEXOR      : '^'	  ;
BITWISEXOREQUAL : "^="  ;
COLON           : ':' ;
COMMA           : ',' ;
DIVIDE          : '/' ;
DIVIDEEQUAL     : "/=" ;
DOTSTAR         : ".*" ;
EQUAL			: "==" ;
ETX	            : '\3'  ;
GREATERTHAN     : ">" ;
GREATERTHANOREQUALTO  : ">=" ;
LCURLY          : '{' ;
LESSTHAN        : "<" ;
LESSTHANOREQUALTO     : "<=" ;
LPAREN          : '('   ;
LSQUARE         : '[' ;
MINUS           : '-' ;
MINUSEQUAL      : "-=" ;
MINUSMINUS      : "--" ;
MOD             : '%' ;
MODEQUAL        : "%=" ;
NOT		        : '!' ;
NOTEQUAL		: "!=" ;
OR		        : "||" ;
PLUS            : '+' ;
PLUSEQUAL       : "+=" ;
PLUSPLUS        : "++" ;
POINTERSTAR     : "->*" ;
POINTERTO       : "->";
QUESTIONMARK    : '?' ;
RCURLY          : '}' ;
RPAREN          : ')'   ;
RSQUARE         : ']' ;
SCOPE           : "::"  ;
SEMICOLON       : ';' ;
SHIFTLEFT       : "<<" ;
SHIFTLEFTEQUAL  : "<<=" ;
SHIFTRIGHT      : ">>" ;
SHIFTRIGHTEQUAL : ">>=" ;
STAR            : '*' ;
TILDE           : '~' ;
TIMESEQUAL      : "*=" ;

protected
ELLIPSIS  : "..." ;

/*
 * Funky HPD #-syntax doesn't map very well to LL-k type parser (for
 * constant 'k').  When written directly, we get lots of lexical
 * ambiguities.  We work around that by doing arbitrary manual
 * look-ahead and just parsing the tokens ourselves.  Any whitespace
 * or EOF stops the lookahead.
 */

protected
PARSE_FQIDENT
    : {
            String matched = "";
            String part = "";

            String partDso = null;
            String partFile = null;
            String partProc = null;
            String partLine = null;

            int i = 0;
            char c;
            if ((c = fqLA(0)) == '#') {
                matched += c;
                i++;
                while (true) {
                    c = fqLA(i++);
                    matched += c;
                    if (Character.isWhitespace(c) || c == EOF_CHAR)
                        // This is a wack.
                        throw new RecognitionException("Nonterminated DSO part `" + matched
                                                       + "' in fully qualified notation.");
                    else if (c == '#')
                        break;
                    part += c;
                }
                if (part.length() == 0)
                    throw new RecognitionException("Empty DSO part `" + matched
                                                   + "' in fully qualified notation.");
                partDso = part;
                part = "";
            }

            // Automaton state is composed of following sub-states:
            final int FILE = 1;
            final int LINE = 2;
            final int SYMB = 4;
            int state = LINE | SYMB;
            loop: while(true) {
                c = fqLA(i++);
                if (Character.isWhitespace(c) || c == EOF_CHAR)
                    break;

                matched += c;
                part += c;
                switch (c) {
                    case '.': {
                        state |= FILE;
                        state &= ~SYMB;
                        break;
                    }

                    case '#': {
                        if (partLine == null && partProc == null) {
                            if ((state & FILE) != 0 && partFile == null)
                                partFile = part.substring(0, part.length() - 1);
                            else if ((state & LINE) != 0)
                                partLine = part.substring(0, part.length() - 1);
                            else if ((state & SYMB) != 0) {
                                partProc = part.substring(0, part.length() - 1);
                                if (!Character.isJavaIdentifierStart(partProc.charAt(0)))
                                    throw new RecognitionException("Procedure part (`" + partProc + "') in fully "
                                                                   + "qualified notation has to be valid identifier.");
                            } else
                                // This # could belong to the next symbol.
                                // Break out and try to match the initial sequence.
                                break loop;
                        } else
                            throw new RecognitionException("Unexpected `#' after line or proc name was defined.");

                        state = SYMB;
                        if (partLine == null && partProc == null)
                            state |= LINE;
                        part = "";
                        break;
                    }

                    default: {
                        if (!(c >= '0' && c <= '9')) {
                            state &= ~LINE;

                            if (!(Character.isJavaIdentifierStart(c)
                                  || c == '@'
                                  || (c == ':' && part.length() == 4
                                      && part.equals("plt:")))) {

                                // Break out early if we are already
                                // just waiting for symbol.
                                if (partLine != null || partProc != null)
                                    break loop;
                                else
                                    state &= ~SYMB;
                            }
                        }
                    }
                }
            }

            // ((state & SYMB) == 0) here means that we've parsed more
            // than a symbol name, in hope it would turn out to be a
            // file name (e.g. hello-world.c#symbol as a symbol
            // reference vs. hello-world.c as an expression involving
            // subtraction and struct access).  In following, we take
            // care not to consume anything that's not an identifier.
            // E.g. when the user types "a+b", we want to match
            // only identifier "a".

            boolean wantPlt = false;
            if (part.startsWith("plt:")) {
                wantPlt = true;
                part = part.substring(4);
            }

            int v = part.indexOf('@');
            String version = null;
            if (v >= 0) {
                version = part.substring(v + 1);
                part = part.substring(0, v);
            }

            // This is delibaretely simplified and ignores request for initial letter.
            // This is for better error reporting below, we first snip off irrelevant
            // parts before yelling at user that his identifier sucks.
            Matcher m = Pattern.compile("[a-zA-Z0-9_$]+").matcher(part);
            if (m.lookingAt()) {
                int diff = part.length() - m.end();
                if (diff > 0) {
                    matched = matched.substring(0, matched.length() - diff);
                    part = part.substring(0, m.end());
                }
            }

            if (!Character.isJavaIdentifierStart(part.charAt(0)))
                throw new RecognitionException("Invalid symbol `" + part + "'.");

            FqIdentToken tok = new FqIdentToken(IDENT, matched);
            tok.dso = partDso;
            tok.file = partFile;
            tok.line = partLine;
            tok.proc = partProc;
            tok.symbol = part;
            tok.version = version;
            tok.wantPlt = wantPlt;
            tok.setLine(getLine());
            $setToken(tok);

            fqmatch(matched);
            tok.setColumn(getColumn() - matched.length());
        } ;

protected
IDENT
    : ('$'|'#'|'a'..'z'|'A'..'Z'|'_') { fqinit = $getText; }
      fqident:PARSE_FQIDENT { $setToken(fqident); } ;

/**
 *  A <TAB> token is returned not only on regular tabs
 *  but also when a TAB is hit after an incomplete identifier.
 */

IDENT_TAB 
    :   '\t'
    |   ident:IDENT { $setType(IDENT); $setToken(ident); }
        ('\t' { $setType(IDENT_TAB);
                ident.setText($getText);
                ident.setType(IDENT_TAB); })?
    ;

protected
NL
    :   '\r' ('\n')?  // DOS/Windows
    |   '\n'    // Unix
        { newline(); }
    ;

WS
    :	(   ' '
        |   '\f'
        |   NL    
        |   '\\'
            (   '\r' ('\n')?
            |   '\n'
            )
        )
        { $setType(Token.SKIP); }
    ; 

CharLiteral
    :	'\'' (Escape | ~( '\'' )) '\''
    ;

StringLiteral
    :	'"'
        (   Escape
        |	(	"\\\r\n"   // MS 
            |	"\\\r"     // MAC
            |	"\\\n"     // Unix
            )
        |	~('"' | '\r' | '\n' | '\\')
        )*
        '"'
    ;

protected
Escape  
    :	'\\'
        ( options {warnWhenFollowAmbig=false;}:
            'a'
        |   'b'
        |   'f'
        |   'n'
        |   'r'
        |   't'
        |   'v'
        |   '"'
        |   '\''
        |   '\\'
        |   '?'
        |   ('0'..'3') 
            (options	{warnWhenFollowAmbig=false;}
            : Digit (options {warnWhenFollowAmbig=false;}  : Digit)? )?
        |   ('4'..'7') (options{warnWhenFollowAmbig=false;}: Digit)?
        |   'x' (options{warnWhenFollowAmbig=false;}: Digit | 'a'..'f' | 'A'..'F')+
        )
    ;

/* Numeric Constants: */

protected
Digit
    :	'0'..'9'
    ;

protected
Decimal
    :	('0'..'9')+
    ;

protected
LongSuffix
    :	'l'
    |   'L'
    ;

protected
UnsignedSuffix
    :	'u'
    |	'U'
    ;

protected
FloatSuffix
    :	'f'
    |	'F'
    ;

protected
Exponent
    :   ('e' | 'E') ('+' | '-')? (Digit)+
    ;

protected
Vocabulary
    :	'\3'..'\377'
    ;

// a numeric literal
NUM
	{Token t=null;}
	:	'.' {_ttype = DOT;}
		(	'.' '.' {_ttype = ELLIPSIS;}
		|	(	('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
				{
				if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
					_ttype = FLOAT;
				}
				else {
					_ttype = DOUBLE; // assume double
				}
				}
			)?
		)

	|	(	'0' {_ttype = DECIMALINT;} // special case for just '0'
			(	('x'|'X')
				(											// hex
					// the 'e'|'E' and float suffix stuff look
					// like hex digits, hence the (...)+ doesn't
					// know when to stop: ambig.  ANTLR resolves
					// it correctly by matching immediately.  It
					// is therefor ok to hush warning.
					options {
						warnWhenFollowAmbig=false;
					}
				:	HEX_DIGIT
				)+					{_ttype = HEXADECIMALINT;}

			|	//float or double with leading zero
				(('0'..'9')+ ('.'|EXPONENT|FLOAT_SUFFIX)) => ('0'..'9')+

			|	('0'..'7')+			{_ttype = OCTALINT;}
			)?
		|	(('1'..'9') ('0'..'9')* {_ttype = DECIMALINT;})
             ( '#' {fqinit = $getText;}
               fqident:PARSE_FQIDENT { $setType(IDENT); $setToken(fqident); } )?
		)
		(	('l'|'L') { _ttype = DECIMALINT; }

		// only check to see if it's a float if looks like decimal so far
		|	{_ttype == DECIMALINT}?
			(	'.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
			|	EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
			|	f4:FLOAT_SUFFIX {t=f4;}
			)
			{
			if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0) {
				_ttype = FLOAT;
			}
			else {
				_ttype = DOUBLE; // assume double
			}
			}
		)?
	;


// Protected methods to assist in matching floating point numbers
// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
	:	('0'..'9'|'A'..'F'|'a'..'f')
	;
protected
EXPONENT
	:	('e'|'E') ('+'|'-')? ('0'..'9')+
	;


protected
FLOAT_SUFFIX
	:	'f'|'F'|'d'|'D'
	;
