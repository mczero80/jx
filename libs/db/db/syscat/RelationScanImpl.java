/*
 * RelationScan.java
 *
 * Created on 30. Juli 2001, 10:11
 */

package db.syscat;

import jx.db.CodedException;
import db.com.Converter;
import db.com.comparators.CompoundComparator;
import db.com.comparators.ByteComparator;
import db.com.Iterator;

import db.dbindex.DbIndex;
import db.dbindex.DbIndexSingleDim;

import db.tid.SetArray;
import db.tid.SetArrayImpl;
import db.tid.SetNumber;
import db.tid.SetAccess;
import db.tid.Set;

import jx.db.RelationalOperator;

import jx.db.RelationScan;
import jx.db.TupleDescriptor;
import jx.db.TupleReader;
import jx.db.TupleWriter;
import jx.db.Key;
import jx.db.IndexInfo;
import jx.db.Index;
import jx.db.Table;
import jx.db.SetNumberReader;


/**
 *
 * @author  ivanich
 * @version
 */
public class RelationScanImpl implements RelationScan {

    public static final int ERR_INVALID_IID = 1;
    public static final int ERR_INDX_NOT_ITERABLE = 2;
    public static final int ERR_UNSUPPORTED_ITYPE = 3;

    private TupleDescriptor m_cTupleDescriptor = null;

    private int             m_iIndexAttrPos = -1;
    private int             m_iRelID        = -1;

    private SystemCatalogImpl   m_cSysCat = null;
    private ByteComparator      m_cSetNumCmp = null;

    private DbIndex         m_cDbIndex = null;
    private Iterator        m_cIterator = null;
    private SetArray        m_cSetArray = null;

    private byte[]          m_baTmpSetNum = null;
    private SetNumber       m_cTmpSetNum = null;

    private boolean         m_bUnique = false;
    private boolean         m_bOpened = false;
    private boolean         m_bEmpty = false;

    private byte[][]        m_baStart = null;
    private byte[][]        m_baEnd = null;
    private byte[][]        m_baTmpKey = null; // temporary storage
    private byte[][]        m_baFirstPossibleKey = null;

    private byte[]          m_baStartSetNum = null;
    private byte[]          m_baEndSetNum = null;

    private Set             m_cKey;
    private Set             m_cData;

    private Key             m_cRetKey;

    private Table           m_cTable;
    private Index           m_cIndex;

    private int[] m_aiAttrMap;

    /** Creates new RelationScan */
    protected RelationScanImpl( SystemCatalogImpl cSysCat, Table cTable, Index cIndex, Key cStart, Key cEnd ) throws CodedException {
        m_cSysCat = cSysCat;
        m_cTable = cTable;
        m_cIndex = cIndex;

        m_iRelID = ((TableImpl)m_cTable).getSetType();

        m_baTmpSetNum = new byte[ SetNumber.getByteLen() ];
        m_cTmpSetNum = new SetNumber();
        m_cSetArray = new SetArrayImpl();
        m_cSetNumCmp = new ByteComparator();

        m_cKey = new Set(null, 0, 0);
        m_cData = new Set(null, 0, 0);

        m_cRetKey = cIndex.getKey();

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        if( cStart != null )
            m_baStart = cStart.getBytes();
        else
            m_baStart = null;

        if( cEnd != null )
            m_baEnd = cEnd.getBytes();
        else
            m_baEnd = null;

        if (m_baStart != null)
            m_baStartSetNum = new byte[SetNumber.getByteLen()];

        if (m_baEnd != null)
            m_baEndSetNum = new byte[SetNumber.getByteLen()];
    }

    //*************************************************************************************************************
    public Key getCurrentKey() throws CodedException {

        if (!m_bOpened && !open())
            throw new CodedException(this, ERR_EMPTY_SEARCH, "Empty search");

        if (m_bEmpty)
            throw new CodedException(this, ERR_EMPTY_SEARCH, "Empty scan!");

        m_cIterator.getCurrent(m_cKey, null);
        m_cRetKey.setBytes( m_cKey.getBytes(), m_cKey.getOffset() );
        return m_cRetKey;
    }

    //*************************************************************************************************************
    public boolean open() throws CodedException {
        m_cTupleDescriptor = m_cTable.getTupleDescriptor();
        IndexInfo cII = m_cIndex.getIndexInfo();

        if (cII == null)
            throw new CodedException(this, ERR_INVALID_IID, "The given index ID is not associated with this table!");

        m_cDbIndex = ((IndexImpl)m_cIndex).getIndex();
        if (!(m_cDbIndex instanceof DbIndexSingleDim ))
            throw new CodedException(this, ERR_INDX_NOT_ITERABLE, "You can't use this Index to iterate through the table!");

            /*if( cII.getAttributeMap().length != 1 )
             throw new CodedException( this, ERR_UNSUPPORTED_ITYPE, "Currently, only single attribute scans supported!" );*/

        if (cII.getType() != SystemCatalogImpl.INDX_TYPE_SINGLE_ATTR &&
            cII.getType() != SystemCatalogImpl.INDX_TYPE_SINGLE_DIM_MULTI_ATTR)
            throw new CodedException(this, ERR_UNSUPPORTED_ITYPE, "Currently, only single attribute scans supported!");

        m_iIndexAttrPos = cII.getAttributeMap()[0];

        m_bUnique = cII.isUnique();

        m_cIterator = ((DbIndexSingleDim) m_cDbIndex).getIterator();

        m_aiAttrMap = m_cIndex.getIndexInfo().getAttributeMap();

        m_baFirstPossibleKey  = ((DbIndexSingleDim)m_cDbIndex).getFirstKey();

        m_baTmpKey = new byte[m_aiAttrMap.length][];

        if (m_baStart != null)
            findStartSetNum();

        if (m_baEnd != null)
            findEndSetNum();

        validateRange();

        m_bOpened = true;

        return moveToFirst();
    }

    //*************************************************************************************************************
    public void close() throws CodedException {
        if (m_bOpened && m_cIterator != null)
            m_cIterator.close();
    }

    //*************************************************************************************************************
    public boolean moveToNext() throws CodedException {
        if (!m_bOpened && !open())
            return false;

        if (m_bEmpty)
            return false;

        if (m_bUnique) {
            if (m_baEnd != null) {
                m_cIterator.getCurrent(null, m_cData);
                if (m_cSetNumCmp.compare(m_cData.getBytes(), m_cData.getOffset(),
                        m_baEndSetNum, 0, m_cData.getLength()) == 0)
                    return false;
            }
            return m_cIterator.moveToNext();
        }else {
            if (!m_cSetArray.moveToNext()) {

                if (m_baEnd != null) {
                    m_cIterator.getCurrent(null, m_cData);
                    if (m_cSetNumCmp.compare(m_cData.getBytes(), m_cData.getOffset(),
                            m_baEndSetNum, 0, m_cData.getLength()) == 0)
                        return false;
                }

                if (!m_cIterator.moveToNext())
                    return false;
                else {
                    m_cIterator.getCurrent(null, m_cData);
                    m_cTmpSetNum.setSetNumber(m_cData.getBytes(), m_cData.getOffset());

                    m_cSetArray.init(m_cSysCat.getSetAccessForSegment(SystemCatalogImpl.SYSCAT_SEGMENT),
                        m_cTmpSetNum, SetNumber.getByteLen(), m_cTupleDescriptor.getRelId());
                }
            }
        }
        return true;
    }

    //*************************************************************************************************************
    public boolean moveToFirst() throws CodedException {
        if (!m_bOpened) {
            return open();
        }else {

            if (m_bEmpty)
                return false;

            if (m_baStart == null) { //no start condition specified

                if (!m_cIterator.moveToFirst())
                    return false;

                if (!m_bUnique) {
                    m_cIterator.getCurrent(m_cKey, m_cData);
                    m_cTmpSetNum.setSetNumber(m_cData.getBytes(), m_cData.getOffset());

                    m_cSetArray.init(m_cSysCat.getSetAccessForSegment(SystemCatalogImpl.SYSCAT_SEGMENT),
                        m_cTmpSetNum, SetNumber.getByteLen(), m_cTupleDescriptor.getRelId());
                }
            }else {
                findStartSetNum();
            }

            return true;
        }

    }

    //*************************************************************************************************************
    public  boolean moveToPrev() throws CodedException {
        throw new CodedException(this, -1, "Not implemented!");
        //return false;
    }

    //*************************************************************************************************************
    public TupleReader getCurrent() throws CodedException {
        if (!m_bOpened && !open())
            throw new CodedException(this, ERR_EMPTY_SEARCH, "Empty search");

        if (m_bEmpty)
            throw new CodedException(this, ERR_EMPTY_SEARCH, "The result set is empty!");

        TupleReader cTupleReader;

        if (m_bUnique) {
            m_cIterator.getCurrent(null, m_cData);
            m_cTmpSetNum.setSetNumber(m_cData.getBytes(), m_cData.getOffset());
            cTupleReader = m_cSysCat.readTuple(m_cTupleDescriptor, m_cTmpSetNum.getReader());
        }else {
            Set cSet = m_cSetArray.getCurrentRecord();

            m_cTmpSetNum.setSetNumber(cSet.getBytes(), cSet.getOffset());
            cTupleReader = m_cSysCat.readTuple(m_cTupleDescriptor, m_cTmpSetNum.getReader());
            cSet.unfix();
        }

        return cTupleReader;
    }

    //*************************************************************************************************************
    public SetNumberReader getCurrentSetNum() throws CodedException {
        if (!m_bOpened && !open())
            throw new CodedException(this, ERR_EMPTY_SEARCH, "Empty search");

        if (m_bEmpty)
            throw new CodedException(this, ERR_EMPTY_SEARCH, "The result set is empty!");

        if (m_bUnique) {
            m_cIterator.getCurrent(null, m_cData);
            m_cTmpSetNum.setSetNumber(m_cData.getBytes(), m_cData.getOffset());
            return m_cTmpSetNum.getReader();
        }else {
            Set cSet = m_cSetArray.getCurrentRecord();

            System.arraycopy(cSet.getBytes(), cSet.getOffset(), m_baTmpSetNum, 0, m_baTmpSetNum.length);
            cSet.unfix();
            m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
            return m_cTmpSetNum.getReader();
        }
    }

    //*************************************************************************************************************
    public TupleDescriptor getTupleDesc() throws CodedException {
        if (!m_bOpened && !open())
            throw new CodedException(this, ERR_EMPTY_SEARCH, "Empty search");

        return m_cTupleDescriptor;
    }

    //*************************************************************************************************************
    protected boolean indexSearch(byte[][] baFieldValue, byte[] baData) throws CodedException {

        m_cDbIndex.clearKey();
        for (int iCnter = 0; iCnter < baFieldValue.length; iCnter++)
            m_cDbIndex.addAttributeToKey(baFieldValue[ iCnter ], 0);

        boolean bRet = m_cDbIndex.find(baData);

        m_cIterator = ((DbIndexSingleDim) m_cDbIndex).getIterator();

        if (!m_cIterator.isEmpty()) {
            m_cIterator.getCurrent(null, m_cData);
            System.arraycopy(m_cData.getBytes(), m_cData.getOffset(), baData, 0, baData.length);
        }else {
            m_bEmpty = true;
            return false;
        }

        if (!m_bUnique && !m_cIterator.isEmpty()) {
            m_cTmpSetNum.setSetNumber(baData);

            m_cSetArray.init(m_cSysCat.getSetAccessForSegment(m_cSysCat.SYSCAT_SEGMENT),
                m_cTmpSetNum, SetNumber.getByteLen(), m_iRelID );
        }

        return bRet;
    }

    //*************************************************************************************************************
    private void findStartSetNum() throws CodedException {
        indexSearch(m_baStart, m_baStartSetNum);
    }

    //*************************************************************************************************************
    private void findEndSetNum() throws CodedException {
        if (!indexSearch(m_baEnd, m_baEndSetNum) && !m_bEmpty) {

            m_cIterator.getCurrent(m_cKey, m_cData);
            m_cTmpSetNum.setSetNumber(m_cData.getBytes(), m_cData.getOffset());

            if (!m_bUnique) {

                m_cSetArray.init(m_cSysCat.getSetAccessForSegment(m_cSysCat.SYSCAT_SEGMENT),
                    m_cTmpSetNum, SetNumber.getByteLen(), m_iRelID);

                Set cSet = m_cSetArray.getCurrentRecord();

                Converter.moveBytes(m_baTmpSetNum, 0, cSet.getBytes(), cSet.getOffset(), cSet.getLength());
                m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
                cSet.unfix();
            }

            Tuple cTuple = m_cSysCat.readTuple( m_cTupleDescriptor, m_cTmpSetNum );

            for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
                int iResult = m_cDbIndex.getComparatorForAttr(iCnter).compare(cTuple.getBytes(),
                        cTuple.getFieldOffset(m_aiAttrMap[ iCnter ]), m_baEnd[ iCnter ], 0, m_baEnd[iCnter].length);

                if (iResult > 0) {
                    cTuple.getReader().close();

                    if (!m_cIterator.moveToPrev()) {
                        m_bEmpty = true;
                        return;
                    }

                    m_cIterator.getCurrent(null, m_cData);
                    System.arraycopy(m_cData.getBytes(), m_cData.getOffset(),
                        m_baEndSetNum, 0, m_baEndSetNum.length);
                    return;
                }else if (iResult < 0)
                    return;
            }
        }
    }

    //*************************************************************************************************************
    private void validateRange() throws CodedException {

        if (m_baEnd == null || m_baStart == null || m_bEmpty)
            return;

        if (!m_bUnique) {

            m_cSetArray.init(m_cSysCat.getSetAccessForSegment(m_cSysCat.SYSCAT_SEGMENT),
                new SetNumber(m_baStartSetNum), SetNumber.getByteLen(), m_iRelID);

            Set cSet = m_cSetArray.getCurrentRecord();

            Converter.moveBytes(m_baTmpSetNum, 0, cSet.getBytes(), cSet.getOffset(), cSet.getLength());
            cSet.unfix();
        }else {
            Converter.moveBytes(m_baTmpSetNum, m_baStartSetNum);
        }

        m_cTmpSetNum.setSetNumber( m_baTmpSetNum );
        Tuple cTuple = m_cSysCat.readTuple( m_cTupleDescriptor, m_cTmpSetNum );

        byte[][] baStart = new byte[ m_aiAttrMap.length ][];

        for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
            baStart[ iCnter ] = new byte[ m_baStart[iCnter].length ];
            Converter.moveBytes(baStart[ iCnter ], 0, cTuple.getBytes(),
                cTuple.getFieldOffset(m_aiAttrMap[iCnter]), baStart[iCnter].length);
        }

        cTuple.getReader().close();

        if (!m_bUnique) {

            m_cSetArray.init(m_cSysCat.getSetAccessForSegment(m_cSysCat.SYSCAT_SEGMENT),
                new SetNumber(m_baEndSetNum), SetNumber.getByteLen(), m_iRelID);

            Set cSet = m_cSetArray.getCurrentRecord();

            Converter.moveBytes(m_baTmpSetNum, 0, cSet.getBytes(), cSet.getOffset(), cSet.getLength());
            cSet.unfix();
        }else {
            Converter.moveBytes(m_baTmpSetNum, m_baEndSetNum);
        }

        m_cTmpSetNum.setSetNumber(m_baTmpSetNum);
        cTuple = m_cSysCat.readTuple(m_cTupleDescriptor, m_cTmpSetNum);

        byte[][] baEnd = new byte[ m_aiAttrMap.length ][];

        for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
            baEnd[ iCnter ] = new byte[ m_baEnd[iCnter].length ];
            Converter.moveBytes(baEnd[ iCnter ], 0, cTuple.getBytes(),
                cTuple.getFieldOffset(m_aiAttrMap[iCnter]), baStart[iCnter].length);
        }

        cTuple.getReader().close();

        for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
            int iResult = m_cDbIndex.getComparatorForAttr(iCnter).compare(baStart[iCnter], 0, m_baEnd[iCnter], 0, m_baEnd[iCnter].length);

            if (iResult > 0) {
                m_bEmpty = true;
                break;
            }else if (iResult < 0)
                break;
        }

        for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
            int iResult = m_cDbIndex.getComparatorForAttr(iCnter).compare(m_baStart[ iCnter ], 0, baEnd[iCnter], 0, baEnd[iCnter].length);

            if (iResult > 0) {
                m_bEmpty = true;
                break;
            }else if (iResult < 0)
                break;
        }

        for (int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter++) {
            int iResult = m_cDbIndex.getComparatorForAttr(iCnter).compare(baStart[iCnter], 0, baEnd[iCnter], 0, baEnd[iCnter].length);

            if (iResult > 0) {
                m_bEmpty = true;
                break;
            }else if (iResult < 0)
                break;
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    public boolean findKey(byte[][] baKey) throws CodedException {
        if (m_baStart != null)
            if (compareKey(m_baStart, baKey) > 0)
                return false;

        if (m_baEnd != null)
            if (compareKey(m_baEnd, baKey) < 0)
                return false;

        for( int iCnter = 0; iCnter < m_aiAttrMap.length; iCnter ++ ){

        }

        return indexSearch(baKey, m_baTmpSetNum);
    }

    ////////////////////////////////////////////////////////////////////////////
    private int compareKey(byte[][] baFirst, byte[][] baSecond)  throws CodedException {
        for (int iCnter = 0; iCnter < baSecond.length; iCnter++) {
            int iResult = m_cDbIndex.getComparatorForAttr(iCnter).compare(baFirst[iCnter], 0, baSecond[iCnter], 0, baSecond[iCnter].length);

            if (iResult > 0)
                return 1;
            else if (iResult < 0)
                return -1;
        }

        return 0;
    }

    //*************************************************************************************************************
    public boolean isEmpty(){
        return m_bEmpty;
    }

    //*************************************************************************************************************
    public void setStartAndEnd(byte[][] baStart, byte[][] baEnd) throws CodedException {
        close();
        m_baStart = baStart;
        m_baEnd = baEnd;

        if (m_baStart != null && m_baStartSetNum == null)
            m_baStartSetNum = new byte[SetNumber.getByteLen()];

        if (m_baEnd != null && m_baEndSetNum == null)
            m_baEndSetNum = new byte[SetNumber.getByteLen()];
    }

    //*************************************************************************************************************
    public IndexInfo getIndexInfo() throws CodedException {
        return m_cIndex.getIndexInfo();
    }

    public int[] getIndexKeyPos() throws CodedException {
        return m_cDbIndex.getKeyOffsets();
    }

}
