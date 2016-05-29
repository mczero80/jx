package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//class to record execution time of an method
public class SimpleExecutionTime implements ExecutionTime {
    public int totalTime = 0;
    public int timeLimit = 0;

    //add the time of other to the time of this
    public void add(ExecutionTime other)  {
	totalTime += ((SimpleExecutionTime)other).totalTime;
    }
    //make this the maximum time of this and other
    public void max(ExecutionTime other) {
	totalTime = (totalTime > ((SimpleExecutionTime)other).totalTime)? totalTime : 
	    ((SimpleExecutionTime)other).totalTime;
    }
    //add the time the Execution of bc takes to this.
    public void addTimeOfBC(ByteCode bc) {
	totalTime++;
    }
    //copy this 
    public ExecutionTime copy() {
	SimpleExecutionTime tmp = new SimpleExecutionTime(totalTime);
	tmp.setTimeLimit(timeLimit);
	return tmp;
    }
    
    //constructor
    public SimpleExecutionTime() {
	this(0);
    }
    public SimpleExecutionTime(int time) {
	totalTime = time;
    }
    //returns true if maxTime (in ms) is exceeded
    public boolean limitExceeded() {
	return (totalTime > timeLimit);
    }
    
    public void setTimeLimit(int maxTime) {this.timeLimit = maxTime;}
    //returns the time (in ms) to the timeLimit.
    public int timeLeft() {
	return timeLimit - totalTime;
    }
    //get the timeLimit (in ms); 
    public int getTimeLimit() {return timeLimit;}
    //use up all remaining time at once
    public void useAllTime()  {
	totalTime = timeLimit;
    }
    
    public String toString() {
	return totalTime + " Operations executed\n";
    }

}
