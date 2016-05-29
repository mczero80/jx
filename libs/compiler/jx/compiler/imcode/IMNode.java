package jx.compiler.imcode;

import jx.classfile.constantpool.*; 
import jx.classfile.datatypes.*;
import jx.classfile.*;
import jx.zero.Debug;
import jx.compiler.*;
import jx.compiler.nativecode.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;
import java.util.Vector;

// ***** IMNode *****

public class IMNode {

    private boolean lowPath = false;

    // tags 
    public static final int BASICBLOCK  = 1;
    public static final int BB_END      = 2;
    public static final int INSTRUCTION = 4;
    public static final int CONSTANT    = 8;
    public static final int BRANCH      = 16;
    public static final int RETURN_SUB  = 32;
    public static final int BLOCKVAR    = 64;
    public static final int OPERATOR    = 128;
    public static final int COMPERATOR  = 256;
    public static final int RETURN      = 1024;
    public static final int VARIABLE    = 2048;

    public static final boolean verbose = true;

    public IMNode prev;
    public IMNode next;

    public IMNode bc_next;

    protected int tag;
    protected int bcPosition;
    protected int bytecode;

    protected String debugTxt="";
    protected int datatype;
    protected boolean varStackMap[];
    protected String varStackMapStr;

    protected CodeContainer    container;
    protected ConstantPool     cPool;
    protected BinaryCodeIA32   code;
    protected RegManager       regs;
    protected StatisticInfo    stat;
    protected MethodStackFrame frame;
    protected ExecEnvironmentInterface execEnv;
    protected CompilerOptionsInterface opts;


    /*********************** init *************************/

    public IMNode() {
	tag = 0;
	bytecode   = BC.NOP;
	bcPosition = -1;
	datatype   = -1;
	tag        = IMNode.INSTRUCTION;
    }

    public IMNode(CodeContainer container) {
	this();
	init(container);
    }

    public void init(CodeContainer container) {
	this.container = container;
	this.cPool     = container.getConstantPool();
	this.code      = container.getIA32Code();
	this.regs      = container.getRegManager();
	this.frame     = container.getMethodStackFrame();
	this.execEnv   = container.getExecEnv();
	this.opts      = container.getOpts();
	this.stat      = container.getStatisticInfo();
    }

    /*********************** TAGS *************************/

    public int getTag() {
	return tag;
    }


    public boolean isBasicBlock() {
	return (tag & IMNode.BASICBLOCK)!=0;
    }

    public boolean isOperator() {
	return (tag & IMNode.OPERATOR)!=0;
    }

    public boolean isInstruction() {
	return (tag & IMNode.INSTRUCTION)!=0;
    }

    public boolean isConstant() {
	return (tag & IMNode.CONSTANT)!=0;
    }

    public final boolean isRealConstant() {
	return (tag & IMNode.CONSTANT)!=0;
    }


    public IMConstant nodeToConstant() {
	throw new ClassCastException("node is not constant!");
    }

    public boolean isBranch() {
	return (tag & IMNode.BRANCH)!=0;
    }

    public boolean isReturnSubroutine() {
	return (tag & IMNode.RETURN_SUB)!=0;
    }

    public boolean isReturn() {
	return (tag & IMNode.RETURN)!=0;
    }

    public boolean isVariable() {
	return (tag & IMNode.VARIABLE)!=0;
    }

    public boolean isBlockVariable() {
	return (tag & IMNode.BLOCKVAR)!=0;
    }

    public boolean isThrow() {
	return (this instanceof IMThrow);
    }

    public boolean isComperator() {
	return (tag & IMNode.COMPERATOR)!=0;
    }

    public boolean isThisPointer() {
	return false;
    }

    public boolean isBinaryOperator() { return false; }

    public CompilerOptionsInterface getCompilerOptions() {
	return opts;
    }

    public boolean hasConstant() { return false; }

    public boolean isSubOrAdd() {
	return false;
    }

    public boolean isDivOrMult() {
	return false;
    }

    public void setLowPriorityPath() {
	lowPath=true;
    }

    public boolean isLowPriorityPath() {
	return lowPath;
    }

    public boolean isEndOfBasicBlock() {
	if ((tag & IMNode.BB_END)!=0) return true;
	if ((bc_next!=null)&&(bc_next.isBasicBlock())) {
	    tag = tag | IMNode.BB_END;
	    return true;
	}
	return false;
    }

    public int  getDatatype() {
	return datatype;
    }

    public String toReadableString() {
	return "<unkown node>";
    }

    public String debugInfo() {
	return debugTxt;
    }

    public void addDebugInfo(String txt) {
	debugTxt+=txt;
    }

    public int getBCPosition() {
	return bcPosition;
    }

    public void saveVarStackMap(MethodStackFrame frame) {
	varStackMap    = frame.getLVarMap();
	varStackMapStr = frame.varMapToString();
    }

    public boolean[] getVarStackMap() {
	return varStackMap;
    }

    public String getVarStackMapString() {
	return varStackMapStr;
    }

    public int getNrRegs() { return 0; }

    public void getCollectVars(Vector vars) { return; }

    public String varList() {
	return frame.varList();
    }

    public String getVarInfo() {
	Vector vars = new Vector();

	getCollectVars(vars);

	String str=" ";
	for (int i=0;i<vars.size();i++) {
	    //str += " "+((IMNode)vars.elementAt(i)).toReadableString();
	    str += ((IMVarAccessInterface)vars.elementAt(i)).toSymbolname()+" ";
	}

	return str;
    }

    public String getLineInfo() {
	BCMethod method = container.getBCMethod();
	return " "+method.getClassName()+"."+method.getName()
		+" (l:"+method.getLineNumber(bcPosition)+" bc:"+bcPosition+") ";
    }

    public int  getBytecode() {
	return bytecode;
    }

    public IMNode processStack(VirtualOperantenStack stack,IMBasicBlock basicBlock) throws CompileException {
	throw new CompileException("operation not implemented! in"+
				   container.getBCMethod().getName()+
				   " BC: "+bytecode+" Pos: "+bcPosition);
    }

    public IMNode constant_folding() throws CompileException {
	return this;
    }  

    public IMNode constant_forwarding(IMNodeList varList) throws CompileException {
	return this;
    }

    public IMNode inlineCode(CodeVector iCode,int depth, boolean forceInline) throws CompileException {
	return this;
    }

    public IMNode assignNewVars(CodeContainer newContainer,int slots[],IMOperant opr[],int retval,int bcPos) throws CompileException {
	init(newContainer);
	//System.err.println("wrong assign method");
	return this;
    }

    public String whichCodeContainer() {
	return " id:"+Integer.toString(container.getID());
    }

    public String toString() {
	return super.toString()+" Bytecode "+Integer.toString(bytecode);
    }

    /*************************** compile **************************/

    // IMNode
    public void translateToPush() throws CompileException {
	Reg rval = regs.getIntRegister(Reg.any);
	translate(rval);
	frame.push(datatype,rval);
	regs.freeIntRegister(rval);
    }

    public final void translate(RegObj result) throws CompileException {
	if (result==null) translate((Reg)result);
	switch (result.getDatatype()) {
	case BCBasicDatatype.FLOAT:
	case BCBasicDatatype.DOUBLE:
	    execEnv.codeThrow(this,-11,bcPosition);
	    break;
	case BCBasicDatatype.LONG:
	    translate((Reg64)result);
	    break;	
	case BCBasicDatatype.INT:
	case BCBasicDatatype.BOOLEAN:
	case BCBasicDatatype.REFERENCE:
	case BCBasicDatatype.VOID:
	    translate((Reg)result);
	    break;
	default:
	    /*
	       Debug.out.println("warn: unknown or unsupported datatype");
	       Debug.out.println(toReadableString()+
	       " type reg: "+BCBasicDatatype.toString(result.getDatatype())+
	       " obj: "+BCBasicDatatype.toString(datatype));
	     */
	    translate((Reg)result);
	}
    }

    public void translate(Reg result) throws CompileException {
	throw new CompileException(getLineInfo()+" int translation not implemented!");
    }

    public void translate(Reg64 result) throws CompileException {
        Debug.out.println(this.getClass().getName());
	Debug.out.println(this.toReadableString());
	throw new CompileException(getLineInfo()+" long translation not implemeted!");
    }

    public final void translateLong(Reg64 result) throws CompileException {
	translate(result);
    }

    public void translateFloat(RegFloat result) throws CompileException {
	throw new CompileException(getLineInfo()+" float translation not implemented!");
    }

    public void translateDouble(RegDouble result) throws CompileException {
	throw new CompileException(getLineInfo()+" float translation not implemented!");
    }    
}
