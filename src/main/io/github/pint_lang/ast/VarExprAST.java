package io.github.pint_lang.ast;

public record VarExprAST<T>(String name, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitVarExprAST(this);
  }
  
}
