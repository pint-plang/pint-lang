package io.github.pint_lang.ast;

import java.util.List;

public record ArrayLiteralExprAST<T>(List<ExprAST<T>> elements, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitArrayLiteralExpr(this);
  }
  
}
