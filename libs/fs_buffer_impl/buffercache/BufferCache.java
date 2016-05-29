package buffercache;

import jx.zero.Debug;
import jx.zero.*;
import jx.zero.debug.*;
import jx.bio.BlockIO;
import jx.collections.Iterator;


/**
 * BufferCache implementation using a hashtable and an LRU freelist.
 *
 * @author Michael Golm, Andreas Weissel
 */

public class BufferCache implements jx.fs.buffercache.BufferCache {
    private static final boolean statistics = false;
    private static final boolean trace = false;
    private static final boolean debugSync = false;
    
    private BufferHashtable buffer_hashtable;
    private BufferFreeList free_list;
    private int nr_buffers;

    private int initialNumberOfBlocks;
    private int maximalNumberOfBlocks;
    private int incrementNumberOfBlocks;
    
    //private Object buffer_wait;
    //private FlushDemon flushd;
    private BlockIO idedevice;
    
    public int hit, miss; // statistics
    
    private Clock clock;
    private CPUManager cpuManager;
    private int bufferSize;
    private Profiler profiler;

    private MemoryManager memMgr = ((MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager"));

    /**
     * @param blockDevice block device that contains the blocks 
     * @param clock needed as time base
     * @param initialNumberOfBlocks initial size of buffer cache (in number of blocks)
     * @param maximalNumberOfBlocks maximal size of buffer cache (in number of blocks)
     * @param incrementNumberOfBlocks number of blocks to grow cache (in number of blocks)
     * @param bufferSize size of one buffer (==block size???)
     */
    public BufferCache(BlockIO blockDevice, Clock clock, 
		       int initialNumberOfBlocks,
		       int maximalNumberOfBlocks,
		       int incrementNumberOfBlocks,
		       int bufferSize
		       ) {
	this.idedevice = blockDevice;
	this.clock = clock;
	this.bufferSize = bufferSize;

	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	profiler = (Profiler) InitialNaming.getInitialNaming().lookup("Profiler");
	
	int memory_size;
	int order;
	
	buffer_hashtable = new BufferHashtable(200000);
	free_list = new BufferFreeList();
    
	this.maximalNumberOfBlocks = maximalNumberOfBlocks;
	this.initialNumberOfBlocks = initialNumberOfBlocks;
	this.incrementNumberOfBlocks = incrementNumberOfBlocks;
	if (maximalNumberOfBlocks < initialNumberOfBlocks) throw new Error("maximalNumberOfBlocks < initialNumberOfBlocks");
	//buffer_wait = new SleepObject();
	//buffer_wait = new Object();
	
	//flushd = new FlushDemon();
	//flushd.start();
    }


    /**
     * Stop the thread that flushes buffers to disk.
     */
    public  void stopDemon() {
	//flushd.stop();
    }

    /**
     * Writes a buffer to the block device.
     *
     * @param bh          the buffer that should be written
     * @param synchronous wait until the block is written
     */
    final void writeBlock(jx.fs.buffercache.BufferHead bh, boolean synchronous) {
	if (trace) Debug.out.println("rwBlock(" + bh.getBlock()+")  size="+bh.getSize());
	// map logical block number to device sector number
	try {
	    idedevice.writeSectors(bh.getBlock() * (1024/512), 1024/512, bh.getData(), true);
	} catch(Error e) {
	    bh.endIo(true, true);
	    return;
	}
	bh.endIo(false, true);
    }

    /**
     * Reads a buffer from the block device.
     *
     * @param bh          the buffer that should be read
     * @param synchronous wait until the block is read
     */
    final void readBlock(jx.fs.buffercache.BufferHead bh, boolean synchronous) {
	if (trace) Debug.out.println("rwBlock(" + bh.getBlock()+")  size="+bh.getSize());
	try {
	    idedevice.readSectors(bh.getBlock() * (1024/512), (1024/512), bh.getData(), true);
	} catch(Error e) {
	    bh.endIo(true, true);
	    return;
	}
	bh.endIo(false, true);
    }

    private  void putFirstFree(BufferHead bh) {
	//if (trace) Debug.out.println("putFirstFree(" + bh.b_block+ ")");
	//if (free_list.contains(bh))
	//   Debug.out.println("FEHLER (putFirstFree): " + bh.b_block);
	free_list.prependElement(bh); // An den Anfang der free_list haengen
    }

    private  void putLastFree(BufferHead bh) {
	//if (trace) Debug.out.println("putLastFree(" + bh.b_block+ ")");
	//if (free_list.contains(bh)) Debug.out.println("FEHLER (putLastFree): " + bh.b_block);
	free_list.appendElement(bh); // Ans Ende der free_list haengen
    }


    final public jx.fs.buffercache.BufferHead findBuffer(int block) {
	BufferHead bh = buffer_hashtable.get(block); //new BufferHeadHashKey(block, size);
	if (bh != null) {
	    //Debug.out.println("buffer found: " + bh.b_block);
	    if (statistics) hit++;
	    bh.ref();
	    if (bh.dirty() == false)
		bh.b_flushtime = 0;

	    if (bh.isInFreeList()) {
		free_list.removeElement(bh);
		// buffer may be in hashtable AND in freelist
	    }
	} else {
	    if (trace) Debug.out.println("MISS: " + block);
	    if (statistics) miss++;
	}
	return bh;
    }

    final public jx.fs.buffercache.BufferHead getblk(int block) {
	BufferHead bh;
	jx.fs.buffercache.BufferHead bh0;
	int isize;

	if (trace) Debug.out.println("getblk(" + block + " )");

	if ((bh0 = findBuffer(block)) != null)
	    return bh0;
	
	if (free_list.size() == 0) {
	    if (trace) Debug.out.println("GROW buffer cache");
	    growBuffers();
	}
	bh = (BufferHead)free_list.undockFirstElement();
	//if (trace) Debug.out.println("UNDOCK  "+bh.id);
	if (bh.isDirty()) {
	    writeBlock(bh, true);
	}
	if (bh.hashtable_hashkey >= 0) {
	    if (trace) Debug.out.println("REMOVE from Hashtable: "+bh.getBlock()+":"+bh.id);
	    buffer_hashtable.remove(bh);
	}
	bh.init(block);
	buffer_hashtable.put(bh);

	if (trace) Debug.out.println("getblk out with " +bh.getBlock());

	return bh;
    }

    /*
     * Gibt den BufferHead frei. Dem Cache wird dadurch mitgeteilt, dass der 
     * BufferHead nicht mehr verwendet wird und in die LRU-Liste eingefuegt werden kann.
     * Falls er als "dirty" markiert ist, wird der Zeitpunkt
     * festgesetzt, zur dem er fr&uuml;hestens geschrieben werden darf
     * (um weitere Zugriffe auf den Puffer ohne die Unterbrechung
     * eines langsamen Festplattenzugriffs zu erm&ouml;glichen). 
     * Diese Zeitspanne betr&auml;gt 30 Sekunden. Ist der
     * Zugriffsz&auml;hler des <code>BufferHead</code>s (<code>b_count</code>) 
     * gleich 0, wird er, falls sein Inhalt g&uuml;ltig
     * ist, ans Ende der LRU-Liste geh&auml;ngt, ansonsten an den Anfang.
     *
     * @param bh der <code>BufferHead</code>, der freigegeben werden soll
     */
    final public  void brelse(jx.fs.buffercache.BufferHead bh0) {
	BufferHead bh = (BufferHead)bh0;
	int newtime;

	if (bh == null)  return;

	if (trace) Debug.out.println("brelse(" + bh.getBlock() +")");
	
	//	if (bh.dirty()) writeBlock( bh, false);
	
	//Debug.out.println("brelse(): " + bh.b_block + ", dirty: " + bh.dirty());
	// Falls dirty, Zeit festlegen, zu der der Buffer geschrieben werden soll
	if (bh.isDirty()) {
	    newtime = clock.getTimeInMillis() + 30000; // 30 Sekunden
	    if (bh.b_flushtime == 0 || bh.b_flushtime > newtime)
		bh.b_flushtime = newtime;
	} else
	    bh.b_flushtime = 0;
	
	release(bh);
    }


    final private void release(BufferHead bh) {
	if (bh.isUnused()) {
	    Debug.out.println("!!! BufferCache.brelse(): attamt to release an unused block: " + bh.b_block);
	    showBuffers();
	    cpuManager.printStackTrace();	  
	    return;
	} else {
	    bh.unref();
	    if (bh.isUnused()) {
		if (bh.isUptodate())
		    putLastFree(bh);
		else
		    putFirstFree(bh);
	    }
	}
    }  

    /**
     * H&auml;ngt den <code>BufferHead</code> ans Ende der LRU-Liste.
     *
     * @param bh der <code>BufferHead</code>, der freigegeben ("vergessen") werden soll
     */
    final public  void bforget(jx.fs.buffercache.BufferHead bh0) {
	BufferHead bh = (BufferHead)bh0;
	if (bh == null)
	    return;
	    
	if (! bh.isUsedOnlyByMe() || bh.isLocked()) {
	    brelse(bh);
	    return;
	}
	bh.unref();
	putLastFree(bh);
    }
    
    /**
     * Liest den angegebenen Block von Festplatte. Der entsprechende <code>BufferHead</code> wird mittels <code>getblk</code>
     * angefordert und der Inhalt des Blocks vom Festplattentreiber gelesen.
     *
     * @param  block  die Nummer des Blocks, der gelesen werden soll
     * @param  size   die Gr&ouml;&szlig;e des gew&uuml;nschten Blocks (normalerweise Vielfache von 1024)
     * @return der <code>BufferHead</code> mit dem Inhalt des angegebenen Blocks
     */
    final public  jx.fs.buffercache.BufferHead bread(int block) {
	jx.fs.buffercache.BufferHead bh;


	if (trace) Debug.out.println("bread(" + block +")");

	bh = getblk(block);
	if (bh.isUptodate())
	    return bh;
	readBlock(bh, true);
	if (bh.isUptodate())
	    return bh;
	throw new Error("IO Error");
	/*
	  brelse(bh);
	  Debug.out.println("bread(" + block + ", " + size + ") fehlgeschlagen");
	  return null;
	*/
    }

    private  void growBuffers() {
	BufferHead bh;
	int isize, count;

	if (trace) Debug.out.println("growBuffers");

	if (free_list.size() == 0) 
	    count = initialNumberOfBlocks;
	else
	    count = incrementNumberOfBlocks;

	if (free_list.size() + count > maximalNumberOfBlocks)
	    throw new Error("nr_buffers + count > maximalNumberOfBlocks");

	for (int i = 0; i < count; i++) {
	    bh = new BufferHead(memMgr, -1, bufferSize);
	    free_list.appendElement(bh);
	    nr_buffers++;
	}
	if (trace) Debug.out.println("BufferCache: created " + count + " buffers");
	Debug.out.println("buffercache.BufferCache: size now: " + (nr_buffers*bufferSize) + " bytes");
	//buffer_wait.wakeUp();
	//buffer_wait.notify();

    }

    /**
     * Gibt eine Statistik der <code>BufferHead</code>s im Cache und der LRU-Liste aus. Die Zahl der gesperrten
     * und als "dirty" markierten <code>BufferHead</code>s wird ermittelt.
     */
    public  void showBuffers() {
	BufferHead bh = null;
	int found = 0, locked = 0, dirty = 0, used = 0;
	String buf_types[] = new String[] {"CLEAN", "LOCKED", "DIRTY"};

	Debug.out.print("BufferHeads: " + nr_buffers);
	Debug.out.println(", free_list: " + free_list.size());

	Iterator e = buffer_hashtable.iterator();
	while (e.hasNext()) {
	    bh = (BufferHead)e.next();
	
	    if (bh == null) {
		Debug.out.println("bh == null !!!!!!!");
		continue;
	    }
	
	    found++;
	    if (bh.isLocked()) {
		/*System.out*/Debug.out.println("Block: " + bh.b_block + " = locked");
		locked++;
	    }
	    if (bh.dirty()) {
		/*System.out*/Debug.out.println("Block: " + bh.b_block + "  = dirty");
		dirty++;
	    }
	    if (bh.isUsed()) {
		/*System.out*/Debug.out.println("Block: " + bh.b_block + " = used");
		used++;
	    }
	
	    ///*System.out*/Debug.out.print("Block: " + bh.b_block + " (usecount = " + bh.b_count + ")");
	    //if (bh.dirty()) /*System.out*/Debug.out.print(", dirty");
	    //if (bh.isLocked()) /*System.out*/Debug.out.print(", gesperrt");
	    ///*System.out*/Debug.out.println(" ");
	}
	Debug.out.println("Hashtable: " + found + " buffers, " + used + " benutzt, " +
			  locked + " gesperrt, " + dirty + " dirty");
    }

    /**
     * Schreibt die <code>BufferHead</code>s des angegebenen Dateisystems (Partition), die als "dirty" markiert sind, auf
     * die angegebene Partition.
     *
     */
    public  void syncDevice(boolean wait) {
	if (debugSync) { 
	    Debug.out.println("Buffers before sync");
	    showBuffers();
	}
	syncBuffers(wait, true);
	if (debugSync) { 
	    Debug.out.println("Buffers after sync");
	    showBuffers();
	}
    }

    private  void syncBuffers(boolean wait, boolean signalError) {
	BufferHead bh = null;
	    
	Iterator e = buffer_hashtable.iterator();
	while (e.hasNext()) {
	    bh = (BufferHead)e.next();
	
	    if (bh == null) {
		Debug.out.println("bh == null !!!!!!!");
		continue;
	    }
	
	    if (bh.isLocked()) {  // Buffer ist gesperrt, falls wait==false, ignorieren
		if (signalError) throw new Error("cannot sync");
		if (wait == false)
		    continue;
		bh.waitOn();
		continue;
	    }
	
	    // Falls ein Buffer nicht gesperrt und nicht uptodate ist, ignorieren (IO-Fehler)
	    if ((wait == true) && (bh.isLocked() == false) &&
		(bh.dirty() == false) && (bh.isUptodate() == false)) {
		continue;
	    }
	
	    // Nur dirty Buffer schreiben
	    if (bh.dirty() == false)
		continue;
	
	    bh.ref();
	    if (debugSync) Debug.out.println("writing " + bh.b_block);
	    bh.b_flushtime = 0;
	    writeBlock( bh, true);
	    bh.unref();
	    if (bh.isUnused())
		bh.markClean();
	}
    }

    /**
     * Alle <code>BufferHead</code>s, die als "dirty" markiert sind, d.h. noch geschrieben werden m&uuml;ssen, werden mittels
     * <code>rwBlock</code> auf die Festplatte zur&uuml;ckgeschrieben, sofern sie nicht gesperrt sind bzw. noch nicht alt genug
     * sind.
     */
    public  void flushCache() {
	int ndirty = 0, nwritten = 0, ncount = 0;
	BufferHead bh = null;

	Debug.out.println("flushing ...");

	Iterator e = buffer_hashtable.iterator();
	while (e.hasNext()) {
	    bh = (BufferHead)e.next();

	    if (bh == null) {
		Debug.out.println("bh == null !!!!!!!");
		continue;
	    }
		    
	    if (bh.isLocked())
		Debug.out.println("Buffer " + bh.b_block + " locked");
	    
	    if (bh.isLocked() || (bh.dirty() == false))
		continue;
	    ndirty++;
	    if (clock.getTimeInMillis() < bh.b_flushtime) {
		///*System.out*/Debug.out.println("nicht alt genug " + bh.b_block);
		continue;
	    }
	    nwritten++;
	    bh.ref();
	    bh.b_flushtime = 0;
	    //Debug.out.println("writing " + bh.b_block);
	    writeBlock( bh, false);
	    ncount++;
	    bh.unref();
	    if (bh.isUnused())
		bh.markClean();
	}
	//if (ncount > 0)
	///*System.out*/Debug.out.println("BufferCache.flush(): "+ncount+" dirty BufferHeads in der dirty-Liste");
	///*System.out*/Debug.out.println("Wrote "+nwritten+"/"+ndirty+" buffers");

	//showBuffers();
    }

    final public void updateBuffer(jx.fs.buffercache.BufferHead bh) {
	if (! bh.isUptodate()) {
	    /*if (profiler.isSampling()) {
		Debug.out.println("update buffer block="+bh.getBlock());
		printStatistics();
		}*/
	    readBlock(bh, true);
	    if (! bh.isUptodate()) {
		throw new Error();
	    }
	}
    }

    final public void bdwrite(jx.fs.buffercache.BufferHead bh) {
	bh.markDirty();
	brelse(bh);
    }

    final public void bdirty(jx.fs.buffercache.BufferHead bh) {
	bh.markDirty();
    }

    final public void bawrite(jx.fs.buffercache.BufferHead bh) {
	bwrite(bh);
    }

    final public void bwrite(jx.fs.buffercache.BufferHead bh) {
	bh.markDirty();
	writeBlock(bh, true);
	release((BufferHead)bh);
    }


    public void breadn(int startBlock, int nBlocks) {
	throw new Error();
    }

    final public void readAhead(jx.fs.buffercache.BufferHead bh) {
	readBlock(bh, true);
	brelse(bh);
	throw new Error();
    }


    public void printStatistics() {
	Debug.out.println("Buffercache:");
	Debug.out.println("hits:"+hit+", missed:"+miss);
	Debug.out.println("Hashtable:");
	buffer_hashtable.printStatistics();
    }
}
