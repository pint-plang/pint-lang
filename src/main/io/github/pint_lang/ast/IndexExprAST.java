package io.github.pint_lang.ast;

public record IndexExprAST<T>(ExprAST<T> indexee, ExprAST<T> index, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitIndexExpr(this);
  }
  
}
