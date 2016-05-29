package jx.zero;

/**
 * The methods in this class a known by the Bytecode-Translater and 
 * some are overwritten by compiler plugins.
 *
 * see jx.compiler.plugins.VMSupport
 *
 */

public class VMSupport {

    public static short swapShortByteOrder(short value) {
	return (short)(((value&0xff00)>>>8)|((value&0x00ff)<<8));
    }

    public static int swapIntByteOrder(int value) {
	int rvalue=0;
	rvalue |= (value & 0x000000ff) <<  24;
	rvalue |= (value & 0x0000ff00) <<  8;
	rvalue |= (value & 0x00ff0000) >>> 8;
	rvalue |= (value & 0xff000000) >>> 24;
	return rvalue;
    }

    /* byte[] */
    public static void arraycopy_byte_left(byte[] src, int srcOffset, byte[] dst, int dstOffset, int count) {
	srcOffset += count;
	dstOffset += count;
	for(int i=0; i<count; ++i) dst[--dstOffset] = src[--srcOffset];
    }
    public static void arraycopy_byte_right(byte[] src, int srcOffset, byte[] dst, int dstOffset, int count) {	
	for(int i=0; i<count; i++) dst[dstOffset+i] = src[srcOffset+i];
    }
    /* char[] */
    public static void arraycopy_char_left(char[] src, int srcOffset, char[] dst, int dstOffset, int count) {
	srcOffset += count;
	dstOffset += count;
	for(int i=0; i<count; ++i) dst[--dstOffset] = src[--srcOffset];
    }
    public static void arraycopy_char_right(char[] src, int srcOffset, char[] dst, int dstOffset, int count) {
	for(int i=0; i<count; i++) dst[dstOffset+i] = src[srcOffset+i];
    }
    /* else */
    public static void arraycopy_left(Object[] src, int srcOffset, Object[] dst, int dstOffset, int count) {
	srcOffset += count;
	dstOffset += count;
	for(int i=0; i<count; ++i) dst[--dstOffset] = src[--srcOffset];
    }
    public static void arraycopy_right(Object[] src, int srcOffset, Object[] dst, int dstOffset, int count) {
	for(int i=0; i<count; i++) dst[dstOffset+i] = src[srcOffset+i];
    }
}
