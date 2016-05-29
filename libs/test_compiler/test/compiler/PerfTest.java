package test.compiler;

import jx.zero.*;

import jx.zero.debug.*;
import jx.zero.timing.*;

import java.util.Vector;

class TestField {
    public int c;
    public final int  getValue() { return c; }
    public final void setValue(int v) { c=v; }
}

public class PerfTest {

    private final static boolean doTiming=true;

    static int count;
    private int value;

    private static void ports_impl() {
	Ports ports;

	int addr = 0;
	int data = 0;

	ports = (Ports)InitialNaming.getInitialNaming().lookup("Ports");
	
	ports.outb(addr,(byte)data);
	ports.outb_p(addr,(byte)data);

	data = ports.inb(addr);
	data = ports.inb_p(addr);

	ports.outl(addr,data);
	ports.outl_p(addr,data);

	data = ports.inl(addr);
	data = ports.inl_p(addr);

	ports.outw(addr, (short)data);
	ports.outw_p(addr, (short)data);

	data = ports.inw(addr);
	data = ports.inw_p(addr);
    }

    private static void perf_empty(int d) {
    }

    private static void perf_empty2(int d) {
    }

    private static void perf_static(int d) {
      count++;
    }

    private static void perf_events(CPUManager cpuManager, int event) {
      cpuManager.recordEvent(event);
    }

    private static void perf_field(TestField tf) {
      tf.c++;
    }

    private static void perf_arraycopy(Object[] s,Object[] d,int len) {
	for (int i=0;i<10;i++) {
	    for (int e=0;e<len;e++) d[e]=s[e];
	}
    }

    private static void perf_arraycopy_system(Object[] s,Object[] d,int len) {
	for (int i=0;i<10;i++) System.arraycopy(s,0,d,0,len);
    }

    private static void perf_local(int d) {
	d++;
    }

    private static void perf_getset(TestField tf) {
	tf.setValue(tf.getValue()+1);
    }

    private static void perf_newobj() {
	Integer i = new Integer(42);
    }

    public PerfTest(int value) {
	this.value = value;
    }

    public boolean perf_equals(Object obj) {
	if (obj != null && obj instanceof PerfTest)
	    {
		return (value == ((PerfTest)obj).value);
	    }
	return false;
    }
    
    public static void do_throw() throws Exception {
	throw new Exception();
    }

    public static void do_throw_null() throws Exception {
	Exception ex = null;
	ex.getClass();
    }

    public static void perf_null_throw() {	
	try {
	    do_throw_null();
	} catch (Exception ex) {
	  return;
	}
    }

    public static void perf_inter_throw() {
	try {
	    do_throw();
	} catch (Exception ex) {
	    return;
	}
    }

    public static void perf_local_throw() {
	try {
	    throw new Exception();
	} catch (Exception ex) {
	    return;
	}
    }

    public static void perf_array(int[] a,int i) {
      a[i]++;
    }

    public static void perf_fix_array(int[] a,int i) {
      a[0]++;
    }    

    public static void perf_new_bug() {
	while (true) {
	    new Integer(42);
	}
    }

    public static void init(Naming naming, String[] argv) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	final DebugPrintStream out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out = out;

        test(naming);

	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager"); 
	while (true) cpuManager.yield();
    }


    public static boolean test(Naming naming) {

	Debug.out.println("");

	TestField tf = new TestField();

	Profiler p = (Profiler) naming.lookup("Profiler");
	
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");

        /*
	int event = cpuManager.createNewEvent("perf_event");

	for (int i=0;i<1000;i++) {
	  perf_events(cpuManager,event);
	}
	*/

	if (doTiming) {
	    Debug.out.println(" start and adjust timing ");
	    jx.zero.timing.Control.startTiming();
	}

	/*
	  Debug.out.println("call notify");
	  tf.notify();
	*/

	Debug.out.println(" test exceptions ");

	for (int i=0;i<1000;i++) {
	  perf_local_throw();
	}

	for (int i=0;i<1000;i++) {
	  perf_inter_throw();
	}

	for (int i=0;i<1000;i++) {
	  perf_null_throw();
	}

	/*
	try {

	  Debug.out.println(" test multi 1 arrays ");

	  Vector[][] array = new Vector[10][10];
	  for (int i=0;i<10;i++) {
	    for (int j=0;j<10;j++) {
	      array[i][j] = new Vector();
	      array[i][j].size();
	    }
	  }
	  
	  Debug.out.println(" test multi 2 arrays ");
	  
	  array = new Vector[5][10];
	  for (int i=0;i<5;i++) {
	    for (int j=0;j<10;j++) {
	      Debug.out.println("array["+i+"]["+j+"]=obj");
	      array[i][j] = new Vector();
	      array[i][j].size();
	    }
	  }

	} catch (Exception ex) {
	  Debug.out.println("array test failed");
	}
	*/
	  
	Debug.out.println(" test access field performance ");

	for (int i=0;i<1000;i++) {
	  perf_empty(0);
	}

	for (int i=0;i<1000;i++) {
	  perf_empty2(0);
	}
	
	for (int i=0;i<1000;i++) {
	  perf_static(0);
	}

	for (int i=0;i<1000;i++) {
	  perf_field(tf);
	}

	for (int i=0;i<1000;i++) {
	  perf_local(0);
	}

	for (int i=0;i<1000;i++) {
	  perf_getset(tf);
	}

	int[] a = new int[2];
	
	for (int i=0;i<1000;i++) {
	  perf_array(a,1);
	}

	for (int i=0;i<1000;i++) {
	  perf_fix_array(a,1);
	}

	Debug.out.println(" test object creation");
	
	for (int i=0;i<1000;i++) {
	  perf_newobj();
	}

	Debug.out.println(" test checkcast performance ");

	PerfTest i1 = new PerfTest(42);
	PerfTest i2 = new PerfTest(42);

	if (!i1.perf_equals(i2)) Debug.out.println("i1!=i2");
	for (int i=0;i<899;i++) {
	    i1.perf_equals(i2);
	}

	PerfTest[] src = new PerfTest[100];
	PerfTest[] des = new PerfTest[100];
	for (int i=0;i<100;i++) src[i] = new PerfTest(i);

	perf_arraycopy(src,des,100);
	perf_arraycopy_system(src,des,100);

	for (int i=0;i<100;i++) {
	    if (!des[i].perf_equals(src[i])) {
		Debug.out.println("arraycopy is buggy! pos:"+i);
		break;
	    }
	}

	/*
	Debug.out.println(" test cast and shift operator ");

	for (int i=0;i<300;i+=4) {

	  byte m_red   = (byte)i;
	  byte m_green = (byte)i;
	  byte m_blue  = (byte)i;

	  short s = (short)((((int)m_red >> 3) << 11)|(((int)m_green >> 2) << 5)|((int)m_blue >> 3));

	  if (s!=testShift((byte)i)) {
	    Debug.out.println(i+" "+s+" "+testShift((byte)i));
	  } else {
	    Debug.out.println(i+" "+s);
	  }
	}
	*/

	if (doTiming) p.shell(); 

	return true;
    }

  /*
  private static short testShift(byte i) {
    return  (short)((((int)i >> 3) << 11)|(((int)i >> 2) << 5)|((int)i >> 3));
  }
  */
}
