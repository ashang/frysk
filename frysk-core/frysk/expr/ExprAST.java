package frysk.expr;

import antlr.CommonAST;

import frysk.value.Type;

public class ExprAST extends CommonAST {
  Type exprType;
  static final long serialVersionUID = 0;

  public ExprAST() {
  }

public Type getExprType() {
    return exprType;
}

public void setExprType(Type exprType) {
    this.exprType = exprType;
}
}
