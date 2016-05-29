package db.tid;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

import jx.db.CodedException;

/** interface implemented by the SetArrayImpl class. This class
 * provides basic functionality for storing large amounts of small
 * bytechains ( byte array sets ) of equal length using normal SetAccess sets
 * A byte array in the chain can not be bigger as a SetAccess set.
 */
public interface SetArray {

    /** error code, thrown if the user tries to create a set array with a
     * too big length of the byte array elements in it. An element in a
     * set array must be smaller than a PageBuffer page.
     */
    public static final int ERR_REC_TOO_BIG   = 0;

    /** error code, thrown when the user calls removeCurrentRecord on
     * an empty set array
     */
    public static final int ERR_ALREADY_EMPTY = 1;

    /** inits an existing SetArray object to point to a byte array set
     * @param cSetAccess SetAccess containing the byte array set
     * @param cStart address of the set containing the first byte array
     * in the chain
     * @param iDataSize size of one byte array
     * @param iSetType set type of the sets that contain the byte array chain
     * @throws CodedException thrown on error ( wrong combination of input params )
     */
    public void init(SetAccess cSetAccess, SetNumber cStart, int iDataSize, int iSetType) throws CodedException;
    /** gets the byte array at the current position in the set array
     * @throws CodedException thrown on error ( empty set array, read error, etc.)
     * @return a Set object pointing to the byte array at the current position
     * in the set array
     */
    public Set getCurrentRecord() throws CodedException;
    /** removes the byte array at the current position
     * @throws CodedException thrown on error ( empty set array )
     */
    public void removeCurrentRecord() throws CodedException;
    /** moves the current position to the beginning of the set array
     * @throws CodedException thrown on error
     * @return true on success, false otherwise
     */
    public boolean moveToFirst() throws CodedException;
    /** moves the current position to the next element in the
     * set array
     * @throws CodedException thrown on error
     * @return true on success, false otherwise ( end of the set array reached )
     */
    public boolean moveToNext() throws CodedException;
    /** adds a new byte array at the beginning of the set array
     * @param baData data to be added
     * @param iOffset offset of the data in the baData byte array
     * @throws CodedException thrown on error ( no place left, etc )
     */
    public void addAtFront(byte[] baData, int iOffset) throws CodedException;

    /** deletes the contents of a set array
     * @throws CodedException trown on error ( set array not initialized, etc. )
     */
    public void deleteArray() throws CodedException;

    /** deletes the contents of a set array
     * @param cSetNum address of the start set of the array
     * @throws CodedException trown on error ( set array not initialized, etc. )
     */
    public void deleteArray( SetNumber cSetNum ) throws CodedException;
    /** creates a new empty byte array chain
     * @param cSetAccess SetAccess object which will contain the new chain
     * @param cSetNum set number that points to the start set of the chain after
     * the function returns
     * @param iSetType set type of the new chain
     * @throws CodedException thrown on error ( wrong input, no place left, etc. )
     */
    public void createStartSet( SetAccess cSetAccess, SetNumber cSetNum, int iSetType ) throws CodedException ;

    /** returns the max byte array length that can be stored in
     * a set array
     * @return the max byte array length that can be stored in
     * a set array
     */
    public int getMaxRecordLen();

}