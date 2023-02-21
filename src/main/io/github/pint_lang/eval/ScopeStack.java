package io.github.pint_lang.eval;

public class ScopeStack {
  
  private Scope scope;
  
  public ScopeStack(GlobalScope scope) {
    if (scope == null) throw new NullPointerException("scope must not be null");
    this.scope = scope;
  }
  
  public Scope peek() {
    return scope;
  }
  
  public void pushLocal() {
    scope = new LocalScope(scope);
  }
  
  public void popLocal() {
    if (scope instanceof LocalScope local) scope = local.parent;
    else throw new ScopeUnderflowException();
  }
  
  public LocalGuard guardLocal() {
    this.pushLocal();
    return this.new LocalGuard();
  }
  
  public void pushFunction() {
    scope = new FunctionScope(scope);
  }
  
  public void popFunction() {
    scope = scope.function().orElseThrow(ScopeUnderflowException::new).caller;
  }
  
  public FunctionGuard guardFunction() {
    this.pushFunction();
    return this.new FunctionGuard();
  }
  
  public final class LocalGuard implements AutoCloseable {
    
    private LocalGuard() {}
    
    @Override
    public void close() {
      ScopeStack.this.popLocal();
    }
    
  }
  
  public final class FunctionGuard implements AutoCloseable {
    
    private FunctionGuard() {}
    
    @Override
    public void close() {
      ScopeStack.this.popFunction();
    }
    
  }
  
}
