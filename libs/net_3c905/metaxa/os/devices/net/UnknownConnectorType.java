package metaxa.os.devices.net;

/**
  * thrown in NicHardwareInformation class if set method used with wrong argument
  * should not happen if method is used the intended way
  */
class UnknownConnectorType extends Exception {
  public UnknownConnectorType() {
    super();
  }
  public UnknownConnectorType(String arg) {
    super(arg);
  }
}
