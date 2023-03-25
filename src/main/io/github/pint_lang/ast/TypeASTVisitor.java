package io.github.pint_lang.ast;

public interface TypeASTVisitor<T, R> {
  
  default R visitType(TypeAST<T> ast) {
    return ast.accept(this);
  }
  
  R visitSimpleType(SimpleTypeAST<T> ast);
  
  R visitUnitType(UnitTypeAST<T> ast);
  
  R visitArrayType(ArrayTypeAST<T> ast);
  
  R visitConditionType(ConditionTypeAST<T> ast);
  
}
