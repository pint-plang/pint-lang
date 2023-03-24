package io.github.pint_lang.codegen;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public record JumpScope(LLVMBasicBlockRef continueBlock, LLVMBasicBlockRef breakBlock, LLVMValueRef phi, String name) {
  
  public JumpScope {
    if (breakBlock == null) throw new NullPointerException("breakBlock must not be null");
    if (name == null) throw new NullPointerException("name must not be null");
  }
  
}
