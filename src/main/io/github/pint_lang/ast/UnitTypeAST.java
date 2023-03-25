package io.github.pint_lang.ast;

public record UnitTypeAST<T>(T data) implements TypeAST<T> {
  
  @Override
  public <R> R accept(TypeASTVisitor<T, R> visitor) {
    return visitor.visitUnitType(this);
  }
  
}
