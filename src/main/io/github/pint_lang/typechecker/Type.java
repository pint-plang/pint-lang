package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.ExprAST;

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
    public Type.Array asArray() {
      return null;
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
    public Type.Array asArray() {
      return switch (this) {
        case ERROR -> null;
        case NEVER -> new Array(Type.NEVER);
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
    public Type.Array asArray() {
      return this;
    }

    @Override
    public String toString() {
      return elementType.toString() + "[]";
    }
    
  }

  record Condition(Type type, ExprAST<Type> condition) implements Type {

    public Condition {
      if (type == null) throw new NullPointerException("type must not be null");
    }

    @Override
    public boolean canBe(Type other) {
      return this.equals(other) || type.canBe(other) || other == Type.NEVER;
    }

    @Override
    public Type unify(Type other, ErrorLogger<Type> logger) {
      return this.equals(other) ? this :
             other instanceof Condition otherCondition ? this.type.unify(otherCondition.type, logger) :
             this.type.unify(other, logger);
    }

    @Override
    public Type.Array asArray() {
      return type.asArray();
    }

    @Override
    public Type unconditional() {
      return type.unconditional();
    }

  }

  // NOTE: this relationship will become a lot more important once type conditions exist
  boolean canBe(Type other);
  
  default boolean eitherCanBe(Type other) {
    return this.canBe(other) || other.canBe(this);
  }
  
  Type unify(Type other, ErrorLogger<Type> logger);

  Type.Array asArray();

  default boolean canBeArray() {
    return asArray() != null;
  }

  default Type unconditional() {
    return this;
  }

  @Override
  boolean equals(Object other);
  
  @Override
  int hashCode();
  
  @Override
  String toString();
  
}
