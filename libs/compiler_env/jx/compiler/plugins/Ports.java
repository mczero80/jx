package jx.compiler.plugins;

import jx.zero.Debug;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.execenv.*;
import jx.compiler.persistent.*;
import jx.compiler.plugins.*;
import jx.compiler.symbols.UnresolvedJump;

public class Ports implements CompilerPlugin {

    private boolean fastConstants=false;

    private ExecEnvironmentIA32      e;
    private CompilerOptionsInterface opts;
    private BinaryCodeIA32           code;
    private RegManager               regs;

    public Ports(ExecEnvironmentIA32 e) {
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

	if (!opts.isOption("ports")) return false;

	this.code = code;
	this.regs = regs;

	String methodName = methodRefCPEntry.getMemberName();
	String signature = methodRefCPEntry.getMemberTypeDesc();
	
	if (opts.doVerbose("plugin") || opts.doVerbose("ports")) {
	    Debug.out.println("Compiling jx.zero.Ports call: "+
			       methodRefCPEntry.getClassName()+"."+
			       methodName + " " +  signature);
	}
	
	boolean retvalue = false;

	if (fastConstants && methodName.startsWith("outb") && args[0].isConstant()) {
	    Reg al = regs.getIntRegister(Reg.eax);
	    args[1].translate(al);

	    code.outb(al, (byte)((IMConstant)args[0]).getIntValue());

	    regs.freeIntRegister(al);
	    retvalue = true;
	} else if (false && methodName.startsWith("outb")) {
	    Reg al = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    args[1].translate(al);
	    regs.readIntRegister(dx);

	    code.outb(al,dx);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(al);
	    retvalue = true;
	} else if (fastConstants && methodName.startsWith("inb") && args[0].isConstant()) {
	    Reg al = regs.getIntRegister(Reg.eax);
	    regs.allocIntRegister(al,-1);

	    code.inb((byte)((IMConstant)args[0]).getIntValue(),al);

	    regs.freeIntRegister(al);
	    retvalue = true; 
	} else if (methodName.startsWith("inb")) {
	    Reg al = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    regs.allocIntRegister(al,-1);

	    code.inb(dx,al);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(al);
	    retvalue = true;
	} else if (fastConstants && methodName.startsWith("outw") && args[0].isConstant()) {
	    Reg ax = regs.getIntRegister(Reg.eax);
	    args[1].translate(ax);

	    code.outb(ax, (byte) ((IMConstant)args[0]).getIntValue());

	    regs.freeIntRegister(ax);
	    retvalue = true;
	} else if (false && methodName.startsWith("outw")) {
	    Reg ax = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    args[1].translate(ax);
	    regs.readIntRegister(dx);

	    code.outw(ax,dx);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(ax);
	    retvalue = true;
	} else if (fastConstants && methodName.startsWith("inw") && args[0].isConstant()) {
	    Reg ax = regs.getIntRegister(Reg.eax);
	    regs.allocIntRegister(ax,-1);

	    code.inb((byte)((IMConstant)args[0]).getIntValue(),ax);

	    regs.freeIntRegister(ax);
	    retvalue = true;
	} else if (methodName.startsWith("inw")) {
	    Reg ax = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    regs.allocIntRegister(ax,-1);

	    code.inw(dx,ax);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(ax);
	    retvalue = true; 
	} else if (false && methodName.startsWith("outl")) {
	    Reg eax = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    args[1].translate(eax);
	    regs.readIntRegister(dx);

	    code.outl(eax,dx);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(eax);
	    retvalue = true;
	} else if (fastConstants && methodName.startsWith("inl") && args[0].isConstant()) {
	    Reg eax = regs.getIntRegister(Reg.eax);
	    regs.allocIntRegister(eax,-1);

	    code.inb((byte)((IMConstant)args[0]).getIntValue(),eax);
	    
	    regs.freeIntRegister(eax);
	    retvalue = true;
	} else if (methodName.startsWith("inl")) {
	    Reg eax = regs.getIntRegister(Reg.eax);
	    Reg dx = regs.getIntRegister(Reg.edx);
	    args[0].translate(dx);
	    regs.allocIntRegister(eax,-1);

	    code.inl(dx,eax);

	    regs.freeIntRegister(dx);
	    regs.freeIntRegister(eax);
	    retvalue = true; 
	}

	if (retvalue) {
	    if (methodName.startsWith("in")) {
		regs.allocIntRegister(result,Reg.eax,-1);
		if (!result.equals(Reg.eax)) code.movl(Reg.eax,result);
	    }		
	    if (methodName.endsWith("_p")) {
		code.outb(Reg.eax,(byte)0x80);
	    }
	    return true;
	} else {
	    if (opts.doVerbose("ports")) Debug.out.println("method has no plugin implementation. call normal method for "+methodName);
	    return false;
	}
    }
}
