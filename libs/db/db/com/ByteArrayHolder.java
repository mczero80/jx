/*
 * ByteArrayHoder.java
 *
 * Created on 24. Juli 2001, 22:37
 */

package db.com;


/**
 *
 * @author  ivanich
 * @version 
 */
public class ByteArrayHolder extends java.lang.Object {

    private byte[] m_baArray = null;
    
    /** Creates new ByteArrayHoder */
    public ByteArrayHolder(byte[] baArray) {
        m_baArray = baArray;
    }
    
    public ByteArrayHolder() {
        m_baArray = null;
    }

    public void setBytes(byte[] baArray) {
        m_baArray = baArray;
    }
    
    public byte[] getBytes() {
        return m_baArray;
    }

}
