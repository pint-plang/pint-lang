package io.github.pint_lang.ast;

public record BinaryExprAST<T>(BinaryOp op, ExprAST<T> left, ExprAST<T> right, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitBinaryExpr(this);
  }
  
}
