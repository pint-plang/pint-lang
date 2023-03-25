package io.github.pint_lang.ast;

public sealed interface DefAST<T> extends AST<T> permits FuncDefAST, VarDefAST {
  
  String name();
  
  @Override
  default <R> R accept(ASTVisitor<T, R> visitor) {
    return accept((DefASTVisitor<T, R>) visitor);
  }
  
  <R> R accept(DefASTVisitor<T, R> visitor);
  
}
