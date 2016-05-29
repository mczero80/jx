package jx.rdp;

public class Constants {

    public static final int width = 800; /* FIXME */
    public static final int height = 600; /* FIXME */
    public static final boolean bitmap_compression = true;
    public static final boolean desktop_save = true;
    public static final int keylayout = 0x409;

    public static final int PORT=3389;
    
    private static boolean encryption=true;
    private static boolean licence = true;
    
    public static boolean getEncryptionStatus() {
	return true;
    }

    public static boolean getLicenceStatus() {
	return true;
    }
    public static void setEncryptionStatus(boolean status) {
	encryption=status;
    }

}
