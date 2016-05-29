package db.collections;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivan Dedinski
 * @version 1.0
 */

import db.list.BiList;
import db.com.*;
import jx.db.CodedException;


public class DBHashtable {
    //////////////////////////////////////////////////////////////////////////////
    private class DBHashEntry {
        private int m_iKey = -1;
        private Object m_cElement = null;

        public DBHashEntry(int iKey, Object cElement) {
            m_iKey = iKey;
            m_cElement = cElement;
        }

        public DBHashEntry() {
            //factory constructor
        }

        public int getKey() {
            return m_iKey;
        }

        public void setKey(int iKey) {
            m_iKey = iKey;
        }

        public Object getElement() {
            return m_cElement;
        }

        public void setElement(Object cElement) {
            m_cElement = cElement;
        }

        public void setBoth(int iKey, Object cElement) {
            m_cElement = cElement;
            m_iKey = iKey;
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    private class DBHashEntryFactory implements PoolableFactory {
        public Object createPoolable() {
            return new DBHashEntry();
        }
    }
    //////////////////////////////////////////////////////////////////////////////
    private Object[] m_caHashArray = null;
    private ObjectPool m_cEntryPool = null;

    public DBHashtable(int iSize, ObjectPool cEntryPool) {
        if (cEntryPool == null) {
            int iPoolSize = iSize / 10; // a little heuristic here

            if (iPoolSize < 10)
                iPoolSize = 10;
            m_cEntryPool = new ObjectPool(iPoolSize, new DBHashEntryFactory());
        }else
            m_cEntryPool = cEntryPool;

        m_caHashArray = new Object[ iSize ];
        for (int iCnter = 0; iCnter < iSize; iCnter++)
            m_caHashArray[ iCnter ] = null;
    }

    public void put(int iKey, Object cElement) {
        if (m_caHashArray[ iKey % m_caHashArray.length ] == null) {
            DBHashEntry cEntry = (DBHashEntry) m_cEntryPool.getObject();

            cEntry.setBoth(iKey, cElement);
            m_caHashArray[ iKey % m_caHashArray.length ] = cEntry;
        }else {
            if (m_caHashArray[ iKey % m_caHashArray.length ] instanceof BiList) {
                DBHashEntry cEntry = (DBHashEntry) m_cEntryPool.getObject();

                cEntry.setBoth(iKey, cElement);
                ((BiList) m_caHashArray[ iKey % m_caHashArray.length ]).insertAtStart(cEntry);
            }else {
                BiList cList = new BiList();
                DBHashEntry cEntry = (DBHashEntry) m_cEntryPool.getObject();

                cEntry.setBoth(iKey, cElement);
                cList.insertAtStart(m_caHashArray[ iKey % m_caHashArray.length ]);
                cList.insertAtStart(cEntry);
                m_caHashArray[ iKey % m_caHashArray.length ] = cList;
            }
        }
    }

    public Object get(int iKey) throws CodedException {
        if (m_caHashArray[ iKey % m_caHashArray.length ] == null)
            return null;

        if (m_caHashArray[ iKey % m_caHashArray.length ] instanceof BiList) {
            BiList cList = (BiList) m_caHashArray[ iKey % m_caHashArray.length ];

            cList.moveToFirst();
            do {
                DBHashEntry cEntry = (DBHashEntry) cList.getCurrent();

                if (cEntry.getKey() == iKey)
                    return cEntry.getElement();
            }
            while (cList.moveToNext());
            return null;
        }else{
            DBHashEntry cEntry = (DBHashEntry) m_caHashArray[ iKey % m_caHashArray.length ];
            if( cEntry.getKey() == iKey )
                return cEntry.getElement();
            else
                return null;
        }
    }

    public void remove(int iKey) throws CodedException {
        if (m_caHashArray[ iKey % m_caHashArray.length ] == null)
            return;

        if (m_caHashArray[ iKey % m_caHashArray.length ] instanceof BiList) {
            BiList cList = (BiList) m_caHashArray[ iKey % m_caHashArray.length ];

            cList.moveToFirst();
            do {
                DBHashEntry cEntry = (DBHashEntry) cList.getCurrent();

                if (cEntry.getKey() == iKey) {
                    cList.removeCurrent();
                    if (cList.isEmpty())
                        m_caHashArray[ iKey % m_caHashArray.length ] = null;
                    return;
                }
            }
            while (cList.moveToNext());
        }else {

            if(((DBHashEntry)m_caHashArray[ iKey % m_caHashArray.length ]).getKey() == iKey ){
              m_cEntryPool.addObject(m_caHashArray[ iKey % m_caHashArray.length ]);
              m_caHashArray[ iKey % m_caHashArray.length ] = null;
            }
        }
    }
}
