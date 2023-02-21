package io.github.pint_lang.eval;

import static java.util.Arrays.stream;

public sealed interface Value permits ArrayValue, StringValue, IntValue, BoolValue, UnitValue {
  
  boolean valueEquals(Value value);
  
  String valueToString();
  
  static ArrayValue array(Value... values) {
    return of(values);
  }
  
  static ArrayValue array(String... values) {
    return of(values);
  }
  
  static ArrayValue array(int... values) {
    return of(values);
  }
  
  static ArrayValue array(boolean... values) {
    return of(values);
  }
  
  static ArrayValue of(Value[] values) {
    return new ArrayValue(values);
  }
  
  static ArrayValue of(String[] values) {
    return new ArrayValue(stream(values).map(Value::of).toArray(StringValue[]::new));
  }
  
  static ArrayValue of(int[] values) {
    return new ArrayValue(stream(values).mapToObj(Value::of).toArray(IntValue[]::new));
  }
  
  static ArrayValue of(boolean[] values) {
    var newValues = new BoolValue[values.length];
    for (var i = 0; i < values.length; i++) newValues[i] = Value.of(values[i]);
    return new ArrayValue(newValues);
  }
  
  static StringValue of(String value) {
    return new StringValue(value);
  }
  
  static IntValue of(int value) {
    return new IntValue(value);
  }
  
  static BoolValue of(boolean value) {
    return value ? TRUE : FALSE;
  }
  
  static UnitValue of() {
    return UNIT;
  }
  
  BoolValue TRUE = new BoolValue(true);
  BoolValue FALSE = new BoolValue(false);
  
  UnitValue UNIT = new UnitValue();
  
}
