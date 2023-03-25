package io.github.pint_lang.ast;

public record BoolLiteralExprAST<T>(boolean value, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitBoolLiteralExpr(this);
  }
  
}
