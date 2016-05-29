/*
 * IntHashKey.java
 *
 * Created on 11. Juli 2001, 22:57
 */

package db.com;


/**
 *
 * @author  ivanich
 * @version 
 */
public class IntHashKey extends java.lang.Object {

    private int m_iValue = 0;
    
    /** Creates new IntHashKey */
    public IntHashKey() {        
    }
    
    public IntHashKey(int iValue) {        
        m_iValue = iValue;
    }

    public int hashCode() {
        return m_iValue;
    }
    
    public void setValue(int iValue) {
        m_iValue = iValue;
    }
    
    public int getValue() {
        return m_iValue;
    }

}
