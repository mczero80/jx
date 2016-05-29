package metaxa.os.devices.net;

/**
  * thrown by the constructor of EthernetAddress if wrong initialising argument is passed
  */
class WrongEthernetAdressFormat extends Exception {
  public WrongEthernetAdressFormat() {
    super();
  }
  public WrongEthernetAdressFormat(String arg) {
    super(arg);
  }
}
