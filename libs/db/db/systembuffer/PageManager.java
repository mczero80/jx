package db.systembuffer;


import jx.db.CodedException;


/** The interface PageManager contains functions for using the memory management in the SystemBuffer Layer of the JXDB
 */
public interface PageManager {

    /** error code for a CodedException. Thrown if the user attempts to allocate a page and no page is free
     */
    public static final int ERR_NO_FREE_PAGE = 3;

    /** this function allocates a new page in the buffer and marks it as allocated
     * @throws CodedException thrown on error ( no free pages, etc. )
     * @return the number of the allocated page
     */
    public int getBlankPage() throws CodedException;
    /** frees a previously allocated page. The page can be now allocated again.
     * @param iPageNum number of the page to be freed
     * @throws CodedException thrown on error ( page not allocated, wrong page number, etc. )
     */
    public void freePage(int iPageNum) throws CodedException;
    /** checks wheather a page is allocated
     * @param iPageNum number of the page
     * @throws CodedException thrown on error ( wrong page number, etc. )
     * @return true, if allocated, false otherwise
     */
    public boolean isPageUsed(int iPageNum) throws CodedException;
    /** formats the page buffer. Formating means creating of data structures for keeping track of the free and used pages.
     * @throws CodedException thrown on error ( disk errors, etc. )
     */
    public void formatMedia() throws CodedException;

}
