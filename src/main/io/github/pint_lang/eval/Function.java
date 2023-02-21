package io.github.pint_lang.eval;

import java.util.List;

public interface Function {
  
  default Value call(ExprEvalVisitor eval, Value... args) {
    return call(eval, List.of(args));
  }
  
  Value call(ExprEvalVisitor eval, List<Value> args);
  
}
