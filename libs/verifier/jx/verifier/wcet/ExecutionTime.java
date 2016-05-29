package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//class to record execution time of an method
public interface ExecutionTime {
    //add the time of other to the time of this
    public void add(ExecutionTime other);
    //make this the maximum time of this and other
    public void max(ExecutionTime other);
    //add the time the Execution of bc takes to this.
    public void addTimeOfBC(ByteCode bc);
    //copy this 
    public ExecutionTime copy();
    //returns true if maxTime (in ms) is exceeded
    public boolean limitExceeded();
    //set the timeLimit (in ms)
    public void setTimeLimit(int maxTime);
    //get the timeLimit (in ms); 
    public int getTimeLimit();
    //use up all remaining time at once
    public void useAllTime();
    //returns the time (in ms) to the timeLimit.
    public int timeLeft();
}
