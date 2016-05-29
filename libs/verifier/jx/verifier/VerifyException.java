package jx.verifier;

import jx.verifier.bytecode.*;
import jx.verifier.typecheck.*;

public class VerifyException extends Exception {
    public String className;
    public String methodName;
    public int bcAddr;
    public ByteCode bCode;
    public ByteCode srBCode; //bc executed in subroutine
    public BCLinkList code;
    public JVMState state;
    //    public boolean foundNull; //indicates that exception was raised because a null-pointer was found were it would raise an exception. 
    private StringBuffer message;
    
    public VerifyException(String message) {
	this.message = new StringBuffer(message);
	className = null;
	methodName = null;
	bcAddr = -1;
	bCode = null;
	code = null;
	//	foundNull = false;
    }
    public VerifyException() {
	this("");
    }
    
    
    
    public String toString() {
	String ret = "Verification failed";
	if (className != null) 
	    ret += "\n  in Class '" + className+"'";
	if (methodName !=null)
	    ret += "\n  in method '"+methodName+"'";
	if (bcAddr != -1) 
	    ret += "\n  at address " + Integer.toHexString(bcAddr);
	if (bCode != null && srBCode == null)
	    ret += "\n  ByteCode: " + bCode.toString();
	if (srBCode != null)
	    ret += "\n  ByteCode: " + srBCode.toString();
	if (message.length() > 0 ) 
	    ret += "\n" + message.toString();
	if (state != null) 
	    ret +="\nState:\n" + state.toString();
	if (code != null)
	    ret +="\nCode:\n" + code.toString() +
		"\n----------------------------------------\n";
	return ret;
    }
    public String getMessage() {
	return message.toString();
    }
    public void append(String m) {
	message.append(m);
    }
}
