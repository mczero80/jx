package jx.verifier.npa;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import jx.classfile.constantpool.*;
import jx.classfile.ClassData;

public final class NPABCEffect {
    
    
    static public NPAState[] simulateBC(NPAState state, ByteCode bCode) 
	throws VerifyException {
	    
	    switch (bCode.getOpCode()) {
		case ByteCode.GETFIELD:
		case ByteCode.GETSTATIC:
		case ByteCode.AALOAD:
		loadFromObject(state, bCode);
		return null;
		case ByteCode.PUTFIELD:
		case ByteCode.PUTSTATIC:
		case ByteCode.AASTORE:
		storeInObject(state, bCode);
		return null;
		case ByteCode.ACONST_NULL:
		case ByteCode.MULTIANEWARRAY:
		case ByteCode.ANEWARRAY:
		case ByteCode.NEWARRAY:
		case ByteCode.NEW:
		case ByteCode.LDC:
		case ByteCode.LDC_W:
		createNewRef(state, bCode);
		return null;
		case ByteCode.INVOKEINTERFACE:
		case ByteCode.INVOKESTATIC:
		case ByteCode.INVOKEVIRTUAL:
		case ByteCode.INVOKESPECIAL:
		case ByteCode.ARETURN:
		methodOp(state, bCode);
		return null;
		case ByteCode.IFNONNULL:
		case ByteCode.IFNULL:
		return ifOp(state, bCode);
		case ByteCode.ALOAD:
		case ByteCode.ALOAD_0:
		case ByteCode.ALOAD_1:
		case ByteCode.ALOAD_2:
		case ByteCode.ALOAD_3:
		case ByteCode.ASTORE:
		case ByteCode.ASTORE_0:
		case ByteCode.ASTORE_1:
		case ByteCode.ASTORE_2:
		case ByteCode.ASTORE_3:
		case ByteCode.DUP:
		case ByteCode.DUP_X1:
		case ByteCode.DUP_X2:
		case ByteCode.DUP2:
		case ByteCode.DUP2_X1:
		case ByteCode.DUP2_X2:
		case ByteCode.SWAP:
		copyOp(state, bCode);
		return null;
		case ByteCode.MONITORENTER:
		case ByteCode.MONITOREXIT:
		runtimeCheck(state, bCode, state.NPAgetStack().NPApop());
		return null;
	    default:
		genericOp(state, bCode);
		return null;
	    }
	}
    
    //all bytecodes that load refs from an object
    //aaload, getfield and getstatic
    static private void loadFromObject(NPAState state, ByteCode bCode) 
	throws VerifyException {
	switch(bCode.getOpCode()) {
	case ByteCode.GETFIELD:
	    runtimeCheck(state, bCode, state.NPAgetStack().NPApeek());
	    state.getStack().pop(); 
	case ByteCode.GETSTATIC:
	    BCCPArgOp bc = (BCCPArgOp)bCode;
	    int cPoolIndex = ((bc.getByteArgs()[0] <<8)&0xff00) |
		(((int) bc.getByteArgs()[1])&0xff);

	    String typeDesc = bc.getCP().
		fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
 	    state.NPAgetStack().push(NPAValue.typeFromTypeDesc(typeDesc), bc.getAddress());
	    break;
	case ByteCode.AALOAD:
	    state.getStack().pop(); //index
	    runtimeCheck(state, bCode, state.NPAgetStack().NPApop()); //arrayref
	    state.NPAgetStack().push(NPAValue.newOTHER(), bCode.getAddress());
	    break;
	default:
	    throw new Error("Internal Error!");
	}
    }
    //all bytecodes that save refs in an object
    //aastore, putfield, putstatic
    static private void storeInObject(NPAState state, ByteCode bCode) 
    throws VerifyException {
	switch(bCode.getOpCode()) {
	case ByteCode.PUTFIELD:
	case ByteCode.PUTSTATIC:
	    BCCPArgOp bc = (BCCPArgOp)bCode;
	    int cPoolIndex = ((bc.getByteArgs()[0] <<8)&0xff00) |
		(((int) bc.getByteArgs()[1])&0xff);
	    
	    String typeDesc = bc.getCP().
		fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    int count = NPAValue.typeFromTypeDesc(typeDesc).length;
	    for (int i = 0; i<count; i++)
		state.getStack().pop(); //value
	    if (bCode.getOpCode() == ByteCode.PUTFIELD)
		runtimeCheck(state, bCode, state.NPAgetStack().NPApop()); //ref
	    break;
	case ByteCode.AASTORE:
	    state.getStack().pop(); //value
	    state.getStack().pop(); //index
	    runtimeCheck(state, bCode, state.NPAgetStack().NPApop()); //arrayref
	    break;
	default:
	    throw new Error("Internal Error!");
	}
	
    }
    //all bytecodes that create new refs
    //aconst_null, [[multi]a]newarray, new, ldc[_w]
    static private void createNewRef(NPAState state, ByteCode bCode) throws VerifyException{
	int cPoolIndex;
	BCCPArgOp bCodeCP;
	switch(bCode.getOpCode()) {
	case ByteCode.ACONST_NULL:
	    state.NPAgetStack().push(NPAValue.newNULL(), bCode.getAddress());
	    break;
	case ByteCode.MULTIANEWARRAY:
	    cPoolIndex = (( bCode.getByteArgs()[0]<<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	     bCodeCP = (BCCPArgOp) bCode;
	    //i dimensions
	    for (int i = 0; i < bCodeCP.getByteArgs()[2]; i++) {
		state.getStack().pop();
	    }
	    
	    state.NPAgetStack().push(NPAValue.newNONNULL(), bCode.getAddress());
	    break;
	case ByteCode.ANEWARRAY:
	case ByteCode.NEWARRAY:
	    state.getStack().pop();
	    state.NPAgetStack().push(NPAValue.newNONNULL(),
				     bCode.getAddress());
	    break;
	case ByteCode.NEW:
	    state.NPAgetStack().push(NPAValue.newNONNULL(),
				     bCode.getAddress());
	    break;
	case ByteCode.LDC:
	case ByteCode.LDC_W:
	    bCodeCP = (BCCPArgOp) bCode;
	    cPoolIndex = ((int) bCodeCP.getByteArgs()[0]) & 0xff;
	    if (bCode.getOpCode() == ByteCode.LDC_W) 
		cPoolIndex = (cPoolIndex <<8) | (((int) bCodeCP.getByteArgs()[1])&0xff);
	    ConstantPoolEntry cpEntry = bCodeCP.getCP().entryAt(cPoolIndex);
	    if (cpEntry.getTag() == ConstantPoolEntry.CONSTANT_STRING) {
		state.NPAgetStack().push(NPAValue.newNONNULL(),
					 bCodeCP.getAddress());
	    } else {
		state.NPAgetStack().push(NPAValue.newOTHER(),
					 bCodeCP.getAddress());
	    }
	    break;
	default:
	    throw new Error("Internal Error! BC " + bCode);
	}
	 
    }
    //all bytecodes concerning method invocation and areturn
    //invokes and return
    static private void methodOp(NPAState state, ByteCode byteCode) throws VerifyException {
	if (byteCode.getOpCode() == ByteCode.ARETURN) {
	    state.getStack().pop();
	    return;
	}
	int cPoolIndex;
	String typeDesc, methodName;
	int bc = byteCode.getOpCode();
	BCCPArgOp bCode = (BCCPArgOp) byteCode;
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
	NPAValue[] args = NPAValue.argTypeFromMethod(typeDesc);
	
	for (int i = args.length-1;i >=0; i-- ) {
	    state.getStack().pop();
	}
	if (bc != ByteCode.INVOKESTATIC) {
	    runtimeCheck(state, byteCode, state.NPAgetStack().NPApeek());
	    state.getStack().pop();
	}

	NPAValue[] ret = NPAValue.returnTypeFromMethod(typeDesc);
	state.NPAgetStack().push(ret, bCode.getAddress());

     }

    //all if constructs concerning refs; returns all branches that cannot yet be discarded
    //ifnull, ifnonnull, ifacmpne, ifacmpeq
    static private NPAState[] ifOp(NPAState state, ByteCode bCode) throws VerifyException {
	NPAState[] retval = null;

	NPAValue  value = state.NPAgetStack().NPApop();

	switch(bCode.getOpCode()) {
	case ByteCode.IFNONNULL:
	    if (value.getValue() == NPAValue.NULL) {
		retval = new NPAState[1];
		retval[0] = state;
		//continue execution with next bc
		retval[0].setNextBC(bCode.getTargets()[0]);
		return retval;
	    } else if (value.getValue() == NPAValue.NONNULL) {
		retval = new NPAState[1];
		retval[0] = state;
		//jump
		retval[0].setNextBC(bCode.getTargets()[1]);
		return retval;
	    } else {
		//continue at both ends; one with value set to null, the other to nonnull
		retval = new NPAState[2];
		retval[0] = state;
		retval[1] = (NPAState)state.copy();
		//continue
		retval[0].setNextBC(bCode.getTargets()[0]);
		setValue(retval[0], value, NPAValue.NULL);
		//jump
		retval[1].setNextBC(bCode.getTargets()[1]);
		setValue(retval[1], value, NPAValue.NONNULL);
		return retval;
		
	    }

	case ByteCode.IFNULL:
	    if (value.getValue() == NPAValue.NONNULL) {
		retval = new NPAState[1];
		retval[0] = state;
		//continue execution with next bc
		retval[0].setNextBC(bCode.getTargets()[0]);
		return retval;
	    } else if (value.getValue() == NPAValue.NULL) {
		retval = new NPAState[1];
		retval[0] = state;
		//jump
		retval[0].setNextBC(bCode.getTargets()[1]);
		return retval;
	    } else {
		//continue at both ends; one with value set to null, the other to nonnull
		retval = new NPAState[2];
		retval[0] = state;
		retval[1] = (NPAState)state.copy();
		//continue
		retval[0].setNextBC(bCode.getTargets()[0]);
		setValue(retval[0], value, NPAValue.NONNULL);
		//jump
		retval[1].setNextBC(bCode.getTargets()[1]);
		setValue(retval[1], value, NPAValue.NULL);
		return retval;
	    }
	default:
	    throw new Error("Internal Error");
	}

    }

    static private void genericOp(NPAState state, ByteCode bCode) throws VerifyException {
	int bc = bCode.getOpCode();
	//pops
	for (int i = 0; i < jx.verifier.typecheck.BCStackEffect.POP[bc].length; i++) {
	    state.getStack().pop();
	}
	//pushs
	for (int i = 0; i < jx.verifier.typecheck.BCStackEffect.PUSH[bc].length; i++) {
	    state.NPAgetStack().push(new NPAValue(NPAValue.OTHER), bCode.getAddress());
	}

	//NOTE: local Variables are not handled. only aload and astore are implemented, 
	//because all other local variable accesses are irrelevant for NPA:
	//all non-reference values are irrelevant, and reference-values must have been written
	//with astore. Because the bytecode has been typechecked before, there must always
	//be an astore prior to an aload, so there is no need to change values to OTHER
	//when writing non-reference values.
	
    }

    //all Ops that copy a value from one location (local Var, stack slot) to another
    //aload[_#], astore[_#], dup[_x#], dup2[_x#], swap
    static public void copyOp(NPAState state, ByteCode bCode) throws VerifyException {
	if (bCode.getOpCode()== ByteCode.ALOAD ||
	    (bCode.getOpCode() >=ByteCode.ALOAD_0 && 
	     bCode.getOpCode() <= ByteCode.ALOAD_3)) {
	    int index = 0;
	    switch(bCode.getOpCode()) {
	    case ByteCode.ALOAD:
		index = ((int)bCode.getByteArgs()[0]) &0xff;
		break;
	    case ByteCode.ALOAD_0:
		index = 0;
		break;
	    case ByteCode.ALOAD_1:
		index = 1;
		break;
	    case ByteCode.ALOAD_2:
		index = 2;
		break;
	    case ByteCode.ALOAD_3:
		index = 3;
		break;
	    }
	    //element will be copied, so id should be valid!
	    NPAValue tmp = state.NPAgetLVars().NPAread(index);
	    tmp.setValidId();
	    state.NPAgetStack().push(tmp.copy(),
				     bCode.getAddress());
	    return;
	}
	//Stores
	if (bCode.getOpCode()== ByteCode.ASTORE ||
	    (bCode.getOpCode() >=ByteCode.ASTORE_0 && 
	     bCode.getOpCode() <= ByteCode.ASTORE_3)) {
	    int index = 0;
	    switch(bCode.getOpCode()) {
	    case ByteCode.ASTORE:
		index = ((int)bCode.getByteArgs()[0]) &0xff;
		break;
	    case ByteCode.ASTORE_0:
		index = 0;
		break;
	    case ByteCode.ASTORE_1:
		index = 1;
		break;
	    case ByteCode.ASTORE_2:
		index = 2;
		break;
	    case ByteCode.ASTORE_3:
		index = 3;
		break;
	    }
	    //element will be copied, so id should be valid!
	    state.NPAgetStack().NPApeek().setValidId();
	    state.NPAgetLVars().write(index, state.NPAgetStack().NPApop().copy(), 
				     bCode.getAddress());
	    return;
	}

	//stackops
	NPAValue e1, e2, e3;
	NPAStackElement ne1, ne2, ne3, ne4;
	switch(bCode.getOpCode()) {
	case ByteCode.DUP:
	    state.NPAgetStack().NPApeek().setValidId();
	    state.NPAgetStack().push(state.NPAgetStack().NPApeek().copy(),
				     bCode.getAddress());
	    break;
	case ByteCode.DUP_X1:
	    ne1 = (NPAStackElement) state.getStack().pop();
	    ne2 = (NPAStackElement) state.getStack().pop();
	    e1 = ne1.getType();
	    e1.setValidId();
	    state.NPAgetStack().push(e1.copy(), bCode.getAddress());
	    state.NPAgetStack().push(ne2.getType(), ne2.getBCAddr());
	    state.NPAgetStack().push(ne1.getType(), ne1.getBCAddr());
	    break;
	case ByteCode.DUP_X2:
	    ne1 = (NPAStackElement)state.getStack().pop();
	    ne2 = (NPAStackElement)state.getStack().pop();
	    ne3 = (NPAStackElement)state.getStack().pop();
	    e1 = ne1.getType();
	    e1.setValidId();
	    state.NPAgetStack().push(e1.copy(), bCode.getAddress());
	    state.NPAgetStack().push(ne3.getType(), ne3.getBCAddr());
	    state.NPAgetStack().push(ne2.getType(), ne2.getBCAddr());
	    state.NPAgetStack().push(ne1.getType(), ne1.getBCAddr());
	    break;
	case ByteCode.DUP2:
	    ne1 = (NPAStackElement)state.getStack().pop();
	    ne2 = (NPAStackElement)state.getStack().pop();
	    e1 = ne1.getType();
	    e2 = ne2.getType();
	    e1.setValidId();
	    e2.setValidId();
	    state.NPAgetStack().push(ne2.getType(), ne2.getBCAddr());
	    state.NPAgetStack().push(ne1.getType(), ne1.getBCAddr());
	    state.NPAgetStack().push(e2.copy(), bCode.getAddress());
	    state.NPAgetStack().push(e1.copy(), bCode.getAddress());
	    break;
	case ByteCode.DUP2_X1:
	    ne1 = (NPAStackElement)state.getStack().pop();
	    ne2 = (NPAStackElement)state.getStack().pop();
	    ne3 = (NPAStackElement)state.getStack().pop();
	    e1 = ne1.getType();
	    e2 = ne2.getType();
	    e1.setValidId();
	    e2.setValidId();
	    state.NPAgetStack().push(e2.copy(), bCode.getAddress());
	    state.NPAgetStack().push(e1.copy(), bCode.getAddress());
	    state.NPAgetStack().push(ne3.getType(), ne3.getBCAddr());
	    state.NPAgetStack().push(ne2.getType(), ne2.getBCAddr());
	    state.NPAgetStack().push(ne1.getType(), ne1.getBCAddr());
	    break;
	case ByteCode.DUP2_X2:
	    ne1 = (NPAStackElement)state.getStack().pop();
	    ne2 = (NPAStackElement)state.getStack().pop();
	    ne3 = (NPAStackElement)state.getStack().pop();
	    ne4 = (NPAStackElement)state.getStack().pop();
	    e1 = ne1.getType();
	    e2 = ne2.getType();
	    e1.setValidId();
	    e2.setValidId();
	    state.NPAgetStack().push(e2.copy(), bCode.getAddress());
	    state.NPAgetStack().push(e1.copy(), bCode.getAddress());
	    state.NPAgetStack().push(ne4.getType(), ne4.getBCAddr());
	    state.NPAgetStack().push(ne3.getType(), ne3.getBCAddr());
	    state.NPAgetStack().push(ne2.getType(), ne2.getBCAddr());
	    state.NPAgetStack().push(ne1.getType(), ne1.getBCAddr());
	    break;
	case ByteCode.SWAP:
	    //just a move, not really a copy!
	    JVMOPStackElement je1, je2;
	    je1 = state.getStack().pop();
	    je2 = state.getStack().pop();

	    state.getStack().push(je1);
	    state.getStack().push(je2);
	    break;
	}

    }

    //find all values on stack and in local variables with same id as 'value' and change
    //their values to newVal. 'value' itself is not changed!
    static public void setValue(NPAState state, NPAValue value, int newVal) {
	if (value.getId() == NPAValue.INVALID_ID) 
	    return;
	//stack
	state.NPAgetStack().setValue(value, newVal);
	//localVariables
	state.NPAgetLVars().setValue(value, newVal);
	
	    
    }

    //simulates a null-Pointer run-time check
    static private void runtimeCheck(NPAState state, ByteCode bCode, NPAValue value) {
	setValue(state, value, NPAValue.NONNULL);
    }
}
