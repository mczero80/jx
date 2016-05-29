package metaxa.os.devices.net;

/** 
  * thrown if set method of ConnectorType get a wrong connector type - should not happen
  * if method is used the intended way 
  */
class UndefinedConnectorException extends Exception {
  public UndefinedConnectorException() {
    super();
  }
  public UndefinedConnectorException(String arg) {
    super(arg);
  }
}
