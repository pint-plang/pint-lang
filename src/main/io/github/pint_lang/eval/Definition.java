package io.github.pint_lang.eval;

import io.github.pint_lang.gen.PintParser.*;
import io.github.pint_lang.eval.ExprEvalControlFlow.*;

import java.util.List;

public sealed interface Definition {
  
  String name();
  
  record Variable(String name, ExprContext valueCst) implements Definition {
    
    public Variable {
      if (name == null) throw new NullPointerException("name must not be null");
      if (valueCst == null) throw new NullPointerException("valueCst must not be null");
    }
    
  }
  
  record Function(String name, String[] params, BlockExprContext bodyCst) implements Definition, io.github.pint_lang.eval.Function {
    
    public Function {
      if (name == null) throw new NullPointerException("name must not be null");
      if (params == null) throw new NullPointerException("params must not be null");
      if (bodyCst == null) throw new NullPointerException("bodyCst must not be null");
    }
    
    @Override
    public Value call(ExprEvalVisitor eval, List<Value> args) {
      if (eval == null) throw new NullPointerException("eval must not be null");
      if (args == null) throw new NullPointerException("args must not be null");
      if (args.size() != params.length) throw new IllegalArgumentException("Expected " + params.length + " arguments; got " + args.size());
      var scope = eval.stack.peek();
      for (var i = 0; i < params.length; i++) scope.defineVariable(params[i], args.get(i));
      var bodyFlow = bodyCst.accept(eval);
      if (bodyFlow instanceof Finish bodyFinish) return bodyFinish.value();
      else if (bodyFlow instanceof Return bodyReturn) return bodyReturn.value();
      else if (bodyFlow instanceof Labeled bodyLabeled) throw new BadJumpException(bodyLabeled.label() != null ? "Missing label '" + bodyLabeled.label() + "' (did you make a typo?)" : "Unlabeled jump outside of any loop");
      else throw new IllegalStateException("Invalid function exit");
    }
    
  }
  
}
