package frysk.expr;

import frysk.value.Variable;

public interface CppSymTab
{
  void put(String s, Variable v);
  Variable get(String s);
  boolean putUndefined();
}
