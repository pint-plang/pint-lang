package io.github.pint_lang.ast;

public enum BinaryOp {
  
  ASSIGN, // :=
  ADD_ASSIGN, // :+=
  SUB_ASSIGN, // :-=
  MUL_ASSIGN, // :*=
  DIV_ASSIGN, // :/=
  OR, // or
  AND, // and
  EQ, // =
  NEQ, // not =
  LT, // <
  NLT, // not <
  LE, // <=
  NLE, // not <=
  GT, // >
  NGT, // not >
  GE, // >=
  NGE, // not >=
  ADD, // +
  SUB, // -
  MUL, // *
  DIV; // /
  
  public boolean shortCircuits() {
    return switch (this) {
      case OR, AND -> true;
      default -> isAssign();
    };
  }
  
  public boolean isAssign() {
    return switch (this) {
      case ASSIGN, ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN -> true;
      default -> false;
    };
  }
  
}
