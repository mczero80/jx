package jx.verifier.bytecode;


/** Sprungbefehle mit mehreren Sprungzielen (table und lookupswitch) */
public class BCMultiBranch extends ByteCode {
    protected int[] targetAddresses; //Eintrag 0: default
    protected int[] keys;

    
    public int[] getTargetAddresses() { return targetAddresses;}

    public BCMultiBranch(int opCode, ByteIterator code) {
	super(opCode, code);

	if (opCode == TABLESWITCH) { //tableswitch
	    byte tmpArgs[] = new byte[12];
	    // read default
	    do {
		tmpArgs[0] = code.getNext();
 	    } while (((code.getIndex()) % 4)!=0); //remove padding zeroes
	    tmpArgs[1] = code.getNext();//defaultbyte2
	    tmpArgs[2] = code.getNext();//defaultbyte3
	    tmpArgs[3] = code.getNext();//defaultbyte4
	    int def = ((tmpArgs[0]<<24)&0xff000000)|
		((tmpArgs[1]<<16)&0xff0000)|
		((tmpArgs[2]<<8)&0xff00)|
		(((int)tmpArgs[3])&0xff);
	    tmpArgs[4] = code.getNext();
	    tmpArgs[5] = code.getNext();
	    tmpArgs[6] = code.getNext();
	    tmpArgs[7] = code.getNext();
	    int low = ((tmpArgs[4]<<24)&0xff000000)|
		((tmpArgs[5]<<16)&0xff0000)|
		((tmpArgs[6]<<8)&0xff00)|
		(((int)tmpArgs[7])&0xff);
	    tmpArgs[8] = code.getNext();
	    tmpArgs[9] = code.getNext();
	    tmpArgs[10] = code.getNext();
	    tmpArgs[11] = code.getNext();
	    int high = ((tmpArgs[8]<<24)&0xff000000)|
		((tmpArgs[9]<<16)&0xff0000)|
		((tmpArgs[10]<<8)&0xff00)|
		(((int)tmpArgs[11])&0xff);
	    
	    int numBranches = high - low + 1;
	    byteArgs = new byte[numBranches * 4+tmpArgs.length];

	    for (int i = 0; i < tmpArgs.length; i++) {
		byteArgs[i] = tmpArgs[i];
	    }
	    for (int i = tmpArgs.length; i < byteArgs.length; i++) {
		byteArgs[i] = code.getNext();
	    }
	    
	    targetAddresses = new int[numBranches + 1];

	    targetAddresses[0] = def + address;
	    for (int i = tmpArgs.length, j=1; i < byteArgs.length; i += 4, j++) {
		targetAddresses[j] = (byteArgs[i]<<24)|
		((byteArgs[i+1]<<16)&0xff0000)|
		((byteArgs[i+2]<<8)&0xff00)|
		(byteArgs[i+3]&0xff);
		targetAddresses[j] += address; // target is given as an OFFSET!
	    }
	    
	}
	else if (opCode == LOOKUPSWITCH) { 

	    // read default
	    byte tmpArgs[] = new byte[8];
	    do {
		tmpArgs[0] = code.getNext(); 
	    } while ((code.getIndex() % 4)!=0); //remove padding zeroes
	    tmpArgs[1] = code.getNext();
	    tmpArgs[2] = code.getNext();
	    tmpArgs[3] = code.getNext();
	    int def = (tmpArgs[0]<<24)|
		((tmpArgs[1]<<16)&0xff0000)|
		((tmpArgs[2]<<8)&0xff00)|
		(tmpArgs[3]&0xff);
	    
	    tmpArgs[4] = code.getNext();
	    tmpArgs[5] = code.getNext();
	    tmpArgs[6] = code.getNext();
	    tmpArgs[7] = code.getNext();
	    int nPairs = (tmpArgs[4]<<24)|
		((tmpArgs[5]<<16)&0xff0000)|
		((tmpArgs[6]<<8)&0xff00)|
		(tmpArgs[7]&0xff);
	    
	    byteArgs = new byte[nPairs*8+tmpArgs.length];

	    for (int i = 0; i < tmpArgs.length; i++) {
		byteArgs[i] = tmpArgs[i];
	    }
	    for (int i = tmpArgs.length; i < byteArgs.length; i++) {
		byteArgs[i] = code.getNext();
	    }
	    
	    targetAddresses = new int[nPairs + 1];
	    keys = new int[nPairs+1];

	    targetAddresses[0] = def + address;
	    keys[0] = -1;

	    for (int i = tmpArgs.length, j=1; i < byteArgs.length; i += 8, j++) {
		keys[j] = (byteArgs[i]<<24)|
		((byteArgs[i+1]<<16)&0xff0000)|
		((byteArgs[i+2]<<8)&0xff00)|
		(byteArgs[i+3]&0xff);
		targetAddresses[j] = (byteArgs[i+4]<<24)|
		((byteArgs[i+5]<<16)&0xff0000)|
		((byteArgs[i+6]<<8)&0xff00)|
		(byteArgs[i+7]&0xff);
		targetAddresses[j] += address; //target was given as an OFFSET!
	    }
	    
	}

    }

    public void linkTargets(BCLinkList bcl, ByteCode next){ 
	targets = new ByteCode[targetAddresses.length];
	for (int i = 0; i < targets.length; i++) { 
	    targets[i]  = bcl.getBCAt(targetAddresses[i]);
	    if (targets[i] == null) {
		throw new Error("BCMultiBranch.linkTarget: Invalid Target Address (t" +i+")" + this);
	    }
	    targets[i].isTarget = true;
	}
	for (int i =0; i < targets.length; i++) {
	    targets[i].addSource(this);
	}

    }


    public void recomputeTargetAddresses() {
	for (int i = 0; i < targets.length; i++) { 
	    targetAddresses[i] = targets[i].getAddress();
	}

	//recompute byteArgs
	//FEHLER
	//FEHLER
	//FEHLER
	//FEHLER die byteArgs muessen noch angepasst werden!

    }

    public String toString() {
	String ret = Integer.toHexString(address) + ": " + getBCName(opCode);
	if (opCode == TABLESWITCH) {
	    for (int i = 0; i < targetAddresses.length; i++) {
		ret += "\n\t"+Integer.toHexString(targetAddresses[i]);
	    }
	}
	else {
	    for (int i = 0; i < targetAddresses.length; i++) {
		ret += "\n\t"+Integer.toHexString(keys[i]) + ":" +
		    Integer.toHexString(targetAddresses[i]);
	    }
	}
	return ret;
    }
}
