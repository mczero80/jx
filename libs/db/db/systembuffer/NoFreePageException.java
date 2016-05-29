/*
 * NoFreePageException.java
 *
 * Created on 24. Oktober 2001, 17:35
 */

package db.systembuffer;

/**
 *
 * @author  ivancho
 * @version 
 */
public class NoFreePageException extends java.lang.Exception {

    /**
     * Creates new <code>NoFreePageException</code> without detail message.
     */
    public NoFreePageException() {
    }


    /**
     * Constructs an <code>NoFreePageException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoFreePageException(String msg) {
        super(msg);
    }
}


