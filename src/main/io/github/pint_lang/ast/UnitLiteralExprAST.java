package io.github.pint_lang.ast;

public record UnitLiteralExprAST<T>(T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitUnitLiteralExpr(this);
  }
  
}
