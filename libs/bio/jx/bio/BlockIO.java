package jx.bio;

import jx.zero.Memory;
import jx.zero.Portal;

/**
 * Access to a block device.
 * @author Michael Golm
 */
public interface BlockIO extends Portal {
    /**
     * Get capacity of the block device.
     *
     * @return number of sectors
     */
    public int getCapacity();

    /**
     * Get the size of a sector
     *
     * @return size of one sector in bytes
     */
    public int getSectorSize();

    /**
     * Read a range of sectors.
     *
     * @param startSector     first sector to read
     * @param numberOfSectors number of sectors to read
     * @param buf             target memory of transfer
     * @param synchronous     deprecated
     * @return                -1 if error, 0 if ok
     */
    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous);

    /**
     * Write a range of sectors.
     *
     * @param startSector     first sector to write
     * @param numberOfSectors number of sectors to write
     * @param buf             source memory of transfer
     * @param synchronous     deprecated
     * @return                -1 if error, 0 if ok
     */
    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous);

}
