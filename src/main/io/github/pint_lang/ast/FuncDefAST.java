package io.github.pint_lang.ast;

import java.util.List;

public record FuncDefAST<T>(String name, List<Param<T>> params,TypeAST<T> returnType, ExprAST<T> body, T data) implements DefAST<T> {
  
  @Override
  public <R> R accept(DefASTVisitor<T, R> visitor) {
    return visitor.visitFuncDef(this);
  }
  
  public record Param<T>(String name, TypeAST<T> type) {}
  
}
