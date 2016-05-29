package jx.compiler.plugins;

import jx.compiler.*;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.execenv.*;
import jx.compiler.persistent.*;
import jx.compiler.plugins.*;
import jx.compiler.symbols.UnresolvedJump;

public class Memory implements CompilerPlugin {

    public final static int ADDR_OFFSET  = 8;
    public final static int SIZE_OFFSET  = 4;
    
    public final static int DZ_OFFSET    = 12;  
    public final static  int VALID_OFFSET = 4;

    private final static boolean fastConstants=true;

    private ExecEnvironmentIA32      e;
    private CompilerOptionsInterface opts;
    private BinaryCodeIA32           code;
    private RegManager               regs;

    public Memory(ExecEnvironmentIA32 e) {
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

	if (!(opts.doFastMemoryAccess() || opts.isOption("fmc"))) return false;

	this.code = code;
	this.regs = regs;
	
	if (opts.doVerbose("memobj")) {
	    java.lang.System.err.println("Compiling jx.zero.Memory call: "+
			       methodRefCPEntry.getClassName()+"::"+
			       methodRefCPEntry.getMemberName() + " " +  
			       methodRefCPEntry.getMemberTypeDesc()); 
	}
	
	String methodName = methodRefCPEntry.getMemberName();
	String signature = methodRefCPEntry.getMemberTypeDesc();
	 if (opts.isOption("fmc") &&
	     opts.doMemoryRangeChecks() &&
	     (
	      methodName.equals("get8") ||
	      methodName.equals("get32") ||
	      methodName.equals("getLittleEndian32")
	     )) {

	    if (opts.doVerbose("memobj") || opts.doVerbose("fmc")) { 
              java.lang.System.err.println("use fast memory call for "+methodName);
	    }

	    Reg memory = regs.getIntRegister(Reg.eax);
	    Reg index  = regs.getIntRegister(Reg.esi);
	    Reg tmp    = regs.getIntRegister(Reg.edi);

	    obj.translate(memory);
	    args[0].translate(index);

	    regs.allocIntRegister(tmp,BCBasicDatatype.INT);
	    regs.readIntRegister(memory);
	    regs.readIntRegister(index);

	    code.call(new VMSupportSTEntry("vm_"+methodName));
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(tmp);

	    regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.INT);
	    if (!result.equals(Reg.eax)) {
		code.movl(Reg.eax,result);
	    }
	    return true;
	} else if (fastConstants && methodName.equals("get32") && args[0].isConstant()) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,result);
	    codeLock(memory,lock,bcPosition);

	    int index = ((IMConstant)args[0]).getIntValue();
	    codeReferenceAndConstantRangeCheck(obj,memory,index,4,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    if (index!=0) code.addl(index*4,memory);
	    code.movl(memory.ref(),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);

	    return true;
	} else if (methodName.equals("get32")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,result);
	    args[0].translate(index);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,4,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movl(memory.rdisp(0,index,4),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);

	    return true;
	} else if (methodName.equals("setLittleEndian32")) {

	    Reg memory = regs.chooseIntRegister();
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory);
	    args[0].translate(index);

	    Reg value = regs.chooseIntRegister(index,memory);
	    args[1].translate(value);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,value);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndBorderedRangeCheck(obj,memory,index,4,bcPosition);

	    // this is important !!
	    regs.readIntRegister(value);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movl(value,memory.rdisp(0,index,1));

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(value);

	    return true;
	} else if (methodName.equals("set8")) {

	    Reg value = regs.getIntRegister(Reg.eax);

	    Reg memory = regs.chooseIntRegister(value);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,value);
	    args[0].translate(index);

	    //Reg value = regs.chooseIntRegister(index,memory);
	    args[1].translate(value);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,value);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,1,bcPosition);

	    regs.readIntRegister(memory);
	    regs.readIntRegister(index);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.addl(index,memory);

	    regs.readIntRegister(value);
	    code.movb(value,memory.ref());
	   
	    codeUnLock(memory,lock,bcPosition);

	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(value);

	    code.endBC();
	    
	    return true;
	} else if (fastConstants && methodName.equals("get8") && args[0].isConstant()) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,result);
	    codeLock(memory,lock,bcPosition);

	    int index = ((IMConstant)args[0]).getIntValue();

	    codeReferenceAndConstantRangeCheck(obj,memory,index,1,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.BYTE);
	    
	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    if (index!=0) code.addl(index,memory);
	    code.movsbl(memory.ref(),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);

	    return true;
	} else if (methodName.equals("get8")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,result);
	    args[0].translate(index);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,1,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.BYTE);
	    
	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movsbl(memory.rdisp(0,index,1),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);

	    return true;
	} else if (methodName.equals("set16")) {

	    Reg memory = regs.chooseIntRegister();
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory);
	    args[0].translate(index);

	    Reg value = regs.chooseIntRegister(index,memory);
	    args[1].translate(value);

	    code.startBC(bcPosition);


	    Reg lock=regs.chooseIntRegister(memory,index,value);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,2,bcPosition);

	    // this is importand !!
	    regs.readIntRegister(value);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movw(value,memory.rdisp(0,index,2));

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(value);

	    return true;
	} else if (fastConstants && methodName.equals("get16") && args[0].isConstant()) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    code.startBC(bcPosition);

	    int index = ((IMConstant)args[0]).getIntValue();

	    codeReferenceAndConstantRangeCheck(obj,memory,index,2,bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndConstantRangeCheck(obj,memory,index,2,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    if (index!=0) code.addl(index*2,memory);
	    code.movswl(memory.ref(),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);

	    return true;
	} else if (methodName.equals("get16")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,result);
	    args[0].translate(index);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,2,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movswl(memory.rdisp(0,index,2),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);

	    return true;
	} else if (methodName.equals("setLittleEndian16")) {

	    Reg memory = regs.chooseIntRegister();
	    obj.translate(memory);
	    
	    Reg index = regs.chooseIntRegister(memory);
	    args[0].translate(index);
	    
	    Reg value = regs.chooseIntRegister(index,memory);
	    args[1].translate(value);
	    
	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,value);
	    codeLock(memory,lock,bcPosition);
	    
	    codeReferenceAndBorderedRangeCheck(obj,memory,index,2,bcPosition);
	    
	    // this is importand !!
	    regs.readIntRegister(value);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movw(value,memory.rdisp(0,index,1));

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(value);

	    return true;
	} else if (methodName.equals("getLittleEndian16")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,result);
	    args[0].translate(index);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,2,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movswl(memory.rdisp(0,index,1),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);

	    return true;
	} else if (methodName.equals("set32")) {

	    Reg memory = regs.chooseIntRegister();
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory);
	    args[0].translate(index);

	    Reg value = regs.chooseIntRegister(index,memory);
	    args[1].translate(value);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,value);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndRangeCheck(obj,memory,index,4,bcPosition);

	    // this is importand !!
	    regs.readIntRegister(value);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movl(value,memory.rdisp(0,index,4));

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);
	    regs.freeIntRegister(value);

	    return true;
	} else if (methodName.equals("getLittleEndian32")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    Reg index = regs.chooseIntRegister(memory,result);
	    args[0].translate(index);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,index,result);
	    codeLock(memory,lock,bcPosition);

	    codeReferenceAndBorderedRangeCheck(obj,memory,index,4,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);

	    code.movl(memory.rdisp(ADDR_OFFSET), memory);
	    code.movl(memory.rdisp(0,index,1),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();
	    
	    regs.freeIntRegister(memory);
	    regs.freeIntRegister(index);

	    return true;	    
	} else if (methodName.equals("size")) {

	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,result);
	    codeLock(memory,lock,bcPosition);

	    if (obj.checkReference())
		e.codeCheckReference(null,memory,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);
	    code.movl(memory.rdisp(SIZE_OFFSET),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();

	    regs.freeIntRegister(memory);

	    return true;
	} else if (methodName.equals("getStartAddress")) {
	    Reg memory = regs.chooseIntRegister(result);
	    obj.translate(memory);

	    code.startBC(bcPosition);

	    Reg lock=regs.chooseIntRegister(memory,result);
	    codeLock(memory,lock,bcPosition);

	    if (obj.checkReference())
		e.codeCheckReference(null,memory,bcPosition);

	    regs.allocIntRegister(result,BCBasicDatatype.INT);
	    code.movl(memory.rdisp(ADDR_OFFSET),result);

	    codeUnLock(memory,lock,bcPosition);

	    code.endBC();

	    regs.freeIntRegister(memory);

	    return true;
	} else if (methodName.equals("map")) {
	    /*
	    code.addMoveIntS_C(retSlot, 0); // return NULL reference 
	    */
	} else if (methodName.equals("copyToMemory")) {
	    /*
	    if (signature.equals("(Ljx/zero/Memory;III)I")) {
		int byteArraySlot = argSlots;
		int srcOffSlot    = argSlots+1;
		int dstOffSlot    = argSlots+2;
		int lenSlot       = argSlots+3;
		numArgWords = 0;
		pushIntS(code, lenSlot);
		pushIntS(code, dstOffSlot);
		pushIntS(code, srcOffSlot);
		pushIntS(code, byteArraySlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_COPY_INTO_MEMORY), retSlot); 
		code.addPopArguments(numArgWords); 
		throw new Error("Memory method copyToMemory with this signature not supported: "+signature);
	    } 
	    */
	} else if (methodName.equals("copyFromMemory")) {
	    /*
	    if (signature.equals("(Ljx/zero/Memory;III)I")) {
		int byteArraySlot = argSlots;
		int srcOffSlot    = argSlots+1;
		int dstOffSlot    = argSlots+2;
		int lenSlot       = argSlots+3;
		numArgWords = 0;
		pushIntS(code, lenSlot);
		pushIntS(code, dstOffSlot);
		pushIntS(code, srcOffSlot);
		pushIntS(code, byteArraySlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_COPY_FROM_MEMORY), retSlot); 
		code.addPopArguments(numArgWords); 
	    } 
	    */
	} else if (methodName.equals("copyToByteArray")) {
	    /*
	    if (signature.equals("([BIII)V")) {
		int byteArraySlot = argSlots;
		int arrOffSlot    = argSlots+1;
		int memOffSlot    = argSlots+2;
		int lenSlot       = argSlots+3;
		numArgWords = 0;
		pushIntS(code, lenSlot);
		pushIntS(code, memOffSlot);
		pushIntS(code, arrOffSlot);
		pushIntS(code, byteArraySlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_COPY_INTO_ARRAY), retSlot); 
		code.addPopArguments(numArgWords); 
	    } 
	    */
	} else if (methodName.equals("copyFromByteArray")) {
	    /*
	    if (signature.equals("([BIII)V")) {
		int byteArraySlot = argSlots;
		int arrOffSlot    = argSlots+1;
		int memOffSlot    = argSlots+2;
		int lenSlot       = argSlots+3;
		numArgWords = 0;
		pushIntS(code, lenSlot);
		pushIntS(code, memOffSlot);
		pushIntS(code, arrOffSlot);
		pushIntS(code, byteArraySlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_COPY_FROM_ARRAY), retSlot); 
		code.addPopArguments(numArgWords); 
	    }
	    */
	} else if (methodName.equals("getSubRange")) {
	    /*
	    if (signature.equals("(II)Ljx/zero/Memory;")) {
		int startSlot = argSlots;
		int sizeSlot  = argSlots+1;
		numArgWords = 0;
		pushIntS(code, sizeSlot);
		pushIntS(code, startSlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_GET_SUB_RANGE), retSlot); 
		code.addPopArguments(numArgWords); 
	    } 
	    */
	} else if (methodName.equals("clear")) {
	
	} else if (methodName.equals("fill16")) {
	    /*
	    if (signature.equals("(SII)V")) {
		int valueSlot     = argSlots;
		int offsetSlot    = argSlots+1;
		int lenSlot       = argSlots+2;
		numArgWords = 0;
		pushIntS(code, lenSlot);
		pushIntS(code, offsetSlot);
		pushIntS(code, valueSlot);
		pushIntS(code, objRefSlot);
		code.addIntCallC(new VMSupportSTEntry(VMSupportSTEntry.VM_MEMORY_FILL16), retSlot); 
		code.addPopArguments(numArgWords); 
	    }
	    */
	}

	if (opts.doVerbose("memobj")) java.lang.System.err.println("method has no fast memory access. call normal method for "+methodName);
	return false;
    }

    private void codeLock(Reg memory, Reg lock, int bcPosition) throws CompileException {
	if (opts.revocationCheckUsingSpinLock()) {
            if (opts.doVerbose("mlock")) java.lang.System.err.println("lock spin");
	    regs.allocIntRegister(lock,BCBasicDatatype.INT);
	    code.movl(new VMAbsoluteSTEntry(VMAbsoluteSTEntry.VM_SPINLOCK),lock);
	    code.spin_lock(lock.ref());
	}
	if (opts.revocationCheckUsingCLI()) {
            if (opts.doVerbose("mlock")) java.lang.System.err.println("lock intr");
	    code.pushfl();
	    code.cli();
	}
	/* revocation check */
	if (opts.revocationCheckUsingCLI() || opts.revocationCheckUsingSpinLock()) {
	    regs.readIntRegister(memory);
            Reg dzmem=regs.chooseIntRegister(lock,memory);
            regs.allocIntRegister(dzmem,-1);
            code.movl(memory.rdisp(DZ_OFFSET),dzmem);
	    code.cmpb(0, dzmem.rdisp(VALID_OFFSET));
	    code.je(e.createExceptionCall(-12,bcPosition));
            regs.freeIntRegister(dzmem);
	}
    }

    private void codeUnLock(Reg memory, Reg lock, int bcPosition) throws CompileException {
	if (opts.revocationCheckUsingCLI()) {
	    if (opts.doVerbose("mlock")) java.lang.System.err.println("lock intr");
	    code.popfl();
	}
	if (opts.revocationCheckUsingSpinLock()) {
            if (opts.doVerbose("mlock")) java.lang.System.err.println("unlock spin");
	    regs.readIntRegister(lock);
	    code.spin_unlock(lock.ref());
	    regs.freeIntRegister(lock);
	}
    }

    private void codeReferenceAndConstantRangeCheck(IMOperant obj,Reg memory,int index,int size,int bcPosition) 
	throws CompileException {
	
	/* TODO: popfl einbauen !!! */

	regs.readIntRegister(memory);

	/* reference check */
	if (obj.checkReference()) 
	    e.codeCheckReference(null,memory,bcPosition);

	/* range check */
	if (opts.doMemoryRangeChecks()) {
	  Reg tmp = regs.chooseIntRegister(memory);
	  regs.allocIntRegister(tmp,BCBasicDatatype.INT);

	  code.movl(memory.rdisp(SIZE_OFFSET),tmp);
	  if (size!=0) {
	    code.cmpl(index*size,tmp);
	  } else {
	    code.cmpl(index,tmp);
	  }
	  //code.jae(e.createExceptionCall(-4,bcPosition));
	  code.jnae(e.createExceptionCall(-4,bcPosition));

	  regs.freeIntRegister(tmp);
	}
    }

    /**
       codeReferenceAndRangeCheck:

       performs runtime reference and range checks for the memory object

       obj    : the memory reference object 

       memory : result of the translated memory-ref-object.
       index  : resutl of the translated index-object.

       size   : the size of the index in bytes

       bcPosition : the bytecode position

     */

    private void codeReferenceAndRangeCheck(IMOperant obj,Reg memory,Reg index,int size,int bcPosition) 
	throws CompileException {
	
	Reg tmp;

	regs.readIntRegister(index);
	regs.readIntRegister(memory);

	/* reference check */
	if (obj.checkReference()) 
	    e.codeCheckReference(null,memory,bcPosition);
	
	/* range check */
	if (opts.doMemoryRangeChecks()) {
	    switch (size) {
	    case 0:		
		code.cmpl(memory.rdisp(SIZE_OFFSET), index);
		code.jae(e.createExceptionCall(-4,bcPosition));
		break;
	    case 2:
		tmp = regs.chooseIntRegister(memory,index);
		regs.allocIntRegister(tmp,BCBasicDatatype.REFERENCE);
		
		code.movl(memory.rdisp(SIZE_OFFSET),tmp);
		code.shrl(1,tmp);
		code.cmpl(tmp,index);
		code.jae(e.createExceptionCall(-4,bcPosition));
		
		regs.freeIntRegister(tmp);
		break;
	    case 4:
		tmp = regs.chooseIntRegister(memory,index);
		regs.allocIntRegister(tmp,BCBasicDatatype.REFERENCE);
		
		code.movl(memory.rdisp(SIZE_OFFSET),tmp);
		code.shrl(2,tmp);
		code.cmpl(tmp,index);
		code.jae(e.createExceptionCall(-4,bcPosition));
		
		regs.freeIntRegister(tmp);
	    }
	}
    }

    private void codeReferenceAndBorderedRangeCheck(IMOperant obj,Reg memory,Reg index,int border,int bcPosition) 
	throws CompileException {
	
	Reg tmp;
	
	regs.readIntRegister(index);
	regs.readIntRegister(memory);
	
	/* reference check */
	if (obj.checkReference()) 
	    e.codeCheckReference(null,memory,bcPosition);
	
	/* range check with border */
	if (opts.doMemoryRangeChecks()) {
	    if (border==0) {
		code.cmpl(memory.rdisp(SIZE_OFFSET), index);
		code.jae(e.createExceptionCall(-4,bcPosition));
	    } else {
		tmp = regs.chooseIntRegister(memory,index);
		regs.allocIntRegister(tmp,BCBasicDatatype.REFERENCE);
		
		code.movl(memory.rdisp(SIZE_OFFSET),tmp);
		code.addl(border,tmp);
		code.cmpl(tmp,index);
		code.jae(e.createExceptionCall(-4,bcPosition));
		
		regs.freeIntRegister(tmp);
	    }
	}
    }
}
