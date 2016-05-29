package metaxa.os.devices.net;

class NicStatistics {

    //
    // Transmit statistics.
    //
    int TxFramesOk;
    int TxBytesOk;
    int TxFramesDeferred;
    int TxSingleCollisions;
    int TxMultipleCollisions;
    int TxLateCollisions;
    int TxCarrierLost;
    
    int TxMaximumCollisions;
    int TxSQEErrors;
    int TxHWErrors;
    int TxJabberError; 
    int TxUnknownError;
    
    int TxLastPackets;
    int TxLastCollisions;
    int TxLastDeferred;
    
    //
    // Receive statistics.
    //
    int RxFramesOk;
    int RxBytesOk;
    
    int RxOverruns;
    int RxBadSSD;
    int RxAlignmentError;
    int RxBadCRCError;
    int RxOversizeError;

    int RxNoBuffer;
    
    int RxLastPackets;
    int UpdateInterval;
    
    //
    // Multicasts statistics
    //
    int Rx_MulticastPkts;
    
    public NicStatistics() {
    }	

    public void reset() {
	
	TxFramesOk = 0;
	TxBytesOk = 0;
	TxFramesDeferred = 0;
	TxSingleCollisions = 0;
	TxMultipleCollisions = 0;
	TxLateCollisions = 0;
	TxCarrierLost = 0;
	TxMaximumCollisions = 0;
	TxSQEErrors = 0;
	TxHWErrors = 0;
	TxJabberError = 0; 
	TxUnknownError = 0;
	TxLastPackets = 0;
	TxLastCollisions = 0;
	TxLastDeferred = 0;
	RxFramesOk = 0;
	RxBytesOk = 0;
	RxOverruns = 0;
	RxBadSSD = 0;
	RxAlignmentError = 0;
	RxBadCRCError = 0;
	RxOversizeError = 0;
	RxNoBuffer = 0;
	RxLastPackets = 0;
	UpdateInterval = 0;
	Rx_MulticastPkts = 0;
    }	

} 
