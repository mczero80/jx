package jx.compiler.imcode;

import jx.compiler.*;
import jx.compiler.symbols.*;
import jx.compiler.nativecode.*;
import jx.compiler.execenv.*;

import jx.classfile.datatypes.*;

public class MethodStackFrame {

    //private final int     EXTRA_FRAME_SPACE = 60;
    private final int     EXTRA_FRAME_SPACE = 500;
    private final int     NUMBER_OF_TEMPS   = 3;

    private final int MAX_TYPES = 5;
    private final int ARGS      = 0;
    private final int VARS      = 1;
    private final int BLKS      = 2;
    private final int TMPS      = 3;
    private final int OPRS      = 4;

    private BCMethod     method;
    private int[]        argTypes;

    private VarVector[]  stack;
    private int          stackSize;
    private int[]        stackOffset;
    private int          maxOperants;
    private int          numberOfOperants;
    private boolean      operantenMap;

    private boolean      reallocStack;

    private int extraStackSpace;
    
    boolean[]        stackMap;
    int              stackMapVars;

    private BinaryCodeIA32 code;

    public MethodStackFrame(BinaryCodeIA32 code, BCMethod method) {

	this.method   = method;
	this.argTypes = method.getArgumentTypes();
	this.code     = code;

	this.stack    = new VarVector[MAX_TYPES];
	this.stackOffset = new int[MAX_TYPES];
	for (int i=0;i<MAX_TYPES;i++) this.stack[i] = new VarVector();

	maxOperants = 0;

	int  offsetArgs = 0; // %ebp , %eip
	if (!method.isStatic()) {
	    stack[ARGS].add(new LocalVariable(ARGS,BCBasicDatatype.REFERENCE,offsetArgs));
	    offsetArgs+=4;
	}

	for (int i=0;i<argTypes.length;i++) {
	    stack[ARGS].add(new LocalVariable(ARGS,argTypes[i],offsetArgs));
	    offsetArgs+=(BCBasicDatatype.sizeInWords(argTypes[i])*4);
	}

	numberOfOperants=0;
	reallocStack = true;
	extraStackSpace = 4;
    }

    public void setExtraSpace(int value) {	
	extraStackSpace = value;
	computeStackLayout();
    }

    public int getExtraSpace() {
	return extraStackSpace;
    }

    public void initReferences(BinaryCodeIA32 code) throws CompileException {
	VarVector curr;
	int csize;
	boolean initECX = false;

	curr = stack[VARS];
	csize = curr.size();
	for (int j=0;j<csize;j++) {
	    LocalVariable slot = curr.elementAt(j);
	    if (slot.isDatatype(BCBasicDatatype.REFERENCE)) {
		int offset = getOffset(slot);
		if (offset==0) throw new Error("zero offset");
		if (!initECX) {
		    initECX=true;
		    code.xorl(Reg.ecx,Reg.ecx);
		}
		code.movl(Reg.ecx,Ref.ebp.disp(offset));
	    }
	}

	curr = stack[BLKS];
        csize = curr.size();
	for (int j=0;j<csize;j++) {
	    LocalVariable slot = curr.elementAt(j);
	    if (slot.isDatatype(BCBasicDatatype.REFERENCE)) {
		int offset = getOffset(slot);
		if (offset==0) throw new Error("zero offset");
		if (!initECX) {
		    initECX=true;
		    code.xorl(Reg.ecx,Reg.ecx);
		}
		code.movl(Reg.ecx,Ref.ebp.disp(offset));
	    }
	}	
    }

    public void addInitLocalSlots(BinaryCodeIA32 code) throws CompileException {

    }

    public int getOffset(LocalVariable slot) throws CompileException {	
        int offset;

	if (reallocStack) computeStackLayout();

	offset=stackOffset[slot.getType()]+slot.off;

	/*stackOffset[VARS] = -extraStackSpace*/

	if ((offset==0)||((offset>-extraStackSpace)&&(slot.getType()!=ARGS))) {
	  System.err.println("### type: "+slot.getType()+
			     " stack offset: "+stackOffset[slot.getType()]+
			     " slot  offset: "+slot.off+" ###");
	  computeStackLayout();
	  System.err.println("### type: "+slot.getType()+
			     " stack offset: "+stackOffset[slot.getType()]+
			     " slot  offset: "+slot.off+" ###");
	  /*if (slot.getType()==TMPS)*/ throw new Error("variable offset out of range");
	}

	return offset;
    }	

    /**
       size and infos about the stack frame
     */

    public MaxFrameSizeSTEntry getMaxFrameSizeSTEntry() {
	return new MaxFrameSizeSTEntry(this);
    }

    /**
       Maximale noch benoetigte Stackgroesse 
    */

    public int getMaxFrameSize() {
	if (reallocStack) computeStackLayout();
	int max_size = -stackSize;
	if (stack[OPRS].size()<3) max_size+=3-stack[OPRS].size();	
	return max_size + EXTRA_FRAME_SPACE;
    }

    public AllocSTEntry getAllocSTEntry() {
	return new AllocSTEntry(this);
    }

    /**
       Groesse des Methoden Frames ohne dem Operantenstack
    */
    
    public int getStackFrameSize() {
	if (reallocStack) computeStackLayout();
	return (-stackOffset[OPRS])-4;
    }

    public int getNumArgs() {
	return stack[ARGS].size();
    }

    public int getNumLocalVars() {
	return stack[VARS].size();
    }

    public int getNumBlockVars() {
	return stack[BLKS].size();
    }

    public int getNumTempSlots() {
	return stack[TMPS].size();
    }

    public int getStackMapSize() {
	if (reallocStack) computeStackLayout();
  	int size=0;
	for (int i=1;i<stack.length;i++) {
	    VarVector curr = stack[i];

	    int csize=0;
	    if (i!=OPRS) csize = curr.size();
	    else csize = numberOfOperants;

	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);
		size+=slot.size;
	    }
	}
	return size;
    }

    public int getVarMapSize() {
	if (reallocStack) computeStackLayout();       

	int size=0;
	for (int i=2;i<stack.length-1;i++) {
	    VarVector curr = stack[i];
	    int csize=0;
	    if (i!=OPRS) csize = curr.size();
	    else csize = numberOfOperants;
	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);
		size+=slot.size;
	    }
	}

	return size;
    }

    public int getLVarMapSize() {
	if (reallocStack) computeStackLayout();       

	int size=0;
	VarVector curr = stack[VARS];
	int csize=0;
	csize = curr.size();
	for (int j=0;j<csize;j++) {
	    LocalVariable slot = curr.elementAt(j);
	    size+=slot.size;
	}

	return size;
    }

    public int getOprMapSize() {
	if (reallocStack) computeStackLayout();       

	int size=0;
	VarVector curr = stack[OPRS];
	for (int j=0;j<numberOfOperants;j++) {
	    LocalVariable slot = curr.elementAt(j);
	    size+=curr.elementAt(j).size;
	}

	return size;
    }

    public String varList() {
	int size = getLVarMapSize();

	if (reallocStack) computeStackLayout();

	String str="";
	VarVector curr = stack[VARS];
	int csize = curr.size();

	for (int j=0;j<csize;j++) {
	    LocalVariable slot = curr.elementAt(j);	    
	    if (slot.isDatatype(BCBasicDatatype.REFERENCE) && !slot.unused) {
		str+="R ";
	    } else {
		str+="i ";
	    }
	}

	return str;
    }

    public boolean[] getLVarMap() {
	
	int size = getLVarMapSize();

	if (reallocStack) computeStackLayout();

	boolean[] newStackMap = new boolean[size];
	VarVector curr = stack[VARS];
	int csize = curr.size();
	int s=0;

	for (int j=0;j<csize;j++) {
	    LocalVariable slot = curr.elementAt(j);

	    if (slot.isDatatype(BCBasicDatatype.REFERENCE) && !slot.unused) {
		newStackMap[s]=true;
	    } else {
		newStackMap[s]=false;
	    }

	    s++;
	    if (slot.size==2) {
		newStackMap[s] = false;
		s++;
	    }		
	}

	return newStackMap;	
    }

    public boolean[] getVarMap() {

	int size = getVarMapSize();

	if (reallocStack) computeStackLayout();

	boolean[] newStackMap = new boolean[size];
	int s=0;
	for (int i=2;i<stack.length-1;i++) {
	    VarVector curr = stack[i];
	    int csize = curr.size();

	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);

		if (slot.isDatatype(BCBasicDatatype.REFERENCE) && !slot.unused) {
		    newStackMap[s]=true;
		} else {
		    newStackMap[s]=false;
		}

		s++;
		if (slot.size==2) {
		    newStackMap[s] = false;
		    s++;
		}		
	    }
	}

	return newStackMap;	
    }

    public boolean[] getOprMap() {
	if (reallocStack) computeStackLayout();

	int csize = getOprMapSize();
	boolean[] newStackMap = new boolean[csize];
	VarVector curr = stack[OPRS];

	int s=0;
	for (int j=0;j<numberOfOperants;j++) {
	    LocalVariable slot = curr.elementAt(j);
	    
	    if (slot.isDatatype(BCBasicDatatype.REFERENCE) && !slot.unused) {
		newStackMap[s]=true;
	    } else {
		newStackMap[s]=false;
	    }
	    
	    s++;
	    if (slot.size==2) {
		newStackMap[s] = false;
		s++;
	    }		
	}
    
	return newStackMap;	
    }

    public boolean[] getStackMap() {
	if (reallocStack) computeStackLayout();

	int size=getStackMapSize();
	boolean[] newStackMap = new boolean[size];

	int s=0;
	for (int i=1;i<stack.length;i++) {
	    VarVector curr = stack[i];

	    int csize=0;
	    if (i!=OPRS) csize = curr.size();
	    else csize = numberOfOperants;

	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);

		if (slot.isDatatype(BCBasicDatatype.REFERENCE)) {
		    newStackMap[s]=true;
		} else {
		    newStackMap[s]=false;
		}

		s++;
		if (slot.size==2) {
		    newStackMap[s] = false;
		    s++;
		}		
	    }
	}

	return newStackMap;
    }

    /**
       local variables
    */

    public LocalVariable[] getLocalVars() {
	int argc = stack[ARGS].size();
	int varc = stack[VARS].size();

	LocalVariable[] vars = new LocalVariable[argc+varc];

	for (int i=0;i<argc;i++) vars[i]      = stack[ARGS].elementAt(i);
	for (int i=0;i<varc;i++) vars[i+argc] = stack[VARS].elementAt(i);

	return vars;
    }

    public void setLocalSlotNotConstant(int varIndex) {
	int argc = stack[ARGS].size();

	if (varIndex<argc) {
	    stack[ARGS].elementAt(varIndex).constant = false;
	    return;
	}

	varIndex -= argc;
	stack[VARS].elementAt(varIndex).constant = false;
    }

    public LocalVariable getLocalVar(int varIndex,int datatype) {
	VarVector args = stack[ARGS];
	VarVector vars = stack[VARS];

	int ssize = args.size();
	
	if (varIndex<ssize) {
	    return args.elementAt(varIndex);
	}

	varIndex -= ssize;
	ssize = vars.size();

	while (varIndex>=ssize) {
	    reallocStack=true;
	    if (varIndex==ssize) {
		vars.add(new LocalVariable(VARS,datatype));
	    } else {
		vars.add(new LocalVariable(VARS,-2));
	    }
	    ssize = vars.size();
	}
	
	LocalVariable var = vars.elementAt(varIndex);

	/*
	if (!var.isDatatype(datatype) && !var.isDatatype(-2)) {
	    System.err.print("!!! type change !!! "+var.getDatatype()+"->"+datatype+" ");
	    System.err.println(BCBasicDatatype.toString(var.getDatatype())+"->"+BCBasicDatatype.toString(datatype));
	    //if (var.datatype!=-2) System.exit(-1);
	}
	*/

	var.setDatatype(datatype);

	return var;
    }

    public int getNewLocalVar(int datatype) {
	VarVector vars = stack[VARS];
	int ssize = vars.size();

	reallocStack=true;
	vars.add(new LocalVariable(VARS,datatype));
	
	return ssize + stack[ARGS].size();
    }

    /*
      operanten stack
     */

    public int start() {
	return numberOfOperants;
    }

    public void push(int datatype, Opr operant) {
	
	if (operant.tag==Opr.REG) {
	    code.pushl((Reg)operant);
	} else {
	    code.pushl((Ref)operant);
	}
	pushOperant(datatype);
    }

    public void push(RegObj reg) {
	reg.push(this);
    }

    /*
    public void pushLong(Reg64 operant) {
	push(-1,operant.high);
	push(-1,operant.low);
    }
    */

    public void push(int datatype, int immd) {
	code.pushl(immd);
	pushOperant(datatype);
    }

    public void push(int datatype, SymbolTableEntryBase entry) {
	code.pushl(entry);
	pushOperant(datatype);
    }

    public void pushfl() {
	code.pushfl();
	pushOperant(-1);
    }

    public void pop(Reg reg) {
	code.popl(reg);
	popOperants(1);
    }

    public void popfl() {
	code.popfl();
	popOperants(1);
    }    

    public void clearStack(int number) {
	code.addl(number*4,Reg.esp);
	popOperants(number);
    }

    public void cleanup(int entries) {
	clearStack(numberOfOperants-entries);
    }

    private void pushOperant(int datatype) {
	VarVector oprs = stack[OPRS];
	if (numberOfOperants>=oprs.size()) {
	    reallocStack=true;
	    oprs.add(new LocalVariable(OPRS,datatype));
	} else {
	    LocalVariable var = oprs.elementAt(numberOfOperants);
	    var.setDatatype(datatype);
	    var.unused = false;
	}
	numberOfOperants++;
    }

    private void popOperants(int number) {
	VarVector oprs = stack[OPRS];
	for (int i=number;i>0;i--) {
	    numberOfOperants--;
	    oprs.elementAt(numberOfOperants).unused=true;
	}
    }

    /*
      block varibalen
    */

    public LocalVariable getBlockSlot(int datatype,int varIndex) {
	VarVector vars = stack[BLKS];
	int ssize = vars.size();

	while (varIndex>=ssize) {
	    reallocStack=true;
	    if (varIndex==ssize) {
		vars.add(new LocalVariable(BLKS,datatype));
	    } else {
		vars.add(new LocalVariable(BLKS,-2));
	    }
	    ssize = vars.size();
	}

	LocalVariable var = vars.elementAt(varIndex);
	var.setDatatype(datatype);
	return var;	
    }

    public LocalVariable getFreeTempSlot(int datatype) {
	VarVector vars = stack[TMPS];
	int ssize = vars.size();
	LocalVariable var;

	//reallocStack=true;
	computeStackLayout();

	for (int i=0;i<ssize;i++) {
	    var = vars.elementAt(i);
	    if (var.unused) {
		var.unused = false;
		var.setDatatype(datatype);
		return var;
	    }
	}

	//reallocStack=true;
	var = new LocalVariable(TMPS,datatype);
	vars.add(var);
	
	return var;
    }

    public void freeAllTemps() {
	    VarVector vars = stack[TMPS];
	    int ssize = vars.size();
	    LocalVariable var;
	    for (int i=0;i<ssize;i++) {
		    var = vars.elementAt(i);
		    var.unused = true;	
		    var.setDatatype(-1);	
	    }
    }

    public void freeSlot(LocalVariable slot) {
	if (slot==null) return;
	if (!slot.isType(VARS) && !slot.isType(BLKS)) {
	    //slot.setDatatype(-1);
	    slot.unused   = true;
	}
    }

    public String varMapToString() {
	VarVector curr = stack[VARS];
	int size = curr.size();
	
	if (size==0) return null;

	String currStackMap = "";

	for (int i=0;i<size;i++) {
	    int t = curr.elementAt(i).getDatatype();
	    currStackMap += BCBasicDatatype.toSymbol(t);
	}
		
	return currStackMap;
    }

    public String stackMapToString(IMNode node) {
	boolean[] sMap = getStackMap();
	String retval = "[";

	if (node.getVarStackMapString()!=null) {
	    retval += node.getVarStackMapString();
	}

	int s=0;
	for (int i=2;i<stack.length;i++) {
	    VarVector curr = stack[i];
	    int csize = curr.size();

	    if (i!=1) retval+=" ";

	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);

		if (slot.unused) {		    
		    //if (slot.isDatatype(BCBasicDatatype.REFERENCE)) retval+="r";
		    //else retval+=".";
		    //else retval+=BCBasicDatatype.toSymbol(slot.getDatatype());
		    retval+=".";
		} else {
		    retval+=BCBasicDatatype.toSymbol(slot.getDatatype());
		}

		//retval += slot.getType();
		
		if (slot.size==2) {
		    retval+="_";
		}
	    }
	}
	retval+="]";
	return retval;
    }

    /*
    public String stackMapToString(IMNode node) {
	int       lvarSize = getLVarMapSize();
	int       varSize  = getVarMapSize();
	boolean[] vars     = getVarMap();
	boolean[] lvars    = node.getVarStackMap();
	boolean[] oprs     = getOprMap();

	char[] currStackMap = new char[lvarSize + varSize + oprs.length + 4];

	currStackMap[0]='[';
	for (int i=1;i<currStackMap.length-1;i++) currStackMap[i]='?';
	currStackMap[currStackMap.length-1]=']';

	for (int i=0;i<lvars.length;i++) {
	    if (lvars[i]) currStackMap[i+1] = 'R';
	    else if (stack[VARS].elementAt(i).getDatatype()>=0) currStackMap[i+1] = '.';
	}

	currStackMap[lvarSize+1] = ' ';

	for (int i=0;i<vars.length;i++) {
	    if (vars[i]) currStackMap[lvarSize + i + 2] = 'R';
	}

	currStackMap[lvarSize + varSize + 2] = ' ';

	for (int i=0;i<oprs.length;i++) {
	    if (oprs[i]) currStackMap[lvarSize + varSize + i + 3] = 'R';
	    else currStackMap[lvarSize + varSize + i + 3] = 'i';
	}
	    
	return new String(currStackMap);
    }
    */

    public String stackMapToString() {
	boolean[] sMap = getStackMap();
	String retval = "[";

	/*
	for (int s=0;s<sMap.length;s++) {
	    if (sMap[s]) retval+="1";
	    else retval+="0";
	}
	    
	retval += "|";
	*/

	int s=0;
	for (int i=1;i<stack.length;i++) {	    
	    VarVector curr = stack[i];
	    int csize = curr.size();

	    if (i!=1) retval+=" ";

	    for (int j=0;j<csize;j++) {
		LocalVariable slot = curr.elementAt(j);

		if (slot.unused) {
		    if (slot.isDatatype(BCBasicDatatype.REFERENCE)) retval+="r";
		    else retval+=".";
		    //else retval+=BCBasicDatatype.toSymbol(slot.getDatatype());
		} else {
		    retval+=BCBasicDatatype.toSymbol(slot.getDatatype());
		}

		//retval += slot.getType();
		
		if (slot.size==2) {
		    retval+="_";
		}
	    }
	}
	retval+="]";
	return retval;
    }

    /**
       helpers
    */

    private int nextOffset(int stype) {
	VarVector vars = stack[stype];
	int ssize = vars.size();
	int offset=0;
	if (ssize>0) {
	    LocalVariable last = vars.elementAt(ssize-1);
	    offset = last.off-(last.size * 4);	
	}	
	return offset;
    }

    private void computeStackLayout() {
	int ssize,csize;
	LocalVariable last,curr;
	VarVector slots;

	// set offset for args (this is fix :-))
	stackOffset[ARGS] =  8;

	// compute offsets for vars
	stackOffset[VARS] = -extraStackSpace;

	slots = stack[VARS];
	csize = slots.size();
	if (csize>0) {
	    last = slots.firstElement();
	    for (int j=1;j<csize;j++) {
		curr = slots.elementAt(j);
		if (curr.isDatatype(-2)) {
		    // FIXME !!
		    // throw new Error("fixme: unknown datatype of v"+Integer.toString(j+stack[ARGS].size())); 
		}
		curr.off = last.off - last.size * 4;
		last = curr;
	    }
	}

	// compute offsets for all other
	int i=2;
	for (;i<stackOffset.length;i++) {
	    stackOffset[i] = stackOffset[i-1];
	    ssize = stack[i-1].size();
	    if (ssize>0) {
		last = stack[i-1].elementAt(ssize-1);
		stackOffset[i]+=( last.off - last.size * 4 );
	    }

	    slots = stack[i];
	    csize = slots.size();
	    if (csize>0) {
		last = slots.firstElement();
		for (int j=1;j<csize;j++) {
		    curr = slots.elementAt(j);
		    // if (curr.isDatatype(-2)) throw new Error("fixme");
		    curr.off = last.off - last.size * 4;
		    last = curr;
		}
	    }	    
	}

	stackSize = stackOffset[i-1];
	ssize = stack[i-1].size();
	if (ssize>0) {
	    last = stack[i-1].elementAt(ssize-1);
	    stackSize+=( last.off - last.size * 4 );
	}	
    }
}
