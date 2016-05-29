package metaxa.os.devices.net;

class TimerList implements Cloneable {
    TimerList next;          
    TimerList prev;
    int expires;
    int  data;
      TimerList(TimerList next, TimerList prev, int expires, int data) {
	this.next = next;
	this.prev = prev;
	this.expires = expires;
	this.data = data;
    }
    public Object clone() {
	Object o = null;
	try {
	    o = super.clone();
	}
	catch (Exception e) {}
	return o;
    }
};                     


class Resourcen {

    private int ReceiveCount;
    private int SendCount;
    private int SharedMemorySize;
    private byte SharedMemoryVirtual; 
    private TimerList Timer = null;
    //    private TqStruct hostErr_task = null;
    private int TimerInterval;
    private int DownPollRate;

    public void set_ReceiveCount(int receivecount) {
	ReceiveCount = receivecount;
    }
    public void set_SendCount(int sendcount) {
	SendCount = sendcount;
    }
    public void set_SharedMemorySize(int sharedmemorysize) {
	SharedMemorySize = sharedmemorysize;
    } 
    public void set_SharedMemoryVirtual(byte sharedmemoryvirtual) {
	SharedMemoryVirtual = sharedmemoryvirtual;
    }
    public void set_Timer(TimerList next, TimerList prev, int expires, int data) {
	if (Timer != null) {
	    System.out.println("Timer schon gesetzt");
	    return;
	}
        Timer = new TimerList(next, prev, expires, data);
    }
    /*
    public void set_hostErr_task(TqStruct next, int sync, byte data) {
	if (hostErr_task != null) {
	    System.out.println("Schon eine TqStruktur");
	    return;
	}
	hostErr_task = new TqStruct(next, sync, data);
    }
    */
    public void set_TimerInterval(int timerinterval) {
	TimerInterval = timerinterval;
    }
    public void set_DownPollRate(int downpollrate) {
	DownPollRate = downpollrate;
    }

    public int get_ReceiveCount() {
	return ReceiveCount;
    }
    public int get_SendCount() {
	return SendCount;
    }
    public int get_SharedMemorySize() {
	return SharedMemorySize;
    } 
    public byte get_SharedMemoryVirtual() {
	return SharedMemoryVirtual;
    }
    public TimerList get_Timer() {
	TimerList ret = (TimerList)Timer.clone();
	return ret;
    }
    /*
    public TqStruct get_hostErr_task() {
	TqStruct ret = (TqStruct)hostErr_task.clone();
	return ret;
    }
    */
    public int get_TimerInterval() {
	return TimerInterval;
    }
    public int get_DownPollRate() {
	return DownPollRate;
    }
}
