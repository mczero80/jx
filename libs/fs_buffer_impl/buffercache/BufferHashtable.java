package buffercache;

import jx.zero.*;
import jx.collections.Iterator;
//import jx.fs.buffercache.BufferHead;

/**
 * Uses "chaining" as a collision resolution technique
 * @author Michael Golm
 */
class BufferHashtable {
    static final boolean trace = false;

    /** Default fill fraction allowed before growing table. */
    static final int DEFAULT_FILL_PERCENT = 30;
    
    /** Minimum size used for hash table. */
    static final int MINIMUM_SIZE = 31;

    /** Number of entries present in table. */
    int entryCount;
    
    int fillPercent;

    /** Entries allowed before growing table. */
    int entryLimit;
    
    /** Array of table slots. */
    BufferHead[] hashTable;
    int hashTable_length;

    /** Statistics */
    final static boolean doStatistics = true;
    
    /**
     * Constructor with full specification.
     * 
     * @param count number of values to assume in initial sizing of table
     * @param percentFill percent full allowed for table before growing
     */
    
    public BufferHashtable(int count, int percentFill) {
	Debug.out.println("BufferHashtable create");
	
	// check the passed in fill fraction
	if (percentFill <= 0 || percentFill >= 100) {
	    throw new IllegalArgumentException("fill value out of range "+Integer.toString(percentFill));
	}
	this.fillPercent = percentFill;
	
	// compute initial table size (ensuring odd)
	int size = Math.max(count * 100 / fillPercent, MINIMUM_SIZE);
	size += (size + 1) % 2;
	//Debug.out.println("size: "+size);
	
	// initialize the table information
	entryLimit = (int) (size * fillPercent / 100);

	hashTable = new BufferHead[size];
	hashTable_length = size;
    }
    

    /**
     * Constructor with only size supplied. Uses default value for fill
     * fraction.
     * 
     * @param count number of values to assume in initial sizing of table
     */
    
    public BufferHashtable(int count) {
	this(count, DEFAULT_FILL_PERCENT);
    }
    
    /**
     * Default constructor.
     */
    
    public BufferHashtable() {
	this(0, DEFAULT_FILL_PERCENT);
    }

    
    /**
     * Get number of items in table.
     * 
     * @return item count present
     */
    
    final public int size() {
	return entryCount;
    }
    
    /**
     * Expand the table. Increases the table size to twice the previous size
     * plus one, reinserting each entry from the smaller table into the new
     * one.
     */
    
    protected void expandTable() {	
	// initialize for the increased table size
	int size = hashTable.length * 2 + 1;
	Debug.out.println("expandTable to size="+size);
	entryLimit = (int) (size * fillPercent / 100);
	BufferHead[] old = hashTable;
	hashTable = new BufferHead[size];
	hashTable_length = size;
	// reinsert all entries into new table
	for (int i = 0; i < old.length; i++) {
	    BufferHead entry = old[i];
	    if (entry != null) {
		putInTable(entry);
		while((entry = entry.hashtable_nextInChain) != null) {
		    putInTable(entry);		
		}
	    }
	}
	
    }
    
    private void putInTable(BufferHead value) {
	int offset= value.hashtable_hashkey % hashTable_length;
	if (hashTable[offset] != null)  { // collision
	    BufferHead bh = hashTable[offset];
	    while (bh.hashtable_nextInChain != null) {
		bh = bh.hashtable_nextInChain;
	    }
	    bh.hashtable_nextInChain = value;
	    value.hashtable_nextInChain = null;
	} else {
	    hashTable[offset] = value;
	}
    }
	/**
     * Add an entry to the table. Note that nothing prevents multiple entries
     * with the same key, but the entry returned by that key will be
     * undetermined in this case.
     * 
     * @param entry entry to be added to table
     */
    
    final public void put(BufferHead value) {
	if (++entryCount > entryLimit) {
	    expandTable();
	}
	putInTable(value);
    }
    
    /**
     * Find an entry in the table.
     * 
     * @param key for entry to be returned
     * @return entry for key, or <code>null</code> if none
     */
    
    final public BufferHead get(int block) {
	int offset = block  % hashTable_length;
	//if (trace) Debug.out.println("Find Hashedentry at "+offset);
	for(BufferHead entry = hashTable[offset]; entry != null; entry=entry.hashtable_nextInChain) {
	    if (block == entry.hashtable_hashkey) {
		return entry;
	    }
	}
	return null;
    }

    /*
    public BufferHead get(BufferHeadHashKey key) {
	throw new Error();
    }
    */

    final public void remove(BufferHead value) {
	int offset = value.hashtable_hashkey % hashTable_length;
	BufferHead entry;
	entry = hashTable[offset];
	if (entry  == null) throw new Error("not found");
	if (value.hashtable_hashkey == entry.hashtable_hashkey) {
	    // first element is a match!
	    hashTable[offset] = entry.hashtable_nextInChain;
	    entryCount--;
	    return;
	}
	// search in collision chain
	while (entry.hashtable_nextInChain != null && 
	       value.hashtable_hashkey != entry.hashtable_nextInChain.hashtable_hashkey) {
	    entry = entry.hashtable_nextInChain;
	}
	if (entry.hashtable_nextInChain == null) throw new Error("not found");
	entry.hashtable_nextInChain = entry.hashtable_nextInChain.hashtable_nextInChain;
	entryCount--;
	return;
    }
    

    final public void printStatistics() {
	Debug.out.println("Hashtable statistics: table Length="+hashTable.length);
	int unused=0;
	int total=0;
	int collisions=0;
	int longest_chain=0;
	for(int i=0; i<hashTable.length; i++) {
	    if (hashTable[i] != null)  { // filled
		int length = 1;
		BufferHead bh = hashTable[i];
		while (bh.hashtable_nextInChain != null) {
		    bh = bh.hashtable_nextInChain;
		    length++;
		}
		total += length;
		collisions += length - 1;
		if (length > longest_chain) longest_chain = length;
		Debug.out.println("  " + alignString(Integer.toString((int)i), 11) 
				+ alignString(Integer.toString((int)(length)), 11)+ "   ");
	    } else {
		unused++;
	    } 
	}
	Debug.out.println("  total slots="+total);
	Debug.out.println("  unused slots: "+unused);
	Debug.out.println("  collisions: "+collisions);
	Debug.out.println("  longest chain: "+longest_chain);
    }
 
    /** do not use the returned Iterator while concurrently
	accessing the table */
    final public Iterator iterator() {
	return new Iterator() {
		int offset=init();
		BufferHead recent;
		int init() {
		    offset = 0;
		    do { 
			offset++;
		    } while(offset < hashTable.length && hashTable[offset] == null);
		    if (offset < hashTable.length) recent =  hashTable[offset];
		    return offset;
		}
		void advance() {
		    if (recent.hashtable_nextInChain != null) {
			recent = recent.hashtable_nextInChain;
		    }
		    do { 
			offset++;
		    } while(offset < hashTable.length && hashTable[offset] == null);
		    if (offset < hashTable.length) recent =  hashTable[offset];
		    else recent = null;
		}
		final public boolean hasNext() {
		    return recent != null;
		}
		final public Object next() {
		    Object ret = recent;
		    advance();
		    return ret;
		}
	    };
    }

    /*************************************************************************/
    /****                                                                 ****/
    /****                                                                 ****/
    /****                         CLASS TEST                              ****/
    /****                                                                 ****/
    /****                                                                 ****/
    /*************************************************************************/
	
    public static void main(String args[]) {
	MemoryManager memMgr = ((MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager"));
	BufferHashtable collect = new BufferHashtable();
	// fill
	BufferHead bh0, bh1, bh2;
	collect.put(bh0 = new BufferHead(memMgr, 0,1024));
	collect.put(bh1 = new BufferHead(memMgr,1,1024));
	collect.put(bh2 = new BufferHead(memMgr,2,1024));
	for(int i=3; i<1000; i++) {
	    collect.put(new BufferHead(memMgr,i,1024));
	}
	BufferHead c1=null;
	collect.put(new BufferHead(memMgr,3+4096,1024)); // collision
	collect.put(c1=new BufferHead(memMgr,3+2*4096,1024)); // collision
	collect.put(new BufferHead(memMgr,3+3*4096,1024)); // collision
	collect.put(new BufferHead(memMgr,3+4*4096,1024)); // collision
	BufferHead c2 = collect.get(3+2*4096); // collision
	if (c1==c2) {
	    Debug.out.println("collision success");
	} else {
	    Debug.out.println("collision failure");
	}
	
	BufferHead bh = collect.get(1);

	if (bh == bh1) {
	    Debug.out.println("BC success");
	} else {
	    Debug.out.println("BC failure");
	}
	collect.remove(bh2);
	bh = collect.get(1);

	if (bh == bh1) {
	    Debug.out.println("BC2 success");
	} else {
	    Debug.out.println("BC2 failure");
	}

    }


    private String alignString(String value, int length) {
	String tmp1 = new String();
	int leer = length - value.length();
	for (int i = 0; i < leer; i++)
	    tmp1 += " ";
	return (tmp1 + value);
    }


}
