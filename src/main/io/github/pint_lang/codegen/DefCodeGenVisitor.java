package io.github.pint_lang.codegen;

import io.github.pint_lang.ast.DefASTVisitor;
import io.github.pint_lang.ast.DefsAST;
import io.github.pint_lang.ast.FuncDefAST;
import io.github.pint_lang.ast.VarDefAST;
import io.github.pint_lang.typechecker.Type;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.HashMap;

import static org.bytedeco.llvm.global.LLVM.*;

public class DefCodeGenVisitor implements DefASTVisitor<Type, Void> {
  
  public final LLVMContextRef context;
  public final LLVMModuleRef module;
  public final GlobalLoader loader;
  
  public DefCodeGenVisitor(LLVMContextRef context, LLVMModuleRef module, GlobalLoader loader) {
    this.context = context;
    this.module = module;
    this.loader = loader;
  }
  
  public void visitDefs(DefsAST<Type> ast) {
    for (var def : ast.defs()) def.accept(this);
  }
  
  @Override
  public Void visitFuncDef(FuncDefAST<Type> ast) {
    // TODO: bad types
    var paramList = ast.params();
    var paramTypes = paramList.stream().map(ignored -> LLVMInt32TypeInContext(context)).toArray(LLVMTypeRef[]::new);
    var fnType = LLVMFunctionType(LLVMInt32TypeInContext(context), new PointerPointer<>(paramTypes), paramTypes.length, 0);
    var function = LLVMAddFunction(module, ast.name(), fnType);
    var params = new HashMap<String, LLVMValueRef>(paramList.size());
    for (var i = 0; i < paramList.size(); i++) {
      var paramName = paramList.get(i).name();
      var param = LLVMGetParam(function, i);
      LLVMSetValueName(param, paramName);
      params.put(paramName, param);
    }
    loader.addFunction(function, params, ast.body());
    return null;
  }
  
  @Override
  public Void visitVarDef(VarDefAST<Type> ast) {
    // TODO: bad types
    var variable = LLVMAddGlobal(module, LLVMInt32TypeInContext(context), ast.name());
    loader.addVariable(variable, ast.value());
    return null;
  }
  
}
