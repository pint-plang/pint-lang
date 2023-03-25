package io.github.pint_lang.ast;

public sealed interface AST<T> permits DefsAST, DefAST, TypeAST, StatAST {
  
  T data();
  
  <R> R accept(ASTVisitor<T, R> visitor);
  
}
