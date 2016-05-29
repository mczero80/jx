package db.tid;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

import db.systembuffer.PageBuffer;
import jx.db.CodedException;


/** a class that references a subset of a byte array or PageBuffer page.
 */
public class Set {
    private byte[]     m_baPage = null;
    private int        m_iPageNum = -1;
    private int        m_iOffset = -1;
    private int        m_iLength = -1;
    private PageBuffer m_cPageBuf = null;

    protected Set(byte[] baPage, int iOffset, int iLength, int iPageNum,
        PageBuffer cPageBuf) {
        m_baPage = baPage;
        m_iOffset = iOffset;
        m_iLength = iLength;
        m_iPageNum = iPageNum;
        m_cPageBuf = cPageBuf;
    }

    /** creates a new Set object
     * @param baPage byte array to be referenced
     * @param iOffset offset in the byte array, where the Set data begins
     * @param iLength lingth of the Set data
     */
    public Set(byte[] baPage, int iOffset, int iLength) {
        m_baPage = baPage;
        m_iOffset = iOffset;
        m_iLength = iLength;
    }

    /** sets additional info about the set( the PageBuffer object containing the page, page number )
     * @param cPageBuf the PageBuffer object containing the Set page
     * @param iPageNum page number in the PageBuffer
     */
    public void setPageInfo(PageBuffer cPageBuf, int iPageNum) {
        m_cPageBuf = cPageBuf;
        m_iPageNum = iPageNum;
    }

    /** sets the offset of the Set in the page
     * @param iOffset offset in the page
     */
    public void setOffset(int iOffset) {
        m_iOffset = iOffset;
    }

    /** returns the offset in the page
     * @return offset in the page
     */
    public int getOffset() {
        return m_iOffset;
    }

    /** sets the length of the Set data
     * @param iLength length of the Set data
     */
    public void setLength(int iLength) {
        m_iLength = iLength;
    }

    /** returns the length of the Set data
     * @return length of the Set data
     */
    public int getLength() {
        return m_iLength;
    }

    /** returns the byte array containing the data
     * @return byte array containing the data
     */
    public byte[] getBytes() {
        return m_baPage;
    }

    /** sets the byte array containing the data
     * @param baPage byte array containing the data
     */
    public void setBytes(byte[] baPage) {
        m_baPage = baPage;
    }

    /** calls the unfix function in the page buffer for the page containing the set
     * @throws CodedException thrown on error ( see {@link PageBuffer}, unfix )
     */
    public void unfix() throws CodedException {
        if (m_iPageNum != -1)
            m_cPageBuf.unfix(m_iPageNum);
    }

    /** calls the setDirty function in the page buffer for the page containing the set
     * @throws CodedException thrown on error ( see {@link PageBuffer}, setDirty )
     */
    public void setDirty() throws CodedException {
        if (m_iPageNum != -1)
            m_cPageBuf.setDirty(m_iPageNum);
    }
}
