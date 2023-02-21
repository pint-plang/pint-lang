package io.github.pint_lang.eval;

import java.util.HashMap;
import java.util.Optional;

public non-sealed class LocalScope implements Scope {
  
  public final Scope parent;
  private final HashMap<String, Value> vars = new HashMap<>();
  
  public LocalScope(Scope parent) {
    this.parent = parent;
  }
  
  @Override
  public GlobalScope global() {
    return parent.global();
  }
  
  @Override
  public Optional<FunctionScope> function() {
    return parent.function();
  }
  
  @Override
  public boolean defineVariable(String name, Value value) {
    if (name == null) throw new NullPointerException("name must not be null");
    if (value == null) throw new NullPointerException("value must not be null");
    vars.put(name, value);
    return true;
  }
  
  @Override
  public boolean hasVariable(String name) {
    return vars.containsKey(name) || parent.hasVariable(name);
  }
  
  @Override
  public Optional<Value> getVariable(String name) {
    return Optional.ofNullable(vars.get(name)).or(() -> parent.getVariable(name));
  }
  
  @Override
  public boolean setVariable(String name, Value value) {
    if (name == null) throw new NullPointerException("name must not be null");
    if (value == null) throw new NullPointerException("value must not be null");
    var newVal = vars.computeIfPresent(name, (key, oldVal) -> value);
    return newVal != null /* i.e. name is present in vars */ || parent.setVariable(name, value);
  }
  
  @Override
  public boolean defineFunction(String name, Function function) {
    return false;
  }
  
  @Override
  public boolean hasFunction(String name) {
    return parent.hasVariable(name);
  }
  
  @Override
  public Optional<Function> getFunction(String name) {
    return parent.getFunction(name);
  }
  
}
