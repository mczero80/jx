package test.nfs;

import jx.zero.*;
import jx.zero.debug.*;

import jx.net.NetInit;

import jx.rpc.RPC;
import jx.rpcsvc.bind2.*;
import jx.rpcsvc.mount1.*;
import jx.rpcsvc.nfs2.*;

import jx.fs.FS;


public class NFSDomain {
    static final int RPC_LOCALPORT = 1000;
    static final int EXT2FS_BLOCKSIZE = 1024;

    public static void init(Naming naming, String[] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("Domain NFSDomain speaking.");
	new NFSDomain(naming, args);
    }
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	new NFSDomain(naming, args);
    }

    NFSDomain(final Naming naming, String args[]) {
	String netName = args[0];
	String fsName = args[1];

	final MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	try {
	    NetInit netinstance = (NetInit) LookupHelper.waitUntilPortalAvailable(naming, netName);

	    final RPC rpc = new RPC(netinstance, RPC_LOCALPORT); 
	    if (rpc==null) throw new Error("RPCNULL!!!!"); 

	    FS fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, fsName);
	    if (fs == null)  throw new Error("no Filesystem found !!");

	    final NFSProc_Impl nfsImpl = new NFSProc_Impl(fs);
	    final MountProc_Impl mountImpl = new MountProc_Impl(fs);
		
	    // init RPC system
	    final RPCBind_Impl bind = new RPCBind_Impl(rpc);
	    final int mountPort = 0x00ba;
	    final int nfsPort = 2049;
	    bind.registerService( MountProc.PROGRAM, MountProc.VERSION, RPC.IPPROTO_UDP,  mountPort);
	    bind.registerService( NFSProc.PROGRAM, NFSProc.VERSION, RPC.IPPROTO_UDP,  nfsPort);
	    new Thread("MountProc") {
		    public void run() {
			new MountProc_Skel(rpc, mountPort, mountImpl);
		    }
		}.start();
	    new Thread("NFSProc") {
		    public void run() {
			new NFSProc_Skel(rpc, nfsPort, nfsImpl);
		    }
		}.start();
	    new Thread("RPCBind") {
		    public void run() {
			new RPCBind_Skel(rpc, 111, bind);
		    }
		}.start();
	} catch(Exception e) {
	    throw new Error("Could not setup");
	}	
    }
}
