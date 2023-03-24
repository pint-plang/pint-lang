package io.github.pint_lang.codegen;

import io.github.pint_lang.gen.PintBaseVisitor;
import io.github.pint_lang.gen.PintParser.*;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.HashMap;

import static org.bytedeco.llvm.global.LLVM.*;

public class DefCodeGenVisitor extends PintBaseVisitor<Void> {
  
  public final LLVMContextRef context;
  public final LLVMModuleRef module;
  public final GlobalLoader loader;
  
  public DefCodeGenVisitor(LLVMContextRef context, LLVMModuleRef module, GlobalLoader loader) {
    this.context = context;
    this.module = module;
    this.loader = loader;
  }
  
  @Override
  public Void visitFile(FileContext ctx) {
    for (var def : ctx.def()) def.accept(this);
    return null;
  }
  
  @Override
  public Void visitDef(DefContext ctx) {
    var funcDef = ctx.funcDef();
    if (funcDef != null) return funcDef.accept(this);
    return ctx.varDef().accept(this);
  }
  
  @Override
  public Void visitFuncDef(FuncDefContext ctx) {
    // TODO: bad types
    var paramList = ctx.paramList().param();
    var paramTypes = paramList.stream().map(ignored -> LLVMInt32TypeInContext(context)).toArray(LLVMTypeRef[]::new);
    var fnType = LLVMFunctionType(LLVMInt32TypeInContext(context), new PointerPointer<>(paramTypes), paramTypes.length, 0);
    var function = LLVMAddFunction(module, ctx.ID().getText(), fnType);
    var params = new HashMap<String, LLVMValueRef>(paramList.size());
    for (var i = 0; i < paramList.size(); i++) {
      var paramName = paramList.get(i).ID().getText();
      var param = LLVMGetParam(function, i);
      LLVMSetValueName(param, paramName);
      params.put(paramName, param);
    }
    loader.addFunction(function, params, ctx.blockExpr());
    return null;
  }
  
  @Override
  public Void visitVarDef(VarDefContext ctx) {
    // TODO: bad types
    var variable = LLVMAddGlobal(module, LLVMInt32TypeInContext(context), ctx.ID().getText());
    loader.addVariable(variable, ctx.expr());
    return null;
  }
  
}
