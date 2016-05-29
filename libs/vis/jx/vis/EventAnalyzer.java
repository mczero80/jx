/*
 * Analyze event trace.
 *
 * Author: Michael Golm
 */
package jx.vis;

import java.util.*;
import java.io.*;
import jx.formats.*;

interface EventAction {
    void action(EventInfo e);
}

public class EventAnalyzer {
    boolean opt_graph = false;
    boolean opt_count = false;
    boolean opt_countfreq = false;
    boolean opt_diff = false;
    boolean opt_dump = false;
    boolean opt_dumpAll = false;
    boolean opt_dumpTypes = false;
    boolean opt_pattern = false;
    boolean opt_remove = false;
    boolean opt_include = false;
    boolean opt_tid = false;
    int nums[]=null;
    String[] names=null;

    InputStream  in;
    PrintStream out=null;
    String filename;
    String starteventname = null;
    boolean verbose=false;
    int maxtime = 0;
    float timefactor = 1;
    long timecorrector = 0;
    int patternlength = 0;
    int maxevents = -1;
    int interval = -1;
    int timescale = -1;
    

    static void usage() {
	System.out.println("<eventin> [-dumpAll] [-dumpTypes] [-dump <eventnames ...>] [-diff <eventnames ...>] [-graph <filename>] [-timefactor <factor>] [-timescale <number>] [-timecorrector <timetosubtract>] [-maxtime <time>] [-pattern <patternlength>] [-startAt <eventname>] [-maxevents <number>] [-remove <eventnames ...>] [-include <eventnames ...>]");
	System.out.println("-graph <filename>: Create file <filename> that contains the times between events as an event graph and can be used as input for the dot program.");
			   System.out.println("-count <filename>: Create file <filename> that contains the number of events and can be used as input for the gnuplot program.");
			   System.out.println("-countfreq <filename> <interval>: Create file <filename> that contains the number of events sampled over <interval> and can be used as input for the gnuplot program.");
			   System.out.println("-maxevents <number>: Only process number events.");
			   System.out.println("-timescale <number>: 1=nano, 2=micro, 3=milli, 4=sec");
			   System.out.println("-threadid: info1 contains thread id");
    }
    public static void main(String[] args) throws Exception {
	new EventAnalyzer(args);
    }

    private String[] getNames(String args[], int argc) {
	String[] names;
	int n;
	for(n=0; n<args.length-argc-1; n++) {
	    if (args[argc+n+1].charAt(0) == '-') break;
	}
	names = new String[n];
	for(int i=0; i<n; i++) {
	    names[i] = args[argc+i+1];
	}
	return names;
    }

    public EventAnalyzer(String[] args) throws Exception {
	if (args.length ==  0) { usage(); return; }

	    in = new FileInputStream(args[0]);

	int argc = 1;
	while (argc < args.length) {
	    if (args[argc].equals("-dump")) {
		opt_dump = true;
		names = getNames(args, argc);
		argc += names.length;
	    } else if (args[argc].equals("-remove")) {
		opt_remove = true;
		names = getNames(args, argc);
		argc += names.length;
	    } else if (args[argc].equals("-include")) {
		opt_include = true;
		names = getNames(args, argc);
		argc += names.length;
	    } else if (args[argc].equals("-dumpTypes")) {
		opt_dumpTypes = true;
	    } else if (args[argc].equals("-dumpAll")) {
		opt_dumpAll = true;
	    } else if (args[argc].equals("-diff")) {
		opt_diff = true;
	    } else if (args[argc].equals("-graph")) {
		out = new PrintStream(new FileOutputStream(args[argc+1]));
		argc++;
		opt_graph = true;
	    } else if (args[argc].equals("-count")) {
		filename = args[argc+1];
		out = new PrintStream(new FileOutputStream(filename));
		argc++;
		opt_count = true;
	    } else if (args[argc].equals("-countfreq")) {
		filename = args[argc+1];
		out = new PrintStream(new FileOutputStream(filename));
		argc++;
		interval = Integer.parseInt(args[argc+1]);
		argc++;
		opt_countfreq = true;
	    } else if (args[argc].equals("-startAt")) {
		starteventname= args[argc+1];
		argc++;
	    } else if (args[argc].equals("-maxevents")) {
		maxevents = Integer.parseInt(args[argc+1]);
		argc++;
	    } else if (args[argc].equals("-maxtime")) {
		maxtime = Integer.parseInt(args[argc+1]);
		argc++;
	    } else if (args[argc].equals("-timefactor")) {
		timefactor = Float.parseFloat(args[argc+1]);
		argc++;
	    } else if (args[argc].equals("-timecorrector")) {
		timecorrector = Long.parseLong(args[argc+1]);
		argc++;
	    } else if (args[argc].equals("-timescale")) {
		timescale = Integer.parseInt(args[argc+1]);
		argc++;
	    } else if (args[argc].equals("-pattern")) {
		patternlength = Integer.parseInt(args[argc+1]);
		argc++;
		opt_pattern = true;
	    } else if (args[argc].equals("-threadid")) {
		opt_tid = true;
	    } else {
		System.out.println("Unknown option "+args[argc]);
		usage();
	    }
	    argc++;
	}

	final Events e = new Events(in, verbose);
	names2nums(e);

	if (opt_graph) {
	    out.println("digraph G {");
	    final Vector edges = new Vector();
	    class Edge {
		int from;
		int to;
		long times[];
		int number;
		long median;
		float variance;
		float deviation;
		public int hashcode() {
		    return (from << 16) | (to & 0xffff);
		}
	    }
	    System.out.println("Creating graph...");
	    processAll(e, new EventAction() {
		    int prev = -1;
		    int i=-1;
		    long prevTime = -1;
		    public void action(EventInfo e) {
			i++;
			System.out.println(i);
			if (prev != -1) {
			    boolean found=false;
			    for(int j=0; j<edges.size(); j++) {
				Edge d = (Edge)edges.elementAt(j);
				if (prev == d.from && e.number == d.to) {
				    d.times[d.number] += e.timestamp-timecorrector-prevTime;
				    if (d.times[d.number]<0) d.times[d.number]=0;
				    d.number++;
				    found = true;
				    break;
				}
			    }
			    if (! found) {
				Edge d = new Edge();
				d.times = new long[40000];
				d.from = prev;
				d.to = e.number;
				d.times[0] = e.timestamp-prevTime-timecorrector;
				if (d.times[0]<0) d.times[0]=0;
				//System.out.println(""+d.time);
				d.number = 1;
				edges.addElement(d);
			    }
			}
			prev = e.number;
			prevTime = e.timestamp;
		    }
		});
	    System.out.println("Done.");
	    // compute variance and deviation
	    for(int i=0; i<edges.size(); i++) {
		Edge d = (Edge)edges.elementAt(i);
		for(int j=0; j<d.number; j++) {
		    d.median += d.times[j];
		}
		d.median /= d.number;
	    }
	    for(int i=0; i<edges.size(); i++) {
		Edge d = (Edge)edges.elementAt(i);
		float diffFromMedian=0;
		for(int j=0; j<d.number; j++) {
		    diffFromMedian = (float)d.median - (float)d.times[j];
		    d.variance += diffFromMedian*diffFromMedian;
		}
		d.variance /= d.number;
		d.deviation = (float)Math.sqrt(d.variance);
	    }


	    // print
	    for(int i=0; i<edges.size(); i++) {
		Edge d = (Edge)edges.elementAt(i);
		if (maxtime != 0 && d.median > maxtime) continue; // time different too large -> dont show
		out.println(d.from + "->" + d.to + " [label=\""+((int)(d.median*timefactor))+" +/- "+((int)(d.deviation*timefactor))+" ("+d.number+")\"];");
	    }
	    for(int i=0; i<e.types.length; i++) {
		// only label nodes that are present in the graph
		boolean found=false;
		for(int j=0; j<edges.size(); j++) {
		    Edge d = (Edge)edges.elementAt(j);
		    if (d.from ==  e.types[i].number || d.to ==  e.types[i].number) {
			found = true;
			break;
		    }
		}
		if (found)
		    out.println(e.types[i].number+ " [label=\""+e.types[i].name+"\"];");
	    }
	    out.println("}");
	} else 	if (opt_count || opt_countfreq) {
	    System.out.println("Creating file...");
	    out.print("# time ");
	    for(int i=0; i<e.types.length; i++) {
		out.print(e.types[i].name+" ");
	    }
	    out.println();
	    final int counts[] = new int[e.types.length];
	    processAll(e, new EventAction() {
		    int prev = -1;
		    long prevTime = -1;
		    long intervalStart = -1;
		    public void action(EventInfo ev) {
			for(int i=0; i<e.types.length; i++) {
			    if (ev.number ==  e.types[i].number) {
				counts[i]++;
				break;
			    }
			}
			
			if (opt_countfreq) {
			    if (intervalStart == -1) intervalStart = ev.timestamp;
			    if ((ev.timestamp - intervalStart) > interval) {
				//out.println(" "+(float)((double)(ev.timestamp-intervalStart)*(double)timefactor));
				out.print((ev.timestamp*timefactor)+" ");
				for(int i=0; i<counts.length; i++) {
				    out.print((double)counts[i]/(double)((ev.timestamp-intervalStart)*timefactor)+" ");
				    counts[i]=0;
				}
				out.println();
				intervalStart=ev.timestamp;
			    }
			    /*
			      if (prevTime != -1) {
			      out.print((ev.timestamp*timefactor)+" ");
			      for(int i=0; i<counts.length; i++) {
			    out.print((double)counts[i]/(double)((ev.timestamp-prevTime)*timefactor)+" ");
			    }
			    out.println();
			    }
			    */
			    prevTime = ev.timestamp;
			} else {
			    // print current counts
			    out.print((ev.timestamp*timefactor)+" ");
			    for(int i=0; i<counts.length; i++) {
				out.print(counts[i]+" ");
			    }
			    out.println();
			}
		    }
		});
	    System.out.println("Use the following lines to control gnuplot:");
	    System.out.println("set output \"schedevents.mif\""); 
	    System.out.println("set terminal mif monochrome"); 
	    if (opt_count) {
		System.out.println("set ylabel \"Events\" "); 
	    } else {
		System.out.println("set ylabel \"Events/"+interval+"\" "); 
	    }
	    System.out.println("set xlabel \"Time ("+getTimeScale()+")\""); 
	    System.out.println("plot \\");
	    for(int i=0; i<e.types.length; i++) {
		System.out.print("\""+filename+"\" using 1:"+(i+2)+" with lines");
		if (i < e.types.length-1) {
		    System.out.print(",");
		}
		System.out.println();
	    }
	} else if (opt_pattern) {
	    Vector patterns = new Vector();
	    class Pattern {
		int[] pattern;
		int number;
	    }
	    for(int i=patternlength; i<e.nevents; i++) {
		boolean found = false;
		for(int j=0; j<patterns.size(); j++) {
		    Pattern p = (Pattern) patterns.elementAt(j);
		    int k;
		    for (k=0; k<patternlength; k++) {
			if (p.pattern[k] == e.all[i-patternlength+k].number) continue;
			break;
		    }
		    if (k==patternlength) {
			p.number++;
			found = true;
			break;
		    }
		}
		if (! found) {
		    Pattern p = new Pattern();
		    p.pattern = new int[patternlength];
		    p.number = 1;
		    for (int j=0; j<patternlength; j++) {
			p.pattern[j] = e.all[i-patternlength+j].number; 
		    }
		    patterns.addElement(p);
		}
	    }
	    // print patterns
	    for(int i=0; i<patterns.size(); i++) {
		    Pattern p = (Pattern) patterns.elementAt(i);
		    System.out.print(p.number + "    ");
		    for (int j=0; j<patternlength; j++) {
			System.out.print(" "+p.pattern[j]);
		    }
		    System.out.println();
	    }
	} else if (opt_dumpAll) {
	    processAll(e, new EventAction() {
		    int i;
		    public void action(EventInfo e) {
			System.out.print((i++)+" "+((long)correctTime(e.timestamp))+" "+e.type.name);
			if (opt_tid) {
			    System.out.print("  "+(e.info1>>>16)+"."+(e.info1 & 0xffff));
			} else {
			    if (e.info1!=0) System.out.print("  "+e.info1);
			}
			if (e.info2!=0) System.out.print("  "+e.info2);
			System.out.println("");
		    }
		});
	} else if (opt_dumpTypes) {
	    for(int i=0; i<e.types.length; i++) {
		System.out.println(e.types[i].number + " " +e.types[i].name);
	    }
	} else {
	    usage();
	}
    }


    private void processAll(Events e, EventAction action) {
	int starteventnumber = -1;
	if (starteventname!=null){
	    for(int i=0; i<e.types.length; i++) {
		if (e.types[i].name.equals(starteventname)) {
		    starteventnumber = e.types[i].number;
		    break;
		}
	    }
	}	
	for(int i=0; i<e.nevents; i++) {
	    if (e.all[i].timestamp < 0) throw new Error();
	    if (starteventnumber != -1 && e.all[i].number != starteventnumber) {
		continue;
	    }
	    starteventnumber = -1;
	    if (isRemovedEvent(e.all[i].number) || (opt_include && ! isIncludedEvent(e.all[i].number))) {
		continue;
	    }
	    if (maxevents != -1) {
		maxevents--;
		if (maxevents < 0) break;
	    }
	    action.action(e.all[i]);
	}
	if (starteventnumber != -1) throw new Error("Start event never happened!!");
    }

    private void names2nums(Events e) {
	if (names==null) return;
	nums = new int[names.length];
	for(int j=0; j<names.length; j++) {
	    for(int i=0; i<e.types.length; i++) {
		if (e.types[i].name.equals(names[j])) {
		    nums[j] = e.types[i].number;
		    break;
		}
	    }
	}
    }

    private boolean isRemovedEvent(int num) {
	if (! opt_remove) return false;
	for(int i=0; i<nums.length; i++) {
	    if (num==nums[i]) return true;
	}
	return false;
    }

    private boolean isIncludedEvent(int num) {
	if (! opt_include) return false;
	for(int i=0; i<nums.length; i++) {
	    if (num==nums[i]) return true;
	}
	return false;
    }

    private String getTimeScale() {
	switch(timescale) {
	case 1: return "nano";
	case 2: return "micro";
	case 3: return "milli";
	case 4: return "sec";
	default:
	    return "unknown";
	}
    }
    private double correctTime(long t) {
	return (double)(t-timecorrector) * timefactor;
    }
}
