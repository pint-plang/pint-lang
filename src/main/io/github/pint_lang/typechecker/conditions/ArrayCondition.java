package io.github.pint_lang.typechecker.conditions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.stream;

public record ArrayCondition(Item[] items) implements Condition {
  
  public ArrayCondition {
    if (items == null) throw new NullPointerException("items array must not be null");
    if (stream(items).anyMatch(Objects::isNull)) throw new NullPointerException("all items must not be null");
  }
  
  public static ArrayCondition array(Item... items) {
    return new ArrayCondition(items);
  }
  
  public static ArrayCondition array(List<Item> items) {
    return new ArrayCondition(items.toArray(Item[]::new));
  }
  
  @Override
  public boolean equals(Object o) {
    return o instanceof ArrayCondition other && Arrays.equals(items, other.items);
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(items);
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    if (!(source instanceof ArrayCondition sourceArray) || items.length != sourceArray.items.length) return false;
    for (var i = 0; i < items.length; i++) {
      if (!items[i].satisfiedBy(sourceArray.items[i], inputs)) return false;
    }
    return true;
  }
  
  @Override
  public ArrayCondition mapInputs(Map<Input, Input> mapping) {
    return new ArrayCondition(stream(items).map(item -> item.mapInputs(mapping)).toArray(Item[]::new));
  }
  
  public record Item(Condition item, boolean spread) {
    
    private boolean satisfiedBy(Item source, InputMapper inputs) {
      return spread == source.spread && item.satisfiedBy(source.item, inputs);
    }
    
    private Item mapInputs(Map<Input, Input> mapping) {
      return new Item(item.mapInputs(mapping), spread);
    }
    
  }
  
}
