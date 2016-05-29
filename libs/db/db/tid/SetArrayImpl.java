/*
 * SetArray.java
 *
 * Created on 16. Juli 2001, 20:03
 *
 * @author  ivanich
 * @version
 */
package db.tid;


import jx.db.CodedException;
import db.com.Converter;

/** a class implementing the {@link SetArray} interface
 */
public class SetArrayImpl implements SetArray {

    private SetAccess m_cSetAccess = null;

    private SetNumber m_cFirstSetNum = null;
    private SetNumber m_cCurrentSetNum = null;
    private SetNumber m_cPreviousSetNum = null;

    private int m_iCurrentOffset = 0;
    private int m_iCurrentSetLen = 0;
    private int m_iSetType = 0;
    private Set m_cCurrentSet = null;

    private int m_iDataSize = 0;

    //temporary storage for deleteArray
    SetNumber m_cNextTmp1   = null;
    SetNumber m_cNextTmp2   = null;
    byte[] m_baTmpSetNum1 = null;
    byte[] m_baTmpSetNum2 = null;


    ////////////////////////////////////////////////////////////////////////////
    /** Creates new SetArray */

    public SetArrayImpl() {

      m_cNextTmp1    = new SetNumber();
      m_cNextTmp2    = new SetNumber();
      m_baTmpSetNum1 = new byte[ SetNumber.getByteLen() ];
      m_baTmpSetNum2 = new byte[ SetNumber.getByteLen() ];
    }

    ////////////////////////////////////////////////////////////////////////////
    /** constructs a new SetArrayImpl object
     * @param cSetAccess SetAccess containing the set array data
     * @param cStart start set of the set array
     * @param iDataSize data size of the elements dtored in the set array
     * @param iSetType set type of the set array
     * @throws CodedException thrown on error ( wrong input parameters, etc. )
     */
    public SetArrayImpl(SetAccess cSetAccess, SetNumber cStart, int iDataSize, int iSetType) throws CodedException {
        init(cSetAccess, cStart, iDataSize, iSetType);
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @param cSetAccess
     * @param cStart
     * @param iDataSize
     * @param iSetType
     * @throws CodedException
     */
    public void init(SetAccess cSetAccess, SetNumber cStart, int iDataSize, int iSetType) throws CodedException {
        m_cSetAccess = cSetAccess;
        m_iDataSize = iDataSize;
        m_iSetType = iSetType;
        m_iCurrentOffset = 0;

        if (m_iDataSize > m_cSetAccess.getMaxSetLen() - 8)
            throw new CodedException(this, ERR_REC_TOO_BIG, "SetArray records too large!");

        if (m_cCurrentSetNum == null) {
            m_cCurrentSetNum = new SetNumber(cStart);
            m_cFirstSetNum = new SetNumber(cStart);
            m_cPreviousSetNum = new SetNumber(cStart);
        }else {
            m_cCurrentSetNum.copy(cStart);
            m_cFirstSetNum.copy(cStart);
            m_cPreviousSetNum.copy(cStart);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @throws CodedException
     * @return
     */
    public Set getCurrentRecord() throws CodedException {
        m_cCurrentSet = m_cSetAccess.getSet(m_cCurrentSetNum, m_iSetType);
        m_cCurrentSet.setOffset(m_cCurrentSet.getOffset() + m_iCurrentOffset);
        m_cCurrentSet.setLength(m_iDataSize);
        return m_cCurrentSet;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @throws CodedException
     */
    public void removeCurrentRecord() throws CodedException {

        m_cCurrentSet = m_cSetAccess.getSet(m_cCurrentSetNum, m_iSetType);
        m_iCurrentSetLen = m_cCurrentSet.getLength();
        int m_iCurrentSetOffset = m_cCurrentSet.getOffset();

        if (m_iCurrentSetLen < m_cSetAccess.getMinSetLen() + m_iDataSize)
            throw new CodedException(this, ERR_ALREADY_EMPTY, "nothing to remove!");

        byte[] baTmp = new byte[ m_iCurrentSetLen - m_iDataSize ];

        Converter.moveBytes(baTmp, 0, m_cCurrentSet.getBytes(), m_iCurrentSetOffset, m_iCurrentOffset);
        Converter.moveBytes(baTmp, m_iCurrentOffset, m_cCurrentSet.getBytes(),
            m_iCurrentSetOffset + m_iCurrentOffset + m_iDataSize,
            m_iCurrentSetLen - (m_iCurrentOffset + m_iDataSize));

        m_iCurrentSetLen -= m_iDataSize;

        m_cCurrentSet.unfix();

        if (m_iCurrentSetLen >= m_iDataSize + m_cSetAccess.getMinSetLen() || m_cPreviousSetNum.equals(m_cCurrentSetNum)) {
            //write out the change
            m_cCurrentSet = m_cSetAccess.modifySet(m_cCurrentSetNum, m_iCurrentSetLen, m_iSetType);
            Converter.moveBytes(m_cCurrentSet.getBytes(), m_cCurrentSet.getOffset(), baTmp, 0,
                m_iCurrentSetLen);
            m_cCurrentSet.unfix();

        }else {
            Set cPrevSet = m_cSetAccess.getSet(m_cPreviousSetNum, m_iSetType);
            byte[] baSetNr = new byte[SetNumber.getByteLen()];

            for (int iCnter = 0; iCnter < baSetNr.length; iCnter++) {
                baSetNr[ iCnter ] = baTmp[ iCnter ];
            }

            Converter.moveBytes(cPrevSet.getBytes(),
                cPrevSet.getOffset() + cPrevSet.getLength() - m_cSetAccess.getMinSetLen(),
                baSetNr, 0, baSetNr.length);

            cPrevSet.unfix();
            m_cSetAccess.removeSet(m_cCurrentSetNum, m_iSetType);

            SetNumber cSetNum = new SetNumber(baSetNr);

            if (cSetNum.getPageNumber() != -1)
                m_cCurrentSetNum = cSetNum;
            else
                moveToFirst();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void moveToBeg() throws CodedException {
        m_cPreviousSetNum.copy(m_cFirstSetNum);
        m_cCurrentSetNum.copy(m_cFirstSetNum);
        m_iCurrentOffset = 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @throws CodedException
     * @return
     */
    public boolean moveToFirst() throws CodedException {
        moveToBeg();

        m_cCurrentSet = m_cSetAccess.getSet(m_cCurrentSetNum, m_iSetType);
        m_iCurrentSetLen = m_cCurrentSet.getLength();
        m_cCurrentSet.unfix();

        if (m_iCurrentSetLen < m_iDataSize + m_cSetAccess.getMinSetLen()) { //first set is empty, try moving ahead!
            return moveToNext();
        }else
            return true;

    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     */
    public boolean moveToNext() throws CodedException {

        m_cCurrentSet = m_cSetAccess.getSet(m_cCurrentSetNum, m_iSetType);
        m_iCurrentSetLen = m_cCurrentSet.getLength();

        if ((m_iCurrentSetLen - m_cSetAccess.getMinSetLen() - m_iCurrentOffset) > m_iDataSize) {
            m_iCurrentOffset += m_iDataSize;
            m_cCurrentSet.unfix();
            return true;
        }else {
            SetNumber cNext = new SetNumber(m_cCurrentSet.getBytes(), m_cCurrentSet.getOffset()
                    + m_iCurrentSetLen - m_cSetAccess.getMinSetLen());

            m_cCurrentSet.unfix();

            if (cNext.getPageNumber() == -1)
                return false;
            else {
                m_iCurrentOffset = 0;
                if (!m_cPreviousSetNum.equals(m_cCurrentSetNum))
                    m_cPreviousSetNum.copy(m_cCurrentSetNum);

                m_cCurrentSetNum.copy(cNext);
                return true;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     */
    public void addAtFront(byte[] baData, int iOffset) throws CodedException {
        moveToBeg();

        m_cCurrentSet = m_cSetAccess.getSet(m_cCurrentSetNum, m_iSetType);
        m_iCurrentSetLen = m_cCurrentSet.getLength();

        if (m_iCurrentSetLen <= m_cSetAccess.getMaxSetLen() - m_iDataSize) { //enough place to insert
            byte[] baTmp = new byte[ m_iCurrentSetLen + m_iDataSize ];

            Converter.moveBytes(baTmp, m_iDataSize, m_cCurrentSet.getBytes(),
                m_cCurrentSet.getOffset(), m_iCurrentSetLen);
            Converter.moveBytes(baTmp, 0, baData, iOffset, m_iDataSize);
            m_iCurrentSetLen += m_iDataSize;
            m_cCurrentSet.unfix();
            m_cCurrentSet = m_cSetAccess.modifySet(m_cCurrentSetNum, m_iCurrentSetLen, m_iSetType);
            Converter.moveBytes(m_cCurrentSet.getBytes(), m_cCurrentSet.getOffset(), baTmp, 0, m_iCurrentSetLen);
            m_cCurrentSet.unfix();
        }else {
            SetNumber cNewSetNum = new SetNumber();
            Set cNewSet = m_cSetAccess.addSet(cNewSetNum, m_iCurrentSetLen, m_iSetType); //make a copy of the current set

            Converter.moveBytes(cNewSet.getBytes(), cNewSet.getOffset(),
                m_cCurrentSet.getBytes(), m_cCurrentSet.getOffset(), m_cCurrentSet.getLength());

            m_cCurrentSet.unfix();
            cNewSet.unfix();

            m_iCurrentSetLen = m_cSetAccess.getMinSetLen();
            m_cCurrentSet = m_cSetAccess.modifySet(m_cCurrentSetNum, m_iCurrentSetLen, m_iSetType);

            Converter.moveBytes(m_cCurrentSet.getBytes(), m_cCurrentSet.getOffset(),
                cNewSetNum.getSetNumber(), 0, SetNumber.getByteLen());

            m_cCurrentSet.unfix();

            addAtFront(baData, iOffset);
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @param cSetAccess
     * @param cSetNum
     * @param iSetType
     * @throws CodedException
     */
    public void createStartSet(SetAccess cSetAccess, SetNumber cSetNum, int iSetType) throws CodedException {
        SetNumber cNext = new SetNumber(-1, -1);

        Set cCurrentSet = cSetAccess.addSet(cSetNum, cSetAccess.getMinSetLen(), iSetType);

        Converter.moveBytes(cCurrentSet.getBytes(), cCurrentSet.getOffset(),
            cNext.getSetNumber(), 0, SetNumber.getByteLen());
        cCurrentSet.unfix();
    }

    ////////////////////////////////////////////////////////////////////////////
    /** implemented function, see {@link SetArray }
     * @throws CodedException
     */
    public void deleteArray() throws CodedException {
        deleteArray( m_cFirstSetNum );
    }

    ////////////////////////////////////////////////////////////////////////////
    public void deleteArray( SetNumber cStart ) throws CodedException{

        System.arraycopy( cStart.getSetNumber(), 0, m_baTmpSetNum1, 0, m_baTmpSetNum1.length );
        m_cNextTmp1.setSetNumber( m_baTmpSetNum1 );

        do {
            m_cCurrentSet = m_cSetAccess.getSet( m_cNextTmp1, m_iSetType );

            System.arraycopy( m_cCurrentSet.getBytes(),
                              m_cCurrentSet.getOffset() + m_iCurrentSetLen - m_cSetAccess.getMinSetLen(),
                              m_baTmpSetNum2, 0, m_baTmpSetNum2.length );

            m_cNextTmp2.setSetNumber( m_baTmpSetNum2 );
            m_cCurrentSet.unfix();
            m_cSetAccess.removeSet( m_cNextTmp1, m_iSetType );
            m_cNextTmp1.copy( m_cNextTmp2 );
        }
        while (m_cNextTmp1.getPageNumber() != -1);
    }

    ////////////////////////////////////////////////////////////////////////////

    /** implemented function, see {@link SetArray }
     * @return
     */
    public int getMaxRecordLen() {
        return m_cSetAccess.getMaxSetLen() - 8;
    }

    ////////////////////////////////////////////////////////////////////////////

}
