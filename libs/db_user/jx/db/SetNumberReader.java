package jx.db;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

/** interface for reading a tuple address
 */
public interface SetNumberReader  extends jx.zero.Portal {
    /** returns page address in the systembuffer
     * @throws CodedException thrown on error ( SetNumber not initialized )
     * @return page address in the systembuffer
     */
    public int getPageNumber() throws CodedException;
    /** returns the logical tuple offset in the page
     * @throws CodedException thrown on error ( SetNumber object not initialized )
     * @return logical tuple offset in the page
     */
    public int getTidNumber() throws CodedException;
    /** receives a byte array containing both page address and logical offset in the page
     * @param baDest destination buffer, where the data should be copied
     * @param iOffset offset in the destination buffer
     * @throws CodedException thrown on error ( SetNumber not initialized )
     */
    public void getSetNumber( byte[] baDest, int iOffset ) throws CodedException;
}
