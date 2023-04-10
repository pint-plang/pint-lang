package io.github.pint_lang.ast;

public interface ExprASTVisitor<T, R> {
  
  default R visitExpr(ExprAST<T> ast) {
    return ast.accept(this);
  }
  
  R visitUnaryExpr(UnaryExprAST<T> ast);
  
  R visitBinaryExpr(BinaryExprAST<T> ast);
  
  R visitBlockExpr(BlockExprAST<T> ast);
  
  R visitVarExprAST(VarExprAST<T> ast);
  
  R visitFuncCall(FuncCallExprAST<T> ast);
  
  R visitIndexExpr(IndexExprAST<T> ast);
  
  R visitSliceExpr(SliceExprAST<T> ast);
  
  R visitItExpr(ItExprAST<T> ast);
  
  R visitIfExpr(IfExprAST<T> ast);
  
  R visitLoopExpr(LoopExprAST<T> ast);
  
  R visitWhileExpr(WhileExprAST<T> ast);
  
  R visitJumpExpr(JumpExprAST<T> ast);
  
  R visitArrayLiteralExpr(ArrayLiteralExprAST<T> ast);
  
  R visitStringLiteralExpr(StringLiteralExprAST<T> ast);
  
  R visitIntLiteralExpr(IntLiteralExprAST<T> ast);
  
  R visitBoolLiteralExpr(BoolLiteralExprAST<T> ast);
  
  R visitUnitLiteralExpr(UnitLiteralExprAST<T> ast);
  
}
