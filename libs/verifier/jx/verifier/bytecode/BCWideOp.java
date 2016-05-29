package jx.verifier.bytecode;


/** Wide Befehl und nachfolgender Befehl */
public class BCWideOp extends ByteCode {
    protected int opCode2;
    public int getOpCode2() { return opCode2;}
    
    public BCWideOp(int opCode, ByteIterator code) {
	super(opCode, code);
	opCode2 = code.getNext()&0xff;
	if (opCode2 == IINC) {
	    byteArgs = new byte[4];
	}
	else {
	    byteArgs = new byte[2];
	}
	for (int i = 0; i < byteArgs.length; i++) {
	    byteArgs[i] = code.getNext();
	}
    }
    public String getBCName() {
	return getBCName(opCode2) + "_WIDE";
    }
}

