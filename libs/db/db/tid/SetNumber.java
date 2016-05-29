/*
 * SetNumber.java
 *
 * Created on 27. Juni 2001, 19:46
 */

package db.tid;

import db.com.Converter;

import jx.db.SetNumberReader;
import jx.db.CodedException;

/**
 *
 * @author  ivanich
 * @version
 */
public class SetNumber extends java.lang.Object {

    public static final int ERR_EMPTY_BUFFER = 0;
    private byte[] m_baValue = null;
    private int    m_iOffset = 0;
    private SetNumberReader m_cReader = null;

    /**************************************************************************/
    private class SetNumberReaderImpl implements SetNumberReader{
      //////////////////////////////////////////////////////////////////////////
      public int getPageNumber() throws CodedException {
       return SetNumber.this.getPageNumber();
      }

      //////////////////////////////////////////////////////////////////////////
      public int getTidNumber() throws CodedException {
        return SetNumber.this.getTidNumber();
      }

      //////////////////////////////////////////////////////////////////////////
      public void getSetNumber( byte[] baDest, int iOffset ) throws CodedException {
        if (m_baValue == null)
            throw new CodedException(this, ERR_EMPTY_BUFFER, "No memory allocated for this set number");
        System.arraycopy( m_baValue, m_iOffset, baDest, iOffset, 6 );
      }
    }
    /**************************************************************************/

    public SetNumber() {
    }

    ////////////////////////////////////////////////////////////////////////////
    public SetNumber(byte[] baValue) {
        m_baValue = baValue;
        m_iOffset = 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    public SetNumber(byte[] baValue, int iOffset) {
        m_baValue = baValue;
        m_iOffset = iOffset;
    }

    ////////////////////////////////////////////////////////////////////////////
    public SetNumber(SetNumber cSetNum) {
        copy(cSetNum);
    }

    ////////////////////////////////////////////////////////////////////////////
    public SetNumber(int iPageNum, int iTidNum) {
        m_baValue = new byte[6];
        m_iOffset = 0;
        Converter.intToBytes(iPageNum, m_baValue, 0);
        Converter.shortToBytes(iTidNum, m_baValue, 4);
    }

    ////////////////////////////////////////////////////////////////////////////
    public void setPageNumber(int iPageNumber) {
        if (m_baValue == null) {
            m_baValue = new byte[6];
            m_iOffset = 0;
        }
        Converter.intToBytes(iPageNumber, m_baValue, 0 + m_iOffset);
    }

    ////////////////////////////////////////////////////////////////////////////
    public void copy(SetNumber cSetNum) {
        if (m_baValue == null) {
            m_baValue = new byte[6];
            m_iOffset = 0;
        }
        Converter.moveBytes(m_baValue, 0, cSetNum.m_baValue, cSetNum.m_iOffset, 6);
    }

    ////////////////////////////////////////////////////////////////////////////
    public void copy( SetNumberReader cSetNumReader ) throws CodedException{
      setTidNumber( cSetNumReader.getTidNumber() );
      setPageNumber( cSetNumReader.getPageNumber());
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getOffset() {
        return m_iOffset;
    }

    ////////////////////////////////////////////////////////////////////////////
    public void setTidNumber(int iTidNumber) {
        if (m_baValue == null) {
            m_baValue = new byte[6];
            m_iOffset = 0;
        }
        Converter.shortToBytes(iTidNumber, m_baValue, 4 + m_iOffset);
    }

    ////////////////////////////////////////////////////////////////////////////
    public byte[] getSetNumber() {
        return m_baValue;
    }

    ////////////////////////////////////////////////////////////////////////////
    public void setSetNumber(byte[] baValue) {
        m_baValue = baValue;
        m_iOffset = 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    public void setSetNumber(byte[] baValue, int iOffset) {
        m_baValue = baValue;
        m_iOffset = iOffset;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static int getByteLen() {
        return 6;
    }

    ////////////////////////////////////////////////////////////////////////////
    public boolean equals(SetNumber cSetNum) throws CodedException {
        if ((cSetNum.getReader().getTidNumber() == getReader().getTidNumber()) &&
            (cSetNum.getReader().getPageNumber() == getReader().getPageNumber()))
            return true;

        return false;
    }
    ////////////////////////////////////////////////////////////////////////////

    public SetNumberReader getReader(){
      if( m_cReader == null )
          m_cReader = new SetNumberReaderImpl();

      return m_cReader;
    }

    ////////////////////////////////////////////////////////////////////////////
      public int getPageNumber() throws CodedException {
        if (m_baValue == null)
            throw new CodedException(this, ERR_EMPTY_BUFFER, "No memory allocated for this set number");
        return Converter.bytesToInt(m_baValue, m_iOffset);
      }

      ////////////////////////////////////////////////////////////////////////////
      public int getTidNumber() throws CodedException {
        if (m_baValue == null)
            throw new CodedException(this, ERR_EMPTY_BUFFER, "No memory allocated for this set number");

        return Converter.bytesToShort(m_baValue, m_iOffset + 4);
      }
      ////////////////////////////////////////////////////////////////////////////
}
