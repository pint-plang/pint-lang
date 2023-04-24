package io.github.pint_lang.typechecker;

public abstract sealed class JumpScope {
  
  private Type type;
  
  public JumpScope(Type type) {
    if (type == null) throw new NullPointerException("type must not be null");
    this.type = type;
  }
  
  public Type getType() {
    return type;
  }
  
  public Type unifyType(Type other, ErrorLogger.Fixed<Type> logger) {
    type = type.unify(other, logger);
    return type;
  }
  
  public sealed interface Labeled {
    
    String getLabel();
    
  }
  
  public sealed interface Anon {}
  
  public static final class LabeledOnly extends JumpScope implements Labeled {
    
    private final String label;
    
    public LabeledOnly(String label, Type defaultType) {
      super(defaultType);
      if (label == null) throw new NullPointerException("label must not be null");
      this.label = label;
    }
    
    public String getLabel() {
      return label;
    }
    
  }
  
  public static final class AnonOnly extends JumpScope implements Anon {
    
    public AnonOnly(Type defaultType) {
      super(defaultType);
    }
    
  }
  
  public static final class LabeledOrAnon extends JumpScope implements Labeled, Anon {
    
    private final String label;
    
    public LabeledOrAnon(String label, Type defaultType) {
      super(defaultType);
      if (label == null) throw new NullPointerException("label must not be null");
      this.label = label;
    }
    
    public String getLabel() {
      return label;
    }
    
  }
  
}
