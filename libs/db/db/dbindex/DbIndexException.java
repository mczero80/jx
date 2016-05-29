/*
 * DbIndexException.java
 *
 * Created on 18. Juli 2001, 16:48
 */

package db.dbindex;

/**
 *
 * @author  ivanich
 * @version 
 */
public class DbIndexException extends java.lang.Exception {

    /**
 * Creates new <code>DbIndexException</code> without detail message.
     */
    public DbIndexException() {
    }


    /**
 * Constructs an <code>DbIndexException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DbIndexException(String msg) {
        super(msg);
    }
}


