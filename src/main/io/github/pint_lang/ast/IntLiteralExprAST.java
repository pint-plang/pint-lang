package io.github.pint_lang.ast;

public record IntLiteralExprAST<T>(int value, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitIntLiteralExpr(this);
  }
  
}
