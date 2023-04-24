package io.github.pint_lang.typechecker.conditions;

import java.util.HashMap;
import java.util.HashSet;

public class InputMapper {
  
  final HashMap<Condition, Input> source2target = new HashMap<>();
  final HashSet<Input> mappedTargets = new HashSet<>();
  
  InputMapper() {}
  
  public boolean matchOrBind(Condition source, Input target) {
    if (source == null) throw new NullPointerException("source must not be null");
    if (target == null) throw new NullPointerException("target must not be null");
    var mappedTarget = source2target.get(source);
    if (mappedTarget != null) return mappedTarget.equals(target);
    if (mappedTargets.contains(target)) return false;
    source2target.put(source, target);
    mappedTargets.add(target);
    return true;
  }
  
  public boolean hypothetically(Hypothesis hypothesis) {
    var scoped = new HypotheticalMapperMapper(this);
    var result = hypothesis.consider(scoped);
    if (result) scoped.save();
    return result;
  }
  
  @FunctionalInterface
  public interface Hypothesis {
    
    boolean consider(InputMapper hypotheticalInputs);
    
  }
  
  private static class HypotheticalMapperMapper extends InputMapper {
    
    private final InputMapper parent;
    
    
    private HypotheticalMapperMapper(InputMapper parent) {
      this.parent = parent;
    }
    
    @Override
    public boolean matchOrBind(Condition source, Input target) {
      if (source == null) throw new NullPointerException("source must not be null");
      if (target == null) throw new NullPointerException("target must not be null");
      var mappedTarget = source2target.get(source);
      if (mappedTarget != null) return mappedTarget.equals(target);
      mappedTarget = parent.source2target.get(source);
      if (mappedTarget != null) return mappedTarget.equals(target);
      if (mappedTargets.contains(target) || parent.mappedTargets.contains(target)) return false;
      source2target.put(source, target);
      mappedTargets.add(target);
      return true;
    }
    
    void save() {
      parent.source2target.putAll(source2target);
      parent.mappedTargets.addAll(mappedTargets);
    }
    
  }
  
}
