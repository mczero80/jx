package db.dbindex;


import jx.db.CodedException;
import db.com.Converter;
import jx.db.types.DbComparator;
import db.com.Iterator;

import db.com.comparators.CompoundComparator;

import db.bxtree.BxTree;
import db.tid.Set;


/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class DbSingleDimIndex implements DbIndexSingleDim {
    public static final int ERR_INVALID_COMPARATOR = 0;

    byte[] m_baKey = null;
    byte[][] m_baDimKey = null;
    int[] m_aiSizes;
    int[] m_aiKeyPos;
    BxTree m_cBxTree;
    Set m_cKeySet;

    int m_iCurrentKeyPos = 0;

    public DbSingleDimIndex(BxTree cBxTree) throws CodedException {
        m_cBxTree = cBxTree;
        m_baKey = new byte[ cBxTree.getKeySize() ];
        m_cKeySet = new Set(null, -1, -1);
        DbComparator cComparator = cBxTree.getComparator();

        if (!(cComparator instanceof CompoundComparator))
            throw new CodedException(this, ERR_INVALID_COMPARATOR, "The BxTree does not contain a CompoundComparator!");

        m_aiSizes = ((CompoundComparator) cComparator).getSizes();
        m_aiKeyPos = new int[ m_aiSizes.length ];
        m_baDimKey = new byte[ m_aiSizes.length ][];

        int iOffset = 0;

        for (int iCnter = 0; iCnter < m_aiSizes.length; iCnter++) {
            m_aiKeyPos[ iCnter ] = iOffset;
            iOffset += m_aiSizes[ iCnter ];
            m_baDimKey[ iCnter ] = new byte[ m_aiSizes[ iCnter ] ];
        }

    }

    public void addAttributeToKey(byte[] baKey, int iOffset) throws CodedException {
        System.arraycopy(baKey, iOffset, m_baKey, m_aiKeyPos[ m_iCurrentKeyPos ],
            m_aiSizes[ m_iCurrentKeyPos ]);
        m_iCurrentKeyPos++;
    }

    public void clearKey() {
        m_iCurrentKeyPos = 0;
    }

    public void insert(byte[] baData) throws CodedException {
        m_cBxTree.insert(m_baKey, baData);
    }

    public boolean find(byte[] baDest) throws CodedException {
        return m_cBxTree.find(m_baKey, baDest);
    }

    public void remove() throws CodedException {
        m_cBxTree.remove(m_baKey);
    }

    public DbComparator getComparatorForAttr(int iDim) throws CodedException {
        return ((CompoundComparator) m_cBxTree.getComparator()).getComparators()[ iDim ];
    }

    public void removeAll() throws CodedException {
        m_cBxTree.removeAll();
    }

    public Iterator getIterator() throws CodedException {
        return m_cBxTree.getIterator();
    }

    public int[]   getKeyOffsets() {
        return m_aiKeyPos;
    }

    public byte[][] getFirstKey() throws CodedException {
        m_cBxTree.goToKey(false);
        Iterator cIterator = m_cBxTree.getIterator();

        cIterator.getCurrent(m_cKeySet, null);
        for (int iCnter = 0; iCnter < m_baDimKey.length; iCnter++) {
            System.arraycopy(m_cKeySet.getBytes(), m_cKeySet.getOffset() + m_aiKeyPos[ iCnter ],
                m_baDimKey[ iCnter ], 0, m_baDimKey[ iCnter ].length);
        }
        return m_baDimKey;
    }

    public byte[][] getLastKey()  throws CodedException {
        m_cBxTree.goToKey(true);
        Iterator cIterator = m_cBxTree.getIterator();

        cIterator.getCurrent(m_cKeySet, null);
        for (int iCnter = 0; iCnter < m_baDimKey.length; iCnter++) {
            System.arraycopy(m_cKeySet.getBytes(), m_cKeySet.getOffset() + m_aiKeyPos[ iCnter ],
                m_baDimKey[ iCnter ], 0, m_baDimKey[ iCnter ].length);
        }
        return m_baDimKey;
    }
}
