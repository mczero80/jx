/*
 * CodedException.java
 *
 * Created on November 2, 2001, 11:45 PM
 */

package jx.db;


/**
 *
 * @author  ivanich
 * @version 
 */
public class CodedException extends java.lang.Exception {

    private Object m_cOriginator = null;
    private int    m_iErrorCode = -1;
    
    public CodedException(Object cOriginator, int iErrorCode, String szErrorMsg) {
        super(szErrorMsg);
        m_cOriginator = cOriginator;
        m_iErrorCode = iErrorCode;
    }
    
    public int getErrorCode() {
        return m_iErrorCode;
    }
    
    public Object getOriginator() {
        return m_cOriginator;
    }
    
}

