package jx.zero;

public interface Memory extends ReadOnlyMemory {

    /**
     * @param where a 8-bit offset into this memory
     */
    public void set8(int where, byte what);

    /**
     * @param where is a 16-bit offset
     */
    public void set16(int where, short what);

    /**
     * @param where is a 32-bit offset
     */
    public void set32(int where, int what);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public void setLittleEndian32(int offset, int value);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public void setLittleEndian16(int offset, short value);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public void setBigEndian32(int offset, int value);

    /**
     * @param offset is a 8-bit offset (need not be aligned)
     */
    public void setBigEndian16(int offset, short value);

    public void copyFromByteArray(byte[] array, int array_offset, int mem_offset, int len);

    public int copyFromMemory(Memory src, int srcOffset, int dstOffset, int len);

    /*------------------ split & join ----------------------------------*/

    public void split2(int offset, Memory[] parts);

    public void split3(int offset, int size, Memory[] parts);

    public Memory getSubRange(int start, int size); // split3 and return middle part

    public Memory joinPrevious();

    public Memory joinNext();

    public Memory joinAll();

    /** extend the range of this memory object */
    //public Memory extendRange(int atBeginning, int atEnd);

    /** extend the memory to its full range */
    //public Memory extendFullRange();

    /** Map part of this memory to an object.
     * The state of the memory is initialized with the contents
     * of this Memory object.
     * @param classname name of the mapped object's class
     */
    public Object map(VMClass vmclass);

    //public void copyFromObject(MappedObject o, int start);

    // Synchronize all memory mappings.
    // Write the contents of all mapped objects into this Memory object.
    //public void syncMappings();

    // JX PROBLEM: Object methods cannot be called at interfaces if they are
    // not part of the interface
    //public boolean equals(Object o);
    //public Object clone();

    /**
     * Revoke access to this memory.
     * All previously obtained references to this memory become invalid.
     * @returns a new valid reference to the memory
     */
    public Memory revoke();

    // public int getOffset();
    public boolean isValid();

    public void copy(int from, int to, int length);
    public void move(int dst, int src, int count);
    public void fill16(short what, int offset, int length);
    public void fill32(int what, int offset, int length);
    public void clear();
    //public void fill8(byte what, int offset, int length);
}



