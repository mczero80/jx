package jx.zero;

import jx.zero.debug.*;
import java.util.Vector;

public class DomainStarter {

  private static BootFS bootfs;
  private static ByteCodeTranslater onc;



  public static Domain createDomain(              String libName, String mainclass,                  int gcinfo0) {
    return createDomain(libName+" - "+mainclass, libName, mainclass,null, gcinfo0, -1, -1);
  }

  public static Domain createDomain(              String libName, String mainclass,                  int gcinfo0, int gcinfo1, int gcinfo2) {
    return createDomain(libName+" - "+mainclass, libName, mainclass,null, gcinfo0, gcinfo1, gcinfo2);
  }

  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0) {
    return createDomain(title, libName, mainclass, null, gcinfo0, -1, -1);
  }

  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0, int gcinfo1, int gcinfo2) {
    return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2);
  }

  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0, String[] argv) {
      return createDomain(title, libName, mainclass, null, gcinfo0, -1, -1,  argv, InitialNaming.getInitialNaming());
  }


  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0, int gcinfo1, int gcinfo2, String[] argv) {
      return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2,  argv, InitialNaming.getInitialNaming());
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2) {
    return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, null, InitialNaming.getInitialNaming());
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,                  Naming naming) {
    return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, null, naming);
  }
  
  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  String [] argv) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2,  argv, InitialNaming.getInitialNaming());
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  int codesize, String [] argv) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, codesize, argv, InitialNaming.getInitialNaming(), null, null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, String [] argv, Object[] portals) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, -1, -1,  argv, InitialNaming.getInitialNaming(), null, portals);
  }
  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  String [] argv, Object[] portals) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2,  argv, InitialNaming.getInitialNaming(), null, portals);
  }

  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0, int gcinfo1, int gcinfo2,                  Naming naming, String interceptorName, Object[] portals) {
    return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2, null, naming, interceptorName, portals);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, Naming naming, String interceptorName) {
    return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2, codesize, null, naming, interceptorName, null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, String[] argv, Naming naming, String interceptorName) {
    return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2, codesize, argv, naming, interceptorName, null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, Naming naming) {
      return createDomain(title, libName, mainclass, null, gcinfo0, -1, -1,  -1, null, naming, null, null,0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  String [] argv, Naming naming) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2,  -1, argv, naming, null, null,0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, String [] argv, Naming naming, String interceptorName) {
      return createDomain(title, libName, mainclass, null, gcinfo0, -1, -1, -1, argv, naming, interceptorName, null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  String [] argv, Naming naming, String interceptorName, Object[] portals) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, -1, argv, naming, interceptorName, portals, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int codesize, Naming naming, Object[] portals) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, -1, -1, codesize, null, naming, null, portals, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int codesize, String[] argv, Naming naming, Object[] portals) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, -1, -1, codesize, argv, naming, null, portals, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int codesize) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, -1, -1, codesize, (String[])null, InitialNaming.getInitialNaming(), null, (Object[])null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int codesize, String[] argv) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, -1, -1, codesize, argv, InitialNaming.getInitialNaming(), null, (Object[])null, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2,  int codesize, String [] argv, Object[] portals) {
      return createDomain(title, libName, mainclass, null, gcinfo0, gcinfo1, gcinfo2, codesize, argv, InitialNaming.getInitialNaming(), null, portals, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, Naming naming, Object[] portals) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, gcinfo1, gcinfo2, codesize, null, naming, null, portals, 0);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, int garbageCollector) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, gcinfo1, gcinfo2, codesize, null, InitialNaming.getInitialNaming(), null, (Object[])null, garbageCollector);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, int garbageCollector, Object[] portals) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, gcinfo1, gcinfo2, codesize, (String[])null, InitialNaming.getInitialNaming(), (String)null, portals, garbageCollector);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, String gcInfo3, int gcInfo4, int codesize, Naming naming, int garbageCollector, Object[] portals) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, gcinfo1, gcinfo2, gcInfo3, gcInfo4, codesize, null, naming, null, portals, garbageCollector);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int gcinfo1, int gcinfo2, int codesize, Naming naming, int garbageCollector, Object[] portals) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, gcinfo1, gcinfo2, codesize, null, naming, null, portals, garbageCollector);
  }

  public static Domain createDomain(String title, String libName, String mainclass, int gcinfo0, int codesize, String [] argv, Object[] portals) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, -1, -1, (String)null, -1, codesize, argv, InitialNaming.getInitialNaming(), (String) null, portals, 0);
  }


  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2,  int codesize, String [] argv, Naming naming, String interceptorName, Object[] portals, int garbageCollector) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, (String)null, -1, codesize, argv, naming, interceptorName, portals, garbageCollector);
  }



  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2, String gcinfo3,  int gcinfo4, int codesize, String [] argv, Naming naming, String interceptorName, Object[] portals, int garbageCollector) {
      return createDomain(title, libName, mainclass, schedName, gcinfo0, gcinfo1, gcinfo2, gcinfo3, gcinfo4, codesize, argv, naming, interceptorName, portals, garbageCollector, (int[])null);
  }

  public static Domain createDomain(String title, String libName, String mainclass,                   int gcinfo0, String[] argv, int[] schedinfo) {
      return createDomain(title, libName, mainclass, (String)null, gcinfo0, -1, -1, (String)null, -1, -1, argv, InitialNaming.getInitialNaming(), (String)null, (Object[])null, 0, schedinfo);
  }

  public static Domain createDomain(String title, String libName, String mainclass, String schedName, int gcinfo0, int gcinfo1, int gcinfo2, String gcinfo3,  int gcinfo4, int codesize, String [] argv, Naming naming, String interceptorName, Object[] portals, int garbageCollector, int[] schedinfo) {
    String name;
    String lib;
    String zip;
    String[] schedulers = null;
    String[] libs = null;
    int i;
    
    setupDomain();
    DomainManager dm = (DomainManager)naming.lookup("DomainManager");
    
    /* Debug.message("create Domain "+libName+" via DomainStarter"); */
    
    if ((i=libName.indexOf(".zip"))>0) {
      name = libName.substring(0,i);
      lib  = name+".jll";
      zip  = libName;
    } else if ((i=libName.indexOf(".jll"))>0) {
      name = libName.substring(0,i);
      lib  = libName;
      zip  = name+".zip";
    } else if ((i=libName.indexOf(".jxd"))>0) {
      name = libName.substring(0,i);
      lib  = libName;
      zip  = name+".zip";
    } else {
      name = libName;
      lib  = name+".jll";
      zip  = name+".zip";
    }

    if ((bootfs!=null)&&(!bootfs.lookup(lib))) {
	Debug.message("lib "+lib+" not found");
	if (bootfs.lookup(zip)) {
	    if ((onc==null)&&(!initDomainStarter())) throw new Error("can`t init domain starter");
	    try {
		onc.translate(zip,lib);
	    } catch (Exception ex) {
		throw new Error("can`t init domain starter: "+ex.getClass().getName());
	    }
	} else {
	    //Debug.message("zip "+zip+" not found (can`t create domain)");
	    throw new Error("zip "+zip+" not found (can`t create domain)");
	}
    } else {
	Debug.message("use library "+lib);
    }

    if (schedName != null) {
	 schedulers =  new String[]{schedName};
	 libs = new String[] {"scheduler.jll"};
    }

 
    Domain domain = dm.createDomain(title,   /* title */
				    null,    /* CPU-Objects */
				    schedulers,
				    lib,
				    libs,
				    mainclass,
				    gcinfo0,
				    gcinfo1, gcinfo2,
				    gcinfo3, gcinfo4,
				    codesize,
				    argv,
				    naming,
				    portals,
				    garbageCollector,
				    schedinfo
				    );
    
    CentralSecurityManager secmgr = (CentralSecurityManager)naming.lookup("SecurityManager");
    if ( secmgr != null ) {
	Debug.out.println("!!!!current Domain: "+dm.getCurrentDomain().getName());
	secmgr.inheritPrincipal(dm.getCurrentDomain(), domain);
	if (interceptorName == null)
	    secmgr.inheritInterceptor(dm.getCurrentDomain(), domain);
	else
	    secmgr.installInterceptor(domain,interceptorName);
    }
    
    return domain;
  }

  public static boolean initDomainStarter() {

    String[] neededLibs = new String[] {
      "compiler_env.jll",
      "compiler.jll",
      "classfile.jll",
    };
    
    Naming naming = setupDomain();
    DomainManager dm = (DomainManager)naming.lookup("DomainManager");

    if (bootfs==null) throw new Error("bootfs or compiler_env.jll not found!!");
    if (!bootfs.lookup("compiler_env.jll")) throw new Error("bootfs or compiler_env.jll not found!!");
  
    Debug.message("init Domain Manager");
    
    dm.createDomain("Compiler",
		    null, // CPU Objects
		    null, // HighLevel-Scheduler Objects
		    "compiler_env.jll",
		    null,
		    "jx/compiler/OnlineCompilerDomain",
		    40 * 1024 * 1024,/* gcinfo 0*/
		    -1,/* gcinfo 1*/
		    -1,/* gcinfo 2*/
		    (String)null,
		    -1,/* gcinfo 2*/
		    0,
		    null,
		    naming,
		    null,
		    0,
		    (int[])null);
    
    /* let the Compiler Domain get ready */
    CPUManager c = (CPUManager)naming.lookup("CPUManager");
    Debug.message("waiting for bytecode translater");
    while (onc==null) {	  
      c.yield();
      onc = (ByteCodeTranslater)naming.lookup("ByteCodeTranslater");
    }      
    
    return true;
  }

  public static Naming setupDomain() {
    Naming naming = InitialNaming.getInitialNaming();
    if (bootfs==null) {
      bootfs = (BootFS)naming.lookup("BootFS");
    }
    return naming;
  }
}
