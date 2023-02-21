package io.github.pint_lang.eval;

import java.util.HashMap;
import java.util.Optional;

public final class GlobalScope implements Scope {
  
  private final HashMap<String, Value> vars = new HashMap<>();
  private final HashMap<String, Function> fns = new HashMap<>();
  
  @Override
  public GlobalScope global() {
    return this;
  }
  
  @Override
  public Optional<FunctionScope> function() {
    return Optional.empty();
  }
  
  @Override
  public boolean defineVariable(String name, Value value) {
    if (name == null) throw new NullPointerException("name must not be null");
    if (value == null) throw new NullPointerException("value must not be null");
    if (fns.containsKey(name)) return false;
    var oldVal = vars.putIfAbsent(name, value);
    return oldVal == null /* i.e. no other value was present */;
  }
  
  @Override
  public boolean hasVariable(String name) {
    return vars.containsKey(name);
  }
  
  @Override
  public Optional<Value> getVariable(String name) {
    return Optional.ofNullable(vars.get(name));
  }
  
  @Override
  public boolean setVariable(String name, Value value) {
    if (name == null) throw new NullPointerException("name must not be null");
    if (value == null) throw new NullPointerException("value must not be null");
    var newVal = vars.computeIfPresent(name, (key, oldVal) -> value);
    return newVal != null /* i.e. name is present in vars */;
  }
  
  @Override
  public boolean defineFunction(String name, Function function) {
    if (name == null) throw new NullPointerException("name must not be null");
    if (function == null) throw new NullPointerException("function must not be null");
    if (vars.containsKey(name)) return false;
    var oldVal = fns.putIfAbsent(name, function);
    return oldVal == null /* i.e. no other function was present */;
  }
  
  @Override
  public boolean hasFunction(String name) {
    return fns.containsKey(name);
  }
  
  @Override
  public Optional<Function> getFunction(String name) {
    return Optional.ofNullable(fns.get(name));
  }
  
}