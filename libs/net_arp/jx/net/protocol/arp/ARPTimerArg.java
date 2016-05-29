package jx.net.protocol.arp;


/**
  * argument for the ARP Timer which clears the ARPCache every 15 minutes
  * contains just a handle to the ARP object in order to call the clear method 
  */
class ARPTimerArg {

  private ARP handle;

  public ARPTimerArg(ARP h) {
    handle = h;
  }
  
  public ARP handle() {
    return handle;
  }
}
