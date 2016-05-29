package jx.compiler.plugins;

import jx.zero.Debug;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.persistent.*;
import jx.compiler.plugins.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.UnresolvedJump;

public class VMSupport implements CompilerPlugin {

    private ExecEnvironmentIA32 e;
    private CompilerOptionsInterface opts;

    public VMSupport(ExecEnvironmentIA32 e) {
	this.e    = e;
	this.opts = e.getCompilerOptions();
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
	String signature = methodRefCPEntry.getMemberTypeDesc();

	if (opts.doVerbose("plugin")) {
	    Debug.out.println("Compiling jx.zero.VMSupport call: "+
			      methodRefCPEntry.getClassName()+"."+
			      methodName + " " + signature); 
	}
	
	if (false && methodName.equals("swapInt")) {

	} else if (methodName.equals("arraycopy_right")) {

	    MethodStackFrame frame = e.getMethodStackFrame();
	    int offset = e.codeStaticPushArgs(args);
	    
	    code.startBC(bcPosition);
	    
	    regs.saveIntRegister();

	    int ip=code.getCurrentIP();
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_ARRAYCOPY_RIGHT));
	    
	    node.addDebugInfo(frame.stackMapToString(node));
	    e.codeStackMap(node,ip);
	    
	    regs.clearActives();
	    e.codeStackCleanup(offset,result,node.getDatatype());
	    
	    code.endBC();
	    return true;
	}

	return false;
    }
}
