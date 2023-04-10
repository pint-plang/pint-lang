package io.github.pint_lang;

import io.github.pint_lang.ast.*;

import java.io.PrintStream;

public class ASTPrintVisitor implements ASTVisitor<Void, Void> {
  
  public final PrintStream out;
  
  public ASTPrintVisitor(PrintStream out) {
    this.out = out;
  }
  
  @Override
  public Void visitDefs(DefsAST<Void> ast) {
    for (var def : ast.defs()) {
      def.accept(this);
      out.println();
    }
    return null;
  }
  
  @Override
  public Void visitFuncDef(FuncDefAST<Void> ast) {
    out.print("let ");
    out.print(ast.name());
    out.print("(");
    for (var param : ast.params()) {
      out.print(param.name());
      out.print(": ");
      param.type().accept(this);
      out.print(", ");
    }
    out.print(") -> ");
    ast.returnType().accept(this);
    out.print(" ");
    ast.body().accept(this);
    return null;
  }
  
  @Override
  public Void visitVarDef(VarDefAST<Void> ast) {
    out.print("let ");
    out.print(ast.name());
    out.print(": ");
    ast.type().accept(this);
    out.print(" := ");
    ast.value().accept(this);
    out.print(";");
    return null;
  }
  
  @Override
  public Void visitUnaryExpr(UnaryExprAST<Void> ast) {
    out.print(switch (ast.op()) {
      case PLUS -> "+";
      case NEG -> "-";
      case NOT -> "not ";
      case ABS -> "|";
    });
    ast.operand().accept(this);
    out.print(switch (ast.op()) {
      case PLUS, NEG, NOT -> "";
      case ABS -> "|";
    });
    return null;
  }
  
  @Override
  public Void visitBinaryExpr(BinaryExprAST<Void> ast) {
    ast.left().accept(this);
    out.print(switch (ast.op()) {
      case ASSIGN -> " := ";
      case ADD_ASSIGN -> " :+= ";
      case SUB_ASSIGN -> " :-= ";
      case MUL_ASSIGN -> " :*= ";
      case DIV_ASSIGN -> " :/= ";
      case OR -> " or ";
      case AND -> " and ";
      case EQ -> " = ";
      case NEQ -> " not = ";
      case LT -> " < ";
      case NLT -> " not < ";
      case LE -> " <= ";
      case NLE -> " not <= ";
      case GT -> " > ";
      case NGT -> " not > ";
      case GE -> " >= ";
      case NGE -> " not >= ";
      case ADD -> " + ";
      case SUB -> " - ";
      case MUL -> " * ";
      case DIV -> " / ";
    });
    ast.right().accept(this);
    return null;
  }
  
  @Override
  public Void visitBlockExpr(BlockExprAST<Void> ast) {
    if (ast.label() != null) {
      out.print(ast.label());
      out.print(": ");
    }
    out.print("{ ");
    for (var stat : ast.stats()) {
      stat.accept(this);
      if (stat instanceof ExprAST<Void>) out.print("; ");
      else out.print(" ");
    }
    out.print("}");
    return null;
  }
  
  @Override
  public Void visitVarExprAST(VarExprAST<Void> ast) {
    out.print(ast.name());
    return null;
  }
  
  @Override
  public Void visitFuncCall(FuncCallExprAST<Void> ast) {
    out.print(ast.funcName());
    out.print("(");
    for (var arg : ast.args()) {
      arg.accept(this);
      out.print(", ");
    }
    out.print(")");
    return null;
  }
  
  @Override
  public Void visitIndexExpr(IndexExprAST<Void> ast) {
    ast.indexee().accept(this);
    out.print("[");
    ast.index().accept(this);
    out.print("]");
    return null;
  }
  
  @Override
  public Void visitSliceExpr(SliceExprAST<Void> ast) {
    ast.slicee().accept(this);
    out.print("[");
    if (ast.from() != null) ast.from().accept(this);
    out.print("...");
    if (ast.to() != null) ast.to().accept(this);
    out.print("]");
    return null;
  }
  
  
  @Override
  public Void visitItExpr(ItExprAST<Void> ast) {
    out.print("it");
    return null;
  }
  
  @Override
  public Void visitIfExpr(IfExprAST<Void> ast) {
    out.print("if ");
    ast.condition().accept(this);
    out.print(" then ");
    ast.thenBody().accept(this);
    if (ast.elseBody() != null) {
      out.print(" else ");
      ast.elseBody().accept(this);
    }
    return null;
  }
  
  @Override
  public Void visitLoopExpr(LoopExprAST<Void> ast) {
    if (ast.label() != null) {
      out.print(ast.label());
      out.print(": ");
    }
    out.print("loop ");
    ast.body().accept(this);
    return null;
  }
  
  @Override
  public Void visitWhileExpr(WhileExprAST<Void> ast) {
    if (ast.label() != null) {
      out.print(ast.label());
      out.print(": ");
    }
    out.print("while ");
    ast.condition().accept(this);
    out.print(" loop ");
    ast.body().accept(this);
    return null;
  }
  
  @Override
  public Void visitJumpExpr(JumpExprAST<Void> ast) {
    out.print(switch (ast.kind()) {
      case RETURN -> "return";
      case BREAK -> "break";
      case CONTINUE -> "continue";
    });
    if (ast.targetLabel() != null) {
      out.print("@");
      out.print(ast.targetLabel());
    }
    if (ast.value() != null) {
      out.print(" ");
      ast.value().accept(this);
    }
    return null;
  }
  
  @Override
  public Void visitArrayLiteralExpr(ArrayLiteralExprAST<Void> ast) {
    out.print("[");
    for (var item : ast.items()) {
      if (item.spread()) out.print("...");
      item.item().accept(this);
      out.print(", ");
    }
    out.print("]");
    return null;
  }
  
  @Override
  public Void visitStringLiteralExpr(StringLiteralExprAST<Void> ast) {
    out.print("\"");
    out.print(ast.value());
    out.print("\"");
    return null;
  }
  
  @Override
  public Void visitIntLiteralExpr(IntLiteralExprAST<Void> ast) {
    out.print(ast.value());
    return null;
  }
  
  @Override
  public Void visitBoolLiteralExpr(BoolLiteralExprAST<Void> ast) {
    out.print(ast.value());
    return null;
  }
  
  @Override
  public Void visitUnitLiteralExpr(UnitLiteralExprAST<Void> ast) {
    out.print("unit");
    return null;
  }
  
  @Override
  public Void visitNopStat(NopStatAST<Void> ast) {
    out.print(";");
    return null;
  }
  
  @Override
  public Void visitSimpleType(SimpleTypeAST<Void> ast) {
    out.print(ast.name());
    return null;
  }
  
  @Override
  public Void visitUnitType(UnitTypeAST<Void> ast) {
    out.print("unit");
    return null;
  }
  
  @Override
  public Void visitArrayType(ArrayTypeAST<Void> ast) {
    ast.innerType().accept(this);
    out.print("[]");
    return null;
  }
  
  @Override
  public Void visitConditionType(ConditionTypeAST<Void> ast) {
    ast.type().accept(this);
    out.print(" when ");
    ast.condition().accept(this);
    return null;
  }
  
}
