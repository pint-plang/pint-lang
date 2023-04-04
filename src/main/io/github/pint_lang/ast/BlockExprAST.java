package io.github.pint_lang.ast;

import java.util.List;

public record BlockExprAST<T>(String label, List<StatAST<T>> stats, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitBlockExpr(this);
  }
  
}
