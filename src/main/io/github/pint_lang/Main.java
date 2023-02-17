package io.github.pint_lang;

import io.github.pint_lang.gen.PintLexer;
import io.github.pint_lang.gen.PintParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Path;

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
    parser.file().accept(new PrintVisitor(System.out));
  }
  
}