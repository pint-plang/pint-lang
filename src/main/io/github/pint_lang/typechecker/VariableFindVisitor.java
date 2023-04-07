package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.*;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class VariableFindVisitor implements StatASTVisitor<Type, Set<VarExprAST<Type>>> {

    private final HashSet<VarExprAST<Type>> vars = new HashSet<>();
    private final Stack<HashSet<String>> excludeStack = new Stack<>();

    @Override
    public Set<VarExprAST<Type>> visitVarDef(VarDefAST<Type> ast) {
        ast.value().accept(this);
        if (excludeStack.isEmpty()) excludeStack.peek().add(ast.name());
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitNopStat(NopStatAST<Type> ast) {
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitUnaryExpr(UnaryExprAST<Type> ast) {
        ast.operand().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitBinaryExpr(BinaryExprAST<Type> ast) {
        ast.left().accept(this);
        ast.right().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitBlockExpr(BlockExprAST<Type> ast) {
        excludeStack.push(new HashSet<>());
        for (var stat : ast.stats()) stat.accept(this);
        excludeStack.pop();
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitVarExprAST(VarExprAST<Type> ast) {
        if (excludeStack.isEmpty() || excludeStack.stream().noneMatch(exclude -> exclude.contains(ast.name()))) vars.add(ast);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitFuncCall(FuncCallExprAST<Type> ast) {
        for (var arg : ast.args()) arg.accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitIndexExpr(IndexExprAST<Type> ast) {
        ast.indexee().accept(this);
        ast.index().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitItExpr(ItExprAST<Type> ast) {
        System.err.println("warning: VariableFindVisitor.visitItExpr was called; please investigate");
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitIfExpr(IfExprAST<Type> ast) {
        ast.condition().accept(this);
        ast.thenBody().accept(this);
        ast.elseBody().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitLoopExpr(LoopExprAST<Type> ast) {
        ast.body().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitWhileExpr(WhileExprAST<Type> ast) {
        ast.condition().accept(this);
        ast.body().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitJumpExpr(JumpExprAST<Type> ast) {
        ast.value().accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitArrayLiteralExpr(ArrayLiteralExprAST<Type> ast) {
        for (var element : ast.elements()) element.accept(this);
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitStringLiteralExpr(StringLiteralExprAST<Type> ast) {
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitIntLiteralExpr(IntLiteralExprAST<Type> ast) {
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitBoolLiteralExpr(BoolLiteralExprAST<Type> ast) {
        return vars;
    }

    @Override
    public Set<VarExprAST<Type>> visitUnitLiteralExpr(UnitLiteralExprAST<Type> ast) {
        return vars;
    }

}
