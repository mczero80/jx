package db.systembuffer.strategy.lru;


//import java.util.Hashtable;
import db.systembuffer.strategy.*;
import db.systembuffer.*;
import db.com.*;
import db.list.*;
import db.collections.*;
import jx.db.CodedException;


/** Implements the LRU Page Strategy, combined with some heuristics
 * Each page is assigned a priority, dirty flag and reference number
 * All three numbers, weighted by factors specified in the class
 * build up the order key in the remove queue.The smaller the key, the bigger
 * the chance for the page to be removed by the next swap operation.
 * The reference number is increased each time when the page is referenced, and the queue is adjusted
 *
 */
public class LRUPageStrategy implements PageStrategy {

    private BiList       m_cLRUList = null;
    private DBHashtable  m_cPageHash = null;
    private int          m_iDirtyWeight = 0;
    private int          m_iPagesCount = 0;

    //////////////////////////////////////////////////////////////////////////////
    private class LRUPageRecord implements PageStrategyRecord {
        private Object m_cListRef = null;

        private boolean m_bDirtyFlag = false;
    private int     m_iFixCnt = 0;

    private int     m_iPageNum = -1;
    private byte[]  m_baPage = null;

    ////////////////////////////////////////////////////////////////////////////

    protected void setFixCnt(int iValue) {
        m_iFixCnt = iValue;
    }

    /** implemented function, see {@link PageStrategyRecord }
     */
    public int getFixCnt() {
        return m_iFixCnt;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setDirtyFlag(boolean bValue) {
        m_bDirtyFlag = bValue;
    }

    /** implemented function, see {@link PageStrategyRecord }
     */
    public boolean getDirtyFlag() {
        return m_bDirtyFlag;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link PageStrategyRecord }
     */
    public int getPageNum() {
        return m_iPageNum;
    }

    protected void setPageNum(int iPageNum) {
        m_iPageNum = iPageNum;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setPage(byte[] baPage) {
        m_baPage = baPage;
    }

    /** implemented function, see {@link PageStrategyRecord }
     */
    public byte[] getPage() {
        return m_baPage;
    }
    ////////////////////////////////////////////////////////////////////////////

    /** Creates a new {@link LRUPageRecord} object
     * @param iPageNum page number
     * @param baPage byte array containing the page data. Should not be modified in this class
     */
        public LRUPageRecord(int iPageNum, byte[] baPage) {
            m_iPageNum = iPageNum;
            m_baPage = baPage;
        }

        protected void setListRef(Object cListRef) {
            m_cListRef = cListRef;
        }

        /** returns a {@link BiList} reference object, that allows fast positioning in the {@link BiList}.
         * The function is used only by LRUPageStrategy
         * @return {@link BiList} reference object
         */
        protected Object getListRef() {
            return m_cListRef;
        }
    }
    //////////////////////////////////////////////////////////////////////////////
    /** Constructs a new LRUPageStrategy object
     * @param iHashSize hash size of the table containing all {@link PageStrategyRecord} objects
     * @param iDirtyWeight weight of the dirty flag, when choosing a page for pageout. Paging out a dirty page is much more inefficient than paging out just a non dirty one.
     * All dirty pages must be written to disk, non dirty can be just discarded. Otherwise it could be better to page out a modified but seldom used page,
     * than a non modified and frequently used one. iDirtyWeight is a measure for that.
     */
    public LRUPageStrategy(int iHashSize, int iDirtyWeight) {
        m_cLRUList = new BiList();
        m_cPageHash = new DBHashtable(iHashSize, null);
        m_iDirtyWeight = iDirtyWeight;
        m_iPagesCount = 0;
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @param iCount
     * @throws CodedException
     */
    public PageStrategyRecord fix(int iPageNum) throws CodedException {

        LRUPageRecord cRecord = (LRUPageRecord) m_cPageHash.get(iPageNum);

        if (cRecord == null)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM,
                    "Page not registered in the buffer! Page Num :" + iPageNum);

        int iFixCnt = cRecord.getFixCnt();

        cRecord.setFixCnt(iFixCnt + 1);

        if (iFixCnt == 0 && cRecord.getListRef() != null) {
            m_cLRUList.setPos(cRecord.getListRef());
            m_cLRUList.removeCurrent();
        }

        return cRecord;
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @throws CodedException
     */
    public void unfix(int iPageNum) throws CodedException {
        LRUPageRecord cRecord = (LRUPageRecord) m_cPageHash.get(iPageNum);

        if (cRecord == null)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Page not registered in the buffer! Page Num :" + iPageNum);

        if (cRecord.getFixCnt() > 0) {
            int iFixCnt = cRecord.getFixCnt() - 1;

            if (iFixCnt < 0)
                throw new CodedException(this, ERR_PAGE_NOT_FIXED, "Page not fixed! Page Num :" + iPageNum);
            cRecord.setFixCnt(iFixCnt);
            if (iFixCnt == 0)
                cRecord.setListRef(m_cLRUList.insertAtStart(cRecord));

        }
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @throws CodedException
     */
    public void setDirty(int iPageNum) throws CodedException {
        LRUPageRecord cRecord = (LRUPageRecord) m_cPageHash.get(iPageNum);

        if (cRecord == null)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Page not registered in the buffer! Page Num :" + iPageNum);

        if (cRecord.getFixCnt() == 0)
            throw new CodedException(this, ERR_INVALID_FIX, "Page not fixed, cant set dirty! Page Num :" + iPageNum);

        cRecord.setDirtyFlag(true);
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @throws CodedException
     */
    public void pageIn(int iPageNum, byte[] baPage) throws CodedException {

        LRUPageRecord cRecord = (LRUPageRecord) m_cPageHash.get(iPageNum);

        if (cRecord != null)
            throw new CodedException(this, ERR_PAGE_EXISTS, "Page already registered in the buffer! Page Num :" + iPageNum);

        cRecord = new LRUPageRecord(iPageNum, baPage);
        m_cPageHash.put(iPageNum, cRecord);
        m_iPagesCount++;
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @throws CodedException
     */
    public PageStrategyRecord pageOut(int iPageNum) throws CodedException {

        LRUPageRecord cRecord = (LRUPageRecord) m_cPageHash.get(iPageNum);

        if (cRecord == null)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Page not registered in the buffer! Page Num :" + iPageNum);

        m_cLRUList.setPos(cRecord.getListRef());
        m_cLRUList.removeCurrent();
        m_cPageHash.remove(iPageNum);

        m_iPagesCount--;
        return cRecord;
    }

    /** implemented function, see {@link PageStrategy }
     * @param cDest
     * @throws CodedException
     */
    public void copy(PageStrategy cDest) throws CodedException {
        throw new CodedException(this, -1, "Not Implemented!");
    }

    /** implemented function, see {@link PageStrategy }
     * @throws CodedException
     * @return
     */
    public int getBestForPageout() throws CodedException {

        if (m_cLRUList.isEmpty())
            throw new CodedException(this, ERR_ALL_PAGES_FIXED, "All pages fixed!");

        LRUPageRecord cRecord;

        m_cLRUList.moveToEnd();
        for (int iCnter = 0; iCnter < m_iDirtyWeight; iCnter++) {
            cRecord = (LRUPageRecord) m_cLRUList.getCurrent();
            if (!cRecord.getDirtyFlag()) {
                return cRecord.getPageNum();
            }
            if (!m_cLRUList.moveToPrev())
                break;
        }

        m_cLRUList.moveToEnd();
        cRecord = (LRUPageRecord) m_cLRUList.getCurrent();
        return cRecord.getPageNum();
    }

    /** implemented function, see {@link PageStrategy }
     * @param iPageNum
     * @throws CodedException
     * @return
     */
    public PageStrategyRecord getPageRecord(int iPageNum) throws CodedException {
        return (LRUPageRecord) m_cPageHash.get(iPageNum);
    }

    /** implemented function, see {@link PageStrategy }
     * @return
     */
    public int getPagesAllocated() {
        return m_iPagesCount;
    }

}
