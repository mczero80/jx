package db.dbindex;

import db.com.Iterator;
import jx.db.CodedException;

/**
 * Title:        SA
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */

public interface DbIndexSingleDim extends DbIndex {
  public byte[][] getFirstKey() throws CodedException;
  public byte[][] getLastKey () throws CodedException;
  public Iterator getIterator() throws CodedException;
}