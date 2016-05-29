package java.lang;

import jx.zero.*;

import java.io.PrintStream;

public class Throwable
{
    class StackFrame {
	String className, methodName;
	int bytecode, line;
	public String toString() {
	    return className + "." + methodName + ", bytecode "+bytecode + ", line "+line;
	}
    }

    private StackFrame[] backtrace;
    private String message;
    private static final boolean createStackTrace = false;
    public Throwable(String message) {
	this.message = message;
	if (createStackTrace) {
	    fillInStackTrace();
	    //printStackTrace(System.out);
	}
    }
    
    public Throwable() {
	this("");
    }
    
    public String toString() {
	return getClass().getName() + ": " + message;
    }
    
    public String getMessage() {
	return message;
    }
    
    private static final int MYOWN_STACK = 0;
    private  void fillInStackTrace() {
	CPUManager c = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
	backtrace = new StackFrame[c.getStackDepth()-MYOWN_STACK];
	for(int i=0; i<backtrace.length-MYOWN_STACK; i++) {
	    backtrace[i] = new StackFrame();
	    backtrace[i].className = c.getStackFrameClassName(i+MYOWN_STACK);
	    backtrace[i].methodName = c.getStackFrameMethodName(i+MYOWN_STACK);
	    backtrace[i].line = c.getStackFrameLine(i+MYOWN_STACK);
	    backtrace[i].bytecode = c.getStackFrameBytecode(i+MYOWN_STACK);
	    
	}
    }
    
    public void printStackTrace() {
	if (System.err!=null) {
	    printStackTrace(System.err);
	} else if (System.out!=null) {
	    printStackTrace(System.out);
	}
    }
    
    public void printStackTrace(PrintStream s) {	
	  if (this.message != null) s.println(this.message);
	  for (int i = 0; i < backtrace.length; i++)
	      s.println(i +" "+backtrace[i]);
    }
    public String getLocalizedMessage() {return null;}
}

