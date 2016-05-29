package jx.db;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */


/** A simple container for temporary storage. Use it if you want to store a large amount of sets in a page buffer.
 * The sets should be of the same length
 */
public interface BufferList  extends jx.zero.Portal {
    /** error code, thrown if the BufferList is empty and the user tries to navigate through the sets
     */
    public static final int ERR_LIST_EMPTY = 0;

    /** moves the position to the beginning of the BufferList
     * @throws CodedException thrown on error
     * @return thrue if successful, false otherwise
     */
    public boolean moveToFirst()  throws CodedException;

    /** moves the position to the next element in the buffer
     * @throws CodedException thrown on error
     * @return true if successful, false otherwise ( end reached )
     */
    public boolean moveToNext() throws CodedException;

    /** inserts a new set at the end of the buffer
     * @throws CodedException thrown on error ( no place left, etc. )
     * @return a Set object allowing modifications on the added set
     */
    public void addAtEnd( byte[] baData, int iOffset ) throws CodedException ;

    /** returns a reference to the set at the current position
     * @throws CodedException thrown on error ( empty buffer, read error, etc.)
     * @return a Set object allowing modifications on the current set
     */
    public byte[] getCurrent() throws CodedException ;

    /** deletes all buffer contents
     * @throws CodedException thrown on error ( read/write error, etc )
     */
    public void delete() throws CodedException ;
}
