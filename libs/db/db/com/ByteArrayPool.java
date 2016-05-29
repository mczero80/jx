package db.com;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class ByteArrayPool {

    private int m_iCurBufferSize = 0;
    private int m_iArraySize = 0;
    private byte[][] m_aArrayList = null;

    public ByteArrayPool(int iMaxBufferSize, int iArraySize) {
        m_iCurBufferSize = 0;
        m_iArraySize = iArraySize;
        m_aArrayList = new byte[ iMaxBufferSize ][];
    }

    public void addArray(byte[] baArray) {
        if (m_iCurBufferSize < m_aArrayList.length) {
            m_aArrayList[ m_iCurBufferSize ] = baArray;
            m_iCurBufferSize++;
        }
    }

    public byte[] getArray() {
        if (m_iCurBufferSize > 0) {
            m_iCurBufferSize--;
            return m_aArrayList[ m_iCurBufferSize ];
        }else {
            return new byte[ m_iArraySize ];
        }
    }
}
