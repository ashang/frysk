// This file is part of PyANTLR. See LICENSE.txt for license
// details..........Copyright (C) Wolfgang Haefelinger, 2004.
//
// $Id: action.g,v 1.1.1.1 2005/11/25 22:29:29 cagney Exp $

header {
package antlr.actions.python;
}

{
import java.io.StringReader;
import antlr.collections.impl.Vector;
import antlr.*;
}

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: action.g,v 1.1.1.1 2005/11/25 22:29:29 cagney Exp $
 */

/** Perform the following translations:

 AST related translations

   ##          -> currentRule_AST
   #(x,y,z)    -> codeGenerator.getASTCreateString(vector-of(x,y,z))
   #[x]        -> codeGenerator.getASTCreateString(x)
   #x          -> codeGenerator.mapTreeId(x)

   Inside context of #(...), you can ref (x,y,z), [x], and x as shortcuts.

 Text related translations

   $append(x)     -> self.text.append(x)
   $setText(x)    -> self.text.setLength(_begin) self.text.append(x)
   $getText       -> self.text.getString(_begin)
   $setToken(x)   -> _token = x
   $setType(x)    -> _ttype = x
   $FOLLOW(r)     -> FOLLOW set name for rule r (optional arg)
   $FIRST(r)      -> FIRST set name for rule r (optional arg)

   experimental:
   $newline, $nl  -> self.newline()
   $skip          -> _ttype = SKIP
 */

class ActionLexer extends Lexer;
options {
	k=3;
	charVocabulary='\3'..'\377';
	testLiterals=false;
	interactive=true;
}

{
	protected RuleBlock currentRule;
	protected CodeGenerator generator;
	protected int lineOffset = 0;
	private Tool antlrTool;	// The ANTLR tool
	ActionTransInfo transInfo;

 	public ActionLexer( 
        String s,
        RuleBlock currentRule,
        CodeGenerator generator,
        ActionTransInfo transInfo) 
    {
		this(new StringReader(s));
		this.currentRule = currentRule;
		this.generator = generator;
		this.transInfo = transInfo;
	}

	public void setLineOffset(int lineOffset) {
		// this.lineOffset = lineOffset;
		setLine(lineOffset);
	}

	public void setTool(Tool tool) {
		this.antlrTool = tool;
	}

	public void reportError(RecognitionException e)
	{
		antlrTool.error(
            "Syntax error in action: "+e,
            getFilename(),getLine(),getColumn());
	}

	public void reportError(String s)
	{
		antlrTool.error(s,getFilename(),getLine(),getColumn());
	}

	public void reportWarning(String s)
	{
		if ( getFilename()==null ) {
			antlrTool.warning(s);
		}
		else {
			antlrTool.warning(s,getFilename(),getLine(), getColumn());
		}
	}
}

// rules are protected because we don't care about nextToken().

public
ACTION
	:	(	STUFF
		|	AST_ITEM
		|	TEXT_ITEM
		)+
	;

// stuff in between #(...) and #id items
protected
STUFF
	:	COMMENT
	|	STRING
	|	CHAR
	|	"\r\n" 		{newline();}
	|	'\r' 		{newline();}
	|	'\n'		{newline();}
	|	'/'	~('/'|'*')	// non-comment start '/'
	|	~('/'|'\n'|'\r'|'$'|'#'|'"'|'\'')
	;

protected
AST_ITEM
	:	'#'! t:TREE
	|	'#'! id:ID
		{
		String idt = id.getText();
		String var = generator.mapTreeId(idt,transInfo);
		if ( var!=null ) {
			$setText(var);
		}
		}
		(WS)?
		( options {greedy=true;} : VAR_ASSIGN )?
	|	'#'! ctor:AST_CONSTRUCTOR
	|	"##"
		{
		String r=currentRule.getRuleName()+"_AST"; $setText(r);
		if ( transInfo!=null ) {
			transInfo.refRuleRoot=r;	// we ref root of tree
		}
		}
		(WS)?
		( options {greedy=true;} : VAR_ASSIGN )?
	;

protected
TEXT_ITEM
	:	"$append" (WS)? '(' a1:TEXT_ARG ')'
		{
			String t = "self.text.append("+a1.getText()+")";
			$setText(t);
		}
	|	"$set"
		(	"Text" (WS)? '(' a2:TEXT_ARG ')'
			{
			String t;
			t = "self.text.setLength(_begin) ; self.text.append("+a2.getText()+")";
			$setText(t);
			}
		|	"Token" (WS)? '(' a3:TEXT_ARG ')'
			{
			String t="_token = "+a3.getText();
			$setText(t);
			}
		|	"Type" (WS)? '(' a4:TEXT_ARG ')'
			{
			String t="_ttype = "+a4.getText();
			$setText(t);
			}
		)
	|	"$getText"
		{
			$setText("self.text.getString(_begin)");
		}
	|	"$FOLLOW" ( (WS)? '(' a5:TEXT_ARG ')' )?
		{
			String rule = currentRule.getRuleName();
			if ( a5!=null ) {
				rule = a5.getText();
			}
			String setName = generator.getFOLLOWBitSet(rule, 1);
			if ( setName==null ) {
				reportError("$FOLLOW("+rule+")"+
							": unknown rule or bad lookahead computation");
			}
			else {
				$setText(setName);
			}
		}
	|	"$FIRST" ( (WS)? '(' a6:TEXT_ARG ')' )?
		{
			String rule = currentRule.getRuleName();
			if ( a6!=null ) {
				rule = a6.getText();
			}
			String setName = generator.getFIRSTBitSet(rule, 1);
			if ( setName==null ) {
				reportError("$FIRST("+rule+")"+
							": unknown rule or bad lookahead computation");
			}
			else {
				$setText(setName);
			}
		}
	|	"$skip"
	        {
	            $setText("_ttype = SKIP");
	        }
	|	( "$nl" | "$newline" )
	        {
        	    $setText("self.newline()"); 
        	}
	;

protected
TREE!
{
	StringBuffer buf = new StringBuffer();
	int n=0;
	Vector terms = new Vector(10);
}
	:	'('
		(WS)?
		t:TREE_ELEMENT {terms.appendElement(t.getText());}
		(WS)?
		(	','	(WS)?
			t2:TREE_ELEMENT {terms.appendElement(t2.getText());}
			(WS)?
		)*
		{$setText(generator.getASTCreateString(terms));}
		')'
	;

protected
TREE_ELEMENT { boolean was_mapped; }
	:	'#'! TREE
	|	'#'! AST_CONSTRUCTOR
	|	'#'! was_mapped=id:ID_ELEMENT
		{	// RK: I have a queer feeling that this maptreeid is redundant
			if( ! was_mapped )
			{
				String t = generator.mapTreeId(id.getText(), null);
				$setText(t);
			}
		}
	|	"##"
		{String t = currentRule.getRuleName()+"_AST"; $setText(t);}
	|	TREE
	|	AST_CONSTRUCTOR
	|	ID_ELEMENT
	|	STRING
	;

protected
AST_CONSTRUCTOR!
	:	'[' (WS)? x:AST_CTOR_ELEMENT (WS)?
		(',' (WS)? y:AST_CTOR_ELEMENT (WS)? )?
		(',' (WS)? z:AST_CTOR_ELEMENT (WS)? )? ']'
		{
		String args = x.getText();
		if ( y!=null ) {
			args += ","+y.getText();
		}
		if ( z!=null ) {
			args += ","+z.getText();
		}
		$setText(generator.getASTCreateString(null,args));
		}
	;

/** The arguments of a #[...] constructor are text, token type,
 *  or a tree.
 */
protected
AST_CTOR_ELEMENT
	:	STRING
	|	INT
	|	TREE_ELEMENT
	;

/** An ID_ELEMENT can be a func call, array ref, simple var,
 *  or AST label ref.
 */
protected
ID_ELEMENT returns [boolean mapped=false]
	:	id:ID (options {greedy=true;}:WS!)?
		(	'(' (options {greedy=true;}:WS!)? ( ARG (',' (WS!)? ARG)* )? (WS!)? ')'	// method call
		|	( '[' (WS!)? ARG (WS!)? ']' )+				// array reference
		|	'.' ID_ELEMENT
		|	/* could be a token reference or just a user var */
			{
				mapped = true;
				String t = generator.mapTreeId(id.getText(), transInfo);
				$setText(t);
			}
			// if #rule referenced, check for assignment
			(	options {greedy=true;}
			:	{transInfo!=null && transInfo.refRuleRoot!=null}?
				(WS)? VAR_ASSIGN
			)?
		)
	;

protected
TEXT_ARG
	:	(WS)? ( TEXT_ARG_ELEMENT (options {greedy=true;}:WS)? )+
	;

protected
TEXT_ARG_ELEMENT
	:	TEXT_ARG_ID_ELEMENT
	|	STRING
	|	CHAR
	|	INT_OR_FLOAT
	|	TEXT_ITEM
	|	'+'
	;

protected
TEXT_ARG_ID_ELEMENT
	:	id:ID (options {greedy=true;}:WS!)?
		(	'(' (options {greedy=true;}:WS!)? ( TEXT_ARG (',' TEXT_ARG)* )* (WS!)? ')'	// method call
		|	( '[' (WS!)? TEXT_ARG (WS!)? ']' )+				// array reference
		|	'.' TEXT_ARG_ID_ELEMENT
		|
		)
	;

protected
ARG	:	(	TREE_ELEMENT
		|	STRING
		|	CHAR
		|	INT_OR_FLOAT
		)
		(options {greedy=true;} : (WS)? ( '+'| '-' | '*' | '/' ) (WS)? ARG )*
	;

protected
ID	:	('a'..'z'|'A'..'Z'|'_')
		(options {greedy=true;} : ('a'..'z'|'A'..'Z'|'0'..'9'|'_'))*
	;

protected
VAR_ASSIGN
	:	'='
		{
		// inform the code generator that an assignment was done to
		// AST root for the rule if invoker set refRuleRoot.
		if ( LA(1)!='=' && transInfo!=null && transInfo.refRuleRoot!=null ) {
			transInfo.assignToRoot=true;
		}
		}
	;


protected
COMMENT
	:	(SL_COMMENT	|	ML_COMMENT) {
        }
	;

protected
SL_COMMENT
	:   "//" {
            /* rewrite comment symbol */
            $setText("#");
        } 

        (
            options {greedy=false;}:.
        )* {
            // do nothing
        }

        ('\n'|"\r\n"|'\r')
		{
            newline();
        }
	;

protected
IGNWS
    : 
        (
            ' '
        |   '\t'
        )*
    ;

protected
ML_COMMENT 
    :
        "/*"
        {
            /* rewrite comment symbol */
            $setText("#");
        }
        
        (	
            options {greedy=false;}
        :	'\r' '\n' IGNWS!	{
                newline();
                $append("# ");
            }
        |	'\r' IGNWS! {
                newline();
                $append("# ");
            }
        |	'\n' IGNWS! {
                newline();
                $append("# ");
            }
        |	.
        )*
        {
            /* force a newline (MK: should actually be the same newline as
	     * was matched earlier in the block comment*/
            $append("\n");
        }
        "*/"!
	;

protected
CHAR :
	'\''
	( ESC | ~'\'' )
	'\''
	;

protected
STRING :
	'"'
	(ESC|~'"')*
	'"'
	;

protected
ESC	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	('0'..'3')
			(	options {greedy=true;}
			:	DIGIT
				(	options {greedy=true;}
				:	DIGIT
				)?
			)?
		|	('4'..'7') (options {greedy=true;}:DIGIT)?
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

protected
INT	:	(DIGIT)+
	;

protected
INT_OR_FLOAT
	:	(options {greedy=true;}:DIGIT)+
		(	options {greedy=true;}
		:	'.' (options {greedy=true;}:DIGIT)*
		|	'L'
		|	'l'
		)?
	;

protected
WS	:	(	options {greedy=true;}
		: 	' '
		|	'\t'
		|	'\r' '\n'	{newline();}
		|	'\r'		{newline();}
		|	'\n'		{newline();}
		)+
	;
