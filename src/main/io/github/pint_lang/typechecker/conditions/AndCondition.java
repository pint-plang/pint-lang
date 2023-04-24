package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record AndCondition(Condition left, Condition right) implements Condition {
  
  public AndCondition {
    if (left == null) throw new NullPointerException("left must not be null");
    if (right == null) throw new NullPointerException("right must not be null");
  }
  
  public static AndCondition and(Condition left, Condition right) {
    return new AndCondition(left, right);
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return
      source instanceof AndCondition sourceAnd
        && (
          left.satisfiedBy(sourceAnd.left, inputs)
          || left.satisfiedBy(sourceAnd.right, inputs)
        )
        && (
          right.satisfiedBy(sourceAnd.left, inputs)
          || right.satisfiedBy(sourceAnd.right, inputs)
        )
      || left.hypotheticallySatisfiedBy(source, inputs)
        && right.hypotheticallySatisfiedBy(source, inputs)
    ;
  }
  
  @Override
  public AndCondition mapInputs(Map<Input, Input> mapping) {
    return new AndCondition(left.mapInputs(mapping), right.mapInputs(mapping));
  }
  
}
