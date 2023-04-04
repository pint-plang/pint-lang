package io.github.pint_lang.ast;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;

import static java.util.stream.Collectors.toList;

public class ASTConversionVisitor extends PintBaseVisitor<AST<Void>> {
  
  @Override
  public DefsAST<Void> visitFile(FileContext ctx) {
    return new DefsAST<>(ctx.def().stream().map(this::visitDef).toList(), null);
  }
  
  @Override
  public DefAST<Void> visitDef(DefContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) return visitVarDef(varDef);
    return visitFuncDef(ctx.funcDef());
  }
  
  @Override
  public FuncDefAST<Void> visitFuncDef(FuncDefContext ctx) {
    return new FuncDefAST<>(
      ctx.ID().getText(),
      ctx.paramList().param().stream().map(param -> new FuncDefAST.Param<>(param.ID().getText(), visitType(param.type()))).toList(),
      visitType(ctx.type()),
      visitBlockExpr(ctx.blockExpr()),
      null
    );
  }
  
  @Override
  public VarDefAST<Void> visitVarDef(VarDefContext ctx) {
    return new VarDefAST<>(
      ctx.ID().getText(),
      visitType(ctx.type()),
      visitExpr(ctx.expr()),
      null
    );
  }
  
  public TypeAST<Void> visitType(TypeContext ctx) {
    return (TypeAST<Void>) ctx.accept(this);
  }
  
  @Override
  public SimpleTypeAST<Void> visitSimpleType(SimpleTypeContext ctx) {
    return new SimpleTypeAST<>(ctx.ID().getText(), null);
  }
  
  @Override
  public UnitTypeAST<Void> visitUnitType(UnitTypeContext ctx) {
    return new UnitTypeAST<>(null);
  }
  
  @Override
  public ArrayTypeAST<Void> visitArrayType(ArrayTypeContext ctx) {
    return new ArrayTypeAST<>(visitType(ctx.type()), null);
  }
  
  @Override
  public ConditionTypeAST<Void> visitConditionType(ConditionTypeContext ctx) {
    return new ConditionTypeAST<>(visitType(ctx.type()), visitExpr(ctx.expr()), null);
  }
  
  public ExprAST<Void> visitExpr(ExprContext ctx) {
    return (ExprAST<Void>) ctx.accept(this);
  }
  
  @Override
  public ExprAST<Void> visitFactorExpr(FactorExprContext ctx) {
    return visitFactor(ctx.factor());
  }
  
  @Override
  public UnaryExprAST<Void> visitUnaryExpr(UnaryExprContext ctx) {
    var op = switch (ctx.op.getText()) {
      case "+" -> UnaryOp.PLUS;
      case "-" -> UnaryOp.NEG;
      case "not" -> UnaryOp.NOT;
      default -> throw new IllegalStateException("invalid unary operator");
    };
    return new UnaryExprAST<>(op, visitExpr(ctx.expr()), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitMulExpr(MulExprContext ctx) {
    var op = switch (ctx.op.getText()) {
      case "*" -> BinaryOp.MUL;
      case "/" -> BinaryOp.DIV;
      default ->  throw new IllegalStateException("invalid multiplication operator");
    };
    return new BinaryExprAST<>(op, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitAddExpr(AddExprContext ctx) {
    var op = switch (ctx.op.getText()) {
      case "+" -> BinaryOp.ADD;
      case "-" -> BinaryOp.SUB;
      default ->  throw new IllegalStateException("invalid addition operator");
    };
    return new BinaryExprAST<>(op, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitCmpExpr(CmpExprContext ctx) {
    var not = ctx.not != null;
    var op = switch (ctx.op.getText()) {
      case "=" -> not ? BinaryOp.NEQ : BinaryOp.EQ;
      case "<" -> not ? BinaryOp.NLT : BinaryOp.LT;
      case ">" -> not ? BinaryOp.NGT : BinaryOp.GT;
      case "<=" -> not ? BinaryOp.NLE : BinaryOp.LE;
      case ">=" -> not ? BinaryOp.NGE : BinaryOp.GE;
      default ->  throw new IllegalStateException("invalid comparison operator");
    };
    return new BinaryExprAST<>(op, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitAndExpr(AndExprContext ctx) {
    return new BinaryExprAST<>(BinaryOp.AND, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitOrExpr(OrExprContext ctx) {
    return new BinaryExprAST<>(BinaryOp.OR, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  @Override
  public BinaryExprAST<Void> visitAssignExpr(AssignExprContext ctx) {
    var op = switch (ctx.op.getText()) {
      case ":=" -> BinaryOp.ASSIGN;
      case ":+=" -> BinaryOp.ADD_ASSIGN;
      case ":-=" -> BinaryOp.SUB_ASSIGN;
      case ":*=" -> BinaryOp.MUL_ASSIGN;
      case ":/=" -> BinaryOp.DIV_ASSIGN;
      default ->  throw new IllegalStateException("invalid assignment operator");
    };
    return new BinaryExprAST<>(op, visitExpr(ctx.left), visitExpr(ctx.right), null);
  }
  
  public ExprAST<Void> visitFactor(FactorContext ctx) {
    return (ExprAST<Void>) ctx.accept(this);
  }
  
  @Override
  public BlockExprAST<Void> visitLabeledBlockFactor(LabeledBlockFactorContext ctx) {
    return visitLabeledBlockExpr(ctx.labeledBlockExpr());
  }
  
  @Override
  public ExprAST<Void> visitParensFactor(ParensFactorContext ctx) {
    return visitExpr(ctx.expr());
  }
  
  @Override
  public UnaryExprAST<Void> visitAbsFactor(AbsFactorContext ctx) {
    return new UnaryExprAST<>(UnaryOp.ABS, visitExpr(ctx.expr()), null);
  }
  
  @Override
  public VarExprAST<Void> visitVarFactor(VarFactorContext ctx) {
    return new VarExprAST<>(ctx.ID().getText(), null);
  }
  
  @Override
  public FuncCallExprAST<Void> visitFuncCallFactor(FuncCallFactorContext ctx) {
    return visitFuncCallExpr(ctx.funcCallExpr());
  }
  
  @Override
  public ExprAST<Void> visitControlFlowFactor(ControlFlowFactorContext ctx) {
    return visitControlFlowExpr(ctx.controlFlowExpr());
  }
  
  @Override
  public IndexExprAST<Void> visitIndexFactor(IndexFactorContext ctx) {
    return new IndexExprAST<>(visitFactor(ctx.factor()), visitIndexOp(ctx.indexOp()), null);
  }
  
  @Override
  public ArrayLiteralExprAST<Void> visitArrayLiteralFactor(ArrayLiteralFactorContext ctx) {
    return visitArrayLiteral(ctx.arrayLiteral());
  }
  
  @Override
  public ExprAST<Void> visitLiteralFactor(LiteralFactorContext ctx) {
    return visitLiteral(ctx.literal());
  }
  
  @Override
  public ItExprAST<Void> visitItFactor(ItFactorContext ctx) {
    return new ItExprAST<>(null);
  }
  
  @Override
  public BlockExprAST<Void> visitLabeledBlockExpr(LabeledBlockExprContext ctx) {
    var label = ctx.label();
    var blockExpr = visitBlockExpr(ctx.blockExpr());
    return label != null ? new BlockExprAST<>(label.ID().getText(), blockExpr.stats(), blockExpr.data()) : blockExpr;
  }
  
  @Override
  public BlockExprAST<Void> visitBlockExpr(BlockExprContext ctx) {
    var stats = ctx.statement().stream().map(this::visitStatement).collect(toList());
    var expr = ctx.expr();
    if (expr != null) stats.add(visitExpr(expr));
    return new BlockExprAST<>(null, stats, null);
  }
  
  @Override
  public StatAST<Void> visitStatement(StatementContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) return visitVarDef(varDef);
    var expr = ctx.expr();
    if (expr != null) return visitExpr(expr);
    return new NopStatAST<>(null);
  }
  
  @Override
  public FuncCallExprAST<Void> visitFuncCallExpr(FuncCallExprContext ctx) {
    return new FuncCallExprAST<>(ctx.ID().getText(), ctx.expr().stream().map(this::visitExpr).toList(), null);
  }
  
  @Override
  public ExprAST<Void> visitControlFlowExpr(ControlFlowExprContext ctx) {
    var ifExpr = ctx.ifExpr();
    if (ifExpr != null) return visitIfExpr(ifExpr);
    var loopExpr = ctx.loopExpr();
    if (loopExpr != null) return visitLoopExpr(loopExpr);
    var whileExpr = ctx.whileExpr();
    if (whileExpr != null) return visitWhileExpr(whileExpr);
    return visitJumpExpr(ctx.jumpExpr());
  }
  
  @Override
  public IfExprAST<Void> visitIfExpr(IfExprContext ctx) {
    return new IfExprAST<>(visitExpr(ctx.cond), visitExpr(ctx.thenBody), ctx.elseBody != null ? visitExpr(ctx.elseBody) : null, null);
  }
  
  @Override
  public LoopExprAST<Void> visitLoopExpr(LoopExprContext ctx) {
    var label = ctx.label();
    return new LoopExprAST<>(label != null ? label.ID().getText() : null, visitExpr(ctx.body), null);
  }
  
  @Override
  public WhileExprAST<Void> visitWhileExpr(WhileExprContext ctx) {
    var label = ctx.label();
    return new WhileExprAST<>(label != null ? label.ID().getText() : null, visitExpr(ctx.cond), visitExpr(ctx.body), null);
  }
  
  @Override
  public JumpExprAST<Void> visitJumpExpr(JumpExprContext ctx) {
    var kind = switch (ctx.jump.getText()) {
      case "return" -> JumpKind.RETURN;
      case "break" -> JumpKind.BREAK;
      case "continue" -> JumpKind.CONTINUE;
      default -> throw new IllegalStateException("invalid jump expression");
    };
    var atLabel = ctx.atLabel();
    var expr = ctx.expr();
    return new JumpExprAST<>(kind, atLabel != null ? atLabel.ID().getText() : null, expr != null ? visitExpr(expr) : null, null);
  }
  
  @Override
  public ExprAST<Void> visitIndexOp(IndexOpContext ctx) {
    return visitExpr(ctx.expr());
  }
  
  @Override
  public ArrayLiteralExprAST<Void> visitArrayLiteral(ArrayLiteralContext ctx) {
    return new ArrayLiteralExprAST<>(ctx.expr().stream().map(this::visitExpr).toList(), null);
  }
  
  @Override
  public ExprAST<Void> visitLiteral(LiteralContext ctx) {
    if (ctx.string != null) return new StringLiteralExprAST<>(parseString(ctx.string.getText()), null);
    if (ctx.int_ != null) return new IntLiteralExprAST<>(parseInt(ctx.int_.getText()), null);
    if (ctx.bool != null) return new BoolLiteralExprAST<>(parseBool(ctx.bool.getText()), null);
    parseUnit(ctx.unit.getText());
    return new UnitLiteralExprAST<>(null);
  }
  
  private String parseString(String literal) {
    if (!literal.startsWith("\"") || !literal.endsWith("\"")) throw new IllegalStateException("invalid string literal");
    return literal.substring(1, literal.length() - 1);
  }
  
  private int parseInt(String literal) {
    return Integer.parseInt(literal);
  }
  
  private boolean parseBool(String literal) {
    return switch (literal) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalStateException("invalid bool literal");
    };
  }
  
  private void parseUnit(String literal) {
    if (!"unit".equals(literal)) throw new IllegalStateException("invalid unit literal");
  }
  
}
