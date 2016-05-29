/*
 * MemoryBuffer.java
 *
 * Created on 24. Oktober 2001, 15:10
 */

package db.systembuffer.memorybuf;


import db.systembuffer.*;
import db.com.*;
import java.util.*;
import jx.db.CodedException;


/** A class implementing the PageBuffer interface. It does not use a disk device to store the pages, but the RAM.
 *
 * @author Ivan Dedinski
 * @version 1.0
 */
public class MemoryBuffer implements db.systembuffer.PageBuffer {

    //MemoryManager     m_cMemoryMgr = null;
    Hashtable         m_cPageHash = null;
    MemoryPageManager m_cPageMgr = null;

    /** Creates new MemoryBuffer */
    public MemoryBuffer(/*MemoryManager cMemMgr*/) {
      //  m_cMemoryMgr = cMemMgr;
        m_cPageHash = new Hashtable();
    }

    /** returns a {@link PageManager} object for this PageBuffer
     * @return {@link PageManager} object for this PageBuffer
     */
    public PageManager getPageManager() {
        if (m_cPageMgr == null)
            m_cPageMgr = new MemoryPageManager();

        return m_cPageMgr;
    }

    /** sets memory size of the virtual buffer device
     * @param lPageCount count in pages
     * @throws CodedException thrown on error ( not enough memory, etc. )
     */
    public void setBufferSize(int lPageCount) throws CodedException {
    }

    /** implemented function, see {@link PageBuffer }
     * @param iPageNum
     * @throws CodedException
     */
    public void setDirty(int iPageNum) throws CodedException {
        return;
    }

    /** implemented function, see {@link PageBuffer }
     * @param iPageNum
     * @throws CodedException
     */
    public void unfix(int iPageNum) throws CodedException {
        return;
    }

    /*public void setPriority(int iPageNum, int iPriority) throws PageException {
     }*/



    /** implemented function, see {@link PageBuffer }
     * @param iPageNum
     * @param cCallBack
     * @throws CodedException
     * @return
     */
    public byte[] fix(int iPageNum, EventSync cCallBack) throws CodedException {
        if (cCallBack != null)
            cCallBack.setNeedToWait(false);

        PageHolder cPage = (PageHolder) m_cPageHash.get(new Integer(iPageNum));

        if (cPage != null)
            return cPage.getBytes();
        else
            throw new CodedException(this, ERR_INVALID_PAGE_NUM, "Page doesn't exist!");
    }

    /** implemented function, see {@link PageBuffer }
     * @param iPageNum
     * @throws CodedException
     * @return
     */
    public byte[] fixSync(int iPageNum) throws CodedException {
        return fix(iPageNum, null);
    }

    /** implemented function, see {@link PageBuffer }
     * @throws CodedException
     * @return
     */
    public int getPageSize() throws CodedException {
        return 4096;
    }

    private class MemoryPageManager implements db.systembuffer.PageManager {
        private int m_iNextPageNum = 0;

        /** implemented function, see {@link PageManager }
         * @param iPageNum
         * @throws CodedException
         */
        public void freePage(int iPageNum) throws CodedException {
            /*PageHolder cPage = (PageHolder) m_cPageHash.get(new Integer(iPageNum));

            if (cPage != null)
                m_cMemoryMgr.addPage(cPage.getBytes());*/

           m_cPageHash.remove( new Integer( iPageNum ));
        }

        /** implemented function, see {@link PageManager }
         * @throws CodedException
         */
        public void formatMedia() throws CodedException {
            return;
        }

        /** implemented function, see {@link PageManager }
         * @param iPageNum
         * @throws CodedException
         * @return
         */
        public boolean isPageUsed(int iPageNum) throws CodedException {
            if (m_cPageHash.get(new Integer(iPageNum)) == null)
                return false;

            return true;
        }

        /** implemented function, see {@link PageManager }
         * @throws CodedException
         * @return
         */
        public int getBlankPage() throws CodedException {
            byte[] baPage = null;

            baPage = new byte[ 4096 ];

            if (baPage == null)
                throw new CodedException(this, ERR_NO_FREE_PAGE, "No free memory pages left!");

            m_cPageHash.put(new Integer(m_iNextPageNum), new PageHolder(baPage));
            m_iNextPageNum++;
            return m_iNextPageNum - 1;
        }
    }

    /** implemented function, see {@link PageBuffer }
     * @param baPage
     */
    public void giveBackPage( byte[] baPage ){
      return;
    }


    public void flush() throws CodedException{
	return; //tut nix
    }

}
