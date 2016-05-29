package java.lang;

/*import jx.zero.debug.DebugInputStream;
import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugChannel;
*/

import jx.zero.InitialNaming;
import jx.zero.Naming;
import jx.zero.Clock;
import jx.zero.VMSupport;
import jx.zero.DomainManager;

import java.io.InputStream;
import java.io.PrintStream;


// DEBUG
import jx.zero.debug.DebugChannel;
import java.io.*;
import java.util.Properties;
// DEBUG

//class Properties{}

public class System {
    
    /*
    public static InputStream in= new DebugInputStream(debugChannel);
    public static PrintStream out= new DebugPrintStream(new DebugOutputStream(debugChannel));
    public static PrintStream err= new DebugPrintStream(new DebugOutputStream(debugChannel));
    */
    public static InputStream in= null;
    public static PrintStream out= null;
    public static PrintStream err= null;

    private static Properties properties = null;
    private static final boolean usePlugin = false;

    private static Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
    //    private static long correct;

    /**
      Copies an array from the specified source array, beginning at the specified position,
      to the specified position of the destination array.
    */

    public static void arraycopy(Object src, int srcOffset, Object dst, int dstOffset, int count) {	
	/* see jx/compiler/plugin/VMSupport.java for implementation details */
	if (usePlugin) {
	    if( src == dst && dstOffset > srcOffset ){
		if (src instanceof byte[] && dst instanceof byte[]) {
		    VMSupport.arraycopy_byte_left((byte[])src,srcOffset,(byte[])dst,dstOffset,count);
		} else if (src instanceof char[] && dst instanceof char[]) {
		    VMSupport.arraycopy_char_left((char[])src,srcOffset,(char[])dst,dstOffset,count);
		} else {
		    VMSupport.arraycopy_left((Object[])src,srcOffset,(Object[])dst,dstOffset,count);
		}
	    } else {
		if (src instanceof byte[] && dst instanceof byte[]) {
		    VMSupport.arraycopy_byte_right((byte[])src,srcOffset,(byte[])dst,dstOffset,count);
		} else if (src instanceof char[] && dst instanceof char[]) {
		    VMSupport.arraycopy_char_right((char[])src,srcOffset,(char[])dst,dstOffset,count);
		} else {
		    VMSupport.arraycopy_right((Object[])src,srcOffset,(Object[])dst,dstOffset,count);
		}
	    }
	} else {
	    if( src == dst && dstOffset > srcOffset ){
		if (src instanceof byte[] && dst instanceof byte[]) {
		    byte []s = (byte[])src;
		    byte []d = (byte[])dst;
		    srcOffset += count;
		    dstOffset += count;
		    for(int i=0; i<count; ++i) {		    
			d[--dstOffset] = s[--srcOffset];
		    }
		} else if (src instanceof char[] && dst instanceof char[]) {
		    char []s = (char[])src;
		    char []d = (char[])dst;
		    srcOffset += count;
		    dstOffset += count;
		    for(int i=0; i<count; ++i) {		    
			d[--dstOffset] = s[--srcOffset];
		    }
		} else if (src instanceof int[] && dst instanceof int[]) {
		    int []s = (int[])src;
		    int []d = (int[])dst;
		    srcOffset += count;
		    dstOffset += count;
		    for(int i=0; i<count; ++i) {		    
			d[--dstOffset] = s[--srcOffset];
		    }
		} else {
		    Object []s = (Object[])src;
		    Object []d = (Object[])dst;
		    srcOffset += count;
		    dstOffset += count;
		    for(int i=0; i<count; ++i) d[--dstOffset] = s[--srcOffset];
		}
	    } else {
		if (src instanceof byte[] && dst instanceof byte[]) {
		    byte []s = (byte[])src;
		    byte []d = (byte[])dst;
		    if (dstOffset+count >  d.length) throw new Error();
		    if (srcOffset+count >  s.length) throw new Error();
		    for(int i=0; i<count; i++) {		    
			d[dstOffset+i] = s[srcOffset+i];
		    }
		} else if (src instanceof char[] && dst instanceof char[]) {
		    char []s = (char[])src;
		    char []d = (char[])dst;
		    if (dstOffset+count >  d.length) throw new Error();
		    if (srcOffset+count >  s.length) throw new Error();
		    for(int i=0; i<count; i++) {		    
			d[dstOffset+i] = s[srcOffset+i];
		    }
		} else if (src instanceof int[] && dst instanceof int[]) {
		    int []s = (int[])src;
		    int []d = (int[])dst;
		    if (dstOffset+count >  d.length) throw new Error();
		    if (srcOffset+count >  s.length) throw new Error();
		    for(int i=0; i<count; i++) {		    
			d[dstOffset+i] = s[srcOffset+i];
		    }
		} else {
		    //dout.println("arraycopyOBJ: src="+src.getClass().getName() + " dst="+dst.getClass().getName());
		    if (src.getClass().getName().equals("[[B") && dst.getClass().getName().equals("[B")) {
			//dout.println(" hash: "+ Integer.toHexString(src.hashCode()));
			throw new Error("CAST");
		    }
		    Object []s = (Object[])src;
		    Object []d = (Object[])dst;
		    if (dstOffset+count >  d.length) throw new Error();
		    if (srcOffset+count >  s.length) throw new Error();
		    for(int i=0; i<count; i++) {		    
			d[dstOffset+i] = (Object)s[srcOffset+i];
		    }
		}
	    }
	}
    }

    /*
    public static void arraycopy(char[] src, int srcOffset, char[] dst, int dstOffset, int count) {
       if( src == dst && dstOffset > srcOffset ){   // beware of overlapping self-copy 
	  srcOffset += count;
	  dstOffset += count;
	  for(int i=0; i<count; i++)
	    dst[--dstOffset] = src[--srcOffset];
       } 
       else 
	 for(int i=0; i<count; i++)
	   dst[dstOffset+i] = src[srcOffset+i];
    }
    */

    public static void setProperties(Properties props) {
	properties = props;
    }

    public static Properties getProperties() { 
	return properties;
    }

    public static String getProperty(String key)
    {
	setDefaultProperties(); // Hack
	return properties.getProperty(key);
    }

    public static String getProperty(String key, String def)
    {
	setDefaultProperties(); // Hack
	String value = properties.getProperty(key);
	if (value==null) return def;
	return value;
    }

    /*
    public static SecurityManager getSecurityManager()
    {
	return null;
    }

    public static void setSecurityManager(SecurityManager s)
    {
    }
    */

    public static long currentTimeMillis() { 
	int ticks = clock.getTimeInMillis();
	return ticks;
    }

    public static void exit(int status) {
	DomainManager domainManager = (DomainManager) InitialNaming.getInitialNaming().lookup("DomainManager");
	domainManager.terminateCaller();
    }

    public static void gc()
    {
    }

    public static void load(String filename)
    {
    }

    public static void loadLibrary(String libname)
    {
    }

    public static int identityHashCode(Object obj) { 
	return 42; // not the best choice for hashing but nevertheless correct
	//throw new Error("NOT IMPLEMENTED");
    }

    private static void setDefaultProperties() {
	if (properties==null) {
	    properties = new Properties();

	    properties.put("java.version","0.9"); // Java Runtime Environment version
	    properties.put("java.vendor","FAU Erlangen IMMD 4"); // Java Runtime Environment vendor
	    properties.put("java.vendor.url","http://www4.informatik.uni-erlangen.de"); // Java vendor.url
	    properties.put("java.home",""); // Java installation directory  
	    properties.put("java.vm.specification.version","0.9"); //  Java Virtual Machine specification version
	    properties.put("java.vm.specification.vendor","FAU Erlangen IMMD 4"); // Java Virtual Machine specification vendor
	    properties.put("java.vm.specification.name","JXOS");  // Java Virtual Machine specification name
	    properties.put("java.vm.version","0.9");//  Java Virtual Machine implementation version
	    properties.put("java.vm.vendor","FAU Erlangen IMMD4"); //Java Virtual Machine implementation vendor  
	    properties.put("java.vm.name","JXOS"); //  Java Virtual Machine implementation name
	    properties.put("java.specification.version","0.9"); // Java Runtime Environment specification version  
	    properties.put("java.specification.vendor","FAU Erlangen IMMD4"); //  Java Runtime Environment specification vendor 
	    properties.put("java.specification.name","JXOS");// Java Runtime Environment specification name  
	    properties.put("java.class.version","0.9"); // Java class format version number
	    properties.put("java.class.path","");// Java class path  
	    properties.put("java.ext.dirs",""); //  Path of extension directory or directories

	    properties.put("os.name","JXOS"); // Operating system name  
	    properties.put("os.arch","i586"); // Operating system architecture  
	    properties.put("os.version","0.01"); // Operating system version  

	    properties.put("file.separator","/"); // File separator ("/" on UNIX) 
	    properties.put("path.separator",":"); //  Path separator (":" on UNIX)
	    properties.put("line.separator","\n"); 
	    
	    properties.put("user.name","golm");
	    properties.put("user.home","Martensstrasse 3"); // User's home directory  	    
	    properties.put("user.dir","/"); //  User's current working directory
	}
    }
}

