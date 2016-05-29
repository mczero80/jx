package metaxa.os.devices.net;

/** 
  * thrown in ManufacturingData if set method is used with wrong argument
  * should not happen if method is used as intended
  */
class WrongDate extends Exception {
  public WrongDate() {
    super();
  }
    public WrongDate(String arg) {
    super(arg);
  }
}
