package io.github.pint_lang.typechecker;

import java.io.PrintStream;
import java.util.ArrayList;

public class ErrorLogger<T> {
  
  private final ArrayList<String> errors = new ArrayList<>();
  private final T errorValue;
  
  public ErrorLogger(T errorValue) {
    this.errorValue = errorValue;
  }
  
  public T error(String message) {
    errors.add(message);
    return errorValue;
  }
  
  public boolean dumpErrors(PrintStream out) {
    for (var error : errors) out.println(error);
    return !errors.isEmpty();
  }
  
}
