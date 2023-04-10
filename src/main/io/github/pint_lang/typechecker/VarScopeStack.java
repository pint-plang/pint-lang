package io.github.pint_lang.typechecker;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

public class VarScopeStack {
  
  private final Stack<HashMap<String, Type>> stack = new Stack<>();
  
  public void push() {
    stack.push(new HashMap<>());
  }
  
  public void pop() {
    try {
      stack.pop();
    } catch (EmptyStackException e) {
      throw new StackUnderflowException(e);
    }
  }
  
  public void putVar(String name, Type type) {
    stack.peek().put(name, type);
  }
  
  public Type getVar(String name) {
    var iter = stack.listIterator(stack.size());
    while (iter.hasPrevious()) {
      var scope = iter.previous();
      var type = scope.get(name);
      if (type != null) return type;
    }
    return null;
  }
  
}
