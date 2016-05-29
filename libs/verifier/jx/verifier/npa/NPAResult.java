package jx.verifier.npa;

import jx.classfile.VerifyResult;


public class NPAResult extends VerifyResult {
    private int count;
    private int initSize;
    private int[] addresses;
    
    public int getCount() {
	return count;
    }
    public NPAResult(int size) {
	super(NPA_RESULT);
	addresses = new int[size];
	if (size ==0) 
	    initSize = 4;
	else 
	    initSize = size;
	count = 0;
    }
    
    //returns true if runtime null Pointer checks can be eliminated for bytecode @ address bcAddress
    public boolean notNull(int bcAddress) {
	for (int i =0; i < count; i++){
	    if (addresses[i] == bcAddress)
		return true;
	}
	
	return false;
    }

    public void setNotNull(int bcAddress) {
	if (count >0 && bcAddress <= addresses[count-1])
	    throw new Error("Verifier: Internal Error\n");
	if (count == addresses.length) {
	    //copy addresses
	    int[] tmp = new int[addresses.length+initSize];
	    for (int i =0; i < addresses.length; i++){
		tmp[i] = addresses[i];
	    }
	    addresses = tmp;
	}
	addresses[count]=bcAddress;
	count++;
    }

    public int[] getNonNullAdresses() {
	int[] ret = new int[count];
	for (int i =0; i < count; i++) {
	    ret[i] = addresses[i];
	}
	return ret;
    }
    
    public String toString(){
	StringBuffer ret = new StringBuffer(count*4);
	for(int i =0; i< count; i++) {
	    ret.append(Integer.toHexString(addresses[i]) +", ");
	}
	return ret.toString();
    }
    
}
