package jx.verifier.bytecode;

import jx.verifier.VerifyException;

/** Alle Sprungbefehle mit einem (!) Sprungziel */
public class BCBranch extends ByteCode {
    protected int targetAddress;
    
    /**holds the start of the subroutine if this is a jsr or jsr_w*/
    private ByteCode jsrTarget = null;
    
    public int getTargetAddress() { return targetAddress;}

    //next is must always be target[0]
    public void linkTargets(BCLinkList bcl, ByteCode next) throws VerifyException { 
	switch(opCode) {
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
	    // all opCodes with "two" targets: next and conditional jump
	    if (next == null) {
		throw new VerifyException("Last Bytecode is not return or unconditional jump!");
	    }
	    
	    targets = new ByteCode[2];
	    targets[0] = next;
	    targets[1] = bcl.getBCAt(targetAddress);
	    if (targets[1] == null) {
		throw new VerifyException("BCBranch.linkTarget: Invalid Target Address (0x"+
					  Integer.toHexString(targetAddress)+")\n" +
					  "Bytecode: " + this);
	    }
	    targets[1].isTarget=true;
	    break; //Ifs
	case GOTO:
	case GOTO_W:
	    targets = new ByteCode[1];
	    targets[0] = bcl.getBCAt(targetAddress);
	    if (targets[0] == null) {
		throw new VerifyException("BCBranch.linkTarget: Invalid Target Address (0x"+
					  Integer.toHexString(targetAddress)+")\n" +
					  "Bytecode: " + this);
	    }
	    targets[0].isTarget=true;
	    break;
	case JSR:
	case JSR_W:
	    //Subroutines must be handled separately. 
	    //After Subroutine, the next bc is executed-->only 'next' is target
	    if (next == null) {
		throw new VerifyException("Last Bytecode is not return or unconditional jump!");
	    }
	    targets = new ByteCode[1];
	    targets[0] = next;
	    targets[0].isTarget = true; 
	    /** the subroutine is handled separately so it is not an ordinary target. */
	    jsrTarget =  bcl.getBCAt(targetAddress);
	    break;
	case RET:
	    // RET Targets are not yet known!
	    targets = new ByteCode[0];
	    break;
	default:
	    throw new Error("Internal Error: BCBranch.linkTargets called with invalid opcode");
	    
	}
	for (int i =0; i < targets.length; i++) {
	    targets[i].addSource(this);
	}

    }
    
    public void recomputeTargetAddresses() {
	switch(opCode) {
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
	    targetAddress = targets[1].getAddress();
	    break; //Ifs
	case GOTO:
	case GOTO_W:
	    targetAddress = targets[0].getAddress();
	    break;
	case JSR:
	case JSR_W:
	    targetAddress = jsrTarget.getAddress();
	    break;
	case RET:
	    break;
	default:
	    throw new Error("Internal Error: BCBranch.linkTargets called with invalid opcode");
	}

	//recompute the byteArgs.
	if (opCode == RET) {
	    //nothing to do
	} else if (opCode != JSR_W && opCode != GOTO_W) {
	    //normal instructions
	    int offset = targetAddress - address;
	    byteArgs[0] = (byte)((offset & 0xff00) >> 8);
	    byteArgs[1] = (byte)(offset & 0xff);
	} else {
	    //wide instructions
	    int offset = targetAddress - address;
	    byteArgs[0] = (byte)((offset & 0xff000000) >> 24);
	    byteArgs[1] = (byte)((offset & 0xff0000) >> 16);
	    byteArgs[2] = (byte)((offset & 0xff00) >> 8);
	    byteArgs[3] = (byte)(offset & 0xff);
	}
    }
	
    public BCBranch(int opCode, ByteIterator code) {
	super(opCode, code);

	if (opCode == RET) {
	    targetAddress = -1;
	} else if (opCode != JSR_W && opCode != GOTO_W) {
	    targetAddress = (byteArgs[0]<<8 |
			     (byteArgs[1]&0xff)) + address; // Args are offset
	} else {
	    targetAddress = ((byteArgs[0]<<24) |
			     ((byteArgs[1]<<16) & 0xff0000) |
			     ((byteArgs[2]<<8) &0xff00) |
			     (byteArgs[3]&0xff)) + address;
	}

    }
    public String toString() {
	return Integer.toHexString(address) + ": " + getBCName(opCode) + " " + Integer.toHexString(targetAddress);
    }
}
