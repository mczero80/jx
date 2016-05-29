package test.compiler;

import jx.zero.*;
import jx.zero.debug.*;

public class PluginTest {

  public static void testMemory() {
     Naming naming = InitialNaming.getInitialNaming();
	MemoryManager memoryManager = (MemoryManager)naming.lookup("MemoryManager");

	Memory mem1 = memoryManager.alloc(1024);
	Memory mem2 = memoryManager.alloc(1024);

        System.out.println("test get and set");

	for (int i=0; i<1024; i++) mem1.set8(i,(byte)i);
        for (int i=0; i<1024; i++) {
            if (mem1.get8(i)!=(i & 0xff)) {
               System.out.println("8 bit failed "+(i & 0xff)+"!="+mem1.get8(i));
               break;
            }
        }

        try {
		for (int i=0; i<254; i++) {
	            mem1.set32(i,(int)0xf0AABB0f);
                    if (mem1.get32(i)!=(int)0xf0AABB0f) {
			System.out.println("32 bit failed "+i+" "+mem1.get32(i));
                        break;
                    }
                }
	} catch (Exception ex) {
		System.out.println("get/set32 "+ex);
        }

        try {
		for (int i=0; i<1020; i+=4) {
	            mem1.setLittleEndian32(i,(int)0xf0AABB0f);
                    if (mem1.getLittleEndian32(i)!=(int)0xf0AABB0f) {
		        System.out.println(
	                  "le32 bit failed "+i+" "+mem1.getLittleEndian32(i));
                        break;
                    }
                }
	} catch (Exception ex) {
		System.out.println("get/set32 "+ex);
        }

	System.out.println("fin");

  }

  public static void testInteger() {
	int a,b;

	System.out.println("test int operation");

	b = 3;
	a = b * 2;
	if (a!=6) {System.out.println("3*2="+a);}
	a = b * 3;
	if (a!=9) {System.out.println("3*3="+a);}
	a = b * 5;
	if (a!=15) {System.out.println("3*5="+a);}
	a = b * 9;
	if (a!=27) {System.out.println("3*9="+a);}
	a = b * 3 + 10;
	if (a!=19) {System.out.println("3*3+10="+a);}

	System.out.println("fin");
  }
  
private static void stepIn(int deep) {
	  int a = 1970;
	  int b = 1970;
	  int c = 1970;
	  int d = 1970;
	  int e = 1970;
	  int f = 1970;
	  int g = 1970;
	  int h = 1970;
	  int i = 1970;
	  if (deep>0) stepIn(--deep);
	  if (i!=1970) throw new Error();
	  return;
  }

  public static void stackTest() {
	int i = 42;
	System.out.println("stack 100 frames");
	stepIn(100);
	if (i!=42) throw new Error();
	System.out.println("stack 1024 frames");
	stepIn(1024);
	if (i!=42) throw new Error();
	System.out.println("fin");
  }

  public static void main (String[] args) throws Exception {
     Naming naming = InitialNaming.getInitialNaming();

     DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
     if (d == null) throw new Error ("Portal \"DebugChannel0\" not found");
     System.out = new java.io.PrintStream(new jx.zero.debug.DebugOutputStream(d));

	stackTest();
     //testMemory();
     //testInteger();
  }
}
