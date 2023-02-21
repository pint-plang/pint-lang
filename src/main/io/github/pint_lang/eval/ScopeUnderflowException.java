package io.github.pint_lang.eval;

public class ScopeUnderflowException extends RuntimeException {
  
  public ScopeUnderflowException() {
    super();
  }
  
  public ScopeUnderflowException(String message) {
    super(message);
  }
  
  public ScopeUnderflowException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public ScopeUnderflowException(Throwable cause) {
    super(cause);
  }
  
}
