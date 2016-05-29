/*
 * DbIndex.java
 *
 * Created on 18. Juli 2001, 16:05
 */

package db.dbindex;


import db.tid.*;
import db.systembuffer.*;
import db.com.*;

import jx.db.CodedException;
import jx.db.types.DbComparator;


/**
 *
 * @author  ivanich
 * @version
 */
public interface DbIndex {

    public static final int ERR_NO_KEY_SELECTED = 0;
    public static final int ERR_INVALID_DIM = 1;

    public void    addAttributeToKey(byte[] baKey, int iOffset) throws CodedException;
    public void    clearKey();
    public void    insert(byte[] baData) throws CodedException;
    public boolean find(byte[] baDest) throws CodedException;
    public void    remove() throws CodedException;
    public DbComparator getComparatorForAttr(int iDim) throws CodedException;
    public int[]   getKeyOffsets();
    public void removeAll() throws CodedException;

}

