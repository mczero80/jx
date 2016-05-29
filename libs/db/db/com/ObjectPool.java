package db.com;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivan Dedinski
 * @version 1.0
 */

import db.list.*;


public class ObjectPool {
    private int m_iCurBufferSize = 0;
    private Object[] m_aObjList = null;

    private PoolableFactory m_cObjFactory = null;
    public ObjectPool(int iMaxBufferSize, PoolableFactory cObjFactory) {
        m_iCurBufferSize = 0;
        m_cObjFactory = cObjFactory;
        m_aObjList = new Object[ iMaxBufferSize ];
    }

    public void addObject(Object cObj) {
        if (m_iCurBufferSize < m_aObjList.length) {
            m_aObjList[ m_iCurBufferSize ] = cObj;
            m_iCurBufferSize++;
        }
    }

    public Object getObject() {
        if (m_iCurBufferSize > 0) {
            m_iCurBufferSize--;
            return m_aObjList[ m_iCurBufferSize ];
        }else {
            return m_cObjFactory.createPoolable();
        }
    }

}
