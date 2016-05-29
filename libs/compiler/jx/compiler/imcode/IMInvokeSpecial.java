package jx.compiler.imcode; 

import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*; 
import jx.classfile.*;
import jx.zero.Debug; 
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;

// ***** IMInvokeSpecial *****

final public class IMInvokeSpecial extends IMInvoke {

    public IMInvokeSpecial(CodeContainer container,int bc,int bcpos,
			   MethodRefCPEntry cpEntry) {
	super(container,bc,bcpos,cpEntry);
    }

    public String toReadableString() {
	String retString = obj.toReadableString()+".";
	retString += cpEntry.getMemberName();
	if (stat_flag) {retString += "@special";}
	return retString += super.toReadableString();

    }

    // IMInvokeSpecial
    public void translate(Reg result) throws CompileException {
	stat.invoke_static();
	execEnv.codeSpecialCall(this,cpEntry,obj,args,datatype,result,bcPosition);
    }

    public void translate(Reg64 result) throws CompileException {
	stat.invoke_static();
	if (opts.isOption("long")) {
	    execEnv.codeSpecialCallLong(this,cpEntry,obj,args,datatype,result,bcPosition);
	} else {
	    execEnv.codeThrow(this,-11,bcPosition);
	}
    } 
}
