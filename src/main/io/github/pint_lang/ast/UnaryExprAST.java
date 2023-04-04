package io.github.pint_lang.ast;

public record UnaryExprAST<T>(UnaryOp op, ExprAST<T> operand, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitUnaryExpr(this);
  }
  
}
