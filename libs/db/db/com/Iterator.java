/*
 * SimpleIterator.java
 *
 * Created on 25. Juni 2001, 11:56
 */

package db.com;


import db.tid.Set;
import jx.db.CodedException;


/**
 *
 * @author  ivanich
 * @version
 */
public interface Iterator {

    public static final int ERR_EMPTY_ITERATOR = 0;

    public boolean moveToFirst() throws CodedException;
    public boolean moveToNext()  throws CodedException;
    public boolean moveToPrev()  throws CodedException;
    public boolean isEmpty();
    public void getCurrent(Set cKey, Set cData) throws CodedException;
    public void open();
    public void close();

}

