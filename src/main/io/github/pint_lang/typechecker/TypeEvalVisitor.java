package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.*;

public class TypeEvalVisitor implements TypeASTVisitor<Void, TypeAST<Type>> {
  
  public final ErrorLogger<Type> logger;
  
  public TypeEvalVisitor(ErrorLogger<Type> logger) {
    this.logger = logger;
  }
  
  @Override
  public SimpleTypeAST<Type> visitSimpleType(SimpleTypeAST<Void> ast) {
    return new SimpleTypeAST<>(ast.name(), switch (ast.name()) {
      case "string" -> Type.STRING;
      case "int" -> Type.INT;
      case "bool" -> Type.BOOL;
      default -> logger.error("No such type as '" + ast.name() + "'");
    });
  }
  
  @Override
  public UnitTypeAST<Type> visitUnitType(UnitTypeAST<Void> ast) {
    return new UnitTypeAST<>(Type.UNIT);
  }
  
  @Override
  public ArrayTypeAST<Type> visitArrayType(ArrayTypeAST<Void> ast) {
    var innerType = ast.innerType().accept(this);
    return new ArrayTypeAST<>(innerType, new Type.Array(innerType.data()));
  }
  
  @Override
  public ConditionTypeAST<Type> visitConditionType(ConditionTypeAST<Void> ast) {
    throw new UnsupportedOperationException("Type conditions are not yet supported");
  }
  
}
