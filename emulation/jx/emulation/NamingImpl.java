package jx.emulation;

import jx.zero.*;
import jx.zero.scheduler.HighLevelScheduler;
import java.util.*;

public class NamingImpl implements Naming {
    Clock clock;
    Hashtable portalNames = new Hashtable();
    Vector portals = new Vector();
    // BlockIOFile ide;
    public NamingImpl() {
	clock = new ClockImpl(); registerPortal(clock, "Clock");
	registerPortal(new ProfilerImpl(), "Profiler");
	registerPortal(new MemoryManagerImpl(), "MemoryManager");
    }
    
    public Portal lookup(String name) {
	/*
	if (name.equals("IDE") && ide == null) {
	    ide = new BlockIOFile("/tmp/jfs_file", 2*1024 * 20);
	    registerPortal(ide, "IDE");
	}
	*/
	return (Portal) portalNames.get(name);
	//return null;
    }

    // FIXME jgbauman: Quickfix does this work
    public Portal lookupOrWait(String depName) {
       return (Portal) portalNames.get(depName);
    }

    public void registerPortal(Portal dep, String name) {
	portalNames.put(name, dep);
    }
}
