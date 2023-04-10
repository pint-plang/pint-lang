package io.github.pint_lang;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;

import java.io.PrintStream;

public class PrintVisitor extends PintBaseVisitor<Void> {
  
  public final PrintStream out;
  
  public PrintVisitor(PrintStream out) {
    this.out = out;
  }
  
  @Override
  public Void visitFile(FileContext ctx) {
    for (var definition : ctx.def()) {
      definition.accept(this);
      out.println();
    }
    return null;
  }
  
  @Override
  public Void visitDef(DefContext ctx) {
    var funcDef = ctx.funcDef();
    if (funcDef != null) {
      funcDef.accept(this);
      return null;
    }
    var varDef = ctx.varDef();
    varDef.accept(this);
    return null;
  }
  
  @Override
  public Void visitFuncDef(FuncDefContext ctx) {
    out.print("let ");
    out.print(ctx.ID().getText());
    out.print("(");
    ctx.paramList().accept(this);
    out.print(") -> ");
    ctx.type().accept(this);
    out.print(" ");
    ctx.blockExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitVarDef(VarDefContext ctx) {
    out.print("let ");
    out.print(ctx.ID().getText());
    out.print(": ");
    ctx.type().accept(this);
    out.print(" := ");
    ctx.expr().accept(this);
    out.print(";");
    return null;
  }
  
  @Override
  public Void visitParamList(ParamListContext ctx) {
    for (var param : ctx.param()) {
      param.accept(this);
      out.print(", ");
    }
    return null;
  }
  
  @Override
  public Void visitParam(ParamContext ctx) {
    out.print(ctx.ID().getText());
    out.print(": ");
    ctx.type().accept(this);
    return null;
  }
  
  @Override
  public Void visitSimpleType(SimpleTypeContext ctx) {
    out.print(ctx.ID().getText());
    return null;
  }
  
  @Override
  public Void visitArrayType(ArrayTypeContext ctx) {
    ctx.type().accept(this);
    out.print("[]");
    return null;
  }
  
  @Override
  public Void visitConditionType(ConditionTypeContext ctx) {
    ctx.type().accept(this);
    out.print(" when ");
    ctx.expr().accept(this);
    return null;
  }
  
  @Override
  public Void visitUnitType(UnitTypeContext ctx) {
    out.print("unit");
    return null;
  }
  
  @Override
  public Void visitFactorExpr(FactorExprContext ctx) {
    ctx.factor().accept(this);
    return null;
  }
  
  @Override
  public Void visitUnaryExpr(UnaryExprContext ctx) {
    out.print(ctx.op.getText());
    ctx.expr().accept(this);
    return null;
  }
  
  @Override
  public Void visitMulExpr(MulExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitAddExpr(AddExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitCmpExpr(CmpExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    if (ctx.not != null) out.print("not ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitAndExpr(AndExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitOrExpr(OrExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitAssignExpr(AssignExprContext ctx) {
    ctx.left.accept(this);
    out.print(" ");
    out.print(ctx.op.getText());
    out.print(" ");
    ctx.right.accept(this);
    return null;
  }
  
  @Override
  public Void visitLabeledBlockFactor(LabeledBlockFactorContext ctx) {
    ctx.labeledBlockExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitParensFactor(ParensFactorContext ctx) {
    out.print("(");
    ctx.expr().accept(this);
    out.print(")");
    return null;
  }
  
  @Override
  public Void visitAbsFactor(AbsFactorContext ctx) {
    out.print("|");
    ctx.expr().accept(this);
    out.print("|");
    return null;
  }
  
  @Override
  public Void visitVarFactor(VarFactorContext ctx) {
    out.print(ctx.ID().getText());
    return null;
  }
  
  @Override
  public Void visitFuncCallFactor(FuncCallFactorContext ctx) {
    ctx.funcCallExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitControlFlowFactor(ControlFlowFactorContext ctx) {
    ctx.controlFlowExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitIndexFactor(IndexFactorContext ctx) {
    ctx.factor().accept(this);
    ctx.indexOp().accept(this);
    return null;
  }
  
  @Override
  public Void visitArrayLiteralFactor(ArrayLiteralFactorContext ctx) {
    ctx.arrayLiteral().accept(this);
    return null;
  }
  
  @Override
  public Void visitLiteralFactor(LiteralFactorContext ctx) {
    ctx.literal().accept(this);
    return null;
  }
  
  @Override
  public Void visitItFactor(ItFactorContext ctx) {
    out.print("it");
    return null;
  }
  
  @Override
  public Void visitLabeledBlockExpr(LabeledBlockExprContext ctx) {
    var label = ctx.label();
    if (label != null) label.accept(this);
    ctx.blockExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitLabel(LabelContext ctx) {
    out.print(ctx.ID().getText());
    out.print(": ");
    return null;
  }
  
  @Override
  public Void visitBlockExpr(BlockExprContext ctx) {
    out.print("{ ");
    for (var statement : ctx.statement()) {
      statement.accept(this);
      out.print(" ");
    }
    var expr = ctx.expr();
    if (expr != null) {
      expr.accept(this);
      out.print(" ");
    }
    out.print("}");
    return null;
  }
  
  @Override
  public Void visitStatement(StatementContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) {
      varDef.accept(this);
    } else {
      var expr = ctx.expr();
      if (expr != null) expr.accept(this);
      out.print(";");
    }
    return null;
  }
  
  @Override
  public Void visitFuncCallExpr(FuncCallExprContext ctx) {
    out.print(ctx.ID().getText());
    out.print("(");
    for (var expr : ctx.expr()) {
      expr.accept(this);
      out.print(", ");
    }
    out.print(")");
    return null;
  }
  
  @Override
  public Void visitControlFlowExpr(ControlFlowExprContext ctx) {
    var ifExpr = ctx.ifExpr();
    if (ifExpr != null) {
      ifExpr.accept(this);
      return null;
    }
    var loopExpr = ctx.loopExpr();
    if (loopExpr != null) {
      loopExpr.accept(this);
      return null;
    }
    var whileExpr = ctx.whileExpr();
    if (whileExpr != null) {
      whileExpr.accept(this);
      return null;
    }
    ctx.jumpExpr().accept(this);
    return null;
  }
  
  @Override
  public Void visitIfExpr(IfExprContext ctx) {
    out.print("if ");
    ctx.cond.accept(this);
    out.print(" then ");
    ctx.thenBody.accept(this);
    if (ctx.elseBody != null) {
      out.print(" else ");
      ctx.elseBody.accept(this);
    }
    return null;
  }
  
  @Override
  public Void visitLoopExpr(LoopExprContext ctx) {
    var label = ctx.label();
    if (label != null) label.accept(this);
    out.print("loop ");
    ctx.body.accept(this);
    return null;
  }
  
  @Override
  public Void visitWhileExpr(WhileExprContext ctx) {
    var label = ctx.label();
    if (label != null) label.accept(this);
    out.print("while ");
    ctx.cond.accept(this);
    out.print(" loop ");
    ctx.body.accept(this);
    return null;
  }
  
  @Override
  public Void visitJumpExpr(JumpExprContext ctx) {
    out.print(ctx.jump.getText());
    var atLabel = ctx.atLabel();
    if (atLabel != null) atLabel.accept(this);
    var expr = ctx.expr();
    if (expr != null) {
      out.print(" ");
      expr.accept(this);
    }
    return null;
  }
  
  @Override
  public Void visitAtLabel(AtLabelContext ctx) {
    out.print("@");
    out.print(ctx.ID().getText());
    return null;
  }
  
  @Override
  public Void visitIndexIndexOp(IndexIndexOpContext ctx) {
    out.print("[");
    ctx.expr().accept(this);
    out.print("]");
    return null;
  }
  
  @Override
  public Void visitSliceIndexOp(SliceIndexOpContext ctx) {
    out.print("[");
    if (ctx.from != null) ctx.from.accept(this);
    out.print("...");
    if (ctx.to != null) ctx.to.accept(this);
    out.print("]");
    return null;
  }
  
  @Override
  public Void visitArrayLiteral(ArrayLiteralContext ctx) {
    out.print("[");
    for (var arrayLiteralItem : ctx.arrayLiteralItem()) {
      arrayLiteralItem.accept(this);
      out.print(", ");
    }
    out.print("]");
    return null;
  }
  
  @Override
  public Void visitArrayLiteralItem(ArrayLiteralItemContext ctx) {
    if (ctx.spread != null) out.print("...");
    ctx.expr().accept(this);
    return null;
  }
  
  @Override
  public Void visitLiteral(LiteralContext ctx) {
    if (ctx.int_ != null) out.print(ctx.int_.getText());
    else if (ctx.string != null) out.print(ctx.string.getText());
    else if (ctx.bool !=  null) out.print(ctx.bool.getText());
    else out.print(ctx.unit.getText());
    return null;
  }
  
}
