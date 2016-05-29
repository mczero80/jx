package jx.compiler;

import java.util.Vector;
import java.util.Hashtable;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;

import jx.zero.Debug;

import jx.compiler.execenv.*;
import jx.compiler.imcode.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;

import jx.compiler.persistent.*;
import jx.compiler.plugins.CompilerPlugin;

class ExceptionEntry {
    int type;
    int bcPosition;
    UnresolvedJump jump;
    UnresolvedJump back_jump;
}

/**
 *
 * @author Christian Wawersich
 */
public class ExecEnvironmentIA32 implements ExecEnvironmentInterface {

	final private static int THROW_RuntimeException       = -1;
	final private static int THROW_NullPointerException   = -2;
	final private static int THROW_OutOfMemoryError       = -3; 
	final private static int THROW_MemoryIndexOutOfBounds = -4;
	final private static int THROW_StackOverflowError     = -5;
	final private static int THROW_ArithmeticException    = -6;
	final private static int THROW_MagicNumber            = -7;
	final private static int THROW_ParanoidCheck          = -8;
	final private static int THROW_StackJam               = -9;
	final private static int THROW_ArrayIndexOutOfBounds  = -10;
	final private static int THROW_UnsupportedByteCode    = -11;
	final private static int THROW_InvalidMemory          = -12;
	final private static int THROW_MemoryExhaustedException  = -13;

	private CodeContainer    container;
	private ConstantPool     cPool;
	private BinaryCodeIA32   code;
	private RegManager       regs;
	private MethodStackFrame frame;
	private BCMethod         method;
	private ClassFinder      classStore;
	private BCClass          bcClass;
	private CompilerOptions  opts;
	private Vector           exceptionStore;
	private Hashtable        plugins;

	private int arrayLengthOffset = 8;
	private int arrayDataStart    = 12;
	private int arrayElementSize  = 4;

	private int memoryLengthOffset = 0;
	private int memoryDataStart    = 4;

	private int     OBJECT_MAGIC_OFF  = -4;
	private int     OBJECT_MAGIC      = 0xbebeceee;
	private int     classDescOffset   = -4;

	private boolean doFastInstanceOf  = false; // buggy! don`t use
	private boolean doFastStatics     = false;

	private boolean extraRegSave      = false; // not needed
	private boolean initStack         = true;
	private boolean extraStackCleans  = false; // stack cleaning at Compact New
	private boolean pushMethodDesc    = false;

	// use compiler option "paranoid" to include extra tests
	private boolean checkVtable       = false;
	private boolean checkStackFrame   = true;
	private boolean checkInitRefs     = false;
	//private boolean doExtraMagic      = false;
	private boolean doExtraMagic      = true;

	private boolean extraTrace        = false;

	public ExecEnvironmentIA32(ClassFinder classFinder,CompilerOptions opts) {
		this.classStore = classFinder;
		this.opts = opts;
		exceptionStore = new Vector();
		initPlugins();
	}

    public void setCodeContainer(CodeContainer container) {
	this.container  = container;
	this.cPool      = container.getConstantPool();
	this.code       = container.getIA32Code();
	this.regs       = container.getRegManager();
	this.frame      = container.getMethodStackFrame();
	this.method     = container.getBCMethod();
    }

    public void setCurrentlyCompiling(BCClass aClass) {
	this.bcClass = aClass;
    }

    public CompilerOptionsInterface getCompilerOptions() {
	return opts;
    }

    public BCMethod getBCMethod(MethodRefCPEntry methodRefCPEntry) {

	if (methodRefCPEntry==null) return null;


	BCClass aClass = classStore.findClass(methodRefCPEntry.getClassName());
	if (aClass!=null) {
	    BCClassInfo info = (BCClassInfo) aClass.getInfo();
	    BCMethod method;

	    String name = methodRefCPEntry.getMemberName();
	    String sig  = methodRefCPEntry.getMemberTypeDesc();

	    for (int i=0;i<info.methods.length;i++) {
		method = info.methods[i];
		if (method.getName().equals(name) &&
		    method.getSignature().equals(sig)) {		
		    return method;
		}
	    }
	}
	
	return null;
    }

    public boolean doOptimize(int level) {
	if (opts.doOptimize()) {
	    return true;
	}
	return false;
    }

    public int getExtraStackSpace() {
      if (pushMethodDesc) return 1;
      return 0;
    }

    public void codeProlog() throws CompileException {	
	// save old frame pointer
	code.pushl(Reg.ebp);
	// copy stackpointer to framepointer
	code.movl(Reg.esp,Reg.ebp);

	if (!pushMethodDesc) {
	    frame.setExtraSpace(4);
	} else {
	    frame.setExtraSpace(8);
	    code.pushl(new MethodeDescSTEntry());
	}

	if (opts.doStackSizeCheck(container.isLeafMethod())) { /* new stack size check */
	    UnresolvedJump back = new UnresolvedJump();
	    code.addJumpTarget(back);
	    code.movl(new CurrentThreadPointerSTEntry(),Reg.esi);
	    code.movl(Ref.esi,Reg.eax);
	    code.movl(Reg.esp,Reg.ecx);
	    code.addl(new TCBOffsetSTEntry(TCBOffsetSTEntry.STACKTOP),Reg.eax);
	    code.subl(frame.getMaxFrameSizeSTEntry(),Reg.ecx);	    
	    code.cmpl(Ref.eax,Reg.ecx);
	    code.jle(createExceptionCall(THROW_StackOverflowError,0,back));
	}

	if (opts.doTrace()) {
	    int offset=frame.start();
	    frame.push(-1,new MethodeDescSTEntry());
	    frame.push(-1,Ref.ebp.disp(4));		    
	    frame.push(-1,0);
	    code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));	
	    frame.cleanup(offset);
	}

	// alloc local variabels
	if (opts.doClearStack() && !initStack) {
    	    if (pushMethodDesc) throw new Error("initStack didn`t work!!");
	    UnresolvedJump loop1 = new UnresolvedJump();
	    UnresolvedJump loop2 = new UnresolvedJump();	    
	    code.movl(frame.getAllocSTEntry(),Reg.eax);
	    code.xorl(Reg.ecx,Reg.ecx);
	    code.jmp(loop1);
	    code.addJumpTarget(loop2);
	    code.pushl(Reg.ecx);
	    code.subl(4,Reg.eax);
	    code.addJumpTarget(loop1);
	    code.test(Reg.eax,Reg.eax);
	    code.jne(loop2);
	} else {
	    code.subl(frame.getAllocSTEntry(),Reg.esp);
	    if (initStack) {
		frame.initReferences(code);
	    }
	}

	// save register (this is history)
	if (extraRegSave) {
	    frame.push(-1,Reg.ebx);
	    frame.push(-1,Reg.esi);
	    frame.push(-1,Reg.edi);
	}

	if (opts.doProfile(bcClass,method)) {
	    addTimerSP();
	}

        if (opts.doLogMethod(bcClass,method)) {
	  jx.compiler.plugins.CPUManager.codeEvent(this,regs,code,new ProfileSTEntry(ProfileSTEntry.EVENT_AUTOID),-1);
	}	

	if (!opts.isOption("noClassInit")) {
		if (method.isStatic() || method.isConstructor()) {
			code.pushl(new ClassSTEntry(bcClass.getClassName()));
			code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_CINIT));
			code.popl(Reg.ecx);
		}
	}

    }

    public void codeEpilog() throws CompileException {

	if (opts.doProfile(bcClass,method)) {
	    addTimerEP();
	}

	if (extraRegSave) {
	    frame.pop(Reg.edi);
	    frame.pop(Reg.esi);
	    frame.pop(Reg.ebx);
	}	    

	if (checkStackFrame && opts.doParanoidChecks()) {
	    // test if operanten stack is clean and framepointer is valid
	    code.addl(frame.getAllocSTEntry(),Reg.esp);
	    if (pushMethodDesc) code.popl(Reg.ecx);
	    code.cmpl(Reg.esp,Reg.ebp);
	    code.jne(createExceptionCall(-9,1));
	    // test if return address is valid
	    code.movl(Ref.esp.disp(4),Reg.ecx);
	    code.test(Reg.ecx,Reg.ecx);
	    code.je(createExceptionCall(-9,3));
	}

	// free c stack
	code.movl(Reg.ebp,Reg.esp);

	if (opts.doTrace()) {
	    frame.push(-1,Reg.eax);
	    frame.push(-1,Reg.edx);
	    int offset=frame.start();
	    frame.push(-1,new MethodeDescSTEntry());
	    frame.push(-1,Ref.ebp.disp(4));		    
	    frame.push(-1,1);
	    code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));	
	    frame.cleanup(offset);
	    frame.pop(Reg.edx);
	    frame.pop(Reg.eax);
	}	

	// restore frame pointer
	code.popl(Reg.ebp);
	// return from method
	code.ret();
	// code exception calls
	codeExceptionCalls();

	code.nop();
    }

    public MethodStackFrame getMethodStackFrame() {
	return frame;
    }

    private void addTimerSP() {
	// current methode_desc => stack
	code.movl(new MethodeDescSTEntry(),Reg.ecx);
	frame.push(-1,Reg.ecx);

	// caller (return addr) => stack
	code.movl(Ref.ebp.disp(4),Reg.ecx);
	frame.push(-1,Reg.ecx);

	// a = *current
        code.movl(new CurrentThreadPointerSTEntry(),Reg.esi);
	code.movl(Ref.esi,Reg.eax);
	// a = current.profile
       	code.addl(new ProfileSTEntry(ProfileSTEntry.PROFILE_OFFSET),Reg.eax);
       	code.movl(Ref.eax,Reg.ecx);
	// *(a)++  <=> current->profile->dirft_ptr++
	code.addl(8,Ref.ecx);
	code.movl(Ref.ecx,Reg.esi);
	code.movl(0,Ref.esi);
	code.movl(0,Ref.esi.disp(4));

	code.rdtsc();
	// timer 1 => stack
	frame.push(-1,Reg.edx);
	frame.push(-1,Reg.eax);

        // current->profile => stack	
	frame.push(-1,Reg.ecx);
    }

    private void addTimerEP() {
	frame.pop(Reg.ecx);

	// save return value => stack
	frame.push(-1,Reg.edx);
	frame.push(-1,Reg.eax);

	if (false) {	
	  // stop irqs 
	  frame.pushfl();
	  code.cli();
	} else {
	  frame.push(-1,0);
	}

	code.rdtsc();
	code.movl(2,Ref.ecx.disp(4));
	
	// timer 2 => stack
	frame.push(-1,Reg.edx);
	frame.push(-1,Reg.eax);	

	frame.push(-1,Reg.ecx);

	// call profile function
	code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_CALL));	

	frame.pop(Reg.ecx);

	code.rdtsc();
        code.movl(0,Ref.ecx.disp(4));

	// eax:edx = t3 - t2
	frame.pop(Reg.ebx);
	code.subl(Reg.ebx,Reg.eax);
	frame.pop(Reg.ebx);
	code.sbbl(Reg.ebx,Reg.edx);

	// cont irqs
	if (false) {	  
	  frame.popfl();
	} else {
	  frame.clearStack(1);
	}

	// drift[0] += eax:edx
	code.movl(Ref.ecx,Reg.esi);
	code.addl(Reg.eax,Ref.esi);
	code.adcl(Reg.edx,Ref.esi.disp(4));

	// restore return value
	frame.pop(Reg.eax);
	frame.pop(Reg.edx);

	// remove methode, caller and timer 1 from stack
	frame.clearStack(4);
    }

    /** 
	at least 2 clks
    */

    public void codeCheckReference(IMNode node,Reg reg,int bcPosition) throws CompileException {
	if (opts.doNullChecks()) {	    
	    code.test(reg,reg);
	    code.je(createExceptionCall(-2,bcPosition));
	}
	if (opts.doMagicChecks()) {
	    code.cmpl(OBJECT_MAGIC,reg.rdisp(OBJECT_MAGIC_OFF));
	    code.jne(createExceptionCall(-7,bcPosition));
	}
    }

     public void codeCheckMagic(IMNode node,Reg reg,int bcPosition) throws CompileException {
	if (doExtraMagic && opts.doMagicChecks() && opts.doParanoidChecks()) {
	    UnresolvedJump jumpForward  = new UnresolvedJump();
	    regs.readIntRegister(reg);
	    code.test(reg,reg);
	    code.je(jumpForward);
	    code.cmpl(OBJECT_MAGIC,reg.rdisp(OBJECT_MAGIC_OFF));
	    code.jne(createExceptionCall(-7,bcPosition));
	    code.addJumpTarget(jumpForward);
	}
    }   

    public void codeCheckDivZero(IMNode node,Reg reg,int bcPosition) throws CompileException {
	if (opts.doZeroDivChecks()) {	    
	    code.test(reg,reg);
	    code.je(createExceptionCall(-6,bcPosition));
	}
    }

    /**
        at least 3-5 clks
    */

    public void codeCheckArrayRange(IMNode node,Reg array,int index,int bcPosition) throws CompileException {
	if (opts.doBoundsChecks()) {
	    Reg len = regs.chooseIntRegister(array);

	    codeGetArrayLength(node,array,len);
	    regs.readIntRegister(len);

	    code.cmpl(index,len);
	    code.jae(createExceptionCall(THROW_ArrayIndexOutOfBounds,bcPosition));
	    regs.freeIntRegister(len);
	}
    }

    public void codeCheckArrayRange(IMNode node,Reg array,Reg index,int bcPosition) throws CompileException {
	if (opts.doBoundsChecks()) {
	    Reg len = regs.chooseIntRegister(array,index);

	    codeGetArrayLength(node,array,len);
	    regs.readIntRegister(index);
	    regs.readIntRegister(len);

	    code.cmpl(len,index);
	    code.jae(createExceptionCall(THROW_ArrayIndexOutOfBounds,bcPosition));

	    regs.freeIntRegister(len);
	}
    }

    public void codeNewObject(IMNode node,ClassCPEntry classCPEntry,Reg result) throws CompileException {
	code.startBC(node.getBCPosition());

	regs.saveIntRegister();
	frame.push(BCBasicDatatype.INT,new ClassSTEntry(classCPEntry.getClassName()));

	int ip=code.getCurrentIP();
	code.call(new AllocObjectSTEntry());
	regs.clearActives();
	codeStackMap(node,ip);
	frame.pop(Reg.ecx);
	regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.REFERENCE);	
	if (result.value!=0) {
	    code.movl(Reg.eax,result);
	}

	code.endBC();
    }

    public void codeCompactNew(IMNode node,ClassCPEntry classCPEntry,MethodRefCPEntry methodRefCPEntry,IMOperant[] args,Reg result) throws CompileException {
	int ip;

	code.startBC(node.getBCPosition());

  	regs.saveIntRegister();

	int offset = frame.start();

	frame.push(BCBasicDatatype.INT,new ClassSTEntry(classCPEntry.getClassName()));
	ip = code.getCurrentIP();
	code.call(new AllocObjectSTEntry());
	regs.clearActives();

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	// code.popl(...); we will cleanup the operanten stack later !!
	if (extraStackCleans && opts.doClearStack()) {
	  frame.clearStack(1);
	}

	// save object reference (eax -> stack)
	Reg objRef = regs.getIntRegister(Reg.eax);
	regs.allocIntRegister(objRef,BCBasicDatatype.REFERENCE);

	if (args.length>0) {
	    regs.saveIntRegister();
	    for (int i=(args.length-1);i>=0;i--) {
		int datatype = args[i].getDatatype();
		if (args[i].isConstant()) {
		    frame.push(datatype,((IMConstant)args[i]).getIntValue());
		} else {
		    Reg reg = regs.chooseIntRegister(null);
		    args[i].translate(reg);
		    frame.push(datatype,reg);
		    regs.freeIntRegister(reg);
		}
	    }
	}

	regs.readIntRegister(objRef);
	frame.push(BCBasicDatatype.REFERENCE,objRef);

	if (checkInitRefs && opts.doParanoidChecks()) codeCheckReference(node,objRef,-1);
	
	regs.freeIntRegister(objRef);
	regs.saveIntRegister();

	DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(methodRefCPEntry.getClassName(),
								     methodRefCPEntry.getMemberName(),
								     methodRefCPEntry.getMemberTypeDesc());
	if (extraTrace && opts.doTrace()) {
	    code.pushl(target);
	    code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
	    code.addl(4,Reg.esp);
	}

	ip=code.getCurrentIP();
	code.call(target);

	regs.clearActives();

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	regs.allocIntRegister(result,BCBasicDatatype.REFERENCE);
	frame.pop(result);

	frame.cleanup(offset);

	code.endBC();
    }
  
    public void codeNewArray(IMNode node,int type,IMOperant size,Reg result) throws CompileException {	
	Reg asize = regs.chooseIntRegister(null);

	size.translate(asize);

	regs.freeIntRegister(asize);
	regs.saveIntRegister();

	code.startBC(node.getBCPosition());

	int offset=frame.start();
	frame.push(BCBasicDatatype.INT,asize);
	frame.push(BCBasicDatatype.INT,new PrimitiveClassSTEntry(type));
	int ip=code.getCurrentIP();
	code.call(new AllocArraySTEntry());
	regs.clearActives();
	codeStackMap(node,ip);
	frame.cleanup(offset);
	regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.REFERENCE);
	if (result.value!=0) {
	    code.movl(Reg.eax,result);
	}

	code.endBC();
    }

    public void codeNewObjectArray(IMNode node,ClassCPEntry classCPEntry,IMOperant size,Reg result) throws CompileException {
	Reg asize = regs.chooseIntRegister(null);

	size.translate(asize);

	regs.freeIntRegister(asize);
	regs.saveIntRegister();

	code.startBC(node.getBCPosition());

	int offset=frame.start();
	frame.push(BCBasicDatatype.INT,asize);
	frame.push(BCBasicDatatype.INT,new ClassSTEntry(classCPEntry.getClassName()));
	int ip=code.getCurrentIP();
	code.call(new AllocArraySTEntry());
	regs.clearActives();
	codeStackMap(node,ip);
	frame.cleanup(offset);
	regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.REFERENCE);
	if (result.value!=0) {
	    code.movl(Reg.eax,result);
	}

	code.endBC();
    }

    public void codeGetArrayField(IMNode node,Reg array,int datatype,int index,Reg result,int bcPosition) throws CompileException {	
	    regs.readIntRegister(array);
	    regs.allocIntRegister(result,datatype);

	    if (opts.isOption("32BitArrays")) {
		    code.movl(array.rdisp(arrayDataStart+(index*4)),result);
	    } else {
			System.out.println("warn: use compact arrays");
		    switch (datatype) {
			    case BCBasicDatatype.BYTE:
				    code.movsbl(array.rdisp(arrayDataStart+index),result);
				    //code.movswl(array.rdisp(arrayDataStart+index),result);
			            //code.andl(0x000000ff,result);	
				    break;
			    case BCBasicDatatype.CHAR:
				    if (opts.isOption("8BitChars")) {
					    code.movsbl(array.rdisp(arrayDataStart+index),result);
					    //code.movswl(array.rdisp(arrayDataStart+index),result);
					    //code.andl(0x000000ff,result);	
				    } else {
					    code.movswl(array.rdisp(arrayDataStart+(index*2)),result);
				    }
				    break;
			    case BCBasicDatatype.SHORT:
				    code.movswl(array.rdisp(arrayDataStart+(index*2)),result);
				    break;
			    case BCBasicDatatype.INT:
			    case BCBasicDatatype.REFERENCE:
				    code.movl(array.rdisp(arrayDataStart+(index*4)),result);
				    break;
			    default:
				    throw new CompileException("not implemented yet!");
		    }
	    }
    }   
    
    public void codeGetArrayField(IMNode node,Reg array,int datatype,Reg index,Reg result,int bcPosition) throws CompileException {	
	    regs.readIntRegister(index);
	    regs.readIntRegister(array);
	    regs.allocIntRegister(result,datatype);

	    if (opts.isOption("32BitArrays")) {
		    code.movl(array.rdisp(arrayDataStart,index,4),result);
	    } else {
			System.out.println("warn: use compact arrays");
		    switch (datatype) {
			    case BCBasicDatatype.BYTE:
				    code.movsbl(array.rdisp(arrayDataStart,index,1),result);
				    //code.movl(array.rdisp(arrayDataStart,index,1),result);
				    //code.andl(0x000000ff,result);	
				    break;
			    case BCBasicDatatype.CHAR:
				    if (opts.isOption("8BitChars")) {
					    code.movsbl(array.rdisp(arrayDataStart,index,1),result);
					    //code.movl(array.rdisp(arrayDataStart,index,1),result);
					    //code.andl(0x000000ff,result);	
				    } else {
					    code.movswl(array.rdisp(arrayDataStart,index,2),result);
				    }
				    break;
			    case BCBasicDatatype.SHORT:
				    code.movswl(array.rdisp(arrayDataStart,index,2),result);
				    break;
			    case BCBasicDatatype.INT:
			    case BCBasicDatatype.REFERENCE:
				    code.movl(array.rdisp(arrayDataStart,index,4),result);
				    break;
			    default:
				    throw new CompileException("not implemented yet!");
		    }
	    }
    }

    public void codeGetArrayFieldLong(IMNode node,Reg array,int datatype,Reg index,Reg64 result,int bcPosition) throws CompileException {	
	    regs.readIntRegister(index);
	    regs.readIntRegister(array);
	    regs.allocLongRegister(result);
	    code.movl(array.rdisp(arrayDataStart,index,8),result.low);
	    code.movl(array.rdisp(arrayDataStart+4,index,8),result.high);
    }

    public void codePutArrayField(IMNode node,Reg array,int datatype,int index,Reg value,int bcPosition) throws CompileException {
	    regs.readIntRegister(array);
	    regs.readIntRegister(value);

	    if (datatype==BCBasicDatatype.REFERENCE && opts.isOption("writeBarrier")) {
		    regs.saveIntRegister();

		    int offset=frame.start();
		    frame.push(value);
		    frame.push(BCBasicDatatype.INT, index);
	            frame.push(array);	
		    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_PUTAFIELD32));
		    frame.cleanup(offset);

		    regs.readIntRegister(array);
		    return;	
	    }	

	    if (opts.isOption("32BitArrays")) {
		    code.movl(value,array.rdisp(arrayDataStart+index*4));
	    } else {
			System.out.println("warn: use compact arrays");
		    switch (datatype) {
			    case BCBasicDatatype.BYTE:
				    if (!value.equals(Reg.eax)) {
					    if (array.equals(Reg.eax)) {
						    Reg tmp = regs.chooseIntRegister(Reg.eax,value);
						    regs.allocIntRegister(tmp,-1);
						    code.movl(array,tmp);
						    regs.freeIntRegister(array); 
						    array=tmp;
					    } 
					    Reg v = regs.getIntRegister(Reg.eax);	
					    regs.allocIntRegister(v,-1);
					    code.movl(value,v);
					    regs.freeIntRegister(value);
					    value=v;
				    }
				    code.movb(value,array.rdisp(arrayDataStart+index));
				    break;
			    case BCBasicDatatype.CHAR:
				    if (opts.isOption("8BitChars")) {
					    if (!value.equals(Reg.eax)) {
						    if (array.equals(Reg.eax)) {
							    Reg tmp = regs.chooseIntRegister(Reg.eax,value);
							    regs.allocIntRegister(tmp,-1);
							    code.movl(array,tmp);
							    regs.freeIntRegister(array); 
							    array=tmp;
						    } 
						    Reg v = regs.getIntRegister(Reg.eax);	
						    regs.allocIntRegister(v,-1);
						    code.movl(value,v);
						    regs.freeIntRegister(value);
						    value=v;
					    }
					    code.movb(value,array.rdisp(arrayDataStart+index));
				    } else {
					    code.movw(value,array.rdisp(arrayDataStart*index*2));
				    }
				    break;
			    case BCBasicDatatype.SHORT:
				    code.movw(value,array.rdisp(arrayDataStart*index*2));
				    break;
			    case BCBasicDatatype.INT:
			    case BCBasicDatatype.REFERENCE:
				    code.movl(value,array.rdisp(arrayDataStart+index*4));
				    break;
			    default:
				    throw new CompileException("not implemented yet!");
		    }
	    }
    }

    public void codePutArrayField(IMNode node,Reg array,int datatype,Reg index,Reg value,int bcPosition) throws CompileException {
	    regs.readIntRegister(index);
	    regs.readIntRegister(array);
	    regs.readIntRegister(value);

	    if (datatype==BCBasicDatatype.REFERENCE && opts.isOption("writeBarrier")) {
		    regs.saveIntRegister();

		    int offset=frame.start();
		    frame.push(value);
		    frame.push(index);
	            frame.push(array);	
		    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_PUTAFIELD32));
		    frame.cleanup(offset);

		    regs.readIntRegister(array);
		    return;	
	    }	

	    if (opts.isOption("32BitArrays")) {
		    code.movl(value,array.rdisp(arrayDataStart,index,4));
	    } else {
			System.out.println("warn: use compact arrays");
		    switch (datatype) {
			    case BCBasicDatatype.BYTE:
				    if (!value.equals(Reg.eax)) {
			              if (index.equals(Reg.eax)) {
					Reg tmp = regs.chooseIntRegister(Reg.eax,value,array);
					regs.allocIntRegister(tmp,-1);
					code.movl(index,tmp);
					regs.freeIntRegister(index); 
					index=tmp;
                                      } 
			              if (array.equals(Reg.eax)) {
					Reg tmp = regs.chooseIntRegister(Reg.eax,value,index);
					regs.allocIntRegister(tmp,-1);
					code.movl(array,tmp);
					regs.freeIntRegister(array); 
					array=tmp;
                                      } 
				      Reg v = regs.getIntRegister(Reg.eax);	
			              regs.allocIntRegister(v,-1);
				      code.movl(value,v);
				      regs.freeIntRegister(value);
				      value=v;
				    }
				    code.movb(value,array.rdisp(arrayDataStart,index,1));
				    break;
			    case BCBasicDatatype.CHAR:
				    if (opts.isOption("8BitChars")) {
					    code.movb(value,array.rdisp(arrayDataStart,index,1));
				    } else {
					    code.movw(value,array.rdisp(arrayDataStart,index,2));
				    }
				    break;
			    case BCBasicDatatype.SHORT:
				    code.movw(value,array.rdisp(arrayDataStart,index,2));
				    break;
			    case BCBasicDatatype.INT:
			    case BCBasicDatatype.REFERENCE:
				    code.movl(value,array.rdisp(arrayDataStart,index,4));
				    break;
			    default:
				    throw new CompileException("not implemented yet!");
		    }
	    }
    }

    public void codeNewMultiArray(IMNode node,ClassCPEntry type,IMOperant[] oprs,Reg result) throws CompileException {	

	int offset=frame.start();
	
	Reg asize = regs.chooseIntRegister(null);
	for (int i=0;i<oprs.length;i++) {
	    oprs[i].translate(asize);
	    frame.push(BCBasicDatatype.INT,asize);
	}
	regs.freeIntRegister(asize);

	code.startBC(node.getBCPosition());

	regs.saveIntRegister();
	frame.push(BCBasicDatatype.INT,oprs.length);
	frame.push(BCBasicDatatype.INT,new ClassSTEntry(type.getClassName()));
	int ip=code.getCurrentIP();
	code.call(new AllocMultiArraySTEntry());
	regs.clearActives();
	codeStackMap(node,ip);
	frame.cleanup(offset);

	regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.REFERENCE);
	if (result.value!=0) {
	    code.movl(Reg.eax,result);
	}

	code.endBC();
    }

    public void codeGetArrayLength(IMNode node,Reg array,Reg result) throws CompileException {
	regs.allocIntRegister(result,node.getDatatype());
	regs.readIntRegister(array);
	code.movl(array.rdisp(arrayLengthOffset),result);
    }

    public void codeThrow(IMNode node,int exception,int bcPosition) throws CompileException {	
	code.startBC(bcPosition);

	regs.saveIntRegister();	
	int offset=frame.start();
	frame.push(BCBasicDatatype.INT,exception);
	code.call(new ExceptionHandlerSTEntry());
	frame.cleanup(offset);

	code.endBC();
    }

    public void codeThrow(IMNode node,IMOperant exception,int bcPosition) throws CompileException {
	
	Reg exRef = regs.chooseIntRegister(null);
	exception.translate(exRef);
	regs.freeIntRegister(exRef);
	code.startBC(bcPosition);
	
	regs.saveIntRegister();
	
	int offset=frame.start();
	frame.push(BCBasicDatatype.REFERENCE,exRef);
	code.call(new ExceptionHandlerSTEntry());
	frame.cleanup(offset);

	code.endBC();
    }
    
    public void codeCheckCast(IMNode node,ClassCPEntry classCPEntry,Reg objRef,int bcPosition) throws CompileException {
	code.startBC(bcPosition);

	if (opts.isOption("noCheckCast")) return;

	if (false && opts.doFastCheckCast()) {

	  UnresolvedJump jumpForward  = new UnresolvedJump();
	  UnresolvedJump jumpForward2 = new UnresolvedJump();

	  //regs.saveIntRegister();

	  /* null pointer test */
	  regs.readIntRegister(objRef);	  
	  code.test(objRef,objRef);
	  code.je(jumpForward);
	  
	  /* class is objclass */
	  Reg vtable = regs.chooseIntRegister(objRef);
	  regs.allocIntRegister(vtable,-1);
	  code.movl(objRef.ref(),vtable);
	  regs.freeIntRegister(vtable);
	  code.cmpl(new ClassSTEntry(classCPEntry.getClassName()),vtable.rdisp(classDescOffset));
	  code.je(jumpForward2);

	  regs.saveIntRegister();

	  int offset=frame.start();
	  frame.push(-1,new ClassSTEntry(classCPEntry.getClassName()));
	  frame.push(BCBasicDatatype.REFERENCE,objRef);
	  code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_CHECKCAST));
	  frame.cleanup(offset);

	  regs.readIntRegister(objRef);

	  code.addJumpTarget(jumpForward);
	  code.addJumpTarget(jumpForward2);

	} else {
	  regs.saveIntRegister();

	  int offset=frame.start();
	  frame.push(-1,new ClassSTEntry(classCPEntry.getClassName()));
	  frame.push(BCBasicDatatype.REFERENCE,objRef);
	  code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_CHECKCAST));
	  frame.cleanup(offset);

	  regs.readIntRegister(objRef);
	}

	code.endBC();
    }

    public void codeInstanceOf(IMNode node,ClassCPEntry classCPEntry,Reg objRef,Reg regEAX,int bcPosition) throws CompileException {
	code.startBC(bcPosition);

	if (opts.isOption("fast_instance_of")) { // BUGGY don`t use
	    
	  UnresolvedJump jumpForward = new UnresolvedJump();  

	  regs.readIntRegister(objRef);
	  //value = regs.chooseIntRegister(objRef);
	  //regs.allocIntRegister(value,BCBasicDatatype.BOOLEAN);
	  regs.allocIntRegister(regEAX,BCBasicDatatype.BOOLEAN);

	  /*
	  code.movl(1,regEAX);
	  code.test(objRef,objRef);
	  code.je();
	  code.cmpl(new ClassSTEntry(classCPEntry.getClassName()),objRef.rdisp(classDescOffset));
	  code.je();
	  */	      

	  code.xorl(regEAX,regEAX);
	  code.test(objRef,objRef);
	  code.sete(regEAX);
	  code.cmpl(new ClassSTEntry(classCPEntry.getClassName()),objRef.rdisp(classDescOffset));
	  code.sete(regEAX);
	  code.je(jumpForward);
	  regs.freeIntRegister(regEAX);
	  regs.saveIntRegister();
	  int offset=frame.start();
	  frame.push(-1,new ClassSTEntry(classCPEntry.getClassName()));
	  frame.push(BCBasicDatatype.REFERENCE,objRef);
	  code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_INSTANCEOF));
	  frame.cleanup(offset);
	  regs.allocIntRegister(regEAX,BCBasicDatatype.BOOLEAN);
	  if (regEAX.value!=0) {
	      code.movl(Reg.eax,regEAX);
	  }
	  code.addJumpTarget(jumpForward);

	} else {

	    regs.saveIntRegister();

	    int offset=frame.start();
	    frame.push(-1,new ClassSTEntry(classCPEntry.getClassName()));
	    frame.push(BCBasicDatatype.REFERENCE,objRef);
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_INSTANCEOF));
	    frame.cleanup(offset);

	    regs.allocIntRegister(regEAX,Reg.eax,BCBasicDatatype.BOOLEAN);
	    if (regEAX.value!=0) {
		code.movl(Reg.eax,regEAX);
	    }
	}

	code.endBC();
    }

    public void codeMonitorEnter(IMNode node,IMOperant obj,int bcPosition) throws CompileException {
	Debug.out.println("-- monitor enter");
	if (opts.monitorClass()!=null) {
	    code.startBC(bcPosition);
	    
	    int datatype = obj.getDatatype();
	    if (obj.isConstant()) {
		frame.push(datatype,((IMConstant)obj).getIntValue());
	    } else {
		Reg reg = regs.chooseIntRegister(null);
		obj.translate(reg);
		frame.push(datatype,reg);
		regs.freeIntRegister(reg);
	    }
	    
	    DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(opts.monitorClass(),
									 "enter",
									 "(Ljava/lang/Object;)V");
	    int ip=code.getCurrentIP();
	    code.call(target);
	    
	    node.addDebugInfo(frame.stackMapToString(node));
	    codeStackMap(node,ip);
	    
	    regs.clearActives();
	    
	    code.endBC();
	    
	} else {
	    /* ignore monitorenter/exit */
	    /*
	    Reg objRef = regs.chooseIntRegister(null);
	    obj.translate(objRef);
	    regs.freeIntRegister(objRef);
	    regs.saveIntRegister();
	    frame.push(BCBasicDatatype.REFERENCE,objRef);
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_MONITORENTER));
	    frame.pop(Reg.ecx);
	    */
	}
    
    }

    public void codeMonitorLeave(IMNode node,IMOperant obj,int bcPosition) throws CompileException {
	Debug.out.println("-- monitor exit");
	if (opts.monitorClass()!=null) {
	    code.startBC(bcPosition);
	    
	    int datatype = obj.getDatatype();
	    if (obj.isConstant()) {
		frame.push(datatype,((IMConstant)obj).getIntValue());
	    } else {
		Reg reg = regs.chooseIntRegister(null);
		obj.translate(reg);
		frame.push(datatype,reg);
		regs.freeIntRegister(reg);
	    }
	    
	    DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(opts.monitorClass(),
									 "exit",
									 "(Ljava/lang/Object;)V");
	    int ip=code.getCurrentIP();
	    code.call(target);
	    
	    node.addDebugInfo(frame.stackMapToString(node));
	    codeStackMap(node,ip);
	    
	    regs.clearActives();
	    
	    code.endBC();
	} else {
	    /* ignore monitorenter/exit */
	    /*
	    Reg objRef = regs.chooseIntRegister(null);
	    obj.translate(objRef);
	    regs.freeIntRegister(objRef);
	    regs.saveIntRegister();
	    frame.push(BCBasicDatatype.REFERENCE,objRef);
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_MONITOREXIT)); 
	    frame.pop(Reg.ecx);
	    */
	}
    }

    public SymbolTableEntryBase getStringRef(StringCPEntry cpEntry) throws CompileException{
	return new StringSTEntry(cpEntry.value());
    } 

    public void codeLoadStringRef(StringCPEntry cpEntry,Reg result,int bcPosition) throws CompileException {
	regs.allocIntRegister(result,BCBasicDatatype.REFERENCE);
	code.movl(new StringSTEntry(cpEntry.value()),result);
    }

    public void codeGetField(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg result,int bcPosition) throws CompileException {
	int offset = getFieldOffset(fieldRefCPEntry);
	String className = fieldRefCPEntry.getClassName();
	BCClass aClass = classStore.findClass(className);
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	BCClass mapIf = classStore.findClass("jx/zero/MappedLittleEndianObject");
	if (classStore.implementsInterface(mapIf, aClass)) {
	    Debug.out.println("       **"+className);
	}
	if (opts.isOption("mappedMemory") && classStore.implementsInterface(mapIf, aClass)) {
	    Debug.out.println("Compile mapped access");
	    offset = info.mappedLayout.getFieldOffset(fieldRefCPEntry.getMemberName());
	    if (opts.isOption("mappedMemoryInline")) {
		Debug.out.println("-- inline mapped access");
		
		code.startBC(bcPosition);
		
		regs.allocIntRegister(result,node.getDatatype());
		//regs.allocIntRegister(result,BCBasicDatatype.INT);
		
		code.movl(objRef.rdisp(MAP_MEM_OFFSET), objRef);
		code.movl(objRef.rdisp(offset),result);
		
		
		code.endBC();
	    } else {
		throw new Error();
	    }	    
	} else {
	    regs.allocIntRegister(result,node.getDatatype());
	    code.movl(objRef.rdisp(offset),result);
	}
    }

    public void codeGetFieldLong(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg64 result,int bcPosition) throws CompileException {
	int offset = getFieldOffset(fieldRefCPEntry);
	regs.allocLongRegister(result);
	code.movl(objRef.rdisp(offset),result.low);
	code.movl(objRef.rdisp(offset+4),result.high);
    }

    private void codeGetStaticFieldAddr(IMNode node,
		    String className, int fieldOffset, Reg addr) throws CompileException {

	    regs.saveIntRegister();
	    int offset=frame.start();
	    frame.push(-1,new ClassSTEntry(className));
	    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_GETSTATICS_ADDR2)); 
	    frame.cleanup(offset);
	    regs.allocIntRegister(addr,node.getDatatype());
	    //code.movl(Reg.eax.rdisp(new StaticFieldSTEntry(className, StaticFieldSTEntry.LIB_INDEX, 0)),addr);
	    //code.addl(new StaticFieldSTEntry(className, StaticFieldSTEntry.TOTAL_OFFSET, fieldOffset),addr);
	    code.lea(Reg.eax.rdisp(new StaticFieldSTEntry(className,
					    StaticFieldSTEntry.TOTAL_OFFSET,
					    fieldOffset)), addr);
    }

    public void codeGetStaticField(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg result,int bcPosition) throws CompileException {
	String className = fieldRefCPEntry.getClassName();
	BCClass aClass = classStore.findClass(className);
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	int offset = info.classLayout.getFieldOffset(fieldRefCPEntry.getMemberName());

	if (offset == -1) {
	    throw new CompileException("Cannot find field "+fieldRefCPEntry.getMemberName()+" in class "+className);
	}
	
	Reg addr = regs.chooseIntRegister(result);
	codeGetStaticFieldAddr(node,className,offset,addr);

	regs.allocIntRegister(result,node.getDatatype());
	code.movl(addr.ref(),result);

	regs.freeIntRegister(addr);
    }

    public void codeGetStaticFieldLong(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg64 result,int bcPosition) throws CompileException {
	String className = fieldRefCPEntry.getClassName();
	BCClass aClass = classStore.findClass(className);
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	int offset = info.classLayout.getFieldOffset(fieldRefCPEntry.getMemberName());

	if (offset == -1) {
	    throw new CompileException("Cannot find field "+fieldRefCPEntry.getMemberName()+" in class "+className);
	}
	
	Reg addr = regs.chooseIntRegister(result.low,result.high);
	codeGetStaticFieldAddr(node,className,offset,addr);

	regs.allocLongRegister(result);
	code.movl(addr.ref(),result.low);
	code.movl(addr.rdisp(4),result.high);

	regs.freeIntRegister(addr);
    }

    public final static int MAP_MEM_OFFSET  = 4;

    public void codePutField(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg value,int bcPosition) throws CompileException {	
	    int offset = getFieldOffset(fieldRefCPEntry);
	    String className = fieldRefCPEntry.getClassName();
	    BCClass aClass = classStore.findClass(className);
	    BCClassInfo info = (BCClassInfo)aClass.getInfo();
	    BCClass mapIf = classStore.findClass("jx/zero/MappedLittleEndianObject");
	    if (classStore.implementsInterface(mapIf, aClass)) {
		Debug.out.println("       **"+className);
	    }
	    if (opts.isOption("mappedMemory") && classStore.implementsInterface(mapIf, aClass)) {
		Debug.out.println("Compile mapped access");
		offset = info.mappedLayout.getFieldOffset(fieldRefCPEntry.getMemberName());
		if (opts.isOption("mappedMemoryInline")) {
		    Debug.out.println("-- inline mapped access");


		    code.startBC(bcPosition);
		    
		    code.movl(objRef.rdisp(MAP_MEM_OFFSET), objRef);
		    code.movl(value,objRef.rdisp(offset));
		    
		    code.endBC();
		    
		} else {
		    regs.saveIntRegister();
		    int foff=frame.start();
		    frame.push(value);
	            frame.push(BCBasicDatatype.INT,offset);	
		    frame.push(objRef);	
		    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_MAP_PUT32));
		    frame.cleanup(foff);
		}
	    } else if (node.getDatatype()==BCBasicDatatype.REFERENCE && opts.isOption("writeBarrier")) {
		    regs.saveIntRegister();
		    int foff=frame.start();
		    frame.push(value);
	            frame.push(BCBasicDatatype.INT,(offset/4));	
		    frame.push(objRef);	
		    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_PUTFIELD32));
		    frame.cleanup(foff);
	    } else {
		    code.movl(value,objRef.rdisp(offset));
	    }
    }

    public void codePutFieldLong(IMNode node,FieldRefCPEntry fieldRefCPEntry,Reg objRef,Reg64 value,int bcPosition) throws CompileException {	
	int offset = getFieldOffset(fieldRefCPEntry);
	code.movl(value.low,objRef.rdisp(offset));
	code.movl(value.high,objRef.rdisp(offset+4));
    }

    public void codePutStaticField(IMNode node, FieldRefCPEntry fieldRefCPEntry, Reg value, int bcPosition) throws CompileException {
	    String className = fieldRefCPEntry.getClassName();
	    BCClass aClass = classStore.findClass(className);
	    BCClassInfo info = (BCClassInfo)aClass.getInfo();
	    int offset = info.classLayout.getFieldOffset(fieldRefCPEntry.getMemberName());

	    if (offset == -1) {
		    throw new CompileException("Cannot find field "+fieldRefCPEntry.getMemberName()+" in class "+className);
	    }

	    if (false && node.getDatatype()==BCBasicDatatype.REFERENCE && opts.isOption("writeBarrier")) {
		    regs.saveIntRegister();

		    int foff=frame.start();
		    frame.push(value);
		    frame.push(BCBasicDatatype.INT,offset);	
		    frame.push(-1,new ClassSTEntry(className));
		    code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_PUTSFIELD32));
		    frame.cleanup(foff);
	    } else {

		    Reg addr = regs.chooseIntRegister(value);
		    codeGetStaticFieldAddr(node, className, offset, addr);
		    regs.readIntRegister(value);

		    code.movl(value,addr.ref());

		    regs.freeIntRegister(addr);
	    }
    }

    public void codePutStaticFieldLong(IMNode node, FieldRefCPEntry fieldRefCPEntry, Reg64 value, int bcPosition) throws CompileException {
	String className = fieldRefCPEntry.getClassName();
	BCClass aClass = classStore.findClass(className);
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	int offset = info.classLayout.getFieldOffset(fieldRefCPEntry.getMemberName());

	if (offset == -1) {
	    throw new CompileException("Cannot find field "+fieldRefCPEntry.getMemberName()+" in class "+className);
	}
	
	Reg addr = regs.chooseIntRegister(value.high,value.low);
	codeGetStaticFieldAddr(node,className,offset,addr);

	regs.readLongRegister(value);
        code.movl(value.low,addr.ref());
        code.movl(value.high,addr.rdisp(4));

	regs.freeIntRegister(addr);
    }

    public void codeLongMul(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg);
	int offset = frame.start();
	frame.push(reg);
	regs.freeLongRegister(reg);

	reg = regs.getLongRegister(Reg64.eax);
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeLongRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.MUL));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	

	return;
    }

    public void codeLongDiv(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg);
	int offset = frame.start();
	frame.push(reg);
	regs.freeLongRegister(reg);

	reg = regs.getLongRegister(Reg64.eax);
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeLongRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.DIV));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	

	return;
    }

    public void codeLongRem(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg);
	int offset = frame.start();
	frame.push(reg);
	regs.freeLongRegister(reg);

	reg = regs.getLongRegister(Reg64.eax);
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeLongRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.REM));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	

	return;
    }

    
    public void codeLongShr(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg64 = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg64);
	int offset = frame.start();
	frame.push(reg64);
	regs.freeLongRegister(reg64);

	Reg reg = regs.chooseIntRegister();
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeIntRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.SHR));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	

	return;
    }

    public void codeLongShl(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg64 = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg64);
	int offset = frame.start();
	frame.push(reg64);
	regs.freeLongRegister(reg64);

	Reg reg = regs.chooseIntRegister();
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeIntRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.SHL));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	

	return;
    }

    public void codeLongUShr(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg64 result,int bcPosition) throws CompileException {

	Reg64 reg64 = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg64);
	int offset = frame.start();
	frame.push(reg64);
	regs.freeLongRegister(reg64);

	Reg reg = regs.chooseIntRegister();
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeIntRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.USHR));

	frame.cleanup(offset);

	regs.allocLongRegister(result,Reg64.eax);
	if (!result.high.equals(Reg64.eax.high)) {
		code.movl(Reg64.eax.high,result.high);
	}
	if (!result.low.equals(Reg64.eax.low)) {
		code.movl(Reg64.eax.low,result.low);
	}
/*
	if (!result.equals(Reg64.eax)) {
	    code.movl(Reg64.eax.low,result.low);
	    code.movl(Reg64.eax.high,result.high);
	}	
*/

	return;
   }


    public void codeLongCompare(IMNode node,IMOperant lOpr,IMOperant rOpr,Reg result,int bcPosition) throws CompileException {

	/** FIXME: wrong execution order ?!? **/
        if (opts.doVerbose("fixme")) Debug.out.println("fixme: wrong execution order");

	Reg64 reg = regs.getLongRegister(Reg64.eax);
	lOpr.translate(reg);
	int offset = frame.start();
	frame.push(reg);
	regs.freeLongRegister(reg);

	reg = regs.getLongRegister(Reg64.eax);
	rOpr.translate(reg);
	frame.push(reg);
	regs.freeLongRegister(reg);

	regs.saveIntRegister();

	code.call(new LongArithmeticSTEntry(LongArithmeticSTEntry.CMP));

	frame.cleanup(offset);

	regs.allocIntRegister(result,Reg.eax,BCBasicDatatype.BOOLEAN);
	if (!result.equals(Reg.eax)) {
	    code.movl(Reg.eax,result);
	}	

	return;
    }

    public void codeVirtualCall(IMNode node,
				MethodRefCPEntry methodRefCPEntry,
				IMOperant        obj,
				IMOperant[]      args,
				int              datatype,
				Reg              result,
				int              bcPosition) throws CompileException {

	String className  = methodRefCPEntry.getClassName();

	CompilerPlugin plugin; 
	if ((plugin = findPlugin(className))!=null) {
	    if (plugin.code(node, regs, code, methodRefCPEntry,obj,args,datatype,result,bcPosition)) return;
	}

	if (isMethod(methodRefCPEntry,"wait","()V") ||
	    isMethod(methodRefCPEntry,"notify","()V") ||
	    isMethod(methodRefCPEntry,"notifyAll","()V")) {

	    Debug.out.println("!!!! call redirect "+methodRefCPEntry.getMemberName()
			       +" in "+method.getName()+" bytecode "+bcPosition+" !!!!");

	    int offset = codeStaticPushArgs(args);
	
	    code.startBC(bcPosition);
	
	    regs.saveIntRegister();

	    DirectMethodCallSTEntry target = new DirectMethodCallSTEntry("jx/zero/env/ConditionalVariables",
									 "impl_"+methodRefCPEntry.getMemberName(),
									 "(Ljava/lang/Object;)V");
	
	    if (extraTrace && opts.doTrace()) {
		code.pushl(target);
		code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
		code.addl(4,Reg.esp);
	    }
	    
	    int ip=code.getCurrentIP();
	    code.call(target);
	    
	    node.addDebugInfo(frame.stackMapToString(node));
	    codeStackMap(node,ip);
	    
	    regs.clearActives();
	    codeStackCleanup(offset,result,node.getDatatype());
	    
	    code.endBC();

	} else {
	    BCClass aClass = classStore.findClass(className);
	    if (aClass==null) Debug.out.println("Can't find ClassInfo for "+className);
	    BCClassInfo info = (BCClassInfo) aClass.getInfo();
	    int index = info.methodTable.getIndex(methodRefCPEntry.getMemberName()+methodRefCPEntry.getMemberTypeDesc());

	    //Debug.out.println("call "+methodRefCPEntry.getMemberName());
	    
	    Reg objRef = regs.chooseIntRegister(null);
	    
	    int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);
	    
	    code.startBC(bcPosition);
	    
	    if (obj.checkReference())
		codeCheckReference(node,objRef,bcPosition);
	    
	    //regs.freeIntRegister(objRef);
	    //if (true) regs.saveIntRegister(); // TODO 
	    regs.saveOtherIntRegister(objRef);
	    
	    // lookup vtable entry
	    Reg vtable = regs.chooseAndAllocIntRegister(objRef,-1);
	    code.movl(objRef.ref(),vtable);
	    code.movl(vtable.rdisp(index*4),objRef);
	    regs.freeIntRegister(vtable);
	    
	    if (checkVtable && opts.doParanoidChecks()) {
		code.test(objRef,objRef);
		code.je(createExceptionCall(-8,bcPosition));
	    }
	    
	    if (extraTrace && opts.doTrace()) {
		code.pushl(objRef);
		code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
		code.popl(objRef);
	    }      
	    
	    int ip=code.getCurrentIP();
	    code.call(objRef);
	    regs.freeIntRegister(objRef);
	    
	    node.addDebugInfo(frame.stackMapToString(node));
	    codeStackMap(node,ip);
	    
	    regs.clearActives();
	    codeStackCleanup(offset,result,node.getDatatype());       
	
	    code.endBC();
	}
    }

    public void codeSpecialCall(IMNode node,
				MethodRefCPEntry methodRefCPEntry,
				IMOperant        obj,
				IMOperant[]      args,
				int              datatype,
				Reg              result,
				int              bcPosition) throws CompileException {

	
	// BCClassInfo cinfo = (BCClassInfo) currentlyCompilingClass.getInfo();
	/* invokespecial is used to invoke
	 *  - the constructor <init>
	 *  - a private method of the same class
	 *  - a method in a superclass of this
	 */

	String className  = methodRefCPEntry.getClassName();

	CompilerPlugin plugin; 
	if ((plugin = findPlugin(className))!=null) {
	    if (plugin.code(node, regs, code, methodRefCPEntry,obj,args,datatype,result,bcPosition)) return;
	}

	Reg objRef = regs.chooseIntRegister();

	int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);

	code.startBC(bcPosition);

	regs.freeIntRegister(objRef);	
	regs.saveIntRegister();

	DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(methodRefCPEntry.getClassName(),
								     methodRefCPEntry.getMemberName(),
								     methodRefCPEntry.getMemberTypeDesc()); 
	if (extraTrace && opts.doTrace()) {
	    code.pushl(target);
	    code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
	    code.addl(4,Reg.esp);
	}

	int ip=code.getCurrentIP();
	code.call(target);

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	regs.clearActives();
	codeStackCleanup(offset,result,node.getDatatype());

	code.endBC();
    }

    public void codeInterfaceCall(IMNode node,
				  InterfaceMethodRefCPEntry interfaceRefCPEntry,
				  IMOperant        obj,
				  IMOperant[]      args,
				  int              datatype,
				  Reg              result,
				  int              bcPosition) throws CompileException {

	String className  = interfaceRefCPEntry.getClassName();
	String methodName = interfaceRefCPEntry.getMemberName();
	
	CompilerPlugin plugin; 
	if ((plugin = findPlugin(className))!=null) {
	    if (plugin.code(node, regs, code,
			    interfaceRefCPEntry,obj,args,datatype,result,bcPosition)) return;
	}
	
	/* normal IF call */
	BCClass aClass = classStore.findClass(className);
	if (aClass==null) Debug.out.println("Can't find ClassInfo for "+className);
	BCClassInfo info = (BCClassInfo) aClass.getInfo();

	int index = info.methodTable.getIndex(methodName+interfaceRefCPEntry.getMemberTypeDesc());
	if (index == 0) {
	    System.out.println("Interface method index = 0");
	    info.methodTable.print();
	    throw new CompileException("Interface method index = 0");
	}

	if (index < 0) {
	    info.methodTable.print();
	    throw new Error("method index ("+Integer.toString(index)+") ouf of range!");
	}

	Reg objRef;
	if (opts.isOption("ds-portal") || opts.doDirectSend()) {
	    /* faster portal calls via ecx == index */
	    /* need vtableindex in a known register for direct_send_portal */
	    objRef = regs.chooseIntRegister(Reg.ecx);
	} else {
	    objRef = regs.chooseIntRegister();
	}
	int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);

	code.startBC(bcPosition);

	if (obj.checkReference())
	    codeCheckReference(node,objRef,bcPosition);
	
	regs.freeIntRegister(objRef);
	regs.saveIntRegister();

	// lookup vtable entry
	Reg vtable;
	if (opts.isOption("ds-portal") || opts.doDirectSend()) {
	    Reg ecx = regs.getIntRegister(Reg.ecx);
	    vtable = regs.chooseIntRegister(ecx,objRef);
	    code.movl(index, Reg.ecx);
	    code.movl(objRef.ref(),vtable);
	    //code.movl(vtable.rdisp(index*4),objRef);
	    code.movl(vtable.rdisp(0,ecx,4),objRef);
	} else {
	    vtable = regs.chooseIntRegister(objRef);
	    code.movl(objRef.ref(),vtable);
	    code.movl(vtable.rdisp(index*4),objRef);
	}

	if (checkVtable && opts.doParanoidChecks()) {	    
	    code.test(objRef,objRef);
	    code.je(createExceptionCall(-8,bcPosition));
	}

	if (extraTrace && opts.doTrace()) {
	  code.pushl(objRef);
	  code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
	  code.popl(objRef);
	}

	int ip=code.getCurrentIP();
	//if (direct_send) regs.readIntRegister(ecx);
	code.call(objRef);

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	regs.clearActives();
	codeStackCleanup(offset,result,node.getDatatype());

	code.endBC();
    } 
   
    public void codeStaticCall(IMNode node,
			       MethodRefCPEntry methodRefCPEntry,
			       IMOperant[]      args,
			       int              datatype,
			       Reg              result,
			       int              bcPosition) throws CompileException {
	
	String className  = methodRefCPEntry.getClassName();

	CompilerPlugin plugin; 
	if ((plugin = findPlugin(className))!=null) {
	    if (plugin.code(node, regs, code, methodRefCPEntry,null,args,datatype,result,bcPosition)) return;
	}

	if (className.equals("jx/zero/InitialNaming")) {
	    if (isMethod(methodRefCPEntry,"getInitialNaming","()Ljx/zero/Naming;")) {
		code.startBC(bcPosition);
		regs.saveIntRegister();
		
		int offset=frame.start();
		code.call(new VMSupportSTEntry(VMSupportSTEntry.VM_GETNAMING));
		frame.cleanup(offset);
		
		regs.allocIntRegister(result,Reg.eax,node.getDatatype());
		if (result.value!=0) {
		    code.movl(Reg.eax,result);
		}	
		
		code.endBC();
		return;

	  /*
		regs.allocIntRegister(result,BCBasicDatatype.REFERENCE);		
		code.movl(new DomainZeroSTEntry(),result);
		return;
	  */
	    } else {
		throw new Error("additional static method in InitialNaming not supported");
	    }
	}

	int offset = codeStaticPushArgs(args);
	
	code.startBC(bcPosition);
	
	regs.saveIntRegister();
	
	DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(methodRefCPEntry.getClassName(),
								     methodRefCPEntry.getMemberName(),
								     methodRefCPEntry.getMemberTypeDesc());
	
	if (extraTrace && opts.doTrace()) {
	    code.pushl(target);
	    code.call(new ProfileSTEntry(ProfileSTEntry.PROFILE_TRACE));
	    code.addl(4,Reg.esp);
	}
	
	int ip=code.getCurrentIP();
	code.call(target);
	
	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);
	
	regs.clearActives();
	codeStackCleanup(offset,result,node.getDatatype());
	
	code.endBC();
    }

    public void codeVirtualCallLong(IMNode node,
				    MethodRefCPEntry methodRefCPEntry,
				    IMOperant        obj,
				    IMOperant[]      args,
				    int              datatype,
				    Reg64            result,
				    int              bcPosition) throws CompileException {

	String className  = methodRefCPEntry.getClassName();

	BCClass aClass = classStore.findClass(className);
	if (aClass==null) Debug.out.println("Can't find ClassInfo for "+className);
	BCClassInfo info = (BCClassInfo) aClass.getInfo();
	int index = info.methodTable.getIndex(methodRefCPEntry.getMemberName()+methodRefCPEntry.getMemberTypeDesc());

	Reg objRef = regs.chooseIntRegister(null);
	int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);
	    
	code.startBC(bcPosition);

	if (obj.checkReference())
		codeCheckReference(node,objRef,bcPosition);

	regs.saveOtherIntRegister(objRef);

	// lookup vtable entry
	Reg vtable = regs.chooseAndAllocIntRegister(objRef,-1);
	code.movl(objRef.ref(),vtable);
	code.movl(vtable.rdisp(index*4),objRef);
	regs.freeIntRegister(vtable);

	if (checkVtable && opts.doParanoidChecks()) {
		code.test(objRef,objRef);
		code.je(createExceptionCall(-8,bcPosition));
	}

	int ip=code.getCurrentIP();
	code.call(objRef);
	regs.freeIntRegister(objRef);

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);
	    
	regs.clearActives();
	codeStackCleanupLong(offset,result);       
	
	code.endBC();
    }

    public void codeSpecialCallLong(IMNode node,
				    MethodRefCPEntry methodRefCPEntry,
				    IMOperant        obj,
				    IMOperant[]      args,
				    int              datatype,
				    Reg64            result,
				    int              bcPosition) throws CompileException {

	String className  = methodRefCPEntry.getClassName();

	Reg objRef = regs.chooseIntRegister();

	int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);

	code.startBC(bcPosition);

	regs.freeIntRegister(objRef);	
	regs.saveIntRegister();

	DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(methodRefCPEntry.getClassName(),
								     methodRefCPEntry.getMemberName(),
								     methodRefCPEntry.getMemberTypeDesc()); 
	int ip=code.getCurrentIP();
	code.call(target);

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	regs.clearActives();
	codeStackCleanupLong(offset,result);

	code.endBC();
    }

    public void codeInterfaceCallLong(IMNode node,
				      InterfaceMethodRefCPEntry interfaceRefCPEntry,
				      IMOperant        obj,
				      IMOperant[]      args,
				      int              datatype,
				      Reg64            result,
				      int              bcPosition) throws CompileException {

	String className  = interfaceRefCPEntry.getClassName();
	String methodName = interfaceRefCPEntry.getMemberName();

 	/*	
	CompilerPlugin plugin; 
	if ((plugin = findPlugin(className))!=null) {
	    if (plugin.code(node, regs, code,
			    interfaceRefCPEntry,obj,args,datatype,result,bcPosition)) return;
	}
        */
	
	/* normal IF call */
	BCClass aClass = classStore.findClass(className);
	if (aClass==null) Debug.out.println("Can't find ClassInfo for "+className);
	BCClassInfo info = (BCClassInfo) aClass.getInfo();

	int index = info.methodTable.getIndex(methodName+interfaceRefCPEntry.getMemberTypeDesc());
	if (index == 0) {
	    System.out.println("Interface method index = 0");
	    info.methodTable.print();
	    throw new CompileException("Interface method index = 0");
	}

	if (index < 0) {
	    info.methodTable.print();
	    throw new Error("method index ("+Integer.toString(index)+") ouf of range!");
	}

	Reg objRef;
	if (opts.isOption("ds-portal") || opts.doDirectSend()) {
	    /* faster portal calls via ecx == index */
	    /* need vtableindex in a known register for direct_send_portal */
	    objRef = regs.chooseIntRegister(Reg.ecx);
	} else {
	    objRef = regs.chooseIntRegister();
	}
	int offset = codeVirtualPushArgs(obj,args,objRef,bcPosition);

	code.startBC(bcPosition);

	if (obj.checkReference())
	    codeCheckReference(node,objRef,bcPosition);
	
	regs.freeIntRegister(objRef);
	regs.saveIntRegister();

	// lookup vtable entry
	Reg vtable;
	if (opts.isOption("ds-portal") || opts.doDirectSend()) {
	    Reg ecx = regs.getIntRegister(Reg.ecx);
	    vtable = regs.chooseIntRegister(ecx,objRef);
	    code.movl(index, Reg.ecx);
	    code.movl(objRef.ref(),vtable);
	    //code.movl(vtable.rdisp(index*4),objRef);
	    code.movl(vtable.rdisp(0,ecx,4),objRef);
	} else {
	    vtable = regs.chooseIntRegister(objRef);
	    code.movl(objRef.ref(),vtable);
	    code.movl(vtable.rdisp(index*4),objRef);
	}

	if (checkVtable && opts.doParanoidChecks()) {	    
	    code.test(objRef,objRef);
	    code.je(createExceptionCall(-8,bcPosition));
	}

	int ip=code.getCurrentIP();
	code.call(objRef);

	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);

	regs.clearActives();
	codeStackCleanupLong(offset,result);

	code.endBC();
    }

    public void codeStaticCallLong(IMNode node,
				   MethodRefCPEntry methodRefCPEntry,
				   IMOperant[]      args,
				   int              datatype,
				   Reg64            result,
				   int              bcPosition) throws CompileException {

	String className  = methodRefCPEntry.getClassName();

	int offset = codeStaticPushArgs(args);
	
	code.startBC(bcPosition);
	
	regs.saveIntRegister();
	
	DirectMethodCallSTEntry target = new DirectMethodCallSTEntry(methodRefCPEntry.getClassName(),
								     methodRefCPEntry.getMemberName(),
								     methodRefCPEntry.getMemberTypeDesc());
	
	int ip=code.getCurrentIP();
	code.call(target);
	
	node.addDebugInfo(frame.stackMapToString(node));
	codeStackMap(node,ip);
	
	regs.clearActives();
	codeStackCleanupLong(offset,result);
	
	code.endBC();
    }

    public int codeVirtualPushArgs(IMOperant thisPtr,IMOperant[] args,Reg obj,int bcPosition) throws CompileException {

	RegObj[] results = new RegObj[args.length];

	int offset = frame.start();

	/*
	 * first execute parameters
	 */
	for (int i=0;i<args.length;i++) {
	    int datatype = args[i].getDatatype();
	    switch (datatype) {
	    case BCBasicDatatype.FLOAT:
	    case BCBasicDatatype.DOUBLE:
		codeThrow(thisPtr,-11,bcPosition);
		break;
	    case BCBasicDatatype.LONG:
		if (opts.isOption("long")) {
		    results[i]=regs.chooseLongRegister();
		    args[i].translate(results[i]);
		} else {
                    Debug.out.println("warn: long parameter not supported");
		    code.nop();
		}
		break;	
	    case BCBasicDatatype.INT:
	    case BCBasicDatatype.BOOLEAN:
	    case BCBasicDatatype.CHAR:
	    case BCBasicDatatype.SHORT:
	    case BCBasicDatatype.BYTE:
	    case BCBasicDatatype.REFERENCE:
		if (!(args[i].isConstant() || args[i].isVariable())) {
		    results[i] = regs.chooseIntRegister(null);
		    args[i].translate(results[i]);
		}
		break;
	    default:
		codeThrow(thisPtr,-11,bcPosition);
	    }
	}

	/*
	 * push results
	 */
	for (int i=(args.length-1);i>=0;i--) {
	    int datatype = args[i].getDatatype();
	    switch (datatype) {
	    case BCBasicDatatype.FLOAT:
		frame.push(-1,0);
		break;
	    case BCBasicDatatype.DOUBLE:
		frame.push(-1,0);
		frame.push(-1,0);
		break;
	    case BCBasicDatatype.LONG:
		if (opts.isOption("long")) {
		    regs.readLongRegister((Reg64)results[i]);
		    frame.push((Reg64)results[i]);
		} else {
		    frame.push(-1,0);
		    frame.push(-1,0);
		}
		break;	
	    case BCBasicDatatype.INT:
	    case BCBasicDatatype.BOOLEAN:
	    case BCBasicDatatype.CHAR:
	    case BCBasicDatatype.SHORT:
	    case BCBasicDatatype.BYTE:
	    case BCBasicDatatype.REFERENCE:
		if (args[i].isConstant()) {
		    frame.push(datatype,((IMConstant)args[i]).getIntValue());
		} else if (args[i].isVariable()) {
		    ((IMReadLocalVariable)args[i]).codePush();
		} else {
		    regs.readIntRegister((Reg)results[i]);
		    frame.push(results[i]);		    
		}
	    }
	}

	regs.allocIntRegister(obj,BCBasicDatatype.REFERENCE);
	thisPtr.translate(obj);
	    if (opts.isOption("chkthisptr")) {
		code.test(obj,obj);
		code.je(createExceptionCall(-8,bcPosition));
	    }
	frame.push(BCBasicDatatype.REFERENCE,obj);
	
	return offset;
    }

    public int codeStaticPushArgs(IMOperant[] args) throws CompileException {

	RegObj[] results = new RegObj[args.length];

	int offset = frame.start();

	/*
	
	for (int i=0;i<args.length;i++) {
	    int datatype = args[i].getDatatype();
	    if (!((args[i].isConstant() && datatype==BCBasicDatatype.INT) ||
		(args[i].isVariable()))) {
		results[i] = regs.chooseIntRegister(null);
		args[i].translate(results[i]);
	    }
	}
	
	for (int i=(args.length-1);i>=0;i--) {
	    int datatype = args[i].getDatatype();
	    if (args[i].isConstant() && datatype==BCBasicDatatype.INT) {
	      frame.push(datatype,((IMConstant)args[i]).getIntValue());
	    } else if (args[i].isVariable()) {
	      ((IMReadLocalVariable)args[i]).codePush();
	    } else {
		if (regs.known(results[i])) {
		    regs.readIntRegister(results[i]);
		    frame.push(datatype,results[i]);
		} else {
		    frame.push(-1,0);
		}
	      regs.freeIntRegister(results[i]);
	    }
	}
	*/

	/*
	 * first execute parameters
	 */
	for (int i=0;i<args.length;i++) {
	    int datatype = args[i].getDatatype();
	    switch (datatype) {
	    case BCBasicDatatype.FLOAT:
	    case BCBasicDatatype.DOUBLE:
		code.nop();
		break;
	    case BCBasicDatatype.LONG:
		if (opts.isOption("long")) {
		    results[i]=regs.chooseLongRegister();
		    args[i].translate(results[i]);
		} else {
		    code.nop();
		}
		break;	
	    case BCBasicDatatype.INT:
	    case BCBasicDatatype.BOOLEAN:
	    case BCBasicDatatype.CHAR:
	    case BCBasicDatatype.SHORT:
	    case BCBasicDatatype.BYTE:
	    case BCBasicDatatype.REFERENCE:
		if (!(args[i].isConstant() || args[i].isVariable())) {
		    results[i] = regs.chooseIntRegister(null);
		    args[i].translate(results[i]);
		}
		break;
	    default:
		throw new CompileException("unknown parameter type! "+BCBasicDatatype.toString(datatype));
	    }
	}

	/*
	 * push results
	 */
	for (int i=(args.length-1);i>=0;i--) {
	    int datatype = args[i].getDatatype();
	    switch (datatype) {
	    case BCBasicDatatype.FLOAT:
		frame.push(-1,0);
		break;
	    case BCBasicDatatype.DOUBLE:
		frame.push(-1,0);
		frame.push(-1,0);
		break;
	    case BCBasicDatatype.LONG:
		if (opts.isOption("long")) {
		    regs.readLongRegister((Reg64)results[i]);
		    frame.push((Reg64)results[i]);
		} else {
		    frame.push(-1,0);
		    frame.push(-1,0);
		}
		break;	
	    case BCBasicDatatype.INT:
	    case BCBasicDatatype.BOOLEAN:
	    case BCBasicDatatype.CHAR:
	    case BCBasicDatatype.SHORT:
	    case BCBasicDatatype.BYTE:
	    case BCBasicDatatype.REFERENCE:
		if (args[i].isConstant()) {
		    frame.push(datatype,((IMConstant)args[i]).getIntValue());
		} else if (args[i].isVariable()) {
		    ((IMReadLocalVariable)args[i]).codePush();
		} else {
		    regs.readIntRegister((Reg)results[i]);
		    frame.push(results[i]);		    
		}
	    }
	}

	return offset;
    }

    final public void codeStackCleanup(int entries,Reg result,int datatype) throws CompileException {
	frame.cleanup(entries);
	if (result!=null) {
	  regs.allocIntRegister(result,Reg.eax,datatype);
	  if (!result.equals(Reg.eax)) {
	    code.movl(Reg.eax,result);
	  }
	}
    }

    final public void codeStackCleanupLong(int entries,Reg64 result) throws CompileException {
	frame.cleanup(entries);
	regs.allocLongRegister(result,Reg64.eax);
	if (!result.equals(Reg64.eax)) {
		code.movl(Reg64.eax.low,result.low);
                code.movl(Reg64.eax.high,result.high);
	}
    }

    /** 
	methods for exception managment
    */

    public UnresolvedJump createExceptionCall(int exception,int bcPosition, UnresolvedJump back) {
	ExceptionEntry entry = new ExceptionEntry();
	entry.type = exception;
	entry.bcPosition = bcPosition;
	entry.jump = new UnresolvedJump();
	entry.back_jump = back;
	exceptionStore.addElement(entry);
	return entry.jump;
    }

    public UnresolvedJump createExceptionCall(int exception,int bcPosition) {
	return createExceptionCall(exception, bcPosition, null);
    }

    private void codeExceptionCalls() {
	    container.getStatisticInfo().exception_calls(exceptionStore.size());
	    for (int s=0;s<exceptionStore.size();s++) {
		    ExceptionEntry entry = (ExceptionEntry)exceptionStore.elementAt(s);
		    code.addJumpTarget(entry.jump);
		    code.startBC(entry.bcPosition);
		    if (entry.type==THROW_StackOverflowError) {
			    code.call(new VMSupportSTEntry("vm_stackoverflow"));
		            code.jmp(entry.back_jump);	
		    } else if (entry.type==THROW_ArrayIndexOutOfBounds) {
			    code.call(new VMSupportSTEntry("vm_arrindex"));
		    } else if (entry.type==THROW_NullPointerException) {
			    code.call(new VMSupportSTEntry("vm_nullchk"));
	            } else if (entry.type==THROW_ArithmeticException) {	
			    code.call(new VMSupportSTEntry("vm_arith"));
		    } else {
			    Debug.out.println("type = "+entry.type);	
			    code.pushl(entry.type);
			    code.call(new ExceptionHandlerSTEntry());	    
		    }
		    code.endBC();
	    }
	    exceptionStore.setSize(0);
    }

    /**
       helper
    */

    public void codeStackMap(IMNode node, int InstructionPointer) throws CompileException {
	StackMapSTEntry entry = new StackMapSTEntry(node,InstructionPointer,frame);
	code.insertConst0(entry);
    }

    private int getFieldOffset(FieldRefCPEntry fieldRefCPEntry) {
	BCClass aClass = classStore.findClass(fieldRefCPEntry.getClassName());
	BCClassInfo info = (BCClassInfo)aClass.getInfo();
	return (1  + info.objectLayout.getFieldOffset(fieldRefCPEntry.getMemberName())) * 4;
    }

    private boolean isMethod(MethodRefCPEntry method,String name,String sig) {
	if (method.getMemberName().equals(name) && method.getMemberTypeDesc().equals(sig)) return true;
	return false;
    }

    private void initPlugins() {
	plugins = new Hashtable();

	/* interfaces */
	plugins.put("jx/zero/Memory", new jx.compiler.plugins.Memory(this));
	plugins.put("jx/zero/ReadOnlyMemory", new jx.compiler.plugins.ReadOnlyMemory(this));
	plugins.put("jx/zero/InterceptOutboundInfo", new jx.compiler.plugins.InterceptOutboundInfo(this));
        plugins.put("jx/zero/InterceptInboundInfo",  new jx.compiler.plugins.InterceptInboundInfo(this));
	plugins.put("jx/zero/VMSupport", new jx.compiler.plugins.VMSupport(this));
	plugins.put("jx/zero/CAS", new jx.compiler.plugins.CompareAndSwap(this));

	/* classes */
	plugins.put("jx/zero/Debug", new jx.compiler.plugins.Debug(this));

	if (opts.doOptimize()) {
	    plugins.put("jx/zero/Ports", new jx.compiler.plugins.Ports(this));
	    //plugins.put("jx/zero/VMClass", new jx.compiler.plugins.VMClass(this));
	    plugins.put("jx/zero/CPUManager", new jx.compiler.plugins.CPUManager(this));
	}
    }

    private CompilerPlugin findPlugin(String className) {
	return (CompilerPlugin)plugins.get(className);
    }
}
