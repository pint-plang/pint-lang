package io.github.pint_lang.ast;

import java.util.List;

public record ArrayLiteralExprAST<T>(List<Item<T>> items, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitArrayLiteralExpr(this);
  }
  
  public record Item<T>(ExprAST<T> item, boolean spread) {}
  
}
