package jx.rpcsvc.nfs2;

import jx.rpc.*;

public class FHandle implements RPCData  {
    public static final int FIXLEN_data = 32;
    public byte[] data; 

    /*
      DO NOT CHANGE THIS INTERFACE !!
      IT CONFORMS TO THE NFS RFC

	 public int identifier;
	 public int deviceIdentifier;
	 public int generation;
	 
    */
    public FHandle() {}
    public FHandle(byte[] data) {
	this.data = data;
    }
}
