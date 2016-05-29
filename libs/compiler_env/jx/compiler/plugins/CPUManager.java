package jx.compiler.plugins;

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

public class CPUManager implements CompilerPlugin {

  private final static int EVENT_LOG = 4;
  private final static int EVENT_RECORD_SIZE = 20;
  private final static int EVENT_TIME_FIELD  = 12;

    private ExecEnvironmentIA32 e;
    private CompilerOptionsInterface     opts;
    private BinaryCodeIA32      code;
    private RegManager          regs;

    public CPUManager(ExecEnvironmentIA32 e) {
	this.e    = e;
	this.opts = e.getCompilerOptions();
    }

    public static void codeEvent(ExecEnvironmentIA32 e,
				 RegManager regs,
				 BinaryCodeIA32 code,
				 ProfileSTEntry event_id,
				 int            bcPosition) throws CompileException {
	if (e.getCompilerOptions().isOption("newevents")) {
	    	  Reg event = regs.getIntRegister(Reg.ebx);
                  regs.allocIntRegister(event,-1); 
                  code.movl(event_id,event);
		  regs.freeIntRegister(event);
                  regs.saveIntRegister();
	          code.call(new VMSupportSTEntry("vm_event"));
	} else {
		// edx:eax ist used by rdtsc()
	Reg rdtsc_edx   = regs.getIntRegister(Reg.edx);
	Reg rdtsc_eax   = regs.getIntRegister(Reg.eax);
	
	Reg event_count = regs.getIntRegister(Reg.esi);
	Reg counter     = regs.getIntRegister(Reg.ecx);
	Reg event_data  = regs.getIntRegister(Reg.edi);
	
	regs.allocIntRegister(event_count,-1);
	regs.allocIntRegister(counter,-1);
	
	code.movl(new ProfileSTEntry(ProfileSTEntry.EVENT_COUNTER),counter);
	code.movl(counter.ref(),event_count);
	
	UnresolvedJump jumpForward  = new UnresolvedJump();
	code.cmpl(new ProfileSTEntry(ProfileSTEntry.EVENT_MAX), event_count);
	//code.je(e.createExceptionCall(-4,bcPosition)); 
	code.je(jumpForward);
  	
	regs.allocIntRegister(event_data,-1);
	
	//regs.readIntRegister(event_count);
	//regs.readIntRegister(counter);
	
	code.movl(new ProfileSTEntry(ProfileSTEntry.EVENT_DATA),event_data);
	//code.lea(event_data.rdisp(0,event_count,EVENT_LOG),event_data);
	//code.shll(EVENT_LOG,event_count);
        code.imull(EVENT_RECORD_SIZE, event_count);
	code.addl(event_count,event_data);
	code.movl(event_id,event_data.ref());
	
	regs.allocIntRegister(rdtsc_eax,-1);
	regs.allocIntRegister(rdtsc_edx,-1);
	
	code.rdtsc();
	
	code.movl(rdtsc_eax,event_data.rdisp(EVENT_TIME_FIELD));
	code.movl(rdtsc_edx,event_data.rdisp(EVENT_TIME_FIELD+4));
	
	regs.freeIntRegister(rdtsc_eax);
	regs.freeIntRegister(rdtsc_edx);
	regs.freeIntRegister(event_data);
	
	code.shrl(EVENT_LOG,event_count);
	code.incl(event_count);
	code.movl(event_count,counter.ref());
	
	regs.freeIntRegister(event_count);
	regs.freeIntRegister(counter);

	code.addJumpTarget(jumpForward);
        }
  }

    public static void codeEvent(ExecEnvironmentIA32 e,
				 RegManager regs,
				 BinaryCodeIA32   code,
				 IMOperant        EventID,
				 int              bcPosition) throws CompileException {


    
 
      
      // edx:eax ist used by rdtsc()
      Reg rdtsc_edx   = regs.getIntRegister(Reg.edx);
      Reg rdtsc_eax   = regs.getIntRegister(Reg.eax);
      
      Reg event_count = regs.getIntRegister(Reg.esi);
      Reg counter     = regs.getIntRegister(Reg.ecx);
      Reg event_type  = regs.getIntRegister(Reg.eax);
      Reg event_data  = regs.getIntRegister(Reg.edi);
      
      regs.allocIntRegister(event_count,-1);
      regs.allocIntRegister(counter,-1);
      
      code.movl(new ProfileSTEntry(ProfileSTEntry.EVENT_COUNTER),counter);
      code.movl(counter.ref(),event_count);
      
      UnresolvedJump jumpForward  = new UnresolvedJump();
      code.cmpl(new ProfileSTEntry(ProfileSTEntry.EVENT_MAX), event_count);
      //code.je(e.createExceptionCall(-4,bcPosition)); 
      code.je(jumpForward);
      
      EventID.translate(event_type);
      
      regs.allocIntRegister(event_data,-1);
      
      regs.readIntRegister(event_count);
      regs.readIntRegister(counter);
      
      code.movl(new ProfileSTEntry(ProfileSTEntry.EVENT_DATA),event_data);
      //code.lea(event_data.rdisp(0,event_count,EVENT_LOG),event_data);
      //code.shll(EVENT_LOG,event_count);
      code.imull(EVENT_RECORD_SIZE, event_count);
      code.addl(event_count,event_data);
      code.movl(event_type,event_data.ref());
      
      regs.freeIntRegister(event_type);
      
      regs.allocIntRegister(rdtsc_eax,-1);
      regs.allocIntRegister(rdtsc_edx,-1);
      
      code.rdtsc();
      
      code.movl(rdtsc_eax,event_data.rdisp(EVENT_TIME_FIELD));
      code.movl(rdtsc_edx,event_data.rdisp(EVENT_TIME_FIELD+4));
      
      regs.freeIntRegister(rdtsc_eax);
      regs.freeIntRegister(rdtsc_edx);
      regs.freeIntRegister(event_data);

      code.shrl(EVENT_LOG,event_count);
      code.incl(event_count);
      code.movl(event_count,counter.ref());
      
      regs.freeIntRegister(event_count);
      regs.freeIntRegister(counter);

      code.addJumpTarget(jumpForward);
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

	if (opts.doVerbose("plugin")) java.lang.System.err.println("Compiling jx.zero.CPUManager call: "+
			   methodRefCPEntry.getClassName()+"::"+
			   methodRefCPEntry.getMemberName() + " " +  
			   methodRefCPEntry.getMemberTypeDesc()); 
	
	String methodName = methodRefCPEntry.getMemberName();
	String signature = methodRefCPEntry.getMemberTypeDesc();
	
	// void recordEvent(int nr);
	if (methodName.equals("recordEvent")) {
	    if (opts.doVerbose("plugin")) java.lang.System.err.print("!!! recordEvent ");
	    
	    if (opts.doEventLoging()) {
		if (opts.doVerbose("plugin")) java.lang.System.err.println("active !!!");
		if (opts.isOption("newevents")) {
	    	  Reg event = regs.getIntRegister(Reg.ebx);
	          args[0].translate(event);
		  regs.freeIntRegister(event);
                  regs.saveIntRegister();
	          code.call(new VMSupportSTEntry("vm_event"));
                } else {
		  codeEvent(e,regs,code,args[0],bcPosition);
                }
	    } else {
		if (opts.doVerbose("plugin")) java.lang.System.err.println("deaktive !!!");
	    }

	    return true;
	}

	return false;
    }
}
