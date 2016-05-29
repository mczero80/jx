package java.lang;

public class IllegalMonitorStateException extends RuntimeException {
  
    public IllegalMonitorStateException() 
    {
	super();
    }
  
    public IllegalMonitorStateException(String msg) 
    { 
	super(msg); 
    }
}
