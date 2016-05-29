package jx.zero;

public interface CPUState extends Portal {

  /** Returns the state of the Thread. (e.g. runnable) 
   *
   * @return    the state of this CPUState (Thread)<BR> possible return values are: <code> STATE_INIT, STATE_RUNNABLE, STATE_ZOMBIE, STATE_SLEEPING, STATE_WAITING, STATE_WAITPORTAL0, STATE_WAITPORTAL1</code> and <code> STATE_BLOCKEDUSER</code>.
   */
  public int getState ();
  static final int STATE_INIT        = 1;
  static final int STATE_RUNNABLE    = 2;
  static final int STATE_ZOMBIE      = 3;
  static final int STATE_SLEEPING    = 5;
  static final int STATE_WAITING     = 6;
  static final int STATE_WAITPORTAL0 = 7;
  static final int STATE_WAITPORTAL1 = 8;
  static final int STATE_BLOCKEDUSER = 9;

  /**
   * Determines wether this Thread is servicing a Portal or not.
   * @return    <code>true</code> if this Thread is servicing a Portal.<BR>
                <code>false</code> if this Thread is not a "portalthread".
   */
  public boolean isPortalThread(); 
    
  /**
   * returns next-"Pointer" used to generate a linked List of CPUStates
   * @return    next CPUState od <code>null</code>
   */
  public CPUState getNext();
  /**
   * sets next-"Pointer" used to generate a linked List of CPUStates
   * @return    the previous value of the next reference
   */
  public CPUState setNext(CPUState next);
}
