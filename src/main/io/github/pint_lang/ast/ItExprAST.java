package io.github.pint_lang.ast;

public record ItExprAST<T>(T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitItExpr(this);
  }
  
}
