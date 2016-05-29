package jx.iozone;

import java.io.IOException;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.fs.*;
import jx.fs.FSException;
import jx.zero.*;
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
    private FS fs;

    private static final int MAX_X           = 100;
    private static final int MAX_Y           = 200;

    private  int KILOBYTES_START;
    private  int KILOBYTES_END;
    private  int RECLEN_START;
    private  int RECLEN_END;

    private static final int MULTIPLIER      = 2;
    /** tells when to switch to large records */
    private static final int CROSSOVER       = 8*1024; 
    private static final int LARGE_REC       = 65536;

    private static final int MINIMAL_MICROS  = 5;

    private int current_x, current_y, max_x, max_y;
    private int report_array[]; 
    private boolean cancel_store=true;
    private Memory buffer;

    Profiler profiler;
    static final boolean sample = false;
    CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
    CycleTime starttimec = new CycleTime();
    CycleTime endtimec = new CycleTime();
    CycleTime diff = new CycleTime();

    final boolean outputThroughput = true;
    final static boolean output = true;
    final boolean doDumpBinary = true;
    
    int rereadtime[];
    int nreread;

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
    public IOZONE(FS fs, int minFileSizeKB, int maxFileSizeKB, int minRecSizeB, int maxRecSizeB) {
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

	profiler = ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
	report_array = new int[MAX_X*MAX_Y]; // new int[MAX_X][MAX_Y]
	MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memMgr.allocAligned(maxFileSizeKB * 1024, 32);
	max_x = 0; max_y = 0;
	try {
	    this.fs = fs;
	    fs.create("iozone.tmp", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	} catch (FSException e) {
	    Debug.out.println("ERROR");
	    return;
	}
	if (output) {
	if (outputThroughput) {
	    Debug.out.println(" read/write throughput in kbytes/sec");
	    Debug.out.println("  file(kB)  rec(kB)   write rewrite    read  reread");
	} else {
	    Debug.out.println(" read/write times in microseconds");
	    Debug.out.println("     file(kB)     rec(kB)      write    rewrite       read     reread");
	}
	}

	for (int i = 0; i < 50; i++) {
	    if (doDumpBinary) nreread = 0;
	    if (output) {
		Debug.out.println("# START IOZONE");
		if (outputThroughput) {
		    Debug.out.println("mbsec");
		} else {
		    Debug.out.println("microsec");
		}
	    }
	    autoTest();
	    ((CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager")).yield();
	    if (doDumpBinary) {
		try {
		    dumpBinary("IOZONE"+i);
		} catch(IOException ex) {throw new Error("BINARY DUMP");}
	    }
	}
	if (output) 	Debug.out.println("# END IOZONE");


	//dumpExcel();

	/*
	try {
	    fs.unlink("iozone.tmp");
	} catch (FSException e) {
	    Debug.out.println("ERROR");
	    return;
	}
	*/


    }

    private String alignString(String value, int length) {
	String tmp1 = new String();
	int leer = length - value.length();
	for (int i = 0; i < leer; i++)
	    tmp1 += " ";
	return (tmp1 + value);
    }

    private void autoTest() {
	int kilosi; 
        int recszi,count1;
	int min_file_size = KILOBYTES_START;
        int max_file_size = KILOBYTES_END;
        int min_rec_size = RECLEN_START;
        int max_rec_size = RECLEN_END;
	int xover = CROSSOVER;
	int mult, save_y = 0;
	int xx;

	Profiler profiler = ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
	if (sample) profiler.startSampling();

	/****************************************************************/
	/* Start with file size of min_file_size and repeat the test 	*/
	/* KILOBYTES_ITER_LIMIT  					*/
	/* times.  Each time we run, the file size is doubled		*/
	/****************************************************************/

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
		if (recszi > (kilosi*1024))
		    break;
		begin(kilosi, recszi);
		current_x = 0;
		current_y++;
	    }
	}

	if (sample) profiler.stopSampling();

    }

    private void begin(int kilobytes, int reclen) { // long, long
	storeValue((int)(kilobytes));
	storeValue((int)(reclen/1024));

	if (output) {
	    Debug.out.print("" + alignString(Integer.toString((int)kilobytes), 11) + alignString(Integer.toString((int)(reclen/1024)), 11)+ "   ");
	}

	writePerfTest(kilobytes, reclen);
	readPerfTest(kilobytes, reclen);

	if (output) 	Debug.out.println("");
    }

    private void readPerfTest(int kilo, int reclen) { // long, long
	Inode inode = null;
	int starttime;
	int endtime;
	int readtime[] = new int[2]; 
	int nanoReadtime[] = new int[2]; 
	int numrecs; // long
	int readrate[] = new int[2]; 
	int filebytes; // long
	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;
	// ASSUME 500 MHz time stamp counter
	try {
	    inode = (Inode)fs.lookup("iozone.tmp");
	    if (inode == null)
		return;

	    for (int j = 0; j < 2; j++) {
		//Debug.out.println("reclen="+reclen+", numrecs="+numrecs+", j="+j);
		clock.getCycles(starttimec);
		if (j==1 && sample) profiler.startSampling();
		for (int i = 0; i < numrecs; i++) {
		    //Debug.out.println("   i="+i);
		    inode.read(buffer, (int)(i*reclen), (int)reclen);
		    //ReadOnlyMemory res = inode.readWeak((int)(i*reclen), (int)reclen); // EXPERIMENTAL
		}
		if (j==1 && sample) profiler.stopSampling();
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		readtime[j] =  clock.toMicroSec(diff);
		if (readtime[j] < 1000) {
		    nanoReadtime[j] =  clock.toNanoSec(diff);
		}
		if (doDumpBinary) {
		    if (j==1) {
			//Debug.out.println("WRITEREAREAD: "+nreread+"="+readtime[j]);
			rereadtime[nreread++] = readtime[j];
		    }
		}

	    }
	    
	    inode.decUseCount();
	    
	    for (int j = 0; j < 2; j++) {
		if (readtime[j] < 3) {
		    readrate[j] = -1;
		} else {
		    if (readtime[j] < 1000) {
			/*
			MyBigNumber d0 = new MyBigNumber(diff.high);
			d0 = d0.mul(429496729);
			d0 = d0.mul(10);
			d0 = d0.add(diff.low);
			Debug.out.println("DIFF: "+d0);
			*/
			// use nanosecond measurement for throughput computation
			MyBigNumber n0 = new MyBigNumber(filebytes);
			n0 = (n0.mul(1024000)).div(nanoReadtime[j]);
			readrate[j] = n0.toInt();
			//Debug.out.println("1: "+readrate[j]+", 2: "+(((filebytes*128)/readtime[j])*8)+", 3:"+readtime[j]+", 4:"+nanoReadtime[j]);
		    } else {
			MyBigNumber n0 = new MyBigNumber(filebytes);
			n0 = (n0.mul(1024)).div(readtime[j]);
			//Debug.out.println("BIG:"+n0);
			//readrate[j] = ((filebytes*128)/readtime[j])*8; //overflow prevention
			readrate[j] = n0.toInt();
			//Debug.out.println("NOR:"+readrate[j]);
			//readrate[j] = (filebytes*1024)/readtime[j];
		    }
		}
	    }
	    
	if (output) {
	    if (outputThroughput) {
		Debug.out.print("" + alignString(Integer.toString((int)readrate[0]), 11) + alignString(Integer.toString((int)readrate[1]), 11));
	    } else {
		Debug.out.print("" + alignString(Integer.toString((int)readtime[0]), 11) + alignString(Integer.toString((int)readtime[1]), 11));
	    }
	    storeValue((int)readrate[0]);
	    storeValue((int)readrate[1]);
	}
	} catch(FSException e) {
	    Debug.out.println("EXCEPTION!"); throw new Error();
	}
    }

    private void writePerfTest(int kilo, int reclen) { // long, long
	Inode inode = null;
	int starttime; //long
	int endtime; //long
	int writetime[] = new int[2]; //long
	int numrecs; // long
	int writerate[] = new int[2]; // long
	int filebytes; // long

	numrecs = (kilo*1024)/reclen;
	filebytes = numrecs*reclen;

	try {
	    inode = (Inode)fs.lookup("iozone.tmp");
	    if (inode == null)
		return;
	    for (int j = 0; j < 2; j++) {
		clock.getCycles(starttimec);
		//Debug.out.println("START "+starttime);
		for (int i = 0; i < numrecs; i++) {
		    inode.write(buffer, (int)(i*reclen), (int)reclen);
		}
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		writetime[j] = clock.toMicroSec(diff);//diff.low/500; //System.currentTimeMillis() - starttime; 
		if (writetime[j] < MINIMAL_MICROS) {
		    //throw new Error("time difference too small. increase record size");
		    writetime[j] = -1;
		}
	    }
	    
	    inode.decUseCount();
	    
	    for (int j = 0; j < 2; j++) {
		if (writetime[j] != -1) {
		  writerate[j] = ((filebytes*128)/writetime[j])*8; //overflow prevention
		    //Debug.out.println("WRITERATE "+writerate[j]);
		} else {
		    writerate[j] = -1;
		}
	    }
	if (output) {
	    if (outputThroughput) {
		Debug.out.print("" + alignString(Integer.toString((int)writerate[0]), 11) + alignString(Integer.toString((int)writerate[1]), 11));
	    } else {
		Debug.out.print("" + alignString(Integer.toString((int)writetime[0]), 11) + alignString(Integer.toString((int)writetime[1]), 11));
	    }
	    storeValue((int)writerate[0]);
	    storeValue((int)writerate[1]);
	}
	} catch(FSException ex) {
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
		Debug.out.println("MAX_X too small");
	    if (max_y >= MAX_Y)
		Debug.out.println("MAX_X too small");
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
        int current_file_size;

	//try {
	    Debug.out.print("      ");
	    //output.writeBytes("      ");
	    for (i = RECLEN_START; i <= (RECLEN_END/MULTIPLIER); i = i*MULTIPLIER) {
		Debug.out.print("\"" + (i/1024) + "\" ");
		//output.writeBytes("\"" + (i/1024) + "\" ");
	    }
	    Debug.out.println("\"" + (RECLEN_END/1024) + "\"");
	    //output.writeBytes("\"" + (RECLEN_END/1024) + "\"");
	    //output.write('\n');
	    
	    current_file_size = report_array[0]; // [0][0];
	    Debug.out.print("\"" + current_file_size + "\" ");
	    //output.writeBytes("\"" + (RECLEN_END/1024) + "\"");
	    
	    for (i = -1; i < max_y; i++){
		if (report_array[(i+1)*MAX_X] != current_file_size) {
		//if (report_array[0][i+1] != current_file_size) {
		    Debug.out.println();
		    //output.write('\n');
		    current_file_size = report_array[(i+1)*MAX_X]; // report_array[0][i+1]
		    Debug.out.print("\"" + current_file_size + "\" ");
		    //output.writeBytes("\"" + current_file_size + "\" ");
		}
		Debug.out.print(" " + report_array[(i+1)*MAX_X+who] + " ");
		//output.writeBytes(" " + report_array[who][i+1] + " ");
	    }
	    Debug.out.println();
	    //output.write('\n');
	    //output.flush();
	    //} catch (IOException e) {
	    //Debug.out.println("IOException");
	    //}
    }


    /********************************************************************/
    /* Wrapper that dumps each of the collected data sets.	       	*/
    /********************************************************************/
    private void dumpExcel() {
	//try {
	    Debug.out.println();
	    //output.write('\n');
	    Debug.out.println("\"Writer report\"");
	    //output.writeBytes("\"Writer report\"");
	    //output.write('\n');
	    dumpReport(2);
	    Debug.out.println();
	    //output.write('\n');
	    Debug.out.println("\"Re-writer report\"");
	    //output.writeBytes("\"Re-writer report\"");
	    //output.write('\n');
	    dumpReport(3); 
	    Debug.out.println();
	    //output.write('\n');
	    Debug.out.println("\"Reader report\"");
	    //output.writeBytes("\"Reader report\"");
	    //output.write('\n');
	    dumpReport(4);
	    Debug.out.println();
	    //output.write('\n');
	    Debug.out.println("\"Re-Reader report\"");
	    //output.writeBytes("\"Re-Reader report\"");
	    //output.write('\n');
	    dumpReport(5);
	    Debug.out.println();
	    //output.write('\n');
	    //} catch (IOException e) {
	    //Debug.out.println("IOException");
	    //return;
	    //}	    

	    /*
	      byte[] bytebuffer = byteoutput.toByteArray(); 
	      
	      fs.create("report.asc", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	      fs.write("report.asc", bytebuffer, 0, bytebuffer.length);
	    */
	
	    /*
	      FileOutputStream fileoutput = new FileOutputStream("report.asc");
	      fileoutput.write(bytebuffer);
	      fileoutput.close();
	    */
    }
}
