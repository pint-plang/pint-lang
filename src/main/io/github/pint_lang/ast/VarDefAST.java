package io.github.pint_lang.ast;

public record VarDefAST<T>(String name, TypeAST<T> type, ExprAST<T> value, T data) implements DefAST<T>, StatAST<T> {
  
  @Override
  public <R> R accept(ASTVisitor<T, R> visitor) {
    return visitor.visitVarDef(this);
  }
  
  @Override
  public <R> R accept(DefASTVisitor<T, R> visitor) {
    return visitor.visitVarDef(this);
  }
  
  @Override
  public <R> R accept(StatASTVisitor<T, R> visitor) {
    return visitor.visitVarDef(this);
  }
  
}
