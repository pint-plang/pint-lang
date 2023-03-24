package io.github.pint_lang.codegen;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import static java.lang.Integer.parseInt;
import static org.bytedeco.javacpp.Pointer.isNull;
import static org.bytedeco.llvm.global.LLVM.*;

public class ExprCodeGenVisitor extends PintBaseVisitor<LLVMValueRef> {
  
  public final LLVMContextRef context;
  public final LLVMModuleRef module;
  public final LLVMBuilderRef builder;
  public final GlobalLoader.Context loaderContext = new GlobalLoader.Context();
  private final JumpScopeStack jumpStack = new JumpScopeStack();
  private final DefScopeStack defStack = new DefScopeStack();
  
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
  public LLVMValueRef visitVarDef(VarDefContext ctx) {
    var name = ctx.ID().getText();
    // TODO: bad types
    var type = LLVMInt32TypeInContext(context);
    var ptr = LLVMBuildAlloca(builder, type, "alloca." + name);
    defStack.putVar(name, ptr, type);
    var value = ctx.expr().accept(this);
    LLVMBuildStore(builder, value, ptr);
    return unit();
  }
  
  @Override
  public LLVMValueRef visitFactorExpr(FactorExprContext ctx) {
    return ctx.factor().accept(this);
  }
  
  @Override
  public LLVMValueRef visitUnaryExpr(UnaryExprContext ctx) {
    var operand = ctx.expr().accept(this);
    return switch (ctx.op.getText()) {
      case "+" -> operand;
      case "-" -> LLVMBuildNeg(builder, operand, "negtmp");
      case "not" -> LLVMBuildICmp(builder, LLVMIntEQ, operand, LLVMConstInt(LLVMTypeOf(operand), 0, 0), "nottmp");
      default -> throw new IllegalStateException("Invalid unary operator");
    };
  }
  
  @Override
  public LLVMValueRef visitMulExpr(MulExprContext ctx) {
    var left = ctx.left.accept(this);
    var right = ctx.right.accept(this);
    return switch (ctx.op.getText()) {
      case "*" -> LLVMBuildMul(builder, left, right, "multmp");
      case "/" -> LLVMBuildSDiv(builder, left, right, "divtmp");
      default -> throw new IllegalStateException("Invalid addition operator");
    };
  }
  
  @Override
  public LLVMValueRef visitAddExpr(AddExprContext ctx) {
    var left = ctx.left.accept(this);
    var right = ctx.right.accept(this);
    return switch (ctx.op.getText()) {
      case "+" -> LLVMBuildAdd(builder, left, right, "addtmp");
      case "-" -> LLVMBuildSub(builder, left, right, "subtmp");
      default -> throw new IllegalStateException("Invalid addition operator");
    };
  }
  
  @Override
  public LLVMValueRef visitCmpExpr(CmpExprContext ctx) {
    var left = ctx.left.accept(this);
    var right = ctx.right.accept(this);
    // TODO! bad types
    var not = ctx.not != null;
    var op = switch (ctx.op.getText()) {
      case "=" -> not ? LLVMIntNE : LLVMIntEQ;
      case "<" -> not ? LLVMIntSGE : LLVMIntSLT;
      case ">" -> not ? LLVMIntSLE : LLVMIntSGT;
      case "<=" -> not ? LLVMIntSGT : LLVMIntSLE;
      case ">=" -> not ? LLVMIntSLT : LLVMIntSGE;
      default -> throw new IllegalStateException("Illegal comparison operator");
    };
    return LLVMBuildICmp(builder, op, left, right, "cmptmp");
  }
  
  @Override
  public LLVMValueRef visitAndExpr(AndExprContext ctx) {
    var left = ctx.left.accept(this);
    var startBlock = LLVMGetInsertBlock(builder);
    var rightBlock = LLVMCreateBasicBlockInContext(context, "andRightBlock");
    var endBlock = LLVMCreateBasicBlockInContext(context, "endAndBlock");
    var function = LLVMGetBasicBlockParent(startBlock);
    LLVMBuildCondBr(builder, left, rightBlock, endBlock);
    LLVMAppendExistingBasicBlock(function, rightBlock);
    LLVMPositionBuilderAtEnd(builder, rightBlock);
    var right = ctx.right.accept(this);
    rightBlock = LLVMGetInsertBlock(builder);
    LLVMBuildBr(builder, endBlock);
    LLVMAppendExistingBasicBlock(function, endBlock);
    LLVMPositionBuilderAtEnd(builder, endBlock);
    var phi = LLVMBuildPhi(builder, LLVMInt1TypeInContext(context), "andtmp");
    LLVMAddIncoming(phi, LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0), startBlock, 1);
    LLVMAddIncoming(phi, right, rightBlock, 1);
    return phi;
  }
  
  @Override
  public LLVMValueRef visitOrExpr(OrExprContext ctx) {
    var left = ctx.left.accept(this);
    var startOrBlock = LLVMGetInsertBlock(builder);
    var rightOrBlock = LLVMCreateBasicBlockInContext(context, "rightOrBlock");
    var endOrBlock = LLVMCreateBasicBlockInContext(context, "endOrBlock");
    var function = LLVMGetBasicBlockParent(startOrBlock);
    LLVMBuildCondBr(builder, left, endOrBlock, rightOrBlock);
    LLVMAppendExistingBasicBlock(function, rightOrBlock);
    LLVMPositionBuilderAtEnd(builder, rightOrBlock);
    var right = ctx.right.accept(this);
    rightOrBlock = LLVMGetInsertBlock(builder);
    LLVMBuildBr(builder, endOrBlock);
    LLVMAppendExistingBasicBlock(function, endOrBlock);
    LLVMPositionBuilderAtEnd(builder, endOrBlock);
    var phi = LLVMBuildPhi(builder, LLVMInt1TypeInContext(context), "ortmp");
    LLVMAddIncoming(phi, LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0), startOrBlock, 1);
    LLVMAddIncoming(phi, right, rightOrBlock, 1);
    return phi;
  }
  
  @Override
  public LLVMValueRef visitAssignExpr(AssignExprContext ctx) {
    if (ctx.left instanceof FactorExprContext factorExpr) {
      var factor = factorExpr.factor();
      if (factor instanceof VarFactorContext varFactor) {
        var name = varFactor.ID().getText();
        LLVMValueRef ptr;
        LLVMTypeRef type;
        var isGlobal = false;
        var local = defStack.getVar(name);
        if (local != null) {
          ptr = local.ptr();
          type = local.type();
        } else if (!isNull(loaderContext.getParam(name))) {
          throw new BadExpressionException("Can't assign to an argument (for now, anyway)");
        } else {
          isGlobal = true;
          var global = LLVMGetNamedGlobal(module, name);
          if (isNull(global)) throw new BadExpressionException("Referenced undefined variable '" + name + "'");
          ptr = global;
          type = LLVMGlobalGetValueType(global);
        }
        var value = ctx.right.accept(this);
        if (!isGlobal || loaderContext.isVariableInitialized(name)) {
          value = switch (ctx.op.getText()) {
            case ":=" -> value;
            case ":+=" -> {
              var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
              yield LLVMBuildAdd(builder, load, value, "addtmp");
            }
            case ":-=" -> {
              var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
              yield LLVMBuildSub(builder, load, value, "subtmp");
            }
            case ":*=" -> {
              var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
              yield LLVMBuildMul(builder, load, value, "multmp");
            }
            case ":/=" -> {
              var load = LLVMBuildLoad2(builder, type, ptr, "loadtmp");
              yield LLVMBuildSDiv(builder, load, value, "divtmp");
            }
            default -> throw new IllegalStateException("Invalid assignment operator");
          };
        } else { // implies isGlobal && !isVariableInitialized(name)
          if (":=".equals(ctx.op.getText())) loaderContext.initializeVariable(name);
          else throw new BadExpressionException("Can't use a compound assignment operator on a global that has not yet been initialized");
        }
        LLVMBuildStore(builder, value, ptr);
      } else if (factor instanceof IndexFactorContext indexFactor) {
        throw new UnsupportedOperationException("Yeah, I'm not messing with this until types are more solid");
      }
    }
    throw new BadExpressionException("Only variables and index expressions can be assigned to");
  }
  
  @Override
  public LLVMValueRef visitLabeledBlockFactor(LabeledBlockFactorContext ctx) {
    return ctx.labeledBlockExpr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitParensFactor(ParensFactorContext ctx) {
    return ctx.expr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitAbsFactor(AbsFactorContext ctx) {
    var expr = ctx.expr().accept(this);
    // TODO bad types
    var startBlock = LLVMGetInsertBlock(builder);
    var negBlock = LLVMCreateBasicBlockInContext(context, "absNegBlock");
    var endBlock = LLVMCreateBasicBlockInContext(context, "endAbsBlock");
    var function = LLVMGetBasicBlockParent(startBlock);
    var cmp = LLVMBuildICmp(builder, LLVMIntSLT, expr, LLVMConstInt(LLVMInt32TypeInContext(context), 0, 1), "abscmptmp");
    LLVMBuildCondBr(builder, cmp, negBlock, endBlock);
    LLVMAppendExistingBasicBlock(function, negBlock);
    LLVMPositionBuilderAtEnd(builder, negBlock);
    var neg = LLVMBuildNeg(builder, expr, "absnegtmp");
    LLVMBuildBr(builder, endBlock);
    LLVMAppendExistingBasicBlock(function, endBlock);
    LLVMPositionBuilderAtEnd(builder, endBlock);
    var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "abstmp");
    LLVMAddIncoming(phi, expr, startBlock, 1);
    LLVMAddIncoming(phi, neg, negBlock, 1);
    return phi;
  }
  
  @Override
  public LLVMValueRef visitVarFactor(VarFactorContext ctx) {
    var name = ctx.ID().getText();
    var local = defStack.getVar(name);
    if (local != null) return LLVMBuildLoad2(builder, local.type(), local.ptr(), "loadtmp");
    var param = loaderContext.getParam(name);
    if (!isNull(param)) return param;
    var global = LLVMGetNamedGlobal(module, name);
    if (isNull(global)) throw new BadExpressionException("Referenced undefined variable '" + name + "'");
    if (!loaderContext.isVariableInitialized(name)) throw new BadExpressionException("Referenced global variable '" + name + "' before it was initialized");
    return LLVMBuildLoad2(builder, LLVMGlobalGetValueType(global), global, "loadtmp");
  }
  
  @Override
  public LLVMValueRef visitFuncCallFactor(FuncCallFactorContext ctx) {
    return ctx.funcCallExpr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitControlFlowFactor(ControlFlowFactorContext ctx) {
    return ctx.controlFlowExpr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitIndexFactor(IndexFactorContext ctx) {
    throw new UnsupportedOperationException("Yeah, I'm not messing with this until types are more solid");
  }
  
  @Override
  public LLVMValueRef visitArrayLiteralFactor(ArrayLiteralFactorContext ctx) {
    return ctx.arrayLiteral().accept(this);
  }
  
  @Override
  public LLVMValueRef visitLiteralFactor(LiteralFactorContext ctx) {
    return ctx.literal().accept(this);
  }
  
  @Override
  public LLVMValueRef visitItFactor(ItFactorContext ctx) {
    throw new BadExpressionException("it does not have a value");
  }
  
  @Override
  public LLVMValueRef visitLabeledBlockExpr(LabeledBlockExprContext ctx) {
    var label = ctx.label();
    if (label != null) {
      var startBlock = LLVMGetInsertBlock(builder);
      var endBlock = LLVMCreateBasicBlockInContext(context, "endLabeledBlock");
      var function = LLVMGetBasicBlockParent(startBlock);
      
      LLVMAppendExistingBasicBlock(function, endBlock);
      LLVMPositionBuilderAtEnd(builder, endBlock);
      // TODO bad types
      var phi = LLVMBuildPhi(builder, LLVMInt32TypeInContext(context), "labeledtmp");
  
      LLVMPositionBuilderAtEnd(builder, startBlock);
      LLVMValueRef result;
      try (var ignored = jumpStack.scopeLabeled(ctx.label().ID().getText(), false, null, endBlock, phi, "a labeled block")) {
        result = ctx.blockExpr().accept(this);
      }
      startBlock = LLVMGetInsertBlock(builder);
      LLVMBuildBr(builder, endBlock);
      
      LLVMPositionBuilderAtEnd(builder, endBlock);
      LLVMAddIncoming(phi, result, startBlock, 1);
      
      return phi;
    } else {
      return ctx.blockExpr().accept(this);
    }
  }
  
  @Override
  public LLVMValueRef visitBlockExpr(BlockExprContext ctx) {
    var lastValue = unit();
    try (var ignored = defStack.scope()) {
      for (var statement : ctx.statement()) lastValue = statement.accept(this);
      var expr = ctx.expr();
      if (expr != null) lastValue = expr.accept(this);
    }
    return lastValue;
  }
  
  @Override
  public LLVMValueRef visitStatement(StatementContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) return varDef.accept(this);
    var expr = ctx.expr();
    if (expr != null) return expr.accept(this);
    return unit();
  }
  
  @Override
  public LLVMValueRef visitFuncCallExpr(FuncCallExprContext ctx) {
    var calleeName = ctx.ID().getText();
    var callee = LLVMGetNamedFunction(module, calleeName);
    if (isNull(callee)) throw new BadExpressionException("Called undefined function: '" + calleeName + "'");
    
    var exprs = ctx.expr();
    var argc = LLVMCountParams(callee);
    if (argc != exprs.size() && LLVMIsFunctionVarArg(LLVMGlobalGetValueType(callee)) == 0) throw new BadExpressionException("Called function '" + calleeName + "' with the wrong number of arguments (expected " + argc + ", got " + exprs.size() + ")");
    
    var argv = new PointerPointer<>(exprs.stream().map(this::visit).toArray(LLVMValueRef[]::new));
    return LLVMBuildCall2(builder, LLVMGlobalGetValueType(callee), callee, argv, argc, "calltmp");
  }
  
  @Override
  public LLVMValueRef visitControlFlowExpr(ControlFlowExprContext ctx) {
    var ifExpr = ctx.ifExpr();
    if (ifExpr != null) return ifExpr.accept(this);
    var loopExpr = ctx.loopExpr();
    if (loopExpr != null) return loopExpr.accept(this);
    var whileExpr = ctx.whileExpr();
    if (whileExpr != null) return whileExpr.accept(this);
    return ctx.jumpExpr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitIfExpr(IfExprContext ctx) {
    var function = LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder));
    var hasElse = ctx.elseBody != null;
    var cond = ctx.cond.accept(this);
    var endIfBlock = LLVMCreateBasicBlockInContext(context, "endIfblock");
    var thenBlock = LLVMCreateBasicBlockInContext(context, "thenBlock");
    var elseBlock = hasElse ? LLVMCreateBasicBlockInContext(context, "elseBlock") : endIfBlock;
    LLVMBuildCondBr(builder, cond, thenBlock, elseBlock);
    LLVMAppendExistingBasicBlock(function, thenBlock);
    LLVMPositionBuilderAtEnd(builder, thenBlock);
    var thenValue = ctx.thenBody.accept(this);
    LLVMBuildBr(builder, endIfBlock);
    thenBlock = LLVMGetInsertBlock(builder);
    LLVMValueRef elseValue = null;
    if (hasElse) {
      LLVMAppendExistingBasicBlock(function, elseBlock);
      LLVMPositionBuilderAtEnd(builder, elseBlock);
      elseValue = ctx.elseBody.accept(this);
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
  public LLVMValueRef visitLoopExpr(LoopExprContext ctx) {
    var label = ctx.label();
    var labelName = label != null ? label.ID().getText() : null;
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
    if (labelName != null) scope = jumpStack.scopeLabeled(labelName, true, startBlock, endBlock, phi, "a labeled loop");
    else scope = jumpStack.scopeAnon(startBlock, endBlock, phi, "an unlabeled loop");
    try (scope) {
      ctx.body.accept(this); // ignore result, unconditionally
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
  public LLVMValueRef visitWhileExpr(WhileExprContext ctx) {
    var label = ctx.label();
    var labelName = label != null ? label.ID().getText() : null;
    var function = LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder));
    var startBlock = LLVMCreateBasicBlockInContext(context, "startWhileBlock");
    var bodyBlock = LLVMCreateBasicBlockInContext(context, "whileBodyBlock");
    var endBlock = LLVMCreateBasicBlockInContext(context, "endWhileBlock");
    
    LLVMBuildBr(builder, startBlock);
    
    LLVMAppendExistingBasicBlock(function, startBlock);
    LLVMPositionBuilderAtEnd(builder, startBlock);
    var cond = ctx.cond.accept(this);
    LLVMBuildCondBr(builder, cond, bodyBlock, endBlock);
    
    LLVMAppendExistingBasicBlock(function, bodyBlock);
    LLVMPositionBuilderAtEnd(builder, bodyBlock);
    JumpScopeStack.Scope scope;
    if (labelName != null) scope = jumpStack.scopeLabeled(labelName, true, startBlock, endBlock, null, "a labeled while loop");
    else scope = jumpStack.scopeAnon(startBlock, endBlock, null, "an unlabeled while loop");
    try (scope) {
      ctx.body.accept(this); // ignore this unconditionally
    }
    LLVMBuildBr(builder, startBlock);
    
    LLVMAppendExistingBasicBlock(function, endBlock);
    LLVMPositionBuilderAtEnd(builder, endBlock);
    
    return unit();
  }
  
  @Override
  public LLVMValueRef visitJumpExpr(JumpExprContext ctx) {
    var atLabel = ctx.atLabel();
    var expr = ctx.expr();
    switch (ctx.jump.getText()) {
      case "return" -> {
        if (!loaderContext.isInFunction()) throw new BadExpressionException("cannot return from a global initializer (consider using a labeled block/break)");
        if (atLabel != null) throw new BadExpressionException("return cannot target a label");
        if (expr != null) {
          var value = expr.accept(this);
          // TODO: bad if value is unit
          LLVMBuildRet(builder, value);
        } else {
          LLVMBuildRetVoid(builder);
        }
        
        return unit();
      }
      case "break" -> {
        JumpScope scope;
        if (atLabel != null) {
          var label = atLabel.ID().getText();
          scope = jumpStack.findLabeled(label);
          if (scope == null) throw new BadExpressionException("break to undefined label: '" + label + "'");
        } else {
          scope = jumpStack.peekAnon();
          if (scope == null) {
            scope = jumpStack.peek();
            if (scope == null) throw new BadExpressionException("break outside of any jump targets");
            throw new BadExpressionException("cannot anonymously break to " + scope.name());
          }
        }
        if (expr != null && scope.phi() == null) throw new BadExpressionException("cannot break with value to " + scope.name());
        if (scope.phi() != null) LLVMAddIncoming(scope.phi(), expr != null ? expr.accept(this) : unit(), LLVMGetInsertBlock(builder), 1);
        LLVMBuildBr(builder, scope.breakBlock());
        return unit();
      }
      case "continue" -> {
        if (expr != null) throw new BadExpressionException("continue cannot accept a value");
        JumpScope scope;
        if (atLabel != null) {
          var label = atLabel.ID().getText();
          scope = jumpStack.findLabeled(label);
          if (scope == null) throw new BadExpressionException("continue to undefined label: '" + label + "'");
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
  public LLVMValueRef visitIndexOp(IndexOpContext ctx) {
    return ctx.expr().accept(this);
  }
  
  @Override
  public LLVMValueRef visitArrayLiteral(ArrayLiteralContext ctx) {
    // TODO bad types; may need to be non-const
    var values = ctx.expr().stream().map(this::visit).toArray(LLVMValueRef[]::new);
    return LLVMConstArray(LLVMInt32TypeInContext(context), new PointerPointer<>(values), values.length);
  }
  
  @Override
  public LLVMValueRef visitLiteral(LiteralContext ctx) {
    if (ctx.int_ != null) {
      return LLVMConstInt(LLVMInt32TypeInContext(context), parseInt(ctx.int_.getText()), 1);
    } else if (ctx.string != null) {
      var str = ctx.string.getText();
      return LLVMConstStringInContext(context, str, str.length(), 0);
    } else if (ctx.bool !=  null) {
      return switch (ctx.bool.getText()) {
        case "true" -> LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0);
        case "false" -> LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0);
        default -> throw new IllegalStateException("Invalid boolean literal");
      };
    } else if (ctx.unit != null) {
      return unit();
    } else {
      throw new IllegalStateException("Invalid literal");
    }
  }
  
}
