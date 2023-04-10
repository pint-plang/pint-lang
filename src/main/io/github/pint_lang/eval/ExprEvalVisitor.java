package io.github.pint_lang.eval;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;
import io.github.pint_lang.eval.ExprEvalControlFlow.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class ExprEvalVisitor extends PintBaseVisitor<ExprEvalControlFlow> {
  
  public final ScopeStack stack;
  
  public ExprEvalVisitor(ScopeStack stack) {
    this.stack = stack;
  }
  
  @Override
  public ExprEvalControlFlow visitVarDef(VarDefContext ctx) {
    var varName = ctx.ID().getText();
    var exprFlow = ctx.expr().accept(this);
    if (!(exprFlow instanceof Finish exprFinish)) return exprFlow;
    stack.peek().defineVariable(varName, exprFinish.value());
    return new Finish(Value.UNIT);
  }
  
  @Override
  public ExprEvalControlFlow visitFactorExpr(FactorExprContext ctx) {
    return ctx.factor().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitAssignExpr(AssignExprContext ctx) {
    if (ctx.left instanceof FactorExprContext factorCtx) {
      var factor = factorCtx.factor();
      if (factor instanceof VarFactorContext varFactor) {
        var scope = stack.peek();
        var leftName = varFactor.ID().getText();
        var leftValue = scope.getVariable(leftName).orElseThrow(() -> NoSuchNameException.variable(leftName));
        var rightFlow = ctx.right.accept(this);
        if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
        var rightValue = rightFinish.value();
        var op = ctx.op.getText();
        if (":=".equals(op)) {
          scope.setVariable(leftName, rightValue);
        } else {
          if (!(leftValue instanceof IntValue leftInt) || !(rightValue instanceof IntValue rightInt)) throw new BadTypeException("Arithmetic assignment operators only apply to ints");
          switch (op) {
            case ":+=" -> scope.setVariable(leftName, Value.of(leftInt.value() + rightInt.value()));
            case ":-=" -> scope.setVariable(leftName, Value.of(leftInt.value() - rightInt.value()));
            case ":*=" -> scope.setVariable(leftName, Value.of(leftInt.value() * rightInt.value()));
            case ":/=" -> scope.setVariable(leftName, Value.of(leftInt.value() / rightInt.value()));
            default -> throw new IllegalStateException("Invalid assignment operator");
          }
        }
        return new Finish(Value.UNIT);
      }
    }
    throw new BadExpressionException("Only variables can be assigned to");
  }
  
  
  @Override
  public ExprEvalControlFlow visitUnaryExpr(UnaryExprContext ctx) {
    var exprFlow = ctx.expr().accept(this);
    if (!(exprFlow instanceof Finish exprFinish)) return exprFlow;
    var exprValue = exprFinish.value();
    switch (ctx.op.getText()) {
      case "+" -> {
        if (!(exprValue instanceof IntValue)) throw new BadTypeException("Unary + operator only applies to ints");
        // no-op
        return exprFinish;
      }
      case "-" -> {
        if (!(exprValue instanceof IntValue factorInt)) throw new BadTypeException("Unary - operator only applies to ints");
        return new Finish(Value.of(-factorInt.value()));
      }
      case "not" -> {
        if (!(exprValue instanceof BoolValue factorBool)) throw new BadTypeException("Unary not operator only applies to ints");
        return new Finish(Value.of(!factorBool.value()));
      }
      default -> throw new IllegalStateException("Invalid unary operator");
    }
  }
  
  @Override
  public ExprEvalControlFlow visitMulExpr(MulExprContext ctx) {
    var leftFlow = ctx.left.accept(this);
    if (!(leftFlow instanceof Finish leftFinish)) return leftFlow;
    if (!(leftFinish.value() instanceof IntValue leftValue)) throw new BadTypeException("Binary arithmetic operators only applies to ints");
    var rightFlow = ctx.right.accept(this);
    if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
    if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("Binary arithmetic operators only applies to ints");
    return new Finish(Value.of(switch (ctx.op.getText()) {
      case "*" -> leftValue.value() * rightValue.value();
      case "/" -> leftValue.value() / rightValue.value();
      default -> throw new IllegalStateException("Invalid multiplication expression");
    }));
  }
  
  @Override
  public ExprEvalControlFlow visitAddExpr(AddExprContext ctx) {
    var leftFlow = ctx.left.accept(this);
    if (!(leftFlow instanceof Finish leftFinish)) return leftFlow;
    if (!(leftFinish.value() instanceof IntValue leftValue)) throw new BadTypeException("Binary arithmetic operators only applies to ints");
    var rightFlow = ctx.right.accept(this);
    if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
    if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("Binary arithmetic operators only applies to ints");
    return new Finish(Value.of(switch (ctx.op.getText()) {
      case "+" -> leftValue.value() + rightValue.value();
      case "-" -> leftValue.value() - rightValue.value();
      default -> throw new IllegalStateException("Invalid addition expression");
    }));
  }
  
  @Override
  public ExprEvalControlFlow visitCmpExpr(CmpExprContext ctx) {
    var leftFlow = ctx.left.accept(this);
    if (!(leftFlow instanceof Finish leftFinish)) return leftFlow;
    var rightFlow = ctx.right.accept(this);
    if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
    var not = ctx.not != null;
    var result = switch (ctx.op.getText()) {
      case "=" -> leftFinish.value().valueEquals(rightFinish.value());
      case "<" -> {
        if (leftFinish.value() instanceof IntValue leftValue) {
          if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("ints can only be compared with ints");
          yield leftValue.value() < rightValue.value();
        } else if (leftFinish.value() instanceof StringValue leftValue) {
          if (!(rightFinish.value() instanceof StringValue rightValue)) throw new BadTypeException("strings can only be compared with strings");
          yield leftValue.value().compareTo(rightValue.value()) < 0;
        } else {
          throw new BadTypeException("Binary comparison operators only apply to ints and strings");
        }
      }
      case "<=" -> {
        if (leftFinish.value() instanceof IntValue leftValue) {
          if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("ints can only be compared with ints");
          yield leftValue.value() <= rightValue.value();
        } else if (leftFinish.value() instanceof StringValue leftValue) {
          if (!(rightFinish.value() instanceof StringValue rightValue)) throw new BadTypeException("strings can only be compared with strings");
          yield leftValue.value().compareTo(rightValue.value()) <= 0;
        } else {
          throw new BadTypeException("Binary comparison operators only apply to ints and strings");
        }
      }
      case ">" -> {
        if (leftFinish.value() instanceof IntValue leftValue) {
          if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("ints can only be compared with ints");
          yield leftValue.value() > rightValue.value();
        } else if (leftFinish.value() instanceof StringValue leftValue) {
          if (!(rightFinish.value() instanceof StringValue rightValue)) throw new BadTypeException("strings can only be compared with strings");
          yield leftValue.value().compareTo(rightValue.value()) > 0;
        } else {
          throw new BadTypeException("Binary comparison operators only apply to ints and strings");
        }
      }
      case ">=" -> {
        if (leftFinish.value() instanceof IntValue leftValue) {
          if (!(rightFinish.value() instanceof IntValue rightValue)) throw new BadTypeException("ints can only be compared with ints");
          yield leftValue.value() >= rightValue.value();
        } else if (leftFinish.value() instanceof StringValue leftValue) {
          if (!(rightFinish.value() instanceof StringValue rightValue)) throw new BadTypeException("strings can only be compared with strings");
          yield leftValue.value().compareTo(rightValue.value()) >= 0;
        } else {
          throw new BadTypeException("Binary comparison operators only apply to ints and strings");
        }
      }
      default -> throw new IllegalStateException("Invalid comparison expression");
    };
    return new Finish(Value.of(not != result)); // i.e. not ? !result : result
  }
  
  @Override
  public ExprEvalControlFlow visitAndExpr(AndExprContext ctx) {
    var leftFlow = ctx.left.accept(this);
    if (!(leftFlow instanceof Finish leftFinish)) return leftFlow;
    if (!(leftFinish.value() instanceof BoolValue leftValue)) throw new BadTypeException("Binary and operator only applies to bools");
    if (!leftValue.value()) return new Finish(Value.FALSE); // short-circuit
    var rightFlow = ctx.right.accept(this);
    if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
    if (!(rightFinish.value() instanceof BoolValue)) throw new BadTypeException("Binary and operator only applies to bools");
    return rightFinish; // i.e. new Finish(Value.of(true && rightValue.value()) since leftValue short-circuits when false
  }
  
  @Override
  public ExprEvalControlFlow visitOrExpr(OrExprContext ctx) {
    var leftFlow = ctx.left.accept(this);
    if (!(leftFlow instanceof Finish leftFinish)) return leftFlow;
    if (!(leftFinish.value() instanceof BoolValue leftValue)) throw new BadTypeException("Binary or operator only applies to bools");
    if (leftValue.value()) return new Finish(Value.TRUE); // short-circuit
    var rightFlow = ctx.right.accept(this);
    if (!(rightFlow instanceof Finish rightFinish)) return rightFlow;
    if (!(rightFinish.value() instanceof BoolValue)) throw new BadTypeException("Binary or operator only applies to bools");
    return rightFinish; // i.e. new Finish(Value.of(false || rightValue.value()) since leftValue short-circuits when true
  }
  
  @Override
  public ExprEvalControlFlow visitLabeledBlockFactor(LabeledBlockFactorContext ctx) {
    return ctx.labeledBlockExpr().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitParensFactor(ParensFactorContext ctx) {
    return ctx.expr().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitAbsFactor(AbsFactorContext ctx) {
    var exprFlow = ctx.expr().accept(this);
    if (!(exprFlow instanceof Finish exprFinish)) return exprFlow;
    if (exprFinish.value() instanceof IntValue exprValue) return new Finish(Value.of(Math.abs(exprValue.value())));
    else if (exprFinish.value() instanceof StringValue exprValue) return new Finish(Value.of(exprValue.value().length()));
    else if (exprFinish.value() instanceof ArrayValue exprValue) return new Finish(Value.of(exprValue.values().length));
    else throw new BadTypeException("Unary | | operator only applies to ints, strings and arrays");
  }
  
  @Override
  public ExprEvalControlFlow visitVarFactor(VarFactorContext ctx) {
    var name = ctx.ID().getText();
    return new Finish(stack.peek().getVariable(name).orElseThrow(() -> NoSuchNameException.variable(name)));
  }
  
  @Override
  public ExprEvalControlFlow visitFuncCallFactor(FuncCallFactorContext ctx) {
    return ctx.funcCallExpr().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitControlFlowFactor(ControlFlowFactorContext ctx) {
    return ctx.controlFlowExpr().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitIndexFactor(IndexFactorContext ctx) {
    var factorFlow = ctx.factor().accept(this);
    if (!(factorFlow instanceof Finish factorFinish)) return factorFlow;
    if (!(factorFinish.value() instanceof ArrayValue factorValue)) throw new BadTypeException("Only arrays can be indexed");
    if (ctx.indexOp() instanceof IndexIndexOpContext indexIndexOpCtx) {
      var indexFlow = indexIndexOpCtx.expr().accept(this);
      if (!(indexFlow instanceof Finish indexFinish)) return indexFlow;
      if (!(indexFinish.value() instanceof IntValue indexValue)) throw new BadTypeException("Array indices must be ints");
      return new Finish(factorValue.values()[indexValue.value()]);
    } else if (ctx.indexOp() instanceof SliceIndexOpContext sliceIndexOpCtx) {
      var from = 0;
      if (sliceIndexOpCtx.from != null) {
        var fromFlow = sliceIndexOpCtx.from.accept(this);
        if (!(fromFlow instanceof Finish fromFinish)) return fromFlow;
        if (!(fromFinish.value() instanceof IntValue fromValue))
          throw new BadTypeException("Array indices must be ints");
        from = fromValue.value();
      }
      var to = factorValue.values().length;
      if (sliceIndexOpCtx.to != null) {
        var toFlow = sliceIndexOpCtx.to.accept(this);
        if (!(toFlow instanceof Finish toFinish)) return toFlow;
        if (!(toFinish.value() instanceof IntValue toValue)) throw new BadTypeException("Array indices must be ints");
        to = toValue.value();
      }
      if (to < from) throw new BadTypeException("Array slice from index must be less than or equal to to index");
      var newValues = new Value[to - from];
      System.arraycopy(factorValue.values(), from, newValues, 0, newValues.length);
      return new Finish(Value.of(newValues));
    } else {
      throw new IllegalStateException("Invalid array index operation");
    }
  }
  
  @Override
  public ExprEvalControlFlow visitArrayLiteralFactor(ArrayLiteralFactorContext ctx) {
    return ctx.arrayLiteral().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitLiteralFactor(LiteralFactorContext ctx) {
    return ctx.literal().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitItFactor(ItFactorContext ctx) {
    throw new BadExpressionException("it does not have a value");
  }
  
  @Override
  public ExprEvalControlFlow visitLabeledBlockExpr(LabeledBlockExprContext ctx) {
    var label = ctx.label();
    var labelName = label != null ? label.ID().getText() : null;
    var blockFlow = ctx.blockExpr().accept(this);
    if (labelName != null) {
      if (blockFlow instanceof Break blockBreak) {
        if (blockBreak.label() == null) throw new BadExpressionException("Unlabeled break not allowed in labeled block");
        if (blockBreak.label().equals(labelName)) return new Finish(blockBreak.value());
      } else if (blockFlow instanceof Continue blockContinue) {
        if (blockContinue.label() == null) throw new BadExpressionException("Unlabeled continue not allowed in labeled block");
        if (blockContinue.label().equals(labelName)) throw new BadExpressionException("Labeled continues cannot target labeled blocks");
      }
      // any breaks or continues that make it here target other labels
    }
    return blockFlow;
  }
  
  @Override
  public ExprEvalControlFlow visitBlockExpr(BlockExprContext ctx) {
    Value lastValue = Value.UNIT;
    try (var ignored = stack.guardLocal()) {
      for (var statement : ctx.statement()) {
        var statementFlow = statement.accept(this);
        if (!(statementFlow instanceof Finish statementFinish)) return statementFlow;
        lastValue = statementFinish.value();
      }
      var expr = ctx.expr();
      if (expr != null) {
        var exprFlow = expr.accept(this);
        if (!(exprFlow instanceof Finish exprFinish)) return exprFlow;
        lastValue = exprFinish.value();
      }
    }
    return new Finish(lastValue);
  }
  
  @Override
  public ExprEvalControlFlow visitStatement(StatementContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) return varDef.accept(this);
    var expr = ctx.expr();
    if (expr != null) return expr.accept(this);
    return new Finish(Value.UNIT);
  }
  
  @Override
  public ExprEvalControlFlow visitFuncCallExpr(FuncCallExprContext ctx) {
    var funcName = ctx.ID().getText();
    var func = stack.peek().getFunction(funcName).orElseThrow(() -> NoSuchNameException.function(funcName));
    var exprs = ctx.expr();
    var args = new ArrayList<Value>(exprs.size());
    for (var expr : exprs) {
      var exprFlow = expr.accept(this);
      if (!(exprFlow instanceof Finish exprFinish)) return exprFlow;
      args.add(exprFinish.value());
    }
    try (var ignored = stack.guardFunction()) {
      return new Finish(func.call(this, args));
    }
  }
  
  @Override
  public ExprEvalControlFlow visitControlFlowExpr(ControlFlowExprContext ctx) {
    var ifExpr = ctx.ifExpr();
    if (ifExpr != null) return ifExpr.accept(this);
    var loopExpr = ctx.loopExpr();
    if (loopExpr != null) return loopExpr.accept(this);
    var whileExpr = ctx.whileExpr();
    if (whileExpr != null) return whileExpr.accept(this);
    return ctx.jumpExpr().accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitIfExpr(IfExprContext ctx) {
    var condFlow = ctx.cond.accept(this);
    if (!(condFlow instanceof Finish condFinish)) return condFlow;
    if (!(condFinish.value() instanceof BoolValue condValue)) throw new BadTypeException("if condition must be a bool");
    return condValue.value() ? ctx.thenBody.accept(this) : ctx.elseBody.accept(this);
  }
  
  @Override
  public ExprEvalControlFlow visitLoopExpr(LoopExprContext ctx) {
    var label = ctx.label();
    var labelName = label != null ? label.ID().getText() : null;
    while (true) {
      var bodyFlow = ctx.body.accept(this);
      if (bodyFlow instanceof Finish) {
        continue; // ignore body value
      } else if (bodyFlow instanceof Continue bodyContinue) {
        if (bodyContinue.label() == null || bodyContinue.label().equals(labelName)) continue; // continue to next loop
        else return bodyFlow; // continue for an outer label
      } else if (bodyFlow instanceof Break bodyBreak) {
        if (bodyBreak.label() == null || bodyBreak.label().equals(labelName)) return new Finish(bodyBreak.value() != null ? bodyBreak.value() : Value.UNIT); // break out of this loop, with given value or unit
        else return bodyFlow; // break for an outer label
      } else {
        return bodyFlow; // some flow not covered by loop (for now, only return)
      }
    }
  }
  
  @Override
  public ExprEvalControlFlow visitWhileExpr(WhileExprContext ctx) {
    var label = ctx.label();
    var labelName = label != null ? label.ID().getText() : null;
    while (true) {
      var condFlow = ctx.cond.accept(this);
      if (!(condFlow instanceof Finish condFinish)) return condFlow; // note that break and continue don't work in the condition
      if (!(condFinish.value() instanceof BoolValue condValue)) throw new BadTypeException("while condition must be a bool");
      if (!condValue.value()) break;
      var bodyFlow = ctx.body.accept(this);
      if (bodyFlow instanceof Finish) {
        continue; // ignore body value
      } else if (bodyFlow instanceof Continue bodyContinue) {
        if (bodyContinue.label() == null || bodyContinue.label().equals(labelName)) continue; // continue to next loop
        else return bodyFlow; // continue for an outer label
      } else if (bodyFlow instanceof Break bodyBreak) {
        if (bodyBreak.label() == null || bodyBreak.label().equals(labelName)) {
          if (bodyBreak.value() == null || bodyBreak.value() instanceof UnitValue) break; // break out of loop
          else throw new BadTypeException("Cannot break out of a while with any value other than unit");
        } else {
          return bodyFlow; // break for an outer label
        }
      } else {
        return bodyFlow; // some flow not covered by while (for now, only return)
      }
    }
    return new Finish(Value.UNIT);
  }
  
  @Override
  public ExprEvalControlFlow visitJumpExpr(JumpExprContext ctx) {
    switch (ctx.jump.getText()) {
      case "return" -> {
        if (ctx.atLabel() != null) throw new IllegalStateException("return cannot target a label");
        var expr = ctx.expr();
        Value value = Value.UNIT;
        if (expr != null) {
          var flow = expr.accept(this);
          if (flow instanceof Finish finish) value = finish.value();
          else return flow;
        }
        return new Return(value);
      }
      case "break" -> {
        String label = null;
        var atLabel = ctx.atLabel();
        if (atLabel != null) label = atLabel.ID().getText();
        var expr = ctx.expr();
        Value value = Value.UNIT;
        if (expr != null) {
          var flow = expr.accept(this);
          if (flow instanceof Finish finish) value = finish.value();
          else return flow;
        }
        return new Break(label, value);
      }
      case "continue" -> {
        if (ctx.expr() != null) throw new IllegalStateException("continue cannot accept value");
        String label = null;
        var atLabel = ctx.atLabel();
        if (atLabel != null) label = atLabel.ID().getText();
        return new Continue(label);
      }
      default -> throw new IllegalStateException("Invalid jump expression");
    }
  }
  
  @Override
  public ExprEvalControlFlow visitArrayLiteral(ArrayLiteralContext ctx) {
    var items = ctx.arrayLiteralItem();
    var values = new ArrayList<Value>(items.size());
    for (var item : items) {
      var flow = item.expr().accept(this);
      if (!(flow instanceof Finish finish)) return flow;
      if (item.spread != null) {
        if (!(finish.value() instanceof ArrayValue arrayValue)) throw new BadTypeException("Only arrays can be spread into arrays");
        values.addAll(List.of(arrayValue.values()));
      } else {
        values.add(finish.value());
      }
    }
    return new Finish(Value.of(values.toArray(Value[]::new)));
  }
  
  @Override
  public ExprEvalControlFlow visitLiteral(LiteralContext ctx) {
    if (ctx.int_ != null) {
      return new Finish(Value.of(parseInt(ctx.int_.getText())));
    } else if (ctx.string != null) {
      var str = ctx.string.getText();
      return new Finish(Value.of(str.substring(1, str.length() - 1)));
    } else if (ctx.bool !=  null) {
      return new Finish(Value.of(switch (ctx.bool.getText()) {
        case "true" -> true;
        case "false" -> false;
        default -> throw new IllegalStateException("Invalid boolean literal");
      }));
    } else if (ctx.unit != null) {
      return new Finish(Value.UNIT);
    }
    else {
      throw new IllegalStateException("Invalid literal");
    }
  }
  
}
