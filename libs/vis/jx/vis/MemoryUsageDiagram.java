package jx.vis;

import java.util.*;
import java.io.*;
import jx.formats.*;

public class MemoryUsageDiagram {
    static void usage() {
	System.out.println("<eventin> <mifout> <eventnames...>");
    }
    public static void main(String[] args) throws Exception {
	new MemoryUsageDiagram(args);
    }
    public MemoryUsageDiagram(String[] args) throws Exception {
	if (args.length ==  0) { usage(); return; }

	InputStream in = new FileInputStream(args[0]);
	FileOutputStream miffile = new FileOutputStream(args[1]);


	boolean verbose = false;
	Events e = new Events(in, verbose);
	EventInfo[] all = e.all;

	int[] nums = new int[args.length-2];
	int n=0;
	for(int i=0; i<e.types.length; i++) {
	    for(int j=2;j<args.length;j++) {
		if (e.types[i].name.equals(args[j])) {
		    nums[n++] = i;
		}
	    }
	}
	if (n != nums.length) {
	    System.out.println("wrong event names");
	    return;
	}

	boolean verbose_all = false;
	long time = 0;
	for(int i=0; i<e.nevents; i++) {
	    time += all[i].timestamp;
	    if (! verbose_all) {
		for(int j=0;j<nums.length;j++) {
		    if (all[i].number == nums[j]) {
			System.out.println(time+"  "+all[i].info1);
		    }
		}
	    }
	    /*
	      if (verbose_all) {
	      if (all[i].number == num_malloc) {
	      System.out.println("MALLOC " +time+":  "+all[i].info1);
	      }
	      if (all[i].number == num_free) {
	      System.out.println("FREE   " +time+":  "+all[i].info1);
	      }
	      }
	    */
	}
	
	Visualizer vis = new jx.vis.mif.MIFVisualizer(miffile);

	// draw data
	vis.init();

	int X0 = 150;
	int X1 = 800;
	int Y0 = 10;
	int Y1 = 500;

	// coordinate system
	vis.drawLine(X0, Y0, X0, Y1);
	vis.drawLine(X0, Y1, X1, Y1);



	vis.finish();	
    }
    

}
