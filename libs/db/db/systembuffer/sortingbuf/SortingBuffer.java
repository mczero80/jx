package db.systembuffer.sortingbuf;


import jx.db.CodedException;
import db.com.EventSync;
import db.com.ObjHolder;

import db.systembuffer.PageBuffer;


/** contains functions for buffering managing read and write requests from a {@link PageBuffer} object to
 * a {@link BlockIO} object
 */
public interface SortingBuffer {

    /** adds a new read request to the sorting buffer
     * @param iPageNum number of the page to be read
     * @param bStillDirty an ObjectHolder, that contains a Boolean object, indicating wheather the read page is still dirty.
     * @param cCallBack {@link EventSync} object, the user can block on it till the page data arrives
     * @param baPage byte array where the page data should arrive when the request is proceeded
     * @throws CodedException thrown on error
     * @return byte array where the page data should arrive when the request is proceeded. can be different from the baPage parameter!
     * ( only if the page is still in the sorting buffer )
     */
    public byte[] readPage(int iPageNum, ObjHolder bStillDirty, EventSync cCallBack, byte[] baPage) throws CodedException;

    /** adds a new write request to the sorting buffer
     * @param iPageNum number of the page to be written
     * @param baPage byte array containing the page data
     * @param bSureWrite ensures, that the page will be written, not depending on further read requests
     * @throws CodedException thrown on error
     */
    public void   writePage(int iPageNum, byte[] baPage, boolean bSureWrite) throws CodedException;

    /** returns the count of the currently buffered pages
     * @return the count of the currently buffered pages
     */
    public int getBufferedCount();
}
