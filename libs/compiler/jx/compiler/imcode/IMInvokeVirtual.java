package jx.compiler.imcode; 

import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;

// ***** IMInvokeVirtual *****

final public class IMInvokeVirtual extends IMInvoke {

    public IMInvokeVirtual(CodeContainer container,int bc,int bcpos,
			   MethodRefCPEntry cpEntry) {
	super(container,bc,bcpos,cpEntry);
    }

    public String toReadableString() {
	String retString = obj.toReadableString()+".";
	retString += cpEntry.getMemberName();
	if (stat_flag) {
	    BCMethod bcMethod = execEnv.getBCMethod(cpEntry);
	    if (doStaticMethodCalls && (bcMethod instanceof BCMethodWithCode) && !bcMethod.isOverrideable()) {
		retString += "@extra";
	    } else if (obj.isThisPointer()) {
		retString += "@this";
	    }
	}
	return retString += super.toReadableString();
    }

    // IMInvokeVirtual
    public void translate(Reg result) throws CompileException {
	if (doStaticMethodCalls) {
	    BCMethod bcMethod = execEnv.getBCMethod(cpEntry);
	    if ((bcMethod instanceof BCMethodWithCode)) {
		if (!bcMethod.isOverrideable()) {
		    stat.invoke_static();
		    execEnv.codeSpecialCall(this,cpEntry,obj,args,datatype,result,bcPosition);
		    return;
		}
	    }
	}
	stat.invoke_virtual();
	execEnv.codeVirtualCall(this,cpEntry,obj,args,datatype,result,bcPosition);
    }

    // IMInvokeVirtual Long
    public void translate(Reg64 result) throws CompileException {
	if (opts.isOption("long")) {
	    if (doStaticMethodCalls) {
		BCMethod bcMethod = execEnv.getBCMethod(cpEntry);
		if ((bcMethod instanceof BCMethodWithCode)) {
		    if (!bcMethod.isOverrideable()) {
			execEnv.codeSpecialCallLong(this,cpEntry,obj,args,datatype,result,bcPosition);
			return;
		    }
		}
	    }
	    execEnv.codeVirtualCallLong(this,cpEntry,obj,args,datatype,result,bcPosition);
	} else {
	    execEnv.codeThrow(this,-11,bcPosition);
	}
    } 
}
