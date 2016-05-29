/*
 * TupleDescriptor.java
 *
 * Created on 15. Juli 2001, 18:23
 */

package db.syscat;

import jx.db.types.DbConverter;
import db.list.List;

import jx.db.*;


/**
 *
 * @author  ivanich
 * @version
 */
public class TupleDescriptorImpl implements TupleDescriptor {

    public static final int ERR_WRONG_OFFSET = 0;
    public static final int ERR_TD_NOT_INITIALIZED = 1;
    public static final int ERR_INVALID_IID = 2;
    public static final int ERR_WRONG_OFFSET_DIM = 3;

    private int  m_iAttrCount;    /*count of the attributes               */
    private int  m_iRelID;        /*id of the relation                    */
    private int  m_iSegNum;       /*segment number of the data            */

    private int     m_aiSizes[];     /*size of each attribute                  */
    private int     m_aiTypes[];     /*type of each attribute                  */
    private DbConverter m_acConverters[]; /* converter for each attribute */
    private String  m_aszNames[];    /*name of each attribute                  */
    private int     m_aiOffsets[] = null; /* byte offset for each attribute (first has 0!)*/
    ////////////////////////////////////////////////////////////////////////////
    /** Creates new TupleDescriptor */
    public TupleDescriptorImpl( int iCount, int iRelID, int iSegNum ){
        m_iAttrCount = iCount;
        m_iRelID = iRelID;
        m_iSegNum = iSegNum;

        m_aiSizes = new int[ iCount ];
        m_aiTypes = new int[ iCount ];
        m_aszNames = new String[ iCount ];
        m_acConverters = new DbConverter[ iCount ];
    }

    ////////////////////////////////////////////////////////////////////////////
    public int  getCount() {
        return m_iAttrCount;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int  getSegNum() {
        return m_iSegNum;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setRelId(int iRelID) {
        m_iRelID = iRelID;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int  getRelId() {
        return m_iRelID;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setAttrSize(int iPos, int iSize) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        m_aiSizes[ iPos ] = iSize;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getAttrSize(int iPos) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        return m_aiSizes[ iPos ];
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getTupleSize() {
        int iSum = 0;

        for (int iCnter = 0; iCnter < m_iAttrCount; iCnter++)
            iSum += m_aiSizes[ iCnter ];

        return iSum;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setType(int iPos, int iType) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        m_aiTypes[ iPos ] = iType;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getType(int iPos) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        return m_aiTypes[ iPos ];
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setName(int iPos, String szName) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        m_aszNames[ iPos ] = szName;
    }

    ////////////////////////////////////////////////////////////////////////////
    public String getName(int iPos) throws CodedException {
        if (iPos >= m_iAttrCount)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        return m_aszNames[ iPos ];
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getFieldOffset(int iPos) throws CodedException {
        if (m_aiOffsets == null) {
            m_aiOffsets = new int[ m_iAttrCount ];
            for (int iCnter1 = 0; iCnter1 < m_iAttrCount; iCnter1++) {
                int iWorkPos = 0;

                for (int iCnter2 = 0; iCnter2 < iCnter1; iCnter2++) {
                    iWorkPos += getAttrSize(iCnter2);
                }
                m_aiOffsets[ iCnter1 ] = iWorkPos;
            }

        }
        return m_aiOffsets[ iPos ];
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setFieldOffsets(int[] aiOffsets) throws CodedException {
        if (aiOffsets.length != m_iAttrCount)
            throw new CodedException(this, ERR_WRONG_OFFSET_DIM,
                    "The offset array must have m_iAttrCount elements");

        m_aiOffsets = aiOffsets;
    }

    ////////////////////////////////////////////////////////////////////////////
    public IndexInfo getIndexInfo(int iIID) throws CodedException {

            throw new CodedException(this, ERR_TD_NOT_INITIALIZED,
                    "Tuple descriptor not initialized, contains no IndexInfos!");
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setConverter(int iPos, DbConverter cConverter) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        m_acConverters[ iPos ] = cConverter;
    }

    ////////////////////////////////////////////////////////////////////////////
    public DbConverter getConverter(int iPos) throws CodedException {
        if (iPos >= m_iAttrCount || iPos < 0)
            throw new CodedException(this, ERR_WRONG_OFFSET, "Wrong offset! " + iPos);
        return m_acConverters[ iPos ];
    }

    ////////////////////////////////////////////////////////////////////////////
    /*
    public TupleDescriptor join(TupleDescriptor cTdRight, int iAttrRight)  throws CodedException {

        TupleDescriptor cRet = new TupleDescriptor(getCount() + cTdRight.getCount() - 1, -1, getSegNum());

        for (int iCnter = 0; iCnter < getCount(); iCnter++) {
            cRet.setName(iCnter, getName(iCnter));
            cRet.setType(iCnter, getType(iCnter));
            cRet.setAttrSize(iCnter, getAttrSize(iCnter));
            cRet.setConverter(iCnter, getConverter(iCnter));
        }

        int iStep = 0;

        for (int iCnter = 0; iCnter < cTdRight.getCount() - 1; iCnter++) {
            if (iCnter == iAttrRight)
                iStep = 1;
            cRet.setName(getCount() + iCnter, cTdRight.getName(iCnter + iStep));
            cRet.setType(getCount() + iCnter, cTdRight.getType(iCnter + iStep));
            cRet.setAttrSize(getCount() + iCnter, cTdRight.getAttrSize(iCnter + iStep));
            cRet.setConverter(getCount() + iCnter, cTdRight.getConverter(iCnter + iStep));
        }

        return cRet;
    }*/
    ////////////////////////////////////////////////////////////////////////////
}
