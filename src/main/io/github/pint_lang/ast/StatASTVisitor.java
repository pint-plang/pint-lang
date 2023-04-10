package io.github.pint_lang.ast;

public interface StatASTVisitor<T, R> extends ExprASTVisitor<T, R> {
  
  default R visitStat(StatAST<T> ast) {
    return ast.accept(this);
  }
  
  R visitVarDef(VarDefAST<T> ast);
  
  R visitNopStat(NopStatAST<T> ast);
  
}
