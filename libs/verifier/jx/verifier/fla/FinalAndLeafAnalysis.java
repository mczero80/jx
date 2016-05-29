package jx.verifier.fla;

import jx.verifier.*;
import jx.verifier.bytecode.*;
import jx.classfile.*;
import jx.classfile.constantpool.*;

public class FinalAndLeafAnalysis {


    static public void  verifyMethod(MethodSource method, 
				      String className, 
				      ConstantPool cPool,
				      ClassTree classTree) throws VerifyException {
	FLAResult result = new  FLAResult();
	if (classTree.findClassTreeElement(className).isSystemFinalMethod(method)) {
	    result.systemFinal = true;
	} else {
	    result.systemFinal = false;
	}

	//check is method is leaf, i.e. if it does not call any other methods.
	BCLinkList code = new BCLinkList(method, cPool);

	for (ByteCode actBc = code.getFirst(); 
	     actBc != null; 
	     actBc = actBc.next) {
	    if (actBc.getOpCode() == ByteCode.INVOKEVIRTUAL ||
		actBc.getOpCode() == ByteCode.INVOKESPECIAL ||
		actBc.getOpCode() == ByteCode.INVOKESTATIC ||
		actBc.getOpCode() == ByteCode.INVOKEINTERFACE){
		result.leaf = true;
		break;
	    }
	}
	method.setVerifyResult(result);

	if (result.isLeaf()) {
	    Verifier.stdPrintln(1, "Method " + 
				className +"." +
				method.getMethodName() + " is leaf.");
	}
	if (result.isSystemFinal()) {
	    Verifier.stdPrintln(1, "Method " + 
				className + "." +
				method.getMethodName() + " is system final.");
	}
    }
}
