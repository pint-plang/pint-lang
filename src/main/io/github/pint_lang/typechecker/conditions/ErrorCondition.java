package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record ErrorCondition() implements Condition {
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return this.equals(source);
  }
  
  @Override
  public ErrorCondition mapInputs(Map<Input, Input> mapping) {
    return this;
  }
  
}
