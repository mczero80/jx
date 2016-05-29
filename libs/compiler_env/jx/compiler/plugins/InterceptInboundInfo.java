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
import jx.compiler.symbols.ProfileSTEntry;
import jx.compiler.symbols.UnresolvedJump;

public class InterceptInboundInfo implements CompilerPlugin {

    private final static boolean testMagic = false;
    private final static int  DOMAIN_MAGIC = 0xd0d0eeee;

    private ExecEnvironmentIA32 e;
    private CompilerOptionsInterface     opts;
    private BinaryCodeIA32      code;
    private RegManager          regs;

    public InterceptInboundInfo(ExecEnvironmentIA32 e) {
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

	this.code = code;
	this.regs = regs;

	if (opts.doVerbose("plugin")) Debug.out.println("Compiling jx.zero.InterceptInboundInfo call: "+
			   methodRefCPEntry.getClassName()+"::"+
			   methodRefCPEntry.getMemberName() + " " +  
			   methodRefCPEntry.getMemberTypeDesc()); 
	
	String methodName = methodRefCPEntry.getMemberName();
	String signature = methodRefCPEntry.getMemberTypeDesc();

	// void recordEvent(int nr);
	if (methodName.equals("getSourceDomain")) {

	  obj.translate(result);	  

	  /* DomainDesc *d = ((struct InterceptInboundInfo_s*)obj)->source; */
//	  code.movl(result.ref(),result); /* would be vtable */
	  code.movl(result.rdisp(4),result);

	  if (testMagic) {
	    code.cmpl(DOMAIN_MAGIC,result.ref());
	    code.jne(e.createExceptionCall(-7,bcPosition));
	  }

	  /* domainDesc2Obj() */
	  /* ObjectDesc *domain= (ObjectDesc*)(((u4_t*)d)-1); */
	  code.subl(4,result);
	  
	  return true;
	} else 	if (methodName.equals("getTargetDomain")) {
	  
	  obj.translate(result);

	  /* DomainDesc *d = ((struct InterceptInboundInfo_s*)obj)->target; */
	  code.movl(result.rdisp(8),result);

	  if (testMagic) {
	    code.cmpl(DOMAIN_MAGIC,result.ref());
	    code.jne(e.createExceptionCall(-7,bcPosition));
	  }

	  /* domainDesc2Obj() */
	  /* ObjectDesc *domain= (ObjectDesc*)(((u4_t*)d)-1); */
	  code.subl(4,result);

	  return true;
	} else 	if (methodName.equals("getMethod")) {
	  
	  obj.translate(result);

	  /* VMMethod *m = ((struct InterceptInboundInfo_s*)obj)->method; */
	  code.movl(result.rdisp(12),result);

	  return true;
/*	} else 	if (methodName.equals("getServiceObject")) {
	  
	  obj.translate(result);

	  // OjectDesc *o = ((struct InterceptInboundInfo_s*)obj)->obj; 
	  code.movl(result.rdisp(16),result);
	  code.test(result,result);
	  code.jne(e.createExceptionCall(-1,bcPosition));

	  return true;
*/	} else {
	    return false;
	}
    }
}
