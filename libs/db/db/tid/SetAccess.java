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

 /** interface containing functions for managing physical database sets
  */
public interface SetAccess {

    /** This function adds a new set to the database
     * @param iDataSize size of the added set
     * @param iSetType set type ( relation id )
     * @param cSetNumber place to store the generated set number
     * @throws CodedException thrown on error ( no place left, etc. )
     * @return a Set object, allowing modifications on the added set
     */
    public Set addSet(SetNumber cSetNumber, int iDataSize, int iSetType) throws CodedException;

    /** this function returns a reference to a set in the database
     * @param iSetType set type ( relation id )
     * @param cSetNumber number of the set, that identifies its position
     * @throws CodedException thrown on error ( wrong SetNumber, etc. )
     * @return a Set object, allowing modifications on the requested set
     */
    public Set getSet(SetNumber cSetNumber, int iSetType/*will be used for transaction locking*/) throws CodedException;

    /** this function removes a set from the database
     * @param iSetType set type ( relation id )
     * @param cSetNumber number of the set to be removed
     * @throws CodedException thrown on error ( invalid SetNumber, etc. )
     */
    public void removeSet(SetNumber cSetNumber, int iSetType/*will be used for transaction locking*/) throws CodedException;

    /** this function modifies the length and the contents of an existing set
     * @param iDataSize new length of the set
     * @param iSetType set type ( relation id )
     * @param cSetNumber number of the set to be modified
     * @throws CodedException thrown on error ( wrong SetNumber, etc. )
     * @return a Set object, allowing modifications on the requested set
     */
    public Set modifySet(SetNumber cSetNumber, int iDataSize, int iSetType) throws CodedException ;

    /** optimizes set redirections in the TID concept, currently not implemented
     */
    public void optimizeSetPage(SetPage cSetPage) ;
    /** returns the maximal set length
     * @return the maximal set length
     */
    public int getMaxSetLen() ;
    /** returns the minimal set length
     * @return the minimal set length
     */
    public int getMinSetLen();
    /** deletes all sets of a given set type
     * @param iSetType number of the set type to be deleted
     * @throws CodedException thrown on error ( wrong set type, etc. )
     */
    public void dropSetType(int iSetType) throws CodedException ;
}