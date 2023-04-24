package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record UnaryCondition(Kind kind, Condition operand) implements Condition {
  
  public UnaryCondition {
    if (kind == null) throw new NullPointerException("kind must not be null");
    if (operand == null) throw new NullPointerException("operand must not be null");
  }
  
  public static Condition plus(Condition operand) {
    return operand;
  }
  
  public static UnaryCondition neg(Condition operand) {
    return new UnaryCondition(Kind.NEG, operand);
  }
  
  public static UnaryCondition not(Condition operand) {
    return new UnaryCondition(Kind.NOT, operand);
  }
  
  public static UnaryCondition abs(Condition operand) {
    return new UnaryCondition(Kind.ABS, operand);
  }
  
  public enum Kind {
    
    NEG,
    NOT,
    ABS
    
  }
  
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return
      source instanceof UnaryCondition sourceUnary
      && operand.hypotheticallySatisfiedBy(sourceUnary.operand, inputs)
      && kind == sourceUnary.kind
    ;
  }
  
  @Override
  public UnaryCondition mapInputs(Map<Input, Input> mapping) {
    return new UnaryCondition(kind, operand.mapInputs(mapping));
  }
  
}
