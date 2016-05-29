package jx.vis;

import java.util.*;
import java.io.*;
import jx.formats.*;

public class IOZoneDiagram {
    class Result {
	int filesize, recordsize, reread;
	float variance,deviation;
	float reread_compare;
    }
    class FileSize {
	int filesize;
	Vector results = new Vector();
    }
    Vector results = new Vector();

    boolean opt_deviation = false;
    boolean opt_recordsizes = false;
    boolean opt_verbose = false;
    boolean opt_max_deviation = false;

    boolean compare = false;
    boolean binary = false;
    boolean better = false;

    double max_deviation;

    static void usage() {
	System.out.println("bars <mifout> <max reread throughput> <helping line interval> <iozone.log>");
	System.out.println("barsvar <mifout> <max reread throughput> <helping line interval> <iozone.log ...>");
	System.out.println("compare <mifout> <max %> <helping line interval> [-binary|-text] <iozone.log ...> [-binary|-text] <iozone.log ...>");
	System.out.println("    Throughput of iozone2.log relative to iozone1.log");
	System.out.println("compare_better <mifout> <min %> <max %> <helping line interval> [-binary|-text] <iozone.log ...> [-binary|-text] <iozone.log ...>");
	System.out.println("    Same as compare but with 100% as base line");
	System.out.println("");
	System.out.println("Use prefix \"bin\" to process binary log files.");
	System.out.println("Use \"-recsizes\" to print record sizes on the x-axis.");
	System.out.println("Use \"-deviation\" to print record sizes on the x-axis.");
	System.out.println("Use \"-max_deviation\" to print the maximal standard deviation.");
	System.out.println("Use \"-verbose\" for more blubber.");
	System.out.println("");
	System.out.println("Examples:");
	System.out.println("bars myiozone.mif 700000 100000 myiozone.log");
	System.out.println("binbars myiozone.mif 700000 100000  myiozone.log");
	System.out.println("binbarsvar myiozone.mif 700000 100000  myiozone0.log myiozone1.log myiozone2.log myiozone3.log");
	System.out.println("compare myiozone.mif 120 10 -binary youriozone0.log youriozone1.log -text myiozone.log");
	System.out.println("compare_better myiozone.mif -20 50 10 -binary youriozone0.log youriozone1.log -binary myiozone.log");
    }
    public static void main(String[] args) throws Exception {
	new IOZoneDiagram(args);
    }
    public IOZoneDiagram(String[] args) throws Exception {
	if (args.length < 1) { usage(); return; }


	String miffile=null;
	int maxtp;
	int mintp=0;
	int maxtp_scale=0;
	int steptp;
	int scale_legend = 1;

	if (args[0].startsWith("bin")) {
	    binary = true;
	    args[0] = args[0].substring(3);
	}

	int argc;
	for(argc=1;;argc++) {
	    if (args[argc].equals("-recsizes")) {
		opt_recordsizes = true;
	    } else if (args[argc].equals("-deviation")) {
		opt_deviation = true;
	    } else if (args[argc].equals("-max_deviation")) {
		opt_max_deviation = true;
	    } else if (args[argc].equals("-verbose")) {
		opt_verbose = true;
	    } else {
		break;
	    }
	}

	if (args[0].equals("bars")) {
	    if (args.length != 5) { usage(); return; }
	    miffile = args[argc++];
	    maxtp = Integer.parseInt(args[argc++]);
	    steptp = Integer.parseInt(args[argc++]);
	    mintp = -200000;
	    scale_legend = 1000;
	    DataInputStream in = new DataInputStream(new FileInputStream(args[4]));
	    results = readIOZone(in);
	} else if (args[0].equals("barsvar")) {
	    scale_legend = 1000;
	    //if (! binary) throw new Error("barsvar only supports binary input!"); 
	    if (args.length < 5) { usage(); return; }

	    miffile = args[argc++];
	    maxtp = Integer.parseInt(args[argc++]);
	    steptp = Integer.parseInt(args[argc++]);

	    String[] names = new String[args.length-argc];
	    System.arraycopy(args, argc, names, 0, names.length);
	    results = readDataSet(names, binary);
	    
	} else if (args[0].equals("compare") || args[0].equals("compare_better")) {
	    compare = true;
	    if (args[0].equals("compare_better")) better = true;
	    miffile = args[argc++];
	    if (better) {
		mintp = Integer.parseInt(args[argc++]);
		maxtp_scale = 60;
	    }
	    maxtp = Integer.parseInt(args[argc++]);
	    steptp = Integer.parseInt(args[argc++]);



	    int first=-1;
	    boolean first_bin = false;
	    int second=-1;
	    boolean second_bin = false;
	    for(int i=argc; i<args.length; i++) {
		if (args[i].equals("-binary") || args[i].equals("-text")) {
		    if (first==-1) {
			first = i;
			if (args[i].equals("-binary")) first_bin = true;
		    }
		    else {
			second = i;
			if (args[i].equals("-binary")) second_bin = true;
		    }
		}
	    }
	    if (first==-1||second==-1||first>second) throw new Error("no first set or no second set");
	    String[] names0 = new String[second-first-1];
	    System.arraycopy(args, first+1, names0, 0, names0.length);
	    Vector results0;
	    results0 = readDataSet(names0, first_bin);
	    String[] names1 = new String[args.length-second-1];
	    System.arraycopy(args, second+1, names1, 0, names1.length);
	    Vector results1 = readDataSet(names1, second_bin);

	    results = new Vector();
	    if (results0.size() != results1.size()) throw new Error("different result set sizes");
	    for(int i=0; i<results0.size(); i++) {
		FileSize f0 = (FileSize) results0.elementAt(i);	    
		FileSize f1 = (FileSize) results1.elementAt(i);	    
		if (f0.results.size() != f1.results.size()) throw new Error("different result set sizes for one file size");
		FileSize f = new FileSize();
		f.results = new Vector();
		f.filesize = f0.filesize;
		results.addElement(f);
		for(int j=0; j<f0.results.size(); j++) {
		    Result r0 = (Result)f0.results.elementAt(j);
		    Result r1 = (Result)f1.results.elementAt(j);
		    Result r = new Result();
		    f.results.addElement(r);
		    r.filesize = r0.filesize;
		    r.recordsize = r0.recordsize;
		    if (better) {
			//r.reread_compare = (float) (100.0 - (r1.reread*100.0 /  r0.reread));
			r.reread_compare = (float) ((r0.reread-r1.reread)*100.0 /  r1.reread);
		    } else {
			r.reread_compare = (float)(r1.reread*100.0 /  r0.reread);
		    }
		    if (opt_verbose) System.out.println(r0.reread +" & "+ r1.reread +" -> "+r.reread_compare);
		}
	    }
	} else {
	    usage(); return; 
	}

	if (! compare && ! better) {
	}

	if (maxtp_scale==0) {//throw new Error("ERROR maxtp_scale");
	    maxtp_scale = maxtp - mintp;
	}
	Visualizer vis = new jx.vis.mif.MIFVisualizer(new FileOutputStream(miffile));


	// ----   draw coordinate system and legend

	vis.init();

	int X0 = 50;
	int X1 = 700;
	int Y0 = 10;
	int Y1 = 500;

	// coordinate system
	//vis.drawLine(X0, Y0, X0, Y1);
	//vis.drawLine(X0, Y1, X1, Y1);

	Visualizer v0 = new ScalingVisualizer(vis, 50, 10, 1000, maxtp_scale*1000, 650, 490, true, -mintp*1000);
	for(int i=mintp; i<maxtp; i+= steptp) {
	    //int fonth = steptp*2/4;
	    int fonth =  (maxtp_scale * 1000 * 60) / 1000;
	    int fonty = i*1000-(fonth/3);
	    int legend = i / scale_legend;
	    v0.drawText(""+legend, -5, fonty, fonth, Visualizer.ALIGN_RIGHT);
	    v0.drawThinLine(0, i*1000, 1000, i*1000);
	}
	v0.drawLine(0, 0, 1000, 0); // x-axis
	v0.drawLine(0, mintp*1000, 0, maxtp*1000); // y-axis
	v0 = null;


	// ----   draw data

	Visualizer v = new ScalingVisualizer(vis, 50, 10, 1000, maxtp_scale, 650, 490, true, -mintp);
	Visualizer vcanon = new ScalingVisualizer(vis, 50, 10, 1000, 1000, 650, 490, true, -mintp);

	int n=0;
	for(int i=0; i<results.size(); i++) {
	    FileSize f = (FileSize) results.elementAt(i);
	    n += f.results.size() + 1;
	}

	int k = 1;
	String[] fill =  {"0", "1", "2", "3", "4", "5", "6", "7"};

	int filesize_h = 60;
	int filesize_y = -filesize_h;
	int recsize_h = 40;
	int recsize_y = -recsize_h;
	if (opt_recordsizes) {
	    filesize_y -= recsize_h;
	} 
	int throughput_x = -130;

	if (opt_max_deviation) {
	    vcanon.drawText("Maximal deviation: "+max_deviation+"%", 300, 900, filesize_h, Visualizer.ALIGN_LEFT, Visualizer.ROTATE_0, Visualizer.STYLE_BOLD);
	}

	vcanon.drawText("filesize in KBytes", 500, filesize_y-filesize_h, filesize_h, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_0, Visualizer.STYLE_BOLD);
	if (compare) {
	    if (better) {
		vcanon.drawText("improvement in percent", throughput_x, 500, filesize_h, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_270, Visualizer.STYLE_BOLD);
	    } else {
		vcanon.drawText("achieved throughput in percent", throughput_x, 500, filesize_h, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_270, Visualizer.STYLE_BOLD);
	    }
	} else {
	vcanon.drawText("throughput in MBytes/sec", throughput_x, 500, filesize_h, Visualizer.ALIGN_CENTER, Visualizer.ROTATE_270, Visualizer.STYLE_BOLD);
	}

	for(int i=0; i<results.size(); i++) {
	    FileSize f = (FileSize) results.elementAt(i);	    
	    int wi = 1000 *  f.results.size() / n;
	    vcanon.drawText(""+f.filesize, 1000 * k / n + wi/2, filesize_y, filesize_h, Visualizer.ALIGN_CENTER);
	    for(int j=0; j<f.results.size(); j++) {
		Result r = (Result)f.results.elementAt(j);
		if (compare) {
		    v.drawRect(1000 * k / n, 0, 1000 * 1 / n, (int)r.reread_compare, fill[j]);
		} else {
		    v.drawRect(1000 * k / n, 0, 1000 * 1 / n, r.reread, fill[j]);
		}
		if (opt_recordsizes) {
		    vcanon.drawText(""+(r.recordsize/1024), 1000 * k / n + 500/n, recsize_y, recsize_h, Visualizer.ALIGN_CENTER);
		}
		if (opt_deviation) {
		    if (r.deviation != 0) {
			int xm = 1000 * k/n + 500 / n; 
			v.drawLine(xm, r.reread - (int)(r.deviation/2), xm, r.reread+(int)(r.deviation/2));
		    }
		}
		k++;
	    }
	    k++;
	}

	vis.finish();	
    }


    Vector readIOZone(InputStream din) throws IOException {
	if (binary) {
	    return readIOZoneBinary(din);
	} else {
	    return readIOZoneText(din);
	}
    }

    Vector readIOZoneText(InputStream din) throws IOException {
	DataInputStream in = new DataInputStream(din);
	// read data
	Vector results = new Vector();
	String s;
	int curfilesize=0;
	FileSize fs=null;
	while((s=in.readLine()) != null) {
	    s = s.trim();
	    //System.out.println(s);
	    if (s.length() == 0) continue;
	    if (s.startsWith("# START IOZONE") && results.size() > 0) return results; 
	    if (s.charAt(0) == '#') continue;
	    if (s.equals("mbsec")) continue;
	    
	    StringTokenizer st = new StringTokenizer(s);
	    Result r = new Result();
	    r.filesize = Integer.parseInt(st.nextToken());
	    r.recordsize = Integer.parseInt(st.nextToken());
	    int write = Integer.parseInt(st.nextToken());
	    int rewrite = Integer.parseInt(st.nextToken());
	    int read = Integer.parseInt(st.nextToken());
	    r.reread = Integer.parseInt(st.nextToken());
	    if (curfilesize != r.filesize) {
		curfilesize = r.filesize;
		fs = new FileSize();
		fs.filesize = r.filesize;
		results.addElement(fs);
	    }
	    fs.results.addElement(r);
	}
	if (results.size() == 0) return null;
	return results;
    }    

    Vector readIOZoneBinary(InputStream din) throws IOException {
	LittleEndianInputStream in = new LittleEndianInputStream(din);
	int KILOBYTES_START = in.readInt();
	int KILOBYTES_END = in.readInt();
	int RECLEN_START = in.readInt();
	int RECLEN_END = in.readInt();

	Vector results = new Vector();
	int curfilesize=0;
	FileSize fs=null;
	int j=0;
	for(int kilo=KILOBYTES_START; kilo<=KILOBYTES_END; kilo *= 2) {
	    fs = new FileSize();
	    fs.filesize = kilo;
	    results.addElement(fs);
	    for(int reclen=RECLEN_START; reclen <= RECLEN_END && reclen <= kilo*1024; reclen *= 2) {
		Result r = new Result();
		r.filesize = kilo;
		r.recordsize = reclen;
		r.reread = (int)((kilo*1024.0*1024.0)/in.readInt()); 
		fs.results.addElement(r);
	    }
	}
	return results;
    }    


    Vector readDataSet(String[] names, boolean binary) throws IOException {
	Vector all = new Vector();
	Vector r;
	int k=0;
	if (opt_verbose) System.out.println("---- ");
	for(int i=0;i<names.length;i++) {
	    if (opt_verbose) System.out.println("read "+names[i]);
	    DataInputStream in = new DataInputStream(new FileInputStream(names[i]));
	    if (binary) r = readIOZoneBinary(in);
	    else r = readIOZoneText(in);
	    in.close();
	    all.addElement(r);
	}
	// compute median and variance
	Vector results = new Vector();
	for(int i=0; i<((Vector) all.elementAt(0)).size(); i++) {
	    FileSize f0 = ((FileSize) ((Vector)all.elementAt(0)).elementAt(i));
	    int size = f0.results.size();
	    int filesize = f0.filesize;
	    FileSize fsize = new FileSize();
	    fsize.filesize = filesize;
	    results.addElement(fsize);
	    for(int j=0; j<size; j++) {
		int recordsize = ((Result)f0.results.elementAt(j)).recordsize;
		if (opt_verbose) System.out.print(filesize+":"+recordsize+":");
		Result res = new Result();
		// compute median
		for(int a=0; a<all.size(); a++) {
		    Vector resultsx = (Vector) all.elementAt(a);
		    FileSize f = (FileSize) resultsx.elementAt(i);	    
		    Result r0 = (Result)f.results.elementAt(j);
		    if (opt_verbose) System.out.print(" "+r0.reread);
		    res.reread += r0.reread;
		}
		res.reread /= all.size();
		res.filesize = filesize;
		res.recordsize = recordsize;
		fsize.results.addElement(res);
		if (opt_verbose) System.out.print(" -> "+res.reread);
		// compute variance
		for(int a=0; a<all.size(); a++) {
		    Vector results0 = (Vector) all.elementAt(a);
		    FileSize f = (FileSize) results0.elementAt(i);	    
		    Result r0 = (Result)f.results.elementAt(j);
		    float diffFromMedian = r0.reread-res.reread;
		    if (opt_verbose) System.out.print(" "+diffFromMedian);
		    res.variance += diffFromMedian*diffFromMedian / all.size();
		}
		res.deviation = (float)Math.sqrt(res.variance);
		double devpercent = (int)(res.deviation*100000.0/res.reread)/1000.0;
		if (devpercent>max_deviation) max_deviation = devpercent;

		if (opt_verbose) System.out.println(" -> "+res.variance + " -> "+res.deviation+ " -> "+devpercent+"%");
	    }
	}

	return results;
    }
    
}
