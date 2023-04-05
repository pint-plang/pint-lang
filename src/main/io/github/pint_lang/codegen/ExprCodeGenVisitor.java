package io.github.pint_lang.codegen;

import io.github.pint_lang.ast.*;
import io.github.pint_lang.typechecker.Type;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.javacpp.Pointer.isNull;
import static org.bytedeco.llvm.global.LLVM.*;

public class ExprCodeGenVisitor implements ExprASTVisitor<Type, LLVMValueRef> {
  
  public final LLVMContextRef context;
  public final LLVMModuleRef module;
  public final LLVMBuilderRef builder;
  public final GlobalLoader.Context loaderContext = new GlobalLoader.Context();
  private final JumpScopeStack jumpStack = new JumpScopeStack();
  private final DefScopeStack defStack = new DefScopeStack();
  private final StatementVisitor statVisitor = this.new StatementVisitor();
  
  public ExprCodeGenVisitor(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder) {
    this.context = context;
    this.module = module;
    this.builder = builder;
  }
  
  @Deprecated
  private LLVMValueRef unit() {
    // TODO: bad types
    return LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0);
  }
  
  @Override
  public LLVMValueRef visitUnaryExpr(UnaryExprAST<Type> ast) {
    var operand = ast.operand().accept(this);
    return switch (ast.op()) {
      case PLUS -> operand;
      case NEG -> LLVMBuildNeg(builder, operand, "negtmp");
      case NOT -> LLVMBuildICmp(builder, LLVMIntEQ, operand, LLVMConstInt(LLVMTypeOf(operand), 0, 0), "nottmp");
      case ABS -> {
        // TODO bad types
        var startBlock = LLVMGetInsertBlock(builder);
        var negBlock = LLVMCreateBasicBlockInContext(context, "absNegBlock");
        var endBlock = LLVMCreateBasicBlockInContext(context, "endAbsBlock");
        var function = LLVMGetBasicBlockParent(startBlock);
        var cmp = LLVMBuildICmp(builder, LLVMIntSLT, operand, LLVMConstInt(LLVMInt32TypeInContext(context), 0, 1), "abscmptmp");
        LLVMBuildCondBr(builder, cmp, negBlock, endBlock);
        LLVMAppendExistingBasicBlock(function, negBlock);
        LLVMPositionBuilderAtEnd(builder, negBlock);
        var neg = LLVMBuildNeg(builder, operand, "absnegtmp");
        LLVMBuildBr(builder, endBlock);
        LLVMAppendExistingBasicBlock(function, endBlock);
        LLVMPositionBuilderAtEnd(builder, endBlock);
        var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "abstmp");
        LLVMAddIncoming(phi, operand, startBlock, 1);
        LLVMAddIncoming(phi, neg, negBlock, 1);
        yield phi;
      }
    };
  }
  
  @Override
  public LLVMValueRef visitBinaryExpr(BinaryExprAST<Type> ast) {
    var left = ast.op().isAssign() ? null : ast.left().accept(this);
    var right = ast.op().shortCircuits() ? null : ast.right().accept(this);
    return switch (ast.op()) {
      case MUL -> LLVMBuildMul(builder, left, right, "multmp");
      case DIV -> LLVMBuildSDiv(builder, left, right, "divtmp");
      case ADD -> LLVMBuildAdd(builder, left, right, "addtmp");
      case SUB -> LLVMBuildSub(builder, left, right, "subtmp");
      // TODO bad types
      case EQ -> LLVMBuildICmp(builder, LLVMIntEQ, left, right, "cmptmp");
      case NEQ -> LLVMBuildICmp(builder, LLVMIntNE, left, right, "cmptmp");
      case LT, NGE -> LLVMBuildICmp(builder, LLVMIntSLT, left, right, "cmptmp");
      case NLT, GE -> LLVMBuildICmp(builder, LLVMIntSGE, left, right, "cmptmp");
      case GT, NLE -> LLVMBuildICmp(builder, LLVMIntSGT, left, right, "cmptmp");
      case NGT, LE -> LLVMBuildICmp(builder, LLVMIntSLE, left, right, "cmptmp");
      
      case AND -> {
        var startBlock = LLVMGetInsertBlock(builder);
        var rightBlock = LLVMCreateBasicBlockInContext(context, "andRightBlock");
        var endBlock = LLVMCreateBasicBlockInContext(context, "endAndBlock");
        var function = LLVMGetBasicBlockParent(startBlock);
        LLVMBuildCondBr(builder, left, rightBlock, endBlock);
        LLVMAppendExistingBasicBlock(function, rightBlock);
        LLVMPositionBuilderAtEnd(builder, rightBlock);
        right = ast.right().accept(this);
        rightBlock = LLVMGetInsertBlock(builder);
        LLVMBuildBr(builder, endBlock);
        LLVMAppendExistingBasicBlock(function, endBlock);
        LLVMPositionBuilderAtEnd(builder, endBlock);
        var phi = LLVMBuildPhi(builder, LLVMInt1TypeInContext(context), "andtmp");
        LLVMAddIncoming(phi, LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0), startBlock, 1);
        LLVMAddIncoming(phi, right, rightBlock, 1);
        yield phi;
      }
      case OR -> {
        var startOrBlock = LLVMGetInsertBlock(builder);
        var rightOrBlock = LLVMCreateBasicBlockInContext(context, "rightOrBlock");
        var endOrBlock = LLVMCreateBasicBlockInContext(context, "endOrBlock");
        var function = LLVMGetBasicBlockParent(startOrBlock);
        LLVMBuildCondBr(builder, left, endOrBlock, rightOrBlock);
        LLVMAppendExistingBasicBlock(function, rightOrBlock);
        LLVMPositionBuilderAtEnd(builder, rightOrBlock);
        right = ast.right().accept(this);
        rightOrBlock = LLVMGetInsertBlock(builder);
        LLVMBuildBr(builder, endOrBlock);
        LLVMAppendExistingBasicBlock(function, endOrBlock);
        LLVMPositionBuilderAtEnd(builder, endOrBlock);
        var phi = LLVMBuildPhi(builder, LLVMInt1TypeInContext(context), "ortmp");
        LLVMAddIncoming(phi, LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0), startOrBlock, 1);
        LLVMAddIncoming(phi, right, rightOrBlock, 1);
        yield phi;
      }
      case ASSIGN, ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN -> {
        if (!(ast.left() instanceof VarExprAST<Type> varExpr)) throw new BadExpressionException("Only variables can be assigned to");
        LLVMValueRef ptr;
        LLVMTypeRef type;
        var local = defStack.getVar(varExpr.name());
        if (local != null) {
          ptr = local.ptr();
          type = local.type();
        } else if (!isNull(loaderContext.getParam(varExpr.name()))) {
          throw new BadExpressionException("Can't assign to an argument (for now, anyway)");
        } else {
          var global = LLVMGetNamedGlobal(module, varExpr.name());
          if (isNull(global)) throw new BadExpressionException("Referenced undefined variable '" + varExpr.name() + "'");
          if (loaderContext.isVariableUninitialized(varExpr.name())) throw new BadExpressionException("Cannot assign to global '" + varExpr.name() + "' before it has been initialized");
          ptr = global;
          type = LLVMGlobalGetValueType(global);
        }
        var value = ast.right().accept(this);
        value = switch (ast.op()) {
          case ASSIGN -> value;
          case ADD_ASSIGN -> {
            var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
            yield LLVMBuildAdd(builder, load, value, "addtmp");
          }
          case SUB_ASSIGN -> {
            var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
            yield LLVMBuildSub(builder, load, value, "subtmp");
          }
          case MUL_ASSIGN -> {
            var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
            yield LLVMBuildMul(builder, load, value, "multmp");
          }
          case DIV_ASSIGN -> {
            var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
            yield LLVMBuildSDiv(builder, load, value, "divtmp");
          }
          default -> throw new IllegalStateException("Invalid assignment operator");
        };
        LLVMBuildStore(builder, value, ptr);
        yield unit();
      }
    };
  }
  
  @Override
  public LLVMValueRef visitVarExpr(VarExprAST<Type> ast) {
    var local = defStack.getVar(ast.name());
    if (local != null) return LLVMBuildLoad2(builder, local.type(), local.ptr(), "loadtmp");
    var param = loaderContext.getParam(ast.name());
    if (!isNull(param)) return param;
    var global = LLVMGetNamedGlobal(module, ast.name());
    if (isNull(global)) throw new BadExpressionException("Referenced undefined variable '" + ast.name() + "'");
    if (loaderContext.isVariableUninitialized(ast.name())) throw new BadExpressionException("Referenced global variable '" + ast.name() + "' before it was initialized");
    return LLVMBuildLoad2(builder, LLVMGlobalGetValueType(global), global, "loadtmp");
  }
  
  @Override
  public LLVMValueRef visitIndexExpr(IndexExprAST<Type> ast) {
    throw new UnsupportedOperationException("Yeah, I'm not messing with this until types are more solid"); // todo
  }
  
  @Override
  public LLVMValueRef visitItExpr(ItExprAST<Type> ast) {
    throw new BadExpressionException("it does not have a value");
  }
  
  @Override
  public LLVMValueRef visitBlockExpr(BlockExprAST<Type> ast) {
    if (ast.label() != null) {
      var startBlock = LLVMGetInsertBlock(builder);
      var endBlock = LLVMCreateBasicBlockInContext(context, "endLabeledBlock");
      var function = LLVMGetBasicBlockParent(startBlock);
      
      LLVMAppendExistingBasicBlock(function, endBlock);
      LLVMPositionBuilderAtEnd(builder, endBlock);
      // TODO bad types
      var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "labeledtmp");
  
      LLVMPositionBuilderAtEnd(builder, startBlock);
      var lastValue = unit();
      try (var ignored = jumpStack.scopeLabeled(ast.label(), false, null, endBlock, phi, "a labeled block")) {
        try (var ignored2 = defStack.scope()) {
          for (var stat : ast.stats()) lastValue = stat.accept(statVisitor);
        }
      }
      startBlock = LLVMGetInsertBlock(builder);
      LLVMBuildBr(builder, endBlock);
      
      LLVMPositionBuilderAtEnd(builder, endBlock);
      LLVMAddIncoming(phi, lastValue, startBlock, 1);
      
      return phi;
    } else {
      var lastValue = unit();
      try (var ignored = defStack.scope()) {
        for (var stat : ast.stats()) lastValue = stat.accept(statVisitor);
      }
      return lastValue;
    }
  }
  
  @Override
  public LLVMValueRef visitFuncCallExpr(FuncCallExprAST<Type> ast) {
    var callee = LLVMGetNamedFunction(module, ast.funcName());
    if (isNull(callee)) throw new BadExpressionException("Called undefined function: '" + ast.funcName() + "'");
    
    var argc = LLVMCountParams(callee);
    if (argc != ast.args().size() && LLVMIsFunctionVarArg(LLVMGlobalGetValueType(callee)) == 0) throw new BadExpressionException("Called function '" + ast.funcName() + "' with the wrong number of arguments (expected " + argc + ", got " + ast.args().size() + ")");
    
    var argv = new PointerPointer<>(ast.args().stream().map(this::visitExpr).toArray(LLVMValueRef[]::new));
    return LLVMBuildCall2(builder, LLVMGlobalGetValueType(callee), callee, argv, argc, "calltmp");
  }
  
  @Override
  public LLVMValueRef visitIfExpr(IfExprAST<Type> ast) {
    var function = LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder));
    var hasElse = ast.elseBody() != null;
    var condition = ast.condition().accept(this);
    var endIfBlock = LLVMCreateBasicBlockInContext(context, "endIfBlock");
    var thenBlock = LLVMCreateBasicBlockInContext(context, "thenBlock");
    var elseBlock = hasElse ? LLVMCreateBasicBlockInContext(context, "elseBlock") : endIfBlock;
    LLVMBuildCondBr(builder, condition, thenBlock, elseBlock);
    LLVMAppendExistingBasicBlock(function, thenBlock);
    LLVMPositionBuilderAtEnd(builder, thenBlock);
    var thenValue = ast.thenBody().accept(this);
    LLVMBuildBr(builder, endIfBlock);
    thenBlock = LLVMGetInsertBlock(builder);
    LLVMValueRef elseValue = null;
    if (hasElse) {
      LLVMAppendExistingBasicBlock(function, elseBlock);
      LLVMPositionBuilderAtEnd(builder, elseBlock);
      elseValue = ast.elseBody().accept(this);
      LLVMBuildBr(builder, endIfBlock);
      elseBlock = LLVMGetInsertBlock(builder);
    }
    LLVMAppendExistingBasicBlock(function, endIfBlock);
    LLVMPositionBuilderAtEnd(builder, endIfBlock);
    if (hasElse) {
      // TODO bad types
      var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "iftmp");
      LLVMAddIncoming(phi, thenValue, thenBlock, 1);
      LLVMAddIncoming(phi, elseValue, elseBlock, 1);
      return phi;
    } else {
      return unit();
    }
  }
  
  @Override
  public LLVMValueRef visitLoopExpr(LoopExprAST<Type> ast) {
    var label = ast.label();
    var function = LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder));
    var startBlock = LLVMCreateBasicBlockInContext(context, "startLoopBlock");
    var endBlock = LLVMCreateBasicBlockInContext(context, "endLoopBlock");
    
    LLVMBuildBr(builder, startBlock);
    
    LLVMAppendExistingBasicBlock(function, startBlock);
    LLVMAppendExistingBasicBlock(function, endBlock);
    LLVMPositionBuilderAtEnd(builder, endBlock);
    // TODO bad types
    var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "looptmp");
    
    LLVMPositionBuilderAtEnd(builder, startBlock);
    JumpScopeStack.Scope scope;
    if (label != null) scope = jumpStack.scopeLabeled(label, true, startBlock, endBlock, phi, "a labeled loop");
    else scope = jumpStack.scopeAnon(startBlock, endBlock, phi, "an unlabeled loop");
    try (scope) {
      ast.body().accept(this); // ignore result, unconditionally
    }
    LLVMBuildBr(builder, startBlock);
    
    LLVMPositionBuilderAtEnd(builder, endBlock);
    if (LLVMCountIncoming(phi) != 0) { // phi node has an incoming node, there is at least one break
      return phi;
    } else {
      LLVMDeleteInstruction(phi);
      return unit();
    }
  }
  
  @Override
  public LLVMValueRef visitWhileExpr(WhileExprAST<Type> ast) {
    var label = ast.label();
    var function = LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder));
    var startBlock = LLVMCreateBasicBlockInContext(context, "startWhileBlock");
    var bodyBlock = LLVMCreateBasicBlockInContext(context, "whileBodyBlock");
    var endBlock = LLVMCreateBasicBlockInContext(context, "endWhileBlock");
    
    LLVMBuildBr(builder, startBlock);
    
    LLVMAppendExistingBasicBlock(function, startBlock);
    LLVMPositionBuilderAtEnd(builder, startBlock);
    var condition = ast.condition().accept(this);
    LLVMBuildCondBr(builder, condition, bodyBlock, endBlock);
    
    LLVMAppendExistingBasicBlock(function, bodyBlock);
    LLVMPositionBuilderAtEnd(builder, bodyBlock);
    JumpScopeStack.Scope scope;
    if (label != null) scope = jumpStack.scopeLabeled(label, true, startBlock, endBlock, null, "a labeled while loop");
    else scope = jumpStack.scopeAnon(startBlock, endBlock, null, "an unlabeled while loop");
    try (scope) {
      ast.body().accept(this); // ignore this unconditionally
    }
    LLVMBuildBr(builder, startBlock);
    
    LLVMAppendExistingBasicBlock(function, endBlock);
    LLVMPositionBuilderAtEnd(builder, endBlock);
    
    return unit();
  }
  
  @Override
  public LLVMValueRef visitJumpExpr(JumpExprAST<Type> ast) {
    var targetLabel = ast.targetLabel();
    var valueAST = ast.value();
    switch (ast.kind()) {
      case RETURN -> {
        if (!loaderContext.isInFunction()) throw new BadExpressionException("cannot return from a global initializer (consider using a labeled block/break)");
        if (targetLabel != null) throw new BadExpressionException("return cannot target a label");
        if (valueAST != null) {
          var value = valueAST.accept(this);
          // TODO: bad if value is unit
          LLVMBuildRet(builder, value);
        } else {
          LLVMBuildRetVoid(builder);
        }
        
        return unit();
      }
      case BREAK -> {
        JumpScope scope;
        if (targetLabel != null) {
          scope = jumpStack.findLabeled(targetLabel);
          if (scope == null) throw new BadExpressionException("break to undefined label: '" + targetLabel + "'");
        } else {
          scope = jumpStack.peekAnon();
          if (scope == null) {
            scope = jumpStack.peek();
            if (scope == null) throw new BadExpressionException("break outside of any jump targets");
            throw new BadExpressionException("cannot anonymously break to " + scope.name());
          }
        }
        if (valueAST != null && scope.phi() == null) throw new BadExpressionException("cannot break with value to " + scope.name());
        if (scope.phi() != null) LLVMAddIncoming(scope.phi(), valueAST != null ? valueAST.accept(this) : unit(), LLVMGetInsertBlock(builder), 1);
        LLVMBuildBr(builder, scope.breakBlock());
        return unit();
      }
      case CONTINUE -> {
        if (valueAST != null) throw new BadExpressionException("continue cannot accept a value");
        JumpScope scope;
        if (targetLabel != null) {
          scope = jumpStack.findLabeled(targetLabel);
          if (scope == null) throw new BadExpressionException("continue to undefined label: '" + targetLabel + "'");
        } else {
          scope = jumpStack.peekAnon();
          if (scope == null) {
            scope = jumpStack.peek();
            if (scope == null) throw new BadExpressionException("continue outside of any jump targets");
            throw new BadExpressionException("cannot anonymously continue to " + scope.name());
          }
        }
        if (scope.continueBlock() == null) throw new BadExpressionException("cannot continue to " + scope.name());
        LLVMBuildBr(builder, scope.continueBlock());
        return unit();
      }
      default -> throw new IllegalStateException("Invalid jump expression");
    }
  }
  
  @Override
  public LLVMValueRef visitArrayLiteralExpr(ArrayLiteralExprAST<Type> ast) {
    // TODO bad types; may need to be non-const
    var values = ast.elements().stream().map(this::visitExpr).toArray(LLVMValueRef[]::new);
    return LLVMConstArray(LLVMInt32TypeInContext(context), new PointerPointer<>(values), values.length);
  }
  
  @Override
  public LLVMValueRef visitStringLiteralExpr(StringLiteralExprAST<Type> ast) {
    return LLVMConstStringInContext(context, ast.value(), ast.value().length(), 0);
  }
  
  @Override
  public LLVMValueRef visitIntLiteralExpr(IntLiteralExprAST<Type> ast) {
    return LLVMConstInt(LLVMInt32TypeInContext(context), ast.value(), 1);
  }
  
  @Override
  public LLVMValueRef visitBoolLiteralExpr(BoolLiteralExprAST<Type> ast) {
    return ast.value() ? LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0) : LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0);
  }
  
  @Override
  public LLVMValueRef visitUnitLiteralExpr(UnitLiteralExprAST<Type> ast) {
    return unit();
  }
  
  private class StatementVisitor extends ExprDelegateStatASTVisitor<Type, LLVMValueRef> {
    
    public StatementVisitor() {
      super(ExprCodeGenVisitor.this);
    }
    
    @Override
    public LLVMValueRef visitVarDef(VarDefAST<Type> ast) {
      // TODO: bad types
      var type = LLVMInt32TypeInContext(context);
      var ptr = LLVMBuildAlloca(builder, type, "alloca." + ast.name());
      defStack.putVar(ast.name(), ptr, type);
      var value = ast.value().accept(this);
      LLVMBuildStore(builder, value, ptr);
      return unit();
    }
    
    @Override
    public LLVMValueRef acceptNopStat(NopStatAST<Type> ast) {
      return unit();
    }
    
  }
  
}
