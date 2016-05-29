package db.systembuffer.collections;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

import db.com.Converter;
import jx.db.CodedException;
import jx.db.BufferList;
import db.systembuffer.PageBuffer;
import db.tid.Set;


/** A simple container for temporary storage. Use it if you want to store a large amount of sets in a page buffer.
 * The sets should be of the same length
 */
public class BufferListImpl implements BufferList {
    /** error code, thrown if the BufferList is empty and the user tries to navigate through the sets
     */
    public static final int ERR_LIST_EMPTY = 0;

    private PageBuffer m_cPageBuf = null;
    private int        m_iDataSize = 0;
    private int        m_iFirstPageNum = -1;
    private byte[]     m_baCurrentPage = null;
    private int        m_iCurrentPageNum = -1;
    private int        m_iOffsetInPage = 0;
    private int        m_iLastPageNum = -1;
    private int        m_iLastOffset = -1;
    //private Set        m_cTmpSet = null;
    private byte[]     m_baTmpBuffer = null;

    /** constructs a new BufferList object
     * @param cPageBuf {@link PageBuffer} object used to store the data.
     * @param iDataSize size of the data sets
     * @throws CodedException thrown on error ( empty buffer, etc. )
     */
    public BufferListImpl(PageBuffer cPageBuf, int iDataSize) throws CodedException {
        m_cPageBuf = cPageBuf;
        m_iDataSize = iDataSize;
        m_iFirstPageNum = m_cPageBuf.getPageManager().getBlankPage();
        m_iCurrentPageNum = m_iLastPageNum = m_iFirstPageNum;
        m_iOffsetInPage = 0;
        m_iLastOffset = -1;
        m_baCurrentPage = m_cPageBuf.fixSync(m_iCurrentPageNum);
        //m_cTmpSet = new Set(null, -1, -1);
        m_baTmpBuffer = new byte[ iDataSize ];

        //mark that page, to be the last one
        Converter.intToBytes(-1, m_baCurrentPage, m_baCurrentPage.length - 5);
    }

    /** moves the position to the beginning of the BufferList
     * @throws CodedException thrown on error
     * @return thrue if successful, false otherwise
     */
    public boolean moveToFirst()  throws CodedException {
        if (m_iLastOffset == -1) //List empty
            return false;

        m_cPageBuf.setDirty(m_iCurrentPageNum);
        m_cPageBuf.unfix(m_iCurrentPageNum);
        m_baCurrentPage = m_cPageBuf.fixSync(m_iFirstPageNum);
        m_iCurrentPageNum = m_iFirstPageNum;
        m_iOffsetInPage = 0;

        return true;
    }

    /** moves the position to the next element in the buffer
     * @throws CodedException thrown on error
     * @return true if successful, false otherwise ( end reached )
     */
    public boolean moveToNext() throws CodedException {
        if (m_iCurrentPageNum == m_iLastPageNum && m_iOffsetInPage >= m_iLastOffset) //end reached
            return false;

        if ((m_iOffsetInPage + m_iDataSize * 2) < (m_baCurrentPage.length - 4)) {
            m_iOffsetInPage += m_iDataSize;
            return true;
        }else {
            int iNextPage = Converter.bytesToInt(m_baCurrentPage, m_baCurrentPage.length - 5);

            m_cPageBuf.setDirty(m_iCurrentPageNum);
            m_cPageBuf.unfix(m_iCurrentPageNum);
            m_baCurrentPage = m_cPageBuf.fixSync(iNextPage);
            m_iCurrentPageNum = iNextPage;
            m_iOffsetInPage = 0;
            return true;
        }
    }

    /** inserts a new set at the end of the buffer
     * @throws CodedException thrown on error ( no place left, etc. )
     * @return a Set object allowing modifications on the added set
     */
    public void addAtEnd( byte[] baData, int iOffset ) throws CodedException {
        if (m_iLastOffset < 0) {
            m_iLastOffset = 0;
            System.arraycopy( baData, iOffset, m_baCurrentPage, m_iOffsetInPage, m_iDataSize );
        }

        if ((m_iLastOffset + m_iDataSize * 2) < (m_baCurrentPage.length - 4)) {
            m_iLastOffset += m_iDataSize;
            if (m_iCurrentPageNum != m_iLastPageNum) {
                m_cPageBuf.setDirty(m_iCurrentPageNum);
                m_cPageBuf.unfix(m_iCurrentPageNum);
                m_baCurrentPage = m_cPageBuf.fixSync(m_iLastPageNum);
            }
            m_iOffsetInPage = m_iLastOffset;
        }else {
            int iNextPage = m_cPageBuf.getPageManager().getBlankPage();

            if (m_iCurrentPageNum == m_iLastPageNum) {
                Converter.intToBytes(iNextPage, m_baCurrentPage, m_baCurrentPage.length - 5);
            }else {
                byte[] baPage = m_cPageBuf.fixSync(m_iLastPageNum);

                Converter.intToBytes(iNextPage, baPage, baPage.length - 5);
                m_cPageBuf.setDirty(m_iLastPageNum);
                m_cPageBuf.unfix(m_iLastPageNum);
            }
            m_cPageBuf.setDirty(m_iCurrentPageNum);
            m_cPageBuf.unfix(m_iCurrentPageNum);
            m_iCurrentPageNum = iNextPage;
            m_baCurrentPage = m_cPageBuf.fixSync(m_iCurrentPageNum);
            m_iOffsetInPage = m_iLastOffset = 0;
            m_iLastPageNum = m_iCurrentPageNum;
            Converter.intToBytes( 0, m_baCurrentPage, m_baCurrentPage.length - 5 );
        }

        System.arraycopy( baData, iOffset, m_baCurrentPage, m_iOffsetInPage, m_iDataSize );
    }

    /** returns a reference to the set at the current position
     * @throws CodedException thrown on error ( empty buffer, read error, etc.)
     * @return a Set object allowing modifications on the current set
     */
    public byte[] getCurrent() throws CodedException {
        if (m_iLastOffset < 0)
            throw new CodedException(this, ERR_LIST_EMPTY, "The list is empty");

        System.arraycopy( m_baCurrentPage, m_iOffsetInPage, m_baTmpBuffer, 0, m_iDataSize );

        return m_baTmpBuffer;
    }

    /** deletes all buffer contents
     * @throws CodedException thrown on error ( read/write error, etc )
     */
    public void delete() throws CodedException {
        m_cPageBuf.unfix(m_iCurrentPageNum);

        m_baCurrentPage = m_cPageBuf.fixSync(m_iFirstPageNum);
        m_iCurrentPageNum = m_iFirstPageNum;
        int iNextPage;

        do {
            iNextPage = Converter.bytesToInt(m_baCurrentPage, m_baCurrentPage.length - 5);

            m_cPageBuf.unfix(m_iCurrentPageNum);
            m_cPageBuf.getPageManager().freePage(m_iCurrentPageNum);

            if (iNextPage == 0 )
                break;

            m_baCurrentPage = m_cPageBuf.fixSync(iNextPage);
            m_iCurrentPageNum = iNextPage;
        }
        while (true);
    }
}
