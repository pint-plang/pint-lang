package io.github.pint_lang.ast;

public record IfExprAST<T>(ExprAST<T> condition, ExprAST<T> thenBody, ExprAST<T> elseBody, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitIfExpr(this);
  }
  
}
