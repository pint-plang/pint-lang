package io.github.pint_lang;

import io.github.pint_lang.eval.*;
import io.github.pint_lang.gen.PintLexer;
import io.github.pint_lang.gen.PintParser;
import io.github.pint_lang.eval.ExprEvalControlFlow.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

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
    file.accept(new PrintVisitor(System.err));
    System.out.println();
    var global = new GlobalScope();
    global.defineFunction("print", (exprEval, vargs) -> {
      for (var arg : vargs) System.out.println(arg.valueToString());
      return Value.UNIT;
    });
    var varNames = new HashSet<String>();
    var vars = new ArrayList<Definition.Variable>();
    var defVisitor = new DefEvalVisitor();
    for (var def : file.def()) {
      var definition = def.accept(defVisitor);
      if (definition instanceof Definition.Function function) {
        if (varNames.contains(function.name()) || !global.defineFunction(function.name(), function)) throw new BadDefinitionException("Duplicate global definition '" + function.name() + "'");
      } else if (definition instanceof Definition.Variable variable) {
        if (varNames.contains(variable.name()) || global.hasFunction(variable.name())) throw new BadDefinitionException("Duplicate global definition '" + variable.name() + "'");
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
        else if (vFlow instanceof Labeled vLabeled) throw new BadJumpException(vLabeled.label() != null ? "Missing label '" + vLabeled.label() + "' (did you make a typo?)" : "Unlabeled jump outside of any loop");
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