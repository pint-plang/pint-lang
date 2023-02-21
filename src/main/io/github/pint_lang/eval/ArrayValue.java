package io.github.pint_lang.eval;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public record ArrayValue(Value[] values) implements Value {
  
  public ArrayValue {
    if (values == null) throw new NullPointerException("values must not be null");
  }
  
  @Override
  public boolean valueEquals(Value value) {
    if (value instanceof ArrayValue other) {
      if (other.values.length != this.values.length) return false;
      for (var i = 0; i < this.values.length; i++) {
        if (!other.values[i].valueEquals(this.values[i])) return false;
      }
    }
    return true;
  }
  
  @Override
  public String valueToString() {
    return stream(values).map(Value::valueToString).collect(joining(", ", "[", "]"));
  }
  
}
