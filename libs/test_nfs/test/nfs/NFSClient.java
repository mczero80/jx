package test.nfs;

import jx.zero.*;
import jx.shell.*;


import jx.zero.debug.Dump;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.NetInit;

import jx.rpcsvc.bind2.*;
import jx.rpcsvc.mount1.*;
import jx.rpcsvc.nfs2.*;


import jx.net.IPAddress;

import jx.devices.pci.PCIAccess;

import metaxa.os.devices.net.ComInit;
import metaxa.os.devices.net.D3C905;

import jx.rpc.RPC;

import jx.rpc.*; // Auth etc.

public class NFSClient {
    static final int RPC_LOCALPORT = 1000;
    static final int EXT2FS_BLOCKSIZE = 1024;

    NFSClient() {
	final Naming naming = InitialNaming.getInitialNaming();
	final MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

	try {
	    NetInit netinstance = (NetInit) naming.lookup("NetManager");


	    final RPC rpc = new RPC(netinstance, RPC_LOCALPORT); 
	    if (rpc==null) Debug.out.println("RPCNULL!!!!");

	    IPAddress rpcHost = new IPAddress(192,168,34,2);
	    MountProc mount = new MountProc_Stub(rpc, rpcHost);
	    
	    String path = "/megadisk/jx/golm/jx";
	    FHStatus m = mount.mnt(new DirPath(path));
	    if (m.status != FHStatus.SWITCH_FHStatusOK) {
		throw new Error("Could not mount " + m.status);
	    }
	    FHandle fileHandle = ((FHStatusOK)m).directory;
	    
	    //Dump.xdump1(fileHandle.data, fileHandle.data.length);
	    
	    
	    NFSProc nfs = new NFSProc_Stub(rpc, rpcHost);
	    Auth a = new AuthUnix(netinstance.getLocalAddress().getHostName(), 10412, 10430, new int[] {10424});
	    Auth ct = new AuthNone();
	    ((NFSProc_Stub)nfs).setAuth(a,ct);
	    
	    // Test getattr
	    AttrStat at = nfs.getattr(fileHandle);
	    if (! (at instanceof AttrStatOK)) {
		throw new Error("getattr Error ");
	    }
	    FAttr attr = ((AttrStatOK)at).attributes;
	    
	    Debug.out.println("Mode: "+attr.mode);
	    Debug.out.println("NLink: "+attr.nlink);
	    Debug.out.println("UID: "+attr.uid);
	    Debug.out.println("GID: "+attr.gid);
	    Debug.out.println("Size: "+attr.size);
	} catch(Exception e) {
	    throw new Error("Could not setup");
	}	
    }
}
