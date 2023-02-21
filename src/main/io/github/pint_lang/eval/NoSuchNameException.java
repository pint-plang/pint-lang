package io.github.pint_lang.eval;

public class NoSuchNameException extends RuntimeException {
  
  private NoSuchNameException(String what, String name) {
    super("No such " + what + " as '" + name + "'");
  }
  
  public static NoSuchNameException variable(String name) {
    return new NoSuchNameException("variable", name);
  }
  
  public static NoSuchNameException function(String name) {
    return new NoSuchNameException("function", name);
  }
  
}
