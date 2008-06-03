// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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
// Copyright 2008 Red Hat Inc.
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

    import frysk.value.ArithmeticType;
    import frysk.value.SignedType;
    import frysk.value.PointerType;
    import frysk.value.CharType;
    import frysk.value.ArrayType;
    import frysk.value.FloatingPointType;
    import frysk.value.CompositeType;
    import frysk.value.Value;
    import frysk.value.Type;
    import frysk.expr.ExprSymTab;
    import inua.eio.ByteOrder;
}

class CTypeEvaluator extends TreeParser;


options {
    importVocab=CExprParser;
}

{
    ArithmeticType longType;
    ArithmeticType intType;
    ArithmeticType shortType;
    FloatingPointType doubleType;
    FloatingPointType floatType;
    CharType charType;
    PointerType charPointerType;
    private ExprSymTab exprSymTab;

    public CTypeEvaluator(ExprSymTab symTab) {
	    exprSymTab = symTab;
        shortType = new SignedType("short", ByteOrder.LITTLE_ENDIAN, 2);
        intType = new SignedType("int", ByteOrder.LITTLE_ENDIAN, 4);
        longType = new SignedType("long", ByteOrder.LITTLE_ENDIAN, exprSymTab.getWordSize());
        floatType = new FloatingPointType("float", ByteOrder.LITTLE_ENDIAN, 4);
        doubleType = new FloatingPointType("double", ByteOrder.LITTLE_ENDIAN, 8);
        charType = new CharType("char", ByteOrder.LITTLE_ENDIAN, 1, true);
        charPointerType = new PointerType("char*", ByteOrder.LITTLE_ENDIAN, 
                                          exprSymTab.getWordSize(), charType);
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

type returns [Type returnVar=null] 
{ Type t1, t2, t3; String s1;}

    :   (   #(PLUS t1=type t2=type)
          | #(STAR t1=type t2=type)
          | #(DIVIDE t1=type t2=type)
          | #(MOD t1=type t2=type)
        ) {	
             returnVar = t1.getALU(t2,
                         exprSymTab.getWordSize())
                         .getResultType();  
        }
    |   ( #(MINUS type type) )=> #(MINUS t1=type t2=type)  {
            returnVar = t1.getALU(t2,
                         exprSymTab.getWordSize())
                         .getResultType();   
        }       
    |   #(MINUS t1=type ) {
            returnVar = t1;
        }        
    |   (   #(SHIFTLEFT  t1=type t2=type) 
          | #(SHIFTRIGHT  t1=type t2=type)
          | #(AMPERSAND  t1=type t2=type)
          | #(BITWISEXOR  t1=type t2=type)
          | #(BITWISEOR  t1=type t2=type)
        ) {
             returnVar = t1.getALU(t2, 
                         exprSymTab.getWordSize())
                         .getResultType();  
        }
    |   (   #(AND  t1=type t2=type) 
          | #(OR  t1=type t2=type)   
          | #(NOT  t1=type )
        ) {
            returnVar = t1.getALU(exprSymTab.getWordSize())
                        .getIntResultType(); 
        }
    |   (   #(LESSTHAN  t1=type t2=type) 
          | #(GREATERTHAN  t1=type t2=type)
          | #(LESSTHANOREQUALTO  t1=type t2=type)
          | #(GREATERTHANOREQUALTO  t1=type t2=type)
          | #(EQUAL  t1=type t2=type)
          | #(NOTEQUAL  t1=type t2=type)
        ) {
             returnVar = t1.getALU(t2, 
                         exprSymTab.getWordSize())
                         .getIntResultType();  
        }
   |    #(ADDRESS_OF t1=type ) {
            PointerType pointerType = new PointerType
                                         ("*", ByteOrder.LITTLE_ENDIAN, 
                                          exprSymTab.getWordSize(), t1);
            returnVar = pointerType;
        }
    |   #(MEMORY t1=type ) {
            returnVar = ((PointerType)t1).getType();
        } 
    |   (   #(TILDE t1=type) 
          | #(ARITHMETIC_PLUS  t1=type)
          | #(ARITHMETIC_MINUS  t1=type)
        ) {
            returnVar = t1;
        }
    |   (   #(PREINCREMENT t1=type)  
          | #(PREDECREMENT  t1=type)
          | #(POSTINCREMENT  t1=type)
          | #(POSTDECREMENT  t1=type) 
        ) {
            returnVar = t1; 
        }
    |   #(COND_EXPR t3=type t1=type t2=type) {
            // FIXME: Is this a good enough estimate of result
            // type without actually evaluating conditional
            // expressions?
            returnVar = (t1.getSize()> t2.getSize())? t1:t2;
        }
    |   OCTALINT  {
            returnVar = longType;
        }
    |   DECIMALINT  {
            returnVar = longType;
        }
    |   HEXADECIMALINT  {
            returnVar = longType;
        }
    |   FLOAT  {
            returnVar = floatType;
        }
    |   DOUBLE  {
            returnVar = doubleType;
        }
    |   (   #(ASSIGNEQUAL t1=type t2=type) 
          | #(PLUSEQUAL t1=type t2=type)
          | #(MINUSEQUAL t1=type t2=type)
          | #(TIMESEQUAL t1=type t2=type)
          | #(DIVIDEEQUAL t1=type t2=type)
          | #(MODEQUAL t1=type t2=type)
          | #(SHIFTLEFTEQUAL t1=type t2=type) 
          | #(SHIFTRIGHTEQUAL t1=type t2=type) 
          | #(BITWISEANDEQUAL t1=type t2=type) 
          | #(BITWISEXOREQUAL t1=type t2=type) 
          | #(BITWISEOREQUAL t1=type t2=type) 
        ) {
            returnVar = t1;
        }
    |   #(CAST pt:primitiveType t2=type) { 	
  	      if(pt.getText().compareTo("long") == 0) {
	        returnVar = longType;
	      }
	      else if(pt.getText().compareTo("int") == 0) {
	         returnVar = intType;
	      }
	      else if(pt.getText().compareTo("short") == 0) {
  	         returnVar = shortType;
	      }
	      else if(pt.getText().compareTo("double") == 0) {
	         returnVar = doubleType;
	      }
	      else if(pt.getText().compareTo("float") == 0) {
  	         returnVar = floatType;
	      }
	      // XXX: Implement casts for other pointer types as well
	      else if(t2.getName().compareTo("*") ==  0) {
	        if (pt.getText().compareTo("char") == 0) {	  
	          returnVar = charPointerType;
	        }
          }
        }
    |   #(EXPR_LIST t3=type) {
            returnVar = t3;
        }  
    |   #(SIZEOF t1=type) {
           returnVar = longType; 
        }    
    |   #(INDEX t1=type t2=type) {
            returnVar = t1.getType();
        }  
    |   #(SLICE t1=type t2=type t3=type) {
            returnVar = t1.getSliceType();
        }
    |   #(MEMBER t1=type s1=identifier) {
            returnVar = ((CompositeType)t1).getMemberType(s1);
        } 
    |   ident:IDENT  {
            returnVar = ((Type)exprSymTab.getValue(ident.getText()).getType());
        }
    |   tident:TAB_IDENT  {
            returnVar = ((Type)exprSymTab.getValue(tident.getText()).getType());
        }
    ;
