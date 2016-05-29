package test.memobj;

import jx.zero.*;
import jx.zero.debug.Dump;
import jx.zero.timing.Control;

class MyMap implements MappedLittleEndianObject {
    int a;
    char b;
    short c;
    int d;
}

public class Main {

    private final static boolean doTiming=false;

    static void testMemory(MemoryManager memMgr, Memory mem, int size) {
       
	Profiler p = null;
	if (doTiming) {	    
	    p=(Profiler)InitialNaming.getInitialNaming().lookup("Profiler");
	    jx.zero.timing.Control.startTiming();
	}

	if (size != mem.size()) {
	    throw new Error("WRONG SIZE");
	} else
	    Debug.out.println("SIZE OK");
	
	test8Bit(memMgr,mem,size);
	test8BitConst(memMgr,mem,size);
	Debug.out.println("set8/get8 OK");
	Dump.xdump(mem, 0, 64);

	test16Bit(memMgr,mem,size);
	Debug.out.println("set16/get16 OK");
	Dump.xdump(mem, 0, 64);

	test16BitLE(memMgr,mem,size);
	Debug.out.println("setLittleEndian16/getLittleEndian16 OK");
	Dump.xdump(mem, 0, 64);
	
	test32Bit(memMgr,mem,size);
	test32BitConst(memMgr,mem,size);
	Debug.out.println("set32/get32 OK");
	Dump.xdump(mem, 0, 64);

	// test copy to/from array
	byte[] b = new byte[13];
	mem.copyToByteArray(b, 3, 2, 10);
	for(int i=0; i<10; i++) {
	    if (mem.get8(2+i) != b[3+i]) {
		throw new Error("copyToByteArray: WRONG value");
	    } 	    
	}
	Debug.out.println("copyToByteArray OK");
	mem.copyFromByteArray(b, 1, 2, 3);
	Debug.out.println("copyFromByteArray OK");

	// test copy to memory
	Memory mem1 = memMgr.alloc(100);
	mem.copyToMemory(mem1, 1, 8, 70);
	Debug.out.println("copyToMemory OK");
	Dump.xdump(mem1, 0, 64);

	throw new Error("extend -> split");
	/*
	// test subrange creation
	Debug.out.println("before alloc");
	Memory mem2 = memMgr.alloc(1000);
	//Debug.out.println("after alloc");
	Memory mem3 = mem2.getSubRange(20, 300);
	Debug.out.println("after getSub");
	Debug.out.println("getSubRange OK");
	Memory mem4 = mem3.extendRange(10, 100);
	Debug.out.println("extendRange OK");
	Memory mem5 = mem3.extendFullRange();
	Debug.out.println("extendFullRange OK");
	*/
	//if (doTiming) p.shell();
    }

    static void test8Bit(MemoryManager memMgr, Memory mem, int size) {
	int value = 0x80;
	mem.set8(0, (byte)value);
	if (mem.get8(0) != (byte)value) throw new Error("sign extension does not work");

	for(int i=0; i<0x7f; i++) {
	    mem.set8(i, (byte)i);
	    int v = mem.get8(i);
	    if (v != i) {
		Dump.xdump(mem, 0, 64);
		throw new Error("get8: WRONG value "+i+", "+v);
	    } 
	}
    }

   static void test8BitConst(MemoryManager memMgr, Memory mem, int size) {
	for(int i=0; i<0x7f; i++) {
	    mem.set8(2, (byte)i);
	    int v = mem.get8(2);
	    if (v != i) {
		Dump.xdump(mem, 0, 64);
		throw new Error("get8: WRONG value "+i+", "+v);
	    } 
	}
    }

    static void test16Bit(MemoryManager memMgr, Memory mem, int size) {
	int value = 0x8000;
	mem.set16(0, (short)value);
	if (mem.get16(0) != (short)value) throw new Error("sign extension does not work");
	for(int i=0; i<0x7fff; i++) {
	    mem.set16(i, (short)i);
	    if (mem.get16(i) != i) {
		throw new Error("get16: WRONG value");
	    } 
	    int n = (((mem.get8(i*2+1) <<  8) & 0xff00) 
		     |  (mem.get8(i*2+0) & 0x00ff));
	    if (n != i) {
		Debug.out.println("ERROR at i="+i+", n="+n);
		Dump.xdump(mem, 0, 64);
		throw new Error("get16: WRONG value (byteorder)");
	    }
	}
    }

    static void test16BitLE(MemoryManager memMgr, Memory mem, int size) {
	for(int i=0; i<0x7fff; i++) {
	    mem.setLittleEndian16(i, (short)i);
	    if (mem.getLittleEndian16(i) != i) {
		throw new Error("getLittleEndian16: WRONG value");
	    } 
	    int n = (((mem.get8(i+1) <<  8) & 0xff00) 
		     |  (mem.get8(i+0) & 0x00ff));
	    if (n != i) {
		Debug.out.println("ERROR at i="+i+", n="+n);
		Dump.xdump(mem, 0, 64);
		throw new Error("getLittleEndian16: WRONG value (byteorder)");
	    }
	}
    }

    static void test32BitConst(MemoryManager memMgr, Memory mem, int size) {
	for(int i=0; i<0x7fff; i++) {
	    mem.set32(2, i);
	    if (mem.get32(2) != i) {
		throw new Error("get32: WRONG value");
	    } 	  
	    int n = ((mem.get8(11) << 24) & 0xff000000) 
		| ((mem.get8(10) << 16) & 0x00ff0000) 
		| ((mem.get8(9) <<  8) & 0x0000ff00) 
		|  (mem.get8(8) & 0xff);
	    if (n != i) {
		Debug.out.println("ERROR at i="+i+", n="+n);
		Dump.xdump(mem, 0, 64);
		throw new Error("get32: WRONG value (byteorder)");
	    }
	}
    }

    static void test32Bit(MemoryManager memMgr, Memory mem, int size) {
	for(int i=0; i<0x7fff; i++) {
	    mem.set32(i, i);
	    if (mem.get32(i) != i) {
		throw new Error("get32: WRONG value");
	    } 	  
	    int n = ((mem.get8(i*4+3) << 24) & 0xff000000) 
		| ((mem.get8(i*4+2) << 16) & 0x00ff0000) 
		| ((mem.get8(i*4+1) <<  8) & 0x0000ff00) 
		|  (mem.get8(i*4+0) & 0xff);
	    if (n != i) {
		Debug.out.println("ERROR at i="+i+", n="+n);
		Dump.xdump(mem, 0, 64);
		throw new Error("get32: WRONG value (byteorder)");
	    }
	}
    }

    static void testSet(Memory mem) {
	mem.set32(0xca, 0xfe);
    }


    static void testSpeed(MemoryManager memMgr) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Memory mem = memMgr.allocAligned(256*4, 4);
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(starttimec);
	for(int k=0; k<100; k++) {
	    for(int i=0; i<256; i++) {
		mem.set32(i, i);
	    }
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("set32 time for 100 1KB blocks="+(clock.toMicroSec(diff))+" microseconds");
    }

    static void testRevoke(Naming naming) {
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Memory mem = memoryManager.alloc(100);
	Memory arr[] = new Memory[3];
	mem.split3(10, 30, arr);
	Memory mem1 = arr[1];
	cpuManager.dump("AFTERSPLIT", mem1);
	Memory mem2 = mem1.revoke();
	cpuManager.dump("AFTERREVOKE", mem2);
	Debug.out.println("Access revoked:");
	mem1.get8(0);
	Debug.out.println("Access done -> Error");
    }

    static void testCreateSpeed(Naming naming, int ntries) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    Memory mem = memoryManager.alloc(100);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" Memory creates="+(clock.toMicroSec(diff))+" microseconds");
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    DeviceMemory mem = memoryManager.allocDeviceMemory(0, 100);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" DeviceMemory creates="+(clock.toMicroSec(diff))+" microseconds");
    }

    static void testRevokeSpeed(Naming naming, int ntries) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Memory mem = memoryManager.alloc(100);
	Memory arr[] = new Memory[3];
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    mem = mem.revoke();
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" revokes="+(clock.toMicroSec(diff))+" microseconds");
    }

    static void testSplitSpeed(Naming naming, int ntries) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Memory mem = memoryManager.alloc(ntries);

	Memory arr[] = new Memory[2];
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    mem.split2(1, arr);
	    mem = arr[1];
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" splits="+(clock.toMicroSec(diff))+" microseconds");
    }

    static void testSplit(Naming naming) {
	DomainManager domainManager = (DomainManager)InitialNaming.getInitialNaming().lookup("DomainManager");	
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	Memory mem = memoryManager.alloc(100);
	Memory arr[] = new Memory[2];
	mem.split2(10, arr);
	boolean ok;
	
	// access base
	ok=false;
	try {
	    mem.set32(0, 0);
	} catch(RuntimeException ex) {
	    ok = true;
	}
	if (! ok) Debug.out.println("ERROR: NO EXCEPTION WHILE ACCESSING BASE MEMORY OF SPLIT");

	// access splitter
	ok=true;
	try {
	    arr[0].set32(0, 0);
	    arr[1].set32(0, 0);
	} catch(RuntimeException ex) {
	    ok = false;
	}
	if (! ok) Debug.out.println("ERROR: EXCEPTION WHILE ACCESSING SPLITTER");

	Debug.out.println("SUCCESS: split test completed successfully");

	//    domainManager.gc(domainManager.getCurrentDomain());
    }

    static void testMap(Naming naming) {
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Memory mem = memoryManager.alloc(12);
	VMClass cl = cpuManager.getClass("test/memobj/MyMap");
	MyMap m = (MyMap) mem.map(cl);
	m.a = 42;
	m.b = 43;
	m.c = 44;
	m.d = 45;
	Dump.xdump(mem, 0, 32);
	Debug.out.println("a="+m.a);
	Debug.out.println("b="+m.b);
	Debug.out.println("c="+m.c);
	Debug.out.println("d="+m.d);
    }

    static void testMapSpeed(Naming naming, int ntries, int ntries2) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();
	
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Memory mem = memoryManager.alloc(120);
	VMClass cl = cpuManager.getClass("test/memobj/MyMap");
	MyMap m = (MyMap) mem.map(cl);

	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    m.a = 42;
	    m.b = 43;
	    m.c = 44;
	    m.d = 45;
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" *4 write accesses="+(clock.toMicroSec(diff))+" microseconds");

	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    int a = m.a;
	    a = m.b;
	    a = m.c;
	    a = m.d;
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" *4 read accesses="+(clock.toMicroSec(diff))+" microseconds");


	clock.getCycles(starttimec);
	for(int i=0; i<ntries2; i++) {
	    m = (MyMap) mem.map(cl);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries2+" mappings="+(clock.toMicroSec(diff))+" microseconds");

	// compare with set32
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    mem.setLittleEndian32(0, 42);
	    mem.setLittleEndian16(4, (short)43);
	    mem.setLittleEndian16(6, (short)44);
	    mem.setLittleEndian32(8, 45);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" *4 set() write accesses="+(clock.toMicroSec(diff))+" microseconds");


	// compare with get32
	clock.getCycles(starttimec);
	for(int i=0; i<ntries; i++) {
	    int a = mem.getLittleEndian32(0);
	    a = mem.getLittleEndian16(4);
	    a = mem.getLittleEndian16(6);
	    a = mem.getLittleEndian32(8);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	Debug.out.println("time for "+ntries+" *4 get() read accesses="+(clock.toMicroSec(diff))+" microseconds");


    }

    public static void main(String[] args) {
	Naming naming=InitialNaming.getInitialNaming();
	Debug.out.println("Memory test...");
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");

	if (args[0].equals("getset")) {
	    // testing size()
	    int size = /*70000*/ 0x7fff * 4;
	    Memory mem = memoryManager.alloc(size);
	    
	    Debug.out.println("Testing memory:");
	    testMemory(memoryManager, mem, size);
	    /*
	    // map a device memory to a heap allocated memory
	    // KEEP THE mem OBJECT LIVE!!
	    int start = mem.getStartAddress();
	    DeviceMemory dmem = memMgr.allocDeviceMemory(start, size);
	    Debug.out.println("Testing device memory:");
	    testMemory(memMgr, dmem, size);
	    */
	} else if (args[0].equals("map")) {
	    testMap(naming);
	    return;
	} else if (args[0].equals("mapSpeed")) {
	    testMapSpeed(naming, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	    return;
	} else if (args[0].equals("revoke")) {
	    testRevoke(naming);
	    return;
	} else if (args[0].equals("createSpeed")) {
	    testCreateSpeed(naming, Integer.parseInt(args[1]));
	    return;
	} else if (args[0].equals("revokeSpeed")) {
	    testRevokeSpeed(naming, Integer.parseInt(args[1]));
	    return;
	} else if (args[0].equals("splitSpeed")) {
	    testSplitSpeed(naming, Integer.parseInt(args[1]));
	    return;
	} else if (args[0].equals("speedAll")) {
	    testCreateSpeed(naming, Integer.parseInt(args[1]));
	    testRevokeSpeed(naming, Integer.parseInt(args[1]));
	    testSplitSpeed(naming, Integer.parseInt(args[1]));
	    testSpeed(memoryManager);
	    testMapSpeed(naming, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	    return;
	} else if (args[0].equals("speed")) {
	    testSpeed(memoryManager);
	} else if (args[0].equals("refs")) { 	// test refcounts
	    MemServerImpl memserver = new MemServerImpl(memoryManager);
	    naming.registerPortal(memserver, "MemServer");
	    DomainStarter.createDomain("MemClient", "test_memobj.jll", "test/memobj/ClientDomain", 2000000);

	} else if (args[0].equals("split")) {
	    testSplit(naming);
	} else if (args[0].equals("gc")) {
	    testGC(naming);
	} else {
	    Debug.out.println("************ NO MEMORY TEST SELECTED *************");
	}

    }

    static void testGC(Naming naming) {
	MemoryManager memoryManager = (MemoryManager) naming.lookup("MemoryManager");
	Memory m = memoryManager.alloc(321);
	m = null;
	if (true) for(;;) new Object();
    }


}

interface MemServer extends Portal {
    Memory getMemory();
    void gc();
}

class MemServerImpl implements MemServer, Service {
    MemoryManager memMgr;
    MemServerImpl(MemoryManager memMgr) {
	this.memMgr = memMgr;
    }
    public Memory getMemory() {
	return memMgr.alloc(42);
    }

    public void gc() {
	DomainManager domainManager = (DomainManager)InitialNaming.getInitialNaming().lookup("DomainManager");	
	domainManager.gc(domainManager.getCurrentDomain());
    }

}

class ClientDomain {
    static Memory m;
    public static void gcMem(MemServer svr) {
	Memory mem;
	for(int i=0; i<10; i++) {
	    mem = svr.getMemory();
	}
    }

    public static void init(Naming naming, String[] args) {
	Debug.out = new jx.zero.debug.DebugPrintStream(new jx.zero.debug.DebugOutputStream((jx.zero.debug.DebugChannel) naming.lookup("DebugChannel0")));
	DomainManager domainManager = (DomainManager)naming.lookup("DomainManager");	
	MemServer svr = (MemServer) LookupHelper.waitUntilPortalAvailable(naming, "MemServer");

	m = svr.getMemory();

	for(;;) {
	    gcMem(svr);
	    
	    domainManager.gc(domainManager.getCurrentDomain());
	    svr.gc();
	}

	//for(;;) Thread.yield();
    }
}
