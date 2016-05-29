package jx.compiler.imcode;

import java.util.Vector;

import jx.zero.Debug;

import jx.classfile.datatypes.*;
import jx.classfile.constantpool.*;
import jx.classfile.*;
import jx.classfile.BC;

import jx.compiler.CompileException;
import jx.compiler.StatisticInfo;
import jx.compiler.execenv.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;

class JumpStack {
    private IMBasicBlock[] stack;
    private int pos;

    public JumpStack() {
	stack = new IMBasicBlock[60];
	pos = 0;
    }

    public void push(IMBasicBlock node) {
	stack[pos++] = node;
	if (pos==stack.length) {
	    //System.err.println("work stack to small - realloc !!");
	    IMBasicBlock[] new_stack = new IMBasicBlock[stack.length+10];
	    for (int i=0;i<stack.length;i++) new_stack[i] = stack[i];
	    stack = new_stack;
	    new_stack=null;
	}
    }

    public IMBasicBlock pop() {
	if (pos<=0) {
	    //System.err.println("no more work!");
	    return null;
	}
	return stack[--pos];
    }
}

public class CodeContainer implements NativeCodeContainer {

    private static int id=0;
    private int myid;

    private IMNode imCodeStart;
    private IMNode imCodeEnd;
    private IMNode current;
    private int    numberOfInstr;

    private IMBasicBlock  label_list;
    private IMNode        bc_list_top;
    private IMNode        bc_list_end;

    private BCMethodWithCode method;
    private MethodStackFrame frame;
    private ConstantPool     cPool;
    private BinaryCodeIA32   code;
    private RegManager       regs;
    private ExecEnvironmentInterface execEnv;
    private CompilerOptionsInterface opts;
    //private StackFrame       stackFrame;

    private IMBasicBlock     epilog;
    private int              numberOfBasicBlocks;

    private boolean          smallMethod      = true;
    private boolean          leafMethod       = true;
    private boolean          experimentalCode = true;

    private ExceptionHandlerData[] handler;
    private IMBasicBlock[]         expHandlerList;

    private StatisticInfo stat;

    public CodeContainer(ExecEnvironmentInterface execEnv,
			 BCMethodWithCode method) {
	this(execEnv,method,new StatisticInfo());
    }

    public CodeContainer(ExecEnvironmentInterface execEnv,
			 BCMethodWithCode method,
			 StatisticInfo stat) {
	// CodeContainer
	imCodeStart=null;
	imCodeEnd=null;
	current=null;
	numberOfInstr=0;
	this.method = method;
	this.stat = stat;
	myid = id++;
	numberOfBasicBlocks=0;

	// Coder
	bc_list_top = null;
	bc_list_end = null;
	this.cPool = method.getConstantPool();
	this.handler = method.getExceptionHandlers();

	// NativeCodeContainer
	code = new BinaryCodeIA32();
	//execEnv    = method.getExecEnvironment();
	//execEnv    = new ExecEnvironment(classFinder,opts);
	this.execEnv = execEnv;	
	this.opts    = execEnv.getCompilerOptions();

	frame = new MethodStackFrame(code,method);
    }

    public void setNoTranslate() {
	method = null;
	regs   = null;
	code   = null;
	frame  = null;
    }

    public int getID() {
	return myid;
    }

    public void init() throws CompileException {
	regs = new RegManager();
	regs.init(this);
	epilog = new IMBasicBlock(this,true);
	readBCInstruction(method.getBytecodeStream());
    }

    public BCMethod getBCMethod() {
	return (BCMethod)method;
    }

    public String getClassName() {
	return method.getClassName();
    }

    public String getMethodName() {
	return method.getName();
    }

    public StatisticInfo getStatisticInfo() {
	return stat;
    }

    public ExecEnvironmentInterface getExecEnv() {
	return execEnv;
    }

    public CompilerOptionsInterface getOpts() {
	return opts;
    }

    public MethodStackFrame getMethodStackFrame() {
	return frame;
    }

    public int getLocalVarSize() {
	return frame.getStackFrameSize()/4;
    }

    public ConstantPool getConstantPool() {
	return cPool;
    }

    public BinaryCodeIA32 getIA32Code() {
	return code;
    }

    public jx.compiler.nativecode.RegManager getRegManager() {
	return regs;
    }

    public BinaryCode getMachineCode() {
	return code.getOldBinaryCode();
    }

    public Vector getInstructionTable() {
	return code.getInstructionTable();
    }

    public int getNumberOfBasicBlocks() {
	return  numberOfBasicBlocks;
    }

    public IMBasicBlock getEpilog() {
	return epilog;
    }

    public boolean isSmallMethod() {
	return smallMethod;
    }

    public boolean isLeafMethod() {
	return leafMethod;
    }

    public void readBCInstruction(BytecodeInputStream bcStream) throws CompileException {
	boolean hasWidePrefix=false;
	int current,ip;
	IMBasicBlock des;

	// ==========================================
	// 1. PASS
	// ==========================================
	// in the first pass we will translate the 
	// bytecode into imcode-Objects, and create
        // the basicblocks and labels
	// ==========================================

	IMBasicBlock basicBlock = createBasicBlock(bcStream.getCurrentPosition(),0);

	expHandlerList = null;
	if (opts.doExceptions()) {
	    if (handler!=null && handler.length>0) {
		expHandlerList = new IMBasicBlock[handler.length];
		for (int i=0;i<handler.length;i++) {
		    createBasicBlock(handler[i].getStartBCIndex(),0);
		    createBasicBlock(handler[i].getEndBCIndex(),0);
		    expHandlerList[i] = createBasicBlock(handler[i].getHandlerBCIndex(),0);
		    expHandlerList[i].setExceptionHandler(new ExceptionTableSTEntry(cPool,handler[i]));
		}
	    } 
	}

	while (bcStream.hasMore()) {

	    current=bcStream.readUnsignedByte();

	    ip = bcStream.getCurrentPosition()-1;

	    if (current==BC.WIDE) {
		hasWidePrefix=true;
		current=bcStream.readUnsignedByte();
		ip = bcStream.getCurrentPosition();		
	    } else {
		hasWidePrefix=false;
	    }	

	    // should we implement the nop operation ?!?
	    // public static const int BC.NOP 0x00;
	    if (current==BC.NOP) {
		insertBCList(new IMNop(this,ip));
		continue;
	    }
	    
	    // immediate constants
	    if (current>=BC.ACONST_NULL && current<=BC.SIPUSH) {
		IMConstant opr = null;
		if (current<=BC.DCONST_1) {
		    opr = new IMConstant(this,current,ip);
		} else if (current==BC.BIPUSH) {
		    int bi = (int) bcStream.readByte();
		    opr = new IMConstant(this,current,ip,bi);
		} else if (current==BC.SIPUSH) {
		    int si = bcStream.readShort();
		    opr = new IMConstant(this,current,ip,si);
		}
		insertBCList(opr);
		continue;
	    }
	    
	    // constants form constantpool
	    // read constants from pool [index 1-2]
	    if (current>=BC.LDC && current<=BC.LDC2_W) {
		int cpIndex;

		if (current==BC.LDC) cpIndex=bcStream.readUnsignedByte();
		else cpIndex = bcStream.readUnsignedShort();

		//Debug.out.println("read constant "+cpIndex);
		ConstantPoolEntry cpEntry = cPool.constantEntryAt(cpIndex);
		
		/*
		IMConstantRef opr = new IMConstantRef(this,current,ip,cpEntry);
		*/

		IMConstant opr = new IMConstant(this,current,ip,cpEntry);

		insertBCList(opr);
		continue;
	    }

	    // read local variables
	    if (current>=BC.ILOAD && current<=BC.ALOAD_3) {

		IMReadLocalVariable opr= null;

		if (current<=BC.ALOAD) {
		    int vi;

		    if (hasWidePrefix) vi = bcStream.readUnsignedShort();
		    else vi = bcStream.readUnsignedByte();

		    opr=new IMReadLocalVariable(this,current,ip,vi);
		} else {
		    opr=new IMReadLocalVariable(this,current,ip);
		} 

		insertBCList(opr);
		continue;
	    }

	    if (current==BC.IINC) {
		int vIndex,value;
		
		if (hasWidePrefix) {
		    vIndex = bcStream.readUnsignedShort();
		    value  = bcStream.readUnsignedShort();
		} else {
		    vIndex = bcStream.readUnsignedByte();
		    value  = bcStream.readByte();
		}
	        insertBCList(new IMInc(this,current,ip,vIndex,value));
		continue;
	    }
				
	    // read array values
	    if (current>=BC.IALOAD && current<=BC.SALOAD) {
		IMReadArray opr = new IMReadArray(this,current,ip);
		insertBCList(opr);
		continue;
	    }
	    
	    if (current>=BC.ISTORE && current<=BC.ASTORE_3) {
		IMStoreLocalVariable opr = null;

		if (current<=BC.ASTORE) {
		    int vi;
		    if (hasWidePrefix) {
			vi = bcStream.readUnsignedShort();
		    } else {
			vi = bcStream.readUnsignedByte();
		    }
		    opr = new IMStoreLocalVariable(this,current,ip,vi);
		} else {
		    opr = new IMStoreLocalVariable(this,current,ip); 
		}
		insertBCList(opr);
		continue;
	    }

	    if (current>=BC.IASTORE && current<=BC.SASTORE) {
		IMStoreArray opr = new IMStoreArray(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    if ( current>=BC.POP && current<=BC.SWAP) {
		IMStackOperation opr = new IMStackOperation(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    // arithmetic types
	    // IMArithmetic
	    if (current<=BC.DREM) {
		IMOperator opr = null;
		if (current<=BC.DADD) {
		    opr = new IMAdd(this,current,ip);
		} else if (current<=BC.DSUB) {
		    opr = new IMSub(this,current,ip);
		} else if (current<=BC.DMUL) {
		    opr = new IMMul(this,current,ip);
		} else if (current<=BC.DDIV) {
		    opr = new IMDiv(this,current,ip);
		} else {
		    opr = new IMRem(this,current,ip);
		}
		insertBCList(opr);
		continue;
	    }

	    if (current<=BC.DNEG) {
		IMNeg opr = new IMNeg(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    if (current<=BC.LXOR) {
		//IMBitOperator opr = new IMBitOperator(this,current,ip);
		IMBitOperator opr = IMBitOperator.getIMOperator(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    // type casting
	    if (current<=BC.I2S) {
		IMCast opr = new IMCast(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    // compare long,float and double read spec carefuly !!!
	    if (current<=BC.DCMPG) {
		IMCompare opr = new IMCompare(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    // conditinal branch
	    if (current<=BC.IF_ACMPNE ||
		current==BC.IFNULL ||
		current==BC.IFNONNULL) {

		int offset = bcStream.readShort();

		des = createBasicBlock(ip,offset);
		
		IMConditionalBranch opr = new IMConditionalBranch(this,current,ip,des);

		if (offset<0) des.setLoopEntry(true);

		insertBCList(opr);
		continue;
	    }
	    
	    if (current==BC.GOTO || current==BC.GOTO_W) {
		int offset;

		if (current==BC.GOTO) offset=bcStream.readShort();
		else offset=bcStream.readInt();

		des = createBasicBlock(ip,offset);

		IMGoto opr = new IMGoto(this,current,ip,des);

		insertBCList(opr);
		continue;
	    }

	    if (current==BC.JSR || current==BC.JSR_W) {
		int offset;

		if (current==BC.JSR) offset=bcStream.readShort();
		else offset=bcStream.readInt();

		des = createBasicBlock(ip,offset);

		IMCall    opr  = new IMCall(this,current,ip,des);

		insertBCList(opr);
		continue;
	    }

	    if (current==BC.RET) {
		int vindex;

		if (hasWidePrefix) {
		    vindex = bcStream.readUnsignedShort();
		} else {
		    vindex = bcStream.readUnsignedByte();
		}

		IMReturnSubroutine opr = new IMReturnSubroutine(this,current,ip,vindex);

		insertBCList(opr);
		continue;
	    }
		
	    if (current==BC.TABLESWITCH) {

		// skip padding zeroes
		while (bcStream.getCurrentPosition() % 4 != 0) bcStream.readByte();

		IMBasicBlock doff = createBasicBlock(ip,bcStream.readInt());

		int low  = bcStream.readInt();
		int high = bcStream.readInt();
		int size = high - low + 1;

		IMBasicBlock[] offsets = new IMBasicBlock[size];

		for (int i=0;i<size;i++) {
		    offsets[i] = createBasicBlock(ip,bcStream.readInt());
		}		

		IMTableSwitch opr = new IMTableSwitch(this,current,ip,doff,low,high,offsets);

		insertBCList(opr);		
		continue;
	    }

	    if (current==BC.LOOKUPSWITCH) {
		
		// skip padding zeroes
		while (bcStream.getCurrentPosition() % 4 != 0) bcStream.readByte();

		IMBasicBlock doff   = createBasicBlock(ip,bcStream.readInt());
		int npairs = bcStream.readInt();

		int[] mpairs = new int[npairs];
		IMBasicBlock[] opairs = new IMBasicBlock[npairs];

		for (int i=0;i<npairs;i++) {
		    mpairs[i] = bcStream.readInt();
		    opairs[i] = createBasicBlock(ip,bcStream.readInt());
		}		
		
		IMLookupSwitch opr = new IMLookupSwitch(this,current,ip,doff,npairs,mpairs,opairs);

		insertBCList(opr);		
		continue;
	    }

	    if (current<=BC.RETURN) {		
		IMReturn opr = new IMReturn(this,current,ip);
		insertBCList(opr);		
		continue;
	    }

	    if (current<=BC.PUTFIELD) {
		int cpIndex = bcStream.readUnsignedShort();
		FieldRefCPEntry cpEntry = cPool.fieldRefEntryAt(cpIndex);
		IMNode opr;

		if (current==BC.GETFIELD) {
		    opr = new IMGetField(this,current,ip,cpEntry);
		} else if (current==BC.GETSTATIC) {
		    opr = new IMGetStatic(this,current,ip,cpEntry);
		} else if (current==BC.PUTFIELD) {
		    opr = new IMPutField(this,current,ip,cpEntry);
		} else {
		    opr = new IMPutStatic(this,current,ip,cpEntry);
		}

		insertBCList(opr);
		continue;
	    }

	    if (current<BC.INVOKEINTERFACE) {
		int cpIndex = bcStream.readUnsignedShort();
		MethodRefCPEntry cpEntry = cPool.methodRefEntryAt(cpIndex);

		IMInvoke opr=null;
		switch (current) {
		case BC.INVOKEVIRTUAL:
		    opr = new IMInvokeVirtual(this,current,ip,cpEntry);
		    break;
		case BC.INVOKESPECIAL:
		    opr = new IMInvokeSpecial(this,current,ip,cpEntry);
		    break;
		case BC.INVOKESTATIC:
		    opr = new IMInvokeStatic(this,current,ip,cpEntry);
		    break;
		default:
		    throw new CompileException("unknown invokation -- not implemented yet! ");
		}

		leafMethod=false;

		insertBCList(opr);
		continue;
	    }

	    if (current==BC.INVOKEINTERFACE) {
		int cpIndex = bcStream.readUnsignedShort();

		int args_size=bcStream.readByte(); // read usless arg length
		bcStream.readByte(); // read usless padding zero

		InterfaceMethodRefCPEntry cpEntry = cPool.InterfaceMethodRefEntryAt(cpIndex);
		IMInvokeInterface opr = new IMInvokeInterface(this,current,ip,cpEntry); // pop 1-x

		leafMethod=false;

		insertBCList(opr);
		continue;
	    }
	    
	    if (current==BC.UNUSED) continue;

	    if (current==BC.NEW) {
		int cpIndex = bcStream.readUnsignedShort();
		ClassCPEntry cpEntry = cPool.classEntryAt(cpIndex);		
		IMNew opr = new IMNew(this,current,ip,cpEntry);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.NEWARRAY) {
		int aType = bcStream.readByte();
		switch (aType) {
		case 4:
		  aType = BCBasicDatatype.BOOLEAN;
		  break;
		case 5:
		  aType = BCBasicDatatype.CHAR;
		  break;
		case 6:
		  aType = BCBasicDatatype.FLOAT;
		  break;
		case 7:
		  aType = BCBasicDatatype.DOUBLE;
		  break;
		case 8:
		  aType = BCBasicDatatype.BYTE;
		  break;
		case 9:
		  aType = BCBasicDatatype.SHORT;
		  break;
		case 10:
		  aType = BCBasicDatatype.INT;
		  break;
		case 11:
		  aType = BCBasicDatatype.LONG;
		  break;
		}
		IMNewArray opr = new IMNewArray(this,current,ip,aType);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.ANEWARRAY) {
		int cpIndex = bcStream.readUnsignedShort();
		ClassCPEntry cpEntry = cPool.classEntryAt(cpIndex);
		IMNewObjArray opr = new IMNewObjArray(this,current,ip,cpEntry);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.ARRAYLENGTH) {
		IMArrayLength opr = new IMArrayLength(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.ATHROW) {
		IMThrow opr = new IMThrow(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.CHECKCAST) {
		int cpIndex = bcStream.readUnsignedShort();
		ClassCPEntry cpEntry = cPool.classEntryAt(cpIndex);
		
		IMCheckCast opr = new IMCheckCast(this,current,ip,cpEntry); 
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.INSTANCEOF) {
		int cpIndex = bcStream.readUnsignedShort();
		ClassCPEntry cpEntry = cPool.classEntryAt(cpIndex);

		IMInstanceOf opr = new IMInstanceOf(this,current,ip,cpEntry);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.MONITORENTER || 
		current==BC.MONITOREXIT) {

		IMMonitor opr = new IMMonitor(this,current,ip);
		insertBCList(opr);
		continue;
	    }

	    if (current==BC.MULTIANEWARRAY) {
		int cpIndex = bcStream.readUnsignedShort();
		int dim     = bcStream.readUnsignedByte();
		ClassCPEntry cpEntry = cPool.classEntryAt(cpIndex);
		
		IMNewMultiArray opr = new IMNewMultiArray(this,current,ip,cpEntry,dim);

		insertBCList(opr);
		continue;
	    }

	    throw new CompileException("unknown bytecode ("+Integer.toString(current)+")");
	    // Reserved opcodes:
	    //    public static const int BC.BREAKPOINT 0xca;
	    //    public static const int BC.IMPDEP1 0xfe;
	    //    public static const int BC.IMPDEP2 0xff;
	}

	// ===============================================
	// 2. Pass
	// ===============================================
	// merge BasicBlockList and ByteCodeList
	// ===============================================

	IMNode node = bc_list_top;
	if (label_list!=null && label_list.getBCPosition()<=node.getBCPosition()) {
	    // remove from labellist
	    basicBlock = label_list;
	    label_list = (IMBasicBlock)label_list.bc_next;
	    // insert basicblock befor bytcodelist
	    basicBlock.bc_next = bc_list_top;
	    bc_list_top = basicBlock;
	}

	//System.err.println(node.toString());
	while (node.bc_next!=null) {
	    int bcpos = node.bc_next.getBCPosition();
	    while (label_list!=null) {
		if (label_list.getBCPosition()<=bcpos) {
		    // remove from labellist
		    basicBlock = label_list;
		    label_list = (IMBasicBlock)label_list.bc_next;
		    // insert befor node.bc_next
		    basicBlock.bc_next = node.bc_next;
		    node.bc_next = basicBlock;
		} else if (label_list.getBCPosition() < bcpos) {
		    // remove labels which not match
		    System.err.println("delete label "+label_list);
		    label_list = (IMBasicBlock)label_list.bc_next; 
		} else {
		    break;
		}
		numberOfBasicBlocks++;
	    }
	    node=node.bc_next;
	} 	

	// ===============================================
	// 3. Pass
	// ===============================================
	// now we will process the virtual operanten stack 
	// ===============================================

	VirtualOperantenStack oprStack = new VirtualOperantenStack(this);
	JumpStack stack = new JumpStack();

	basicBlock = null;
	if (bc_list_top.isBasicBlock()) {
	    basicBlock=(IMBasicBlock)bc_list_top;
	}

	int expHandler=0;
	if (opts.doExceptions()) {
	    if (expHandlerList!=null) expHandler=expHandlerList.length;
	}

	while (basicBlock!=null) {
	    join(basicBlock);

	    basicBlock.processBasicBlock(oprStack);
	    IMBasicBlock[] nextBB = basicBlock.getSucc();
	    IMOperant[]    leave  = basicBlock.getLeaveStack();

	    if (basicBlock.isSubroutine()) {
	        basicBlock=stack.pop();
		if (basicBlock!=null) {
		    basicBlock.setInitStack(leave);
		    continue;
		}
		//System.err.println("FIN 1");
		break;
	    }

	    if (nextBB!=null) {
		for (int i=nextBB.length-1;i>=0;i--) {
		    if (nextBB[i]!=null) {
			//System.err.println(" -> "+nextBB[i]);
			nextBB[i].setInitStack(leave);
			stack.push(nextBB[i]);
		    } else {
			System.err.println("nextBB["+Integer.toString(i)+"]==null\n");
		    }
		}
	    }
	    basicBlock=stack.pop();
	    while (basicBlock!=null && basicBlock.done) {
		//System.err.println(basicBlock+" already done");
		basicBlock=stack.pop();
	    }

	    if (basicBlock==null && expHandler>0) {
		IMOperant[] enterStack = new IMOperant[1];
		enterStack[0] = new IMCaughtException(this);
		basicBlock = expHandlerList[--expHandler];
		basicBlock.setInitStack(enterStack);
	    }
	    //if (basicBlock==null) System.err.println("FIN 2");
	}
	
	// ===============================================
	// 4. Pass
	// ===============================================
	// found special code pattern
	// ===============================================

	IMNode inode = imCodeStart;
	//IMBasicBlock curr = null;
	int ic=0;
	smallMethod = true;
	
	while (inode!=null) {	    
	    if (inode.isBasicBlock()) {
		basicBlock = (IMBasicBlock)inode;
	    } else if (inode.isInstruction()) {

		if (inode.isThrow()) basicBlock.setLowPriorityPath();

		if (inode.isBlockVariable() && inode.next!=null) optPatternBlockVar((IMStoreBlockVariable)inode);

		if (experimentalCode && opts.doOptimize()) {
		  if (inode instanceof IMGoto) {
		    IMGoto igoto = (IMGoto)inode;
		    IMBasicBlock targets[] = igoto.getTargets();
		    
		    /* nur 91 faelle
		    if (targets[0].next instanceof IMGoto) {
		      igoto.addDebugInfo("forward goto");
		    }
		    */

		    if (targets[0].next.isReturn()) {
			igoto.addDebugInfo("forward return");			
		    }
		  }
		}

		// test if it is a "small method"
		if (smallMethod) {
		    // void-returns won't count
		    if (inode.isReturn()) {
			if (((IMReturn)inode).isVoidReturn()) ic--;
		    }
		    ic++;
		    if (ic>1) smallMethod=false;
		}
	    } /* instuction */	    
	    inode = inode.next;
	} /* while */

	append(epilog);

	// ===============================================
	// 5. Pass
	// ===============================================
	// resort basic blocks
	// ===============================================

	if (opts.isOption("O5")) {
	    inode=imCodeStart;	
	    while (inode!=null) {	    
		if (inode.isBasicBlock()) {
		    basicBlock = (IMBasicBlock)inode;
		} else if (inode.isInstruction() && inode.next!=null) {
		    if (opts.doOptExecPath() || opts.isOption("path_"+method.getClassName()+"."+method.getName())) {
			if (inode.isEndOfBasicBlock() &&
			    inode.isBranch() &&
			    inode.next.isLowPriorityPath()) {
			    
			    if (inode instanceof IMConditionalBranch) {
				IMConditionalBranch bnode = (IMConditionalBranch)inode;
				IMBasicBlock succ[] = bnode.getTargets();
				
				if (opts.doVerbose("path")) {
				    Debug.out.println("o:"+bnode.toReadableString());
				}
				
				try {
				    bnode.addDebugInfo("swap jump "+succ[0].toLabel()+" "+succ[1].toLabel());
				} catch (Exception ex) {
				    bnode.addDebugInfo("swap jump");
				}
				
				moveBasicBlockToEnd(succ[0]);
				
				bnode.swapJumpTargets();
				
				if (opts.doVerbose("path")) {
				    Debug.out.println("n:"+bnode.toReadableString());
				}
			    }
			}
		    }
		}
		inode = inode.next;
	    } /* while */
	}
    }

    private void moveBasicBlockToEnd(IMNode basicblock) {
	IMNode prev_node= basicblock.prev;
	IMNode end_node = basicblock;

	while (!end_node.next.isBasicBlock()) end_node=end_node.next;

	basicblock.prev = imCodeEnd;
	imCodeEnd.next  = basicblock;

	end_node.next.prev = prev_node;
	prev_node.next     = end_node.next;

	imCodeEnd          = end_node;
	end_node.next      = null;	
    }

    /*
      b0 = ... ;
    */
    private void optPatternBlockVar(IMStoreBlockVariable bvar) {	
	//IMStoreBlockVariable bvar = (IMStoreBlockVariable)inode;
	IMOperant opr = bvar.getOperant();
	if ((opr instanceof IMNew) && (bvar.next instanceof IMInvoke)) {	    
	    /* 
	       b0 = new ... ;
	       b0.<init>() ;   ==> b0 = new ...() ;
	       ...
	    */
	    //System.err.println(" bn.<init>() ");
	    IMInvoke initInstr = (IMInvoke)bvar.next;
	    IMCompactNew inew = initInstr.getCompactNew(bvar.getBlockVarIndex(),(IMNew)opr);
	    if (inew!=null) {
		/*
		  inew.prev = inode.prev;
		  inew.next = inode.next.next;
		  inew.prev.next = inew;
		  inew.next.prev = inew;
		*/
		bvar.setOperant(inew);
		bvar.next = bvar.next.next;
		bvar.next.prev = bvar;
		
		if (experimentalCode && opts.doOptimize()) {
		    
		    /*
		      b0 = new ..()
		      vn = b0; | throw b0; | return b0;
		      remove b0 if b0 is dead
		    */
		    
		    if (bvar.next instanceof IMStoreLocalVariable) {
			IMStoreLocalVariable lstore = (IMStoreLocalVariable)bvar.next;
			
			if (lstore.getOperant().isBlockVariable()) {
			    
			    lstore.prev = bvar.prev;
			    lstore.prev.next = lstore;
			    
			    lstore.setOperant(inew);
			    
			    //lstore.addDebugInfo("bn forwarded ");
			}
		    }				
		    if (bvar.next instanceof IMThrow) {
			IMThrow lthrow = (IMThrow)bvar.next;
			
			lthrow.prev = bvar.prev;
			lthrow.prev.next = lthrow;
			
			lthrow.setOperant(inew);
			
			//lthrow.addDebugInfo("bn forwarded ");
		    }
		    if (bvar.next instanceof IMReturn) {
			IMReturn  ret = (IMReturn) bvar.next;
			IMOperant ropr = ret.getOperant();
			
			if (ropr!=null && ropr.isBlockVariable()) {
			    ret.prev = bvar.prev;
			    ret.prev.next = ret;
			    
			    ret.setOperant(inew);
			    //bvar.next.addDebugInfo("may forwarded ");
			}
		    }
		    
		}
	    }
	}
    }


    public void join(IMNode node) {

	/*
	if (method.getName().equals("expandTable")) {
	    System.out.println("join "+Integer.toString(node.getBCPosition())+": "+node.toReadableString());
	    //try { Thread.sleep(100); } catch (Exception ex) {}
	}
	*/

	if (node.isInstruction()) numberOfInstr++;

	if (imCodeStart==null) {
	    insertBefore(null,node);
	} else {
	    int node_bcpos = node.getBCPosition();	    
	    for (IMNode curr=top();curr!=null;curr=next(curr)) {
		int curr_bcpos = curr.getBCPosition();

		if (curr_bcpos<node_bcpos) continue;

		if (curr_bcpos>node_bcpos) {
		    insertBefore(curr,node);
		    return;
		}

		// curr_bcpos == node_bcpos
		if (curr.isBasicBlock() || curr.isBlockVariable()) {
		    //insertBehind(curr,node);
		    continue;
		} else {
		    insertBefore(curr,node);
		}

		return;
	    }

	    insertBehind(null,node);
	}
    }
	    
    public IMNode top() {
	return imCodeStart;
    }

    public IMNode next(IMNode curr) {
	if (curr!=null) return curr.next;
	return null;
    }

    public IMNode prev(IMNode curr) {
	if (curr!=null) return curr.prev;
	return null;
    }

    public IMNode end() {
	return imCodeEnd;
    }

    public void insertBefore(IMNode curr,IMNode entry) {
	//if (entry==null) throw new Error("null");
	if (imCodeStart==null && imCodeEnd==null) {
	    imCodeStart = entry;
	    imCodeEnd   = entry;
	    entry.prev=null;
	    entry.next=null;
	    return;
	}	    
	if (curr==null) curr=imCodeStart;
	if (curr.prev==null) {
	    if (imCodeStart!=curr) throw new Error("mixup");
	    entry.prev=null;
	    imCodeStart=entry;
	} else {
	    entry.prev = curr.prev;
	    entry.prev.next = entry;
	}
	curr.prev = entry;
	entry.next = curr;	
    }

    public void insertBehind(IMNode curr,IMNode entry) {
	//if (entry==null) throw new Error("null");
	if (imCodeStart==null && imCodeEnd==null) {
	    imCodeStart = entry;
	    imCodeEnd   = entry;
	    entry.prev=null;
	    entry.next=null;
	    return;
	}
	if (curr==null) curr=imCodeEnd;
	if (curr.next==null) {
	    if (imCodeEnd!=curr) throw new Error("mixup");
	    entry.next=null;
	    imCodeEnd=entry;
	} else {
	    entry.next = curr.next;
	    entry.next.prev = entry;
	}
	curr.next = entry;
	entry.prev = curr;
    }    

    /**

     */

    public void inlineMethods(int depth) throws CompileException {
	inlineMethods(depth,opts.forceInline(method));
    }

    public void inlineMethods(int depth, boolean forceInline) throws CompileException {
	IMNode node = imCodeStart;

	if (depth<=0) return;

	while (node!=null) {
	    if (node.isInstruction()) {
		
		IMNode inode = (IMNode)node;
		//if (depth<4) System.err.println(inode.toReadableString());
		CodeVector inlineCode = new CodeVector();

		IMNode newinode = inode.inlineCode(inlineCode, depth, forceInline);

		if (newinode!=null && opts.doExtraFalting()) {
		    IMNode newnode = newinode.constant_folding();
		    if (newnode instanceof IMNode) {
			newinode = (IMNode)newnode;
		    } else {
			Debug.out.println("!!! bad instr node in CodeContainer");
		    }
		}

		for (int i=0;i<inlineCode.size();i++) {
		    CodeContainer inCode = inlineCode.element(i);
		    IMNode top = inCode.getTop();
		    IMNode end = inCode.getEnd();
		    node.prev.next = top;
		    top.prev       = node.prev;		    
		    end.next       = node;
		    node.prev      = end;	    
		}

		if (newinode!=null && newinode!=inode) {
		    //System.err.println("change node");
		    if (node.prev!=null) node.prev.next = newinode;
		    newinode.prev  = node.prev;
		    if (node.next!=null) node.next.prev = newinode;
		    newinode.next  = node.next;
		    node = newinode;
		}
	    }
	    node = node.next;
	}

	if (opts.isOption("cfor")) constant_forwarding();
    }

    /** 
	Small methods are real small. They have only one
	instruction node, no local variable and all arguments
	must be constant.

	see IMInvoke.inline() for details
    */
    /*
    public IMNode inlineSmallMethod() {
	IMNode node = imCodeStart;
	if (node==null || node.next==null) {
	    throw new Error("this is not a small method");
	}
	return (IMNode)node.next;
    }
    */

    public IMNode transformSmallMethodForInline(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {	
	IMNode node = imCodeStart;

	this.cPool     = newContainer.getConstantPool();
	//this.code      = newContainer.getIA32Code();
	this.code      = null;
	this.regs      = newContainer.getRegManager();
	this.frame     = newContainer.getMethodStackFrame();
	this.execEnv   = newContainer.getExecEnv();

	if (!smallMethod) throw new Error("this is not a small Method");
	if (node==null || node.next==null) {
	    throw new Error("this is not a small method");
	}
	node=node.next;

	IMNode newnode =  (IMNode)node.assignNewVars(newContainer,slots,opr,retval,bcPos);
	if (newnode!=null) {
	    newnode.constant_folding();
	    return newnode;
	} else {
	    return null;
	}
    }

    public void transformForInline(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {	
	IMNode node = imCodeStart;

	this.cPool     = newContainer.getConstantPool();
	//this.code      = newContainer.getIA32Code();
	this.code      = null;
	this.regs      = newContainer.getRegManager();
	this.frame     = newContainer.getMethodStackFrame();
	this.execEnv   = newContainer.getExecEnv();

	while (node!=null) {
	    if (node.isReturn()) {
		IMCodeVector inCode = new IMCodeVector();
		((IMReturn)node).assignNewVars(newContainer,slots,opr,retval,inCode,bcPos);
		for (int i=0;i<inCode.size();i++) {
		    IMNode newnode = inCode.element(i);
		    newnode.prev = node.prev;
		    newnode.next = node.next;
		    if (node.prev==null) {
			imCodeStart = newnode;
		    } else {
			node.prev.next = newnode;
		    }
		    if (node.next==null) {
			imCodeEnd = newnode;
		    } else {
			node.next.prev = newnode;
		    }
		}
	    } else {
		IMNode newnode = node.assignNewVars(newContainer,slots,opr,retval,bcPos);
		newnode.prev = node.prev;
		newnode.next = node.next;
		if (node.prev==null) {
		    imCodeStart = newnode;
		} else {
		    node.prev.next = newnode;
		}
		if (node.next==null) {
		    imCodeEnd = newnode;
		} else {
		    node.next.prev = newnode;
		}
	    }
	    node = node.next;
	}		
    }

    public void constant_forwarding() throws CompileException {
	IMNode node=imCodeStart;

	//======================================
	// forward constants all basic blocks
	//======================================

	while (node!=null) {
	    if (node.isBasicBlock()) {
		((IMBasicBlock)node).constant_forwarding();
		node.constant_folding();
		//node.constant_forwarding();
	    }
	    node=node.next;
	}

    }

    public void translate() throws CompileException {
	Reg    result;
	IMNode node=imCodeStart;

	//======================================
	// translate all basic blocks
	//======================================

	execEnv.setCodeContainer(this);
	
	if (opts.isOption("fast_thisptr") &&
	    method.getName().equals("read")) {
	    Debug.out.println(method.getClassName());
	}

	while (node!=null) {
	    if (node.isBasicBlock()) {
		IMBasicBlock label=(IMBasicBlock)node;		
		if (opts.doAlignCode() && label.isLoopEntry()) code.alignIP();
		node.translate((Reg)null);
	    }
	    node=node.next;
	}

	//if (opts.doAlignCode()) code.alignIP();
	if (opts.doAlignCode()) code.alignIP_16_Byte();
    }

    public IMNode getTop() {
	return imCodeStart;
    }

    public IMNode getEnd() {
	return imCodeEnd;
    }
    
    public IMNode first() {
	current = imCodeStart;
	return current;
    }

    public IMNode last() {
	current = imCodeEnd;
	return current;
    }

    public IMNode next() {
	if (current!=null) current=current.next;
	return current;
    }
    
    public IMNode prev() {
	if (current!=null) current=current.prev;
	return current;
    }
    
    public IMNode current() {
	return current();
    }
    
    public void remove() {
	if (current==imCodeStart) {
	    imCodeStart = current.next;
	    if (imCodeStart!=null) {
		imCodeStart.prev = null;
	    } else {
		imCodeEnd = null;
	    }
	    current = imCodeStart;
	} else if (current==imCodeEnd) {
	    imCodeEnd = current.prev;
	    if (imCodeEnd!=null) {
		imCodeEnd.next = null;
	    } else {
		System.err.println("mix up in codecontainer !!! ");
		imCodeStart=null;
	    }
	    current = imCodeEnd;
	} else {
	    current.prev.next = current.next;
	    current.next.prev = current.prev;
	    current=current.next;
	}	
    }

    public void insertBefor(IMNode node) {
	
	if (imCodeStart==null) {
	    node.next=null;	    
	    imCodeStart=node;
	    imCodeEnd=node;
	    node.prev=null;
	    return;
	}

	if (current==null) current = imCodeStart;

	if (current==imCodeStart) {
	    node.prev     = null;
	    node.next     = imCodeStart;
	    imCodeStart.prev = node;
	    imCodeStart      = node;
	} else if (current==imCodeEnd) {
	    node.next        = imCodeEnd;
	    node.prev        = imCodeEnd.prev;
	    imCodeEnd.prev.next = node;
	    imCodeEnd.prev      = node;
	} else {
	    node.next         = current;
	    node.prev         = current.prev;
	    current.prev.next = node;
	    current.prev      = node;
	}
    }	    

    public void insertTop(IMNode node) {
	if (node!=null) {
	    node.prev = null;
	    node.next = imCodeStart;
	    if (imCodeStart==null) {
		imCodeEnd   = node;
	    } else {
		imCodeStart.prev = node;
	    }
	    imCodeStart = node;
	}
    }

    public void append(IMNode node) {
	node.next=null;
	if (imCodeStart==null) {
	    // first and only element
	    imCodeStart=node;
	    imCodeEnd=node;
	    node.prev=null;
	} else {
	    // last element
	    node.prev      = imCodeEnd;
	    imCodeEnd.next = node;
	    imCodeEnd      = node;
	}
	if (node.isInstruction()) numberOfInstr++;
    }
    
    public boolean hasMore() {
	return current.next!=null;
    }
	
    public void writeCode(java.io.PrintStream outStream) {
	IMNode node=imCodeStart;

	outStream.println(" ");
	outStream.println(" method "+method.getName()+method.getSignature()+" {");	
	/*
	outStream.println("// NumArgs     : "+
			  Integer.toString(frame.getNumArgs()));
	outStream.println("// NumLocalVars: "+
			  Integer.toString(frame.getNumLocalVars()));
	outStream.println("// NumBlockVars: "+
			  Integer.toString(frame.getNumBlockVars()));
	outStream.println("// NumTempSlots: "+
			  Integer.toString(frame.getNumTempSlots()));
	*/
	//outStream.println("// Stackmap : " + frame.stackMapToString());

	while (node!=null) {
	    if (node.isBasicBlock()) {
		IMBasicBlock label=(IMBasicBlock)node;		
		outStream.println("  "+label.toReadableString()+":  "+label.getDebugString());
		//outStream.println("  "+label.varList());
	    } else {
		outStream.println("        "+node.toReadableString()+";   "+node.debugInfo());
		//outStream.println(node.getNrRegs()+"        "+node.toReadableString()+";   "+node.debugInfo());
		//outStream.println(node.getVarInfo()+" :      "+node.toReadableString()+";   "+node.debugInfo());
		//outStream.println("       "+node.toReadableString()+";   ["+node.getVarInfo()+"]");
		//outStream.println("  "+node.getVarInfo()+" "+node.toReadableString()+";");
	    }
	    node=node.next;
	}

	if (handler!=null && handler.length>0) {
	    outStream.println("EXCEPTIONHANDLER-TABLE:");
	    for (int i=0;i<handler.length;i++) {
		outStream.println(handler[i].toString());
	    }
	}	

	outStream.println(" }");

    }

    public void writeStatistics(java.io.PrintStream outStream) {
	if (regs.getStatistics()!=null)
	    outStream.println("// Register: "+regs.getStatistics()+" "+method.getName());
    }


    private void insertBCList(IMNode node) {
	node.bc_next=null;
	if (bc_list_top==null) {
	    bc_list_top = node;
	} else {
	    bc_list_end.bc_next=node;
	}
	bc_list_end = node;
    }

    private IMBasicBlock createBasicBlock(int ip,int offset) {
	int bcpos = ip + offset;
	IMBasicBlock label=null;
	IMBasicBlock curr;

	if (label_list==null) {
	    label = new IMBasicBlock(this,bcpos);
	    label_list = label;
	    return label;
	} 

	if (label_list.getBCPosition() > bcpos) {
	    label = new IMBasicBlock(this,bcpos);
	    label.bc_next = label_list;	
	    label_list = label;
	    return label;
	}

	if (label_list.getBCPosition() == bcpos) {
	    label_list.incCounter();
	    return label_list;
	}

	curr = label_list;	
	while (curr.bc_next != null && curr.bc_next.getBCPosition() < bcpos) curr = (IMBasicBlock)curr.bc_next;

	if (curr.bc_next!=null && curr.bc_next.getBCPosition() == bcpos) {
	    label = (IMBasicBlock)curr.bc_next;
	    label.incCounter();
	} else {
	    label = new IMBasicBlock(this,bcpos);
	    label.bc_next = curr.bc_next;		    
	    curr.bc_next = label;
	}
	    
        return label;
    }
}
