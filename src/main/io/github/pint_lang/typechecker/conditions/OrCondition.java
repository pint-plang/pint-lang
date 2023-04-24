package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record OrCondition(Condition left, Condition right) implements Condition {
  
  public OrCondition {
    if (left == null) throw new NullPointerException("left must not be null");
    if (right == null) throw new NullPointerException("right must not be null");
  }
  
  public static OrCondition or(Condition left, Condition right) {
    return new OrCondition(left, right);
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    // todo: weird things may happen like p or (q or r) not being satisfied by q or (p or r)
    // todo: I probably want to flatten nested (left, right) into a list
    return
      source instanceof OrCondition sourceOr
        && (
          left.hypotheticallySatisfiedBy(sourceOr.left, inputs)
          || left.hypotheticallySatisfiedBy(sourceOr.right, inputs)
          || right.hypotheticallySatisfiedBy(sourceOr.left, inputs)
          || right.hypotheticallySatisfiedBy(sourceOr.right, inputs)
        )
      || left.hypotheticallySatisfiedBy(source, inputs)
      || right.hypotheticallySatisfiedBy(source, inputs)
    ;
  }
  
  @Override
  public OrCondition mapInputs(Map<Input, Input> mapping) {
    return new OrCondition(left.mapInputs(mapping), right.mapInputs(mapping));
  }
  
}
