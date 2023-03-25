package io.github.pint_lang.ast;

public sealed interface TypeAST<T> extends AST<T> permits SimpleTypeAST, UnitTypeAST, ArrayTypeAST, ConditionTypeAST {
  
  @Override
  default <R> R accept(ASTVisitor<T, R> visitor) {
    return accept((TypeASTVisitor<T, R>) visitor);
  }
  
  <R> R accept(TypeASTVisitor<T, R> visitor);
  
}
