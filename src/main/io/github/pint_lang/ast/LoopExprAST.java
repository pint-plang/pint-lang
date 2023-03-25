package io.github.pint_lang.ast;

public record LoopExprAST<T>(String label, ExprAST<T> body, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitLoopExpr(this);
  }
  
}
