/*
 * Tuple.java
 *
 * Created on 15. Juli 2001, 19:36
 */

package db.syscat;


/**
 *
 * @author  ivanich
 * @version
 */

import db.tid.*;
import jx.db.CodedException;
import db.com.Converter;
import jx.db.TupleDescriptor;
import jx.db.TupleReader;
import jx.db.TupleWriter;
import jx.db.Database;
import jx.db.SetNumberReader;
import jx.db.types.TypeManager;

public class Tuple extends java.lang.Object {

    public static final int ERR_EMPTY_TUPLE = 0;
    public static final int ERR_INVALID_ATTR = 1;

    private TupleDescriptor     m_cTD = null;
    private Set                 m_cData = null;
    private SetNumber           m_cSetNum = null;

    private TupleReader m_cTupleReader = null;
    private TupleWriter m_cTupleWriter = null;

    private Object m_cUseReference = null;
    private Database m_cSyscat = null;

    ////////////////////////////////////////////////////////////////////////////
    protected Tuple() {
    }

    ////////////////////////////////////////////////////////////////////////////
    /** Creates new Tuple */
    public Tuple(TupleDescriptor cTD) {
        m_cTD = cTD;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected Tuple(TupleDescriptor cTD, Set cData) {
        m_cTD = cTD;
        m_cData = cData;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected TupleReader getReader(){
      if( m_cTupleReader == null )
          m_cTupleReader = new TupleReaderImpl();

      return m_cTupleReader;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected TupleWriter getWriter(){
      if( m_cTupleWriter == null )
          m_cTupleWriter = new TupleWriterImpl();

      return m_cTupleWriter;
    }

    /**************************************************************************/
    private class TupleReaderImpl implements TupleReader {

        ////////////////////////////////////////////////////////////////////////////
        public TupleDescriptor getTd() {
            return m_cTD;
        }

        ////////////////////////////////////////////////////////////////////////////
        public SetNumberReader getSetNumReader() {
            return m_cSetNum.getReader();
        }

        ////////////////////////////////////////////////////////////////////////////
        public void getField(int iPos, Object cDest, int iDestOffset) throws CodedException {
            if (m_cData == null)
                throw new CodedException(this, ERR_EMPTY_TUPLE, "Tuple not initialized with a set!");

            int iSrcOffset = getFieldOffset(iPos);
            byte[] baSrc = m_cData.getBytes();

            if (cDest instanceof byte[]) {
                System.arraycopy(baSrc, iSrcOffset, cDest, iDestOffset, m_cTD.getAttrSize(iPos));
            }else {
                m_cTD.getConverter(iPos).revert(baSrc, iSrcOffset, cDest, iDestOffset, m_cTD.getAttrSize(iPos));
            }
        }

        ////////////////////////////////////////////////////////////////////////////
        public int getField(int iPos) throws CodedException {
            if (m_cData == null)
                throw new CodedException(this, ERR_EMPTY_TUPLE, "Tuple not initialized with a set!");

            if (m_cTD.getType(iPos) != TypeManager.DATP_DID_INT)
                throw new CodedException(this, ERR_INVALID_ATTR, "Attribute type is not the expected type! DID =" + m_cTD.getType(iPos));

            return Converter.bytesToInt(m_cData.getBytes(), getFieldOffset(iPos));
        }

        ////////////////////////////////////////////////////////////////////////////
        public void dump() throws CodedException {
            for (int iCnter = 0; iCnter < m_cTD.getCount(); iCnter++) {
                System.out.print("| ");
                switch (m_cTD.getType(iCnter)) {
                case TypeManager.DATP_DID_INT:
                    System.out.print(getField(iCnter));
                    break;

                case TypeManager.DATP_DID_STR:
                    StringBuffer s = new StringBuffer("");

                    getField(iCnter, s, 0);
                    System.out.print(s);
                    break;
                }
            }
            System.out.println("|");
        }

        ////////////////////////////////////////////////////////////////////////////
        public void getBytes( byte[] baDest, int iOffset ) throws CodedException{
            System.arraycopy( m_cData.getBytes(), m_cData.getOffset(), baDest, iOffset, m_cTD.getTupleSize() );
        }

        ////////////////////////////////////////////////////////////////////////////

        public void close() throws CodedException{
          (( SystemCatalogImpl )m_cSyscat).endReadTuple( m_cUseReference );
        }
    }


    /**************************************************************************/

    private class TupleWriterImpl implements TupleWriter {
        ////////////////////////////////////////////////////////////////////////////
        public void setField(int iPos, Object cSrc, int iSrcOffset) throws CodedException {

            if (m_cData == null)
                throw new CodedException(this, ERR_EMPTY_TUPLE, "Tuple not initialized with a set!");

            int iDestOffset = getFieldOffset(iPos);
            byte[] baDest = m_cData.getBytes();

            if (cSrc instanceof byte[]) {
                System.arraycopy(cSrc, iSrcOffset, baDest, iDestOffset, m_cTD.getAttrSize(iPos));
            }else {
                m_cTD.getConverter(iPos).convert(cSrc, iSrcOffset, baDest, iDestOffset, m_cTD.getAttrSize(iPos));
            }
        }

        ////////////////////////////////////////////////////////////////////////////
        public void setField(int iPos, int iSrc) throws CodedException {
            if (m_cData == null)
                throw new CodedException(this, ERR_EMPTY_TUPLE, "Tuple not initialized with a set!");

            if (m_cTD.getType(iPos) != TypeManager.DATP_DID_INT)
                throw new CodedException(this, ERR_INVALID_ATTR, "Attribute type is not the expected type! DID =" + m_cTD.getType(iPos));

            int iDestOffset = getFieldOffset(iPos);
            byte[] baDest = m_cData.getBytes();

            Converter.intToBytes(iSrc, m_cData.getBytes(), iDestOffset);
        }

        public TupleReader getReader(){
          return Tuple.this.getReader();
        }

        ////////////////////////////////////////////////////////////////////////////
        public void close()  throws CodedException{
          (( SystemCatalogImpl )m_cSyscat).endModifyTuple( m_cUseReference );
        }
    }
    /**************************************************************************/

    protected void setTd(TupleDescriptor cTD) {
        m_cTD = cTD;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setData(Set cData) {
        m_cData = cData;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected byte[] getBytes() throws CodedException {
        if (m_cData != null)
            return m_cData.getBytes();
        else
            return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    public void setData(Tuple cTuple) {
        m_cData = cTuple.m_cData;
        //save the set number also, for RelationSort for instance
        m_cSetNum = cTuple.m_cSetNum;
    }

    ////////////////////////////////////////////////////////////////////////////

    protected void setDirty() throws CodedException {
        if (m_cData != null)
            m_cData.setDirty();
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void unfix() throws CodedException {
        if (m_cData != null)
            m_cData.unfix();
        m_cData = null;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected int getFieldOffset(int iPos) throws CodedException {
        if (m_cData == null || m_cTD == null)
            throw new CodedException(this, ERR_EMPTY_TUPLE, "the tuple is not valid!");

        if (iPos > (m_cTD.getCount() - 1))
            throw new CodedException(this, ERR_INVALID_ATTR, "Invalid attribute");

        return m_cTD.getFieldOffset(iPos) + m_cData.getOffset();

    }

    ////////////////////////////////////////////////////////////////////////////
    protected SetNumber getSetNum(){
      return m_cSetNum;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setSetNum(SetNumber cSetNum) {
        m_cSetNum = cSetNum;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setUseReference( Object cReference ){
      m_cUseReference = cReference;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setSysCatReference( Database cReference ){
      m_cSyscat = cReference;
    }

    /********************************************************************************************/

}
