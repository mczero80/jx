package javafs;

import java.util.Hashtable;
import java.util.Vector;
import jx.zero.Debug;
import jx.zero.Clock;
import jx.fs.DeviceNaming;
import jx.fs.NotExistException;

import jx.fs.buffercache.*;

/**
 * Diese Klasse ist die zentrale Stelle f&uuml;r die Verwaltung des Dateisystems. Hier lassen sich neue Bl&ouml;ocke und Inodes
 * anfordern, Bl&ouml;cke und Inodes freigeben und allgemeine Informationen &uuml;ber das Dateisystem erhalten, die &uuml;ber
 * das Konzept der Inode (als Hauptklasse des Packets <code>fs</code> hinausgehen.
 */
public class Super {
    private Vector         desc_vector;
    private Vector         bitmap_cache;
    private BufferHead     sb_bh; // Superblock-BufferHead
    private SuperBlockData sb_data; // Superblock-Inhalt
    private Vector         s_dirty;
    private boolean        s_readonly;
    private int            s_db_per_group, s_desc_per_block, s_itb_per_group;
    /** die Anzahl der Blockgruppen */
    public  int            s_groups_count;
    /** die Anzahl der Inodes innerhalb des Dateisystems */
    public  int            s_inodes_count;
    /** die Gro&ouml;&szlig;e eines Blocks in Byte (ein Vielfaches von 512) */
    public  int            s_blocksize;
    /** die Nummer des ersten Datenblocks */
    public  int            s_first_data_block;
    private BufferCache    bufferCache;
    private InodeCache     inodeCache;
    private DirInode       rootInode;

    Clock clock;

    FileSystem fileSystem;

    private static final boolean dumpDetails = false;

    public Super(FileSystem fileSystem, boolean s_readonly, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	this.fileSystem = fileSystem;
	s_dirty = new Vector(100); // 100 ???
	bitmap_cache = new Vector(16);
	this.s_readonly = s_readonly;
	this.bufferCache = bufferCache;
	this.inodeCache = inodeCache;
	this.clock = clock;
	try {
	    readSuper();
	} catch (BufferIOException e) { throw new Error(); }
    }

    public void dump() {
	Debug.out.println("Number of blockgroups: "+s_groups_count);
	Debug.out.println("Number of inodes: "+s_inodes_count);
	Debug.out.println("Blocksize: "+s_blocksize);
	Debug.out.println("First data block: "+s_first_data_block);
    }

    /**
     * Gibt zur&uuml;ck, ob das Dateisystem als nur lesbar angemeldet wurde.
     *
     * @return falls auf das Dateisystem nur lesend zugegriffen werden darf, <code>true</code>, sonst <code>false</code>
     */
    public boolean isReadonly() {
	return s_readonly;
    }

    /**
     * Liefert den freien Platz auf dem Dateisystem.
     *
     * @return der freie Platz in Byte
     */
    public int available() {
	if (sb_data.s_free_blocks_count() < sb_data.s_r_blocks_count())
	    return 0;
	return ((sb_data.s_free_blocks_count() - sb_data.s_r_blocks_count()) * s_blocksize);
    }


    public int getNumberOfBlocks() {
	return sb_data.s_blocks_count();
    }
    public int getNumberOfFreeBlocks() {
	return sb_data.s_free_blocks_count();
    }


    /**
     * Gibt die Verwaltungsbl&ouml;cke des Dateisystems frei (Superblock, Gruppenbezeichner und Bitmaps). Danach sollte
     * nicht mehr auf das Dateisystem zugegriffen werden.
     */
    public  void putSuper() {
	GroupData desc;
	Bitmap bitmap;
	int blocknr = -1;

	rootInode.decUseCount();
	for (int i = 0; i < desc_vector.size(); i++) {
	    desc = (GroupData)desc_vector.elementAt(i);
	    if (desc.bh.getBlock() != blocknr) {
		blocknr = desc.bh.getBlock();
		bufferCache.brelse(desc.bh);
	    }
	}
	desc_vector.removeAllElements();

	for (int i = 0; i < bitmap_cache.size(); i++) {
	    bitmap = (Bitmap)bitmap_cache.elementAt(i);
	    bitmap.releaseBitmap();
	}
	bitmap_cache.removeAllElements();

	bufferCache.brelse(sb_bh);
    }

    private  void readSuper() throws BufferIOException {
	BufferHead bh;
	GroupData desc;
	//Debug.out.println("Reading superblock of device ");

        if ((sb_bh = bufferCache.bread(1)) == null) {
	    /*System.out*/Debug.out.println("unable to read superblock");
	    throw new BufferIOException();
        }
	sb_data = new SuperBlockData(sb_bh);
	//sb_data.dump();


	s_blocksize = 1024;
	for (int i = 0; i < sb_data.s_log_block_size(); i++)
	    s_blocksize *= 2;
	//Debug.out.println("readSuper(): blocksize: " + s_blocksize);

	s_inodes_count = sb_data.s_inodes_count();
	if (sb_data.s_inodes_count() == 0) {
	    Debug.out.println("Superblock ungueltig 1");
	    throw new BufferIOException();
	}
	if (sb_data.s_blocks_per_group() == 0) {
	    Debug.out.println("Superblock ungueltig 2");
	    throw new BufferIOException();
	}
	s_desc_per_block = s_blocksize / 32;
	s_first_data_block = sb_data.s_first_data_block();
	s_groups_count = (sb_data.s_blocks_count() - s_first_data_block + sb_data.s_blocks_per_group() - 1) /
	    sb_data.s_blocks_per_group();
        s_db_per_group = (s_groups_count + s_desc_per_block - 1) / s_desc_per_block;  // Anzahl Desc-Bloecke pro Gruppe
	s_itb_per_group = sb_data.s_inodes_per_group() * sb_data.s_inode_size() / s_blocksize;

	desc_vector = new Vector(s_groups_count);

	for (int i = 0; i < s_db_per_group; i++) {
	    bh = bufferCache.bread(sb_data.s_first_data_block()+i+1);
	    for (int j = 0; j < s_desc_per_block && i*s_desc_per_block+j < s_groups_count; j++) {
		desc = new GroupData(bh, j*32);
		if (dumpDetails) Debug.out.println("  info: block/offset: " + desc.bh.getBlock() + "/" + desc.offset + " bbitmap: " + desc.bg_block_bitmap() + " ibitmap: " + desc.bg_inode_bitmap() + " itable: " + desc.bg_inode_table() + " free blocks: " + desc.bg_free_blocks_count() + " free inodes: " + desc.bg_free_inodes_count() + " used dirs: " + desc.bg_used_dirs_count());
		desc_vector.addElement(desc);
	    }
	}
	
	setupSuper();
    }

    private  void setupSuper() {
	sb_data.s_mtime(clock.getTimeInMillis());
	//Debug.out.println("setupSuper");
	try {
	    checkBlockBitmaps();
	    checkInodeBitmaps();
	} catch (BufferIOException e) { /* was tun? */ throw new Error(); }
        sb_bh.markDirty();
	InodeData i_data = getInodeData(2);
	rootInode = new DirInode(fileSystem, this, 2, i_data, bufferCache, inodeCache, clock);
	inodeCache.addInode(rootInode);
    }

    private  void commitSuper() {
        sb_data.s_wtime(clock.getTimeInMillis());
	sb_bh.markDirty();
    }

    public DirInode getRootInode() {
	rootInode.incUseCount();
	return rootInode;
    }

    /**
     * Markiert den Superblock als "dirty" und setzt den Zeitstempel der letzten &Auml;nderung.
     */
    public  void writeSuper() {
	sb_data.s_mtime(clock.getTimeInMillis());
	commitSuper();
    }

    /**
     * Liefert die Blockgruppe mit der Nummer <code>block_group</code> zur&uuml;ck. Um einen schnelleren Zugriff auf
     * h&auml;ufig verwendete Gruppen zu erm&ouml;glichen, wird ein Cache verwaltet, in dem zuerst gesucht wird. Wird die
     * gew&uuml;nschte Gruppe dort nicht gefunden, wird eine neue erzeugt und dem Cache hinzugef&uuml;gt.
     *
     * @param     block_group die Nummer der gew&uuml;nschten Blockgruppe
     * @param     desc        der Gruppenbezeichner der gew&uuml;nschten Blockgruppe
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  Bitmap loadBitmap(int block_nr)
	throws BufferIOException {
	/* moeglich waere auch, alle Elemente mit Bitmaps besetzt zu halten,
	   die dann nicht geloescht und neu erzeugt werden, sondern nur neu initialisiert. */
        int slot;
	Bitmap bitmap;
        
        /* Zuerst wird der Cache durchsucht. Neue Objekte werden am Anfang der Liste eingefuegt, damit schnell
	   darauf zugegriffen werden kann. */
	for (int i = 0; i < bitmap_cache.size(); i++) {
	    bitmap = (Bitmap)bitmap_cache.elementAt(i);
            if (bitmap.block_nr == block_nr) {
		//Debug.out.println("loadBitmap(): cache hit 1");
		return bitmap;
	    }
	}

	if (block_nr >= sb_data.s_blocks_count())
	    return null;

	// Der Cache ist voll. Das letzte Element wird geloescht, um Platz fuer die gewuenschte Gruppe zu schaffen.
	if (bitmap_cache.size() > 15) {
	    try {
		bitmap = (Bitmap)bitmap_cache.elementAt(15);
		bitmap.releaseBitmap();
		bitmap_cache.removeElementAt(15);
	    } catch (ArrayIndexOutOfBoundsException e) { /* kann nicht passieren */ }
	}

	//Debug.out.println("block_bitmap: " + desc.bg_block_bitmap());
	// Eine neue Gruppe wird erzeugt, initialisiert und dem Cache hinzugefuegt (an Position 0)
	
	bitmap = new Bitmap(bufferCache,  s_blocksize, block_nr);
	try {
	    bitmap_cache.insertElementAt(bitmap, 0);
	} catch (ArrayIndexOutOfBoundsException e) { /* kann nicht passieren */ }
	bitmap.loadBitmap();
	return bitmap;
    }

    /**
     * Liefert die im Superblock gespeicherte Anzahl an freien Inodes zur&uuml;ck. Falls die Debugfunktion aktiviert ist,
     * werden zum einen die freien Inodes pro Gruppe (unter Verwendung von Bitmap-Objekten) gez&auml;hlt und aufsummiert.
     * Zum anderen wird die Zahl der freien Inodes, wie sie in den Gruppenbezeichner gespeichert ist, ebenfalls aufsummiert.
     *
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  int countFreeInodes() throws BufferIOException {
	if (true) {
	    GroupData desc;
	    Bitmap bitmap;
	    int desc_count = 0, bitmap_count = 0;

	    for (int i = 0; i < s_groups_count; i++) {
		desc = (GroupData)desc_vector.elementAt(i);
		desc_count += desc.bg_free_inodes_count();
		bitmap = loadBitmap(desc.bg_inode_bitmap());
		if (bitmap == null)
		    return 0;
		bitmap_count += bitmap.countFree(sb_data.s_inodes_per_group());
	    }

	    Debug.out.println("countFreeInodes: stored = " + sb_data.s_free_inodes_count() + ", computed = " + desc_count + ", " + bitmap_count);
	    return desc_count;
	} else {
	    return sb_data.s_free_inodes_count();
	}
    }

    /**
     * Liefert die im Superblock gespeicherte Anzahl an freien Bl&ouml;cken zur&uuml;ck. Falls die Debugfunktion aktiviert ist,
     * werden zum einen die freien Bl&ouml;ke pro Gruppe (unter Verwendung von Bitmap-Objekten) gez&auml;hlt und aufsummiert.
     * Zum anderen wird die Zahl der freien Bl&ouml;cke, wie sie in den Gruppenbezeichner gespeichert ist, ebenfalls aufsummiert.
     *
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  int countFreeBlocks() throws BufferIOException {
	if (true) {
	    GroupData desc;
	    Bitmap bitmap;
	    int desc_count = 0, bitmap_count = 0;
	    
	    for (int i = 0; i < s_groups_count; i++) {
		desc = (GroupData)desc_vector.elementAt(i);
		desc_count += desc.bg_free_blocks_count();
		bitmap = loadBitmap(desc.bg_block_bitmap());
		if (bitmap == null)
		    return 0;
		if (i < s_groups_count-1)
		    bitmap_count += bitmap.countFree(sb_data.s_blocks_per_group());
		else
		    bitmap_count += bitmap.countFree((sb_data.s_blocks_count() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group());
	    }
	    
	    //Debug.out.println("countFreeBlocks: stored = " + sb_data.s_free_blocks_count + ", computed = " + desc_count + ", " + bitmap_count);
	    return desc_count;
	} else {
	    return sb_data.s_free_blocks_count();
	}
    }

    /**
     * Gibt eine zusammenh&auml;ngende Gruppe von Bl&ouml;cken frei. Die Blockgruppe, die den ersten der Bl&ouml;cke
     * enth&auml;lt (<code>block</code>) wird geladen und in der zugeh&ouml;rigen Blockbitmap die Bl&ouml;cke als frei
     * gekennzeichnet. Dabei wird aufgepa&szlig;t, da&szlig; keine Bl&ouml;cke, die f&uuml;r Verwaltungsdaten (Metadaten)
     * verwendet werden, freigegeben werden (Superblock, Gruppenbezeichner usw.).
     *
     * @param     block der erste freizugebende Block
     * @param     count die Anzahl der Bl&ouml;cke
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  void freeBlocks(int block, int count) throws BufferIOException {
        int block_group, bit, overflow;
	GroupData desc;
	Bitmap bitmap;

        if (block < sb_data.s_first_data_block() || (block + count) > sb_data.s_blocks_count()) {
	    /*System.out*/Debug.out.println("freeBlocks: Freeing blocks not in datazone - block = " + block + ", count = " + count);
	    return;
        }

        //Debug.out.println("freeing " + count + " block(s) beginning with " + block);

	while (true) {
	    overflow = 0;
	    block_group = (block - sb_data.s_first_data_block()) / sb_data.s_blocks_per_group();
	    bit = (block - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group();

	    // Ueberpruefen, dass wir nur Bloecke innerhalb der Gruppe freigeben
	    if (bit + count > sb_data.s_blocks_per_group()) {
		overflow = bit + count - sb_data.s_blocks_per_group();
		count -= overflow;
	    }
	    
	    desc = (GroupData)desc_vector.elementAt(block_group);
	    bitmap = loadBitmap(desc.bg_block_bitmap());
	    if (bitmap == null)
		return;

	    if (((block <= desc.bg_block_bitmap()) && (block + count - 1 >= desc.bg_block_bitmap())) ||
		((block <= desc.bg_inode_bitmap()) && (block + count - 1 >= desc.bg_inode_bitmap())) ||
		((block >= desc.bg_inode_table()) && (block <= desc.bg_inode_table() + s_itb_per_group + 1)) ||
		((block + count >= desc.bg_inode_table() + 1) && (block + count <= desc.bg_inode_table() + s_itb_per_group + 2))) {
                /*System.out*/Debug.out.println("freeBlocks(): Freeing blocks in system zones - Block = "+block+", count = "+count);
	    }
		
	    for (int i = 0; i < count; i++) {
		if (bitmap.testBit(bit + i) == 0) {
		    /*System.out*/Debug.out.println("freeBlocks(): bit already cleared for block "+block);
		} else {
		    bitmap.clearBit(bit + i);
		    bitmap.markBitmapDirty();
		    desc.bg_free_blocks_count((short)(desc.bg_free_blocks_count()+1));
		    sb_data.s_free_blocks_count(sb_data.s_free_blocks_count()+1);
		}
	    }

	    //bitmap.releaseBitmap();
	    desc.bh.markDirty();
	    sb_bh.markDirty();
	    
	    if (overflow > 0) {
		block += count;
		count = overflow;
		continue;
	    }
	    break;
	}
    }

    /**
     * Gibt die Inode frei. Die Blockgruppe, die die Inode <code>inode</code> enth&auml;lt, wird geladen und das entsprechende
     * Bit in der Inodebitmap gel&ouml;scht.
     *
     * @param     inode die freizugebende Inode
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  void freeInode(InodeImpl inode) throws BufferIOException {
	int ino, block_group, bit, bitmap_nr;
	GroupData desc;
	Bitmap bitmap;

        if (inode.i_count > 1) {
	    /*System.out*/Debug.out.println("ext2_free_inode: inode has count=" + inode.i_count);
	    return;
        }
        if (inode.i_data.i_links_count() > 0) {
	    /*System.out*/Debug.out.println("ext2_free_inode: inode has links count=" + inode.i_data.i_links_count());
	    return;
        }

        ino = inode.i_ino;
        Debug.out.println("freeing inode " + ino);

        if (ino < sb_data.s_first_ino() || ino > sb_data.s_inodes_count()) {
	    /*System.out*/Debug.out.println("freeInode(): reserved inode or nonexistent inode");
	    return;
        }
        block_group = (ino - 1) / sb_data.s_inodes_per_group();
        bit = (ino - 1) % sb_data.s_inodes_per_group();

	desc = (GroupData)desc_vector.elementAt(block_group);
	bitmap = loadBitmap(desc.bg_inode_bitmap());
	if (bitmap == null)
	    return; 
        
	if (bitmap.testBit(bit) == 0) {
	    /*System.out*/Debug.out.println("freeInode(): bit already cleared for inode " + ino);
        } else {
	    bitmap.clearBit(bit);
	    bitmap.markBitmapDirty();

	    desc.bg_free_inodes_count((short)(desc.bg_free_inodes_count()+1));
	    try {
		if (inode.isDirectory())
		    desc.bg_used_dirs_count((short)(desc.bg_used_dirs_count()-1));
	    } catch (NotExistException e) {     }
	    desc.bh.markDirty();

	    sb_data.s_free_inodes_count(sb_data.s_free_inodes_count()+1);
	    sb_bh.markDirty();
        }
	//bitmap.releaseBitmap();
    }

    /**
     * &Uuml;berpr&uuml;pft die Blockbitmaps aller Gruppen. Bl&ouml;cke, die Verwaltungsdaten (Metadaten) enthalten
     * (Superblock, Gruppenbezeichner, Bitmaps und Inode-Tabelle) d&uuml;rfen nicht als frei gekennzeichnet sein. Die Anzahl
     * belegter Bits in den Blockbitmaps mu&szlig; den Werten entsprechen, die im Superblock und in den Gruppenbezeichner
     * gespeichert sind.
     *
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  void checkBlockBitmaps() throws BufferIOException {
	checkBlockBitmaps(null);
    }

    /**
     * &Uuml;berpr&uuml;pft die Blockbitmaps aller Gruppen. Zus&auml;tzlich zur Funktionalit&auml;t von
     * <code>checkBlockBitmaps()</code> wird &uuml;berpr&uuml;ft, ob die in <code>block_map</code> angegebenen Bl&ouml;cke
     * in der Blockbitmap als belegt markiert sind (wird von <code>checkFS</code> verwendet).
     *
     * @param block_map enth&auml;lt als Schl&uuml;ssel die Nummern der Bl&ouml;cke, die in der Blockbitmap belegt sein sollten
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  void checkBlockBitmaps(Hashtable block_map) throws BufferIOException {
        int desc_count = 0, bitmap_count = 0;
	short x;
        int desc_blocks, count, block;
        GroupData desc;
	Bitmap bitmap;

        desc_blocks = (s_groups_count + s_desc_per_block - 1) / s_desc_per_block;
        for (int i = 0; i < s_groups_count; i++) {
	    desc = (GroupData)desc_vector.elementAt(i);

	    desc_count += desc.bg_free_inodes_count();
	    bitmap = loadBitmap(desc.bg_block_bitmap());
	    if (bitmap == null)
		return;
	    
	    if (bitmap.testBit(0) == 0) {
		/*System.out*/Debug.out.println("checkBlockBitmaps: Superblock in group "+i+" is marked free");
		bitmap.setBit(0);
		bitmap.markBitmapDirty();
	    }
		
	    for (int j = 0; j < desc_blocks; j++)
		if (bitmap.testBit(j + 1) == 0) {
		    /*System.out*/Debug.out.println("checkBlockBitmaps: Descriptor block #"+j+" in group "+i+" is marked free");
		    bitmap.setBit(j+1);
		    bitmap.markBitmapDirty();
		}

	    if (bitmap.testBit((desc.bg_block_bitmap() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group()) == 0) {
		/*System.out*/Debug.out.println("checkBlockBitmaps: Block bitmap for group "+i+" is marked free");
		bitmap.setBit((desc.bg_block_bitmap() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group());
		bitmap.markBitmapDirty();
	    }

	    if (bitmap.testBit((desc.bg_inode_bitmap() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group()) == 0) {
		/*System.out*/Debug.out.println("checkBlockBitmaps: Inode bitmap for group "+i+" is marked free");
		bitmap.setBit((desc.bg_inode_bitmap() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group());
		bitmap.markBitmapDirty();
	    }

	    for (int j = 0; j < s_itb_per_group; j++)
		if (bitmap.testBit((desc.bg_inode_table() + j - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group()) == 0) {
		    /*System.out*/Debug.out.println("checkBlockBitmaps: Block #"+j+" of the inode table in group "+i+" is marked free");
		    bitmap.setBit((desc.bg_inode_table() + j - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group());
		    bitmap.markBitmapDirty();
		}

	    if (block_map != null) {
		if (i < s_groups_count-1)
		    count = sb_data.s_blocks_per_group();
		else
		    count = (sb_data.s_blocks_count() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group();
		for (int j = 0; j < count; j++) {
		    block = i*sb_data.s_blocks_per_group() + j + sb_data.s_first_data_block();  // sb_data.s_first_data_block abziehen!
		    if (block_map.containsKey(new Integer(block))) {
			if (bitmap.testBit(j) == 0) {
			    /*System.out*/Debug.out.println("checkBlockBitmaps: Block #" + block + " is used but was marked free");
			    bitmap.setBit(j);
			    bitmap.markBitmapDirty();
			}
		    }
		}
	    }

	    if (i < s_groups_count-1)
		x = bitmap.countFree(sb_data.s_blocks_per_group());
	    else
		x = bitmap.countFree((sb_data.s_blocks_count() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group());
	    //bitmap.releaseBitmap();
	    if (desc.bg_free_blocks_count() != x) {
		/*System.out*/Debug.out.println("checkBlockBitmaps: Falsche Anzahl freier Blocke in Gruppe " + i + ": gesichert = " + desc.bg_free_blocks_count() + ", ermittelt = " + x + ": Wert wird korrigiert");
		desc.bg_free_blocks_count(x);
		desc.bh.markDirty();
	    }
	    bitmap_count += x;
        }
	if (sb_data.s_free_blocks_count() != bitmap_count) {
	   /*System.out*/Debug.out.println("checkBlockBitmaps: Falsche Anzahl freier Bloecke im Superblock: gesichert = " + sb_data.s_free_blocks_count() + ", ermittelt = " + bitmap_count + ": Wert wird korrigiert");
	   sb_data.s_free_blocks_count(bitmap_count);
	   sb_data.bh.markDirty();
	}
    }

    /**
     * &Uuml;berpr&uuml;ft die Inodebitmaps aller Gruppen. Die Anzahl belegter Bits in den Inodebitmaps mu&szlig; identisch
     * sein mit denen in den Gruppenbezeichner, die Summe mit der im Superblock.
     *
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  void checkInodeBitmaps() throws BufferIOException {
	int desc_count = 0, bitmap_count = 0;
	short x;
        GroupData desc;
	Bitmap bitmap;

	for (int i = 0; i < s_groups_count; i++) {
	    desc = (GroupData)desc_vector.elementAt(i);

	    desc_count += desc.bg_free_inodes_count();
	    bitmap = loadBitmap(desc.bg_inode_bitmap());
	    if (bitmap == null)
		return;

	    if (i == 0) {
		for (int j = 1; j < sb_data.s_first_ino(); j++) {
		    if (bitmap.testBit(j-1) == 0) {
			/*System.out*/Debug.out.println("checkInodeBitmaps: Inode " + (j-1) + " was marked free");
			bitmap.setBit(j-1);
			bitmap.markBitmapDirty();
		    }
		}
	    }
	    //if (i < s_groups_count-1)
		x = bitmap.countFree(sb_data.s_inodes_per_group());
		//else
		//x = bitmap.countFreeInodes(sb_data.s_inodes_count() % sb_data.s_inodes_per_group());
		//bitmap.releaseBitmap();
	    if (desc.bg_free_inodes_count() != x) {
		/*System.out*/Debug.out.println("checkInodeBitmaps: Wrong free inodes count in group " + i + ": stored = " + desc.bg_free_inodes_count() + ", counted = " + x);
		desc.bg_free_inodes_count(x);
		desc.bh.markDirty();
	    }
	    bitmap_count += x;
        }
        if (sb_data.s_free_inodes_count() != bitmap_count) {
	    /*System.out*/Debug.out.println("checkInodeBitmaps: Wrong free inodes count in super block: stored = " + sb_data.s_free_inodes_count() + ", counted = " + bitmap_count);
	    sb_data.s_free_inodes_count(bitmap_count);
	    sb_data.bh.markDirty();
	}
    }

    /**
     * Liefert die Nummer des ersten Blocks der Blockgruppe zur&uuml;ck, innerhalb der die angegebene Inode liegt.
     *
     * @param i_ino die Nummer der Inode
     * @return die Blocknummer des ersten Blocks der Blockgruppe
     */
    public  int groupStart(int i_ino) {
	int block_group = (i_ino - 1) / sb_data.s_inodes_per_group();
	return (block_group * sb_data.s_blocks_per_group() + sb_data.s_first_data_block());
    }

    /**
     * Liefert das <code>InodeData</code> der angegebenen Inode zur&uuml;ck. Der entsprechende Eintrag innerhalb der Inodetabelle
     * wird ermittelt und ein neues <code>InodeData</code>-Objekt erzeugt.
     *
     * @param i_ino die Nummer der Inode, deren Struktur gelesen werden soll
     * @return ein <code>InodeData</code>-Objekt, das die Struktur der angegebenen Inode enth&auml;lt
     */
    public  InodeData getInodeData(int i_ino) {
	BufferHead bh;
	GroupData desc;
	int block_group, offset, block;

	//inode.setLocked(true); aus InodeCache
	if (dumpDetails) Debug.out.println("getInodeData: getting " + i_ino);

        if ((i_ino != 2 /* ROOT_INO */ && i_ino < sb_data.s_first_ino()) ||
       	     (i_ino > sb_data.s_inodes_count())) {
	    /*System.out*/Debug.out.println("createInode: bad inode number " + i_ino);
	    return null;
        }
	block_group = (i_ino - 1) / sb_data.s_inodes_per_group();
	if (block_group >= s_groups_count) {
	    /*System.out*/Debug.out.println("createInode: group >= groups count");
	    return null;
        }
	
	if (dumpDetails) Debug.out.println("createInode: getting block group "+block_group);
	desc = (GroupData)desc_vector.elementAt(block_group);


	// Zuerste noch überprüfen, ob die Inode mit der Inodenummer i_ino überhaupt 
	// exisitiert. Dazu wird nachgeschaut, ob in der InodeBitmap das Bit gesetzt ist 
	// oder nicht.
 	try {
	    int position = ((i_ino - 1) % sb_data.s_inodes_per_group());
 	    Bitmap bitmap = loadBitmap(desc.bg_inode_bitmap());
 	    if (bitmap.testBit(position) == 0) return null;
 	} catch (BufferIOException e) {
 	    return null;
 	}

        // Den Offset innerhalb der Inodetabelle der Blockgruppe berechnen
        offset = ((i_ino - 1) % sb_data.s_inodes_per_group()) * sb_data.s_inode_size();
        block = desc.bg_inode_table() + (offset / s_blocksize);
        if ((bh = bufferCache.bread(block)) == null) {
	    /*System.out*/Debug.out.println("createInode(): unable to read inode block - inode = " + i_ino + ", block = " + block);
	    return null;
        }

	//inode.setLocked(false); aus InodeCache
	
	offset &= s_blocksize - 1;
	return new InodeData(bh, offset);
    }

    /**
     * Erzeugt eine neue Inode. Der Typ der Inode (Verzeichnis-, Datei- bzw. symbolischer Link-Inode) wird aus der
     * <code>mode</code>-Variablen ermittelt. Falls es sich um ein Verzeichnis handelt, wird die Inode in der ersten Blockgruppe
     * angelegt, die weniger Inodes als der Durchschnitt enth&auml;lt. Dadurch wird erreicht, da&szlig; die Verzeichnisse
     * gleichm&auml;ssig &uuml;ber die Partition verteilt werden. Bei einer Datei und einem symbolischen Link wird versucht, die
     * Inode in die Blockgruppe ihres &uuml;bergeordneten Verzeichnisses zu plazieren (<code>dir</code>). Falls dort nicht genug
     * Platz vorhanden ist, werden alle Blockgruppen nach einer mit freien Inodes durchsucht. Nachdem diese Blockgruppe geladen
     * ist, wird ihre Inodebitmap nach einem freien Bit durchsucht (<code>iFindFirstZeroBit()</code>). Die Festplattenstruktur
     * der Inode (in Form eines <code>InodeData</code>-Objekts) wird initialisiert und eine neue <code>FileInode</code>,
     * <code>DirInode</code> oder <code>SymlinkInode</code> erzeugt (je nach <code>mode</code>).
     *
     * @param dir  das &uuml;bergeordnete Verzeichnis
     * @param mode der Inodetyp (S_IFDIR, S_IFREG oder S_IFLNK) und Zugriffsrechte
     */
    public  InodeImpl newInode(InodeImpl dir, int mode) {
	int avefreei, i = 0, j;
	InodeImpl inode;
	InodeData i_data;
	GroupData desc = null, tmp_desc;
	Bitmap bitmap;

	if (dir == null || dir.i_data.i_links_count() == 0) {
	    /*System.out*/Debug.out.println("newInode(): dir doesn't exist");
	    return null;
	}

	if ((mode & InodeImpl.S_IFDIR) > 0) {
	    /* falls es sich um ein Verzeichnis handelt, die erste Blockgruppe suchen, die weniger Inodes als der Durchschnitt
	       enthaelt. */
	    avefreei = sb_data.s_free_inodes_count() / s_groups_count;
	    for (j = 0; j < s_groups_count; j++) {
		tmp_desc = (GroupData)desc_vector.elementAt(j);
		//Debug.out.println("avefreei = " + avefreei + " / " + tmp_desc.bg_free_inodes_count());
		if (tmp_desc != null && tmp_desc.bg_free_inodes_count() >= avefreei) {
		    if (desc == null || tmp_desc.bg_free_blocks_count() > desc.bg_free_blocks_count()) {
			i = j;
			desc = tmp_desc;
		    }
		}
	    }
	} else { // Datei (oder symbolischer Link)
	    // es wird versucht, die Inode in die Blockgruppe ihres uebergeordnetes Verzeichnisses zu plazieren
	    i = (dir.i_ino - 1) / sb_data.s_inodes_per_group();
	    //Debug.out.println("parent - block_group = " + i);
	    tmp_desc = (GroupData)desc_vector.elementAt(i);
	    if (tmp_desc != null && tmp_desc.bg_free_inodes_count() > 0)
		desc = tmp_desc;
	    else {
		/* die Blockgruppennummer bei jedem Durchlauf verdoppeln, damit die Inodes besser ueber die
		   Partition verteilt werden.
		*/
		for (j = 1; j < s_groups_count; j <<= 1) {
		    i += j;
		    if (i >= s_groups_count)
			i -= s_groups_count;
		    tmp_desc = (GroupData)desc_vector.elementAt(i);
		    if (tmp_desc != null && tmp_desc.bg_free_inodes_count() > 0) {
			desc = tmp_desc;
			break;
		    }
		}
	    }
	    if (desc == null) {
		// lineare Suche durch die Blockgruppen nach einer freien Inode 
		i = (dir.i_ino - 1) / sb_data.s_inodes_per_group() + 1;
		for (j = 2; j < s_groups_count; j++) {
		    if (++i >= s_groups_count)
			i = 0;
		    tmp_desc = (GroupData)desc_vector.elementAt(i);
		    if (tmp_desc != null && tmp_desc.bg_free_inodes_count() > 0) {
			desc = tmp_desc;
			break;
		    }
		}
	    }
	}
        if (desc == null) {
	    Debug.out.println("no free group found");
	    return null;
	}

	//Debug.out.println("newInode(): loading bitmap of block group " + i);
	try {
	    bitmap = loadBitmap(desc.bg_inode_bitmap());
	    if (bitmap == null) {
		Debug.out.println("bitmap == null");
		return null;
	    }
	} catch (BufferIOException e) {
	    Debug.out.println("loadBitmap: BufferIOException");
	    return null;
	}

        if ((j = bitmap.findFirstZeroBit()) != -1) {
	    //Debug.out.println("  found free inode at " + j + " (i_ino == " + (i*sb_data.s_inodes_per_group()+j+1) + ")");
	    bitmap.setBit(j);
	    //for (int m = 0; m < 20; m++)
	    ///*System.out*/Debug.out.println("bit " + m + " = " + group.iTestBit(m));
	    bitmap.markBitmapDirty();
	    //bitmap.releaseBitmap();
        } else {
	    //bitmap.releaseBitmap();
	    if (desc.bg_free_inodes_count() != 0) {
		/*System.out*/Debug.out.println("Free inodes count corrupted in group " + i);
		return null;
	    }
	    Debug.out.println("  no free inode found");
	    return null;
	}
	j += i * sb_data.s_inodes_per_group() + 1;
        if (j < sb_data.s_first_ino() || j > sb_data.s_inodes_count()) {
	    /*System.out*/Debug.out.println("newInode(): reserved inode or inode > inodes count - block_group = "+ i +", inode = " + j);
	    return null;
        }

	desc.bg_free_inodes_count((short)(desc.bg_free_inodes_count()-1));
	if ((mode & InodeImpl.S_IFDIR) > 0)
	    desc.bg_used_dirs_count((short)(desc.bg_used_dirs_count()+1));
	desc.bh.markDirty();
	//Debug.out.println("free: " + sb_data.s_free_inodes_count());
        sb_data.s_free_inodes_count(sb_data.s_free_inodes_count()-1);
	//Debug.out.println("free: " + sb_data.s_free_inodes_count());
        sb_bh.markDirty();

	i_data = getInodeData(j);
	if (mode > 0) {
	    i_data.clear();
	    i_data.i_mode((short)mode);
	    i_data.i_links_count((short)1);
	    i_data.i_uid((short)0);

	    //i_data.i_uid(current.fsuid);
	    if ((dir.i_data.i_mode() & InodeImpl.S_ISGID) > 0) {
                i_data.i_gid(dir.i_data.i_gid());
                if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFDIR)
		    i_data.i_mode((short)(i_data.i_mode() | InodeImpl.S_ISGID));
	    }
            // else i_data.i_gid(current.fsgid);

	    i_data.i_blocks(0);
	    for (int z = 0; z < 15; z++)
		i_data.i_block(z, 0);
	    int time = clock.getTimeInMillis();
	    i_data.i_atime(time);
	    i_data.i_ctime(time);
	    i_data.i_mtime(time);
	    i_data.i_dtime(0);
	    i_data.i_version(time);
	    i_data.bh.markDirty();
	}
	inode = null;
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFREG) {
	    //Debug.out.println("newInode(): erzeuge FileInode");
	    inode = new FileInode(fileSystem, this, j, i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFDIR) {
	    //Debug.out.println("newInode(): erzeuge DirInode");
	    inode = new DirInode(fileSystem, this, j, i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFLNK) {
	    //Debug.out.println("newInode(): erzeuge SymlinkInode");
	    inode = new SymlinkInode(fileSystem, this, j, i_data, bufferCache, inodeCache, clock);
	}
	if (inode == null) {
	    /*System.out*/Debug.out.println("newInode(): Fehler beim Erzeugen der Inode");
	    return null;
	}
	inode.setParent(dir); 
	inodeCache.addInode(inode);
	//Debug.out.println("-- " + inode.i_data.bh.b_block + " / " + dir.i_data.bh.b_block);
	inode.setDirty(true);

	//Debug.out.println("allocating inode " + inode.i_ino);

        return inode;
    }

    final public  int newBlock(int goal) throws BufferIOException {
	return  newBlock(goal, true);
    }
    /**
     * Liefert einen nicht benutzten Block. Es wird versucht, den Block <code>goal</code>, einen Block in der unmittelbaren
     * N&auml;he oder wenigstens einen Block in der gleichen Blockgruppe zu reservieren. Zuerst wird die zugeh&ouml;rige
     * Blockgruppe geladen und in dieser der Block <code>goal</code> &uuml;berpr&uuml;ft. Ist er schon belegt, wird nach einem
     * freien Byte innerhalb der Blockbitmap gesucht (um zusammengeh&ouml;rende Bl&ouml;cke einer Inode besser zu gruppieren)
     * oder bei Mi&szlig;erfolg nach einem freien Bit. Ist die Blockgruppe v&ouml;llig ausgesch&ouml;pft, wird nach einer Gruppe
     * mit freien Bl&ouml;cken gesucht und in der zugeh&ouml;rigen Blockbitmap zuerst nach einem freien Byte, dann nach einem
     * freien Bit. Der so ermittelte Block wird als belegt gekennzeichnet (<code>bSetBit()</code>) und seine Nummer
     * zur&uuml;ckgeliefert.
     *
     * @param goal der gew&uuml;nschte Block
     * @param clear clear new block
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    public  int newBlock(int goal, boolean clear) throws BufferIOException {
	int i, j = 0, k, tmp;
	BufferHead bh;
	Bitmap bitmap = null;
	GroupData desc;

	if (sb_data.s_free_blocks_count() < sb_data.s_r_blocks_count()) {
	    sb_data.dump();
	    // TODO: ROOT is allowed to use reserved blocks
	    throw new Error("no more free blocks (free blocks < reserved blocks)");
	}

	if (goal < sb_data.s_first_data_block() || goal >= sb_data.s_blocks_count())
	    goal = sb_data.s_first_data_block();
        i = (goal - sb_data.s_first_data_block()) / sb_data.s_blocks_per_group();

	desc = (GroupData)desc_vector.elementAt(i);

	if (desc.bg_free_blocks_count() > 0) {
	    j = (goal - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group();

	    bitmap = loadBitmap(desc.bg_block_bitmap());
	    if (bitmap == null)
		throw new Error("Bitmap could not be loaded");
                
	    if (bitmap.testBit(j) == 1) {
		/*
		 * Weitersuchen innerhalb derselben Blockgruppe (nach freiem BYTE)
		 */
		k = bitmap.findNextZeroByte(j); // nach freiem Byte suchen
		if (k == -1) {
		    k = bitmap.findNextZeroBit(j); // nach freiem Bit suchen
		}
		if (k != -1)
		    j = k;
		else
		    j = -1;
	    }
	}

	if (desc.bg_free_blocks_count() < 0 || j == -1) {
	    /*System.out*/Debug.out.println("Bit not found in block group " + i + ".\n");
	    /* Als naechstes werden die uebrigen Gruppen durchsucht. i und gdb
	       zeigen auf die letzte besuchte Gruppe. */
	    for (k = 0; k < s_groups_count; k++) {
		i++;
		if (i >= s_groups_count)
		    i = 0;
		
		desc = (GroupData)desc_vector.elementAt(i);		
		if (desc.bg_free_blocks_count() > 0)
		    break;
	    }
	    
	    if (k >= s_groups_count)
		throw new Error("no free block found in groups");
	    
	    bitmap = loadBitmap(desc.bg_block_bitmap());
	    if (bitmap == null)
		throw new Error("bitmap not loaded");
	    
	    if ((j = bitmap.findFirstZeroByte()) == -1) {
		if ((j = bitmap.findFirstZeroBit()) == -1) {
		    //bitmap.releaseBitmap();
		    /*System.out*/Debug.out.println("Free blocks count corrupted for block group " + i + ".\n");
		    throw new Error();
		}
	    }
	}

        //Debug.out.println("using block group " + i + " (" + desc.bg_free_blocks_count() + ").\n");

        tmp = j + i * sb_data.s_blocks_per_group() + sb_data.s_first_data_block(); // sb_data.s_first_data_block ???
	bitmap.setBit(j);

        //Debug.out.println("newBlock(): bit " + j + " markiert (Block " + tmp + ").");

        j = tmp;
	bitmap.markBitmapDirty();
	//bitmap.releaseBitmap();

        if (j >= sb_data.s_blocks_count()) {
	    /*System.out*/Debug.out.println("block >= blocks_count - block_group=" + i + ", block="+j);
	    throw new Error();
        }
        bh = bufferCache.getblk(j);
	if (clear) bh.clear();
        bh.markUptodate();
	bufferCache.bdwrite(bh);

        desc.bg_free_blocks_count((short)(desc.bg_free_blocks_count()-1));
        desc.bh.markDirty();
        sb_data.s_free_blocks_count(sb_data.s_free_blocks_count()-1);
        sb_bh.markDirty();
        return j;
    }
}
