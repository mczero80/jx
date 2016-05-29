package jx.verifier.bytecode;

import java.lang.*;
import jx.classfile.*;
import jx.classfile.constantpool.*;
import java.io.*;
import jx.verifier.*;

public class ByteCode {
    
    private static final String OPNAMES[] = {
	/*0x00*/ "nop", "aconst_null", "iconst_m1", "iconst_0", 
	/*0x04*/ "iconst_1", "iconst_2", "iconst_3", "iconst_4", 
	/*0x08*/ "iconst_5", "lconst_0", "lconst_1", "fconst_0", 
	/*0x0c*/ "fconst_1", "fconst_2", "dconst_0", "dconst_1", 
	/*0x10*/ "bipush", "sipush", "ldc", "ldc_w", 
	/*0x14*/ "ldc2_w", "iload", "lload", "fload",
	/*0x18*/ "dload", "aload", "iload_0", "iload_1",
	/*0x1c*/ "iload_2","iload_3", "lload_0", "lload_1",
	/*0x20*/ "lload_2", "lload_3", "fload_0", "fload_1",
	/*0x24*/ "fload_2", "fload_3", "dload_0", "dload_1",
	/*0x28*/ "dload_2", "dload_3", "aload_0", "aload_1",
	/*0x2c*/ "aload_2", "aload_3", "iaload", "laload",
	/*0x30*/ "faload", "daload", "aaload", "baload", 
	/*0x34*/ "caload", "saload", "istore", "lstore", 
	/*0x38*/ "fstore", "dstore", "astore", "istore_0",
	/*0x3c*/ "istore_1", "istore_2", "istore_3", "lstore_0",
	/*0x40*/ "lstore_1", "lstore_2", "lstore_3", "fstore_0",
	/*0x44*/ "fstore_1", "fstore_2", "fstore_3", "dstore_0",
	/*0x48*/ "dstore_1", "dstore_2", "dstore_3", "astore_0",
	/*0x4c*/ "astore_1", "astore_2", "astore_3", "iastore", 
	/*0x50*/ "lastore", "fastore", "dastore", "aastore", 
	/*0x54*/ "bastore", "castore", "sastore", "pop",
	/*0x58*/ "pop2", "dup", "dup_x1", "dup_x2", 
	/*0x5c*/ "dup2", "dup2_x1", "dup2_x2", "swap",
	/*0x60*/ "iadd", "ladd", "fadd", "dadd",
	/*0x64*/ "isub", "lsub", "fsub", "dsub",
	/*0x68*/ "imul", "lmul", "fmul", "dmul", 
	/*0x6c*/ "idiv", "ldiv", "fdiv", "ddiv",
	/*0x70*/ "irem", "lrem", "frem", "drem",
	/*0x74*/ "ineg", "lneg", "fneg", "dneg",
	/*0x78*/ "ishl", "lshl", "ishr", "lshr",
	/*0x7c*/ "iushr", "lushr", "iand", "land",
	/*0x80*/ "ior", "lor", "ixor", "lxor",
	/*0x84*/ "iinc", "i2l", "i2f", "i2d",
	/*0x88*/ "l2i", "l2f", "l2d", "f2i",
	/*0x8c*/ "f2l", "f2d", "d2i", "d2l",
	/*0x90*/ "d2f", "i2b", "i2c", "i2s",
	/*0x94*/ "lcmp", "fcmpl", "fcmpg", "dcmpl",
	/*0x98*/ "dcmpg", "ifeq", "ifne", "iflt",
	/*0x9c*/ "ifge", "ifgt", "ifle", "if_icmpeq",
	/*0xa0*/ "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt",
	/*0xa4*/ "if_icmple", "if_acmpeq", "if_acmpne", "goto",
	/*0xa8*/ "jsr", "ret", "tableswitch", "lookupswitch",
	/*0xac*/ "ireturn", "lreturn", "freturn", "dreturn", 
	/*0xb0*/ "areturn", "return", "getstatic", "putstatic",
	/*0xb4*/ "getfield", "putfield", "invokevirtual", "invokespecial",
	/*0xb8*/ "invokestatic", "invokeinterface", "UNUSED", "new",
	/*0xbc*/ "newarray", "anewarray", "arraylength", "athrow",
	/*0xc0*/ "checkcast", "instanceof", "monitorenter", "monitorexit",
	/*0xc4*/ "wide", "multianewarray", "ifnull", "ifnonnull", 
	/*0xc8*/ "goto_w", "jsr_w"
    };
    
    //number of argument bytes following the opCode in the bytecodearray.
    private static final int OPNUMARGS[] = {
	/*0x00*/ 0,0,0,0,
	/*0x04*/ 0,0,0,0,
	/*0x08*/ 0,0,0,0,
	/*0x0c*/ 0,0,0,0,
	/*0x10*/ 1,2,1,2,
	/*0x14*/ 2,1,1,1,
	/*0x18*/ 1,1,0,0,
	/*0x1c*/ 0,0,0,0,
	/*0x20*/ 0,0,0,0,
	/*0x24*/ 0,0,0,0,
	/*0x28*/ 0,0,0,0,
	/*0x2c*/ 0,0,0,0,
	/*0x30*/ 0,0,0,0,
	/*0x34*/ 0,0,1,1,
	/*0x38*/ 1,1,1,0,
	/*0x3c*/ 0,0,0,0,
	/*0x40*/ 0,0,0,0,
	/*0x44*/ 0,0,0,0,
	/*0x48*/ 0,0,0,0,
	/*0x4c*/ 0,0,0,0,
	/*0x50*/ 0,0,0,0,
	/*0x54*/ 0,0,0,0,
	/*0x58*/ 0,0,0,0,
	/*0x5c*/ 0,0,0,0,
	/*0x60*/ 0,0,0,0,
	/*0x64*/ 0,0,0,0,
	/*0x68*/ 0,0,0,0,
	/*0x6c*/ 0,0,0,0,
	/*0x70*/ 0,0,0,0,
	/*0x74*/ 0,0,0,0,
	/*0x78*/ 0,0,0,0,
	/*0x7c*/ 0,0,0,0,
	/*0x80*/ 0,0,0,0,
	/*0x84*/ 2,0,0,0,
	/*0x88*/ 0,0,0,0,
	/*0x8c*/ 0,0,0,0,
	/*0x90*/ 0,0,0,0,
	/*0x94*/ 0,0,0,0,
	/*0x98*/ 0,2,2,2,
	/*0x9c*/ 2,2,2,2,
	/*0xa0*/ 2,2,2,2,
	/*0xa4*/ 2,2,2,2,
	/*0xa8*/ 2,1,-1,-1,
	/*0xac*/ 0,0,0,0,
	/*0xb0*/ 0,0,2,2,
	/*0xb4*/ 2,2,2,2,
	/*0xb8*/ 2,4,0,2,
	/*0xbc*/ 1,2,0,0,
	/*0xc0*/ 2,2,0,0,
	/*0xc4*/ -1,3,2,2,
	/*0xc8*/ 4,4
    };
    

    public static final int NOP = 0x00;
    public static final int ACONST_NULL = 0x01;
    public static final int ICONST_M1 = 0x02;
    public static final int ICONST_0 = 0x03;
    public static final int ICONST_1 = 0x04;
    public static final int ICONST_2 = 0x05;
    public static final int ICONST_3 = 0x06; 
    public static final int ICONST_4 = 0x07; 
    public static final int ICONST_5 = 0x08; 
    public static final int LCONST_0 = 0x09; 
    public static final int LCONST_1 = 0x0a; 
    public static final int FCONST_0 = 0x0b; 
    public static final int FCONST_1 = 0x0c; 
    public static final int FCONST_2 = 0x0d; 
    public static final int DCONST_0 = 0x0e; 
    public static final int DCONST_1 = 0x0f; 
    public static final int BIPUSH = 0x10;
    public static final int SIPUSH = 0x11;
    public static final int LDC = 0x12; 
    public static final int LDC_W = 0x13; 
    public static final int LDC2_W = 0x14; 
    public static final int ILOAD = 0x15; 
    public static final int LLOAD = 0x16; 
    public static final int FLOAD = 0x17; 
    public static final int DLOAD = 0x18; 
    public static final int ALOAD = 0x19; 
    public static final int ILOAD_0 = 0x1a; 
    public static final int ILOAD_1 = 0x1b; 
    public static final int ILOAD_2 = 0x1c; 
    public static final int ILOAD_3 = 0x1d; 
    public static final int LLOAD_0 = 0x1e;
    public static final int LLOAD_1 = 0x1f; 
    public static final int LLOAD_2 = 0x20;
    public static final int LLOAD_3 = 0x21;
    public static final int FLOAD_0 = 0x22; 
    public static final int FLOAD_1 = 0x23; 
    public static final int FLOAD_2 = 0x24; 
    public static final int FLOAD_3 = 0x25; 
    public static final int DLOAD_0 = 0x26; 
    public static final int DLOAD_1 = 0x27; 
    public static final int DLOAD_2 = 0x28; 
    public static final int DLOAD_3 = 0x29; 
    public static final int ALOAD_0 = 0x2a; 
    public static final int ALOAD_1 = 0x2b; 
    public static final int ALOAD_2 = 0x2c; 
    public static final int ALOAD_3 = 0x2d; 
    public static final int IALOAD = 0x2e; 
    public static final int LALOAD = 0x2f; 
    public static final int FALOAD = 0x30;
    public static final int DALOAD = 0x31; 
    public static final int AALOAD = 0x32; 
    public static final int BALOAD = 0x33; 
    public static final int CALOAD = 0x34; 
    public static final int SALOAD = 0x35; 
    public static final int ISTORE = 0x36; 
    public static final int LSTORE = 0x37; 
    public static final int FSTORE = 0x38; 
    public static final int DSTORE = 0x39; 
    public static final int ASTORE = 0x3a; 
    public static final int ISTORE_0 = 0x3b; 
    public static final int ISTORE_1 = 0x3c; 
    public static final int ISTORE_2 = 0x3d; 
    public static final int ISTORE_3 = 0x3e; 
    public static final int LSTORE_0 = 0x3f; 
    public static final int LSTORE_1 = 0x40;
    public static final int LSTORE_2 = 0x41; 
    public static final int LSTORE_3 = 0x42;
    public static final int FSTORE_0 = 0x43;
    public static final int FSTORE_1 = 0x44; 
    public static final int FSTORE_2 = 0x45; 
    public static final int FSTORE_3 = 0x46; 
    public static final int DSTORE_0 = 0x47; 
    public static final int DSTORE_1 = 0x48; 
    public static final int DSTORE_2 = 0x49; 
    public static final int DSTORE_3 = 0x4a; 
    public static final int ASTORE_0 = 0x4b; 
    public static final int ASTORE_1 = 0x4c; 
    public static final int ASTORE_2 = 0x4d; 
    public static final int ASTORE_3 = 0x4e; 
    public static final int IASTORE = 0x4f; 
    public static final int LASTORE = 0x50;
    public static final int FASTORE = 0x51; 
    public static final int DASTORE = 0x52; 
    public static final int AASTORE = 0x53; 
    public static final int BASTORE = 0x54; 
    public static final int CASTORE = 0x55; 
    public static final int SASTORE = 0x56; 
    public static final int POP = 0x57; 
    public static final int POP2 = 0x58; 
    public static final int DUP = 0x59; 
    public static final int DUP_X1 = 0x5a; 
    public static final int DUP_X2 = 0x5b; 
    public static final int DUP2 = 0x5c; 
    public static final int DUP2_X1 = 0x5d; 
    public static final int DUP2_X2 = 0x5e; 
    public static final int SWAP = 0x5f; 
    public static final int IADD = 0x60; 
    public static final int LADD = 0x61; 
    public static final int FADD = 0x62; 
    public static final int DADD = 0x63; 
    public static final int ISUB = 0x64; 
    public static final int LSUB = 0x65; 
    public static final int FSUB = 0x66; 
    public static final int DSUB = 0x67; 
    public static final int IMUL = 0x68; 
    public static final int LMUL = 0x69; 
    public static final int FMUL = 0x6a; 
    public static final int DMUL = 0x6b; 
    public static final int IDIV = 0x6c; 
    public static final int LDIV = 0x6d; 
    public static final int FDIV = 0x6e; 
    public static final int DDIV = 0x6f; 
    public static final int IREM = 0x70;
    public static final int LREM = 0x71; 
    public static final int FREM = 0x72; 
    public static final int DREM = 0x73; 
    public static final int INEG = 0x74; 
    public static final int LNEG = 0x75; 
    public static final int FNEG = 0x76; 
    public static final int DNEG = 0x77; 
    public static final int ISHL = 0x78; 
    public static final int LSHL = 0x79; 
    public static final int ISHR = 0x7a; 
    public static final int LSHR = 0x7b; 
    public static final int IUSHR = 0x7c; 
    public static final int LUSHR = 0x7d; 
    public static final int IAND = 0x7e; 
    public static final int LAND = 0x7f; 
    public static final int IOR = 0x80;
    public static final int LOR = 0x81; 
    public static final int IXOR = 0x82; 
    public static final int LXOR = 0x83; 
    public static final int IINC = 0x84; 
    public static final int I2L = 0x85; 
    public static final int I2F = 0x86;
    public static final int I2D = 0x87; 
    public static final int L2I = 0x88; 
    public static final int L2F = 0x89; 
    public static final int L2D = 0x8a; 
    public static final int F2I = 0x8b; 
    public static final int F2L = 0x8c; 
    public static final int F2D = 0x8d; 
    public static final int D2I = 0x8e; 
    public static final int D2L = 0x8f; 
    public static final int D2F = 0x90;
    public static final int I2B = 0x91; 
    public static final int I2C = 0x92; 
    public static final int I2S = 0x93; 
    public static final int LCMP = 0x94; 
    public static final int FCMPL = 0x95; 
    public static final int FCMPG = 0x96; 
    public static final int DCMPL = 0x97; 
    public static final int DCMPG = 0x98; 
    public static final int IFEQ = 0x99; 
    public static final int IFNE = 0x9a; 
    public static final int IFLT = 0x9b; 
    public static final int IFGE = 0x9c; 
    public static final int IFGT = 0x9d; 
    public static final int IFLE = 0x9e; 
    public static final int IF_ICMPEQ = 0x9f; 
    public static final int IF_ICMPNE = 0xa0;
    public static final int IF_ICMPLT = 0xa1; 
    public static final int IF_ICMPGE = 0xa2; 
    public static final int IF_ICMPGT = 0xa3; 
    public static final int IF_ICMPLE = 0xa4; 
    public static final int IF_ACMPEQ = 0xa5; 
    public static final int IF_ACMPNE = 0xa6; 
    public static final int GOTO  = 0xa7; 
    public static final int JSR = 0xa8; 
    public static final int RET = 0xa9; 
    public static final int TABLESWITCH = 0xaa; 
    public static final int LOOKUPSWITCH = 0xab; 
    public static final int IRETURN = 0xac; 
    public static final int LRETURN = 0xad; 
    public static final int FRETURN = 0xae; 
    public static final int DRETURN = 0xaf; 
    public static final int ARETURN = 0xb0;
    public static final int RETURN = 0xb1; 
    public static final int GETSTATIC = 0xb2; 
    public static final int PUTSTATIC = 0xb3; 
    public static final int GETFIELD = 0xb4; 
    public static final int PUTFIELD = 0xb5; 
    public static final int INVOKEVIRTUAL = 0xb6; 
    public static final int INVOKESPECIAL = 0xb7; 
    public static final int INVOKESTATIC = 0xb8;
    public static final int INVOKEINTERFACE = 0xb9;
    public static final int NEW = 0xbb; 
    public static final int NEWARRAY = 0xbc; 
    public static final int ANEWARRAY = 0xbd;
    public static final int ARRAYLENGTH = 0xbe; 
    public static final int ATHROW = 0xbf; 
    public static final int CHECKCAST = 0xc0;
    public static final int INSTANCEOF = 0xc1; 
    public static final int MONITORENTER = 0xc2; 
    public static final int MONITOREXIT = 0xc3; 
    public static final int WIDE = 0xc4; 
    public static final int MULTIANEWARRAY = 0xc5; 
    public static final int IFNULL = 0xc6; 
    public static final int IFNONNULL = 0xc7; 
    public static final int GOTO_W = 0xc8; 
    public static final int JSR_W = 0xc9; 
    
    

    //for a double linked list of bytecodes
    public ByteCode prev = null;
    public ByteCode next = null;

    public boolean isTarget = false;
    public JVMState beforeState = null; //state of the jvm just before executing this bc
    public int mvCheckCount = 0; //counts how often this is in checkQueue of a methodverifier
    public int svCheckCount = 0; //how often in checkQueue of SubroutineVerifier

    protected int opCode; //opCode of this bc
    protected byte[] byteArgs; //arguments from the bytecode for this operation
    protected int address; //address in the bytecode (0 = start)

    protected ByteCode[] targets; //all oprations that might possibly executed after this one. ExceptionHandlers are held separately
    protected ByteCode[] sources; //all operations that might have been executed just before this one. Again without exceptions
    public ExceptionHandler[] eHandlers; //all ExceptionHandlers that protect this bytecode
    public ExceptionHandler startsEH = null; //if this operation is the first op. of an exception handler, startsEH is a reference to this exception handler; normally null

    public ByteCode[] getTargets() {return targets;}
    public ByteCode[] getSources() {return sources;}

    public void addSource(ByteCode newEntry) {
	//FEHLER sehr aufwändig, vielleicht gehts anders einfacher.
	ByteCode[] ns = new ByteCode[sources.length+1];
	for (int i = 0; i < sources.length; i++) {
	    if (newEntry.getAddress() == sources[i].getAddress())
		return; //entry already registered!
	    ns[i] = sources[i];
	}
	ns[ns.length-1] = newEntry;
	sources = ns;
    }

     public String getBCName() {return getBCName(opCode);}
    public static String getBCName(int opCode) { 
	return ((opCode >= 0) && (opCode < OPNAMES.length))? OPNAMES[opCode] : null;}
    public int getOpCode() { return opCode;}
    public String getByteArgsString() {
	String ret = "";
	if (byteArgs == null) return ret;
	for (int i = 0; i < byteArgs.length; i++) {
	    ret += Integer.toHexString(byteArgs[i] &0xff) + " ";
	}
	return ret;
    }
    public byte[] getByteArgs() { return byteArgs;}
    public int getAddress() {return address;}
    public void setAddress(int newAddress) {address = newAddress;}

    public String toString() {
	return Integer.toHexString(address) + ": " +
	    getBCName() + "("+ Integer.toHexString(opCode) +") "+
	    getByteArgsString();
    }
	    
    //returns the size of this Bytecode in Bytes
    public int getSize() {
	return ((byteArgs != null)? byteArgs.length : 0) + 1; //arguments + 1 byte for opCode
    }
    
    protected ByteCode(int opCode, ByteIterator code){
	this.opCode = opCode;
	sources = new ByteCode[0];

	address = code.getIndex();
	if (OPNUMARGS[opCode] > 0) {
	    byteArgs = new byte[OPNUMARGS[opCode]];
	    for (int i = 0; i < byteArgs.length; i++) {
		byteArgs[i] = code.getNext();
	    }
	}
	
    }

    /** Create new Bytecode from codeBytes.
     * the address of the newly created bytecode is -1.
     */
    public ByteCode(byte[] codeBytes) {
	this.opCode = ((int) codeBytes[0])&0xff;
	sources = new ByteCode[0];
	address = -1;
	if (OPNUMARGS[opCode] > 0) {
	    byteArgs = new byte[OPNUMARGS[opCode]];
	    if (codeBytes.length < byteArgs.length+1) 
		throw new Error("Internal Error!");
	    for (int i = 0; i < byteArgs.length; i++) {
		byteArgs[i] = codeBytes[i+1];
	    }
	}
    }

    /**Adds the bytecode for this instruction to the bytecode array.
     * @param index the position where the bytecode should be written.
     * @param array the array to which the bc. should be written.
     * @return the index of the first byte after the bytes just added.
     */
    public int toByteArray(int index, byte[] array) {
	int ret = index;
	array[ret++] = (byte)opCode;
	if (byteArgs != null) {
	    for (int i = 0; i < byteArgs.length; i++)
		array[ret++] = byteArgs[i];
	}
	return ret;
    }

    public static ByteCode newByteCode(ByteIterator code, ConstantPool cPool, ByteCode prev) {
	return newByteCode(code.getNext(), code, cPool, prev);
    }
    public static ByteCode newByteCode(byte opCode, ByteIterator code, ConstantPool cPool, ByteCode prev) {
	int intOpCode = ((int)opCode)&0xff;
	return newByteCode(intOpCode, code, cPool, prev);
    }
    
    public static ByteCode newByteCode(int opCode, ByteIterator code, ConstantPool cPool, ByteCode prev) {
	if ((opCode < 0) || opCode >= OPNAMES.length) {
	    //FEHLER kein internal error, sondern ein verifyerror!
	    throw new Error("Internal Error: newByteCode called with invalid opCode (" + opCode + ")!");
	}
	if (code == null) {
	    throw new Error("Internal Error: newByteCode called code == null");
	}
	
	ByteCode retVal = null;
	switch (opCode) {
	case IFEQ:
	case IFNE:
	case IFLT:
	case IFGE:
	case IFGT:
	case IFLE:
	case IF_ICMPEQ:
	case IF_ICMPNE:
	case IF_ICMPLT:
	case IF_ICMPGE:
	case IF_ICMPGT:
	case IF_ICMPLE:
	case IF_ACMPEQ:
	case IF_ACMPNE:
	case IFNULL:
	case IFNONNULL:
	case GOTO:
	case JSR:
	case JSR_W:
	case RET:
	    retVal = new BCBranch(opCode, code);
	    break;
	case TABLESWITCH:
	case LOOKUPSWITCH:
	    retVal = new BCMultiBranch(opCode, code);
	    break;
	case WIDE:
	    retVal = new BCWideOp(opCode, code);
	    break;
	case ANEWARRAY:
	case CHECKCAST:
	case GETFIELD:
	case GETSTATIC:
	case INSTANCEOF:
	case INVOKEINTERFACE:
	case INVOKESPECIAL:
	case INVOKESTATIC:
	case INVOKEVIRTUAL:
	case LDC:
	case LDC_W:
	case LDC2_W:
	case MULTIANEWARRAY:
	case NEW:
	case PUTFIELD:
	case PUTSTATIC:
	    if (cPool == null) {
		throw new Error("Internal Error: newByteCode called with cPool == null!");
	    }
	    retVal = new BCCPArgOp(opCode, code, cPool);
	    break;
	default:
	    retVal = new ByteCode(opCode, code);
	}
	if (prev != null) {
	    retVal.prev = prev;
	    retVal.next = prev.next;
	    prev.next = retVal;
	    if (retVal.next != null) {
		retVal.next.prev = retVal;
	    }
	}
	return retVal;
    }

    public void linkTargets(BCLinkList bcl, ByteCode next) throws VerifyException { 
	switch(opCode) {
	case RETURN:
	case IRETURN:
	case LRETURN:
	case FRETURN:
	case DRETURN:
	case ARETURN:
	case ATHROW:
	    targets = new ByteCode[0];
	    break;
	default:
	    if(next != null) {
		targets = new ByteCode[1];
		targets[0] = next;
	    } else
		targets = new ByteCode[0];
	    
	}
	for (int i =0; i < targets.length; i++) {
	    targets[i].addSource(this);
	}
    }
    public void recomputeTargetAddresses() {
	//nothing to do for ordinary Bytecodes.
    }


}

