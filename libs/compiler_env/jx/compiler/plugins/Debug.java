package jx.compiler.plugins;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.persistent.*;
import jx.compiler.plugins.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.UnresolvedJump;

public class Debug implements CompilerPlugin {

    private ExecEnvironmentIA32 e;
    private CompilerOptionsInterface     opts;

    public Debug(ExecEnvironmentIA32 e) {
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

	/*
	 *  Use Compiler flag "-debug" to set debug
	 */

	if (!opts.doDebug()) {
	    if ((methodName.equals("verbose"))) {
		/* remove verbose messages*/
		return true;
	    }
	}

	/*
	 * Use Compiler flag "-noDbg" to suppress the following
	 */
	if (opts.doRemoveDebug()) {
	    if ((methodName.equals("assert"))) {
		/* remove assertions */
		return true;
	    }
	    if ((methodName.equals("check"))) {
		/* remove check */
		return true;
	    }
	    if ((methodName.equals("message"))) {
		/* remove message */
		return true;
	    }
	}

	return false;
    }
}
