package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.*;
import io.github.pint_lang.typechecker.conditions.*;
import io.github.pint_lang.typechecker.conditions.ErrorCondition;

public class ConditionMapVisitor implements ExprASTVisitor<Type, Condition> {

    private final String varName;
    
    private final ConditionBindings.Builder bindings = new ConditionBindings.Builder();
    
    // todo: this might be the wrong approach; I think these errors will appear if the condition of an if expression contains complex expressions, not if an actual type condition does; I'm leaving it as-is for now.
    private final ErrorLogger.Fixed<Condition> logger;
    
    public ConditionMapVisitor(String varName, ErrorLogger logger) {
        this.varName = varName;
        this.logger = logger.fix(new ErrorCondition());
    }
    
    public ConditionBindings takeBindings() {
        return bindings.finishAndReset();
    }
    
    @Override
    public Condition visitUnaryExpr(UnaryExprAST<Type> ast) {
        var operand = ast.operand().accept(this);
        return switch (ast.op()) {
            case PLUS -> UnaryCondition.plus(operand);
            case NEG -> UnaryCondition.neg(operand);
            case NOT -> UnaryCondition.not(operand);
            case ABS -> UnaryCondition.abs(operand);
        };
    }
    
    @Override
    public Condition visitBinaryExpr(BinaryExprAST<Type> ast) {
        var left = ast.left().accept(this);
        var right = ast.right().accept(this);
        return switch (ast.op()) {
            case ASSIGN, ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN -> logger.error("Assignment operators are not allowed in type conditions");
            case OR -> OrCondition.or(left, right);
            case AND -> AndCondition.and(left, right);
            case EQ -> CmpCondition.eq(left, right);
            case NEQ -> CmpCondition.neq(left, right);
            case LT -> CmpCondition.lt(left, right);
            case NLT -> CmpCondition.nlt(left, right);
            case LE -> CmpCondition.le(left, right);
            case NLE -> CmpCondition.nle(left, right);
            case GT -> CmpCondition.gt(left, right);
            case NGT -> CmpCondition.ngt(left, right);
            case GE -> CmpCondition.ge(left, right);
            case NGE -> CmpCondition.nge(left, right);
            case ADD -> BinaryCondition.add(left, right);
            case SUB -> BinaryCondition.sub(left, right);
            case MUL -> BinaryCondition.mul(left, right);
            case DIV -> BinaryCondition.div(left, right);
        };
    }
    
    @Override
    public Condition visitBlockExpr(BlockExprAST<Type> ast) {
        return logger.error("Block expressions are not supported in type conditions");
    }
    
    @Override
    public Condition visitVarExprAST(VarExprAST<Type> ast) {
        if (ast.name().equals(varName)) return bindings.getOrBindIt();
        else return bindings.getOrBindVar(ast.name());
    }
    
    @Override
    public Condition visitFuncCall(FuncCallExprAST<Type> ast) {
        return logger.error("Function calls are not supported in type conditions (yet)"); // todo
    }
    
    @Override
    public Condition visitIndexExpr(IndexExprAST<Type> ast) {
        return logger.error("Index expressions are not supported in type conditions (yet)");
    }
    
    @Override
    public Condition visitSliceExpr(SliceExprAST<Type> ast) {
        return logger.error("Slice expressions are not supported in type conditions (yet)");
    }
    
    @Override
    public Condition visitItExpr(ItExprAST<Type> ast) {
        return logger.error("ConditionMapVisitor::visitItExpr; I'm not sure whether this is a problem");
    }
    
    @Override
    public Condition visitIfExpr(IfExprAST<Type> ast) {
        return logger.error("if expressions are not supported in type conditions");
    }
    
    @Override
    public Condition visitLoopExpr(LoopExprAST<Type> ast) {
        return logger.error("loops are not supported in type conditions");
    }
    
    @Override
    public Condition visitWhileExpr(WhileExprAST<Type> ast) {
        return logger.error("while loops are not supported in type conditions");
    }
    
    @Override
    public Condition visitJumpExpr(JumpExprAST<Type> ast) {
        return logger.error("jump expressions are not supported in type conditions");
    }
    
    @Override
    public Condition visitArrayLiteralExpr(ArrayLiteralExprAST<Type> ast) {
        return ArrayCondition.array(ast.items().stream().map(item -> new ArrayCondition.Item(item.item().accept(this), item.spread())).toList());
    }
    
    @Override
    public Condition visitStringLiteralExpr(StringLiteralExprAST<Type> ast) {
        return ConstantCondition.string(ast.value());
    }
    
    @Override
    public Condition visitIntLiteralExpr(IntLiteralExprAST<Type> ast) {
        return ConstantCondition.integer(ast.value());
    }
    
    @Override
    public Condition visitBoolLiteralExpr(BoolLiteralExprAST<Type> ast) {
        return ConstantCondition.bool(ast.value());
    }
    
    @Override
    public Condition visitUnitLiteralExpr(UnitLiteralExprAST<Type> ast) {
        return ConstantCondition.unit();
    }

}
