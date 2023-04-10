package io.github.pint_lang.ast;

public sealed interface ExprAST<T> extends StatAST<T> permits ArrayLiteralExprAST, BinaryExprAST, BlockExprAST, BoolLiteralExprAST, FuncCallExprAST, IfExprAST, IndexExprAST, SliceExprAST, IntLiteralExprAST, ItExprAST, JumpExprAST, LoopExprAST, StringLiteralExprAST, UnaryExprAST, UnitLiteralExprAST, VarExprAST, WhileExprAST {
  
  @Override
  default <R> R accept(ASTVisitor<T, R> visitor) {
    return accept((ExprASTVisitor<T, R>) visitor);
  }
  
  @Override
  default <R> R accept(StatASTVisitor<T, R> visitor) {
    return accept((ExprASTVisitor<T, R>) visitor);
  }
  
  <R> R accept(ExprASTVisitor<T, R> visitor);
  
}
