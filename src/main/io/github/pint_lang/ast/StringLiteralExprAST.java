package io.github.pint_lang.ast;

public record StringLiteralExprAST<T>(String value, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitStringLiteralExpr(this);
  }
  
}
