/*
 * DbSingleAttrIndex.java
 *
 * Created on 18. Juli 2001, 16:32
 */

package db.dbindex;



import db.com.Iterator;
import jx.db.CodedException;
import jx.db.types.DbComparator;
import db.com.Converter;

import db.bxtree.BxTree;

import db.tid.Set;

import db.systembuffer.PageBuffer;


//import db.list.*;
/**
 *
 * @author  ivanich
 * @version
 */
public class DbSingleAttrIndex implements db.dbindex.DbIndexSingleDim {

    BxTree m_cBxTree = null;
    byte[] m_baKey = null;
    byte[][] m_baDimKey = null;
    int[] m_aiOffsets = null;
    Set m_cKeySet = null;

    /** Creates new DbSingleAttrIndex */
    public DbSingleAttrIndex(BxTree cBxTree) {
        m_cBxTree = cBxTree;
        m_baKey = new byte[ cBxTree.getKeySize() ];
        m_baDimKey = new byte[1][];
        m_baDimKey[ 0 ] = m_baKey;
        m_aiOffsets = new int[1];
        m_aiOffsets[ 0 ] = 0;
        m_cKeySet = new Set( null, -1, -1 );

    }

    public void    addAttributeToKey(byte[] baKey, int iOffset) throws CodedException {
        Converter.moveBytes(m_baKey, 0, baKey, iOffset, m_baKey.length);
    }

    public void    clearKey() {
        return;
    }

    public void insert(byte[] baData) throws CodedException {
        if (m_baKey == null)
            throw new CodedException(this, ERR_NO_KEY_SELECTED, "No key selected for this index!");
        else
            m_cBxTree.insert(m_baKey, baData);
    }

    public boolean find(byte[] baDest) throws CodedException {
        if (m_baKey == null)
            throw new CodedException(this, ERR_NO_KEY_SELECTED, "No key selected for this index!");
        else
            return m_cBxTree.find(m_baKey, baDest);
    }

    public void remove() throws CodedException {
        if (m_baKey == null)
            throw new CodedException(this, ERR_NO_KEY_SELECTED, "No key selected for this index!");
        else
            m_cBxTree.remove(m_baKey);
    }

    public Iterator getIterator() throws CodedException {
        return m_cBxTree.getIterator();
    }

    public void removeAll() throws CodedException {
        m_cBxTree.removeAll();
    }

    public DbComparator getComparatorForAttr(int iDim) throws CodedException {
        if (iDim > 0)
            throw new CodedException(this, ERR_INVALID_DIM, "The key for the index contains no such dimension");
        else
            return m_cBxTree.getComparator();
    }

    public int[]   getKeyOffsets() {
        return m_aiOffsets;
    }

    public byte[][] getFirstKey() throws CodedException{
           m_cBxTree.goToKey( false );
           Iterator cIterator = m_cBxTree.getIterator();

           cIterator.getCurrent( m_cKeySet, null);
           System.arraycopy( m_cKeySet.getBytes(), m_cKeySet.getOffset(), m_baKey, 0, m_baKey.length );
           return m_baDimKey;
    }

    public byte[][] getLastKey()  throws CodedException{
           m_cBxTree.goToKey( true );
           Iterator cIterator = m_cBxTree.getIterator();

           cIterator.getCurrent( m_cKeySet, null);
           System.arraycopy( m_cKeySet.getBytes(), m_cKeySet.getOffset(), m_baKey, 0, m_baKey.length );
           return m_baDimKey;
    }
}

