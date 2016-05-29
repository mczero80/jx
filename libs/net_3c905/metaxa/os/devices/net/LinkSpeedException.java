package metaxa.os.devices.net;

/**
  * thrown in D3C905 if a error in determining the link speed occures
  */
public class LinkSpeedException extends Exception {
  public LinkSpeedException() {
    super();
  }
  public LinkSpeedException(String arg) {
    super(arg);
  }
}
