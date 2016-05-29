package jx.compiler.plugins;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.*;
import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.*;

import jx.compiler.persistent.*;
import jx.compiler.plugins.*;

public interface CompilerPlugin {
    
    public boolean code(IMNode node,
			RegManager regs,
			BinaryCodeIA32   code,
			ClassMemberCPEntry methodRefCPEntry,
			IMOperant        obj,
			IMOperant[]      args,
			int              datatype,
			Reg              result,
			int              bcPosition) throws CompileException;
}
