package io.github.pint_lang.eval;

public record BoolValue(boolean value) implements Value {
  
  @Override
  public boolean valueEquals(Value value) {
    return value instanceof BoolValue other && other.value == this.value;
  }
  
  @Override
  public String valueToString() {
    return String.valueOf(value);
  }
  
}
