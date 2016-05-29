package jx.verifier;

public class VerifierOptions {
    static final public String helpString = "Verifier Options:\n" +
				"[+/-typecheck] - enable/disable standard Java Verification (Default: true)\n"+
				"[+/-npa] - enable/disable Null Pointer Analysis (Default: false)\n"+
				"[+/-fla] - enable/disable check for final and leaf methods (Default: false)\n"+
				"[+/-wcet] - enable/disable Worst Case Execution Time Analysis (Default: false)\n"+
				"[+wcet: <Package>/<Class>.<Method> <MaxTime>] - enable WCETime Analysis for one Method\n"+
				"-debug - enable Debug Mode\n" +
				"-silent - enable silent mode (minimal outputs)\n"+
				"[-h/-?/--help] - Display this message";
		
    public boolean doTypecheck=true; //Std. Java Verification
    public boolean doNPA=false; //Null Pointer Analysis
    public boolean doFLA = false; //SystemFinal and Leaf Analysis
    public boolean doWCET = false; //WorstCase Execution Time Analysis
    public int debugMode = 1; //0 - silent(no outputs at all!), 1 normal, 2 verbose

    public String wcetMethodArg=null;
    public int WCETmaxTime = 0;

    public String[] parseArgs(String[] args) {
	String[] tmp = new String[args.length];
	int argscount = 0;
	
	//process every option and take out those that we know.
	for (int i =0; i< args.length; i++) {
	    if (args[i].toLowerCase().equals("-typecheck")) {
		doTypecheck = false;
	    } else if (args[i].toLowerCase().equals("+typecheck")) {
		doTypecheck = true;
	    } else if (args[i].toLowerCase().equals("-npa")) {
		doNPA = false;
	    } else if (args[i].toLowerCase().equals("+npa")) {
		doNPA = true;
	    } else if (args[i].toLowerCase().equals("-fla")) {
		doFLA = false;
	    } else if (args[i].toLowerCase().equals("+fla")) {
		doFLA = true;
	    } else if (args[i].toLowerCase().equals("-wcet")) {
		doWCET = false;
	    } else if (args[i].toLowerCase().equals("+wcet")) {
		doWCET = true;
	    } else if (args[i].toLowerCase().equals("+wcet:")) {
		doWCET = true;
		i++;
		wcetMethodArg = args[i];
		i++;
		WCETmaxTime = Integer.parseInt(args[i]);
	    } else if (args[i].toLowerCase().equals("-debug")) {
		debugMode = 2;
	    } else if (args[i].toLowerCase().equals("-silent")) {
		debugMode = 0;
	    } else if(args[i].toLowerCase().equals("-h") || 
		      args[i].toLowerCase().equals("-?") || 
		      args[i].toLowerCase().equals("--help")) {
		System.out.println(helpString);
		System.exit(0);
	    } else {
		//unknown option --> leave untouched
		tmp[argscount]=args[i];
		argscount++;
	    }
	}

	//copy leftover arguments into String of appropriate length
	String[]ret = new String[argscount];
	for (int i=0; i<ret.length; i++) {
	    ret[i] = tmp[i];
	}
	return ret;	
    }
}
	

