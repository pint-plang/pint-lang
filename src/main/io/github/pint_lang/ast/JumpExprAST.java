package io.github.pint_lang.ast;

public record JumpExprAST<T>(JumpKind kind, String targetLabel, ExprAST<T> value, T data) implements ExprAST<T> {
  
  @Override
  public <R> R accept(ExprASTVisitor<T, R> visitor) {
    return visitor.visitJumpExpr(this);
  }
  
}
