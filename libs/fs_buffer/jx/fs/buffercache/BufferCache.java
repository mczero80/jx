package jx.fs.buffercache;

/**
 * BufferCache interface to be used by all file systems.
 *
 * @author Michael Golm
 */
public interface BufferCache {

    /**
     * Reads the block from the block device.
     * The returned buffer is locked. It must be released by either by calling
     * - brelse() if the buffer was not modified
     * - if the buffer has been modified (is dirty)
     *    - bdwrite()
     *    - bawrite()
     *    - bwrite()
     *
     * @param  block  block number
     * @param  size   block size (multiples of 1024??)
     * @return a locked <code>BufferHead</code> containing the block
     */
    BufferHead bread(int block);


    /**
     * Releases the buffer.
     * Tells the buffer cache that the buffer is no longer used and can be
     * reused for other data.
     * 
     * If the buffer is "dirty" a write to the block device is scheduled, but
     * not performed immediately.
     *
     * @param bh the buffer that should be released
     */
    void brelse(BufferHead bh);

    /**
     * Releases the buffer.
     * Mark the buffer as modified and schedule a write after a certain time.
     * Do not enqueue buffer for writing.
     *
     * @param bh the buffer that should be released and written
     */
    void bdwrite(BufferHead bh);

    /**
     * Releases the buffer.
     * Mark the buffer as modified and schedule an immediate write.
     *
     * @param bh the buffer that should be released and written
     */
    void bawrite(BufferHead bh);

    /**
     * Releases the buffer.
     * Mark the buffer as modified and perform a synchronous write.
     *
     * @param bh the buffer that should be released and written
     */
    void bwrite(BufferHead bh);

    /**
     * Mark buffer as dirty but do not release it.
     *
     * @param bh the buffer that should be marked
     */
    void bdirty(BufferHead bh);



    /**
     * Fills the buffer with data and releases the buffer immediately.
     */
    void breadn(int startBlock, int numberBlocks);


    /**
     * Forget this buffer. The buffer can be reused.
     *
     * @param bh the buffer
     */
    void bforget(BufferHead bh);


    /**
     * Get a block from the cache.
     * Do not read the block from the block device if it is not found in the cache.
     * Return null in this case.
     *
     * @param  block  block number
     * @return the buffer or null if the buffer is not in the cache
     */
    BufferHead findBuffer(int block);

    /**
     * Get a block from the cache or from the block device.
     * First tries to find the block in the cache. If it is not in the cache
     * reads the block from the block device.
     * If the cache is full and all buffers are in use, tries to enlarge the cache.
     * If this is not possible, this method blocks until a buffer becomes available.
     *
     * @param  block  block number
     * @return the buffer. Never null.
     */
    BufferHead getblk(int block);


  


    
    /**
     * Writes all dirty buffers to the block device.
     * @param wait wait for buffers to be unlocked
     */
    void syncDevice(boolean wait);

    /**
     * Write all dirty buffers to the block device.
     * Locked buffers or "young" buffers are not yet written.
     */
    void flushCache();


    /**
     * Ensure that the contents of the buffer is up-to-date.
     */
    void updateBuffer(BufferHead bh);


    /**
     * Print a buffer cache statistics.
     */
    void showBuffers();

}
