package io.github.pint_lang.codegen;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class JumpScopeStack {
  
  private Node head;
  
  public JumpScopeStack() {
    this.head = null;
  }
  
  public void pushLabeled(String label, boolean allowAnon, JumpScope scope) {
    if (label == null) throw new NullPointerException("label must not be null");
    if (scope == null) throw new NullPointerException("scope must not be null");
    head = new Node(head, label, allowAnon, scope);
  }
  
  public void pushLabeled(String label, boolean allowAnon, LLVMBasicBlockRef continueBlock, LLVMBasicBlockRef breakBlock, LLVMValueRef phi, String name) {
    pushLabeled(label, allowAnon, new JumpScope(continueBlock, breakBlock, phi, name));
  }
  
  public Scope scopeLabeled(String label, boolean allowAnon, JumpScope scope) {
    pushLabeled(label, allowAnon, scope);
    return new Scope();
  }
  
  public Scope scopeLabeled(String label, boolean allowAnon, LLVMBasicBlockRef continueBlock, LLVMBasicBlockRef breakBlock, LLVMValueRef phi, String name) {
    pushLabeled(label, allowAnon, continueBlock, breakBlock, phi, name);
    return new Scope();
  }
  
  public void pushAnon(JumpScope scope) {
    if (scope == null) throw new NullPointerException("scope must not be null");
    head = new Node(head, null, true, scope);
  }
  
  public void pushAnon(LLVMBasicBlockRef continueBlock, LLVMBasicBlockRef breakBlock, LLVMValueRef phi, String name) {
    pushAnon(new JumpScope(continueBlock, breakBlock, phi, name));
  }
  
  public Scope scopeAnon(JumpScope scope) {
    pushAnon(scope);
    return new Scope();
  }
  
  public Scope scopeAnon(LLVMBasicBlockRef continueBlock, LLVMBasicBlockRef breakBlock, LLVMValueRef phi, String name) {
    pushAnon(continueBlock, breakBlock, phi, name);
    return new Scope();
  }
  
  public JumpScope peek() {
    return head.scope;
  }
  
  public JumpScope peekAnon() {
    return head != null && head.allowAnon ? head.scope : null;
  }
  
  public JumpScope findLabeled(String label) {
    if (label == null) throw new NullPointerException("label must not be null");
    for (var current = head; current != null; current = current.parent) {
      if (label.equals(current.label)) return current.scope;
    }
    return null;
  }
  
  public JumpScope pop() {
    if (head != null) {
      var anon = head;
      head = anon.parent;
      return anon.scope;
    } else {
      return null;
    }
  }
  
  record Node(Node parent, String label, boolean allowAnon, JumpScope scope) {}
  
  public class Scope implements AutoCloseable {
    
    private Scope() {}
    
    @Override
    public void close() {
      if (pop() == null) throw new StackUnderflowException();
    }
    
  }
  
}
