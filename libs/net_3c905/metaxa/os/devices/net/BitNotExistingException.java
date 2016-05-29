package metaxa.os.devices.net;

/**
  * exception that is thrown by methods that set bits, manipulate bits or check
  * for set bits - thrown if the index is bigger than the number of bits the
  * corresponding datatype has 
  */
class BitNotExistingException extends Exception {
  public BitNotExistingException() {
    super();
  }
  public BitNotExistingException(String arg) {
    super(arg);
  } 
}
