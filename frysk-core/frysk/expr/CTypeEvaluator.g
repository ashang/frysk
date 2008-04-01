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
        
typeCast
    :   primitiveType (STAR)?
    ;        

identifier returns [String idSpelling=null]
    :   ident:IDENT  {idSpelling=ident.getText();} ;

expr returns [Type returnVar=null] 
{ Type t1, t2; String s1;}

    :   (   #(PLUS t1=expr t2=expr)
          | #(MINUS t1=expr t2=expr)
          | #(STAR t1=expr t2=expr)
          | #(DIVIDE t1=expr t2=expr)
          | #(MOD t1=expr t2=expr)
        ) {	
             returnVar = t1.getALU(t2,
                         exprSymTab.getWordSize())
                         .getResultType();  
        }
    |   (   #(SHIFTLEFT  t1=expr t2=expr) 
          | #(SHIFTRIGHT  t1=expr t2=expr)
          | #(AMPERSAND  t1=expr t2=expr)
          | #(BITWISEXOR  t1=expr t2=expr)
          | #(BITWISEOR  t1=expr t2=expr)
        ) {
             returnVar = t1.getALU(t2, 
                         exprSymTab.getWordSize())
                         .getResultType();  
        }
    |   (   #(AND  t1=expr t2=expr) 
          | #(OR  t1=expr t2=expr)   
          | #(NOT  t1=expr )
        ) {
            returnVar = t1.getALU(exprSymTab.getWordSize())
                        .getIntResultType(); 
        }
    |   (   #(LESSTHAN  t1=expr t2=expr) 
          | #(GREATERTHAN  t1=expr t2=expr)
          | #(LESSTHANOREQUALTO  t1=expr t2=expr)
          | #(GREATERTHANOREQUALTO  t1=expr t2=expr)
          | #(EQUAL  t1=expr t2=expr)
          | #(NOTEQUAL  t1=expr t2=expr)
        ) {
             returnVar = t1.getALU(t2, 
                         exprSymTab.getWordSize())
                         .getIntResultType();  
        }
   |    #(ADDRESS_OF t1=expr ) {
            PointerType pointerType = new PointerType
                                         ("*", ByteOrder.LITTLE_ENDIAN, 
                                          exprSymTab.getWordSize(), t1);
            returnVar = pointerType;
        }
    |   #(MEMORY t1=expr ) {
            returnVar = ((PointerType)t1).getType();
        } 
    |   (   #(TILDE t1=expr) 
          | #(ARITHMETIC_PLUS  t1=expr)
          | #(ARITHMETIC_MINUS  t1=expr)
          | #(MINUS t1=expr )
        ) {
            returnVar = t1;
        }
    |   (   #(PREINCREMENT t1=expr)  
          | #(PREDECREMENT  t1=expr)
          | #(POSTINCREMENT  t1=expr)
          | #(POSTDECREMENT  t1=expr) 
        ) {
            returnVar = t1; 
        }
    |   #(COND_EXPR expr t1=expr t2=expr) {
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
    |   (   #(ASSIGNEQUAL t1=expr t2=expr) 
          | #(PLUSEQUAL t1=expr t2=expr)
          | #(MINUSEQUAL t1=expr t2=expr)
          | #(TIMESEQUAL t1=expr t2=expr)
          | #(DIVIDEEQUAL t1=expr t2=expr)
          | #(MODEQUAL t1=expr t2=expr)
          | #(SHIFTLEFTEQUAL t1=expr t2=expr) 
          | #(SHIFTRIGHTEQUAL t1=expr t2=expr) 
          | #(BITWISEANDEQUAL t1=expr t2=expr) 
          | #(BITWISEXOREQUAL t1=expr t2=expr) 
          | #(BITWISEOREQUAL t1=expr t2=expr) 
        ) {
            returnVar = t1;
        }
    |   #(CAST pt:typeCast t2=expr) { 	
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
    |   #(EXPR_LIST t1=expr) {
            returnVar = t1;
        }  
    |   #(SIZEOF t1=expr) {
           returnVar = longType; 
        }    
    |   #(INDEX t1=expr t2=expr) {
            returnVar = t1.getType();
        }  
    |   #(SLICE t1=expr t2=expr expr) {
            returnVar = t1.getSliceType();
        }
    |   #(MEMBER t1=expr s1=identifier) {
            returnVar = ((CompositeType)t1).getMemberType(s1);
        } 
    |   ident:IDENT  {
            returnVar = ((Type)exprSymTab.getValue(ident.getText()).getType());
        }
    |   tident:TAB_IDENT  {
            returnVar = ((Type)exprSymTab.getValue(tident.getText()).getType());
        }
    ;
