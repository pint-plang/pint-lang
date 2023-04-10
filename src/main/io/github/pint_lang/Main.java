package io.github.pint_lang;

import io.github.pint_lang.ast.ASTConversionVisitor;
import io.github.pint_lang.eval.*;
import io.github.pint_lang.gen.PintLexer;
import io.github.pint_lang.gen.PintParser;
import io.github.pint_lang.eval.ExprEvalControlFlow.*;

import io.github.pint_lang.typechecker.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class Main {
  
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("error: missing a source file");
      System.exit(-1);
    }
    if (args.length > 1) {
      System.err.println("warning: ignoring extra command-line arguments");
    }
    var chars = CharStreams.fromPath(Path.of(args[0]));
    var lexer = new PintLexer(chars);
    var tokens = new CommonTokenStream(lexer);
    var parser = new PintParser(tokens);
    var file = parser.file();
    {
      file.accept(new PrintVisitor(System.err));
    }
    System.out.println("-----------------------------------");
    {
      file.accept(new ASTConversionVisitor()).accept(new ASTPrintVisitor(System.err));
    }
    System.out.println("-----------------------------------");
    boolean errors;
    {
      var logger = new ErrorLogger<>(Type.ERROR);
      var globals = new GlobalLookup();
      addFunctions(globals, logger);
      var defs = new ASTConversionVisitor().visitFile(file);
      new TypecheckVisitor(logger, globals).visitDefs(defs);
      errors = logger.dumpErrors(System.err);
      if (!errors) System.out.println("No type errors detected");
    }
    System.out.println();
    if (!errors) {
      var global = new GlobalScope();
      global.defineFunction("prints", (exprEval, vargs) -> {
        if (!(vargs.get(0) instanceof StringValue arg)) throw new BadExpressionException("Expected a string");
        System.out.print(arg.value());
        return Value.UNIT;
      });
      global.defineFunction("printi", (exprEval, vargs) -> {
        System.out.print(vargs.get(0).valueToString());
        return Value.UNIT;
      });
      global.defineFunction("printsln", (exprEval, vargs) -> {
        if (!(vargs.get(0) instanceof StringValue arg)) throw new BadExpressionException("Expected a string");
        System.out.println(arg.value());
        return Value.UNIT;
      });
      global.defineFunction("printiln", (exprEval, vargs) -> {
        System.out.println(vargs.get(0).valueToString());
        return Value.UNIT;
      });
      global.defineFunction("println", (exprEval, vargs) -> {
        System.out.println();
        return Value.UNIT;
      });
      var in = new Scanner(System.in);
      global.defineFunction("reads", (exprEval, vargs) -> {
        var line = in.nextLine();
        return Value.of(line);
      });
      global.defineFunction("readi", (exprEval, vargs) -> {
        try {
          var i = parseInt(in.nextLine());
          return Value.of(i);
        } catch (NumberFormatException e) {
          System.err.println("IO error: expected an int");
          throw exit(-1);
        }
      });
      global.defineFunction("asks", (exprEval, vargs) -> {
        if (!(vargs.get(0) instanceof StringValue arg)) throw new BadExpressionException("Expected a string");
        System.out.print(arg.value());
        var line = in.nextLine();
        return Value.of(line);
      });
      global.defineFunction("aski", (exprEval, vargs) -> {
        if (!(vargs.get(0) instanceof StringValue arg)) throw new BadExpressionException("Expected a string");
        System.out.print(arg.value());
        try {
          var i = parseInt(in.nextLine());
          return Value.of(i);
        } catch (NumberFormatException e) {
          System.err.println("IO error: expected an int");
          throw exit(-1);
        }
      });
      global.defineFunction("exit", (exprEval, vargs) -> {
        if (!(vargs.get(0) instanceof IntValue arg)) throw new BadExpressionException("Expected an int");
        System.err.println("Exited with code " + arg.value());
        throw exit(arg.value());
      });
      var varNames = new HashSet<String>();
      var vars = new ArrayList<Definition.Variable>();
      var defVisitor = new DefEvalVisitor();
      for (var def : file.def()) {
        var definition = def.accept(defVisitor);
        if (definition instanceof Definition.Function function) {
          if (varNames.contains(function.name()) || !global.defineFunction(function.name(), function))
            throw new BadDefinitionException("Duplicate global definition '" + function.name() + "'");
        } else if (definition instanceof Definition.Variable variable) {
          if (varNames.contains(variable.name()) || global.hasFunction(variable.name()))
            throw new BadDefinitionException("Duplicate global definition '" + variable.name() + "'");
          varNames.add(variable.name());
          vars.add(variable);
        } else {
          throw new IllegalStateException("Invalid definition");
        }
      }
      var main = global.getFunction("main").orElseThrow(() -> new BadDefinitionException("Missing a main method"));
      var exprVisitor = new ExprEvalVisitor(new ScopeStack(global));
      for (var v : vars) {
        var vFlow = v.valueCst().accept(exprVisitor);
        if (!(vFlow instanceof Finish vFinish)) {
          if (vFlow instanceof Return) throw new BadJumpException("Returned from variable definition");
          else if (vFlow instanceof Labeled vLabeled)
            throw new BadJumpException(vLabeled.label() != null ? "Missing label '" + vLabeled.label() + "' (did you make a typo?)" : "Unlabeled jump outside of any loop");
          else throw new IllegalStateException("Invalid variable definition exit");
        }
        global.defineVariable(v.name(), vFinish.value());
      }
      var mainValue = main.call(exprVisitor);
      if (!(mainValue instanceof UnitValue)) throw new BadTypeException("main() must return unit");
      System.out.println();
      System.out.println("Successfully returned from main()");
    }
  }
  
  private static void addFunctions(GlobalLookup globals, ErrorLogger<?> logger) {
    globals.addFunction("prints", new GlobalLookup.FunctionType(Type.UNIT, List.of(new GlobalLookup.Param("s", Type.STRING))), logger);
    globals.addFunction("printi", new GlobalLookup.FunctionType(Type.UNIT, List.of(new GlobalLookup.Param("i", Type.INT))), logger);
    globals.addFunction("printsln", new GlobalLookup.FunctionType(Type.UNIT, List.of(new GlobalLookup.Param("s", Type.STRING))), logger);
    globals.addFunction("printiln", new GlobalLookup.FunctionType(Type.UNIT, List.of(new GlobalLookup.Param("i", Type.INT))), logger);
    globals.addFunction("println", new GlobalLookup.FunctionType(Type.UNIT, List.of()), logger);
    globals.addFunction("reads", new GlobalLookup.FunctionType(Type.STRING, List.of()), logger);
    globals.addFunction("readi", new GlobalLookup.FunctionType(Type.INT, List.of()), logger);
    globals.addFunction("asks", new GlobalLookup.FunctionType(Type.STRING, List.of(new GlobalLookup.Param("s", Type.STRING))), logger);
    globals.addFunction("aski", new GlobalLookup.FunctionType(Type.INT, List.of(new GlobalLookup.Param("s", Type.STRING))), logger);
    globals.addFunction("exit", new GlobalLookup.FunctionType(Type.NEVER, List.of(new GlobalLookup.Param("code", Type.INT))), logger);
  }
  
  // A hack to allow control flow analysis to understand that this exits from a function
  public static RuntimeException exit(int code) {
    System.exit(code);
    throw new IllegalStateException("Good job, you broke Java!");
  }
  
}