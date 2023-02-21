package io.github.pint_lang.eval;

public class BadTypeException extends RuntimeException {
  
  public BadTypeException() {
    super();
  }
  
  public BadTypeException(String message) {
    super(message);
  }
  
  public BadTypeException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public BadTypeException(Throwable cause) {
    super(cause);
  }
  
}
