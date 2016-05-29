package test.monitor;

import jx.zero.*;

class Target {
    int a;
    synchronized void n() {}
     void m() {
	 synchronized(this) {
	     a++;
	 }
     }
}

class Main {
    public static void main(String[] args) {
	Target o = new Target();
	Debug.out.println("Start call");
	o.m();
	Debug.out.println("End call");
    }
}
