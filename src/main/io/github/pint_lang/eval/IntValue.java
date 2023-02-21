package io.github.pint_lang.eval;

public record IntValue(int value) implements Value {
  
  @Override
  public boolean valueEquals(Value value) {
    return value instanceof IntValue other && other.value == this.value;
  }
  
  @Override
  public String valueToString() {
    return String.valueOf(value);
  }
  
}
