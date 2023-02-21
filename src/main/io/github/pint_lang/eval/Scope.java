package io.github.pint_lang.eval;

import java.util.Optional;

public sealed interface Scope permits GlobalScope, LocalScope {
  
  GlobalScope global();
  
  Optional<FunctionScope> function();
  
  boolean defineVariable(String name, Value value);
  
  boolean hasVariable(String name);
  
  Optional<Value> getVariable(String name);
  
  boolean setVariable(String name, Value value);
  
  boolean defineFunction(String name, Function function);
  
  boolean hasFunction(String name);
  
  Optional<Function> getFunction(String name);
  
}
