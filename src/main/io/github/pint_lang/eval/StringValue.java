package io.github.pint_lang.eval;

public record StringValue(String value) implements Value {
  
  public StringValue {
    if (value == null) throw new NullPointerException("value must not be null");
  }
  
  @Override
  public boolean valueEquals(Value value) {
    return value instanceof StringValue other && other.value.equals(this.value);
  }
  
  @Override
  public String valueToString() {
    return '"' + value + '"';
  }
  
}
