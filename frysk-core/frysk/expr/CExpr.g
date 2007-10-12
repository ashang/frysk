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
}

class CExprParser extends Parser;

options {
    defaultErrorHandler=false;
    buildAST=true;
    ASTLabelType = "ExprAST";
    k=2;
}

{
/** 
  *	A member variable to keep track of TAB completions requests.
  *	If this is true the normal course of action is to simply
  *	bail out by throwing an exception
  */
    private boolean bTabPressed;
    private int assign_stmt_RHS_found;
    //private String sInputExpression;

    protected CExprParser(TokenStream lexer, String sInput)
    {
        this(lexer);
        bTabPressed = false;
        //sInputExpression = sInput;
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
	REFERENCE
	SUBSCRIPT
	COND_EXPR
	EXPR_LIST
	FUNC_CALL
	MEMORY
    ;

/** 
  * The start rule simply expects an expression list following by
  * the end of text symbol (ETX \3).
  *
  * The TabException propagates all the way up, to the start rule,
  * which then propagates it up to the calling program.
  */
start
    :   expressionList ETX
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
    :	inclusive_or_expression (AND^ inclusive_or_expression)* 
    ;

inclusive_or_expression 
    :	exclusive_or_expression (BITWISEOR^ exclusive_or_expression)*
    ;

exclusive_or_expression 
    :   and_expression (BITWISEXOR^ and_expression)*
    ;

and_expression 
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
        (options	{warnWhenFollowAmbig = false;}:
            (PLUS^ | MINUS^) multiplicative_expression
        )*
    ;

multiplicative_expression 
    :	unary_expression
        (options{warnWhenFollowAmbig = false;}:
            (STAR^ | DIVIDE^ | MOD^) unary_expression
        )*
    ;
 
unary_expression 
    :   PLUS^ unary_expression
    |   MINUS^ unary_expression
    |   PLUSPLUS^ postfix_expression
    |   MINUSMINUS^ postfix_expression
    |   TILDE^ unary_expression
    |   NOT^ unary_expression
    |   unary_expression_simple
    ;

unary_expression_simple 
    :   AMPERSAND prim_expr: id_expression
        {
            ## = #([ADDRESS_OF, "Address Of"], #prim_expr); 
        }
    |   STAR mem_expr: id_expression
        {
            ## = #([MEMORY, "Memory"], #mem_expr); 
        }
    |   cast_expression
//    |   postfix_expression selector* (PLUSPLUS |MINUSMINUS)?
    | postfix_expression
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

cast_expression! 
    :  LPAREN type:primitiveType RPAREN expr:unary_expression
//       | LPAREN (expression) RPAREN unary_expression_simple
//       |  LPAREN (expression | primitiveType) RPAREN unary_expression_simple
        {
            ## = #([CAST, "Cast"], #type, #expr);
        }
	;

postfix_expression 
{String sTabText;}
    //  should subscript, component, call, post inc/dec be moved here?
    :	post_expr1:primary_expression 
        {
          if (bTabPressed) {
	      // ??? Use antlr expressions instead of tree surgery.
            if (#post_expr1.getFirstChild() != null)
              if (#post_expr1.getFirstChild().getNextSibling() != null)
                sTabText = #post_expr1.getFirstChild().getNextSibling().getText();
		      else
		  		sTabText = #post_expr1.getFirstChild().getText();
            else 
              sTabText = #post_expr1.getText();

	        if (#post_expr1.getText().startsWith("Class Reference"))
		      sTabText += ".";

            throw new TabException(#post_expr1, sTabText);
          }
        }
    ;

/**
  *	The TAB over here is not part of the C++ grammar.
  *	This enables auto-completion by allowing the user
  *	to press TAB whenever auto-completion is required
  */
primary_expression 
    :   (TAB {bTabPressed = true;}
	    | primary_identifier)
    |   constant
    |   "this"
    |   LPAREN! expression RPAREN!
    ;

/***
  *  TabException is raised everytime the TAB is pressed.
  *  The parser thus bails out immediately and returns the
  *  parse tree constructed so far.
  */
/* ??? add (id_expression | (TAB {bTabPressed = true;})) */

primary_identifier! 
{
    ExprAST astPostExpr = null, astDotExpr = null;
} 
    :   (   prim_expr: id_expression
            {
                astPostExpr = #prim_expr;
            }

            (   options {warnWhenFollowAmbig = false;}

            : LPAREN (expr2:expressionList)? RPAREN
                { astPostExpr = #([FUNC_CALL, "FuncCall"], #astPostExpr, #expr2); }
	        | LSQUARE arrExpr1:expression (COLON arrExpr2:expression)? RSQUARE
                // a[b][c] => (Array Reference a (Subscript b-lbound)
                //            (Subscript b-hbound) (Subscript c-lbound)...)
                {
                  ExprAST sub = null;
		 		  if (astPostExpr.getFirstChild() != null) {
		      	    #sub = #(#[SUBSCRIPT,"Subscript"], #arrExpr1);
                    astPostExpr.addChild(#sub);
                    // arr[n] is treated as arr[n:n]
                    if (#arrExpr2 != null)
		      	      #sub = #(#[SUBSCRIPT,"Subscript"], #arrExpr2);
                    else
                      #sub =  #(#[SUBSCRIPT,"Subscript"], #arrExpr1);
                    astPostExpr.addChild(#sub);
                  }
                  else {
		      	    #sub = #(#[SUBSCRIPT,"Subscript"], #arrExpr1);
                    #astPostExpr = #(#[REFERENCE,"Array Reference"], #astPostExpr, #sub);
                    if (#arrExpr2 != null)
		      	      #sub = #(#[SUBSCRIPT,"Subscript"], #arrExpr2);
                    else
                      #sub =  #(#[SUBSCRIPT,"Subscript"], #arrExpr1);
                    astPostExpr.addChild(#sub);
                  }
                }

// causes nondeterminism warnings
// 	        | AT at_expr:expression
//                 // a@N => (Array Reference a (Subscript N) (Subscript N))
//                 {
// 			      ExprAST sub = null;
// 		      	  #sub = #(#[SUBSCRIPT,"Subscript"], #[DECIMALINT,"0"]);
//                   #astPostExpr = #(#[REFERENCE,"Array Reference"], #astPostExpr, #sub);
//                   // allow for 0 origin lower bound
//                   #at_expr.setText(new String(Integer.toString(Integer.parseInt(#at_expr.getText()) - 1)));
// 		      	  #sub = #(#[SUBSCRIPT,"Subscript"], #at_expr);
//                   astPostExpr.addChild(#sub);
//                 }

            |   DOT!
                (   tb:TAB
                    {
                        bTabPressed = true;
                        astDotExpr = #tb; 
                    }
                |   id_expr1:id_expression
                    { astDotExpr = #id_expr1;}
                )
                // a.b.c => (Class Reference a b c))
                {
				  if (astPostExpr.getFirstChild() != null) {
		            if (#astDotExpr.getText().endsWith("\t") == false)
                      astPostExpr.addChild(#astDotExpr); 
		          }
                  else {
		     	    if (#astDotExpr.getText().endsWith("\t") == false)
		       		  #astPostExpr = #(#[REFERENCE,"Class Reference"], #astPostExpr, #astDotExpr);
		     		else
                      #astPostExpr = #(#[REFERENCE,"Class Reference"], #astPostExpr);
		   		  }
                }
            |   POINTERTO id_expr2:id_expression
                { astPostExpr = #(POINTERTO, #astPostExpr, #id_expr2); }
            |   PLUSPLUS
                { astPostExpr = #(PLUSPLUS, #astPostExpr); }
            |   MINUSMINUS
                { astPostExpr = #(MINUSMINUS, #astPostExpr); }
            )*
        )
        { 
            ## = #astPostExpr; 
        }
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

id_expression
    :   IDENT
    ;

tid_expression
    :   TAB_IDENT 
    ;

/*----------------------------------------------------------------------------
   * The Lexer
   *----------------------------------------------------------------------------*/

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
options {testLiterals = true;}
    :   ('$')*('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
;

/**
  *  A <TAB> token is returned not only on regular tabs
  *  but also when a TAB is hit after an incomplete variable
  */
protected
TAB
    :   (IDENT)?'\t'
    ;

TAB_IDENT 
    :   ((IDENT)'\t')=>TAB {$setType(TAB);}
    |   ('\t')=>TAB {$setType(TAB);}
    |   IDENT {$setType(IDENT);}
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
		|	('1'..'9') ('0'..'9')*  {_ttype = DECIMALINT;}
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
