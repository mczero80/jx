package jx.zip;

import java.io.*;
import jx.zero.ReadOnlyMemory;
import jx.zero.Memory;
import jx.zero.debug.Dump;
import jx.zero.Debug;

public class ZipFile  {
    final static boolean doDebug = false;

    final static int LREC_SIZE   =  30;  
    final static int CREC_SIZE   =  46;
    final static int ECREC_SIZE  =  22;  
    

    final static int LOCAL_HEADER_SIGNATURE = 0x04034b50;
    final static int L_VERSION_NEEDED_TO_EXTRACT_0   =   4;
    final static int L_VERSION_NEEDED_TO_EXTRACT_1  =    5;
    final static int L_GENERAL_PURPOSE_BIT_FLAG     =    6;
    final static int L_COMPRESSION_METHOD            =   8;
    final static int L_LAST_MOD_FILE_TIME            =   10;
    final static int L_LAST_MOD_FILE_DATE             =  12;
    final static int L_CRC32                         =   14;
    final static int L_COMPRESSED_SIZE               =   18;
    final static int L_UNCOMPRESSED_SIZE             =   22;
    final static int L_FILENAME_LENGTH               =   26;
    final static int L_EXTRA_FIELD_LENGTH             =  28;

    final static int CENTRAL_HEADER_SIGNATURE = 0x02014b50;
    final static int C_SIGNATURE                     =   0;
    final static int C_VERSION_MADE_BY_0             =   4;
    final static int C_VERSION_MADE_BY_1              =  5;
    final static int C_VERSION_NEEDED_TO_EXTRACT_0   =   7;
    final static int C_VERSION_NEEDED_TO_EXTRACT_1   =   8;
    final static int C_GENERAL_PURPOSE_BIT_FLAG     =    9;
    final static int C_COMPRESSION_METHOD           =   10;
    final static int C_LAST_MOD_FILE_TIME           =   12;
    final static int C_LAST_MOD_FILE_DATE           =   14;
    final static int C_CRC32                        =   16;
    final static int C_COMPRESSED_SIZE              =   20;
    final static int C_UNCOMPRESSED_SIZE            =   24;
    final static int C_FILENAME_LENGTH              =   28;
    final static int C_EXTRA_FIELD_LENGTH           =   30;
    final static int C_COMMENT_LENGTH               =   32;
    final static int C_DISK_NUMBER_START            =   34;
    final static int C_INTERNAL_FILE_ATTRIBUTES     =   36;
    final static int C_EXTERNAL_FILE_ATTRIBUTES     =   38;
    final static int C_RELATIVE_OFFSET_LOCAL_HEADER  =  42;
    final static int C_FILENAME                       =  46;

    final static int END_CENTRAL_DIR_SIGNATURE = 0x06054b50;
    final static int NUMBER_THIS_DISK                 = 4;
    final static int NUM_DISK_WITH_START_CENTRAL_DIR  = 6;
    final static int NUM_ENTRIES_CENTRL_DIR_THS_DISK  = 8;
    final static int TOTAL_ENTRIES_CENTRAL_DIR        = 10;
    final static int SIZE_CENTRAL_DIRECTORY           = 12;
    final static int OFFSET_START_CENTRAL_DIRECTORY   = 16;
    final static int ZIPFILE_COMMENT_LENGTH           = 20;

    //private Memory zip;
    private ReadOnlyMemory zip;
    private int count;
    private int dirofs;
    private int mempos;
    private byte[] dirbuf;
    private int current;

    private void seek(int pos) {
	if (pos<0) Debug.throwError("Seeking to negative position "+pos);
	mempos = pos;
    }

    private void read(byte[] buf) {
	zip.copyToByteArray(buf, 0, mempos, buf.length);
    }

    //public ZipFile(Memory zip) {
    public ZipFile(ReadOnlyMemory zip) {
	this.zip = zip;
	
	byte[] buffer = new byte[ECREC_SIZE];
	int len = (int)zip.size();
	if (doDebug) {
	    Debug.out.println("Ziplen="+len);
	    Debug.out.println("Zip start of buffer:");
	    Dump.xdump(zip,0,32);
	    Debug.out.println("Zip directory:");
	    Dump.xdump(zip,len-ECREC_SIZE,32);
	}
	seek(len-ECREC_SIZE);
	read(buffer);
	if (doDebug) {
	    Debug.out.println("Array Zip directory:");
	    Dump.xdump(buffer,32);
	}
	int signature = makelong(buffer, 0);
	if (signature != END_CENTRAL_DIR_SIGNATURE) {
	    Debug.out.println("Wrong signature=0x"+Integer.toHexString(signature));
	    return;
	}
	count = makeword(buffer, TOTAL_ENTRIES_CENTRAL_DIR);
	//Debug.out.println("count="+count);
	int dir_size = makelong(buffer, SIZE_CENTRAL_DIRECTORY);
	//Debug.out.println("dir_size="+dir_size);
	seek(len-(dir_size+ECREC_SIZE));
	dirbuf = new byte[dir_size];
	read (dirbuf);
	//Debug.out.println("nr disk = " + makeword(dirbuf, NUMBER_THIS_DISK));
	//Debug.out.println("nr disk cdir = " + makeword(dirbuf, NUM_DISK_WITH_START_CENTRAL_DIR));
	//Debug.out.println("num entries = " + makeword(dirbuf,NUM_ENTRIES_CENTRL_DIR_THS_DISK));
	dirofs = 0;
	current = 0;
    }

    public ZipEntry getNextEntry() {
	if (current == count) return null;
	ZipEntry entry = new ZipEntry();
	current++;
	//Debug.out.println("** "+current);
	int signature = makelong(dirbuf, dirofs+0);
	if (signature != CENTRAL_HEADER_SIGNATURE) {
	    Debug.out.println("wrong central header signature 0x"+Integer.toHexString(signature));
	    Dump.xdump(dirbuf, dirofs, 64);
	    return null; 
	}
	entry.uncompressed_size = makelong (dirbuf, dirofs+C_UNCOMPRESSED_SIZE);
	entry.compression_method = makeword (dirbuf, dirofs+C_COMPRESSION_METHOD);
	if (entry.compression_method != 0) {
	    Debug.out.println("Compression not supported.");
	}
	int filename_length = makeword (dirbuf, dirofs+C_FILENAME_LENGTH);
	int cextra_length = makeword (dirbuf, dirofs+C_EXTRA_FIELD_LENGTH);
	int comment_length = makeword (dirbuf, dirofs+C_COMMENT_LENGTH);
	int local_header_offset = makelong(dirbuf, dirofs+C_RELATIVE_OFFSET_LOCAL_HEADER);
	//Debug.out.println("Local header offset: " + local_header_offset);
	//Debug.out.println("Filename length: " + filename_length);
	//Debug.out.println("Extra length: " + cextra_length);
	//Debug.out.println("Comment length: " + comment_length);
	
	// read local header
	seek(local_header_offset);
	byte[] header = new byte[LREC_SIZE];
	read(header);
	if (makelong(header, 0) != LOCAL_HEADER_SIGNATURE) {
	    Debug.out.println("wrong local header signature");
	    Dump.xdump(header, 0, LREC_SIZE);
	    return null;
	}
	      
	int filestart = local_header_offset + LREC_SIZE + makeword(header, L_FILENAME_LENGTH) 
	    + makeword(header, L_EXTRA_FIELD_LENGTH);
		
	entry.filename = makestring(dirbuf, dirofs+C_FILENAME, filename_length);
	//Debug.out.println("Filename:"+entry.filename);
	if (entry.compression_method != 0) {
	    Debug.out.println("File "+entry.filename+ " is compressed.");
	    Debug.out.println("Compression not supported.");
	    //return null;
	    throw new Error();
	}
	entry.data = zip.getReadOnlySubRange(filestart, entry.uncompressed_size);
	//Dump.xdump(entry.data, 0, 128);
	dirofs += C_FILENAME + filename_length + cextra_length + comment_length;
	return entry;
    }
    
    private static int makeword(byte[] b, int offset) {
	//Debug.out.println("1:"+Integer.toHexString(((int)b[offset+1]<<8))
	//		   +" 0:"+Integer.toHexString(b[offset]));
	return (((int)(b[offset+1]) << 8) | ((int)(b[offset])&0xff));
    }
    private static int makelong(byte[] b, int offset) {
	return ((((int)b[offset+3]) << 24) & 0xff000000)
	    | ((((int)b[offset+2]) << 16) & 0x00ff0000)
	    | ((((int)b[offset+1]) << 8) & 0x0000ff00)
	    | ((int)b[offset+0] & 0xff);
    }
    private static String makestring(byte[] b, int offset, int len) {
      //Dump.xdump(b, offset, len);
	return new String(b, offset, len);
    }

}
