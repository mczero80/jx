package test.jdkfs;

public class Main {
    public static void main(String[] args) throws Exception {	
	if (args!=null && args.length>0) {System.out.println("JDK FS-Test "+args[0]);}
	//IOZONE iozone = new IOZONE(args[0], 4, IOZONE.IOZONE_MAX_FILESIZE, 4*1024, 16*1024*1024);
	System.out.println("IOZONE.IOZONE_MAX_FILESIZE = "+IOZONE.IOZONE_MAX_FILESIZE);
	IOZONE iozone = new IOZONE("iozone.tmp", 4, IOZONE.IOZONE_MAX_FILESIZE, 4*1024, 16*1024*1024);
    }
}
