package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import jx.classfile.constantpool.*;
import jx.classfile.ClassData;

public final class BCEffectPass0 {

    // which types are popped from the stack when executing this bc
    public static final int POP[] = { 
	/*0x00 nop*/ 0,
	/*0x01 aconst_null*/ 0,
	/*0x02 iconst_m1*/ 0,
	/*0x03 iconst_0*/ 0,
	/*0x04 iconst_1*/ 0,
	/*0x05 iconst_2*/ 0,
	/*0x06 iconst_3*/ 0,
	/*0x07 iconst_4*/ 0,
	/*0x08 iconst_5*/ 0,
	/*0x09 lconst_0*/ 0,
	/*0x0a lconst_1*/ 0,
	/*0x0b fconst_0*/ 0,
	/*0x0c fconst_1*/ 0,
	/*0x0d fconst_2*/ 0,
	/*0x0e dconst_0*/ 0,
	/*0x0f dconst_1*/ 0,
	/*0x10 bipush*/ 0,
	/*0x11 sipush*/ 0,
	/*0x12 ldc*/ 0,
	/*0x13 ldc_w*/ 0,
	/*0x14 ldc2_w*/ 0,
	/*0x15 iload*/ 0,
	/*0x16 lload*/ 0,
	/*0x17 fload*/ 0,
	/*0x18 dload*/ 0,
	/*0x19 aload*/ 0,
	/*0x1a iload_0*/ 0,
	/*0x1b iload_1*/ 0,
	/*0x1c iload_2*/ 0,
	/*0x1d iload_3*/ 0,
	/*0x1e lload_0*/ 0,
	/*0x1f lload_1*/ 0,
	/*0x20 lload_2*/ 0,
	/*0x21 lload_3*/ 0,
	/*0x22 fload_0*/ 0,
	/*0x23 fload_1*/ 0,
	/*0x24 fload_2*/ 0,
	/*0x25 fload_3*/ 0,
	/*0x26 dload_0*/ 0,
	/*0x27 dload_1*/ 0,
	/*0x28 dload_2*/ 0,
	/*0x29 dload_3*/ 0,
	/*0x2a aload_0*/ 0,
	/*0x2b aload_1*/ 0,
	/*0x2c aload_2*/ 0,
	/*0x2d aload_3*/ 0,
	/*0x2e iaload*/ 2,
	/*0x2f laload*/ 2,
	/*0x30 faload*/ 2,
	/*0x31 daload*/ 2,
	/*0x32 aaload*/ 2,
	/*0x33 baload*/ 2,
	/*0x34 caload*/ 2,
	/*0x35 saload*/ 2,
	/*0x36 istore*/ 1,
	/*0x37 lstore*/ 2,
	/*0x38 fstore*/ 1,
	/*0x39 dstore*/ 2,
	/*0x3a astore*/ 1,
	/*0x3b istore_0*/ 1,
	/*0x3c istore_1*/ 1,
	/*0x3d istore_2*/ 1,
	/*0x3e istore_3*/ 1,
	/*0x3f lstore_0*/ 2,
	/*0x40 lstore_1*/ 2,
	/*0x41 lstore_2*/ 2,
	/*0x42 lstore_3*/ 2,
	/*0x43 fstore_0*/ 1,
	/*0x44 fstore_1*/ 1,
	/*0x45 fstore_2*/ 1,
	/*0x46 fstore_3*/ 1,
	/*0x47 dstore_0*/ 2,
	/*0x48 dstore_1*/ 2,
	/*0x49 dstore_2*/ 2,
	/*0x4a dstore_3*/ 2,
	/*0x4b astore_0*/ 1,
	/*0x4c astore_1*/ 1,
	/*0x4d astore_2*/ 1,
	/*0x4e astore_3*/ 1,
	/*0x4f iastore*/ 3,
	/*0x50 lastore*/ 4,
	/*0x51 fastore*/ 3,
	/*0x52 dastore*/ 4,
	/*0x53 aastore*/ 3,
	/*0x54 bastore*/ 3,
	/*0x55 castore*/ 3,
	/*0x56 sastore*/ 3,
	/*0x57 pop*/ 1,
	/*0x58 pop2*/ 2,
	/*0x59 dup*/ 0,
	/*0x5a dup_x1*/ 0,
	/*0x5b dup_x2*/ 0,
	/*0x5c dup2*/ 0,
	/*0x5d dup2_x1*/ 0,
	/*0x5e dup2_x2*/ 0,
	/*0x5f swap*/ 0,
	/*0x60 iadd*/ 2,
	/*0x61 ladd*/ 4,
	/*0x62 fadd*/ 2,
	/*0x63 dadd*/ 4,
	/*0x64 isub*/ 2,
	/*0x65 lsub*/ 4,
	/*0x66 fsub*/ 2,
	/*0x67 dsub*/ 4,
	/*0x68 imul*/ 2,
	/*0x69 lmul*/ 4,
	/*0x6a fmul*/ 2,
	/*0x6b dmul*/ 4,
	/*0x6c idiv*/ 2,
 	/*0x6d ldiv*/ 4,
	/*0x6e fdiv*/ 2,
	/*0x6f ddiv*/ 4,
	/*0x70 irem*/ 2,
	/*0x71 lrem*/ 4,
	/*0x72 frem*/ 2,
	/*0x73 drem*/ 4,
	/*0x74 ineg*/ 1,
	/*0x75 lneg*/ 2,
	/*0x76 fneg*/ 1,
	/*0x77 dneg*/ 2,
	/*0x78 ishl*/ 2,
	/*0x79 lshl*/ 3,
	/*0x7a ishr*/ 2,
	/*0x7b lshr*/ 3,
	/*0x7c iushr*/ 2,
	/*0x7d lushr*/ 3,
	/*0x7e iand*/ 2,
	/*0x7f land*/ 4,
	/*0x80 ior*/ 2,
	/*0x81 lor*/ 4,
	/*0x82 ixor*/ 2,
	/*0x83 lxor*/ 4,
	/*0x84 iinc*/ 0,
	/*0x85 i2l*/ 1,
	/*0x86 i2f*/ 1,
	/*0x87 i2d*/ 1,
	/*0x88 l2i*/ 2,
	/*0x89 l2f*/ 2,
	/*0x8a l2d*/ 2,
	/*0x8b f2i*/ 1,
	/*0x8c f2l*/ 1,
	/*0x8d f2d*/ 1,
	/*0x8e d2i*/ 2,
	/*0x8f d2l*/ 2,
	/*0x90 d2f*/ 2,
	/*0x91 i2b*/ 1,
	/*0x92 i2c*/ 1,
	/*0x93 i2s*/ 1,
	/*0x94 lcmp*/ 4,
	/*0x95 fcmpl*/ 2,
	/*0x96 fcmpg*/ 2,
	/*0x97 dcmpl*/ 4, 
	/*0x98 dcmpg*/ 4,
	/*0x99 ifeq*/ 1,
	/*0x9a ifne*/ 1,
	/*0x9b iflt*/ 1,
	/*0x9c ifge*/ 1,
	/*0x9d ifgt*/ 1,
	/*0x9e ifle*/ 1,
	/*0x9f if_icmpeq*/ 2,
	/*0xa0 if_icmpne*/ 2,
	/*0xa1 if_icmplt*/ 2,
	/*0xa2 if_icmpge*/ 2,
	/*0xa3 if_icmpgt*/ 2,
	/*0xa4 if_icmple*/ 2,
	/*0xa5 if_acmpeq*/ 2,
	/*0xa6 if_acmpne*/ 2,
	/*0xa7 goto*/ 0,
	/*0xa8 jsr*/ 0,
	/*0xa9 ret*/ 0,
	/*0xaa tableswitch*/ 1,
	/*0xab lookupswitch*/ 1,
	/*0xac ireturn*/ 1,
	/*0xad lreturn*/ 2,
	/*0xae freturn*/ 1,
	/*0xaf dreturn*/ 2,
	/*0xb0 areturn*/ 1,
	/*0xb1 return*/ 0,
	/*0xb2 getstatic*/ 0,
	/*0xb3 putstatic*/ 0,
	/*0xb4 getfield*/ 1,
	/*0xb5 putfield*/ 0,
	/*0xb6 invokevirtual*/ 0,
	/*0xb7 invokespecial*/ 0,
	/*0xb8 invokestatic*/ 0,
	/*0xb9 invokeinterface*/ 0,
	/*0xba xxxunusedxxx*/ 0,
	/*0xbb new*/ 0,
	/*0xbc newarray*/ 1,
	/*0xbd anewarray*/ 1,
	/*0xbe arraylength*/ 1,
	/*0xbf athrow*/ 1,
	/*0xc0 checkcast*/ 0,
	/*0xc1 instanceof*/ 1,
	/*0xc2 monitorenter*/ 1,
	/*0xc3 monitorexit*/ 1,
	/*0xc4 wide*/ 0,
	/*0xc5 multianewarray*/ 0,
	/*0xc6 ifnull*/ 1,
	/*0xc7 ifnonnull*/ 1,
	/*0xc8 goto_w*/ 0,
	/*0xc9 jsr_w*/ 0 
    };


    // which types are pushed onto the stack when executing this bc
    public static final int PUSH[] = {
	/*0x00 nop*/ 0,
	/*0x01 aconst_null*/ 1,
	/*0x02 iconst_m1*/ 1,
	/*0x03 iconst_0*/ 1,
	/*0x04 iconst_1*/ 1,
	/*0x05 iconst_2*/ 1,
	/*0x06 iconst_3*/ 1,
	/*0x07 iconst_4*/ 1,
	/*0x08 iconst_5*/ 1,
	/*0x09 lconst_0*/ 2,
	/*0x0a lconst_1*/ 2,
	/*0x0b fconst_0*/ 1,
	/*0x0c fconst_1*/ 1,
	/*0x0d fconst_2*/ 1,
	/*0x0e dconst_0*/ 2,
	/*0x0f dconst_1*/ 2,
	/*0x10 bipush*/ 1,
	/*0x11 sipush*/ 1,
	/*0x12 ldc*/ 1,
	/*0x13 ldc_w*/ 1,
	/*0x14 ldc2_w*/ 2,
	/*0x15 iload*/ 1,
	/*0x16 lload*/ 2,
	/*0x17 fload*/ 1,
	/*0x18 dload*/ 2,
	/*0x19 aload*/ 1,
	/*0x1a iload_0*/ 1,
	/*0x1b iload_1*/ 1,
	/*0x1c iload_2*/ 1,
	/*0x1d iload_3*/ 1,
	/*0x1e lload_0*/ 2,
	/*0x1f lload_1*/ 2,
	/*0x20 lload_2*/ 2,
	/*0x21 lload_3*/ 2,
	/*0x22 fload_0*/ 1,
	/*0x23 fload_1*/ 1,
	/*0x24 fload_2*/ 1,
	/*0x25 fload_3*/ 1,
	/*0x26 dload_0*/ 2,
	/*0x27 dload_1*/ 2,
	/*0x28 dload_2*/ 2,
	/*0x29 dload_3*/ 2,
	/*0x2a aload_0*/ 1,
	/*0x2b aload_1*/ 1,
	/*0x2c aload_2*/ 1,
	/*0x2d aload_3*/ 1,
	/*0x2e iaload*/ 1,
	/*0x2f laload*/ 2,
	/*0x30 faload*/ 1,
	/*0x31 daload*/ 2,
	/*0x32 aaload*/ 1,
	/*0x33 baload*/ 1,
	/*0x34 caload*/ 1,
	/*0x35 saload*/ 1,//byte, char, short all pushed as ints!
	/*0x36 istore*/ 0,
	/*0x37 lstore*/ 0,
	/*0x38 fstore*/ 0,
	/*0x39 dstore*/ 0,
	/*0x3a astore*/ 0,
	/*0x3b istore_0*/ 0,
	/*0x3c istore_1*/ 0,
	/*0x3d istore_2*/ 0,
	/*0x3e istore_3*/ 0,
	/*0x3f lstore_0*/ 0,
	/*0x40 lstore_1*/ 0,
	/*0x41 lstore_2*/ 0,
	/*0x42 lstore_3*/ 0,
	/*0x43 fstore_0*/ 0,
	/*0x44 fstore_1*/ 0,
	/*0x45 fstore_2*/ 0,
	/*0x46 fstore_3*/ 0,
	/*0x47 dstore_0*/ 0,
	/*0x48 dstore_1*/ 0,
	/*0x49 dstore_2*/ 0,
	/*0x4a dstore_3*/ 0,
	/*0x4b astore_0*/ 0,
	/*0x4c astore_1*/ 0,
	/*0x4d astore_2*/ 0,
	/*0x4e astore_3*/ 0,
	/*0x4f iastore*/ 0,
	/*0x50 lastore*/ 0,
	/*0x51 fastore*/ 0,
	/*0x52 dastore*/ 0,
	/*0x53 aastore*/ 0,
	/*0x54 bastore*/ 0,
	/*0x55 castore*/ 0,
	/*0x56 sastore*/ 0,
	/*0x57 pop*/ 0,
	/*0x58 pop2*/ 0,
	/*0x59 dup*/ 1, 
	/*0x5a dup_x1*/ 1,
	/*0x5b dup_x2*/ 1,
	/*0x5c dup2*/ 2,
	/*0x5d dup2_x1*/ 2,
	/*0x5e dup2_x2*/ 2,
	/*0x5f swap*/ 0,
	/*0x60 iadd*/ 1,
	/*0x61 ladd*/ 2,
	/*0x62 fadd*/ 1,
	/*0x63 dadd*/ 2,
	/*0x64 isub*/ 1,
	/*0x65 lsub*/ 2,
	/*0x66 fsub*/ 1,
	/*0x67 dsub*/ 2,
	/*0x68 imul*/ 1,
	/*0x69 lmul*/ 2,
	/*0x6a fmul*/ 1,
	/*0x6b dmul*/ 2,
	/*0x6c idiv*/ 1,
 	/*0x6d ldiv*/ 2,
	/*0x6e fdiv*/ 1,
	/*0x6f ddiv*/ 2,
	/*0x70 irem*/ 1,
	/*0x71 lrem*/ 2,
	/*0x72 frem*/ 1,
	/*0x73 drem*/ 2,
	/*0x74 ineg*/ 1,
	/*0x75 lneg*/ 2,
	/*0x76 fneg*/ 1,
	/*0x77 dneg*/ 2,
	/*0x78 ishl*/ 1,
	/*0x79 lshl*/ 2,
	/*0x7a ishr*/ 1,
	/*0x7b lshr*/ 2,
	/*0x7c iushr*/ 1,
	/*0x7d lushr*/ 2,
	/*0x7e iand*/ 1,
	/*0x7f land*/ 2,
	/*0x80 ior*/ 1,
	/*0x81 lor*/ 2,
	/*0x82 ixor*/ 1,
	/*0x83 lxor*/ 2,
	/*0x84 iinc*/ 0,
	/*0x85 i2l*/ 2,
	/*0x86 i2f*/ 1,
	/*0x87 i2d*/ 2,
	/*0x88 l2i*/ 1,
	/*0x89 l2f*/ 1,
	/*0x8a l2d*/ 2,
	/*0x8b f2i*/ 1,
	/*0x8c f2l*/ 2,
	/*0x8d f2d*/ 2,
	/*0x8e d2i*/ 1,
	/*0x8f d2l*/ 2,
	/*0x90 d2f*/ 1,
	/*0x91 i2b*/ 1,
	/*0x92 i2c*/ 1,
	/*0x93 i2s*/ 1,
	/*0x94 lcmp*/ 1,
	/*0x95 fcmpl*/ 1,
	/*0x96 fcmpg*/ 1,
	/*0x97 dcmpl*/ 1,
	/*0x98 dcmpg*/ 1,
	/*0x99 ifeq*/ 0,
	/*0x9a ifne*/ 0,
	/*0x9b iflt*/ 0,
	/*0x9c ifge*/ 0,
	/*0x9d ifgt*/ 0,
	/*0x9e ifle*/ 0,
	/*0x9f if_icmpeq*/ 0,
	/*0xa0 if_icmpne*/ 0,
	/*0xa1 if_icmplt*/ 0,
	/*0xa2 if_icmpge*/ 0,
	/*0xa3 if_icmpgt*/ 0,
	/*0xa4 if_icmple*/ 0,
	/*0xa5 if_acmpeq*/ 0,
	/*0xa6 if_acmpne*/ 0,
	/*0xa7 goto*/ 0,
	/*0xa8 jsr*/ 0,
	/*0xa9 ret*/ 0,
	/*0xaa tableswitch*/ 0,
	/*0xab lookupswitch*/ 0,
	/*0xac ireturn*/ 0,
	/*0xad lreturn*/ 0,
	/*0xae freturn*/ 0,
	/*0xaf dreturn*/ 0,
	/*0xb0 areturn*/ 0,
	/*0xb1 return*/ 0,
	/*0xb2 getstatic*/ 0,
	/*0xb3 putstatic*/ 0,
	/*0xb4 getfield*/ 0,
	/*0xb5 putfield*/ 0,
	/*0xb6 invokevirtual*/ 0,
	/*0xb7 invokespecial*/ 0,
	/*0xb8 invokestatic*/ 0,
	/*0xb9 invokeinterface*/ 0,
	/*0xba xxxunusedxxx*/ 0,
	/*0xbb new*/ 1,
	/*0xbc newarray*/ 1,
	/*0xbd anewarray*/ 1,
	/*0xbe arraylength*/ 1,
	/*0xbf athrow*/ 0,
	/*0xc0 checkcast*/ 0,
	/*0xc1 instanceof*/ 1,
	/*0xc2 monitorenter*/ 0,
	/*0xc3 monitorexit*/ 0,
	/*0xc4 wide*/ 0,
	/*0xc5 multianewarray*/ 1,
	/*0xc6 ifnull*/ 0,
	/*0xc7 ifnonnull*/ 0,
	/*0xc8 goto_w*/ 0,
	/*0xc9 jsr_w*/ 0
    };

    static public JVMState[] simulateBC(WCETState state) 
	throws VerifyException {
	ByteCode bCode = state.getNextBC();
	int bc = bCode.getOpCode();

	if (bc == ByteCode.JSR || bc == ByteCode.JSR_W) { 
	    jsr(bc, bCode, state); 
	} else if (bc <= 0x11) { //<t>consts, [b|s]ipush
	    genericOp(bc, bCode, state);
	} else if (bc <= 0x14) { //ldcs
	    ldcOp(bc, (BCCPArgOp) bCode, state);
	} else if (bc <= 0xb1) {
	    //FEHLER ret muss noch extra behandelt werden!!!
	    genericOp(bc, bCode, state);
	} else if (bc <= 0xb5) { //[get|put][static|field]
	    fieldOp(bc, (BCCPArgOp) bCode, state);
  	} else if (bc <= 0xb9) { //invokes
	    invoke(bc, (BCCPArgOp) bCode, state);
	} else if (bc <= 0xba) { //xxxUNUSEDxxx
	    throw new VerifyException("BCStackEffect.simulateBC: Bytecode " +bCode+"found!");
	} else if (bc <= 0xc4) { //arraylength, athrow, checkcast, instanceof, monitorenter/exit, wide, new, anewarray, newarray
	    genericOp(bc, bCode, state);
	} else if (bc <= 0xc5) { //multianewarray
	    objectCreationOp(bc, bCode, state);
	} else { //if<non>null, goto_w
	    genericOp(bc, bCode, state);
	}

	return null;
    }

    static private void genericOp(int bc, ByteCode bCode, WCETState state) 
    throws VerifyException {
	for (int i=0; i < POP[bc]; i++) 
	    state.getStack().pop();
	for (int i=0; i < PUSH[bc]; i++)
	    state.WCETgetStack().push(bCode.getAddress());
    }

    static public void jsr(int bc, ByteCode bCode, WCETState state) {
	//do nothing. all done by JVMState.
	//FEHLER stimmt das so?
    }

    //how many datawords are in typeDesc
    static public int countFromTypeDesc(String typeDesc) {
	return jx.verifier.typecheck.TCTypes.typeFromTypeDesc(typeDesc).length;
    }


    //[get|put][static|field]
    static private void fieldOp(int bc, BCCPArgOp bCode, WCETState state) 
	throws VerifyException {
	String typeDesc;
	int cPoolIndex;

	switch(bc) {
	case ByteCode.GETFIELD:
	    state.getStack().pop();
	case ByteCode.GETSTATIC:
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8)&0xff00) |
		(((int) bCode.getByteArgs()[1])&0xff);
	    typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    for (int i=countFromTypeDesc(typeDesc); i > 0 ; i--) {
		state.WCETgetStack().push(bCode.getAddress());
	    }
	    break;
	case ByteCode.PUTFIELD:
	case ByteCode.PUTSTATIC:
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    for (int i = countFromTypeDesc(typeDesc); i>0; i--) {
		state.getStack().pop();
	    }
	    if (bc == ByteCode.PUTSTATIC) break;
	    state.getStack().pop();
	    break;
	default:
	    throw new Error("Internal Error: BCStackEffect.fieldOp called with wrong bytecode!");
	}
    }

    //how many parameters recieves a method of type typeDesc
    static public int countArgTypeFromMethod(String typeDesc) {
	return jx.verifier.typecheck.TCTypes.argTypeFromMethod(typeDesc).length;
    }
    //what is the size of the return value.
    static public  int countReturnTypeFromMethod(String typeDesc) {
	return jx.verifier.typecheck.TCTypes.returnTypeFromMethod(typeDesc).length;
    }

    //invokes
    static private void invoke (int bc, BCCPArgOp bCode, WCETState state) 
	throws VerifyException {

	int cPoolIndex;
	String typeDesc, methodName;
	cPoolIndex = (bCode.getByteArgs()[0]<<8)|
	    (((int)bCode.getByteArgs()[1])&0xff);

	//get typedesc for method
	if (bc !=ByteCode.INVOKEINTERFACE) {
	    typeDesc = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    methodName = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberName();
	} else { //invokeinterface
	    typeDesc = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		getMemberTypeDesc();
	    methodName = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		getMemberName();
	}

	for (int i= countArgTypeFromMethod(typeDesc); i>0; i--) {
	    state.getStack().pop();
	}


	if (bc != ByteCode.INVOKESTATIC) {
	    state.getStack().pop();
	}

	for (int i= countReturnTypeFromMethod(typeDesc); i>0; i--) {
	    state.WCETgetStack().push(bCode.getAddress());
	}

    }

    //multianewarray
    static private void objectCreationOp(int bc, ByteCode bCode, WCETState state) 
	throws VerifyException {
	if (bc != ByteCode.MULTIANEWARRAY) {
	    throw new Error("objectCreationOp called with invalid opcode (" + bCode+")");
	}
	
	BCCPArgOp bCodeCP = (BCCPArgOp) bCode;
	//i dimensions
	for (int i = 0; i < bCodeCP.getByteArgs()[2]; i++) {
	    state.getStack().pop();
	}
	state.WCETgetStack().push(bCodeCP.getAddress());
    }
    
    //ldcs
    static private void ldcOp(int bc, BCCPArgOp bCode, WCETState state) throws VerifyException{
	int cPoolIndex = ((int) bCode.getByteArgs()[0]) & 0xff;
	if (bc == ByteCode.LDC_W|| bc == ByteCode.LDC2_W) 
	    cPoolIndex = (cPoolIndex <<8) | (((int) bCode.getByteArgs()[1])&0xff);
	ConstantPoolEntry cpEntry = bCode.getCP().entryAt(cPoolIndex);
	switch (cpEntry.getTag()) {
	case ConstantPoolEntry.CONSTANT_LONG:
	case ConstantPoolEntry.CONSTANT_DOUBLE:
	    state.WCETgetStack().push(bCode.getAddress());
	    state.WCETgetStack().push(bCode.getAddress());
	    break;
	default:
	    state.WCETgetStack().push(bCode.getAddress());
	}

    }

}


