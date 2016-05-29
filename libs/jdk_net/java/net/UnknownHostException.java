package java.net;

import java.io.IOException;

public class UnknownHostException extends IOException {
    public UnknownHostException(String host) {super(host); }
    public UnknownHostException() {  }
}
