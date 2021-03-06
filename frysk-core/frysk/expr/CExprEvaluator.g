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
    import frysk.value.SignedType;
    import frysk.value.UnsignedType;
    import frysk.value.FloatingPointType;
    import frysk.value.PointerType;
    import frysk.value.CharType;
    import frysk.value.Value;
    import frysk.expr.ExprSymTab;
    import inua.eio.ByteOrder;
}

class CExprEvaluator extends TreeParser;

options {
    importVocab=CExprParser;
}

{
    ArrayList      refList;
    ArithmeticType arithmeticType;
    ArithmeticType longType;
    ArithmeticType intType;
    ArithmeticType shortType;
    CharType charType;
    PointerType charPointerType;
    FloatingPointType doubleType;
    FloatingPointType floatType;
    private ExprSymTab exprSymTab;
    public CExprEvaluator(ExprSymTab symTab) {
	    exprSymTab = symTab;
        // FIXME: The ExprSymTab can provide types such as this.
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

expr returns [Value returnVar=null] 
{ Value v1, v2, v3, log_expr; String s1; }
    :   #(PLUS v1=expr v2=expr)  { 
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .add(v1, v2);  
        }
    |   #(PLUSEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .plusEqual(v1, v2);
        }        
    |   ( #(MINUS expr expr) )=> #(MINUS v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .subtract(v1, v2);  
        }
    |   #(MINUSEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(),
                        exprSymTab.getWordSize())
                        .minusEqual(v1, v2);
        }        
    |   #(MINUS v1=expr ) {
            returnVar = intType.createValue(0);
            returnVar = returnVar.getType().getALU(v1.getType(), 
                        exprSymTab.getWordSize())
                        .subtract(returnVar, v1); 
        }
    |   ( #(STAR expr expr) )=> #(STAR  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .multiply(v1, v2); 
        }
    |   #(TIMESEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .timesEqual(v1, v2);
        }    
    |   #(DIVIDE  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .divide(v1, v2); 
        } 
    |   #(DIVIDEEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .divideEqual(v1, v2);
        } 
    |   #(MOD  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .mod(v1, v2);  
        }   
    |   #(MODEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .modEqual(v1, v2);
        }                               
    |   #(MEMORY v1=expr ) {
            returnVar = v1.getType()
                        .dereference(v1, exprSymTab.taskMemory());
        } 
    |   #(SHIFTLEFT  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .shiftLeft(v1, v2);  
        }
    |   #(SHIFTRIGHT  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .shiftRight(v1, v2); 
        }
    |   #(LESSTHAN  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .lessThan(v1, v2); 
        }
    |   #(GREATERTHAN  v1=expr v2=expr) {
            returnVar =  v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .greaterThan(v1, v2); 
        }
    |   #(LESSTHANOREQUALTO  v1=expr v2=expr) {
            returnVar =  v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .lessThanOrEqualTo(v1, v2); 
        }
    |   #(GREATERTHANOREQUALTO  v1=expr v2=expr) {
            returnVar =  v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .greaterThanOrEqualTo(v1, v2); 
        }
    |   #(NOTEQUAL  v1=expr v2=expr) {
            returnVar =  v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .notEqual(v1, v2); 
        }
    |   #(EQUAL  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .equal(v1, v2); 
        }
    |   ( #(AMPERSAND expr expr) )=>#(AMPERSAND  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseAnd(v1, v2); 
        }
    |   #(ADDRESS_OF v1=expr ) {
            returnVar = v1.getType().addressOf(v1, 
                        exprSymTab.order(), exprSymTab.getWordSize());
        }
    |   #(BITWISEXOR  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseXor(v1, v2); 
        }
    |   #(BITWISEOR  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseOr(v1, v2); 
        }
    |   #(AND  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(exprSymTab.getWordSize())
                        .logicalAnd(v1, v2); 
        }
    |   #(OR  v1=expr v2=expr) {
            returnVar = v1.getType().getALU(exprSymTab.getWordSize())
                        .logicalOr(v1, v2); 
        }
    |   #(NOT  v1=expr) {
            // byte buffer needed for Pointer/Address types
            returnVar = v1.getType().getALU(exprSymTab.getWordSize())
                        .logicalNegation(v1); 
        }
    |   #(TILDE v1=expr) {
            returnVar = v1.getType().getALU(exprSymTab.getWordSize())
                        .bitWiseComplement(v1); 
        }
    |   #(ARITHMETIC_PLUS  v1=expr) {
            returnVar = v1; 
        }    
    |   #(ARITHMETIC_MINUS  v1=expr) {
            Value zero = longType.createValue(0);
            returnVar = v1.getType().getALU(zero.getType(), 
                        exprSymTab.getWordSize())
                        .subtract(zero, v1); 
        }              
    |   #(PREINCREMENT  v1=expr) {
            Value one = longType.createValue(1);
            v1.assign(v1.getType().getALU(one.getType(), 
                      exprSymTab.getWordSize())
                     .add(one, v1)); 
            returnVar = v1; 
        }       
    |   #(PREDECREMENT  v1=expr) {
            Value one = longType.createValue(1);
            v1.assign(v1.getType().getALU(one.getType(), 
                      exprSymTab.getWordSize())
                     .subtract(v1, one)); 
            returnVar = v1;     
        }   
    |   #(POSTINCREMENT  v1=expr) {
            Value one = longType.createValue(1);
            v1.assign(v1.getType().getALU(one.getType(), 
                      exprSymTab.getWordSize())
                      .add(one, v1)); 
            returnVar = (v1.getType().getALU(one.getType(), 
                         exprSymTab.getWordSize())
                        .subtract(v1, one));                      
        }       
    |   #(POSTDECREMENT  v1=expr) {
            Value one = longType.createValue(1);            
            v1.assign(v1.getType().getALU(one.getType(), 
                      exprSymTab.getWordSize())
                     .subtract(v1, one));  
            returnVar = (v1.getType().getALU(one.getType(), 
                         exprSymTab.getWordSize())
                        .add(v1, one));                         
        }          
    |   #(COND_EXPR  log_expr=expr v1=expr v2=expr) {
            returnVar = ((log_expr.getType().getALU(log_expr.getType(), 
                        exprSymTab.getWordSize())
                        .getLogicalValue(log_expr)) 
                        ? v1 : v2);
        }
    |   o:OCTALINT  {
    	    char c = o.getText().charAt(o.getText().length() - 1);
    	    int l = o.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar =
                longType.createValue(Long.parseLong(o.getText().substring(1, l), 8));
        }
    |   i:DECIMALINT  {
    	    char c = i.getText().charAt(i.getText().length() - 1);
    	    int l = i.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar =
                longType.createValue(Long.parseLong(i.getText().substring(0, l)));
        }
    |   h:HEXADECIMALINT  {
    	    char c = h.getText().charAt(h.getText().length() - 1);
    	    int l = h.getText().length();
    	    if (c == 'u' || c == 'U' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar =
                longType.createValue(Long.parseLong(h.getText().substring(2, l), 16));
        }
    |   f:FLOAT  {
    	    char c = f.getText().charAt(f.getText().length() - 1);
    	    int l = f.getText().length();
    	    if (c == 'f' || c == 'F' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar =
                floatType.createValue(Float.parseFloat(f.getText().substring(0, l)));
        }
    |   d:DOUBLE  {
    	    char c = d.getText().charAt(d.getText().length() - 1);
    	    int l = d.getText().length();
    	    if (c == 'f' || c == 'F' || c == 'l' || c == 'L')
    	       l -= 1;
            returnVar =
                doubleType.createValue(Double.parseDouble(d.getText().substring(0, l)));
        }
    |   #(ASSIGNEQUAL v1=expr v2=expr)  {
            v1.assign(v2);
            returnVar = v1;
        }
    |   #(SHIFTLEFTEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .shiftLeftEqual(v1, v2);
        }
    |   #(SHIFTRIGHTEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .shiftRightEqual(v1, v2);
        }
    |   #(BITWISEANDEQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseAndEqual(v1, v2);
  
        }
    |   #(BITWISEXOREQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseXorEqual(v1, v2);
        }
    |   #(BITWISEOREQUAL v1=expr v2=expr)  {
            returnVar = v1.getType().getALU(v2.getType(), 
                        exprSymTab.getWordSize())
                        .bitWiseOrEqual(v1, v2);
        }
    |   #(CAST pt:primitiveType v2=expr) {
	    if(pt.getText().compareTo("long") == 0) {
	      returnVar = longType.createValue(0);
              returnVar.assign(v2);
	      }
	    else if(pt.getText().compareTo("int") == 0) {
	      returnVar = intType.createValue(0);
              returnVar.assign(v2);
	      }
	    else if(pt.getText().compareTo("short") == 0) {
	      returnVar = shortType.createValue(0);
              returnVar.assign(v2);
	      }
	    else if(pt.getText().compareTo("double") == 0) {
	      returnVar = doubleType.createValue(0.0);
              returnVar.assign(v2);
	      }
	    else if(pt.getText().compareTo("float") == 0) {
	      returnVar = floatType.createValue(0.0);
              returnVar.assign(v2);
	      }
	    // XXX: Implement casts for other pointer types as well
	    else if(v2.getType().getName().compareTo("*") ==  0) {
	      if (pt.getText().compareTo("char") == 0) {	  
	        returnVar = new Value(charPointerType);
            returnVar.assign(v2);
	      }
	    }  
	    else returnVar = v2;
        }
    |   #(EXPR_LIST v1=expr) {
            returnVar = v1;
        }
    |   #(FUNC_CALL v1=expr v2=expr) {
            returnVar = v1;
        }
    |   #(MEMBER v1=expr s1=identifier) {
           returnVar = v1.getType().member(v1, s1);
        }  
    |   #(SIZEOF v1=expr) {
           returnVar = longType.createValue((long)(v1.getType().getSize())); 
        }    
    |   #(INDEX v1=expr v2=expr) {
           returnVar = v1.getType().index(v1, v2, exprSymTab.taskMemory());
        }      
    |   #(SLICE v1=expr v2=expr v3=expr) {
           returnVar = v1.getType().slice(v1, v2, v3, exprSymTab.taskMemory());
        }                
    |   ident:IDENT  {
            returnVar = ((Value)exprSymTab.getValue(ident.getText()));
        }
    |   tident:TAB_IDENT  {
            returnVar = ((Value)exprSymTab.getValue(tident.getText()));
        }
    ;
