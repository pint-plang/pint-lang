package io.github.pint_lang.typechecker;

import java.io.PrintStream;
import java.util.ArrayList;

public class ErrorLogger {
  
  private final ArrayList<String> errors = new ArrayList<>();
  
  public ErrorLogger() {}
  
  public <T> T error(String message, T errorValue) {
    errors.add(message);
    return errorValue;
  }
  
  public <T> T error(String message) {
    return error(message, null);
  }
  
  public boolean dumpErrors(PrintStream out) {
    for (var error : errors) out.println(error);
    return !errors.isEmpty();
  }
  
  public static <T> Fixed<T> fixed(T errorValue) {
    return new ErrorLogger().fix(errorValue);
  }
  
  public <T> Fixed<T> fix(T errorValue) {
    return this.new Fixed<>(errorValue);
  }
  
  public class Fixed<T> {
    
    private final T errorValue;
    
    private Fixed(T errorValue) {
      this.errorValue = errorValue;
    }
    
    public T error(String message) {
      return ErrorLogger.this.error(message, errorValue);
    }
    
    public ErrorLogger parent() {
      return ErrorLogger.this;
    }
    
    public <U> Fixed<U> fix(U errorValue) {
      return ErrorLogger.this.fix(errorValue);
    }
    
    public boolean dumpErrors(PrintStream out) {
      return ErrorLogger.this.dumpErrors(out);
    }
    
  }
  
}
