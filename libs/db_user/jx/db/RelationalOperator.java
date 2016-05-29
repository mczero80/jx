/*
 * RelationalOperator.java
 *
 * Created on 30. Juli 2001, 10:08
 */

package jx.db;

/**
 *
 * @author  ivanich
 * @version
 */
public interface RelationalOperator {

    public static final int ERR_EMPTY_SEARCH = 0;

    public void close() throws CodedException;

    public boolean moveToNext() throws CodedException;

    public boolean moveToFirst() throws CodedException;

    public boolean moveToPrev() throws CodedException;

    public TupleReader getCurrent() throws CodedException;

    public TupleDescriptor getTupleDesc() throws CodedException;

    public boolean isEmpty() throws CodedException;

}
