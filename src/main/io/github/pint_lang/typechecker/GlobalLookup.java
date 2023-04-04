package io.github.pint_lang.typechecker;

import io.github.pint_lang.ast.DefASTVisitor;
import io.github.pint_lang.ast.FuncDefAST;
import io.github.pint_lang.ast.VarDefAST;

import java.util.HashMap;
import java.util.List;

public class GlobalLookup {
  
  private final HashMap<String, Type> vars = new HashMap<>();
  private final HashMap<String, FunctionType> funcs = new HashMap<>();
  
  private FunctionType current = null;
  
  public Type getVariableType(String name) {
    return vars.get(name);
  }
  
  public FunctionType getFunctionType(String name) {
    return funcs.get(name);
  }
  
  public FunctionType getThisFunctionType() {
    return current;
  }
  
  public void setThisFunctionType(FunctionType current) {
    this.current = current;
  }
  
  public void addVariable(String name, Type type, ErrorLogger<?> logger) {
    if (funcs.containsKey(name)) {
      logger.error("Duplicate global '" + name + "'");
      return;
    }
    vars.put(name, type);
  }
  
  public void addFunction(String name, FunctionType type, ErrorLogger<?> logger) {
    if (vars.containsKey(name)) {
      logger.error("Duplicate global '" + name + "'");
      return;
    }
    funcs.put(name, type);
  }
  
  public record FunctionType(Type returnType, List<Param> params) {}
  
  public record Param(String name, Type type) {}
  
  public class BuildVisitor implements DefASTVisitor<Void, Void> {
    
    private final TypeEvalVisitor typeEval;
    private final ErrorLogger<Type> logger;
    
    public BuildVisitor(TypeEvalVisitor typeEval) {
      this.typeEval = typeEval;
      this.logger = typeEval.logger;
    }
    
    @Override
    public Void visitFuncDef(FuncDefAST<Void> ast) {
      var params = ast.params().stream().map(param -> new Param(param.name(), param.type().accept(typeEval).data())).toList();
      var returnType = ast.returnType().accept(typeEval).data();
      addFunction(ast.name(), new FunctionType(returnType, params), logger);
      return null;
    }
    
    @Override
    public Void visitVarDef(VarDefAST<Void> ast) {
      addVariable(ast.name(), ast.type().accept(typeEval).data(), logger);
      return null;
    }
    
  }
  
}
