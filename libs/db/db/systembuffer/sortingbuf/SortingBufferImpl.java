package db.systembuffer.sortingbuf;


import java.util.*;
import db.com.*;
import jx.bio.BlockIO;
import db.systembuffer.*;
import db.systembuffer.firststage.FirstStageBuf;
import db.list.List;
import jx.db.CodedException;

import jx.zero.*;


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**
 * A class used to sort disk IO requests
 *
 * @author Ivan Dedinski
 * @see db.systembuffer.bio.BlockIO
 * @see db.systembuffer.firststage.FirstStageBuf
 */
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class SortingBufferImpl implements SortingBuffer {

    private final static int ERR_WRONG_SEQ = 0;

    private BlockIO              m_cBio;
    private db.treemap.TreeMap   m_cSortMap;
    private Hashtable            m_cNotifyHash;
    private PageBuffer           m_cPageBuf = null;
    private Memory               m_cBuffer  = null;

    private int m_iBlocksPerPage = 8;
    private IOThread m_cIOThread = null;

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * A private class that represents the info needed by SortingBufferImpl to manage the requests
     *
     */
    private class SBPageRecord {
        private byte[]  m_baPage;
        private boolean m_bSureProceed;
        private boolean m_bRead;

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * Constructor
         *
         * @param baPage byte array of the page stored
         * @param bSureProceed if true and the request is a write request, the page will be written on the media for sure
         * @param bRead true, if the request is a read request, false otherwise
         */
        public SBPageRecord(byte[] baPage, boolean bSureProceed, boolean bRead) {
            m_baPage = baPage;
            m_bSureProceed = bSureProceed;
            m_bRead = bRead;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * gets the page associated with that record
         * @return byte array of the page
         */
        public byte[] getPage() {
            return m_baPage;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * associates a new page with this record
         * @param baPage byte array of the page
         */
        public void setPage(byte[] baPage) {
            m_baPage = baPage;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * gets the bSureProceed flag
         */
        public boolean getSureProceed() {
            return m_bSureProceed;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * sets the bSureProceed flag
         */
        public void setSureProceed(boolean bValue) {
            m_bSureProceed = bValue;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * gets the bRead flag
         */
        public boolean getRead() {
            return m_bRead;
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////
    private class IOThread extends Thread {
        
        public IOThread() {	   
            setDaemon(true);
            start();
        }

        public void run() {
            ObjHolder pcNotifyList = new ObjHolder();
            ObjHolder pcPageNum = new ObjHolder();

            //System.out.println("Starting the IO Thread!");
            while (true) {
                SBPageRecord cRec = null;

                try {
                    cRec = getPageToProceed(pcNotifyList, pcPageNum);
                }catch (Exception e) {
                    e.printStackTrace();
                }
                Integer cPageNum = (Integer) pcPageNum.getObj();

                if (cRec.getRead() == true) {
                    List cList = (List) pcNotifyList.getObj();
		   
                    m_cBio.readSectors(cPageNum.intValue() * m_iBlocksPerPage, m_iBlocksPerPage, m_cBuffer, true);
		    m_cBuffer.copyToByteArray( cRec.getPage(), 0, 0, 512 * m_iBlocksPerPage ); 

                    if (cList != null) {
                        //Iterator i = cList.iterator();
                        if (cList.moveToFirst()) {
                            do {
                                try {
                                    EventSync cEvent = (EventSync) cList.getCurrent();

                                    cEvent.setEvent();
                                }catch (CodedException cex) {
                                    cex.printStackTrace();
                                }
                                // System.out.println("IOThread: succesfully sync in Page" + cPageNum.intValue());
                            }
                            while (cList.moveToNext());
                        }
                    }
                }else {
                    if (Globals.DEBUG) {
                        //System.out.println("IOThread: writing out Page" + cPageNum.intValue());
                    }
		    m_cBuffer.copyFromByteArray( cRec.getPage(), 0, 0, 512 * m_iBlocksPerPage ); 
                    m_cBio.writeSectors(cPageNum.intValue() * m_iBlocksPerPage, m_iBlocksPerPage, m_cBuffer, true);
                    m_cPageBuf.giveBackPage(cRec.getPage());
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////

    /*************************************************************************************************************************************/
    public SortingBufferImpl(BlockIO cBio) {
        m_cBio = cBio;
        m_iBlocksPerPage = Globals.BLOCKS_PER_PAGE;

	MemoryManager memMgr = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
	m_cBuffer = memMgr.alloc( m_iBlocksPerPage * 512 );

        if (!Globals.SINGLE_TREADED) {
            m_cSortMap = new db.treemap.TreeMap();
            m_cNotifyHash = new Hashtable();
            m_cIOThread = new IOThread();
        }

    }

    /*************************************************************************************************************************************/
    public synchronized byte[] readPage(int iPageNum, ObjHolder bStillDirty, EventSync cCallBack, byte[] baPage) throws CodedException {

        if (!Globals.SINGLE_TREADED) {
            Integer cKey = new Integer(iPageNum);

            if (m_cSortMap.containsKey(cKey)) { //the page is already mentioned in the buffer ...
                SBPageRecord cRec = (SBPageRecord) m_cSortMap.get(cKey);

                if (cRec.getRead() == true) { // ... as a read request
                    List cList = (List) m_cNotifyHash.get(cKey);

                    if (cList != null) {
                        cList.insert(cCallBack);
                    }else {
                        cList = new List();
                        cList.insert(cCallBack);
                        m_cNotifyHash.put(cKey, cList);
                    }
                    bStillDirty.setObj(Boolean.FALSE);
                    m_cPageBuf.giveBackPage(baPage);
                    return cRec.getPage();
                } else {
                    if (cRec.getSureProceed() == true) {
                        bStillDirty.setObj(Boolean.FALSE);
                        byte[] srcPage = cRec.getPage();

                        System.arraycopy(srcPage, 0, baPage, 0, baPage.length);
                    } else {
                        bStillDirty.setObj(Boolean.TRUE);
                        m_cSortMap.remove(cKey);
                        m_cPageBuf.giveBackPage(baPage);
                        baPage = cRec.getPage();
                    }

                    cCallBack.setNeedToWait(false);

                    return baPage;
                }
            }else {
                SBPageRecord cRec = new SBPageRecord(baPage, true, true);

                m_cSortMap.put(cKey, cRec);

                List cList = new List();

                cList.insert(cCallBack);

                m_cNotifyHash.put(cKey, cList);
                bStillDirty.setObj(Boolean.FALSE);

                notify();

                return cRec.getPage();
            }
        }else {
	    m_cBio.readSectors(iPageNum * m_iBlocksPerPage, m_iBlocksPerPage, m_cBuffer, true);
	    m_cBuffer.copyToByteArray( baPage, 0, 0, 512 * m_iBlocksPerPage ); 

            cCallBack.setNeedToWait(false);
            bStillDirty.setObj(Boolean.FALSE);

            return baPage;
        }
    }

    /*************************************************************************************************************************************/
    public synchronized void writePage(int iPageNum, byte[] baPage, boolean bSureWrite) throws CodedException {

        if (!Globals.SINGLE_TREADED) {
            Integer cKey = new Integer(iPageNum);

            if (m_cSortMap.containsKey(cKey)) {
                SBPageRecord cRec = (SBPageRecord) m_cSortMap.get(cKey);

                if (cRec.getRead() == true)
                    throw new CodedException(this, ERR_WRONG_SEQ, "Wrong Read/Write Sequence!");
                m_cPageBuf.giveBackPage(cRec.getPage());
                cRec.setPage(baPage);
                if (bSureWrite == true)
                    cRec.setSureProceed(true);
            }else {
                SBPageRecord cRec = new SBPageRecord(baPage, bSureWrite, false);

                m_cSortMap.put(cKey, cRec);
                notify();
            }

        }else {
	    m_cBuffer.copyFromByteArray( baPage, 0, 0, 512 * m_iBlocksPerPage ); 
	    m_cBio.writeSectors(iPageNum * m_iBlocksPerPage, m_iBlocksPerPage, m_cBuffer, true);
            m_cPageBuf.giveBackPage( baPage );
        }
    }

    /*************************************************************************************************************************************/
    public synchronized void flushPage(int iPageNum, EventSync cCallBack) throws CodedException {
        return;
    }

    /*************************************************************************************************************************************/
    private synchronized SBPageRecord getPageToProceed(ObjHolder cNotifyList, ObjHolder cPageNum) throws CodedException {
        do {
            Integer cKey = null;

            if (m_cSortMap.isEmpty()) {
                try {
                    wait();
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
                continue;
            }

            try {
                cKey = (Integer) m_cSortMap.firstKey();
            }catch (Exception e) {
                e.printStackTrace();
            }

            SBPageRecord cRec = (SBPageRecord) m_cSortMap.get(cKey);

            cNotifyList.setObj(m_cNotifyHash.get(cKey));
            cPageNum.setObj(cKey);
            m_cSortMap.remove(cKey);
            m_cNotifyHash.remove(cKey);
            return cRec;
        }
        while (true);
    }

    public synchronized void  setPageBuffer(PageBuffer cPageBuf) {
        m_cPageBuf = cPageBuf;
    }

    /*************************************************************************************************************************************/

    /** returns the count of the currently buffered pages
     * @return the count of the currently buffered pages
     */
    public synchronized int getBufferedCount(){
	return m_cSortMap.getCount();
    }
    
}
