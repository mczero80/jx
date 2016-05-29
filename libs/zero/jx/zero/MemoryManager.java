package jx.zero;

public interface MemoryManager extends Portal {
    public Memory alloc(int size);
    /**
     * Allocate memory that is aligned to "bytes".
     * @param size size of memory block
     * @param bytes alignment of memory block
     */
    public Memory allocAligned(int size, int bytes);
    public DeviceMemory allocDeviceMemory(int start, int size);

    /* query global status information */
    public int getTotalMemory();   
    public int getTotalFreeMemory();

    /* query domain specific status information */
    public int getFreeHeapMemory();
}



