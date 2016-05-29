package test.gc;

import jx.zero.*;

class X {
  int a;
  int b;
  int c;
  X x;
}

public class Main {
    
    public static void main(String[] args) {
	Naming naming=InitialNaming.getInitialNaming();
	final CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	CPUState thread = cpuManager.createCPUState(new ThreadEntry() {
		int i;
		public void run() {
		    cpuManager.setThreadName("Other");
		    for(;;) {
			for(int j=0; j<10000; j++) i++;
			cpuManager.yield();
		    }
		}});
	cpuManager.start(thread);
	
      X root;
      X x;
      x = new X();
      root = x;
      for(;;){
	X y = new X();
	for(int i=0; i<40; i++) {
	    new X(); // garbage
	}
	x.x = y;
	x = y;
	cpuManager.yield();
      }
    }
    public static void init1(Naming naming) {
	int a = 0xaffe;
	X root = new X();
	int b = 0xaaaa;

	//work0(new X(), root);
	
	new Main().w0(new X(), root);
    }

    public static void work0(X x0, X x1) {
      work1(x1,1,2,3,4,5,x0);
    }
    public static void work1(X x0, int a1, int a2, int a3, int a4, int a5, X x1) {
	int a=0x91;
	int b=0x92;
	int c=0x93;
	Object o = new Object();
	int d=0x94;
	work(x1);
    }
    public static void work(X x0) {
	int a = 0x42;
	X x = x0;
	for(;;){
	    int b = 0x43;
	    X y = new X();
	    new X();
	    x.x = y;
	    x = y;
	}
    }
    


    public void w0(X x0, X x1) {
      w1(x1,1,2,3,4,5,x0);
    }
    public void w1(X x0, int a1, int a2, int a3, int a4, int a5, X x1) {
	int a=0x91;
	int b=0x92;
	int c=0x93;
	Object o = new Object();
	int d=0x94;
	w(x1);
    }
    public void w(X x0) {
	int a = 0x42;
	X x = x0;
	for(;;){
	    Main.work0(new X(), x);
	}
    }
    



}


