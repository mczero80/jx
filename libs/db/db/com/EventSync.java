package db.com;


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**
 * class EventSync - an implementation for a simple synchronization event
 */
public class EventSync {
    private int m_iObjCnter = 0;

    private boolean m_bEvent;
    private boolean m_bNeedToWait;
    private Object  m_cMessage;
    //////////////////////////////////////////////////////////////////////////////////////////////
    public EventSync() {
        m_bEvent = false;
        m_bNeedToWait = true;
        m_cMessage = null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    public synchronized void waitForEvent() {
        try {
            do {
                if (m_bEvent == true) {
                    m_bEvent = false;
                    return; //received event!
                }
                wait();
            }
            while (true);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public /*synchronized*/ void resetEvent() {
        m_bEvent = false;
        m_bNeedToWait = true;
        m_cMessage = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public synchronized void setEvent() {
        m_bEvent = true;
        notify();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public synchronized void setEvent(Object cMessage) {
        m_bEvent = true;
        m_cMessage = cMessage;
        notify();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public boolean getNeedToWait() {
        return m_bNeedToWait;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public Object getMessage() {
        return m_cMessage;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    public void setNeedToWait(boolean bNeedToWait) {
        m_bNeedToWait = bNeedToWait;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////
}
