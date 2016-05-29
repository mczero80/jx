package db.syscat;


import db.list.List;
import jx.db.CodedException;
import jx.db.IndexInfo;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class IndexInfoImpl implements IndexInfo {
    private int m_iID = 0;
    private int[] m_aiAttrMap = null;
    private List m_cAttrPositions = null;
    private boolean m_bUnique = false;
    private int m_iCount = 0;
    private int m_iIndexType = 0;

    ////////////////////////////////////////////////////////////////////////////
    protected IndexInfoImpl(int iID) {
        m_iID = iID;
        m_cAttrPositions = new List();
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void addAttributeToMap(int iPos) {
        m_cAttrPositions.insert(new Integer(iPos));
        m_iCount++;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setAttributeMap(int[] aiMap) {
        m_aiAttrMap = aiMap;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setUnique(boolean bValue) {
        m_bUnique = bValue;
    }

    ////////////////////////////////////////////////////////////////////////////
    public final int[] getAttributeMap() throws CodedException {
        if (m_aiAttrMap != null)
            return m_aiAttrMap;
        else {
            m_aiAttrMap = new int[ m_iCount ];
            m_cAttrPositions.moveToFirst();

            for (int iCnter = 0; iCnter < m_iCount; iCnter++) {
                m_aiAttrMap[ iCnter ] = ((Integer) m_cAttrPositions.getCurrent()).intValue();
                m_cAttrPositions.moveToNext();
            }
            return m_aiAttrMap;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    public boolean isUnique() {
        return m_bUnique;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getIID() {
        return m_iID;
    }

    ////////////////////////////////////////////////////////////////////////////
    protected void setType(int iType) {
        m_iIndexType = iType;
    }

    ////////////////////////////////////////////////////////////////////////////
    public int getType() {
        return m_iIndexType;
    }
}
