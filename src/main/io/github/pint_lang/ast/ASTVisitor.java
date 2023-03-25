package io.github.pint_lang.ast;

public interface ASTVisitor<T, R> extends DefASTVisitor<T, R>, TypeASTVisitor<T, R>, StatASTVisitor<T, R> {
  
  default R visit(AST<T> ast) {
    return ast.accept(this);
  }
  
  R visitDefs(DefsAST<T> ast);
  
}
