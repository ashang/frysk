// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import frysk.junit.TestCase;

/**
 * Scratch is for the case where there isn't any symbols, basic ops
 * should still work.
 */

public class TestArithmetics extends TestCase {
    private void checkScratchExpr(String expr, long value) {
	ExprSymTab symTab = new ScratchSymTab();
	Expression e = ExpressionFactory.parse(symTab, expr);
	assertEquals(expr, value, e.getValue().asLong());
    }
    public void testAdd() {
	checkScratchExpr("1 + 2", 3);
	checkScratchExpr("+1 + +3*2", 7);
	checkScratchExpr("+1 + (-3)*2", -5);
    }

    private void checkVariableExpr(String expr, long value) {
	ExprSymTab symTab = new TestbedSymTab();
	Expression e = ExpressionFactory.parse(symTab, expr);
	assertEquals(expr, value, e.getValue().asLong());
    }
    public void testMember() {
	checkVariableExpr("a.alpha", 0x01020304);
    }
    
    public void testIncrement() {
	checkVariableExpr("a.kappa++ + ++a.kappa", 4); 
    }
    
    public void testDecrement() {
	checkVariableExpr("a.kappa-- + --a.kappa", 0); 
    }

    public void testArrayAccess() {
	checkVariableExpr("arr[0]", 0x01020304);
	checkVariableExpr("arr[1]", 0x05060708);
    }

    public void testArraySlice() {
	checkVariableExpr("arr[1:2][0]", 0x05060708);
	checkVariableExpr("arr[1:2][1]", 0x09101112);
	checkVariableExpr("arr[0:2][0:1][0:0][0]", 0x01020304);
	checkVariableExpr("arr[0:2][1:2][0:0][0]", 0x05060708);
	checkVariableExpr("arr[0:2][1:2][0:1][1]", 0x09101112);
    }

    private void checkErrorExpr(String input, String error) {
	Throwable t = null;
	try {
	    ExprSymTab symTab = new ScratchSymTab();
	    ExpressionFactory.parse(symTab, input);
	} catch (SyntaxException e) {
	    t = e;
	}
	assertNotNull("error", t);
	assertEquals("error", error, t.getMessage());
    }
    public void testEndOfFileError() {
	checkErrorExpr("&", "incomplete expression");
    }
    public void testExpressionError() {
	checkErrorExpr("1 . 2", "invalid expression at: 2");
    }
    public void testTokenError() {
	checkErrorExpr("1...2", "unexpected input: expecting '.', found '2'");
    }
    // Testing types
    private void checkExprType (String expr, String resultType){
	ExprSymTab symTab = new TestbedSymTab();
	Expression e = ExpressionFactory.parse(symTab, expr);
	assertEquals(expr, resultType, e.getType().getName());
    }

    public void testTypes() {
	checkExprType("++a.kappa", "int32b_t");
	checkExprType("a.alpha", "int32b_t");
	checkExprType("a.alpha/1.0", "double");
    }
}
