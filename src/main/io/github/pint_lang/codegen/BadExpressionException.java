package io.github.pint_lang.codegen;

public class BadExpressionException extends RuntimeException {
  
  public BadExpressionException() {
    super();
  }
  
  public BadExpressionException(String message) {
    super(message);
  }
  
  public BadExpressionException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public BadExpressionException(Throwable cause) {
    super(cause);
  }
  
}
