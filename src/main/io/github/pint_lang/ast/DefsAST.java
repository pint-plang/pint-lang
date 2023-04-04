package io.github.pint_lang.ast;

import java.util.List;

public record DefsAST<T>(List<DefAST<T>> defs, T data) implements AST<T> {
  
  @Override
  public <R> R accept(ASTVisitor<T, R> visitor) {
    return visitor.visitDefs(this);
  }
  
}
