package io.github.pint_lang.typechecker;

public class JumpScopeStack {
  
  private Node head;
  
  public JumpScopeStack() {
    head = null;
  }
  
  public void push(JumpScope scope) {
    if (scope == null) throw new NullPointerException("scope must not be null");
    head = new Node(head, scope);
  }
  
  public void pushLabeledOnly(String label, Type defaultType) {
    head = new Node(head, new JumpScope.LabeledOnly(label, defaultType));
  }
  
  public void pushAnonOnly(Type defaultType) {
    head = new Node(head, new JumpScope.AnonOnly(defaultType));
  }
  
  public void pushLabeledOrAnon(String label, Type defaultType) {
    head = new Node(head, new JumpScope.LabeledOrAnon(label, defaultType));
  }
  
  public JumpScope pop() {
    if (head == null) throw new StackUnderflowException("Tried to pop a function scope");
    var scope = head.scope;
    head = head.parent;
    return scope;
  }
  
  public JumpScope peek() {
    if (head == null) throw new StackUnderflowException("Tried to peek a function scope");
    return head.scope;
  }
  
  public JumpScope peekAnon() {
    if (head == null) throw new StackUnderflowException("Tried to peek a function scope");
    var scope = head.scope;
    if (!(scope instanceof JumpScope.Anon)) return null;
    return scope;
  }
  
  public JumpScope findLabeled(String label) {
    if (label == null) throw new NullPointerException("label must not be null");
    for (var current = head; current != null; current = current.parent) {
      var scope = current.scope;
      if (scope instanceof JumpScope.Labeled labeledScope && labeledScope.getLabel().equals(label)) {
        return scope;
      }
    }
    return null;
  }
  
  private record Node(Node parent, JumpScope scope) {}
  
}
