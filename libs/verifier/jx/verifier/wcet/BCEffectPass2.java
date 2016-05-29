package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import jx.classfile.constantpool.*;
import jx.classfile.ClassData;

public final class BCEffectPass2 {

    //returns how many words are pushed on the stack as result of this op.
    static public int getStackResults(ByteCode bCode) {
	int opCode = bCode.getOpCode();
	int retval = 0;
	int cPoolIndex = 0;
	String typeDesc, methodName;
	BCCPArgOp bCodeArg=null;
	switch(opCode) {
	case ByteCode.DUP2_X1:
	case ByteCode.DUP2_X2:
	    retval++;
	case ByteCode.DUP_X1:
	case ByteCode.DUP_X2:
	    retval++;
	case ByteCode.SWAP:
	    //these must be handled separately!!!
	    //throw new Error("Internal Error"); //nicht immer!
	    break;
	case ByteCode.GETFIELD:
	case ByteCode.GETSTATIC:
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8)&0xff00) |
		(((int) bCode.getByteArgs()[1])&0xff);
	    bCodeArg = (BCCPArgOp) bCode;
	    typeDesc = bCodeArg.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    retval = BCEffectPass0.countFromTypeDesc(typeDesc);
	    break;
	case ByteCode.INVOKEVIRTUAL:
	case ByteCode.INVOKEINTERFACE:
	case ByteCode.INVOKESPECIAL:
	case ByteCode.INVOKESTATIC:
	    cPoolIndex = (bCode.getByteArgs()[0]<<8)|
		(((int)bCode.getByteArgs()[1])&0xff);
	    
	    bCodeArg = (BCCPArgOp) bCode;
	    //get typedesc for method
	    if (opCode !=ByteCode.INVOKEINTERFACE) {
		typeDesc = bCodeArg.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		methodName = bCodeArg.getCP().methodRefEntryAt(cPoolIndex).getMemberName();
	    } else { //invokeinterface
		typeDesc = bCodeArg.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		    getMemberTypeDesc();
		methodName = bCodeArg.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		    getMemberName();
	    }
	    retval =  BCEffectPass0.countReturnTypeFromMethod(typeDesc);
	    break;
	case ByteCode.LDC:
	case ByteCode.LDC_W:
	case ByteCode.LDC2_W:
	    cPoolIndex = ((int) bCode.getByteArgs()[0]) & 0xff;
	    if (opCode == ByteCode.LDC_W|| opCode == ByteCode.LDC2_W) 
		cPoolIndex = (cPoolIndex <<8) | (((int) bCode.getByteArgs()[1])&0xff);
	    ConstantPoolEntry cpEntry = ((BCCPArgOp)bCode).getCP().entryAt(cPoolIndex);
	    switch (cpEntry.getTag()) {
	    case ConstantPoolEntry.CONSTANT_LONG:
	    case ConstantPoolEntry.CONSTANT_DOUBLE:
		retval = 2;
	    default:
		retval = 1;
	    }
	    break;
	default:
	    //default - for bcs with fixed number of parameters
	    retval = BCEffectPass0.PUSH[opCode];
	}
	return retval;
    }

    //returns how many words are popped from the stack to be operands of this op.
    static public int getStackOperands(ByteCode bCode) {
	int opCode = bCode.getOpCode();
	String typeDesc, methodName;
	int cPoolIndex;
	int retval = 0;
	
	switch(opCode) {
	case ByteCode.DUP2_X1:
	case ByteCode.DUP2_X2:
	case ByteCode.DUP_X1:
	case ByteCode.DUP_X2:
	case ByteCode.SWAP:
	    //these must be handled separately!!!
	    //throw new Error("Internal Error"); //nicht immer
	    retval = 0;
	    break;
	case ByteCode.PUTFIELD:
	    retval++;
	case ByteCode.PUTSTATIC:
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    typeDesc = ((BCCPArgOp)bCode).getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    retval += BCEffectPass0.countFromTypeDesc(typeDesc);
	    break;
	case ByteCode.INVOKEVIRTUAL:
	case ByteCode.INVOKEINTERFACE:
	case ByteCode.INVOKESPECIAL:
	case ByteCode.INVOKESTATIC:
	    cPoolIndex = (bCode.getByteArgs()[0]<<8)|
		(((int)bCode.getByteArgs()[1])&0xff);
	    //get typedesc for method
	    if (opCode !=ByteCode.INVOKEINTERFACE) {
		typeDesc = ((BCCPArgOp)bCode).getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
		methodName = ((BCCPArgOp)bCode).getCP().methodRefEntryAt(cPoolIndex).getMemberName();
	    } else { //invokeinterface
		typeDesc = ((BCCPArgOp)bCode).getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		    getMemberTypeDesc();
		methodName = ((BCCPArgOp)bCode).getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		    getMemberName();
	    }
	    retval += BCEffectPass0.countArgTypeFromMethod(typeDesc);
	    if (opCode != ByteCode.INVOKESTATIC) {
		retval++;
	    }
	    break;
	case ByteCode.MULTIANEWARRAY:
	    retval = bCode.getByteArgs()[2];
	    break;
	case ByteCode.ARRAYLENGTH:
	    //FEHLER soll doch der "benutzer" dann sagen müssen, welchen wert das annimmt.
	    //these bytecodes access something which value cannot be determined. So end
	    //this dependence path here.
	    retval = 0;
	    break;
	case ByteCode.GETFIELD:
	    retval = 1;
	    break;
	case ByteCode.IALOAD:
	case ByteCode.LALOAD:
	case ByteCode.FALOAD:
	case ByteCode.DALOAD:
	case ByteCode.AALOAD:
	case ByteCode.BALOAD:
	case ByteCode.CALOAD:
	case ByteCode.SALOAD:
	    //these bytecodes access something which value cannot be determined. So end
	    //this dependence path here.
	    retval = 0;
	    break;
	default:
	    //default - for bcs with fixed number of parameters
	    retval = BCEffectPass0.POP[opCode];
	}
	return retval;
    }

    //returns the indices of the lVars in which results of this bc are stored
    static public int[] getLVarsResults(ByteCode bc) {
	int opCode = bc.getOpCode();
	int[] retval;
	switch(opCode) {
	case ByteCode.DUP_X1:
	case ByteCode.DUP_X2:
	case ByteCode.DUP2_X1:
	case ByteCode.DUP2_X2:
	case ByteCode.SWAP:
	    //these must be handled separately!!!
	    //throw new Error("Internal Error"); //nicht immer!
	    retval = new int[0];
	    break;
	case ByteCode.LSTORE:
	case ByteCode.DSTORE:
	    retval = new int[2];
	    retval[0] = ((int)bc.getByteArgs()[0]) &0xff;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.ISTORE:
	case ByteCode.FSTORE:
	case ByteCode.ASTORE:
	case ByteCode.IINC:
	    retval = new int[1];
	    retval[0] = ((int)bc.getByteArgs()[0]) &0xff;
	    break;
	case ByteCode.ISTORE_0:
	case ByteCode.FSTORE_0:
	case ByteCode.ASTORE_0:
	    retval = new int[1];
	    retval[0] = 0;
	    break;
	case ByteCode.ISTORE_1:
	case ByteCode.FSTORE_1:
	case ByteCode.ASTORE_1:
	    retval = new int[1];
	    retval[0] = 1;
	    break;
	case ByteCode.ISTORE_2:
	case ByteCode.FSTORE_2:
	case ByteCode.ASTORE_2:
	    retval = new int[1];
	    retval[0] = 2;
	    break;
	case ByteCode.ISTORE_3:
	case ByteCode.FSTORE_3:
	case ByteCode.ASTORE_3:
	    retval = new int[1];
	    retval[0] = 3;
	    break;
	case ByteCode.LSTORE_0:
	case ByteCode.DSTORE_0:
	    retval = new int[2];
	    retval[0] = 0;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LSTORE_1:
	case ByteCode.DSTORE_1:
	    retval = new int[2];
	    retval[0] = 1;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LSTORE_2:
	case ByteCode.DSTORE_2:
	    retval = new int[2];
	    retval[0] = 2;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LSTORE_3:
	case ByteCode.DSTORE_3:
	    retval = new int[2];
	    retval[0] = 3;
	    retval[1] = retval[0]+1;
	    break;
	default:
	    retval = new int[0];
	}
	return retval;
    }

    //returns the indices of the lVars from which operands are read.
    static public int[] getLVarsOperands(ByteCode bc) {
	int opCode = bc.getOpCode();
	int[] retval;
	switch(opCode) {
	case ByteCode.DUP_X1:
	case ByteCode.DUP_X2:
	case ByteCode.DUP2_X1:
	case ByteCode.DUP2_X2:
	case ByteCode.SWAP:
	    //these must be handled separately!!!
	    //throw new Error("Internal Error"); //nicht immer
	    retval = new int[0];
	    break;
	case ByteCode.LLOAD:
	case ByteCode.DLOAD:
	    retval = new int[2];
	    retval[0] = ((int)bc.getByteArgs()[0]) &0xff;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.ILOAD:
	case ByteCode.FLOAD:
	case ByteCode.ALOAD:
	case ByteCode.IINC:
	    retval = new int[1];
	    retval[0] = ((int)bc.getByteArgs()[0]) &0xff;
	    break;
	case ByteCode.ILOAD_0:
	case ByteCode.FLOAD_0:
	case ByteCode.ALOAD_0:
	    retval = new int[1];
	    retval[0] = 0;
	    break;
	case ByteCode.ILOAD_1:
	case ByteCode.FLOAD_1:
	case ByteCode.ALOAD_1:
	    retval = new int[1];
	    retval[0] = 1;
	    break;
	case ByteCode.ILOAD_2:
	case ByteCode.FLOAD_2:
	case ByteCode.ALOAD_2:
	    retval = new int[1];
	    retval[0] = 2;
	    break;
	case ByteCode.ILOAD_3:
	case ByteCode.FLOAD_3:
	case ByteCode.ALOAD_3:
	    retval = new int[1];
	    retval[0] = 3;
	    break;
	case ByteCode.LLOAD_0:
	case ByteCode.DLOAD_0:
	    retval = new int[2];
	    retval[0] = 0;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LLOAD_1:
	case ByteCode.DLOAD_1:
	    retval = new int[2];
	    retval[0] = 1;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LLOAD_2:
	case ByteCode.DLOAD_2:
	    retval = new int[2];
	    retval[0] = 2;
	    retval[1] = retval[0]+1;
	    break;
	case ByteCode.LLOAD_3:
	case ByteCode.DLOAD_3:
	    retval = new int[2];
	    retval[0] = 3;
	    retval[1] = retval[0]+1;
	    break;
	default:
	    retval = new int[0];
	}
	return retval;
    }

}


