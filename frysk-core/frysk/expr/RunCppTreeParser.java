
  package frysk.expr;

  import java.util.*;
  import frysk.lang.*;
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

    public static void main(String []argv)  {
      String []sTestResult = { "PASS", "FAIL"};
      int a,b,c;
      a=5;
      b=3;
      c=8;

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
      Map symTab = new HashMap();
      try {
	for(int i=0;i<expressionTest.length;i++)
	{
	  sInput = expressionTest[i].sExpression + (char)3;

	  CppLexer lexer = new CppLexer(new StringReader(sInput));
	  CppParser parser = new CppParser(lexer);
	  parser.start();

	  CommonAST t = (CommonAST)parser.getAST();
	  CppTreeParser treeParser = new CppTreeParser(4, 2, symTab);

	  try {
	    result = treeParser.expr(t);
	    System.out.println(expressionTest[i].sOperation + 
		": " + ((expressionTest[i].sResult.equals(result.toString()))?"PASS":"FAIL"));
	  }	catch (ArithmeticException ae)  {
	    System.err.println("Arithematic Exception occured:  " + ae);
	  }
	}
      } catch (Exception e) {
	  System.err.println("exception: "+e);
	  e.printStackTrace(System.err);   // so we can get stack trace
      }
    }
  }
