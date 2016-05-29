package javafs;

import java.util.Hashtable;
import java.util.Vector;
import jx.zero.Debug;
import jx.zero.*;
import jx.bio.BlockIO;
import jx.fs.DeviceNaming;

import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.FSException;

import jx.fs.buffercache.*;

class TooSmallException extends Exception { }
class TooBigException extends Exception { }

/**
 * Diese Klasse bietet Methoden, um ein neues Dateisystem zu erstellen oder ein bestehendes Dateisystem zu &uuml;berpr&uuml;fen
 * und reparieren.
 */
public class Tools {
    private Vector desc_vector;
    private int group_desc_count, desc_blocks, desc_per_block, inode_blocks_per_group;
    private SuperBlockData sb_data;
    private int block_size;
    private BufferCache bufferCache;
    private InodeCache inodeCache;
    private BlockIO idedevice;
    private Clock clock;

    static final boolean debugFSCK = false;

    //private static Tools instance = new Tools();

     Tools(BlockIO blockDevice, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	this.bufferCache = bufferCache;
	this.inodeCache = inodeCache;
	this.clock = clock;
	idedevice = blockDevice;
    }

    /**
     * Erzeugt ein neues Dateisystem auf der angegebenen Partition bzw. Festplatte.
     * Zuerst wird das Dateisystem initialisiert, der Superblock, die Gruppenbezeichner, die Bitmaps und die Inode-Tabelle
     * werden angelegt und mit sinnvollen Werten gef&uuml;llt (in den Blockbitmaps werden z.B. die Bl&ouml;cke der
     * Gruppendescriptoren und der anderen Verwaltungsdaten reserviert). Als n&auml;chster Schritt werden die Blockgruppen
     * mit den Kopien der Superbl&ouml;cke und Gruppendescriptoren angelegt und geschrieben. Schlie&szlig;lich wird noch das
     * Wurzelverzeichnis ("<code>/</code>") erzeugt und die Daten der ersten Blockgruppe geschrieben.
     *
     * @param name       der Name, den das Dateisystem bekommen soll
     * @param blocksize  die Blockgr&ouml;&szlig;e, die f&uuml;r Bl&ouml;cke des Dateisystems verwendet werden soll
     * @param device     die Kennung der Partition, auf der das Dateisystem installiert werden soll
     */
    public void makeFS(String name, int blocksize) {
	makeFS(name, blocksize, idedevice.getCapacity());
    }

    /**
     * Erzeugt ein neues Dateisystem auf der angegebenen Partition
     * (s. <code>makeFS(String name, int blocksize, String devname, int max_blocks)</code>).
     *
     * @param name       der Name, den das Dateisystem bekommen soll
     * @param blocksize  die Blockgr&ouml;&szlig;e, die f&uuml;r Bl&ouml;cke des Dateisystems verwendet werden soll
     * @param max_blocks die Anzahl der Bl&ouml;cke, die vom Dateisystem belegt werden sollen (falls nicht die gesamte Partition
     *                   verwendet werden soll)
     */
    public void makeFS(String name, int blocksize, int max_blocks) {
	try {
	    /*System.out*/Debug.out.println("makeFS: verwende " + max_blocks/2048 + " MB.");
	    /*System.out*/Debug.out.println("Initialisiere Dateisystem  ...");
	    initializeFS(name, blocksize, max_blocks);
	    /*System.out*/Debug.out.println("Erzeuge Wurzelverzeichnis ...");
	    createRootDir();
	    /*System.out*/Debug.out.println("Schreibe Dateisystem ...");
	    closeFS();
	} catch (BufferIOException e) {
	    /*System.out*/Debug.out.println("Tools.makeFS(): IO-Fehler");
	} catch (TooSmallException e) {
	    /*System.out*/Debug.out.println("Tools.makeFS(): Platz fuer Filesystem nicht ausreichend");
	} catch (TooBigException e) {
	    /*System.out*/Debug.out.println("Tools.makeFS(): Dateisystem zu gross dimensioniert (Ueberlauf)");
	}
    }

    private void createRootDir() throws BufferIOException {
	InodeData  inode_data;
	DirEntryData de_data;
	GroupData  desc;
	Bitmap bitmap;
	BufferHead bh, dir_block;
	int nr;

	desc = (GroupData)desc_vector.firstElement();

	// reservierte Inodes auf 0 setzen !!!!!

	desc.bg_used_dirs_count((short)(desc.bg_used_dirs_count()+1));

	bh = bufferCache.bread(desc.bg_inode_table());

	inode_data = new InodeData(bh, sb_data.s_inode_size());  // Inode 2 (erste Inode: 1)
	inode_data.clear();
	inode_data.i_mode((short)(InodeImpl.S_IFDIR | 0755));
	inode_data.i_uid((short)0);
	inode_data.i_gid((short)0);
	inode_data.i_size(block_size);
        inode_data.i_links_count((short)2);
	//long time = System.currentTimeMillis();
        inode_data.i_ctime(0); // (time);
	inode_data.i_atime(0); // (time);
	inode_data.i_mtime(0); // (time);
	inode_data.i_dtime(0);
        inode_data.i_blocks(block_size / 512);
	for (int i = 0; i < 15; i++)
	    inode_data.i_block(i, 0);

	bitmap = new Bitmap(bufferCache, block_size, desc.bg_block_bitmap());
	bitmap.loadBitmap();
	nr = bitmap.findFirstZeroBit();
	bitmap.setBit(nr);
	bitmap.markBitmapDirty();
	bitmap.releaseBitmap();

	//Debug.out.println("erster freier Block: " + nr);
	nr += sb_data.s_first_data_block();
	//Debug.out.println("erster freier Block: " + nr);

        if ((dir_block = bufferCache.getblk(nr)) == null) {
	    Debug.out.println("cannot get block " + nr);
	    throw new BufferIOException();
        }
	inode_data.i_block(0, nr);

	dir_block.clear();
        //dir_block.markUptodate(true);

	//Debug.out.println("dir_block: " + dir_block.b_block);

	de_data = new DirEntryData(dir_block, 0);
	de_data.inode((short)2);
	de_data.name_len((short)1);
	de_data.rec_len((short)12);
	de_data.name(".");
	
	de_data.init(de_data.rec_len());
	de_data.inode((short)2);
	de_data.rec_len((short)(block_size - 12));
	de_data.name_len((short)2);
	de_data.name("..");

        desc.bg_free_blocks_count((short)(desc.bg_free_blocks_count()-1));
        desc.bh.markDirty();
        sb_data.s_free_blocks_count(sb_data.s_free_blocks_count()-1);

	//bufferCache.brelse(desc.bh);
	bufferCache.bdwrite(bh);
	bufferCache.bdwrite(dir_block);
	// eventuell Rechte setzen: inode_data.i_uid(getuid()); inode_data.i_gid(getgid());
    }

    private void closeFS() {
	inodeCache.syncInodes();
	GroupData desc;

	for (int j = 0; j < desc_blocks; j++) {
	    desc = (GroupData)desc_vector.elementAt(j*desc_per_block);
	    //Debug.out.println("  writing blockgroup 0 (block " + desc.bh.b_block + ")");
	    //Debug.out.println("  info: bbitmap: " + desc.bg_block_bitmap() + " ibitmap: " + desc.bg_inode_bitmap() + " itable: " + desc.bg_inode_table() + " free blocks: " + desc.bg_free_blocks_count() + " free inodes: " + desc.bg_free_inodes_count() + " used dirs: " + desc.bg_used_dirs_count());
	    desc.bh.markDirty();
	    bufferCache.brelse(desc.bh);
	}

        sb_data.s_wtime(clock.getTimeInMillis());
        sb_data.s_block_group_nr((short)0);
	sb_data.bh.markDirty();
	bufferCache.brelse(sb_data.bh);
	//bufferCache.showBuffers();
	bufferCache.syncDevice(false);
	inodeCache.syncInodes();
	bufferCache.syncDevice(true); // wait
	inodeCache.invalidateInodes();
	bufferCache.flushCache();
    }

    private void initializeFS(String name, int blocksize, int max_blocks) throws BufferIOException, TooSmallException, TooBigException {
	BufferHead sb_bh, bh = null;
	GroupData desc;
	InodeData inode_data;
	Bitmap bitmap;
	int numblocks, group_block, overhead, rem, blk, off;
        int sgrp, blockbits;

	if (blocksize < 1024)
	    throw new Error("blocksize < 1024");
	if (blocksize % 1024 != 0) {
	    Debug.out.println("Tools.initializeFS(): blocksize % 1024 != 0");
	    blocksize = (int)(blocksize/1024) * 1024;
	}

	if (max_blocks < 0)
	    throw new TooBigException();

	blockbits = 0;
	int blocksizetmp = blocksize;
	while (blocksizetmp != 1 && blocksizetmp > 0) {
	    blocksizetmp = blocksizetmp >> 1;
	    blockbits++;
	}
	blockbits -= 10;

	desc_vector = new Vector();
	block_size = blocksize;

	if ((sb_bh = bufferCache.getblk(1)) == null) {
	    Debug.out.println("Tools: unable to read superblock");
	    throw new BufferIOException();
        }
	sb_data = new SuperBlockData(sb_bh);
	sb_data.s_rev_level(0);

	sb_data.s_magic((short)0xEF53); // EXT2_SUPER_MAGIC
        sb_data.s_state((short)0x0001); // EXT2_VALID_FS

	sb_data.s_log_block_size(blockbits);
	sb_data.s_log_frag_size(blockbits);
	if (blocksize == 1024)
	    sb_data.s_first_data_block(1);
	else
	    sb_data.s_first_data_block(0);

	sb_data.s_mnt_count((short)0);
	sb_data.s_max_mnt_count((short)20); // EXT2_DFL_MAX_MNT_COUNT
        sb_data.s_errors((short)1); // EXT2_ERRORS_DEFAULT
        sb_data.s_feature_compat(0);
        sb_data.s_feature_incompat(0);
        sb_data.s_feature_ro_compat(0);
	sb_data.s_minor_rev_level((short)0);
        sb_data.s_rev_level(0); // EXT2_GOOD_OLD_REV
	sb_data.s_first_ino(11); // EXT2_GOOD_OLD_FIRST_INO
	sb_data.s_inode_size((short)128); // EXT2_GOOD_OLD_INODE_SIZE
	sb_data.s_lastcheck(0);
	sb_data.s_last_mounted("/");
	sb_data.s_checkinterval(0); // EXT2_DFL_CHECKINTERVAL
	sb_data.s_creator_os(0); // EXT2_OS_LINUX
	sb_data.s_def_resuid((short)0);
	sb_data.s_def_resgid((short)0);
	for (int i = 0; i < 16; i++)
	    sb_data.s_uuid(i, (byte)1);
	sb_data.s_alg_usage_bitmap(0);
	sb_data.s_prealloc_blocks((byte)0);
	sb_data.s_prealloc_dir_b((byte)0);

        sb_data.s_blocks_per_group(block_size * 8);
	sb_data.s_frags_per_group(sb_data.s_blocks_per_group());
	sb_data.s_blocks_count(max_blocks >> (blocksize / 1024));
	/*System.out*/Debug.out.println("  Erster Datenblock:  " + sb_data.s_first_data_block());
	/*System.out*/Debug.out.println("  Bloecke pro Gruppe: " + sb_data.s_blocks_per_group());
	/*System.out*/Debug.out.println("  Anzahl Bloecke:     " + sb_data.s_blocks_count());

	sb_data.s_r_blocks_count((sb_data.s_blocks_count() * 5) / 100);

	while (true) {
	    Debug.out.println("check 2");

	    group_desc_count = (sb_data.s_blocks_count() - sb_data.s_first_data_block() + sb_data.s_blocks_per_group() - 1) /
		sb_data.s_blocks_per_group();

	    if (group_desc_count == 0)
		throw new TooSmallException();
	    desc_per_block = block_size/32;
	    desc_blocks = (group_desc_count + desc_per_block - 1) / desc_per_block;
	    
	    // block_size ist <= 4096
	    sb_data.s_inodes_count(sb_data.s_blocks_count() / (4096 / block_size));

	    // Sicherstellen, dass wenigstens s_first_ino + 1 Inodes existieren
	    if (sb_data.s_inodes_count() < sb_data.s_first_ino()+1)
		sb_data.s_inodes_count(sb_data.s_first_ino()+1);

	    /* Anzahl der Inodes pro Gruppe berechnen (die Bitmap einer Gruppe
	       muss allerdings in einem Block untergebracht werden koennen) */
	    sb_data.s_inodes_per_group((sb_data.s_inodes_count() + group_desc_count - 1) / group_desc_count);
	    if (sb_data.s_inodes_per_group() > block_size*8)
                sb_data.s_inodes_per_group(block_size*8);
	    
	    /* Falls die Anzahl Inodes pro Gruppe die Inodetabelle nicht voll
	       ausfuellt (der letzte Block nicht voll belegt ist), werden noch
	       ein paar zusaetzliche Inodes hinzugefuegt. */
	    inode_blocks_per_group = (((sb_data.s_inodes_per_group() * sb_data.s_inode_size()) +
				       block_size - 1) / (block_size));
	    sb_data.s_inodes_per_group((inode_blocks_per_group * block_size) / sb_data.s_inode_size());

	    /* Die Anzahl der Inodes pro Gruppe sollte ein Vielfaches von 8 sein,
	       um den Zugriff auf die Bitmap einfacher zu gestalten */
	    sb_data.s_inodes_per_group(sb_data.s_inodes_per_group() & ~7);
	    inode_blocks_per_group = (((sb_data.s_inodes_per_group() * sb_data.s_inode_size()) +
				       block_size - 1) / block_size);

	    // Inodezahl aus der Anzahl der Inodegruppen berechnen
	    sb_data.s_inodes_count(sb_data.s_inodes_per_group() * group_desc_count);
	    sb_data.s_free_inodes_count(sb_data.s_inodes_count());

	    /* Overhead umfasst die Superblockkopien, die Kopien der Gruppenbezeichner,
	       die Inode- und Blockbitmap und die Inodetabelle */
	    overhead = 3 + desc_blocks + inode_blocks_per_group;
	    
	    /* Die letzte Gruppe muss gross genug ist, um die notwendigen
	       Datenstrukturen aufzunehmen (notfalls letzte Gruppe entfernen */
	    rem = (sb_data.s_blocks_count() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group();
	    if ((group_desc_count == 1) && (rem > 0) && (rem < overhead))
		throw new TooSmallException();
	    if ((rem > 0) && (rem < overhead + 50)) {
		sb_data.s_blocks_count(sb_data.s_blocks_count() - rem);
		continue;
	    }
	    break;
	}

	/*System.out*/Debug.out.println("  Anzahl Gruppen:     " + group_desc_count);
	/*System.out*/Debug.out.println("  Inodes pro Gruppe:  " + sb_data.s_inodes_per_group());
	/*System.out*/Debug.out.println("  Anzahl Inodes:      " + sb_data.s_inodes_count());

	sb_data.s_volume_name(name);

        /* Superblock und Gruppenbezeichner fuer jede Gruppe reservieren
	   und Zaehler der Gruppenbezeichner initialisieren */
	/*System.out*/Debug.out.print("erzeuge Gruppen "); // Debug.out.println
        group_block = sb_data.s_first_data_block();
        sb_data.s_free_blocks_count(0);

	blk = sb_data.s_first_data_block();
	off = block_size;
        for (int i = 0; i < group_desc_count; i++) {
	    /*System.out*/Debug.out.print("."); // debug
	    if (off+32 > block_size) {
		blk++;
		off = 0;
		bh = bufferCache.getblk(blk); //Fehler
	    }
	    desc = new GroupData(bh, off);

	    if (i == group_desc_count-1) { // die letzte Blockgruppe ist vielleicht etwas kleiner
		numblocks = (sb_data.s_blocks_count() - sb_data.s_first_data_block()) % sb_data.s_blocks_per_group();
		if (numblocks == 0)
		    numblocks = sb_data.s_blocks_per_group();
	    } else
		numblocks = sb_data.s_blocks_per_group();
	    numblocks -= (3 + desc_blocks + inode_blocks_per_group);

	    sb_data.s_free_blocks_count(sb_data.s_free_blocks_count() + numblocks);
	    desc.bg_free_blocks_count((short)numblocks);
            desc.bg_free_inodes_count((short)sb_data.s_inodes_per_group());
	    desc.bg_used_dirs_count((short)0);
	    // die folgenden drei Blocknummern sind absolute Werte:
	    desc.bg_block_bitmap(group_block + 1 + desc_blocks);
	    desc.bg_inode_bitmap(group_block + 1 + desc_blocks + 1);
	    desc.bg_inode_table(group_block + 1 + desc_blocks + 2);
	    Debug.out.println("  Gruppe " + i + " [block_bitmap: " + desc.bg_block_bitmap() + ", inode_bitmap: " + desc.bg_inode_bitmap() + ", inode_table: " + desc.bg_inode_table() + "]");
	    desc_vector.addElement(desc);

	    bitmap = new Bitmap(bufferCache, block_size, desc.bg_block_bitmap());
	    bitmap.loadBitmap();
	    bitmap.clearBitmap();
	    for (int j = 0; j < 3+desc_blocks+inode_blocks_per_group; j++)
		bitmap.setBit(j);
	    for (int j = numblocks + 3 + desc_blocks + inode_blocks_per_group; j < sb_data.s_blocks_per_group(); j++)
		bitmap.setBit(j); // padding

	    //Debug.out.println("belegte Bloecke: " + (3+desc_blocks+inode_blocks_per_group) + " " + inode_blocks_per_group);
	    //Debug.out.println(" erster freier Block: " + bitmap.findFirstZeroBit());
	    bitmap.markBitmapDirty();
	    bitmap.releaseBitmap();
	    bitmap = new Bitmap(bufferCache, block_size, desc.bg_inode_bitmap());
	    bitmap.loadBitmap();
	    bitmap.clearBitmap();
	    bitmap.markBitmapDirty();
	    bitmap.releaseBitmap();
	    group_block += sb_data.s_blocks_per_group();
	    off += 32;
        }
	/*System.out*/Debug.out.println(""); // debug

	/*System.out*/Debug.out.print("Initialisiere Inodes "); // debug
	// Inodetabellen initialisieren (auf 0 setzen)
	for (int i = 0; i < group_desc_count; i++) {
	    desc = (GroupData)desc_vector.elementAt(i);

	    /*System.out*/Debug.out.print(".");
	    blk = desc.bg_inode_table();
	    
	    /*
	    bh = bufferCache.newBufferHead(-1, block_size*8);  // 8 Bloecke auf einmal laden
	    for (int j = 0; j < inode_blocks_per_group; j += 8) {
		if (inode_blocks_per_group - j < 8)
		    bh = bufferCache.newBufferHead(-1, block_size*(inode_blocks_per_group - j));
		// BufferCache umgehen
		idedevice.memoryIO(bh.getData(), (blk+j)*(block_size/512), true , true); // READ
		bh.clear();
		idedevice.memoryIO(bh.getData(), (blk+j)*(block_size/512), false, true); // WRITE
	    }
	    */
	    
	    for (int j = 0; j < inode_blocks_per_group; j++) {
		bh = bufferCache.getblk(blk+j);
		bh.clear();
		bh.markDirty();
		bufferCache.brelse(bh);
	    }
	    
	}
	/*System.out*/Debug.out.println(""); // debug

	// Die Inodes bis sb_data.s_first_ino reservieren
	desc = (GroupData)desc_vector.firstElement();
	bitmap = new Bitmap(bufferCache, block_size, desc.bg_inode_bitmap());
	bitmap.loadBitmap();
	for (int i = 0; i < sb_data.s_first_ino()-1; i++)
	    bitmap.setBit(i);
	bitmap.markBitmapDirty();
	bitmap.releaseBitmap();

	desc.bg_free_inodes_count((short)(desc.bg_free_inodes_count() - sb_data.s_first_ino() + 1));
        sb_data.s_free_inodes_count(sb_data.s_free_inodes_count() - sb_data.s_first_ino() + 1);

        // Die Gruppenbezeichner- und Superblockkopien auf Platte schreiben
        group_block = sb_data.s_first_data_block() + sb_data.s_blocks_per_group();
	/*System.out*/Debug.out.println("Schreibe Kopien auf Platte ...");
        for (int i = 1; i < group_desc_count; i++) {
	    sgrp = i;
	    if (sgrp > ((1 << 16) - 1))
		sgrp = (1 << 16) - 1;
		
	    bh = bufferCache.getblk(group_block);
	    sb_data.s_block_group_nr((short)sgrp);
	    sb_data.bh.getData().copyToMemory(bh.getData(), 0, 0, 1024);
	    bh.markDirty();
	    bufferCache.brelse(bh);
	    
	    for (int j = 0; j < desc_blocks; j++) {
		bh = bufferCache.getblk(group_block+1+j);
		desc = (GroupData)desc_vector.elementAt(j*desc_per_block);
		desc.bh.getData().copyToMemory(bh.getData(), 0, 0, block_size);
		
		//Debug.out.println("  schreibe Blockgruppe " + i + " (block " + bh.b_block + ")");
		
		bh.markDirty();
		bufferCache.brelse(bh);
	    }

	    group_block += sb_data.s_blocks_per_group();
	}
    }

    /**
     * Das Dateisystem wird auf Konsistenz &uuml;berpr&uuml;ft und gegebenenfalls repariert. Es wird untersucht, ob die Block-
     * und Inodebitmaps korrekt initialisiert sind (Bl&ouml;cke, die Verwaltungsdaten enthalten, wie Gruppenbezeichner und
     * Kopien des Superblocks, m&uuml;ssen als belegt markiert sein), ob Bl&ouml;cke von mehr als einer Inode verwendet werden
     * (in diesem Fall werden die betroffenen Bl&ouml;cke dupliziert) und ob Verzeichnisse oder Dateien im Verzeichnisbaum
     * h&auml;ngen (wenn nicht, werden sie in dem Verzeichnis "<code>lost+found</code>" angelegt).
     *
     */
    public void checkFS(AnswerMachine oracle) {
	Vector desc_vector;
	Super fs;
	GroupData desc;
	int block, offset, rb, ino, count;
	InodeData i_data;
	Bitmap bitmap;
	SuperBlockData sb_data;
	BufferHead sb_bh;
	Hashtable block_to_inode_map;
	Vector duplicate_inodes, directories, duplicate_blocks;

	block_to_inode_map = new Hashtable();
	duplicate_blocks = new Vector();
	duplicate_inodes = new Vector();
	directories = new Vector();

	Debug.out.println("Pass 1: Checking inodes, blocks, and sizes");

        if ((sb_bh = bufferCache.bread(1)) == null) {
	    throw new Error("unable to read superblock");
        }
	sb_data = new SuperBlockData(sb_bh);

	block_size = 1024;
	for (int i = 0; i < sb_data.s_log_block_size(); i++)
	    block_size *= 2;

	BufferHead bh = bufferCache.bread(sb_data.s_first_data_block()+1);

	/* FIXME: is this code necessary?
	i_data = new InodeData(bh, sb_data.s_inode_size());  // RootInode wird fuer Super-Objekt benoetigt
	if ((i_data.i_mode() & InodeImpl.S_IFMT) != InodeImpl.S_IFDIR)
	    if (oracle.ask("Wrong mode. Change?")) i_data.i_mode((short)(InodeImpl.S_IFDIR | (i_data.i_mode() & InodeImpl.S_IRWXUGO)));

	
	if (i_data.i_size() < block_size) {
	    if (oracle.ask("Size too small. Increase to "+block_size+"?")) i_data.i_size(block_size);
	}
	

	if (i_data.i_links_count() < 2)
	    if (oracle.ask("Links count < 2. Increase to 2?")) i_data.i_links_count((short)2);
	*/


	fs = new Super(null, false, bufferCache, inodeCache, clock);
	desc_vector = new Vector(fs.s_groups_count);

	int s_desc_per_block = fs.s_blocksize / 32;
	int s_db_per_group = (fs.s_groups_count + s_desc_per_block - 1) / s_desc_per_block;

	for (int i = 0; i < s_db_per_group; i++) {
	    bh = bufferCache.bread(fs.s_first_data_block+i+1);
	    for (int j = 0; j < s_desc_per_block && i*s_desc_per_block+j < fs.s_groups_count; j++) {
		desc = new GroupData(bh, j*32);
		desc_vector.addElement(desc);
	    }
	}
	
	for (int i = 0; i < fs.s_groups_count; i++) {
	    desc = (GroupData)desc_vector.elementAt(i);
	    bitmap = new Bitmap(bufferCache, fs.s_blocksize, desc.bg_inode_bitmap());
	    try {
		bitmap.loadBitmap();
	    } catch (BufferIOException e) {
		return;
	    }

	    bh = null;
	    for (int j = 0; j < sb_data.s_inodes_per_group(); j++) {
		if (bitmap.testBit(j) == 1) {
		    offset = j * sb_data.s_inode_size();
		    block = desc.bg_inode_table() + (offset / fs.s_blocksize);
		    offset &= fs.s_blocksize - 1;
		    if (offset == 0) {
			if (bh != null)
			    bufferCache.brelse(bh);
			// dieser Fehler existiert vielleicht in anderen Klasse auch (brelse u.U. nicht aufgerufen)
			/* UEBERPRUEFEN */
			bh = bufferCache.bread(block);
		    }
		    i_data = new InodeData(bh, offset);
		    ino = j + i * sb_data.s_inodes_per_group() + 1;
		    if (debugFSCK) Debug.out.println("Inode "+ ino+ ": i_blocks="+i_data.i_blocks());
		    int mode = i_data.i_mode() & InodeImpl.S_IFMT;
		    if (((mode & InodeImpl.S_IFDIR) > 0) && ((mode & (InodeImpl.S_IFREG|InodeImpl.S_IFLNK)) > 0)) {
			/*System.out*/Debug.out.println("Typ der Inode " + ino + " nicht eindeutig, setze Verzeichnis");
			i_data.i_mode((short)((i_data.i_mode() & InodeImpl.S_IRWXUGO) | InodeImpl.S_IFDIR));
			i_data.bh.markDirty();
		    }
		    if ((i_data.i_mode() & InodeImpl.S_IFMT) == (InodeImpl.S_IFREG|InodeImpl.S_IFLNK)) {
			/*System.out*/Debug.out.println("Typ der Inode " + ino + " nicht eindeutig, setze Datei");
			i_data.i_mode((short)((i_data.i_mode() & InodeImpl.S_IRWXUGO) | InodeImpl.S_IFREG));
			i_data.bh.markDirty();
		    }
		    if ((i_data.i_mode() & InodeImpl.S_IFMT) == (InodeImpl.S_IFREG)) {
		      int expectedBlocks = computeIBlocks(i_data);
		      if (i_data.i_blocks() != expectedBlocks) {
			if (oracle.ask("i_blocks was="+i_data.i_blocks()+", expected="+expectedBlocks+". Correct?")) {
			    i_data.i_blocks(expectedBlocks);
			    i_data.bh.markDirty();
			}
		      }
		    }
		    // TODO: check i_blocks also for other inode types
		    if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFDIR)
			directories.addElement(new Integer(ino));

		    /*
		    int nr_blocks = i_data.i_blocks()*512 / fs.s_blocksize;
		    if (nr_blocks <= 12) {
		      // TODO: check also larger files that contain indirect blocks
		    for (int ib = 0; ib < nr_blocks; ib++) {
			rb = bmapReplace(i_data, ib, fs.s_blocksize, 0);
			if (rb == 0) {
			    Debug.out.println("Wrong i_size (" + ib + ", " + nr_blocks  + "): " + i_data.i_size());
			    i_data.i_blocks(ib * (fs.s_blocksize/512));
			    i_data.i_size(ib*fs.s_blocksize);
			    i_data.bh.markDirty();
			    Debug.out.println("Setze Groesse der Inode " + ino + ", Block " + block + " auf " + i_data.i_size());
			    break;
			}
			Integer rbkey = new Integer(rb);
			if (block_to_inode_map.containsKey(rbkey)) {
			    if (duplicate_blocks.contains(rbkey) == false) {
				duplicate_inodes.addElement(new Integer(ino));
				duplicate_blocks.addElement(rbkey);
			    }
			}
			else
			    block_to_inode_map.put(new Integer(rb), new Integer(ino));			    
		    }
		    }
		    //if (offset == 0)
		    //bufferCache.brelse(bh);
		    */
		    // count blocks that are referenced by this inode
		    int counted = 0;
		    int nrininode = 0;
		    for(int ib=0; ib<12; ib++) {
		      if (i_data.i_block(ib) == 0) break;
		      counted++;
		      nrininode++;
		    }
		    int indnr = i_data.i_block(12);
		    if (counted == 12 && indnr != 0) {
		      // read indirect block list
			counted++;  // indirect block list
			Debug.out.println("block "+indnr +", contains first indirect block list");
		      BufferHead bhi = bufferCache.bread(indnr);
		      BufferHeadAccess bhai = new BufferHeadAccess();
		      bhai.init(bhi, 0);
		      for(int ii=0; ii<1024/4; ii++) {
			  int blocknr = bhai.readInt(ii*4);
			  if (blocknr == 0) break;
			  Debug.out.println("block "+blocknr +", #"+nrininode);
			  counted++;
			  nrininode++;
		      }
		      bufferCache.brelse(bhi);
		    }
		    if (debugFSCK) Debug.out.println("Counted blocks: "+counted);
		}
	    }
	    if (bh != null)
		bufferCache.brelse(bh);
	    bitmap.releaseBitmap();
	}

	try {
	    fs.checkBlockBitmaps(block_to_inode_map);
	    fs.checkInodeBitmaps();
	} catch (BufferIOException e) {
	    return;
	}
	Debug.out.println("Pass 2: Checking directory structure");
	repairDirectories(directories, 2, 2, fs);
	Debug.out.println("Pass 3: Checking directory connectivity");
	linkLostDirectories(directories, fs);

	if (duplicate_blocks.size() > 0) {
	    count = duplicate_blocks.size();
	    for (int i = 0; i < count; i++) { // doppelten Block kopieren
		block = ((Integer)duplicate_blocks.firstElement()).intValue();
		int inodenr = ((Integer)duplicate_inodes.firstElement()).intValue();
		i_data = fs.getInodeData(inodenr);
		int newblock = 0;
		try {
		    newblock = fs.newBlock(block);
		} catch(BufferIOException e) {
		    return;
		}
		BufferHead bhold = bufferCache.bread(block);
		BufferHead bhnew = bufferCache.getblk(block);
		bhold.getData().copyToMemory(bhnew.getData(), 0, 0, fs.s_blocksize);
		bhnew.markDirty();
		bufferCache.brelse(bhold);
		bufferCache.brelse(bhnew);
		bmapReplace(i_data, block, fs.s_blocksize, newblock);
		duplicate_blocks.removeElementAt(0);
		duplicate_inodes.removeElementAt(0);
	    }
	}

	fs.writeSuper();
	fs.putSuper();
	inodeCache.syncInodes();
	bufferCache.syncDevice(false);
	inodeCache.syncInodes();
	bufferCache.syncDevice(true); //wait
	inodeCache.invalidateInodes();
	bufferCache.flushCache();
    }

  private int computeIBlocks(InodeData i_data) {
    // IGNORE HOLES (TODO)
    int nblocks = (i_data.i_size() + 1023) / 1024;
    if (nblocks < 12) return nblocks*2; // 512 byte blocks
    nblocks -= 12;
    if (nblocks < 1024 / 4) return (12 + nblocks)*2;
    nblocks -= 1024 / 4; 
    if (nblocks < 1024 / 4) return (12 + 1024 / 4 + 2 + nblocks)*2;
    throw new Error();
  }

    private void repairDirectories(Vector directories, int current, int parent, Super fs) {
	InodeData i_data;
	DirEntryData de_data, de_data1;
	BufferHead bh;
	DirInode inode;
	String remove;
	int blk, offset, pos, inode_to_remove;

	directories.removeElement(new Integer(current));
	i_data = fs.getInodeData(current);
	inode = new DirInode(fs.fileSystem, fs, current, i_data, bufferCache, inodeCache, clock);
	if (i_data.i_block(0) == 0) {
	    int goal = fs.groupStart(current);
	    int block = 0;
	    try {
		block = fs.newBlock(goal);
	    } catch (BufferIOException e) {
		return;
	    }
	    i_data.i_block(0, block);
	    i_data.i_size(fs.s_blocksize);
	    i_data.i_blocks(fs.s_blocksize/512);
	}

	try {
	    de_data = inode.findDirEntry(".");
	    de_data1 = inode.findDirEntry("..");
	} catch (InodeIOException e) {
	    return;
	}
	if ((de_data == null) || (de_data1 == null)) {
	    try {
		if (de_data != null)
		    inode.deleteDirEntry(de_data);
		if (de_data1 != null)
		    inode.deleteDirEntry(de_data1);
	    } catch (InodeNotFoundException e) { }
	    bh = inode.bread(0, false);
	    offset = 0;
	    inode_to_remove = 0;
	    while (offset < fs.s_blocksize && inode_to_remove == 0) {
		de_data = new DirEntryData(bh, offset);
		remove = de_data.name();
		inode_to_remove = de_data.inode();
		offset += de_data.rec_len();
	    }
	    try {
		if (inode_to_remove != 0)
		    inode.deleteDirEntry(de_data);
	    } catch(InodeNotFoundException e) { }
	    inode.addDirEntry(".", parent);
	    inode.addDirEntry("..", parent);
	}

	pos = 0; offset = 0;
	while (pos < i_data.i_size()) {
	    blk = pos / fs.s_blocksize;
	    bh = inode.bread(blk, false);
	    
	    de_data = new DirEntryData();
	    while ((offset < i_data.i_size()) && (offset < fs.s_blocksize)) {
		de_data.init(bh, offset);
		if (directories.contains(new Integer(de_data.inode())))
		    repairDirectories(directories, de_data.inode(), current, fs);
		offset += de_data.rec_len();
		pos += de_data.rec_len();
	    }
	    offset = 0;
	    bufferCache.brelse(bh);
        }	
    }

    private void linkLostDirectories(Vector directories, Super fs) {
	DirInode lost_and_found;
	DirEntryData de_data;
	InodeImpl inode;

	if (directories.size() == 0)
	    return;

	InodeData i_data = fs.getInodeData(2);
	DirInode rootinode = new DirInode(fs.fileSystem, fs, 2, i_data, bufferCache, inodeCache, clock);
	try {
	    lost_and_found = (DirInode)rootinode.getInode("lost+found");
	} catch (InodeNotFoundException e) {
	    try {
		lost_and_found = (DirInode)rootinode.mkdir("lost+found", 0);
	    } catch (FSException e1) {
		return;
	    }
	} catch (FSException e) {
	    return;
	}
	
	for (int i = 0; i < directories.size(); i++) {
	    String name = "lostfile"+i;
	    inode = new FileInode(fs.fileSystem, fs, ((Integer)directories.elementAt(i)).intValue(), null, bufferCache, inodeCache, clock);
	    try {
		de_data = lost_and_found.findDirEntry(name);
		if (de_data != null)
		    lost_and_found.deleteDirEntry(de_data);
		lost_and_found.addDirEntry(name, inode.i_ino);
	    } catch(FSException e) { }
	}

	lost_and_found.setDirty(true);
	lost_and_found.decUseCount();
    }

    private int blockBmap(BufferHead bh, int nr, int new_block) {
	int tmp;
	InodeBlockData bd_slot;

        if (bh == null)
	    return 0;
	bd_slot = new InodeBlockData(bh, nr*4);
	if (new_block != 0)
	    bd_slot.bd_entry(new_block);
	tmp = bd_slot.bd_entry();
        bufferCache.brelse(bh);
        return tmp;
    }

    private int inodeBmap(InodeData i_data, int nr, int new_block) {
	if (new_block != 0)
	    i_data.i_block(nr, new_block);
	return i_data.i_block(nr);
    }

    private int bmapReplace(InodeData i_data, int block, int s_blocksize, int new_block) {
	BufferHead bh;
        int i;
	int addr_per_block = s_blocksize / 4; // Laenge eines Int
	int addr_per_block_bits = 0;

	int addr = addr_per_block;
	while (addr != 1 && addr > 0) {
	    addr = addr >> 1;
	    addr_per_block_bits++;
	}

        if (block < 0) {
	    Debug.out.println("Inode.bmap(): block < 0");
	    return 0;
        }
        if (block > 12 + addr_per_block + (1 << (addr_per_block_bits * 2)) +
                ((1 << (addr_per_block_bits * 2)) << addr_per_block_bits)) {
	    Debug.out.println("Inode.bmap(): block > big");
	    return 0;
        }

        if (block < 12)
	    return inodeBmap(i_data, block, new_block);
        block -= 12;

        if (block < addr_per_block) {
	    i = inodeBmap(i_data, 12, new_block);
	    if (i <= 0)
		return 0;
	    bh = bufferCache.bread(i);
	    return blockBmap(bh, block, new_block);
        }
        block -= addr_per_block;
        if (block < (1 << (addr_per_block_bits * 2))) {
	    i = inodeBmap(i_data, 13, new_block);
	    if (i <= 0)
		return 0;
	    bh = bufferCache.bread(i);
	    i  = blockBmap(bh, block >> addr_per_block_bits, new_block);
	    if (i <= 0)
		return 0;
	    bh = bufferCache.bread(i);
	    return blockBmap(bh, block & (addr_per_block -1), new_block);
        }
        block -= (1 << (addr_per_block_bits * 2));

	i = inodeBmap(i_data, 14, new_block);
	if (i <= 0)
	    return 0;
	bh = bufferCache.bread(i);
	i  = blockBmap(bh, block >> (addr_per_block_bits * 2), new_block);
	if (i <= 0)
	    return 0;
	bh = bufferCache.bread(i);
	i  = blockBmap(bh, (block >> addr_per_block_bits) & (addr_per_block - 1), new_block);
	if (i <= 0)
	    return 0;
	bh = bufferCache.bread(i);
	return blockBmap(bh, block & (addr_per_block - 1), new_block);
    }
}
