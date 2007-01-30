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

  import java.util.*;
  import frysk.value.*;
  import antlr.*;
  import java.io.*;

  class RunCppTreeParser {
    static class ExpressionTest {
      String sExpression;
      String sResult;
      String sOperation;

      ExpressionTest(String expr, String result, String oper) {
	sExpression = expr;
	sResult = result;
	sOperation = oper;
      }
    }

    static class rctpsymTab implements CppSymTab
    {
      final Map symTab = new HashMap();
      public void put(String s, Variable v)
      { symTab.put(s, v);}
      public Variable get(String s)
      { return (Variable)symTab.get(s); }
      public Variable get(String s, ArrayList v)
      { return (Variable)symTab.get(s); }
      public boolean putUndefined ()
      { return true; }
    }

    
    public static void main(String []argv)  {
      //String []sTestResult = { "PASS", "FAIL"};
      int a,b,c;
      a=5;
      b=3;
      c=8;
      rctpsymTab hpdsymTab = new rctpsymTab();

      ExpressionTest expressionTest[] = { 
	(new ExpressionTest("a=" + a, String.valueOf(a), "assign equal")), 
	(new ExpressionTest("b=" + b, String.valueOf(b), "assign equal2")), 
	(new ExpressionTest("c=" + c, String.valueOf(c), "assign equal3")), 
	(new ExpressionTest("a+b", String.valueOf(a+b), "addition")), 
	(new ExpressionTest("a-b", String.valueOf(a-b), "subtraction")), 
	(new ExpressionTest("a*b", String.valueOf(a*b), "multiplication")), 
	(new ExpressionTest("a/b", String.valueOf(a/b), "division")), 
	(new ExpressionTest("a%b", String.valueOf(a%b), "modulus")), 
	(new ExpressionTest("a+b*c", String.valueOf(a+b*c), "addition, multiplication")), 
	(new ExpressionTest("a<<b", String.valueOf(a<<b), "shift left")), 
	(new ExpressionTest("a>>b", String.valueOf(a>>b), "shift right")), 
	(new ExpressionTest("a<b", ((a<b)?"1":"0"), "less than")), 
	(new ExpressionTest("a>b", ((a>b)?"1":"0"), "greater than")), 
	(new ExpressionTest("a<=b", ((a<=b)?"1":"0"), "less than equal to ")), 
	(new ExpressionTest("a>=b", ((a>=b)?"1":"0"), "greater than equal to ")), 
	(new ExpressionTest("a==b", ((a==b)?"1":"0"), "equal to")), 
	(new ExpressionTest("a!=b", ((a!=b)?"1":"0"), "not equal to")), 
	(new ExpressionTest("a&b", String.valueOf(a&b), "bitwise and")), 
	(new ExpressionTest("a^b", String.valueOf(a^b), "bitwise xor")), 
	(new ExpressionTest("a|b", String.valueOf(a|b), "bitwise or")), 
	(new ExpressionTest("((a>b)&&(b<c))", (((a>b)&&(b<c))?"1":"0"), "logical and")), 
	(new ExpressionTest("((a<b)||(b<c))", (((a<b)||(b<c))?"1":"0"), "logical or")), 
	(new ExpressionTest("a?b:c", String.valueOf((a!=0)?b:c), "conditional expression")), 
	(new ExpressionTest("a+=b", String.valueOf(a+=b), "plus equal")), 
	(new ExpressionTest("a-=b", String.valueOf(a-=b), "minus equal")), 
	(new ExpressionTest("a*=b", String.valueOf(a*=b), "times equal")), 
	(new ExpressionTest("a/=b", String.valueOf(a/=b), "divide equal")), 
	(new ExpressionTest("a%=b", String.valueOf(a%=b), "mod equal")), 
	(new ExpressionTest("a<<=b", String.valueOf(a<<=b), "shift left equal")), 
	(new ExpressionTest("a>>=b", String.valueOf(a>>=b), "shift right equal")), 
	(new ExpressionTest("a&=b", String.valueOf(a&=b), "bitwise and equal")), 
	(new ExpressionTest("a|=b", String.valueOf(a|=b), "bitwise or equal")), 
	(new ExpressionTest("a^=b", String.valueOf(a^=b), "bitwise xor equal")), 
      };


      String sInput;
      Variable result;
      try {
	for(int i=0;i<expressionTest.length;i++)
	{
	  sInput = expressionTest[i].sExpression + (char)3;

	  CppLexer lexer = new CppLexer(new StringReader(sInput));
	  CppParser parser = new CppParser(lexer);
	  parser.start();

	  CommonAST t = (CommonAST)parser.getAST();
	  CppTreeParser treeParser = new CppTreeParser(4, 2, hpdsymTab);

	  try {
	    result = treeParser.expr(t);
	    System.out.println(expressionTest[i].sOperation + 
		": " + ((expressionTest[i].sResult.equals(result.toString()))?"PASS":"FAIL"));
	  }	catch (ArithmeticException ae)  {
	    System.err.println("Arithmetic Exception occurred:  " + ae);
	  }
	}
      } catch (Exception e) {
	  System.err.println("exception: "+e);
	  e.printStackTrace(System.err);   // so we can get stack trace
      }
    }
  }
