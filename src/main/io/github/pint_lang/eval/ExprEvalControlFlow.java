package io.github.pint_lang.eval;

public sealed interface ExprEvalControlFlow {
  
  sealed interface Valued extends ExprEvalControlFlow {
    
    Value value();
    
  }
  
  sealed interface Labeled extends ExprEvalControlFlow {
    
    String label();
    
  }
  
  // Evaluation actually completed rather than being cut short with a jump expression (i.e. return, break or continue)
  record Finish(Value value) implements ExprEvalControlFlow, Valued {}
  
  record Return(Value value) implements ExprEvalControlFlow, Valued {}
  
  record Break(String label, Value value) implements ExprEvalControlFlow, Valued, Labeled {}
  
  record Continue(String label) implements ExprEvalControlFlow, Labeled {}
  
}
