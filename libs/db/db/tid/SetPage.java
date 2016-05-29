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

 /** the SetPage interface is implemented by the SetPageImpl class and
  * provides functions for managing the contents of a single page in
  * the TID concept
  */
public interface SetPage {

    /** error code, thrown if an invalid set address is used
     */
    public static final int ERR_INVALID_SET_NO = 2;

    /** formats the associated page, the page is empty after that
     */
    public void format();
    /** associates a new page with this SetPage object
     * @param baPage byte array containing the contents of the page
     * @throws CodedException thrown on error, if the byte array has wrong size
     */
    public void setPage(byte[] baPage) throws CodedException;
    /** adds a new SetAccess into the page
     * @param cSetNumber this SetNumber object will contain the address of the
     * new set after the function returns
     * @param iDataSize size of the new set
     * @throws CodedException thrown on error ( the size is too big, etc. )
     * @return a Set object pointing to the new set
     */
    public Set addSet(SetNumber cSetNumber, int iDataSize) throws CodedException;
    /** gets a set from the page
     * @param cSetNumber address of the set
     * @throws CodedException thrown on error ( wrong set address, etc. )
     * @return a Set object pointing to the set
     */
    public Set getSet(SetNumber cSetNumber) throws CodedException;
    /** removes a set from the page
     * @param cSetNumber address of the set to be removed
     * @throws CodedException thrown on error ( wrong address, etc. )
     */
    public void removeSet(SetNumber cSetNumber) throws CodedException;
    /** changes the size of a set in the page
     * @param cSetNumber address of the set to be modified
     * @param iDataSize the new size of the set
     * @throws CodedException thrown on error ( wrong set address, wrong length, etc.)
     * @return a Set object pointing to the modified set
     */
    public Set modifySet(SetNumber cSetNumber, int iDataSize) throws CodedException;
    /** transforms a set in a page to a set redirection ( see TID concept docs )
     * @param cSetNumber address of the set to be transformed
     * @param cRedirectionSetNumber address of the set that will be referenced
     * by this redirection
     * @param iDataLen length of the referenced set
     * @throws CodedException thrown on error
     */
    public void setRedirection(SetNumber cSetNumber, SetNumber cRedirectionSetNumber, int iDataLen) throws CodedException;
    /** reads the contents of a redirection set
     * @param cSetNumber address of the redirection set
     * @param cRedirectionSetNumber place where the contents of the redirection set should
     * be placed
     * @throws CodedException thrown on error
     * @return size of the referenced set
     */
    public int getRedirection(SetNumber cSetNumber, SetNumber cRedirectionSetNumber) throws CodedException;
    /** returns the length of the requested set
     * @param cSetNumber address of the set
     * @throws CodedException thrown on error ( wrong set address, etc. )
     * @return the length of the set
     */
    public int getSetLength(SetNumber cSetNumber) throws CodedException;
    /** returns the free space in the page
     * @return the free space in the page
     */
    public int getFreeSpace();
    /** returns the count of the sets currently stored in the page
     * @return the count of the sets currently stored in the page
     */
    public int getSetSlotCount();
    /** checks whether a set is a redirection
     * @param iIndex tid index for the set in the page
     * @return true, if the set is a redirection set, false otherwise
     */
    public boolean getRedirectionFlag(int iIndex);

}