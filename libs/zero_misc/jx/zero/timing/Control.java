package jx.zero.timing;

import jx.zero.*;

public class Control {

    private final static boolean use_standart_value = false;
    private final static int     cal_rounds = 1000;

    private static void timing_empty_method() {
    }

    private static void notiming_empty_method() {
    }

    private static void timing_adjust_raw() {
	notiming_empty_method();
    }

    private static void timing_adjust_boiled() {
	timing_empty_method();
    }

    private static boolean calibrate_timing() {
	Profiler p = (Profiler) InitialNaming.getInitialNaming().lookup("Profiler");
	boolean result = false;

	if (use_standart_value) {
	    p.startCalibration();

            int t1 = 37;
	    int t2 = 48;
	    int t3 = 146;
	    
	    result = p.endCalibration(t1,t2,t3);	    
	} else {
	    timing_adjust_boiled();
	    timing_adjust_raw();
	    
	    p.startCalibration();
	    
	    for (int i=0;i<cal_rounds;i++) {
		timing_adjust_boiled();
	    }
	    
	    for (int i=0;i<cal_rounds;i++) {
		timing_adjust_raw();
	    }	   	    

	    int t1 = p.getAverageCyclesOfMethod("jx.zero.timing.Control.timing_empty_method");
	    int t2 = p.getAverageCyclesOfMethod("jx.zero.timing.Control.timing_adjust_raw");
	    int t3 = p.getAverageCyclesOfMethod("jx.zero.timing.Control.timing_adjust_boiled");
	    
	    result = p.endCalibration(t1,t2,t3);
	}
	    
	for (int i=0;i<cal_rounds+100;i++) {
	  timing_adjust_boiled();
	}

	for (int i=0;i<cal_rounds+100;i++) {
	  timing_adjust_raw();
	}

	return result;
    }
   
    public static void startTiming() {
	calibrate_timing();	
    }

    public static void shell() {
	Profiler p = (Profiler) InitialNaming.getInitialNaming().lookup("Profiler");
	p.shell();
    }
}
