package io.github.pint_lang.ast;

import java.util.List;

public record FuncCallExprAST<T>(String funcName, List<ExprAST<T>> args, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitFuncCallExpr(this);
  }
  
}
