package db.systembuffer;


import db.com.EventSync;
import jx.db.CodedException;


/** The PageBuffer interface contains functions for using the SystemBuffer layer of JXDB
 */
public interface PageBuffer {
    /** error code for a CodedException. Thrown if the user requests an invalid page number
     */
    public static final int ERR_INVALID_PAGE_NUM = 0;

    /** this function fixes a page in the page buffer. A fix guaranties, that that page will not be paged out before unfix is called for it.
     * The function returns immediately, if the requestet page was not in the buffer, the returned array will probably contain invalid
     * data, until the page arrives
     * @param iPageNum number of the page to be fixed
     * @param cCallBack call back object, the user can block on it until the page contents arrive from disk
     * @throws CodedException thrown on error ( wrong page number, no place in the buffer, etc )
     * @return byte array containing the page. If cCallBack is signalled, the byte array contains valid information, otherwise the user should wait.
     */
    public byte[] fix(int iPageNum, EventSync cCallBack) throws CodedException;
    /** this function fixes a page in the page buffer. A fix guaranties, that that page will not be paged out before unfix is called for it.
     * The function does the same as fix, but blocks the caller until the requested page arrives in the buffer
     * @param iPageNum number of the page to be fixed
     * @throws CodedException thrown on error ( wrong page number, no place in the buffer, etc )
     * @return byte array containing the page data
     */
    public byte[] fixSync(int iPageNum) throws CodedException;
    /** this function decreases the fix count of a page. If the count becomes 0, the page may be paged out.
     * @param iPageNum number of the page to be unfixed
     * @throws CodedException thrown on error ( invalid page number, page not fixed, etc. )
     */
    public void   unfix(int iPageNum) throws CodedException;
    /** this function marks a page as modified. All modified pages will be written on disk, when paged out.
     * @param iPageNum number of the page to be marked
     * @throws CodedException thrown on error ( wrong page number, etc. )
     */
    public void   setDirty(int iPageNum) throws CodedException;
    /** returns the page size for this buffer
     * @throws CodedException thrown on error
     * @return page size
     */
    public int    getPageSize()              throws CodedException;
    /** this function is used by the sorting buffer to return page frames to the buffer.
     * @param baPage page frame, that has to be returned
     */
    public void giveBackPage( byte[] baPage );
    /** returns a page manager object for this buffer
     * @return the requested page manager
     */
    public PageManager getPageManager();

    /** flushes all modified pages to the persistent media
     */
    public void flush() throws CodedException;
}
