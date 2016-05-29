package jx.verifier.wcet;

public class WCETimeExceededException extends Exception {
    private ExecutionTime eTime;
    public ExecutionTime getETime() {return eTime;}
    public WCETimeExceededException(ExecutionTime eTime) {
	this.eTime = eTime;
    }
}
