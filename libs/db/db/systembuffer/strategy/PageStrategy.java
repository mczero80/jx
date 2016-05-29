package db.systembuffer.strategy;


import jx.db.CodedException;


/** Classes, that implement paging functionality have to implement the PageStrategy interface.
 */
public interface PageStrategy {

    /** error code, thrown if all pages in the buffer are fixed and the getBestForPageout function is called.
     *
     */
    public static final int ERR_ALL_PAGES_FIXED = 0;
    /** error code, thrown if the requested page is not currently paged in
     */
    public static final int ERR_INVALID_PAGE_NUM = 1;
    /** error code, thrown if a pageIn is requested for a page that is already paged in.
     */
    public static final int ERR_PAGE_EXISTS = 2;
    /** error code, thrown if unfix is called for a non fixed page
     */
    public static final int ERR_PAGE_NOT_FIXED = 4;
    /** error code, thrown if getBestForPageout is called and no pages are currently paged in
     */
    public static final int ERR_EMPTY_BUFFER = 5;
    /** error code, thrown if setDirty is called for a non fixed page
     */
    public static final int ERR_INVALID_FIX = 6;

    /** tells the page strategy to increment the fix counter of the page
     * pages with fix counter > 0 can not be paged out
     * @param iPageNum number of the page to be fixed
     * @throws CodedException thrown on error ( invalid page number, etc. )
     * @return {@link PageStrategyRecord }, that contains the information about the page, managed by the {@link PageStrategy}.
     */
    public PageStrategyRecord fix(int iPageNum) throws CodedException;
    /** tells the strategy to decrement the fix counter of a page. If the fix count becomes 0, the page may be paged out
     * @param iPageNum number of the page to be unfixed
     * @throws CodedException thrown on error ( ERR_PAGE_NOT_FIXED, etc. )
     */
    public void unfix(int iPageNum) throws CodedException;
    /** marks a page as modified.
     * @param iPageNum number of the page to be marked
     * @throws CodedException thrown on error ( wrong page number, etc )
     */
    public void setDirty(int iPageNum)throws CodedException;

    /** inserts a new page in the strategy management structures
     * @param iPageNum number of a page to be inserted
     * @param baPage byte array, containing the page data. The strategy should not modify this array.
     * @throws CodedException thrown on error ( wrong page number, page already paged in, etc. )
     */
    public void pageIn(int iPageNum, byte[] baPage) throws CodedException;
    /** removes a page from the strategy
     * @param iPageNum number of the page to be removed
     * @throws CodedException thrown on error ( wrong page number, page fixed, etc. )
     * @return {@link PageStrategyRecord }, that contains the information about the page, managed by the {@link PageStrategy}.
     */
    public PageStrategyRecord pageOut(int iPageNum) throws CodedException;
    /** copies the data structures of this page strategy into another page strategy
     * @param cDest destination {@link PageStrategy} object
     * @throws CodedException thrown on error
     */
    public void copy(PageStrategy cDest)throws CodedException;
    /** uses the page strategy rules to determine the best candidate for paging out
     * @throws CodedException thrown on error ( all pages fixed, etc. )
     * @return number of the best page
     */
    public int getBestForPageout() throws CodedException;
    /** gets the {@link PageStrategyRecord} object for a page.
     * @param iPageNum number of the requested page
     * @throws CodedException thrown on error ( invalid page number, etc. )
     * @return {@link PageStrategyRecord }, that contains the information about the page, managed by the {@link PageStrategy}.
     */
    public PageStrategyRecord getPageRecord(int iPageNum)throws CodedException;
    /** returns the count of the currently paged in pages
     * @throws CodedException thrown on error
     * @return count of all paged in pages
     */
    public int getPagesAllocated() throws CodedException;
}
