package io.github.pint_lang.ast;

public sealed interface StatAST<T> extends AST<T> permits VarDefAST, NopStatAST, ExprAST {
  
  @Override
  default <R> R accept(ASTVisitor<T, R> visitor) {
    return accept((StatASTVisitor<T, R>) visitor);
  }
  
  <R> R accept(StatASTVisitor<T, R> visitor);
  
}
