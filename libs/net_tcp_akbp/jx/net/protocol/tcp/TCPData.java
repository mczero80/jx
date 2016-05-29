package jx.net.protocol.tcp;

import jx.net.IPData;

public class TCPData extends IPData {

    protected int retransmitCounter;
    protected int retransmitTimestamp;

    TCPData (IPData d){
	sourceAddress = d.sourceAddress;
	destinationAddress = d.destinationAddress;
	mem = d.mem;
	offset = d.offset;
	size = d.size;
	retransmitCounter = 0;
	retransmitTimestamp = 0;
    }

    public int getRetransmitCounter() {
        return retransmitCounter;
    }

    public void setRetransmitCounter(int retransmitCounter) {
        this.retransmitCounter = retransmitCounter;
    }

    public int getRetransmitTimestamp() {
        return retransmitTimestamp;
    }

    public void setRetransmitTimestamp(int retransmitTimestamp) {
        this.retransmitTimestamp = retransmitTimestamp;
    }
}

