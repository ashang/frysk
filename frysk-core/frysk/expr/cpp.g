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

    import java.util.ArrayList;
    import frysk.value.ArithmeticType;
    import frysk.value.Value;
    import frysk.debuginfo.DebugInfoFrame;
    import javax.naming.NameNotFoundException;
    import frysk.value.InvalidOperatorException;
    import frysk.value.OperationNotDefinedException;
    import inua.eio.ByteOrder;
    import lib.dwfl.BaseTypes;
}

class CppParser extends Parser;

options {
    defaultErrorHandler=false;
    buildAST=true;
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

    protected CppParser(TokenStream lexer, String sInput)
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
start throws TabException
    :   expressionList ETX
    ;

/**
  *  This rule looks for comma separated expressions.
  */
expressionList throws TabException
    :   expression (COMMA! expression)*
        {#expressionList = #(#[EXPR_LIST,"Expr list"], expressionList);}
        ;

expression! throws TabException
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
assign_op  throws TabException 
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
assignment_expression! throws TabException 
    :   (c:conditional_expression
            (a:assign_op   r:remainder_expression)?)
        {
            if (#a != null)
## = #(#a, #c, #r);
            else
## = #c;
        }
    ;

remainder_expression throws TabException 
    :   ((conditional_expression 
                (COMMA|SEMICOLON|RPAREN))=>
            {assign_stmt_RHS_found += 1;}

            assignment_expression
            {
                if (assign_stmt_RHS_found > 0)
                  assign_stmt_RHS_found -= 1;
                else
                {
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
conditional_expression! throws TabException 
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

logical_or_expression throws TabException 
    :	logical_and_expression (OR^ logical_and_expression)* 
    ;

logical_and_expression throws TabException 
    :	inclusive_or_expression (AND^ inclusive_or_expression)* 
    ;

inclusive_or_expression throws TabException 
    :	exclusive_or_expression (BITWISEOR^ exclusive_or_expression)*
    ;

exclusive_or_expression throws TabException 
    :   and_expression (BITWISEXOR^ and_expression)*
    ;

and_expression throws TabException 
    :   equality_expression (AMPERSAND^  equality_expression)*
    ;

equality_expression throws TabException 
    :   relational_expression ((NOTEQUAL^ | EQUAL^) relational_expression)*
    ;

relational_expression throws TabException 
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

shift_expression throws TabException 
    :   additive_expression ((SHIFTLEFT^ | SHIFTRIGHT^) additive_expression)*
    ;

additive_expression throws TabException 
    :   multiplicative_expression
        (options	{warnWhenFollowAmbig = false;}:
            (PLUS^ | MINUS^) multiplicative_expression
        )*
    ;

multiplicative_expression throws TabException 
    :	unary_expression
        (options{warnWhenFollowAmbig = false;}:
            (STAR^ | DIVIDE^ | MOD^) unary_expression
        )*
    ;
 
unary_expression throws TabException 
    :   PLUS^ unary_expression
    |   MINUS^ unary_expression
    |   PLUSPLUS^ pm_expression
    |   MINUSMINUS^ pm_expression
    |   TILDE^ unary_expression
    |   NOT^ unary_expression
    |   unary_expression_simple
    ;

unary_expression_simple throws TabException 
    :   AMPERSAND prim_expr: id_expression
        {
## = #([ADDRESS_OF, "Address Of"], #prim_expr); 
        }
    |   STAR mem_expr: id_expression
        {
## = #([MEMORY, "Memory"], #mem_expr); 
        }
    |   cast_expression
//    |   pm_expression selector* (PLUSPLUS |MINUSMINUS)?
    | pm_expression
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

cast_expression! throws TabException 
    :  LPAREN type:primitiveType RPAREN expr:unary_expression
//       | LPAREN (expression) RPAREN unary_expression_simple
//       |  LPAREN (expression | primitiveType) RPAREN unary_expression_simple
        {
## = #([CAST, "Cast"], #type, #expr);
        }
	;

pm_expression throws TabException 
{String sTabText;}
    :	post_expr1:primary_expression 
        {
          if (bTabPressed)
            {
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
  *  TabException is raised everytime the TAB is pressed.
  *  The parser thus bails out immediately and returns the
  *  parse tree constructed so far.
  */
/* ??? add (id_expression | (TAB {bTabPressed = true;})) */

variable! throws TabException 
{
    AST astPostExpr = null, astDotExpr = null;
} 
    :   (   prim_expr: id_expression
            {
                astPostExpr = #prim_expr;
            }

            (   options {warnWhenFollowAmbig = false;}

            : LPAREN (expr2:expressionList)? RPAREN
                { astPostExpr = #([FUNC_CALL, "FuncCall"], #astPostExpr, #expr2); }
	        | LSQUARE expr1:expression RSQUARE
                // a[b][c] => (Array Reference a (Subscript b) (Subscript c))
                {AST sub = null;
		 		 if (astPostExpr.getFirstChild() != null) {
		      	    #sub = #(#[SUBSCRIPT,"Subscript"], #expr1);
                    astPostExpr.addChild(#sub);
		    	 }
                 else {
		      	    #sub = #(#[SUBSCRIPT,"Subscript"], #expr1);
                    #astPostExpr = #(#[REFERENCE,"Array Reference"], #astPostExpr, #sub);
                 }
                }

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
                {if (astPostExpr.getFirstChild() != null) {
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
                {astPostExpr = #(PLUSPLUS, #astPostExpr); }
            |   MINUSMINUS
                {astPostExpr = #(MINUSMINUS, #astPostExpr); }
            )*
        )
        { 
## = #astPostExpr; 
        }
    ;

/**
  *	The TAB over here is not part of the C++ grammar.
  *	This enables auto-completion by allowing the user
  *	to press TAB whenever auto-completion is required
  */
primary_expression throws TabException 
    :   (TAB {bTabPressed = true;}
	    | variable)
    |   constant
    |   "this"
    |   LPAREN! expression RPAREN!
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

class CppLexer extends Lexer;

options {
    charVocabulary = '\0'..'\377';
    testLiterals=true;    
    k=3;
}

tokens
{
    OPERATOR = "operator";
}

/* Operators: */

ASSIGNEQUAL     : '=' ;
COLON           : ':' ;
COMMA           : ',' ;
QUESTIONMARK    : '?' ;
SEMICOLON       : ';' ;
POINTERTO       : "->";


ETX	    : '\3'  ;
LPAREN    : '('   ;
RPAREN    : ')'   ;

LSQUARE   : '[' ;
RSQUARE   : ']' ;

LCURLY    : '{' ;
RCURLY    : '}' ;

AT : '@' ;

EQUAL			: "==" ;
NOTEQUAL		: "!=" ;
LESSTHANOREQUALTO     : "<=" ;
LESSTHAN              : "<" ;
GREATERTHANOREQUALTO  : ">=" ;
GREATERTHAN           : ">" ;

DIVIDE          : '/' ;
DIVIDEEQUAL     : "/=" ;
PLUS            : '+' ;
PLUSEQUAL       : "+=" ;
PLUSPLUS        : "++" ;
MINUS           : '-' ;
MINUSEQUAL      : "-=" ;
MINUSMINUS      : "--" ;
STAR            : '*' ;
TIMESEQUAL      : "*=" ;
MOD             : '%' ;
MODEQUAL        : "%=" ;
SHIFTRIGHT      : ">>" ;
SHIFTRIGHTEQUAL : ">>=" ;
SHIFTLEFT       : "<<" ;
SHIFTLEFTEQUAL  : "<<=" ;

AND		  : "&&" ;
NOT		  : '!' ;
OR		  : "||" ;

AMPERSAND       : '&' ;
BITWISEANDEQUAL : "&=" ;
TILDE           : '~' ;
BITWISEOR       : '|'	  ;
BITWISEOREQUAL  : "|="  ;
BITWISEXOR      : '^'	  ;
BITWISEXOREQUAL : "^="  ;

protected
ELLIPSIS  : "..." ;

SCOPE           : "::"  ;

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


class CppTreeParser extends TreeParser;

options {
    importVocab=CppParser;
}

{
    ArrayList      refList;
    ArithmeticType arithmeticType;
    ArithmeticType longType;
    ArithmeticType intType;
    ArithmeticType shortType;
    ArithmeticType doubleType;
    ArithmeticType floatType;
    private CppSymTab cppSymTabRef;
    private DebugInfoFrame frame;
    public CppTreeParser(int intSize, DebugInfoFrame frame, CppSymTab symTab) {
        this();
	    cppSymTabRef = symTab; 
        this.frame = frame;
        shortType = new ArithmeticType(intSize / 2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeShort, "short");
        intType = new ArithmeticType(intSize, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeInteger, "int");
        longType = new ArithmeticType(intSize * 2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeLong, "long");
        floatType = new ArithmeticType(intSize, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeFloat, "float");
        doubleType = new ArithmeticType(intSize * 2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeDouble, "double");
    }
}


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

identifier returns [String idSpelling=null]
    :   ident:IDENT  {idSpelling=ident.getText();} ;

references returns [ArrayList el = null;]
	throws  InvalidOperatorException, 
		OperationNotDefinedException,
		NameNotFoundException
    {   refList = new ArrayList();}
    :   (subscript_or_member)* {el=refList;};

subscript_or_member returns [String id=null;] 
	throws  InvalidOperatorException, 
		OperationNotDefinedException,
		NameNotFoundException
    {   Value s; }
    :   #(SUBSCRIPT s=expr) {refList.add(new Integer(s.intValue()).toString());}
    |    id=identifier {refList.add(id);};

expr returns [Value returnVar=null] 
	throws  InvalidOperatorException, 
		OperationNotDefinedException,
		NameNotFoundException
{ Value v1, v2, log_expr; String s1; ArrayList el;}
    :   #(PLUS  v1=expr v2=expr)  {	returnVar = v1.getType().add(v1, v2);  }
    |   ( #(MINUS expr expr) )=> #(MINUS v1=expr v2=expr) 
        { returnVar = v1.getType().subtract(v1, v2);  }
    |   #(MINUS v1=expr ) 
        { returnVar = ArithmeticType.newIntegerValue(intType, 0);
          returnVar = returnVar.getType().subtract(returnVar, v1); }
    |   ( #(STAR expr expr) )=> #(STAR  v1=expr v2=expr)  
	{ returnVar = v1.getType().multiply(v1, v2); }
    |   #(MEMORY s1=identifier )
	{ returnVar = ArithmeticType.newLongValue(longType, (long)0);
          returnVar = (Value)cppSymTabRef.getMemory(frame, s1); }
    |   #(DIVIDE  v1=expr v2=expr)  { returnVar = v1.getType().divide(v1, v2); }
    |   #(MOD  v1=expr v2=expr)  {	returnVar = v1.getType().mod(v1, v2);  }
    |   #(SHIFTLEFT  v1=expr v2=expr)  {	
            if((v1.getType().getTypeId() < BaseTypes.baseTypeByte && v1.getType().getTypeId() > BaseTypes.baseTypeLong) ||
                (v2.getType().getTypeId() < BaseTypes.baseTypeByte && v2.getType().getTypeId() > BaseTypes.baseTypeLong)) {

                throw new OperationNotDefinedException("binary operator << not defined for types " + 
                    v1.getType().getName() + " and " + v2.getType().getName());
            }

            returnVar = v1.getType().shiftLeft(v1, v2);  }
    |   #(SHIFTRIGHT  v1=expr v2=expr)  {	
            if((v1.getType().getTypeId() < BaseTypes.baseTypeByte && v1.getType().getTypeId() > BaseTypes.baseTypeLong) ||
                (v2.getType().getTypeId() < BaseTypes.baseTypeByte && v2.getType().getTypeId() > BaseTypes.baseTypeLong)) {

                throw new OperationNotDefinedException("binary operator >> not defined for types " + 
                    v1.getType().getName() + " and " + v2.getType().getName());
            }

            returnVar = v1.getType().shiftRight(v1, v2);  }
    |   #(LESSTHAN  v1=expr v2=expr)  { returnVar = v1.getType().lessThan(v1, v2);  }

    |   #(GREATERTHAN  v1=expr v2=expr)  { returnVar = v1.getType().greaterThan(v1, v2);  }

    |   #(LESSTHANOREQUALTO  v1=expr v2=expr)  { returnVar = v1.getType().lessThanOrEqualTo(v1, v2);  }

    |   #(GREATERTHANOREQUALTO  v1=expr v2=expr)  { returnVar = v1.getType().greaterThanOrEqualTo(v1, v2);  }

    |   #(NOTEQUAL  v1=expr v2=expr)  { returnVar = v1.getType().notEqual(v1, v2);  }

    |   #(EQUAL  v1=expr v2=expr)  { returnVar = v1.getType().equal(v1, v2);  }
    |   ( #(AMPERSAND expr expr) )=>#(AMPERSAND  v1=expr v2=expr)  
	{ returnVar = v1.getType().bitWiseAnd(v1, v2);  }
    |   #(ADDRESS_OF s1=identifier )
	{ returnVar = ArithmeticType.newLongValue(longType, (long)0);
          returnVar = (Value)cppSymTabRef.getAddress(frame, s1); }
    |   #(BITWISEXOR  v1=expr v2=expr)  { returnVar = v1.getType().bitWiseXor(v1, v2);  }
    |   #(BITWISEOR  v1=expr v2=expr)  { returnVar = v1.getType().bitWiseOr(v1, v2);  }
    |   #(AND  v1=expr v2=expr)  { returnVar = v1.getType().logicalAnd(v1, v2);  }
    |   #(OR  v1=expr v2=expr)  { returnVar = v1.getType().logicalOr(v1, v2);  }
    |   #(NOT  v1=expr)  { returnVar = v1.getType().logicalNegation(v1);  }
    |   #(TILDE v1=expr)  { returnVar = v1.getType().bitWiseComplement(v1);  }
    |   #(COND_EXPR  log_expr=expr v1=expr v2=expr)  { 
            returnVar = ((log_expr.getType().getLogicalValue(log_expr)) ? v1 : v2);  
        }
    |   o:OCTALINT  {
    	    char c = o.getText().charAt(o.getText().length() - 1);
    	    int l = o.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar = ArithmeticType.newLongValue (
                longType, Long.parseLong(o.getText().substring(1, l), 8));
        }
    |   i:DECIMALINT  {
    	    char c = i.getText().charAt(i.getText().length() - 1);
    	    int l = i.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar = ArithmeticType.newLongValue (
                longType, Long.parseLong(i.getText().substring(0, l)));
        }
    |   h:HEXADECIMALINT  {
    	    char c = h.getText().charAt(h.getText().length() - 1);
    	    int l = h.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar = ArithmeticType.newLongValue (
                longType, Long.parseLong(h.getText().substring(2, l), 16));
        }
    |   f:FLOAT  {
    	    char c = f.getText().charAt(f.getText().length() - 1);
    	    int l = f.getText().length();
    	    if (c == 'f' || c == 'F' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar = ArithmeticType.newFloatValue (
                floatType, Float.parseFloat(f.getText().substring(0, l)));
        }
    |   d:DOUBLE  {
    	    char c = d.getText().charAt(d.getText().length() - 1);
    	    int l = d.getText().length();
    	    if (c == 'f' || c == 'F' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar = ArithmeticType.newDoubleValue (
                doubleType, Double.parseDouble(d.getText().substring(0, l)));
        }
    |   #(ASSIGNEQUAL v1=expr v2=expr)  {
            v1.getType().assign(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(TIMESEQUAL v1=expr v2=expr)  {
            v1.getType().timesEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(DIVIDEEQUAL v1=expr v2=expr)  {
            v1.getType().divideEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(MINUSEQUAL v1=expr v2=expr)  {
            v1.getType().minusEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(PLUSEQUAL v1=expr v2=expr)  {
            v1.getType().plusEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(MODEQUAL v1=expr v2=expr)  {
            v1.getType().modEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(SHIFTLEFTEQUAL v1=expr v2=expr)  {
            v1.getType().shiftLeftEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(SHIFTRIGHTEQUAL v1=expr v2=expr)  {
            v1.getType().shiftRightEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(BITWISEANDEQUAL v1=expr v2=expr)  {
            v1.getType().bitWiseAndEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(BITWISEXOREQUAL v1=expr v2=expr)  {
            v1.getType().bitWiseXorEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(BITWISEOREQUAL v1=expr v2=expr)  {
            v1.getType().bitWiseOrEqual(v1, v2);
            returnVar = v1;
            cppSymTabRef.put(frame, v1.getTextFIXME(), v1);
        }
    |   #(CAST pt:primitiveType v2=expr) { 
	    if(pt.getText().compareTo("long") == 0) {
	      returnVar = ArithmeticType.newLongValue(longType, (long)0);
              returnVar.getType().assign(returnVar, v2);
	      }
	    else if(pt.getText().compareTo("int") == 0) {
	      returnVar = ArithmeticType.newIntegerValue(intType, (int)0);
              returnVar.getType().assign(returnVar, v2);
	      }
	    else if(pt.getText().compareTo("short") == 0) {
	      returnVar = ArithmeticType.newShortValue(shortType, (short)0);
              returnVar.getType().assign(returnVar, v2);
	      }
	    else if(pt.getText().compareTo("double") == 0) {
	      returnVar = ArithmeticType.newDoubleValue(doubleType, (double)0);
              returnVar.getType().assign(returnVar, v2);
	      }
	    else if(pt.getText().compareTo("float") == 0) {
	      returnVar = ArithmeticType.newFloatValue(floatType, (float)0);
              returnVar.getType().assign(returnVar, v2);
	      }
	    else returnVar = v2;
        }
    |   #(EXPR_LIST v1=expr)  { returnVar = v1; }
    |   #(FUNC_CALL v1=expr v2=expr)  { returnVar = v1; }
    |   #(REFERENCE el=references) {
          returnVar = (Value)cppSymTabRef.get(frame, el);
          }
    |   ident:IDENT  {
            if((returnVar = ((Value)cppSymTabRef.get(frame, ident.getText()))) == null
		&& cppSymTabRef.putUndefined()) {
                returnVar = ArithmeticType.newIntegerValue(intType, 0);
                cppSymTabRef.put(null, ident.getText(), returnVar);
            }
        }
    |   tident:TAB_IDENT  {
            if((returnVar = ((Value)cppSymTabRef.get(frame, tident.getText()))) == null
		&& cppSymTabRef.putUndefined()) {
                returnVar = ArithmeticType.newIntegerValue(intType, 0);
                cppSymTabRef.put(null, tident.getText(), returnVar);
            }
        }
    ;
