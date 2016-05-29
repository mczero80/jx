package jx.zero;

public interface ReadOnlyMemory extends Portal {
    /**
     * @param where is a 8-bit offset
     */
    public byte get8(int where);

    /**
     * @param where is a 16-bit offset
     */
    public short get16(int where);

    /**
     * @param where is a 32-bit offset
     */
    public int get32(int where);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public int getLittleEndian32(int offset);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public short getLittleEndian16(int offset);

    public int getBigEndian32(int where);

    public short getBigEndian16(int where);
    
    public void copyToByteArray(byte[] array, int array_offset, int mem_offset, int len);
    
    public int copyToMemory(Memory dst, int srcOffset, int dstOffset, int len);

    public ReadOnlyMemory getReadOnlySubRange(int start, int size);
    
    /**
     * return size in bytes
     */
    public int size();
    
    public int getStartAddress();
}



