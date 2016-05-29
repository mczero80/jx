package jx.net.protocol.tcp;
import java.util.*;
import metaxa.os.*;
import jx.zero.*;
import jx.timer.*;

/**
 * this class encapsulates one TCP segment which has been sent but not yet acknowledget
 * therefore we have to keep it in order to retransmit it after a specific timeout
 * each socket has a vector containing these TcpRetransmitSegment objects
 *
 */
public class TcpRetransmitSegment {
    
    private int firstseqnum;
    private int lastseqnum;
    private int queuedtime;
    private int numretransmit;
    private byte[] segment;
    
    /**
     * the only possibility to set some values is through the constructor
     *
     * param fs the first sequence number of the segment contained
     * @param ls the last sequence number of the segment contained
     * @param s the actual segment which may be retransmitted
     *
     */
    public TcpRetransmitSegment(int fs, int ls, byte[] s, TimerManager timerManager) { 
	
	firstseqnum = fs;
	lastseqnum = ls;
	queuedtime = timerManager.getTimeInMillis();
	numretransmit = 0;
	segment = s;
    }
    
    /**
     * read the first sequence number 
     *
     * @return the number of first octet in the tcp segment
     *
     */
    public int firstseqnum() {
	return firstseqnum;
    }
    
    /**
     * read the last sequence number
     *
     * @return the number of the last octet in the tcp segment
     *
     */
    public int lastseqnum() {
	return lastseqnum;
    }
    
    /**
     * read the queuing time
     * 
     * @return the time in clock ticks at which this segment got queued
     *
     */
    public int queuedtime() {
	return queuedtime;
    }
    
    /**
     * read number of retransmits
     *
     * @return the number of retransmissions of this segment - if this value exceeds some boundary the connection
     *        may be aborted
     *
     */
    public int numretransmit() {
	return numretransmit;
    }
    
    /**
     * increment the number of retransmits by one 
     *
     */
    
    public void incnumretransmit() {
	numretransmit++;
    }
    
    /**
     * access the tcp segment
     *
     * @return the reference to the tcp segment contained
     *
     */ 
    public byte[] segment() {
	return segment;
    }
}
