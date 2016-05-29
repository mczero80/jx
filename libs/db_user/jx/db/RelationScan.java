package jx.db;


/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public interface RelationScan extends RelationalOperator, jx.zero.Portal{

    /** returns a byte array containing the key of the current tuple
     * @throws CodedException thrown on error
     * @return byte array containing the key of the current tuple
     */
    public Key getCurrentKey() throws CodedException;

    /** returns the address of the current tuple
     * @throws CodedException thrown on error
     * @return SetNumberReader object containing address of the current tuple
     */
    public SetNumberReader getCurrentSetNum() throws CodedException;

    /** searches for the given key and sets the current position at the nearest place
     * @param baKey key to search for
     * @throws CodedException thrown on error ( wrong attribute count, etc.)
     * @return true if the key was found, false otherwise
     */
    public boolean findKey(byte[][] baKey) throws CodedException;

    /** returns an IndexInfo object describing an index of a table
     * @throws CodedException thrown on error
     * @return IndexInfo object describing an index of a table
     */
    public IndexInfo getIndexInfo() throws CodedException;

    /** returns an integer array containing the positions of the index attributes
     * @throws CodedException thrown on error
     * @return an integer array containing the positions of the index attributes
     */
    public int[] getIndexKeyPos() throws CodedException;
}
