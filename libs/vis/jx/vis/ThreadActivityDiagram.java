package jx.vis;

import java.util.*;
import java.io.*;
import jx.formats.*;

public class ThreadActivityDiagram {
    class ThreadInfo {
	int domainID;
	int threadID;
	String name;
	int number;
	public String toString() {
	    return domainID + "." + threadID + " "+name;
	}
    }
    class Switch {
	long timediff; /* time difference to previous entry; this time is the runtime of the previous thread */
	ThreadInfo to;
    }
    int numberOfThreads;
    long endTime;

    /* for text data (deprecated) */
    Vector athreads = new Vector();
    Vector aswitches = new Vector();

    ThreadInfo[] threads;
    Switch[] switches;
    int nswitches;

    boolean opt_verbose_read = false;
    boolean opt_verbose_switch = false;
    boolean opt_equalized = false;
    boolean opt_text = false;
    boolean opt_optimized = false;
    boolean opt_reltime = false;
    boolean opt_sec = false;

    int readahead = 10;
    int timerresolution = 100;

    int MHZ = 500;

    static void usage() {
	System.out.println("<switches.log> <mifout> [-sec] [-reltime] [-text] [-equalized] [-start <timestart>] [-end <timeend>] [-res <timeres>] [-verbose_switch]");
	System.out.println("-reltime: relative time; diagram time starts with 0");
	System.out.println("-sec: time in seconds (default is milliseconds)");
	System.out.println("-text: text input format");
	System.out.println("-equalized: Equidistand thread switches (ignores time)");
    }
    public static void main(String[] args) throws Exception {
	new ThreadActivityDiagram(args);
    }
    public ThreadActivityDiagram(String[] args) throws Exception {
	if (args.length ==  0) { usage(); return; }

	InputStream in = new FileInputStream(args[0]);
	FileOutputStream miffile = new FileOutputStream(args[1]);

	long starttime=-1;
	long endtime=-1;
	long timeunit=-1;

	int argc;
	for(argc = 2; argc < args.length; argc++) {
	    if (args[argc].equals("-verbose_read")) {
		opt_verbose_read = true;
	    } else if (args[argc].equals("-verbose_switch")) {
		opt_verbose_switch = true;
	    } else if (args[argc].equals("-start")) {
		argc++;
		starttime = Integer.parseInt(args[argc]) * 1000L;
	    } else if (args[argc].equals("-end")) {
		argc++;
		endtime = Integer.parseInt(args[argc]) * 1000L;
	    } else if (args[argc].equals("-res")) {
		argc++;
		opt_optimized = true;
		timerresolution = Integer.parseInt(args[argc]);
	    } else if (args[argc].equals("-unit")) {
		argc++;
		timeunit = Integer.parseInt(args[argc]) * 1000L;
	    } else if (args[argc].equals("-text")) {
		opt_text = true;
	    } else if (args[argc].equals("-reltime")) {
		opt_reltime = true;
	    } else if (args[argc].equals("-equalized")) {
		opt_equalized = true;
	    } else if (args[argc].equals("-sec")) {
		opt_sec = true;
	    } else {
		System.out.println("Unknown option ignored: "+args[argc]);
		break;
	    }
	}

	// correct start and end time if relative times are used
	if (opt_reltime) {
	}

	if (opt_text) {
	    readSwitchesText(in);
	} else {
	    readSwitchesBinary(in);
	} 
	
	Visualizer vis = new jx.vis.mif.MIFVisualizer(miffile);

	// sort threads by domain
	ThreadInfo[] nthreads = new ThreadInfo[threads.length];
	int num=0;
	for(int i=0; i<numberOfThreads; i++) {
	    ThreadInfo found = null;
	    int mind = 0x0fffffff;
	    int mint = 0x0fffffff;
	    int pos=-1;
	    for(int j=0; j<threads.length; j++) {
		ThreadInfo info = threads[j];	    
		if (info != null && ((info.domainID < mind)  || (info.domainID == mind && info.threadID < mint))) {
		    mind = info.domainID;
		    mint = info.threadID;
		    found = info;
		    pos = j;
		}
	    }
	    threads[pos] = null;
	    found.number = i;
	    nthreads[num++] = found;
	}
	threads = nthreads;

	// draw data
	vis.init();

	int X0 = 150;
	int X1 = 800;
	int Y0 = 10;
	int Y1 = 500;

	// coordinate system
	vis.drawLine(X0, Y0, X0, Y1);
	vis.drawLine(X0, Y1, X1, Y1);


	Visualizer v0 = new ScalingVisualizer(vis, X0, Y0, 1000, numberOfThreads*10, X1-X0, Y1-Y0);
	// thin lines
	for(int i=0; i<numberOfThreads*10; i+=10) {
	    ThreadInfo info = threads[i/10];
	    String name = "";
	    if (info.name != null) {
		name = info.name + " ";
	    }
	    name += info.domainID+"."+info.threadID;
	    v0.drawText(name, -5, i+9, 10, Visualizer.ALIGN_RIGHT);
	    if ((i % 50) == 0) v0.drawThinLine(0, i, 1000, i);
	}

	VisualizerLong v;
	if (starttime == -1 || starttime < switches[0].timediff) {
	    starttime = switches[0].timediff;
	}
	if (endtime == -1 || endtime > endTime) {
	    endtime = endTime;
	}
	System.out.print("StartTime: "+starttime);
	if (starttime != switches[0].timediff) System.out.print(" (was "+switches[0].timediff+")");
	System.out.println();
	System.out.print("Endtime: "+endtime);
	if (endtime != endTime) System.out.print(" (was "+endTime+")");
	System.out.println();
	System.out.println("Diff: "+(endtime-starttime));
	long timerange = endtime-starttime;
	if (! opt_equalized) {
	    v = new ScalingVisualizerLong(vis, X0, Y0, timerange, numberOfThreads*10, X1-X0, Y1-Y0);
	} else {
	    v = new ScalingVisualizerLong(vis, X0, Y0, nswitches, numberOfThreads*10, X1-X0, Y1-Y0);
	}

	System.out.print("Generating MIF:   0% ");
	int n5 = nswitches/5;
	if (opt_equalized) {
	    for(int i=0; i<nswitches; i+=nswitches/10) {
		v.drawText(""+i, i, numberOfThreads*10+10, 5, Visualizer.ALIGN_CENTER);
	    }
	    for(int i=0; i<nswitches; i++) {
		Switch sw = switches[i];
		v.drawRect(i, sw.to.number*10, 1, 10, "5");
		if (i%n5==0) System.out.print("\b\b\b"+(i*100/nswitches)+"%");
	    }
	    
	} else {
	    ScalingVisualizerLong vxtext = new ScalingVisualizerLong(vis, X0, Y0, timerange, 1000, X1-X0, Y1-Y0);
	    if (timeunit == -1) timeunit = (timerange)/10;
	    for(long i=0; i<timerange; i+=timeunit) {
		String timestr;
		long time;
		if (opt_reltime) {
		    time = i;
		} else {
		    time = i+starttime; 
		}
		if (opt_sec) {
		    time /= 1000000000; 
		} else {
		    time /= 1000000; 
		}
		if (time > 1000) {
		    timestr = (time/1000)+",";
		    long rem = (time-(time/1000)*1000);
		    if (rem < 100) timestr += "0";
		    if (rem < 10) timestr += "0";
		    timestr += rem;
		} else {
		    timestr = ""+time;
		}
		vxtext.drawText(timestr, i, 1040, 30, Visualizer.ALIGN_CENTER);
		vxtext.drawThinLine(i, 0, i, 1000);
	    }
	    if (opt_sec) {
		vxtext.drawText("Time in Seconds", timerange/2, 1070, 30, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_0, Visualizer.STYLE_BOLD);
	    } else {
		vxtext.drawText("Time in Milliseconds", timerange/2, 1070, 30, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_0, Visualizer.STYLE_BOLD);
	    }

	    int i=0;
	    long time = 0;
	    /* skip data until starttime */
	    for(i=0; i<nswitches; i++) {
		if (time >= starttime) break;
		time += switches[i].timediff;
	    }
	    
	    time = 0;

	    ThreadInfo current = switches[i-1].to;
	    for(; i<nswitches && time < timerange; i++) {
		String fill = "5";
		if (current != null) {
		    /* switch to draw (current == null means already drawn) */
		    int endi = i+1;
		    long timediff = switches[i].timediff;
		    if (opt_optimized && timediff < timerresolution) {
			long tmptimediff = 0;
			for(int k=endi; k<endi+readahead && k < nswitches-1; k++) {
			    tmptimediff += switches[k].timediff;
			    if (tmptimediff > timerresolution 
				|| switches[k+1].timediff > timerresolution 
				) break;
			    if(switches[k].to == current) {
				endi = k;
				switches[k].to = null; // we draw this switch together with the current one
				timediff += tmptimediff+ switches[k+1].timediff;
				tmptimediff = 0;
				//System.out.println("Found backswitch "+sw0.to.number+" at time "+(time+timediff));
				k++;
			    }
			}
			if (timediff != switches[i].timediff) {
			    //System.out.println("Connect "+sw0.to.number+" at time "+time);
			    fill = "0";
			}
		    }
		    if (time + timediff > timerange) timediff = timerange - time;
		    v.drawRect(time, current.number*10, timediff, 10, fill);
		    if (opt_verbose_switch) System.out.println(current.number+":"+time+".."+(time+timediff));
		} else {
		    if (opt_verbose_switch) System.out.println("skip "+i+"timediff="+switches[i].timediff);
		}
		time += switches[i].timediff;
		current = switches[i].to;
		if (i%n5==0) System.out.print("\b\b\b"+(i*100/nswitches)+"%");
	    }
	    System.out.println("\b\b\b"+100+"%");
	}
	vis.finish();	
    }


    void readSwitchesBinary(InputStream input) throws IOException {
	// read data
	LittleEndianInputStream in = new LittleEndianInputStream(new BufferedInputStream(input));
	
	long time;
	endTime = 0;
	int i=0;
	int nthreads=in.readInt();
	threads = new ThreadInfo[nthreads];
	for(i=0;i<nthreads;i++) {
	    int domainID=in.readInt();
	    int threadID=in.readInt();
	    String name = in.readString2ByteAligned();
	    //System.out.println("T: "+domainID+"."+threadID);
	    ThreadInfo info = new ThreadInfo();
	    info.domainID = domainID;
	    info.threadID = threadID;
	    info.number = numberOfThreads++;
	    info.name = name;
	    threads[i] = info;
	}
	try {
	    nswitches=in.readInt();
	    switches = new Switch[nswitches];
	    if (! opt_verbose_read) System.out.print("Reading data:    0% ");
	    int n5 = nswitches/5;
	    for(i=0;i<nswitches;i++) {
		time=in.readLong();
		time = time * 1000 / MHZ;
		int domainID=in.readInt();
		int threadID=in.readInt();
		Switch sw = new Switch();
		sw.timediff = time;
		if (opt_verbose_read) {
		    System.out.println(i+": "+time+", "+domainID+"."+threadID);
		    if (threadID == 1) {System.out.println("GC");throw new Error();}
		}
		for(int j=0; j<threads.length; j++) {
		    ThreadInfo info = threads[j];
		    if (domainID == info.domainID && threadID == info.threadID) {
			sw.to = info; 
			break;
		    }
		}
		if (sw.to == null)  throw new Error();
		
		endTime += time;
		switches[i] = sw;
		
		if (! opt_verbose_read) if (i%n5==0) System.out.print("\b\b\b"+(i*100/nswitches)+"%");
		
	    }
	} catch(EOFException e) {
	    nswitches = i-1;
	}
	if (! opt_verbose_read) System.out.println("\b\b\b"+100+"%");
    }    



    
    void readSwitchesText(InputStream input) throws IOException {
	DataInputStream in = new DataInputStream(input);
	// read data
	Vector switches0 = new Vector();
	Vector threads0 = new Vector();
	String s;
	while((s=in.readLine()) != null) {
	    s = s.trim();
	    if (s.length() == 0) break;
	    //   System.out.println(s);
	    char c[] = s.toCharArray();
	    // read thread ID
	    int i=0;
	    int domainID=0; for(; i < c.length && c[i] != '.'; i++) domainID = domainID * 10 + c[i] - '0';  
	    int threadID=0; for(i++; i < c.length && c[i] != ' '; i++) threadID = threadID * 10 + c[i] - '0';  

	    ThreadInfo info = new ThreadInfo();
	    info.domainID = domainID;
	    info.threadID = threadID;
	    info.number = numberOfThreads++;
	    if (i== c.length) {
		info.name = null;
	    } else {
		i++;
		info.name = new String(c, i, c.length-i).trim();
	    }
	    //System.out.println(info);
	    threads0.addElement(info);
	}

	threads = new ThreadInfo[threads0.size()];
	threads0.copyInto(threads);
	threads0 = null;
	System.out.println(threads.length+" threads.");

	endTime = 0;
	while((s=in.readLine()) != null) {
	    s = s.trim();
	    //System.out.println(s);
	    if (s.length() == 0) continue;
	    if (s.charAt(0) == '#') continue;

	    char c[] = s.toCharArray();
	    int i=0;
	    long time=0; for(; i < c.length && c[i] != ' '; i++) time = time * 10 + c[i] - '0';  

	    for(; i < c.length && c[i] == ' '; i++);
	    
	    int domainID=0; for(; i < c.length && c[i] != '.'; i++) domainID = domainID * 10 + c[i] - '0';  
	    int threadID=0; for(i++; i < c.length && c[i] != ' '; i++) threadID = threadID * 10 + c[i] - '0';  

	    Switch sw = new Switch();
	    sw.timediff = time;
	    for(i=0; i<threads.length; i++) {
		ThreadInfo info = threads[i];
		if (domainID == info.domainID && threadID == info.threadID) {
		    sw.to = info; 
		    break;
		}
	    }
	    if (sw.to == null)  throw new Error();
	    endTime += time;
	    switches0.addElement(sw);
	}
	switches = new Switch[switches0.size()];
	switches0.copyInto(switches);
	nswitches = switches.length;
	System.out.println(switches.length+" switches.");
    }    
    
    

}
