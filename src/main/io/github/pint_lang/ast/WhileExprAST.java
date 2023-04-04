package io.github.pint_lang.ast;

public record WhileExprAST<T>(String label, ExprAST<T> condition, ExprAST<T> body, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitWhileExpr(this);
  }
  
}
