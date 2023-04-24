package io.github.pint_lang.typechecker.conditions;

import java.util.HashMap;
import java.util.Map;

public class ConditionBindings {
  
  private final Map<Input, String> input2Var;
  private final Map<String, Input> var2Input;
  private final Input itInput;
  
  private ConditionBindings(HashMap<Input, String> input2Var, HashMap<String, Input> var2Input, Input itInput) {
    this.input2Var = Map.copyOf(input2Var);
    this.var2Input = Map.copyOf(var2Input);
    this.itInput = itInput;
  }
  
  public String getVar(Input input) {
    return input2Var.get(input);
  }
  
  public Input getVarInput(String var) {
    return var2Input.get(var);
  }
  
  public Input getItInput() {
    return itInput;
  }
  
  public Merge merge(ConditionBindings other) {
    var thisToNewMapping = new HashMap<Input, Input>(input2Var.size() + 1);
    var otherToNewMapping = new HashMap<Input, Input>(other.input2Var.size() + 1);
    var builder = new Builder();
    if (itInput != null) thisToNewMapping.put(itInput, builder.getOrBindIt());
    if (other.itInput != null) otherToNewMapping.put(other.itInput, builder.getOrBindIt());
    for (var entry : input2Var.entrySet()) thisToNewMapping.put(entry.getKey(), builder.getOrBindVar(entry.getValue()));
    for (var entry : other.input2Var.entrySet()) otherToNewMapping.put(entry.getKey(), builder.getOrBindVar(entry.getValue()));
    return new Merge(builder.finishAndReset(), thisToNewMapping, otherToNewMapping);
  }
  
  @Override
  public boolean equals(Object o) {
    // This is close, but I'm not 100% sure that this is exactly correct; it should be verified at some point
    return o instanceof ConditionBindings other && (itInput != null) == (other.itInput != null) && var2Input.keySet().equals(other.var2Input.keySet());
  }
  
  @Override
  public int hashCode() {
    // This is as correct as .equals()
    return itInput.hashCode() + var2Input.keySet().hashCode();
  }
  
  public static class Builder {
    
    private final HashMap<Input, String> input2Var = new HashMap<>();
    private final HashMap<String, Input> var2Input = new HashMap<>();
    private Input itInput = null;
    private Input.Generator inputs = new Input.Generator();
    
    public Input getOrBindVar(String var) {
      Input input = var2Input.computeIfAbsent(var, ignored -> inputs.next());
      input2Var.put(input, var);
      return input;
    }
    
    public Input getOrBindIt() {
      return itInput == null ? itInput = inputs.next() : itInput;
    }
    
    public String getBoundVar(Input input) {
      return input2Var.get(input);
    }
    
    public Input getVarBinding(String var) {
      return var2Input.get(var);
    }
    
    public Input getItBinding() {
      return itInput;
    }
    
    public ConditionBindings finishAndReset() {
      var bindings = new ConditionBindings(input2Var, var2Input, itInput);
      input2Var.clear();
      var2Input.clear();
      itInput = null;
      inputs = new Input.Generator();
      return bindings;
    }
    
  }
  
  public record Merge(ConditionBindings merged, HashMap<Input, Input> thisToNewMapping, HashMap<Input, Input> otherToNewMapping) {}
  
}
