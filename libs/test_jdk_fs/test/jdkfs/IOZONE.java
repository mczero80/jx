package test.jdkfs;

import java.io.*;

import jx.zero.Clock;
import jx.zero.CycleTime;
import jx.zero.InitialNaming;
import jx.zero.CPUManager;
import jx.zero.Profiler;

/**
 * iozone filesystem benchmark
 * ported from C
 *
 * @author Michael Golm
 * @author Andreas Weissel
 */

public class IOZONE {

    private static final boolean sample = true;
    private Profiler profiler;

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
    private byte[] buffer;

    private PrintStream out;
    String filename;

    public final static int IOZONE_MAX_FILESIZE = 4096; /* in kBytes */

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
	out = System.out;
	this.filename = filename;

	//maxFileSizeKB = IOZONE_MAX_FILESIZE;
	maxFileSizeKB = 512;

	KILOBYTES_START = minFileSizeKB;
	KILOBYTES_END = maxFileSizeKB;
	RECLEN_START = minRecSizeB;
	RECLEN_END = maxRecSizeB;

	
	out.println("IOZONE yield!");
	((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).yield();
	out.println("IOZone started Buffersize = "+MAXBUFFERSIZE);

	profiler = ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));

	clock =  (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	report_array = new int[MAX_X*MAX_Y];
	buffer = new byte[MAXBUFFERSIZE];
	max_x = 0; max_y = 0;

	while (true) {
	    out.println(" read/write throughput in kbytes/sec");
	    out.println(" read/write times in microseconds");   
	    out.println("  file(kB)  rec(kB)   write rewrite    read  reread");
	    autoTest();
	    out.println("");
	    ((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).yield();
	    out.println("let do it again ;-)\n");
	}
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
	    if (false) {
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
	//storeValue((int)(kilobytes));
	//storeValue((int)(reclen/1024));

	out.print("" + alignString(Integer.toString((int)kilobytes), 8) + alignString(Integer.toString((int)(reclen/1024)), 8)+ "   ");
	
	writePerfTest(kilobytes, reclen);
	readPerfTest(kilobytes, reclen);

	out.println("");
    }

    /*
    private void readPerfTest(int kilo, int reclen) throws Exception { 
	long starttime;
	long endtime;
	long readtime[] = new long[2]; 
	int starttime;
	int enttime;
	int readtime[] = new int[2];
	int numrecs; 
	double readrate[] = new double[2]; 
	int filebytes; 
	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;

	RandomAccessFile file = new RandomAccessFile(filename, "r");

	for (int j = 0; j < 2; j++) {
	    file.seek(0);
	    starttime = getTime();
	    for (int i = 0; i < numrecs; i++) {
		file.read(buffer, (int)(i*reclen), (int)reclen);
	    }
	    endtime = getTime();
	    readtime[j] =  (endtime - starttime)*1000;
	    if (readtime[j] == 0 && j == 1) { // reread time too short
		out.print("*");
		starttime = getTime();
		for (int k = 0; k < 1000; k++) {
		    file.seek(0);
		    for (int i = 0; i < numrecs; i++) {
			file.read(buffer, (int)(i*reclen), (int)reclen);
		    }
		}
		endtime = getTime();
		readtime[j] =  (endtime - starttime);
	    }
	}
	
	file.close();
	    
	for (int j = 0; j < 2; j++) {
	    if (readtime[j] < 3) {
		readrate[j] = -1;
	    } else {
		readrate[j] = ((double)filebytes*1024)/(double)readtime[j];
	    }
	}
	    
	out.print("" + alignString(Integer.toString((int)readrate[0]), 8) + alignString(Integer.toString((int)readrate[1]), 8));
	storeValue((long)readrate[0]);
	storeValue((long)readrate[1]);
    }
    */

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
	// ASSUME 500 MHz time stamp counter
	try {

	    RandomAccessFile file = new RandomAccessFile(filename, "r");

	    if (sample) profiler.startSampling();

	    for (int j = 0; j < 2; j++) {
		file.seek(0);

		clock.getCycles(starttimec);

		for (int i = 0; i < numrecs; i++) {
		    file.read(buffer, (int)(i*reclen), (int)reclen);
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);

		readtime[j] = clock.toMicroSec(diff);
		if (readtime[j] < MINIMAL_MICROS) {
		    readtime[j] = -1;
		}
	    }

	    if (sample) profiler.stopSampling();
	    
	    file.close();
	    
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

    /*
    private void writePerfTest(int kilo, int reclen) throws Exception { 
	long starttime;
	long endtime; 
	long writetime[] = new long[2];
	int numrecs; 
	long writerate[] = new long[2]; 
	int filebytes;

	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;

	RandomAccessFile file = new RandomAccessFile(filename, "rw");
	
	for (int j = 0; j < 2; j++) {
	    file.seek(0);
	    starttime = getTime();
	    //out.println("START "+starttime);
	    for (int i = 0; i < numrecs; i++) {
		file.write(buffer, (int)(i*reclen), (int)reclen);
	    }
	    endtime = getTime();
	    writetime[j] = (endtime - starttime)*1000;
	    if (writetime[j] < MINIMAL_MICROS) {
		writetime[j] = -1;
	    }
	}
	
	file.close();
	
	for (int j = 0; j < 2; j++) {
	    if (writetime[j] != -1) {
		writerate[j] = ((filebytes*128)/writetime[j])*8; //overflow prevention
		//out.println("WRITERATE "+writerate[j]);
	    } else {
		writerate[j] = -1;
	    }
	}
	out.print("" + alignString(Integer.toString((int)writerate[0]), 8) + alignString(Integer.toString((int)writerate[1]), 8));
	storeValue(writerate[0]);
	storeValue(writerate[1]);
    }
    */

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

	    RandomAccessFile file = new RandomAccessFile(filename, "rw");

	    if (sample) profiler.startSampling();
	
	    for (int j = 0; j < 2; j++) {
		file.seek(0);
		clock.getCycles(starttimec);
		for (int i = 0; i < numrecs; i++) {
		    file.write(buffer, (int)(i*reclen), (int)reclen);
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		
		writetime[j] = clock.toMicroSec(diff);
		if (writetime[j] < MINIMAL_MICROS) {
		    writetime[j] = -1;
		}
	    }

	    if (sample) profiler.stopSampling();
	
	    file.close();

	    for (int j = 0; j < 2; j++) {
		if (writetime[j] != -1) {
		    writerate[j] = ((filebytes*128)/writetime[j])*8; //overflow prevention
		    //out.println("WRITERATE "+writerate[j]);
		} else {
		    writerate[j] = -1;
		}
	    }

	    out.print("" + alignString(Integer.toString((int)writetime[0]), 11) + alignString(Integer.toString((int)writetime[1]), 11));
	    //storeValue((int)writerate[0]);
	    //storeValue((int)writerate[1]);
	} catch(Exception ex) {
	    out.println(ex);
	    throw new Error();
	}
    }

    /*
      private void storeValue(long value) {
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
    }
    */

    private void storeValue(int value) {	
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
}
