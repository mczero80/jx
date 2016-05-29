package metaxa.os.devices.net;

/**
  * thrown in D3C905 (main driver class) at several places 
  * if some kind of initialisation method fails this exception is thrown
  */
class NicStatusFailure extends Exception {
    
   public NicStatusFailure() {
	super();
    }
    NicStatusFailure(String msg) {
	super(msg);
    }
}
