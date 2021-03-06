// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008 Red Hat Inc.
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
    :  LPAREN tc:primitiveType (STAR)? RPAREN expr:prefix_expression
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
    final FQIdentParser fqIdParser
        = new FQIdentParser(this, true, false, true, false);
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
LSQUARE         : '[' {
                        // We can't use ('0'..'9')? here, because even
                        // if there is 0..9, it still doesn't have to
                        // parse correctly as [a.b#c] syntax.  But
                        // antlr would already match the digit.
                        if (((LA(1) >= '0' && LA(1) <= '9')))
                            try {
                                Token tok = fqIdParser.parse($getText);
                                if (tok != null) {
                                    $setToken(tok);
                                    $setType(IDENT);
                                }
                            } catch (RecognitionException exc) { }
                       } ;
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

protected
IDENT
    : ('$'|'#'|'a'..'z'|'A'..'Z'|'_') {
          $setToken(fqIdParser.parse($getText));
      } ;

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
             ( '#' {
                   $setType(IDENT);
                   $setToken(fqIdParser.parse($getText));
               } )?
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
