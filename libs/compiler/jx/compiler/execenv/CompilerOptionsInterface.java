/**
 * parser and container class for compiler options
 *
 * Autor: Chr. Wawersich
 */

package jx.compiler.execenv;

import java.util.Vector;

public interface CompilerOptionsInterface {

  public boolean doFastMemoryAccess();
  public boolean doMemoryRangeChecks();
  public boolean doExceptions();
  public boolean doClearStack();
  public boolean doZeroDivChecks();
  public boolean doNullChecks();
  public boolean doBoundsChecks();
  public boolean doStackSizeCheck(boolean isLeafMethod);
  public boolean doVerbose();
  public boolean doVerbose(String kind);
  public boolean doPrintStackTrace();
  public boolean doParanoidChecks();
  public boolean doMagicChecks();
  public boolean doTrace();
  public boolean doDebug();
  public boolean doRemoveDebug();
  public boolean doEventLoging();
  //public void printVerbose(String txt);

  public boolean isOption(String kind);

  public boolean doOptimize();
  public boolean doAlignCode();
  public boolean doFastStatics();
  public boolean doFastCheckCast();
  public boolean doFastThisPtr();

  public boolean doUsePackedArrays();
  public boolean doOptExecPath();

  public boolean doInlining(BCClass bcClass,BCMethod bcMethod);
  public boolean doInlining(String methodName);

  public boolean forceInline(BCMethod bcMethod);

  public boolean doExtraFalting();

  public boolean doProfile(BCClass bcClass, BCMethod bcMethod);
  public boolean doLogMethod(BCClass bcClass, BCMethod bcMethod);

  public void setUseNewCompiler(boolean flag);
  public boolean doNewCompiler();
  public boolean doNewCompiler(BCClass bcClass,BCMethod bcMethod);

  public boolean revocationCheckUsingCLI();
  public boolean revocationCheckUsingSpinLock();

  //public boolean doPrintIMCode();
  //public boolean doProfileNoIRQ();
  //public void debug(String txt);
  //public boolean doDebug();
  //public void setDebug(boolean flag);
  //public boolean doDebug(String flag);
}
