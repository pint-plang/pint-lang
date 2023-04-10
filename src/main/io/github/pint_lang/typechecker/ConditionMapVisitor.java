package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.*;

public class ConditionMapVisitor implements ExprASTVisitor<Type, ExprAST<Type>> {

    private final String varName;
    private boolean isShadowed = false;
    private final StatVisitor statVisitor;

    public ConditionMapVisitor(String varName) {
        this.varName = varName;
        statVisitor = this.new StatVisitor();
    }

    @Override
    public ExprAST<Type> visitUnaryExpr(UnaryExprAST<Type> ast) {
        return new UnaryExprAST<>(ast.op(), ast.operand().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitBinaryExpr(BinaryExprAST<Type> ast) {
        return new BinaryExprAST<>(ast.op(), ast.left().accept(this), ast.right().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitBlockExpr(BlockExprAST<Type> ast) {
        if (isShadowed) return ast;
        var stats = ast.stats().stream().map(statVisitor::visitStat).toList();
        isShadowed = false;
        return new BlockExprAST<>(ast.label(), stats, ast.data());
    }

    @Override
    public ExprAST<Type> visitVarExprAST(VarExprAST<Type> ast) {
        return varName.equals(ast.name()) ? new ItExprAST<>(ast.data()) : ast;
    }

    @Override
    public ExprAST<Type> visitFuncCall(FuncCallExprAST<Type> ast) {
        return new FuncCallExprAST<>(ast.funcName(), ast.args().stream().map(this::visitExpr).toList(), ast.data());
    }

    @Override
    public ExprAST<Type> visitIndexExpr(IndexExprAST<Type> ast) {
        return new IndexExprAST<>(ast.indexee().accept(this), ast.index().accept(this), ast.data());
    }
    
    @Override
    public ExprAST<Type> visitSliceExpr(SliceExprAST<Type> ast) {
        return new SliceExprAST<>(ast.slicee().accept(this), ast.from().accept(this), ast.to().accept(this), ast.data());
    }
    
    @Override
    public ExprAST<Type> visitItExpr(ItExprAST<Type> ast) {
        System.err.println("warning: ConditionMapVisitor.visitItExpr was called; please investigate");
        return ast;
    }

    @Override
    public ExprAST<Type> visitIfExpr(IfExprAST<Type> ast) {
        return new IfExprAST<>(ast.condition().accept(this), ast.thenBody().accept(this), ast.elseBody().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitLoopExpr(LoopExprAST<Type> ast) {
        return new LoopExprAST<>(ast.label(), ast.body().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitWhileExpr(WhileExprAST<Type> ast) {
        return new WhileExprAST<>(ast.label(), ast.condition().accept(this), ast.body().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitJumpExpr(JumpExprAST<Type> ast) {
        return new JumpExprAST<>(ast.kind(), ast.targetLabel(), ast.value().accept(this), ast.data());
    }

    @Override
    public ExprAST<Type> visitArrayLiteralExpr(ArrayLiteralExprAST<Type> ast) {
        return new ArrayLiteralExprAST<>(ast.items().stream().map(item -> new ArrayLiteralExprAST.Item<>(item.item().accept(this), item.spread())).toList(), ast.data());
    }

    @Override
    public StringLiteralExprAST<Type> visitStringLiteralExpr(StringLiteralExprAST<Type> ast) {
        return ast;
    }

    @Override
    public IntLiteralExprAST<Type> visitIntLiteralExpr(IntLiteralExprAST<Type> ast) {
        return ast;
    }

    @Override
    public BoolLiteralExprAST<Type> visitBoolLiteralExpr(BoolLiteralExprAST<Type> ast) {
        return ast;
    }

    @Override
    public UnitLiteralExprAST<Type> visitUnitLiteralExpr(UnitLiteralExprAST<Type> ast) {
        return ast;
    }

    private class StatVisitor extends ExprDelegateStatASTVisitor<Type, StatAST<Type>> {

        public StatVisitor() {
            super(ConditionMapVisitor.this);
        }

        @Override
        public StatAST<Type> visitVarDef(VarDefAST<Type> ast) {
            var value = ast.value().accept(ConditionMapVisitor.this);
            if (ast.name().equals(varName)) isShadowed = true;
            return new VarDefAST<>(ast.name(), ast.type(), value, ast.data());
        }

        @Override
        public StatAST<Type> visitNopStat(NopStatAST<Type> ast) {
            return ast;
        }

    }

}
