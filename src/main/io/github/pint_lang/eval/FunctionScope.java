package io.github.pint_lang.eval;

import java.util.Optional;

public class FunctionScope extends LocalScope {
  
  public final Scope caller;
  
  public FunctionScope(Scope caller) {
    super(caller.global());
    this.caller = caller;
  }
  
  @Override
  public Optional<FunctionScope> function() {
    return Optional.of(this);
  }
  
}
