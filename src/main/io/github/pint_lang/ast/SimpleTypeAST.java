package io.github.pint_lang.ast;

public record SimpleTypeAST<T>(String name, T data) implements TypeAST<T> {
  
  @Override
  public <R> R accept(TypeASTVisitor<T, R> visitor) {
    return visitor.visitSimpleType(this);
  }
  
}
