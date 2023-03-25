package io.github.pint_lang.ast;

public record ConditionTypeAST<T>(TypeAST<T> type, ExprAST<T> condition, T data) implements TypeAST<T> {
  
  @Override
  public <R> R accept(TypeASTVisitor<T, R> visitor) {
    return visitor.visitConditionType(this);
  }
  
}
