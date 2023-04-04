package io.github.pint_lang.codegen;

import io.github.pint_lang.gen.PintParser;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.bytedeco.llvm.global.LLVM.*;

public class GlobalLoader {
  
  private final ArrayList<Function> functions = new ArrayList<>();
  private final ArrayList<Variable> variables = new ArrayList<>();
  
  public void addFunction(LLVMValueRef function, HashMap<String, LLVMValueRef> params, PintParser.BlockExprContext body) {
    functions.add(new Function(function, params, body));
  }
  
  public void addVariable(LLVMValueRef variable, PintParser.ExprContext initializer) {
    variables.add(new Variable(variable, initializer));
  }
  
  public void codeGen(ExprCodeGenVisitor visitor) {
    var initFnType = LLVMFunctionType(LLVMVoidTypeInContext(visitor.context), (PointerPointer<?>) null, 0, 0);
    var initFn = LLVMAddFunction(visitor.module, "<ginit>", initFnType);
    for (var function : functions) {
      visitor.loaderContext.enterFunction(function.params());
      var entryBlock = LLVMAppendBasicBlockInContext(visitor.context, function.function, "entry");
      LLVMPositionBuilderAtEnd(visitor.builder, entryBlock);
      if ("main".equals(LLVMGetValueName(function.function).getString())) {
        LLVMBuildCall2(visitor.builder, initFnType, initFn, null, 0, "initcalltmp");
      }
      var value = function.body.accept(visitor);
      // TODO: bad if value is unit
      LLVMBuildRet(visitor.builder, value);
      visitor.loaderContext.exitFunction();
    }
    var initEntryBlock = LLVMAppendBasicBlockInContext(visitor.context, initFn, "entry");
    LLVMPositionBuilderAtEnd(visitor.builder, initEntryBlock);
    visitor.loaderContext.checkInitialized();
    for (var variable : variables) {
      LLVMSetInitializer(variable.variable, LLVMGetUndef(LLVMGlobalGetValueType(variable.variable)));
      var name = LLVMGetValueName(variable.variable).getString();
      var value = variable.initializer.accept(visitor);
      LLVMBuildStore(visitor.builder, value, variable.variable);
      visitor.loaderContext.initiailzedVars.add(name);
    }
    visitor.loaderContext.uncheckInitialized();
    LLVMBuildRetVoid(visitor.builder);
  }
  
  public record Function(LLVMValueRef function, HashMap<String, LLVMValueRef> params, PintParser.BlockExprContext body) {}
  
  public record Variable(LLVMValueRef variable, PintParser.ExprContext initializer) {}
  
  public static class Context {
    
    private boolean inFunction = false;
    private HashMap<String, LLVMValueRef> params = null;
    private boolean checkInitialized = false;
    private final HashSet<String> initiailzedVars = new HashSet<>();
    
    public Context() {}
    
    private void checkInitialized() {
      checkInitialized = true;
    }
    
    private void uncheckInitialized() {
      checkInitialized = false;
      initiailzedVars.clear();
    }
    
    public boolean isVariableInitialized(String name) {
      return !checkInitialized || initiailzedVars.contains(name);
    }
    
    public void initializeVariable(String name) {
      if (checkInitialized) initiailzedVars.add(name);
    }
    
    public LLVMValueRef getParam(String name) {
      return params.get(name);
    }
    
    public boolean isInFunction() {
      return inFunction;
    }
    
    private void enterFunction(HashMap<String, LLVMValueRef> params) {
      inFunction = true;
      this.params = params;
    }
    
    private void exitFunction() {
      inFunction = false;
      this.params = null;
    }
    
  }
  
}