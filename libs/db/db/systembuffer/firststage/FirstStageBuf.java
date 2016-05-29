package db.systembuffer.firststage;


import java.util.*;
import db.com.*;
import db.systembuffer.*;
import db.systembuffer.strategy.*;
import db.systembuffer.strategy.lru.*;
import db.systembuffer.sortingbuf.*;
import db.list.BiList;

import jx.db.CodedException;


public class FirstStageBuf implements PageBuffer {

    //error codes of firststage buffer
    public static final int ERR_PAGE_FREED = 1;
    public static final int ERR_PAGE_FIXED = 2;

    private int           m_iBlocksPerPage;
    private int           m_iMaxBufferSize;
    private int           m_iPagesTotal;
    private int           m_iPagedIn = 0;
    private int           m_iPagesInList = 0;
    private int           m_iPagesOutside = 0;

    private PageStrategy  m_cPageStrategy;
    private SortingBuffer m_cSortingBuf;

    private PageManager   m_cPageMgr = null;
    private BiList        m_cPageList = null;
    private boolean       m_bUnfixLockFlag = false; //true, if the lock was released abnormally

    /**************************************************************************************************/

    /**
     *  gets the best candidate to page out and pages it out
     */
    private void pageOut() throws CodedException {
        int iPageNum = m_cPageStrategy.getBestForPageout();
        PageStrategyRecord cRecord = m_cPageStrategy.pageOut(iPageNum);

        m_iPagedIn--;

        byte[] page = cRecord.getPage();

        if (cRecord.getDirtyFlag()) {
            m_iPagesOutside++;
            m_cSortingBuf.writePage(iPageNum, page, false);
        }else {
            m_cPageList.insertAtStart(page); //return the unused page to the memory manager
            m_iPagesInList++;
        }
        return;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /**************************************************************************************************/

    /**
     *  Constructor
     *  @param cBio reference to a {@link BlockIO} object does the low level IO
     *  @param iBufferSize size of the allocated memory in pages
     *  @param iBlocksPerPage harddisk blocks per logical page
     */
    public FirstStageBuf( SortingBuffer cSortingBuf, int iMaxBufferSize, int iPagesTotal) {
        m_iBlocksPerPage = Globals.BLOCKS_PER_PAGE;
        m_iMaxBufferSize = iMaxBufferSize;
        m_iPagesTotal = iPagesTotal;
        m_cSortingBuf = cSortingBuf;
        ((SortingBufferImpl)m_cSortingBuf).setPageBuffer( this );
        m_cPageStrategy = new LRUPageStrategy(1000, 10);
        m_cPageList = new BiList();
    }

    /**************************************************************************************************/

    /**
     *  fixes a buffer page for writing
     *  @param iPageNum number of a page to fix
     *  @return reference to the page memory
     */
    public synchronized byte[] fix(int iPageNum, EventSync cCallBack) throws CodedException {

        if (iPageNum > (m_iPagesTotal - 1) || iPageNum < 0)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Invalid page number ->" + iPageNum);

        PageStrategyRecord cRecord = m_cPageStrategy.getPageRecord(iPageNum);

        byte[] baPage = null;

        if (cRecord == null) {
            if (m_iPagesInList + m_iPagedIn + m_iPagesOutside < m_iMaxBufferSize) {
                baPage = new byte[ m_iBlocksPerPage * 512 ];
                m_iPagedIn++;
            }else {
                if (m_iPagesInList <= 0) {
                    pageOut();
                    while (m_iPagesInList <= 0) {

                        try {
                            wait();
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                baPage = (byte[]) m_cPageList.getCurrent();
                m_cPageList.removeCurrent();
                m_iPagesInList--;
                m_iPagedIn++;

            }

            ObjHolder bStillDirty = new ObjHolder();

            baPage = m_cSortingBuf.readPage(iPageNum, bStillDirty, cCallBack, baPage);

            //fixit - the next 2 could be 1
            m_cPageStrategy.pageIn(iPageNum, baPage);
            m_cPageStrategy.fix(iPageNum);

            if (((Boolean) bStillDirty.getObj()).booleanValue() == true)
                m_cPageStrategy.setDirty(iPageNum);

            return baPage;
        }else {
            baPage = cRecord.getPage();
            m_cPageStrategy.fix( iPageNum );

            cCallBack.setNeedToWait(false); //no need to block up there

            return baPage;

        }
    }
    ////////////////////////////////////////////////////////////////////////////
    public synchronized void unfix(int iPageNum) throws CodedException {
        if (iPageNum > (m_iPagesTotal - 1) || iPageNum < 0)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Invalid page number");

        m_cPageStrategy.unfix(iPageNum);
    }

    /**************************************************************************************************/
    public synchronized void setDirty(int iPageNum) throws CodedException {
        if (iPageNum > (m_iPagesTotal - 1) || iPageNum < 0)
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Invalid page number");

        m_cPageStrategy.setDirty(iPageNum);
    }

    /**************************************************************************************************/
    public void   setBufferSize(int lPageCount) throws CodedException {
         throw new CodedException(this, Globals.ERR_NOT_IMPLEMENTED, "not implemented yet!");
    }

    /**************************************************************************************************/
    public void   setPriority(int iPageNum, int iPriority) throws CodedException {
        throw new CodedException(this, Globals.ERR_NOT_IMPLEMENTED, "not implemented yet!");
    }

    /**************************************************************************************************/
    public synchronized void   setStrategy(PageStrategy cStrategy) throws CodedException {
        m_cPageStrategy.copy(cStrategy);
        m_cPageStrategy = cStrategy;
    }

    /**************************************************************************************************/
    public int getPageSize() {
        return m_iBlocksPerPage * 512;
    }

    /**************************************************************************************************/
    public synchronized byte[] fixSync(int iPageNum) throws CodedException {
        byte[] baRet;
        EventSync e = new EventSync();

        baRet = fix(iPageNum, e);
        if (e.getNeedToWait())
            e.waitForEvent();

        return baRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    public synchronized int getPagesAllocated() throws CodedException {
        int iRet = m_cPageStrategy.getPagesAllocated();

        return iRet;
    }

    ////////////////////////////////////////////////////////////////////////////
    public PageManager getPageManager() {
        if (m_cPageMgr == null)
            m_cPageMgr = new FirstStagePageMgr( this, m_iBlocksPerPage, m_iPagesTotal );

        return m_cPageMgr;
    }
    ////////////////////////////////////////////////////////////////////////////
    public synchronized void giveBackPage( byte[] baPage ){
      m_cPageList.insertAtStart( baPage );
      m_iPagesOutside --;
      m_iPagesInList ++;
      notify();
    }
    ////////////////////////////////////////////////////////////////////////////

    /** flushes all modified pages to the persistent media
     */
    public void flush() throws CodedException {

	try{
	    while(true){
		pageOut();
	    }

	}catch( CodedException cex ){
	    if( cex.getErrorCode() != PageStrategy.ERR_ALL_PAGES_FIXED )
		throw cex; //weiterwerfen
	}
	
	//warten, bis alle pages auf der platte sind
	while( m_cSortingBuf.getBufferedCount() > 0 )
	    try{
		Thread.sleep(100);
	    }catch(Exception ex){
		ex.printStackTrace();
	    }

    }

}
