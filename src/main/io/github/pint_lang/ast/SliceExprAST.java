package io.github.pint_lang.ast;

public record SliceExprAST<T>(ExprAST<T> slicee, ExprAST<T> from, ExprAST<T> to, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitSliceExpr(this);
  }
  
}
