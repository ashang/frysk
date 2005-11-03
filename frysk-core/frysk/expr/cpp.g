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


  header
  {
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

    import frysk.lang.*;
    import java.util.*;
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
    private String sInputExpression;

    protected CppParser(TokenStream lexer, String sInput)
    {
      this(lexer);
      bTabPressed = false;
      sInputExpression = sInput;
    }
  }

 /** 
  * These tokens are never returned by the Lexer. These are imaginary
  * tokens used as root nodes in the generated Abstract Syntax Tree.
  */
  imaginaryTokenDefinitions
  :
    APPLY
    ARG_LIST
    EXPR_LIST
    COND_EXPR
    ARRAY_REF
  ;

   /** 
    * The start rule simply expects an expression list following by
    * the end of text symbol (ETX \3).
    *
    * The TabException propagates all the way up, to the start rule,
    * which then propagates it up to the calling program.
    */
  start throws TabException
  :
    expression ETX
  ;

  /**
   *  This rule looks for comma separated expressions.
   */
  expression! throws TabException
  :
    assign_expr1: assignment_expression (comma:COMMA assign_expr2:assignment_expression)*
    {
     /** 
      * The following syntax is used for generation of ASTs. The # before a symbol refers
      * to the AST for that symbol. ## refers to the AST returned by this rule (expression)
      */

      if(#comma!=null)
	## = #([EXPR_LIST, "ExpressionList"], #assign_expr1, #assign_expr2);
      else
	## = #assign_expr1;
    }
  ;

  /**
   *  Various types of assignment operators like +=, *=, /= etc.
   */
  assign_op  throws TabException 
  :
    ASSIGNEQUAL^
  |
    TIMESEQUAL^
  |
    DIVIDEEQUAL^
  |
    MINUSEQUAL^
  |
    PLUSEQUAL^
  |
    MODEQUAL^
  |
    SHIFTLEFTEQUAL^
  |
    SHIFTRIGHTEQUAL^
  |
    BITWISEANDEQUAL^
  |
    BITWISEXOREQUAL^
  |
    BITWISEOREQUAL^
  ;

  /**
   *  Assignment expressions of the form "expr1 = expr2 = expr3".
   *  Notice that the operator can by any assignment operator.
   */
  assignment_expression! throws TabException 
  :
    (c:conditional_expression
      (a:assign_op   r:remainder_expression)?)
    {
      if(#a != null)
	## = #(#a, #c, #r);
      else
	## = #c;
    }
  ;

  remainder_expression throws TabException 
  :
    ((conditional_expression 
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
    |	
      assignment_expression
    )
  ;

  /**
   *  Conditional expressions of the form
   *  (logical_expr)?expr:expr
   */
  conditional_expression! throws TabException 
  :
    log_or_expr:logical_or_expression
    (ques:QUESTIONMARK expr:expression colon:COLON cond_expr:conditional_expression)?
    {
      if(ques != null)
	## = #([COND_EXPR, "ConditionalExpression"], #log_or_expr, #expr, #cond_expr);
      else
	## = #log_or_expr;
    }
  ;

  logical_or_expression throws TabException 
  :	
    logical_and_expression (OR^ logical_and_expression)* 
  ;

  logical_and_expression throws TabException 
  :	
    inclusive_or_expression (AND^ inclusive_or_expression)* 
  ;

  inclusive_or_expression throws TabException 
  :	
    exclusive_or_expression (BITWISEOR^ exclusive_or_expression)*
  ;

  exclusive_or_expression throws TabException 
  :
    and_expression (BITWISEXOR^ and_expression)*
  ;

  and_expression throws TabException 
  :
    equality_expression (AMPERSAND^  equality_expression)*
  ;

  equality_expression throws TabException 
  :
    relational_expression ((NOTEQUAL^ | EQUAL^) relational_expression)*
  ;

  relational_expression throws TabException 
  :
    shift_expression
    (options {warnWhenFollowAmbig = false;}:
      (	
	LESSTHAN^
      |	
	GREATERTHAN^
      |	
	LESSTHANOREQUALTO^
      |	
	GREATERTHANOREQUALTO^
      )
      shift_expression
    )*
  ;

  shift_expression throws TabException 
  :
    additive_expression ((SHIFTLEFT^ | SHIFTRIGHT^) additive_expression)*
  ;

  additive_expression throws TabException 
  :
    multiplicative_expression
      (options	{warnWhenFollowAmbig = false;}:
	(PLUS^ | MINUS^) multiplicative_expression
      )*
  ;

  multiplicative_expression throws TabException 
  :	
    pm_expression
      (options{warnWhenFollowAmbig = false;}:
	      (STAR^ | DIVIDE^ | MOD^) pm_expression
      )*
  ;

  pm_expression throws TabException 
  {String sTabText;}
  :	
    post_expr1:postfix_expression 
    {
      if(bTabPressed)
      {
	if(#post_expr1.getFirstChild() != null)
	  sTabText = #post_expr1.getFirstChild().getNextSibling().getText();
	else 
	  sTabText = #post_expr1.getText();

	throw new TabException(#post_expr1, sTabText);
      }
    }

    ( (DOTMBR^ | POINTERTOMBR^) 
      post_expr2:postfix_expression
      {
	if(bTabPressed)
	{
	  if(#post_expr1.getFirstChild() != null)
	    sTabText = #post_expr1.getFirstChild().getNextSibling().getText();
	  else 
	    sTabText = #post_expr1.getText();

	  throw new TabException(#post_expr1, sTabText);
	}
      }
    )*
  ;

  /**
   *  TabException is raised everytime the TAB is pressed.
   *  The parser thus bails out immediately and returns the
   *  parse tree constructed so far.
   */
  postfix_expression! throws TabException 
  {
    AST astPostExpr = null, astDotExpr = null;
  } 
  :
    (
      prim_expr: primary_expression
      {
	astPostExpr = #prim_expr;
      }

      (
       options {warnWhenFollowAmbig = false;}:

	LSQUARE expr1:expression RSQUARE
	{	astPostExpr = #([ARRAY_REF, "ArrayReference"], #astPostExpr, #expr1); }
      |
	LPAREN (expr2:expression)? RPAREN
	{	astPostExpr = #([APPLY, "FuncCall"], #astPostExpr, #expr2); }
      |
	DOT
	(
	  tb:TAB
	  {
	    bTabPressed = true;
	    astDotExpr = #tb; 
	  }
	| 
	  id_expr1:id_expression
	  { astDotExpr = #id_expr1;}
	)
	{astPostExpr = #(DOT, #astPostExpr, #astDotExpr); }
      |
	POINTERTO id_expr2:id_expression
	{ astPostExpr = #(POINTERTO, #astPostExpr, #id_expr2); }
      |
	PLUSPLUS
	{astPostExpr = #(PLUSPLUS, #astPostExpr); }
      |
	MINUSMINUS
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
  :
    (id_expression | (TAB {bTabPressed = true;}))
  |
    constant
  |
    "this"
  |
    LPAREN! expression RPAREN!
  ;

  constant
  :
    OCTALINT
  |
    DECIMALINT
  |
    HEXADECIMALINT
  |
    CharLiteral
  |
    (StringLiteral)+
  |
    FLOATONE
  |
    FLOATTWO
  |
    "true"
  |
    "false"
  ;

  id_expression
  :
    IDENT 
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
  DOT	    : '.'   ;

  protected
  ELLIPSIS  : "..." ;

  POINTERTOMBR    : "->*" ;
  DOTMBR          : ".*"  ;

  SCOPE           : "::"  ;

  protected
  IDENT
  options {testLiterals = true;}
  : 
    ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
  ;

  /**
   *  A <TAB> token is returned not only on regular tabs
   *  but also when a TAB is hit after an incomplete variable
   */
  protected
  TAB
    :
      (IDENT)?'\t'
    ;

  TAB_IDENT 
    :   
      ((IDENT)'\t')=>TAB {$setType(TAB);}
    | 
      ('\t')=>TAB {$setType(TAB);}
    | 
      IDENT {$setType(IDENT);}
    ;

  protected
  NL
    :
      '\r' ('\n')?  // DOS/Windows
    | 
      '\n'    // Unix
      { newline(); }
    ;

  WS
    :	
    ( 
      ' '
    | 
      '\f'
    | 
      NL    
    |
      '\\'
      (
	'\r' ('\n')?
      |
	'\n'
      )
    )
    { $setType(Token.SKIP); }
    ; 

  CharLiteral
  :	
    '\'' (Escape | ~( '\'' )) '\''
  ;

  StringLiteral
  :	
    '"'
    ( 
      Escape
    |	
      (	
	"\\\r\n"   // MS 
      |	
	"\\\r"     // MAC
      |	
	"\\\n"     // Unix
      )
    |	
      ~('"' | '\r' | '\n' | '\\')
    )*
    '"'
  ;

  protected
  Escape  
  :	
    '\\'
    ( options {warnWhenFollowAmbig=false;}:
      'a'
    | 
      'b'
    | 
      'f'
    | 
      'n'
    | 
      'r'
    | 
      't'
    | 
      'v'
    | 
      '"'
    | 
      '\''
    | 
      '\\'
    | 
      '?'
    | 
      ('0'..'3') 
      (options	{warnWhenFollowAmbig=false;}
       : Digit (options {warnWhenFollowAmbig=false;}  : Digit)? )?
    | 
      ('4'..'7') (options{warnWhenFollowAmbig=false;}: Digit)?
    | 
      'x' (options{warnWhenFollowAmbig=false;}: Digit | 'a'..'f' | 'A'..'F')+
    )
  ;

  /* Numeric Constants: */

  protected
  Digit
  :	
    '0'..'9'
  ;

  protected
  Decimal
  :	
    ('0'..'9')+
  ;

  protected
  LongSuffix
  :	
    'l'
  |
    'L'
  ;

  protected
  UnsignedSuffix
  :	
    'u'
  |	
    'U'
  ;

  protected
  FloatSuffix
  :	
    'f'
  |	
    'F'
  ;

  protected
  Exponent
  :
    ('e' | 'E') ('+' | '-')? (Digit)+
  ;

  protected
  Vocabulary
  :	
    '\3'..'\377'
  ;

  NUM
  :	
    ( (Digit)+ ('.' | 'e' | 'E') )=> 
      (Digit)+
	  ( 
	      '.' (Digit)* (Exponent)? {_ttype = FLOATONE;} //Zuo 3/12/01
	    | 
	      Exponent                 {_ttype = FLOATTWO;} //Zuo 3/12/01
	  )                          //{_ttype = DoubleDoubleConst;}
	  (
	    FloatSuffix               //{_ttype = FloatDoubleConst;}
	  |
	    LongSuffix                //{_ttype = LongDoubleConst;}
	  )?

  |	
    ("...")=> "..."            {_ttype = ELLIPSIS;}

  |
    '.'                     {_ttype = DOT;}
    ((Digit)+ (Exponent)?   {_ttype = FLOATONE;} //Zuo 3/12/01
	    (
	      FloatSuffix           //{_ttype = FloatDoubleConst;}
	    |
	      LongSuffix            //{_ttype = LongDoubleConst;}
	    )?
    )?

  |
    ('0' ('0'..'7'))=>
      '0' ('0'..'7')*            //{_ttype = IntOctalConst;}
      (
	LongSuffix                //{_ttype = LongOctalConst;}
      |
	UnsignedSuffix            //{_ttype = UnsignedOctalConst;}
      )*                         {_ttype = OCTALINT;}

  |	
    ('0' ('x' | 'X'))=>
    '0' ('x' | 'X') ('a'..'f' | 'A'..'F' | Digit)+
			     //{_ttype = IntHexConst;}
      (
	LongSuffix                //{_ttype = LongHexConst;}
      |
	UnsignedSuffix            //{_ttype = UnsignedHexConst;}
      )*                         {_ttype = HEXADECIMALINT;}   
  |	
    ('0' | ('1'..'9' (Digit)*))          //{_ttype = IntIntConst;}
      (
	LongSuffix                //{_ttype = LongIntConst;}
      |
	UnsignedSuffix            //{_ttype = UnsignedIntConst;}
      )*                         {_ttype = DECIMALINT;}  

  ;

  /*----------------------------------------------------------------------------
  * The Tree Parser/Walker (evaluator)
  *---------------------------------------------------------------------------*/

  class CppTreeParser extends TreeParser;

  options {
    importVocab=CppParser;
  }

  {
    IntegerType intType;
    ShortType shortType;
    Map symTab;
    public CppTreeParser(int intSize, int shortSize, Map _symTab) {
      this();
      symTab = _symTab;
      intType = new IntegerType(intSize, Endian.BIG_ENDIAN);
      shortType = new ShortType(shortSize, Endian.LITTLE_ENDIAN);
    }
  }

  
  expr returns [Variable returnVar=null] throws InvalidOperatorException, OperationNotDefinedException
  { Variable v1, v2, log_expr;}
  :
    #(PLUS  v1=expr v2=expr)  {	returnVar = v1.getType().add(v1, v2);  }
  |
    #(MINUS  v1=expr v2=expr)  {  returnVar = v1.getType().subtract(v1, v2);  }
  |
    #(STAR  v1=expr v2=expr)  {	returnVar = v1.getType().multiply(v1, v2);  }
  |
    #(DIVIDE  v1=expr v2=expr)  { returnVar = v1.getType().divide(v1, v2);  }
  |
    #(MOD  v1=expr v2=expr)  {	returnVar = v1.getType().mod(v1, v2);  }
  |
    #(SHIFTLEFT  v1=expr v2=expr)  {	
      if((v1.getType().getTypeId() < BaseTypes.baseTypeChar && v1.getType().getTypeId() > BaseTypes.baseTypeLong) ||
	  (v2.getType().getTypeId() < BaseTypes.baseTypeChar && v2.getType().getTypeId() > BaseTypes.baseTypeLong)) {

	throw new OperationNotDefinedException("binary operator << not defined for types " + 
	    v1.getType().getName() + " and " + v2.getType().getName());
      }

      returnVar = v1.getType().shiftLeft(v1, v2);  }
  |
    #(SHIFTRIGHT  v1=expr v2=expr)  {	
      if((v1.getType().getTypeId() < BaseTypes.baseTypeChar && v1.getType().getTypeId() > BaseTypes.baseTypeLong) ||
	  (v2.getType().getTypeId() < BaseTypes.baseTypeChar && v2.getType().getTypeId() > BaseTypes.baseTypeLong)) {

	throw new OperationNotDefinedException("binary operator >> not defined for types " + 
	    v1.getType().getName() + " and " + v2.getType().getName());
      }

      returnVar = v1.getType().shiftRight(v1, v2);  }
  |
    #(LESSTHAN  v1=expr v2=expr)  { returnVar = v1.getType().lessThan(v1, v2);  }

  |
    #(GREATERTHAN  v1=expr v2=expr)  { returnVar = v1.getType().greaterThan(v1, v2);  }

  |
    #(LESSTHANOREQUALTO  v1=expr v2=expr)  { returnVar = v1.getType().lessThanOrEqualTo(v1, v2);  }

  |
    #(GREATERTHANOREQUALTO  v1=expr v2=expr)  { returnVar = v1.getType().greaterThanOrEqualTo(v1, v2);  }

  |
    #(NOTEQUAL  v1=expr v2=expr)  { returnVar = v1.getType().notEqual(v1, v2);  }

  |
    #(EQUAL  v1=expr v2=expr)  { returnVar = v1.getType().equal(v1, v2);  }
  |
    #(AMPERSAND  v1=expr v2=expr)  { returnVar = v1.getType().bitWiseAnd(v1, v2);  }
  |
    #(BITWISEXOR  v1=expr v2=expr)  { returnVar = v1.getType().bitWiseXor(v1, v2);  }
  |
    #(BITWISEOR  v1=expr v2=expr)  { returnVar = v1.getType().bitWiseOr(v1, v2);  }
  |
    #(AND  v1=expr v2=expr)  { returnVar = v1.getType().logicalAnd(v1, v2);  }
  |
    #(OR  v1=expr v2=expr)  { returnVar = v1.getType().logicalOr(v1, v2);  }
  |
    #(COND_EXPR  log_expr=expr v1=expr v2=expr)  { 
      returnVar = ((log_expr.getType().getLogicalValue(log_expr)) ? v1 : v2);  
    }
  |
    i:DECIMALINT  {
      returnVar = IntegerType.newIntegerVariable(
	  intType, Integer.parseInt(i.getText()));
    }
  |
    #(ASSIGNEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().assign(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(TIMESEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().timesEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(DIVIDEEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().divideEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(MINUSEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().minusEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(PLUSEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().plusEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(MODEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().modEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(SHIFTLEFTEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().shiftLeftEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(SHIFTRIGHTEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().shiftRightEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(BITWISEANDEQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().bitWiseAndEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(BITWISEXOREQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().bitWiseXorEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(BITWISEOREQUAL v1=expr v2=expr)  {
      if(v1.getType().getTypeId() != v2.getType().getTypeId())
	v1 = v2.getType().newVariable(v2.getType(), v1);
      v1.getType().bitWiseOrEqual(v1, v2);
      returnVar = v1;
      symTab.put(v1.getText(), v1);
    }
  |
    #(EXPR_LIST v1=expr v2=expr)  { returnVar = v1; }
  |
    ident:IDENT  {
      if((returnVar = ((Variable)symTab.get(ident.getText()))) == null) {
	returnVar = IntegerType.newIntegerVariable(intType, ident.getText(), 0);
	symTab.put(ident.getText(), returnVar);
      }
    }
  ;

