package io.github.pint_lang.eval;

public record UnitValue() implements Value {
  
  @Override
  public boolean valueEquals(Value value) {
    return value instanceof UnitValue;
  }
  
  @Override
  public String valueToString() {
    return "unit";
  }
  
}
