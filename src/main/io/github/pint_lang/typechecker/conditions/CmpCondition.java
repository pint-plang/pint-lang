package io.github.pint_lang.typechecker.conditions;

import java.util.Map;

public record CmpCondition(Kind kind, Condition left, Condition right) implements Condition {
  
  public CmpCondition {
    if (kind == null) throw new NullPointerException("kind must not be null");
    if (left == null) throw new NullPointerException("left must not be null");
    if (right == null) throw new NullPointerException("right must not be null");
  }
  
  public static CmpCondition eq(Condition left, Condition right) {
    return new CmpCondition(Kind.EQ, left, right); // l = r
  }
  
  public static CmpCondition neq(Condition left, Condition right) {
    return new CmpCondition(Kind.NEQ, left, right); // l not = r
  }
  
  public static CmpCondition lt(Condition left, Condition right) {
    return new CmpCondition(Kind.LT, left, right); // l < r
  }
  
  public static CmpCondition le(Condition left, Condition right) {
    return new CmpCondition(Kind.LTE, left, right); // l <= r
  }
  
  public static CmpCondition gt(Condition left, Condition right) {
    return new CmpCondition(Kind.LT, right, left); // l > r -> r < l
  }
  
  public static CmpCondition ge(Condition left, Condition right) {
    return new CmpCondition(Kind.LTE, right, left); // l >= r -> r <= l
  }
  
  public static CmpCondition nlt(Condition left, Condition right) {
    return new CmpCondition(Kind.LTE, right, left); // l not < r -> l >= r -> r <= l
  }
  
  public static CmpCondition nle(Condition left, Condition right) {
    return new CmpCondition(Kind.LT, right, left); // l not <= r -> l > r -> r < l
  }
  
  public static CmpCondition ngt(Condition left, Condition right) {
    return new CmpCondition(Kind.LTE, left, right); // l not > r -> l <= r
  }
  
  public static CmpCondition nge(Condition left, Condition right) {
    return new CmpCondition(Kind.LT, left, right); // l not >= r -> l < r
  }
  
  public enum Kind {
    
    EQ,
    NEQ,
    LT,
    LTE;
    
    private boolean symmetric() {
      return switch (this) {
        case EQ, NEQ -> true;
        case LT, LTE -> false;
      };
    }
    
    private boolean satisfiedBy(Kind source) {
      return switch (this) {
        case EQ -> source == EQ;
        case NEQ -> source == NEQ || source == LT;
        case LT -> source == LT;
        case LTE -> source == LTE || source == LT || source == EQ;
      };
    }
    
  }
  
  @Override
  public boolean satisfiedBy(Condition source, InputMapper inputs) {
    return
      source instanceof CmpCondition sourceCmp
        && (
          left.hypotheticallySatisfiedBy(sourceCmp.left, inputs)
            && right.hypotheticallySatisfiedBy(sourceCmp.right, inputs)
          || sourceCmp.kind.symmetric()
            && left.hypotheticallySatisfiedBy(sourceCmp.right, inputs)
            && right.hypotheticallySatisfiedBy(sourceCmp.left, inputs)
        )
        && kind.satisfiedBy(sourceCmp.kind)
    ;
  }
  
  @Override
  public CmpCondition mapInputs(Map<Input, Input> mapping) {
    return new CmpCondition(kind, left.mapInputs(mapping), right.mapInputs(mapping));
  }
  
}
