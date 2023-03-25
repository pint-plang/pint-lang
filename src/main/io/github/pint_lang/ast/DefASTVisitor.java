package io.github.pint_lang.ast;

public interface DefASTVisitor<T, R> {
  
  default R visitDef(DefAST<T> ast) {
    return ast.accept(this);
  }
  
  R visitFuncDef(FuncDefAST<T> ast);
  
  R visitVarDef(VarDefAST<T> ast);
  
}
