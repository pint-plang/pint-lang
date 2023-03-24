package io.github.pint_lang.codegen;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.HashMap;

public class DefScopeStack {
  
  private Node head;
  
  public DefScopeStack() {
    this.head = new Node(null, new HashMap<>());
  }
  
  public void push() {
    head = new Node(head, new HashMap<>());
  }
  
  public Scope scope() {
    push();
    return new Scope();
  }
  
  public boolean pop() {
    if (head != null) {
      head = head.parent;
      return true;
    } else {
      return false;
    }
  }
  
  public boolean putVar(String name, VarData var) {
    if (head != null) {
      head.vars.put(name, var);
      return true;
    } else {
      return false;
    }
  }
  
  public boolean putVar(String name, LLVMValueRef ptr, LLVMTypeRef type) {
    return putVar(name, new VarData(ptr, type));
  }
  
  public VarData getVar(String name) {
    if (name == null) throw new NullPointerException("name must not be null");
    for (var current = head; current != null; current = current.parent) {
      var v = current.vars.get(name);
      if (v != null) return v;
    }
    return null;
  }
  
  private record Node(Node parent, HashMap<String, VarData> vars) {}
  
  public record VarData(LLVMValueRef ptr, LLVMTypeRef type) {
    
    public VarData {
      if (ptr == null) throw new NullPointerException("ptr must not be null");
      if (type == null) throw new NullPointerException("type must not be null");
    }
    
  }
  
  public class Scope implements AutoCloseable {
    
    private Scope() {}
    
    @Override
    public void close() {
      if (!pop()) throw new StackUnderflowException();
    }
    
  }
  
}
