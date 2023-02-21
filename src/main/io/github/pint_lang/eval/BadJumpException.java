package io.github.pint_lang.eval;

public class BadJumpException extends RuntimeException {
  
  public BadJumpException() {
    super();
  }
  
  public BadJumpException(String message) {
    super(message);
  }
  
  public BadJumpException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public BadJumpException(Throwable cause) {
    super(cause);
  }
  
}
