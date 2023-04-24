package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public sealed interface ConstantCondition extends Condition {
  
  record Str(String value) implements ConstantCondition {
    
    public Str {
      if (value == null) throw new NullPointerException("value must not be null");
    }
    
  }
  
  record Int(int value) implements ConstantCondition {}
  
  record Bool(boolean value) implements ConstantCondition {}
  
  record Unit() implements ConstantCondition {}
  
  static ConstantCondition string(String value) {
    return new Str(value);
  }
  
  static ConstantCondition integer(int value) {
    return new Int(value);
  }
  
  static ConstantCondition bool(boolean value) {
    return new Bool(value);
  }
  
  static ConstantCondition unit() {
    return new Unit();
  }
  
  @Override
  default boolean satisfiedBy(Condition source, InputMapper inputs) {
    return source.equals(this);
  }
  
  @Override
  default ConstantCondition mapInputs(Map<Input, Input> mapping) {
    return this;
  }
  
}
