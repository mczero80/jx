package metaxa.os.devices.net;

/**
  * thrown in Mii class if read access to Mii-Registers fails 
  */
class ReadMIIException extends Exception {
  public ReadMIIException() {
    super();
  }
  public ReadMIIException(String arg) {
    super(arg);
  }
}
