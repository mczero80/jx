package test.compiler;

import jx.zero.*;
import jx.zero.debug.*;

class LongTestField {
  public static long l1;
  public long l2;

  public LongTestField(long l) {
	l2=l;
  }

  public long getL2() {
	return l2;
  }
}

public class LongTest {

  private static int testCall(int d1, long l, int d2) {
        if (d1!=0) System.out.println("call test failed d1="+d1); 
	System.out.println(l);
        if (d2!=0) System.out.println("call test failed d2="+d2);
        return (int)l;
  }

  public static void testLong() {
      	long l1 = -1L;
        long l2 = 0L;
      	long l3 = 1L;

	l3 = l3 << 8;
	l3 = -l3;

	System.out.println(l1);
	System.out.println(l3);

        System.out.println("l1 = -1L = "+Long.toString(l1,10));
        System.out.println("l2 =  0L = "+l2);

        int r = testCall(0,l1,0);
 
	/*
        if (l1==(l2-1L)) System.out.println("l1==(l2-1L)");
	else  System.out.println("l1==(l2-1L) failed");

	if (l2==(l1+1)) System.out.println("l2==(l1+1)"); 
	else System.out.println("l2==(l1+1) failed");
        
	if (-2==(l1*2)) System.out.println("-2==(l1*2)"); 
	else System.out.println("-2==(l1*2) failed");

	if ((-4L*l1)/2==-2L) System.out.println("(-4L*l1)/2)==-2L"); 
	else System.out.println("(-4L*l1)/2)==-2L failed");

        int r = testCall(0,l1,0);
        System.out.println("testCall(0,l1,0) = "+r);
        System.out.println("testCall(0,4L,0) = "+testCall(0,4L,0));

	LongTestField.l1=l1;
        System.out.println("l1 = "+LongTestField.l1);
        LongTestField tobj = new LongTestField(l2);
	*/

	System.out.println("fin");
  }

  public static void main (String[] args) throws Exception {
     Naming naming = InitialNaming.getInitialNaming();

     DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
     if (d == null) throw new Error ("Portal \"DebugChannel0\" not found");
     System.out = new java.io.PrintStream(new jx.zero.debug.DebugOutputStream(d));
	
     testLong();
  }
}
