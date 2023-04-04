package io.github.pint_lang.typechecker;

public class StackUnderflowException extends RuntimeException {
  
  public StackUnderflowException() {
    super();
  }
  
  public StackUnderflowException(String message) {
    super(message);
  }
  
  public StackUnderflowException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public StackUnderflowException(Throwable cause) {
    super(cause);
  }
  
}
