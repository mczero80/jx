package jx.rpc;

import jx.zero.*;

public class Waiter {
    int xid;
    Memory buf;
    CPUState waiter;
    RPC rpc;
    Waiter(RPC rpc, int xid, Memory buf) { this.rpc = rpc; this.xid = xid; this.buf = buf; }
    
    void doWait() {
	waiter = rpc.cpuManager.getCPUState();
	rpc.cpuManager.block();
    }
    
    void unblock() {
	rpc.cpuManager.unblock(waiter);
    }

}
