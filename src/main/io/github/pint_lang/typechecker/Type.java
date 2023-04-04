package io.github.pint_lang.typechecker;

public sealed interface Type {
  
  Type
    STRING = Primitive.STRING,
    INT = Primitive.INT,
    BOOL = Primitive.BOOL,
    UNIT = Primitive.UNIT,
    ERROR = Inferred.ERROR,
    NEVER = Inferred.NEVER;
  
  enum Primitive implements Type {
    
    STRING,
    INT,
    BOOL,
    UNIT;
  
    @Override
    public boolean canBe(Type other) {
      return other == this;
    }
    
    @Override
    public Type unify(Type other, ErrorLogger<Type> logger) {
      return other == this || other == Type.NEVER ? this : other == Type.ERROR ? other : logger.error("Failed to unify types '" + this + "' and '" + other + "'");
    }
    
    
    @Override
    public String toString() {
      return switch (this) {
        case STRING -> "string";
        case INT -> "int";
        case BOOL -> "bool";
        case UNIT -> "unit";
      };
    }
    
  }
  
  enum Inferred implements Type {
    
    ERROR,
    NEVER;
    
    @Override
    public boolean canBe(Type other) {
      return switch (this) {
        case ERROR -> false;
        case NEVER -> true;
      };
    }
  
    @Override
    public Type unify(Type other, ErrorLogger<Type> logger) {
      return switch (this) {
        case ERROR -> this;
        case NEVER -> other;
      };
    }
  
  
    @Override
    public String toString() {
      return switch (this) {
        case ERROR -> "[error]";
        case NEVER -> "[never]";
      };
    }
    
  }
  
  record Array(Type elementType) implements Type {
  
    @Override
    public boolean canBe(Type other) {
      // NOTE: there is a special case so that never[] can be any array type; this way, [] can have type never[]
      // NOTE: elementType.canBe(otherArray.elementType) would make arrays covariant, which may not be desirable
      // NOTE: (in this case, the only way to observe this is by use of never (nested, to avoid the never special case), e.g. let x: int[][] = [[loop {}]] would typecheck with covariance, but doesn't like this)
      return other instanceof Array otherArray && (elementType.equals(otherArray.elementType) || elementType == Type.NEVER);
    }
  
    @Override
    public Type unify(Type other, ErrorLogger<Type> logger) {
      return other instanceof Array otherArray ? new Array(elementType.unify(otherArray.elementType, logger)) : other == Type.NEVER ? this : other == Type.ERROR ? other : logger.error("Failed to unify types '" + this + "' and '" + other + "'");
    }
    
    @Override
    public String toString() {
      return elementType.toString() + "[]";
    }
    
  }
  
  // NOTE: this relationship will become a lot more important once type conditions exist
  boolean canBe(Type other);
  
  default boolean eitherCanBe(Type other) {
    return this.canBe(other) || other.canBe(this);
  }
  
  Type unify(Type other, ErrorLogger<Type> logger);
  
  @Override
  boolean equals(Object other);
  
  @Override
  int hashCode();
  
  @Override
  String toString();
  
}
