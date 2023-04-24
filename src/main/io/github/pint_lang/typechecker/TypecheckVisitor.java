package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.*;
import io.github.pint_lang.typechecker.conditions.*;

import java.util.ArrayList;
import java.util.Stack;

public class TypecheckVisitor implements DefASTVisitor<Void, Void>, ExprASTVisitor<Void, ExprAST<Type>>, TypeASTVisitor<Void, TypeAST<Type>> {

  public final ErrorLogger.Fixed<Type> logger;
  public final GlobalLookup globals;
  private final GlobalLookup.BuildVisitor globalsBuilder;
  private final JumpScopeStack jumpStack;
  private final VarScopeStack varStack;
  private final Stack<Type> itStack = new Stack<>();
  private final StatVisitor statVisitor = new StatVisitor();
  private final ConditionBuildVisitor conditionBuildVisitor;
  
  public TypecheckVisitor(ErrorLogger.Fixed<Type> logger, GlobalLookup globals) {
    this.logger = logger;
    this.globals = globals;
    this.globalsBuilder = globals.new BuildVisitor(this);
    this.jumpStack = new JumpScopeStack();
    this.varStack = new VarScopeStack();
    this.conditionBuildVisitor = new ConditionBuildVisitor(logger.parent());
  }
  
  public void visitDefs(DefsAST<Void> ast) {
    for (var def : ast.defs()) {
      if (def instanceof FuncDefAST<Void>) def.accept(globalsBuilder);
    }
    for (var def : ast.defs()) def.accept(this);
  }
  
  @Override
  public Void visitFuncDef(FuncDefAST<Void> ast) {
    var funcType = globals.getFunctionType(ast.name());
    globals.setThisFunctionType(funcType);
    var value = ast.body().accept(this);
    if (value.data() != Type.ERROR && !value.data().canBe(funcType.returnType())) logger.error("Tried to return wrong type");
    globals.setThisFunctionType(null);
    return null;
  }
  
  @Override
  public Void visitVarDef(VarDefAST<Void> ast) {
    var type = ast.type().accept(this).data();
    var value = ast.value().accept(this);
    if (!value.data().canBe(type)) logger.error("Tried to initialize a variable with the wrong type");
    ast.accept(globalsBuilder);
    return null;
  }

  @Override
  public SimpleTypeAST<Type> visitSimpleType(SimpleTypeAST<Void> ast) {
    return new SimpleTypeAST<>(ast.name(), switch (ast.name()) {
      case "string" -> Type.STRING;
      case "int" -> Type.INT;
      case "bool" -> Type.BOOL;
      default -> logger.error("No such type as '" + ast.name() + "'");
    });
  }

  @Override
  public UnitTypeAST<Type> visitUnitType(UnitTypeAST<Void> ast) {
    return new UnitTypeAST<>(Type.UNIT);
  }

  @Override
  public ArrayTypeAST<Type> visitArrayType(ArrayTypeAST<Void> ast) {
    var innerType = ast.innerType().accept(this);
    return new ArrayTypeAST<>(innerType, new Type.Array(innerType.data()));
  }

  @Override
  public ConditionTypeAST<Type> visitConditionType(ConditionTypeAST<Void> ast) {
    var type = ast.type().accept(this);
    itStack.push(type.data());
    var conditionAST = ast.condition().accept(this);
    var condition = conditionAST.accept(conditionBuildVisitor);
    itStack.pop();
    return new ConditionTypeAST<>(type, conditionAST, new Type.Condition(type.data(), condition, conditionBuildVisitor.takeBindings()));
  }
  
  @Override
  public UnaryExprAST<Type> visitUnaryExpr(UnaryExprAST<Void> ast) {
    var op = ast.op();
    var operand = ast.operand().accept(this);
    var data = operand.data();
    if (data == Type.ERROR) return new UnaryExprAST<>(op, operand, Type.ERROR);
    return new UnaryExprAST<>(op, operand, switch (op) {
      case PLUS, NEG -> data.canBe(Type.INT) ? Type.INT : logger.error("Unary arithmetic operators only apply to integers");
      case NOT -> data.canBe(Type.BOOL) ? Type.BOOL : logger.error("The unary not operator only applies to booleans");
      case ABS -> data.canBe(Type.INT) || data.canBe(Type.STRING) || data.canBeArray() ? Type.INT : logger.error("The unary magnitude operator only applies to integers, strings, or arrays");
    });
  }
  
  @Override
  public BinaryExprAST<Type> visitBinaryExpr(BinaryExprAST<Void> ast) {
    var op = ast.op();
    var left = ast.left().accept(this);
    var right = ast.right().accept(this);
    var leftData = left.data();
    var rightData = right.data();
    if (leftData == Type.ERROR || rightData == Type.ERROR) return new BinaryExprAST<>(op, left, right, Type.ERROR);
    return new BinaryExprAST<>(op, left, right, switch (op) {
        case ASSIGN -> rightData.canBe(leftData) ? left instanceof VarExprAST<Type> || left instanceof IndexExprAST<Type> ? Type.UNIT : logger.error("Only variables or items of an array can be assigned to") : logger.error("The simple binary assignment operator only applies to similar types");
        case ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN -> leftData.canBe(Type.INT) && rightData.canBe(Type.INT) ? left instanceof VarExprAST<Type> || left instanceof IndexExprAST<Type> ? Type.UNIT : logger.error("Only variables or items of an array can be assigned to") : logger.error("Compound binary assignment operators only apply to integers");
        case OR, AND -> leftData.canBe(Type.BOOL) && rightData.canBe(Type.BOOL) ? Type.BOOL : logger.error("Binary logical operators only apply to booleans");
        case EQ, NEQ -> leftData.eitherCanBe(rightData) ? Type.BOOL : logger.error("Binary equality operators only apply to similar types");
        case LT, NLT, LE, NLE, GT, NGT, GE, NGE -> leftData.canBe(Type.INT) && rightData.canBe(Type.INT) || leftData.canBe(Type.STRING) && rightData.canBe(Type.STRING) ? Type.BOOL : logger.error("Binary comparison operators only apply to integers or strings");
        case ADD, SUB, MUL, DIV -> leftData.canBe(Type.INT) && rightData.canBe(Type.INT) ? Type.INT : logger.error("Binary arithmetic operators only apply to integers");
    });
  }
  
  @Override
  public BlockExprAST<Type> visitBlockExpr(BlockExprAST<Void> ast) {
    if (ast.label() != null) {
      varStack.push();
      jumpStack.pushLabeledOnly(ast.label(), Type.NEVER);
      var stats = new ArrayList<StatAST<Type>>(ast.stats().size());
      for (var statAST : ast.stats()) {
        var stat = statAST.accept(statVisitor);
        if (stat.data() == Type.ERROR) jumpStack.peek().unifyType(Type.ERROR, logger);
        stats.add(stat);
      }
      var lastStatType = !stats.isEmpty() ? stats.get(stats.size() - 1).data() : Type.UNIT;
      var type = jumpStack.pop().unifyType(lastStatType, logger);
      varStack.pop();
      return new BlockExprAST<>(ast.label(), stats, type);
    } else {
      varStack.push();
      var stats = new ArrayList<StatAST<Type>>(ast.stats().size());
      var error = false;
      for (var statAST : ast.stats()) {
        var stat = statAST.accept(statVisitor);
        if (stat.data() == Type.ERROR) error = true;
        stats.add(stat);
      }
      var lastStatType = !stats.isEmpty() ? stats.get(stats.size() - 1).data() : Type.UNIT;
      var type = error ? Type.ERROR : lastStatType;
      varStack.pop();
      return new BlockExprAST<>(ast.label(), stats, type);
    }
  }
  
  @Override
  public VarExprAST<Type> visitVarExprAST(VarExprAST<Void> ast) {
    var name = ast.name();
    var local = varStack.getVar(name);
    if (local != null) return new VarExprAST<>(name, local);
    var params = globals.getThisFunctionType().params();
    for (var param : params) if (param.name().equals(name)) return new VarExprAST<>(name, param.type());
    var global = globals.getVariableType(name);
    if (global != null) return new VarExprAST<>(name, global);
    return new VarExprAST<>(name, logger.error("No such variable as '" + name + "'"));
  }
  
  @Override
  public FuncCallExprAST<Type> visitFuncCall(FuncCallExprAST<Void> ast) {
    var funcType = globals.getFunctionType(ast.funcName());
    var args = ast.args().stream().map(this::visitExpr).toList();
    if (funcType == null) return new FuncCallExprAST<>(ast.funcName(), args, logger.error("No such function as '" + ast.funcName() + "'"));
    if (funcType.params().size() != ast.args().size()) return new FuncCallExprAST<>(ast.funcName(), args, logger.error("Function '" + ast.funcName() + "' expected " + funcType.params().size() + " arguments, got " + args.size()));
    var hasError = false;
    for (var i = 0; i < args.size(); i++) {
      if (args.get(i).data() == Type.ERROR) {
        hasError = true;
      } else if (!args.get(i).data().canBe(funcType.params().get(i).type())) {
        hasError = true;
        logger.error("Function '" + ast.funcName() + "' expected an argument of type '" + funcType.params().get(i).type() + "', got '" + args.get(i).data() + "'");
      }
    }
    return new FuncCallExprAST<>(ast.funcName(), args, hasError ? Type.ERROR : funcType.returnType());
  }
  
  @Override
  public IndexExprAST<Type> visitIndexExpr(IndexExprAST<Void> ast) {
    var indexee = ast.indexee().accept(this);
    var index = ast.index().accept(this);
    if (indexee.data() == Type.ERROR || index.data() == Type.ERROR) return new IndexExprAST<>(indexee, index, Type.ERROR);
    var indexeeArray = indexee.data().asArray();
    if (indexeeArray == null) return new IndexExprAST<>(indexee, index, logger.error("Only arrays can be indexed"));
    if (!index.data().canBe(arrayIndexCondition(indexee))) return new IndexExprAST<>(indexee, index, logger.error("Only int when it >= 0 and it < |<arr>| can be used as indices for array <arr>"));
    return new IndexExprAST<>(indexee, index, indexeeArray.elementType());
  }
  
  private Type.Condition arrayIndexCondition(ExprAST<Type> indexee) {
    var itCondition = new ItExprAST<>(Type.INT).accept(conditionBuildVisitor);
    var indexeeCondition = indexee.accept(conditionBuildVisitor);
    return new Type.Condition(
      Type.INT,
      AndCondition.and(
        CmpCondition.ge(
          itCondition,
          ConstantCondition.integer(0)
        ),
        CmpCondition.lt(
          itCondition,
          UnaryCondition.abs(
            indexeeCondition
          )
        )
      ),
      conditionBuildVisitor.takeBindings()
    );
  }
  
  @Override
  public SliceExprAST<Type> visitSliceExpr(SliceExprAST<Void> ast) {
    var slicee = ast.slicee().accept(this);
    var from = ast.from() != null ? ast.from().accept(this) : null;
    var to = ast.to() != null ? ast.to().accept(this) : null;
    if (slicee.data() == Type.ERROR || from != null && from.data() == Type.ERROR || to != null && to.data() == Type.ERROR) return new SliceExprAST<>(slicee, from, to, Type.ERROR);
    var sliceeArray = slicee.data().asArray();
    if (sliceeArray == null) return new SliceExprAST<>(slicee, from, to, logger.error("Only arrays can be sliced"));
    if (from != null && !from.data().canBe(arraySliceFrom(slicee))) return new SliceExprAST<>(slicee, from, to, logger.error("Only int when it >= 0 and it <= |<arr>| can be used as from indices for slicing array <arr>"));
    if (to != null && !to.data().canBe(arraySliceTo(slicee, from))) return new SliceExprAST<>(slicee, from, to, logger.error("Only int when it >= <from> and it <= |<arr>| can be used as to indices for slicing array <arr>"));
    return new SliceExprAST<>(slicee, from, to, new Type.Array(sliceeArray.elementType()));
  }
  
  private Type.Condition arraySliceFrom(ExprAST<Type> slicee) {
    var itCondition = new ItExprAST<>(Type.INT).accept(conditionBuildVisitor);
    var sliceeCondition = slicee.accept(conditionBuildVisitor);
    return new Type.Condition(
      Type.INT,
      AndCondition.and(
        CmpCondition.ge(
          itCondition,
          ConstantCondition.integer(0)
        ),
        CmpCondition.le(
          itCondition,
          UnaryCondition.abs(
            sliceeCondition
          )
        )
      ),
      conditionBuildVisitor.takeBindings()
    );
  }
  
  private Type.Condition arraySliceTo(ExprAST<Type> slicee, ExprAST<Type> from) {
    var itCondition = new ItExprAST<>(Type.INT).accept(conditionBuildVisitor);
    var sliceeCondition = slicee.accept(conditionBuildVisitor);
    var fromCondition = from != null ? from.accept(conditionBuildVisitor) : ConstantCondition.integer(0);
    return new Type.Condition(
      Type.INT,
      AndCondition.and(
        CmpCondition.ge(
          itCondition,
          fromCondition
        ),
        CmpCondition.le(
          itCondition,
          UnaryCondition.abs(
            sliceeCondition
          )
        )
      ),
      conditionBuildVisitor.takeBindings()
    );
  }
  
  @Override
  public ItExprAST<Type> visitItExpr(ItExprAST<Void> ast) {
    if (itStack.isEmpty()) return new ItExprAST<>(logger.error("It must only be in a type condition"));
    return new ItExprAST<>(itStack.peek());
  }
  
  @Override
  public IfExprAST<Type> visitIfExpr(IfExprAST<Void> ast) {
    var condition = ast.condition().accept(this);
    var vars = condition.accept(new VariableFindVisitor());
    varStack.push();
    for (var var : vars) {
      var mapper = new ConditionMapVisitor(var.name(), logger.parent());
      var typeCondition = condition.accept(mapper);
      varStack.putVar(var.name(), var.data().joinCondition(typeCondition, mapper.takeBindings()));
    }
    var thenBody = ast.thenBody().accept(this);
    varStack.pop();
    var elseBody = ast.elseBody() != null ? ast.elseBody().accept(this) : null;
    if (condition.data() == Type.ERROR || thenBody.data() == Type.ERROR || elseBody != null && elseBody.data() == Type.ERROR) return new IfExprAST<>(condition, thenBody, elseBody, Type.ERROR);
    if (condition.data() != Type.BOOL) return new IfExprAST<>(condition, thenBody, elseBody, logger.error("If conditions must be booleans"));
    var resultType = elseBody != null ? thenBody.data().unify(elseBody.data(), logger) : Type.UNIT;
    return new IfExprAST<>(condition, thenBody, elseBody, resultType);
  }
  
  @Override
  public LoopExprAST<Type> visitLoopExpr(LoopExprAST<Void> ast) {
    if (ast.label() != null) jumpStack.pushLabeledOrAnon(ast.label(), Type.NEVER);
    else jumpStack.pushAnonOnly(Type.NEVER);
    var body = ast.body().accept(this);
    var type = jumpStack.pop().getType();
    if (body.data() == Type.ERROR) type = Type.ERROR;
    return new LoopExprAST<>(ast.label(), body, type);
  }
  
  @Override
  public WhileExprAST<Type> visitWhileExpr(WhileExprAST<Void> ast) {
    if (ast.label() != null) jumpStack.pushLabeledOrAnon(ast.label(), Type.UNIT);
    else jumpStack.pushAnonOnly(Type.UNIT);
    var condition = ast.condition().accept(this);
    var type = condition.data() == Type.BOOL ? Type.UNIT : condition.data() == Type.ERROR ? Type.ERROR : logger.error("While conditions must be booleans");
    jumpStack.peek().unifyType(type, logger);
    var vars = condition.accept(new VariableFindVisitor());
    varStack.push();
    for (var var : vars) {
      var mapper = new ConditionMapVisitor(var.name(), logger.parent());
      var typeCondition = condition.accept(mapper);
      varStack.putVar(var.name(), var.data().joinCondition(typeCondition, mapper.takeBindings()));
    }
    var body = ast.body().accept(this);
    varStack.pop();
    type = jumpStack.pop().getType();
    if (body.data() == Type.ERROR) type = Type.ERROR;
    return new WhileExprAST<>(ast.label(), condition, body, type);
  }
  
  @Override
  public JumpExprAST<Type> visitJumpExpr(JumpExprAST<Void> ast) {
    Type type = Type.NEVER;
    var value = ast.value() != null ? ast.value().accept(this) : null;
    return switch (ast.kind()) {
      case RETURN -> {
        if (ast.targetLabel() != null) type = logger.error("return can't target a label");
        var funcType = globals.getThisFunctionType();
        if (funcType == null) type = logger.error("Cannot return here");
        else if ((value == null || value.data() != Type.ERROR) && !(value != null ? value.data() : Type.UNIT).canBe(funcType.returnType())) type = logger.error("Tried to return wrong type");
        yield new JumpExprAST<>(JumpKind.RETURN, ast.targetLabel(), value, type);
      }
      case BREAK -> {
        JumpScope scope;
        if (ast.targetLabel() != null) {
          scope = jumpStack.findLabeled(ast.targetLabel());
          if (scope == null) type = logger.error("No such label as '" + ast.targetLabel() + "'");
        } else {
          scope = jumpStack.peekAnon();
          if (scope == null) type = logger.error("Cannot break anonymously here");
        }
        if (scope != null) scope.unifyType(value != null ? value.data() : Type.UNIT, logger);
        yield new JumpExprAST<>(JumpKind.BREAK, ast.targetLabel(), value, type);
      }
      case CONTINUE -> {
        if (ast.targetLabel() != null) {
          if (jumpStack.findLabeled(ast.targetLabel()) == null) type = logger.error("No such label as '" + ast.targetLabel() + "'");
        } else {
          if (jumpStack.peekAnon() == null) type = logger.error("Cannot continue anonymously here");
          if (value != null) type = logger.error("continue cannot accept a value");
        }
        yield new JumpExprAST<>(JumpKind.CONTINUE, ast.targetLabel(), value, type);
      }
    };
  }
  
  @Override
  public ArrayLiteralExprAST<Type> visitArrayLiteralExpr(ArrayLiteralExprAST<Void> ast) {
    Type elementType = Type.NEVER;
    var elements = new ArrayList<ArrayLiteralExprAST.Item<Type>>(ast.items().size());
    for (var item : ast.items()) {
      if (item.spread()) {
        var spreadee = item.item().accept(this);
        elements.add(new ArrayLiteralExprAST.Item<>(spreadee, true));
        if (spreadee.data() == Type.ERROR) {
          elementType = Type.ERROR;
        } else {
          var spreadeeArray = spreadee.data().asArray();
          if (spreadeeArray != null) elementType = elementType.unify(spreadeeArray.elementType(), logger);
          else elementType = logger.error("Only arrays can be spread into arrays");
        }
      } else {
        var element = item.item().accept(this);
        elements.add(new ArrayLiteralExprAST.Item<>(element, false));
        elementType = elementType.unify(element.data(), logger);
      }
    }
    return new ArrayLiteralExprAST<>(elements, elementType == Type.ERROR ? Type.ERROR : new Type.Array(elementType));
  }
  
  @Override
  public StringLiteralExprAST<Type> visitStringLiteralExpr(StringLiteralExprAST<Void> ast) {
    return new StringLiteralExprAST<>(ast.value(), Type.STRING);
  }
  
  @Override
  public IntLiteralExprAST<Type> visitIntLiteralExpr(IntLiteralExprAST<Void> ast) {
    return new IntLiteralExprAST<>(ast.value(), Type.INT);
  }
  
  @Override
  public BoolLiteralExprAST<Type> visitBoolLiteralExpr(BoolLiteralExprAST<Void> ast) {
    return new BoolLiteralExprAST<>(ast.value(), Type.BOOL);
  }
  
  @Override
  public UnitLiteralExprAST<Type> visitUnitLiteralExpr(UnitLiteralExprAST<Void> ast) {
    return new UnitLiteralExprAST<>(Type.UNIT);
  }
  
  private class StatVisitor extends ExprDelegateStatASTVisitor<Void, StatAST<Type>> {
    
    public StatVisitor() {
      super(TypecheckVisitor.this);
    }
    
    @Override
    public VarDefAST<Type> visitVarDef(VarDefAST<Void> ast) {
      Type type = Type.UNIT;
      var varType = ast.type().accept(TypecheckVisitor.this);
      var value = ast.value().accept(TypecheckVisitor.this);
      if (!value.data().canBe(varType.data())) type = logger.error("Tried to initialize a variable with the wrong type");
      varStack.putVar(ast.name(), varType.data());
      return new VarDefAST<>(ast.name(), varType, value, type);
    }
    
    @Override
    public NopStatAST<Type> visitNopStat(NopStatAST<Void> ast) {
      return new NopStatAST<>(Type.UNIT);
    }
    
  }
  
  private static class ConditionBuildVisitor implements ExprASTVisitor<Type, Condition> {
    
    private final ConditionBindings.Builder bindings = new ConditionBindings.Builder();
    private final ErrorLogger.Fixed<Condition> logger;
    
    private ConditionBuildVisitor(ErrorLogger logger) {
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
        case ASSIGN, ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN -> logger.error("Assignment operators are not supported in type conditions");
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
      return bindings.getOrBindVar(ast.name());
    }
    
    @Override
    public Condition visitFuncCall(FuncCallExprAST<Type> ast) {
      return logger.error("Function calls are not supported in type conditions (yet)");
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
      return bindings.getOrBindIt();
    }
    
    @Override
    public Condition visitIfExpr(IfExprAST<Type> ast) {
      return logger.error("If expressions are not supported in type conditions");
    }
    
    @Override
    public Condition visitLoopExpr(LoopExprAST<Type> ast) {
      return logger.error("Loops are not supported in type conditions");
    }
    
    @Override
    public Condition visitWhileExpr(WhileExprAST<Type> ast) {
      return logger.error("While loops are not supported in type conditions");
    }
    
    @Override
    public Condition visitJumpExpr(JumpExprAST<Type> ast) {
      return logger.error("Jump expressions are not supported in type conditions");
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
  
}