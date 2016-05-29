package jx.compiler;

public class StatisticInfo {

    String libname;

    int inline_succed_count;
    int inline_try_count;

    int invoke_virtual;
    int invoke_static;
    int invoke_interface;

    int exception_calls;

    public StatisticInfo() {
	inline_succed_count = 0;
	inline_try_count = 0;
    }

    public StatisticInfo(String libname) {
	this();
	this.libname=libname;
    }

    public void invoke_virtual() {
	invoke_virtual++;
    }

    public void invoke_static() {
	invoke_static++;
    }

    public void invoke_interface() {
	invoke_interface++;
    }
    
    public void tryinline() {
	inline_try_count++;
    }

    public void inline() {
	inline_succed_count++;
    }

    public void exception_calls() {
	exception_calls++;
    }

    public void exception_calls(int value) {
	exception_calls += value;
    }

    public String toString() {
	String ret;
	if (libname!=null) {
	    ret = "INVOKE:\tv:"+invoke_virtual+" s:"+invoke_static+" i:"+invoke_interface+" \t"+libname+"\n";
	} else {
	    ret = "INVOKE:\tv:"+invoke_virtual+" s:"+invoke_static+" i:"+invoke_interface+"\n";
	}
	if (libname!=null) {
	    ret += "EX:\t "+exception_calls+" \t"+libname+"\n";
	} else {
	    ret += "EX:\t "+exception_calls+"\n";
	}
	if (libname!=null) {
	    ret += "INLINE: "+inline_succed_count+"/"+inline_try_count+" \t"+libname;
	} else {
	    ret += "INLINE: " + inline_succed_count + "/"+inline_try_count;
	}
	return ret;
    }
}
