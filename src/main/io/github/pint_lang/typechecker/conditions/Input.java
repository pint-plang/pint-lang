package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public final class Input implements Condition {
  
  private final int id;
  
  private Input(int id) {
    this.id = id;
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return inputs.matchOrBind(source, this);
  }
  
  @Override
  public Input mapInputs(Map<Input, Input> mapping) {
    return mapping.get(this);
  }
  
  @Override
  public int hashCode() {
    return id;
  }
  
  @Override
  public boolean equals(Object o) {
    return o instanceof Input other && id == other.id;
  }
  
  @Override
  public String toString() {
    return "Input" + ((long) id & 0xFFFFFFFFL);
  }
  
  public static final class Generator {
    
    private int nextId = 0;
    
    public Input next() {
      return new Input(nextId++);
    }
    
  }
  
}
