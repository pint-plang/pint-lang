package io.github.pint_lang.ast;

public abstract class ExprDelegateStatASTVisitor<T, R> implements StatASTVisitor<T, R> {
  
  protected final ExprASTVisitor<T, ? extends R> expr;
  
  public ExprDelegateStatASTVisitor(ExprASTVisitor<T, ? extends R> expr) {
    this.expr = expr;
  }
  
  @Override
  public R visitExpr(ExprAST<T> ast) {
    return expr.visitExpr(ast);
  }
  
  @Override
  public R visitUnaryExpr(UnaryExprAST<T> ast) {
    return expr.visitUnaryExpr(ast);
  }
  
  @Override
  public R visitBinaryExpr(BinaryExprAST<T> ast) {
    return expr.visitBinaryExpr(ast);
  }
  
  @Override
  public R visitBlockExpr(BlockExprAST<T> ast) {
    return expr.visitBlockExpr(ast);
  }
  
  @Override
  public R visitVarExprAST(VarExprAST<T> ast) {
    return expr.visitVarExprAST(ast);
  }
  
  @Override
  public R visitFuncCall(FuncCallExprAST<T> ast) {
    return expr.visitFuncCall(ast);
  }
  
  @Override
  public R visitIndexExpr(IndexExprAST<T> ast) {
    return expr.visitIndexExpr(ast);
  }
  
  @Override
  public R visitItExpr(ItExprAST<T> ast) {
    return expr.visitItExpr(ast);
  }
  
  @Override
  public R visitIfExpr(IfExprAST<T> ast) {
    return expr.visitIfExpr(ast);
  }
  
  @Override
  public R visitLoopExpr(LoopExprAST<T> ast) {
    return expr.visitLoopExpr(ast);
  }
  
  @Override
  public R visitWhileExpr(WhileExprAST<T> ast) {
    return expr.visitWhileExpr(ast);
  }
  
  @Override
  public R visitJumpExpr(JumpExprAST<T> ast) {
    return expr.visitJumpExpr(ast);
  }
  
  @Override
  public R visitArrayLiteralExpr(ArrayLiteralExprAST<T> ast) {
    return expr.visitArrayLiteralExpr(ast);
  }
  
  @Override
  public R visitStringLiteralExpr(StringLiteralExprAST<T> ast) {
    return expr.visitStringLiteralExpr(ast);
  }
  
  @Override
  public R visitIntLiteralExpr(IntLiteralExprAST<T> ast) {
    return expr.visitIntLiteralExpr(ast);
  }
  
  @Override
  public R visitBoolLiteralExpr(BoolLiteralExprAST<T> ast) {
    return expr.visitBoolLiteralExpr(ast);
  }
  
  @Override
  public R visitUnitLiteralExpr(UnitLiteralExprAST<T> ast) {
    return expr.visitUnitLiteralExpr(ast);
  }
  
}
