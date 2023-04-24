package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public sealed interface Condition permits ErrorCondition, OrCondition, AndCondition, CmpCondition, BinaryCondition, UnaryCondition, ArrayCondition, ConstantCondition, Input {
  
  default boolean satisfies(Condition target) {
    return target.satisfiedBy(this, new InputMapper());
  }
  
  boolean satisfiedBy(Condition source, InputMapper inputs);
  
  default boolean hypotheticallySatisfiedBy(Condition source, InputMapper inputs) {
    return inputs.hypothetically(hypotheticalInputs -> this.satisfiedBy(source, hypotheticalInputs));
  }
  
  Condition mapInputs(Map<Input, Input> mapping);
  
}
