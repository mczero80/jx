package metaxa.os.devices.net;

/**
  * thrown in NicHardwareInformation if set method is used with wrong argument
  * should not happen if method is used the intended way
  */
class UnknownLinkState extends Exception {
  public UnknownLinkState() {
    super();
  }
  public UnknownLinkState(String arg) {
    super(arg);
  }
}
