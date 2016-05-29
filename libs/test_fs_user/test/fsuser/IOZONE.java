package test.fsuser;


import jx.fs.*;
import jx.zero.*;
import java.io.IOException;
import jx.zero.memory.*;
import jx.formats.*;

/**
 * iozone filesystem benchmark
 * ported from C
 *
 * @author Michael Golm
 * @author Andreas Weissel
 */

public class IOZONE {
    private static final int LARGE_REC       = 65536;
    private static final int KILOBYTES       = 512;
    private static final int RECLEN          = 1024;
    private static final int FILESIZE        = KILOBYTES*1024;
    private static final int NUMRECS         = FILESIZE/RECLEN;

    private static final int MAXBUFFERSIZE   = 1*1024*1024;//16*1024*1024;//1*1024*1024; // 8*1024*1024
    private static final int MINBUFFERSIZE   = 128;

    private static final int MAX_X           = 100;
    private static final int MAX_Y           = 200;

    private  int KILOBYTES_START = 4 * 1024; // 1024
    private  int KILOBYTES_END   = 4096; //6*1024*1024; //256; // 1024*64;
    private  int RECLEN_START    = 16 * 1024; //128*1024; //16*4096;
    private  int RECLEN_END      = MAXBUFFERSIZE;
    private static final int MULTIPLIER      = 2;
    /** tells when to switch to large records */
    private static final int CROSSOVER       = 8*1024; 

    private static final int MINIMAL_MICROS  = 5;

    private Clock clock = null;

    private int current_x, current_y, max_x, max_y;
    //private long report_array[]; 
    private int report_array[];
    private boolean cancel_store=true;
    private Memory buffer;

    String filename;
    private RegularFile file = null;
    jx.zero.debug.DebugPrintStream out;

    public final static int IOZONE_MAX_FILESIZE = 4096; /* in kBytes */

    final boolean doDumpBinary = true;

    int rereadtime[];
    int nreread;


    public static void main(String[] args) throws Exception {
	IOZONE iozone = new IOZONE(args[0], 4, IOZONE_MAX_FILESIZE, 4*1024, 16*1024*1024);
    }

    /**
     * iozone filesystem performance benchmark.
     *
     * File size starts with minFileSizeKB and is doubled each time
     * until it reaches maxFileSizeKB.
     *
     * Record size starts with minRecSizeB and is doubled each time
     * until it reaches maxRecSizeB.
     *
     * @param minFileSizeKB start file size in kilobytes
     * @param maxFileSizeKB end file size in kilobytes
     * @param minRecSizeB start record size in bytes
     * @param maxRecSizeB end record size in bytes
     */
    public IOZONE(String filename, int minFileSizeKB, int maxFileSizeKB, int minRecSizeB, int maxRecSizeB) throws Exception {
	out = Debug.out;
	this.filename = filename;

	//maxFileSizeKB = IOZONE_MAX_FILESIZE;
	maxFileSizeKB = 512;

	KILOBYTES_START = minFileSizeKB;
	KILOBYTES_END = maxFileSizeKB;
	RECLEN_START = minRecSizeB;
	RECLEN_END = maxRecSizeB;

	if (doDumpBinary) {
	    int j=0;
	    for(int kilo=KILOBYTES_START; kilo<=KILOBYTES_END; kilo *= 2) {
		for(int reclen=RECLEN_START; reclen <= RECLEN_END && reclen <= kilo*1024; reclen *= 2) {
		    j++;
		}
	    }
	    rereadtime = new int[j];
	}

	
	out.println("IOZONE yield!");
	((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).yield();
	out.println("IOZone started Buffersize = "+MAXBUFFERSIZE);

	clock =  (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	report_array = new int[MAX_X*MAX_Y];
	MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memMgr.allocAligned(MAXBUFFERSIZE, 4);
	max_x = 0; max_y = 0;

	//out.println(" read/write throughput in kbytes/sec");
	//out.println("  file(kB)  rec(kB)   write rewrite    read  reread");
	out.println(" read/write throughput in microsec");
	out.println("  file(kB)  rec(kB)   write rewrite    read  reread");

	file = fileopen(filename);
	for (int i = 0; i < 50; i++) {
	    if (doDumpBinary) nreread = 0;
	    out.println("# START IOZONE");
	    out.println("microsec");
	    autoTest();
	    ((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).yield();
	    if (doDumpBinary) {
		try {
		    dumpBinary("IOZONE"+i);
		} catch(IOException ex) {throw new Error("BINARY DUMP");}
	    }
	}
	out.println("# END IOZONE");

	//file.close();

	//dumpExcel();
    }


    private void autoTest() throws Exception {
	int kilosi, recszi, count1;
	int min_file_size = KILOBYTES_START;
        int max_file_size = KILOBYTES_END;
        int min_rec_size = RECLEN_START;
        int max_rec_size = RECLEN_END;
	int xover = CROSSOVER;
	int mult, save_y = 0;
	int xx;

	Profiler profiler = ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
	if (profiler != null) profiler.startSampling();

	/****************************************************************/
	/* Start with file size of min_file_size and repeat the test 	*/
	/* KILOBYTES_ITER_LIMIT  					*/
	/* times.  Each time we run, the file size is doubled		*/
	/****************************************************************/

	kilosi= min_file_size;
	//out.println("enter autoTest (kilosi "+kilosi+" max_file_size "+max_file_size+")");

        for (kilosi = min_file_size; kilosi <= max_file_size; kilosi *= MULTIPLIER) {
	    /***************************************************************/
	    /* Start with record size of min_rec_size bytes and repeat the */
	    /* test, multiplying the record size by MULTIPLIER each time,  */
	    /* until we reach max_rec_size. At the CROSSOVER we stop doing */
	    /* small buffers as it takes forever and becomes very 	   */
	    /* un-interesting.						   */
	    /***************************************************************/
	    if (kilosi > xover) {
		min_rec_size = LARGE_REC;
		mult = RECLEN_START/1024;
		/*********************************/
		/* Generate dummy entries in the */
		/* Excel buffer for skipped      */
		/* record sizes			 */
		/*********************************/
		for (count1 = min_rec_size; (count1 != RECLEN_START) && (mult <= (kilosi*1024)); count1 = (count1>>1)) {
		    current_x = 0;
		    storeValue((int)kilosi);
		    storeValue(mult);
		    for (xx = 0; xx < 20; xx++)
			storeValue(0);
		    mult = mult*2;
		    current_y++;
		    if (current_y > max_y)
			max_y = current_y;
		    current_x = 0;
		}
	    }

	    
	    for (recszi = min_rec_size; recszi <= max_rec_size; recszi *= MULTIPLIER) {
		if (recszi > (kilosi*1024)) break;
		begin(kilosi, recszi);
		current_x = 0;
		current_y++;
	    }
	}

	//out.println("leave autoTest");
    }

    private void begin(int kilobytes, int reclen) throws Exception {
	storeValue((int)(kilobytes));
	storeValue((int)(reclen/1024));

	out.print("" + alignString(Integer.toString((int)kilobytes), 8) + alignString(Integer.toString((int)(reclen/1024)), 8)+ "   ");
	
	writePerfTest(kilobytes, reclen);
	readPerfTest(kilobytes, reclen);

	out.println("");
    }

    private void readPerfTest(int kilo, int reclen) { 
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	int readtime[] = new int[2];
	int numrecs; 
	int readrate[] = new int[2]; 
	int filebytes; 
	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;
	try {

	    //    RegularFile file = fileopen(filename);

	    for (int j = 0; j < 2; j++) {
		int filePos = 0;

		clock.getCycles(starttimec);

		for (int i = 0; i < numrecs; i++) {
		    int ret=file.read(filePos, buffer, i*reclen, reclen);
		    if (ret < 0) throw new Error();
		    filePos += reclen;
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);

		readtime[j] = clock.toMicroSec(diff);
		if (readtime[j] < MINIMAL_MICROS) {
		    readtime[j] = -1;
		}
		if (doDumpBinary) {
		    if (j==1) {
			//Debug.out.println("WRITEREAREAD: "+nreread+"="+readtime[j]);
			rereadtime[nreread++] = readtime[j];
		    }
		}
	    }
	    
	    //	    file.close();
	    
	    for (int j = 0; j < 2; j++) {
		if (readtime[j] < 3) {
		    readrate[j] = -1;
		} else {
		    readrate[j] = ((filebytes*128)/readtime[j])*8; //overflow prevention
		}
	    }
	    
	    out.print("" + alignString(Integer.toString((int)readtime[0]), 11) + alignString(Integer.toString((int)readtime[1]), 11));
	    storeValue((int)readrate[0]);
	    storeValue((int)readrate[1]);

	} catch(Exception e) {
	    out.println("EXCEPTION!"); throw new Error();
	}
    }

    private void writePerfTest(int kilo, int reclen) { // long, long

	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	int writetime[] = new int[2]; //long
	int numrecs; // long
	int writerate[] = new int[2]; // long
	int filebytes; // long

	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;

	try {
	    //	    RegularFile file = fileopen(filename);

	    for (int j = 0; j < 2; j++) {
		int filePos = 0;
		clock.getCycles(starttimec);
		for (int i = 0; i < numrecs; i++) {
		    int ret = file.write(filePos, buffer, (int)(i*reclen), (int)reclen);
		    if (ret==-1) throw new Error();
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		
		writetime[j] = clock.toMicroSec(diff);
		if (writetime[j] < MINIMAL_MICROS) {
		    writetime[j] = -1;
		}
	    }
	
	    // file.close();

	    for (int j = 0; j < 2; j++) {
		if (writetime[j] != -1) {
		    writerate[j] = ((filebytes*128)/writetime[j])*8; //overflow prevention
		    //out.println("WRITERATE "+writerate[j]);
		} else {
		    writerate[j] = -1;
		}
	    }

	    out.print("" + alignString(Integer.toString((int)writetime[0]), 11) + alignString(Integer.toString((int)writetime[1]), 11));
	    storeValue((int)writerate[0]);
	    storeValue((int)writerate[1]);
	} catch(Exception ex) {
	    out.println(ex);
	    throw new Error();
	}
    }

    private void storeValue(int value) {
	if (cancel_store) return;
	try {
	    report_array[current_y*MAX_X + current_x] = value;
	    current_x++;
	    if (current_x > max_x)
		max_x = current_x;
	    if (current_y > max_y)
		max_y = current_y;
	    if (max_x >= MAX_X)
		out.println("MAX_X too small");
	    if (max_y >= MAX_Y)
		out.println("MAX_X too small");
	} catch (Exception ex) {
	    Debug.out.println("\n!!! Exception in storeValue caught !!!");
	    cancel_store = true;
	}
    }

    private void dumpBinary(String name) throws IOException {
	MemoryManager memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	Memory mem = memoryManager.alloc(4096);
	MemoryOutputStream mout = new MemoryOutputStream(mem);
	LittleEndianOutputStream out = new LittleEndianOutputStream(mout); 
	out.writeInt(KILOBYTES_START);
	out.writeInt(KILOBYTES_END);
	out.writeInt(RECLEN_START);
	out.writeInt(RECLEN_END);
	int j=0;
	for(int kilo=KILOBYTES_START; kilo<=KILOBYTES_END; kilo *= 2) {
	    for(int reclen=RECLEN_START; reclen <= RECLEN_END && reclen <= kilo*1024; reclen *= 2) {
		out.writeInt(rereadtime[j++]);
	    }
	}
	if (j != nreread) throw new Error("???");
	DebugSupport deb = (DebugSupport) InitialNaming.getInitialNaming().lookup("DebugSupport");
	deb.sendBinary(name, mem, 4*(4+nreread));
    }


    /********************************************************************/
    /* dump_report()							*/
    /* Dumps the Excel report on standard output.			*/
    /********************************************************************/
    private void dumpReport(int who) {
	int i, j;
        long current_file_size;

	out.print("      ");
	for (i = RECLEN_START; i <= (RECLEN_END/MULTIPLIER); i = i*MULTIPLIER) {
	    out.print("\"" + (i/1024) + "\" ");
	}
	out.println("\"" + (RECLEN_END/1024) + "\"");
	    
	current_file_size = report_array[0]; // [0][0];
	out.print("\"" + current_file_size + "\" ");
	    
	for (i = -1; i < max_y; i++){
	    if (report_array[(i+1)*MAX_X] != current_file_size) {
		out.println();
		current_file_size = report_array[(i+1)*MAX_X]; // report_array[0][i+1]
		out.print("\"" + current_file_size + "\" ");
	    }
	    out.print(" " + report_array[(i+1)*MAX_X+who] + " ");
	}
	out.println();
    }

    /********************************************************************/
    /* Wrapper that dumps each of the collected data sets.	       	*/
    /********************************************************************/
    private void dumpExcel() {
	out.println();
	out.println("\"Writer report\"");
	dumpReport(2);
	out.println();
	out.println("\"Re-writer report\"");
	dumpReport(3); 
	out.println();
	out.println("\"Reader report\"");
	dumpReport(4);
	out.println();
	out.println("\"Re-Reader report\"");
	dumpReport(5);
	out.println();
    }

    private String alignString(String value, int length) {
	String tmp1 = new String();
	int leer = length - value.length();
	for (int i = 0; i < leer; i++)
	    tmp1 += " ";
	return (tmp1 + value);
    }

    private long getTime() {
	return System.currentTimeMillis();
    }


    RegularFile fileopen(String path) {
	RegularFile fsobj=null;
	try {
	    ExtendedFileSystemInterface fs = ExtendedFileSystemInterface.getExtFileSystemInterface("FSInterface");
	    fsobj = (RegularFile)fs.openRW(path);
	    if (fsobj==null) {fsobj = (RegularFile)fs.create(path);}
	} catch (Exception ex) {}
	return fsobj;
    }
    
}
