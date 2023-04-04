package io.github.pint_lang.ast;

public record ArrayTypeAST<T>(TypeAST<T> innerType, T data) implements TypeAST<T> {
  
  @Override
  public <R> R accept(TypeASTVisitor<T, R> visitor) {
    return visitor.visitArrayType(this);
  }
  
}
