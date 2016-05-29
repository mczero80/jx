package jx.net.protocol.ip;

import jx.zero.Debug;
import jx.zero.*;
import jx.zero.debug.Dump;
import jx.buffer.separator.*;
import jx.net.dispatch.Dispatch;
import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.PacketsConsumer1;
import jx.net.AddressResolution;
import jx.net.IPAddress;

import jx.net.IPProducer;
import jx.net.IPProducer1;
import jx.net.IPConsumer;
import jx.net.IPConsumer1;
import jx.net.EtherConsumer1;
import jx.net.EtherProducer1;
import jx.net.IPData;
import jx.net.EtherData;

/**
 * Implementation of the IP protocol.
 * Dispatching and packet reassembly.
 * @author Michael Golm
 */
public class IP implements MemoryConsumer, IPProducer, IPProducer1, EtherConsumer1 {
    static final boolean dumpAll = false; // switch on to see all ip packets
    static final boolean debugFrag = false; // debug fragmentation and reassembly
    private final static boolean debugPacketNotice = false;
    static final boolean printRegistration = false;

    public static final int IPVERSION = 4;

    private int ipid = 23; // unique ID??

    private IPAddress ownAddress;

    private boolean insertChecksum = true; // false == hardware inserts checksum

    private PacketsConsumer lowerConsumer;
    private PacketsProducer lowerProducer;
    private AddressResolution addressResolution;
    
    final int space=IPFormat.requiresSpace();

    IPConsumer myUDPConsumer;
    IPConsumer myTCPConsumer;
    IPConsumer1 myUDPConsumer1;
    IPConsumer1 myTCPConsumer1;

    IPConsumer ipConsumer;


    public static final int PROTO_UDP = 17;
    public static final int PROTO_TCP = 6;

    private Dispatch dispatch;
    {
	dispatch = new Dispatch(15);
	dispatch.add(PROTO_UDP, "UDP");
	dispatch.add(PROTO_TCP, "TCP");
	dispatch.add(1, "ICMP");
	dispatch.add(0, "IP");
	dispatch.add(2, "IGMP");
	dispatch.add(3, "GGP");
	dispatch.add(4, "IPIP");
	dispatch.add(8, "EGP");
	dispatch.add(12, "PUP");
	dispatch.add(22, "IDP");
	dispatch.add(29, "TP");
	dispatch.add(46, "RSVP");
	dispatch.add(80, "EON");
	dispatch.add(98, "ENCAP");
	dispatch.add(255, "RAW");
    }

    // reassembly
    class Fragment {
	Memory data;
	int offset;
	int size;
	Fragment next;
    }
    class ReAssembly {
	Fragment firstFragment;
	int numberFragments;
	int fragID;
	int expectedSize=-1;
	ReAssembly next;
	ReAssembly prev;
	ReAssembly() {
	}
	void addFragment(int offs, int size, Memory buf) {
	    Fragment f;
	    if (firstFragment == null) {
		firstFragment = new Fragment();
		f = firstFragment;
	    } else {
		if (firstFragment.offset > offs) {
		    // add as first
		    f = new Fragment();
		    f.next = firstFragment;
		    //firstFragment.prev = f;
		    firstFragment = f;
		} else {
		    Fragment prev = firstFragment;
		    Fragment cur = prev.next;
		    f = null;
		    while(cur != null) {
			if (cur.offset > offs) {
			    // insert fragment
			    f = new Fragment();
			    //f.prev = prev;
			    f.next = cur;
			    prev.next = f;
			    //cur.prev = f;
			    break;
			}
			prev = cur;
			cur = cur.next;
		    }
		    if (f == null) {
		      // throw new Error("no insert position found");
		      // append to list
		      f = new Fragment();
		      prev.next = f;
		    }
		}
	    }
	    f.offset = offs;
	    f.size = size;
	    f.data = buf.revoke();
	}
    }
    Memory[] memPool;    
    Memory[] largePool;    
    ReAssembly reass;
    MemoryManager memMgr;
    CPUManager cpuManager;

    private int event_rcv;
    private int event_snd;

    public IP(PacketsConsumer lowerConsumer, PacketsProducer lowerProducer, AddressResolution a) { 
	init();
	this.lowerProducer = lowerProducer;
	this.lowerConsumer = lowerConsumer; 

	lowerProducer.registerConsumer(this, "IP");

	addressResolution = a;	
	if (addressResolution != null) addressResolution.register(this);
    }
    public IP(EtherProducer1 lowerProducer) { 
	init();
	lowerProducer.registerConsumer1(this, "IP");
    }
    private void init() {
	memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	event_rcv = cpuManager.createNewEvent("IPRcv");
	event_snd = cpuManager.createNewEvent("IPSnd");
	/*
	memPool = new Memory[30];
	for(int i=0; i<memPool.length; i++) {
	    memPool[i] = memMgr.allocAligned(1700,4).getSubRange(100,1600);
	}
	largePool = new Memory[100];
	for(int i=0; i<largePool.length; i++) {
	    largePool[i] = memMgr.allocAligned(10000,4);
	}
	*/
	// cleanup unused memory
	//	DomainManager domainManager = (DomainManager)InitialNaming.getInitialNaming().lookup("DomainManager");	
	//domainManager.gc(domainManager.getCurrentDomain());

    }

    public void setAddressResolution(AddressResolution a) {
	addressResolution = a;
	if (addressResolution != null) addressResolution.register(this);
    }

    public boolean registerConsumer1(IPConsumer1 consumer, String name) {
	if (name.equals("UDP")) {
	    if (myUDPConsumer1 != null) throw new Error("UDP consumer already registered");
	    myUDPConsumer1 = consumer;
	    Debug.out.println("IP: Registered UDP consumer");
	    return true;
	}
	if (name.equals("TCP")) {
	    if (myTCPConsumer1 != null) throw new Error("TCP consumer already registered1");
	    myTCPConsumer1 = consumer;
	    return true;	    
	}
	throw new Error("Unknown consumer type");
    }


    public boolean registerConsumer(IPConsumer consumer, String name) {
	//	return dispatch.registerConsumer(consumer, name);
	if (name.equals("UDP")) {
	    if (myUDPConsumer != null) throw new Error("UDP consumer already registered");
	    myUDPConsumer = consumer;
	    return true;
	}
	if (name.equals("TCP")) {
	    if (myTCPConsumer != null) throw new Error("TCP consumer already registered");
	    myTCPConsumer = consumer;
	    return true;	    
	}
	throw new Error("Unknown consumer type");
    }

    public boolean registerIPConsumer(IPConsumer consumer) {
	if (printRegistration) {
	  Debug.out.println("Register IP receiver");
	}
	if (ipConsumer != null) throw new Error("ip consumer already registered");
	ipConsumer = consumer;
	return true;
    }

    public boolean unregisterIPConsumer(IPConsumer consumer) {
	if (ipConsumer == null) throw new Error("no consumer registered");
	if (ipConsumer != consumer) throw new Error("this consumer is not registered");
	ipConsumer = null;
	return true;
    }


    public void changeSourceAddress(IPAddress address) {
	ownAddress = address;
	if (addressResolution != null) addressResolution.notifyAddressChange(this);
    }

    /** the TCP-Layer needs to know the own IP-Address to compute the Pseudoheader-chechsum
     */
    public IPAddress getOwnAddress() {
	return ownAddress;
    }

    public int getMTU() {
	return lowerConsumer.getMTU();
    }


    private void insertInMemPool(Memory m) {
	for (int i=0; i<memPool.length; i++) {
	    if (memPool[i] == null) {
		memPool[i] = m;
		return;
	    }
	}
    }
    private Memory getFromMemPool() {
	for (int i=0; i<memPool.length; i++) {
	    if (memPool[i] != null) {
		Memory retMem = memPool[i];
		memPool[i] = null;
		return retMem;
	    }
	}
	throw new Error("NO MEMORY IN REASSEMBLY");
    }

    Memory getLargeMemory(int size) {
	for (int i=0; i<largePool.length; i++) {
	    if (largePool[i] != null) {
		Memory retMem = largePool[i];
		largePool[i] = null;
		return retMem.getSubRange(0, size);
	    }
	}
	throw new Error("NO MEMORY IN REASSEMBLY");
	//	Memory xbuf = memMgr.allocAligned(fsize+space, 4);
    }


    public Memory processMemory(Memory buf) {
	throw new Error();
    }

    public Memory processEther1(EtherData buf) {
	if (debugPacketNotice) Debug.out.println("ARP.processEther: "+buf.size);
	cpuManager.recordEvent(event_rcv);
	//buf = buf.revoke();
	IPFormat ip = new IPFormat(buf.mem, buf.offset);

	if (dumpAll) {
	    Debug.out.println("IP.receive: size="+buf.size);
	    ip.dump();
	}

	int id = ip.getProtocol();
	int flags = ip.getFlags();
	int foffs = ip.getFragmentOffset() * 8;
	int fid = ip.getIdentification();
	IPAddress sourceAddress = new IPAddress(ip.getSourceIPAddress());
        IPAddress destAddress = new IPAddress(ip.getDestIPAddress());
	
	if (debugFrag) Debug.out.println("IP-Identification: "+ip.getIdentification());
	if (debugFrag) Debug.out.println("IP-Fragmentoffset: "+foffs);
	if (debugFrag) Debug.out.println("IP-TTL: "+ip.getTimeToLive());
	if (debugFrag) Debug.out.println("IP-Flags: "+flags);
	
	/*
	ReAssembly curFrag = null;
	if ((flags & IPFormat.FLAGS_MORE_FRAGMENTS) != 0) {
	    if (debugFrag) Debug.out.println("*IP-Fragment packet received");
	    curFrag = addFragment(fid, foffs, buf.size()-space, buf);
	} else {
	    if (foffs != 0) {
		if (debugFrag) Debug.out.println("LAST IP-Fragment "+fid+" packet received ");
		int payload = buf.size()-space;
		curFrag = addFragment(fid, foffs, payload, buf);
		curFrag.expectedSize = foffs + payload;
	    }
	}
	
	if (curFrag != null) {
	    // received fragment
	    if (checkComplete(curFrag)) {
		if (debugFrag) Debug.out.println("*IP-Fragment "+fid+" complete.");
		Memory data = assemble(curFrag);
		//Dump.xdump1(data, 0, 128);
                // FIXME! Vorsicht, hier wird ganzer Puffer statt tatsaechlicher Paketlaenge uebergeben
                // Checksum-Berechnungen auf oberen Schichten funktionieren dann bei fragmentierten IP-Paketen nicht
		return dispatch(id, data, sourceAddress, destAddress);
	    }
	    return getFromMemPool();
	}
	*/

	if (debugFrag) Debug.out.println("*IP-FullPacket received");
	return dispatch(id, buf, sourceAddress, destAddress);
    }

    Memory dispatch(int id, EtherData data, IPAddress addr, IPAddress dAddr) {
	IPData ip = new IPData();
	ip.sourceAddress = addr;
	ip.destinationAddress = dAddr;
	if (id == PROTO_UDP) {
	    if (myUDPConsumer1!= null) {
		ip.mem = data.mem;
		ip.offset = data.offset + IPFormat.requiresSpace();
		ip.size = data.size-IPFormat.requiresSpace();
		return myUDPConsumer1.processIP1(ip);
	    } else if (myUDPConsumer!= null) {
		ip.mem = data.mem.getSubRange(space, data.mem.size()-space);
		return myUDPConsumer.processIP(ip);
	    }
	    Debug.out.println("  No UDP consumer for this IP packet.");
	    return data.mem;
	} else if (id == PROTO_TCP) {
	    if (myTCPConsumer1!= null) {
		ip.mem = data.mem;
		ip.offset = data.offset + IPFormat.requiresSpace();
/*		ip.size = data.size-IPFormat.requiresSpace();
		IPFormat ipf = new IPFormat(data.mem, data.offset);
		Debug.out.println("ipf:"+(ipf.getTotalLength()-ipf.getHeaderLength()) +" sz:"+	ip.size);
*/
		IPFormat ipf = new IPFormat(data.mem, data.offset);
		ip.size = (ipf.getTotalLength()-ipf.getHeaderLength());
		return myTCPConsumer1.processIP1(ip);
	    } else if (myTCPConsumer!= null) {
		IPFormat ipf = new IPFormat(data.mem, data.offset);
		ip.mem = data.mem.getSubRange(data.offset + IPFormat.requiresSpace(), 
					      (ipf.getTotalLength()-ipf.getHeaderLength()));
		return myTCPConsumer.processIP(ip);
	    }
	    Debug.out.println("  No TCP consumer for this IP packet.");
	    return data.mem;
	} else {
	    Debug.out.println("Unsupported protocol in IP packet: "+dispatch.findName(id));
	    return data.mem;
	}
    }

    public PacketsConsumer1 getTransmitter1(final PacketsConsumer1 lowerConsumer, final IPAddress dst, final int id) {
	return new PacketsConsumer1() {
		public Memory processMemory(Memory userbuf, int offset, int size) {
		    if (debugPacketNotice) Debug.out.println("IP.transmit: "+size);
		    cpuManager.recordEvent(event_snd);
		    Memory buf = userbuf;
		    offset -= IPFormat.requiresSpace();
		    IPFormat ip = new IPFormat(buf, offset);
		    ip.insertHeaderLength();
		    ip.insertTypeOfService(0);
		    ip.insertTotalLength(size+IPFormat.requiresSpace());
		    ip.insertIdentification(ipid++); 
		    ip.insertFragmentOffset(0);
		    ip.insertTimeToLive(64);
		    ip.insertProtocol(id);
		    if (ownAddress != null) ip.insertSourceAddress(ownAddress);
		    ip.insertDestAddress(dst);
		    if (insertChecksum) ip.insertChecksum();
		    //if (insertChecksum) ip.insertChecksum(cpuManager.ipchecksum(buf, offset+0, 20));

		    if (dumpAll) {
			Debug.out.println("IP send");
			ip.dump();
		    }
		    Memory ret = lowerConsumer.processMemory(buf, offset, IPFormat.requiresSpace() + size);		    
		    return ret;
		}
		public int requiresSpace() {return IPFormat.requiresSpace();}
		public int getMTU() {return 1000; /*TODO*/}
	    };
    }

    public PacketsConsumer getTransmitter(final PacketsConsumer lowerConsumer, final IPAddress dst, String name) {
	final int id = dispatch.findID(name);
	return getTransmitter(lowerConsumer, dst, id);
    }

    public PacketsConsumer getTransmitter(final PacketsConsumer lowerConsumer, final IPAddress dst, final int id) {
	return new PacketsConsumer() {
		public Memory processMemory(Memory userbuf) {
		    Debug.out.println("SEND2 IP to "+dst);
		    if (debugPacketNotice) Debug.out.println("IP.transmit: "+userbuf.size());
		    cpuManager.recordEvent(event_snd);
		    Memory buf = userbuf.joinPrevious();
		    IPFormat ip = new IPFormat(buf);
		    ip.insertHeaderLength();
		    ip.insertTypeOfService(0);
		    ip.insertTotalLength(buf.size());
		    ip.insertIdentification(ipid++); 
		    ip.insertFragmentOffset(0);
		    ip.insertTimeToLive(64);
		    ip.insertProtocol(id);
		    if (ownAddress != null) ip.insertSourceAddress(ownAddress);
		    ip.insertDestAddress(dst);
		    if (insertChecksum) ip.insertChecksum();
		    //if (insertChecksum) ip.insertChecksum(cpuManager.ipchecksum(buf, 0, 20));

		    Debug.out.println("IP send");
		    ip.dump();
		    Memory ret = lowerConsumer.processMemory(buf);		    
		    int space = ip.length();
		    return ret.getSubRange(space, ret.size()-space);
		}
		public int requiresSpace() {return IPFormat.requiresSpace();}
		public int getMTU() {return 1000; /*TODO*/}
	    };
    }

    
    public MemoryConsumer getReceiver() {
	return new MemoryConsumer() {
		public Memory processMemory(Memory buf) {
		    if (dumpAll)Debug.out.println("processMemory");
		    return lowerConsumer.processMemory(buf);		    
		}
	    };
    }
    

    private ReAssembly addFragment(int fid, int offs, int size, Memory buf) {
	// find reass
	ReAssembly r = reass;
	ReAssembly prev = null;
	while(r != null) {
	    if (r.fragID == fid) {
		r.addFragment(offs, size, buf);
		return r;
	    }
	    prev = r;
	    r = r.next;
	}
	
	// new series of fragments
	r = new ReAssembly();
	if (prev==null) {
	    reass = r;
	}
	else {
	    prev.next = r;
	    r.prev = prev;
	}
	r.fragID = fid;
	r.addFragment(offs, size, buf);
	return r;
    }

    private boolean checkComplete(ReAssembly r) {
	// check for holes in assembly line
	int offs = 0;
	
	for(Fragment f = r.firstFragment; f != null; f=f.next) {
	    if (f.offset != offs) {
		Debug.out.println(r.fragID+" not complete. missing "+offs+" ... "+f.offset);
		return false;
	    }
	    offs += f.size;
	}

	if (offs != r.expectedSize) {
	    Debug.out.println(r.fragID+" not complete. got size "+offs+" but expected "+r.expectedSize);
	    return false;
	}
	return true;
    }

    /**
     * @returns payload subrange memory
     */
    private Memory assemble(ReAssembly r) {
	// remove reassembled packet from assembly line
	if (r.prev==null) reass = r.next; 
	else r.prev.next = r.next;

	int fsize=r.expectedSize;
	Memory xbuf = getLargeMemory(fsize+space);
	
	int offs=space;
	Fragment f = r.firstFragment;
	xbuf.copyFromMemory(f.data, 0, 0, f.size+space); // copy IP header
	for(f = f.next; f != null; f=f.next) {
	    xbuf.copyFromMemory(f.data, space, f.offset+space, f.size);
	    //Debug.out.println(r.fragID+" copy "+f.offset);
	    insertInMemPool(f.data);
	    f.data = null;
	}
	return xbuf.getSubRange(space, xbuf.size()-space);
    }

}
