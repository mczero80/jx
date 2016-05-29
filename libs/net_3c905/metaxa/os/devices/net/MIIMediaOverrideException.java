package metaxa.os.devices.net;

/**
  * thrown in Mii class if errors occures by MediaOverrides via MII 
  */
class MIIMediaOverrideException extends Exception {
  public MIIMediaOverrideException() {
    super();
  }
  public MIIMediaOverrideException(String arg) {
    super(arg);
  }
}
