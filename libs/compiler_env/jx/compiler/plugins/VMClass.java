package jx.compiler.plugins;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.persistent.*;
import jx.compiler.plugins.*;
import jx.compiler.symbols.UnresolvedJump;

public class VMClass implements CompilerPlugin {

    ExecEnvironmentIA32 e;

    public VMClass(ExecEnvironmentIA32 e) {
	this.e = e;
    }

    public boolean code(IMNode node,
			RegManager regs,
			BinaryCodeIA32   code,
			ClassMemberCPEntry methodRefCPEntry,
			IMOperant        obj,
			IMOperant[]      args,
			int              datatype,
			Reg              result,
			int              bcPosition) throws CompileException {
	

	String methodName = methodRefCPEntry.getMemberName();

	regs.saveIntRegister();
	Reg objRef = regs.chooseIntRegister(result);
	obj.translate(objRef);
	regs.freeIntRegister(objRef);
	code.pushl(objRef);
	if (methodName.equals("getName")) {
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_GETCLASSNAME));
	} else if (methodName.equals("getInstanceSize")) {
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_GETINSTANCESIZE));
	} else if (methodName.equals("isPrimitive")) {
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_ISPRIMITIVE));
	} else {
	    throw new CompileException("VMClass: method not implemented");
	}
	code.addl(4,Reg.esp);

	if (methodName.equals("getName")) {
	    regs.allocIntRegister(result,BCBasicDatatype.REFERENCE);
	} else {
	    regs.allocIntRegister(result,BCBasicDatatype.INT);
	}
	if (result!=null && result.value!=0) {
	    code.movl(Reg.eax,result);
	}
	
	return true;
    }
    
}
