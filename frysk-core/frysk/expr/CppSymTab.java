package frysk.expr;

import frysk.lang.Variable;

public interface CppSymTab
{
  void put(String s, Variable v);
  Variable get(String s);
}
