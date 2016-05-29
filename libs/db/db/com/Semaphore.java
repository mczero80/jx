/*
 * Semaphore.java
 *
 * Created on October 23, 2001, 5:28 PM
 */

package db.com;


//import jx.zero.*;

/**
 *
 * @author  ivanich
 * @version 
 */
public class Semaphore {
    // private static CPUManager m_cpuManager;
    // private CPUState

    private int m_iCount = 0;
    /** Creates new Semaphore */
    public Semaphore() {
    }
    
    public synchronized void acquire(){
	/* while( m_iCount < 0 ){
	   try{
	   wait();
	   }catch( Exception ex ){
	   ex.printStackTrace();
	   }
	   }
	   m_iCount --;*/
    }
    
    public synchronized void release(){
	/* m_iCount ++;
	   notify();*/
    }

}
