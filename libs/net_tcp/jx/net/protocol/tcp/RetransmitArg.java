package jx.net.protocol.tcp;

/**
  * a simple argument object for the Retransmit class - it has just one handle inside, but may be extended if more information is needed
  * so the overhead of hiding a tcp handle inside this argument is justified because of extensibility
  *
  */
class RetransmitArg {
  private TCP tcp;
  
  /**
    * construct the argument object with the reference to a TCP to hold
    *
    */
  public RetransmitArg(TCP tcp) {
    this.tcp = tcp;
    }

  /**
    * return handle to TCP
    *
    * @return the handle of the tcp the argument object hides 
    *
    */
  public TCP tcp() {
    return tcp;
  }  
}
