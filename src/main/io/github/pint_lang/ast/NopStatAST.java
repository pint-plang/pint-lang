package io.github.pint_lang.ast;

public record NopStatAST<T>(T data) implements StatAST<T> {
  @Override
  public <R> R accept(StatASTVisitor<T, R> visitor) {
    return visitor.visitNopStat(this);
  }
  
}
