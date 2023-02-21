package io.github.pint_lang.eval;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;

public class DefEvalVisitor extends PintBaseVisitor<Definition> {
  
  @Override
  public Definition visitDef(DefContext ctx) {
    var funcDef = ctx.funcDef();
    if (funcDef != null) return funcDef.accept(this);
    return ctx.varDef().accept(this);
  }
  
  @Override
  public Definition visitFuncDef(FuncDefContext ctx) {
    var params = ctx.paramList().param();
    if (params.stream().distinct().count() != params.size()) throw new BadDefinitionException("Function with duplicate parameters");
    return new Definition.Function(ctx.ID().getText(), params.stream().map(param -> param.ID().getText()).toArray(String[]::new), ctx.blockExpr());
  }
  
  @Override
  public Definition visitVarDef(VarDefContext ctx) {
    return new Definition.Variable(ctx.ID().getText(), ctx.expr());
  }
  
}
