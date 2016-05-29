package bioide;

import jx.zero.*;

/**
 * Parse string.
 * @author Michael Golm
 * @author Andreas Weissel
 */
public class DataFormat {
    public static String readString(Memory memory, int pos, int len) {
	byte[] array = new byte[len];
	for (int i = 0; i < len; i++)
	    array[i] = memory.get8(pos+i);
	return new String(array);
    }

    public static void writeString(Memory memory, int pos, String value, int len) {
	byte strarray[] = value.getBytes();
	int strlen = strarray.length;
	if (strlen > len)
	    strlen = len;
	for (int i = 0; i < strlen; i++)
	    memory.set8(pos+i, strarray[i]);
	while (strlen < len)
	    memory.set8(pos+strlen++, (byte)0);
    }
}
