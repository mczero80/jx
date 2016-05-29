package jx.db;

public interface Index extends jx.zero.Portal {

    /*Index Types, currently only BxTree( single attr, single dim ) supported, later Grid File maybe */
    public static final int INDX_TYPE_SINGLE_ATTR = 0;
    public static final int INDX_TYPE_SINGLE_DIM_MULTI_ATTR = 1;


    /** deletes an index with all its structures
     * @throws CodedException thrown on error ( write error, etc. )
     */
    public void dropIndex() throws CodedException;


    /** returns an IndexInfo object for an index
     * @throws CodedException thrown on error ( no valid index, etc )
     * @return returns an IndexInfo object, containing information about the index ( attribute map, unique flag, etc )
     */
    public IndexInfo getIndexInfo() throws CodedException;


    /** returns the id of the table, containing the index, which id is iIID
     * @throws CodedException thrown on error ( no valid index, etc )
     * @return id of the table containing the index
     */
    public Table getTable() throws CodedException;


    public Key getKey() throws CodedException;

}
