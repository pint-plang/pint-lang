package io.github.pint_lang.eval;

public class BadDefinitionException extends RuntimeException {
  
  public BadDefinitionException() {
    super();
  }
  
  public BadDefinitionException(String message) {
    super(message);
  }
  
  public BadDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public BadDefinitionException(Throwable cause) {
    super(cause);
  }
  
}
