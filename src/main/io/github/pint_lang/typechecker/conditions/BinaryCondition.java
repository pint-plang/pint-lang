package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record BinaryCondition(Kind kind, Condition left, Condition right) implements Condition {
  
  public BinaryCondition {
    if (kind == null) throw new NullPointerException("kind must not be null");
    if (left == null) throw new NullPointerException("left must not be null");
    if (right == null) throw new NullPointerException("right must not be null");
  }
  
  public static BinaryCondition add(Condition left, Condition right) {
    return new BinaryCondition(Kind.ADD, left, right);
  }
  
  public static BinaryCondition sub(Condition left, Condition right) {
    return new BinaryCondition(Kind.SUB, left, right);
  }
  
  public static BinaryCondition mul(Condition left, Condition right) {
    return new BinaryCondition(Kind.MUL, left, right);
  }
  
  public static BinaryCondition div(Condition left, Condition right) {
    return new BinaryCondition(Kind.DIV, left, right);
  }
  
  public enum Kind {
    
    ADD,
    SUB,
    MUL,
    DIV;
    
    private boolean symmetric() {
      return switch (this) {
        case ADD, MUL -> true;
        case SUB, DIV -> false;
      };
    }
    
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return
      source instanceof BinaryCondition sourceBinary
        && (
          left.hypotheticallySatisfiedBy(sourceBinary.left, inputs)
          && right.hypotheticallySatisfiedBy(sourceBinary.right, inputs)
          || sourceBinary.kind.symmetric()
          && left.hypotheticallySatisfiedBy(sourceBinary.right, inputs)
          && right.hypotheticallySatisfiedBy(sourceBinary.left, inputs)
        )
        && kind == sourceBinary.kind
      ;
  }
  
  @Override
  public BinaryCondition mapInputs(Map<Input, Input> mapping) {
    return new BinaryCondition(kind, left.mapInputs(mapping), right.mapInputs(mapping));
  }
  
}
