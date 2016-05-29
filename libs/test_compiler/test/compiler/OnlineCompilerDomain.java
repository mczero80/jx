package test.compiler;

import jx.zero.*;
import jx.zero.debug.*;

import jx.zip.*;

public class OnlineCompilerDomain {

    public static boolean test(Naming naming) {	

	//DomainManager.createDomain("Compiler","test_compiler.jll","test/compiler/OnlineCompilerDomain",50*1024*1024);
	Debug.throwError();

	return true;
    }

    public static void init(Naming naming) {

	//this.naming = naming;

	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));

	Debug.out.println("Test Compiler Domain started.");

	start("test/fs/FileTreeWalk","test_app","File_Tree_Walk");
    }


    public static boolean startDomain(String mainclass,String filename,int memsize) {
	return startDomain(mainclass,filename,"**"+mainclass+"**",memsize);
    }

    public static boolean startDomain(String mainclass,String filename,String title,int memsize) {
	/*
	int i=0;
	
	if ((i=filename.indexOf(".zip"))>0) {
	    String source = filename.substring(i);

	    OnlineCompiler onc = new OnlineCompiler(InitialNaming.getInitialNaming());
	    onc.compile(source+".jll",source+".jln",filename,"int");

	    filename = source+".jll";
	}
	    
	Debug.out.println("create domain "+title+" lib: "+filename+" main: "+mainclass);

	

	InitialNaming.getInitialNaming().lookup("DomainManager").createDomain(title,
						      null, // CPU Objects
						      null, // HighLevel-Scheduler Objects
						      null, null,
						      filename,
						      null,
						      mainclass,
						      memsize);	
	return true;
	*/
	return false;
    }

    /**
       compiles and starts zip-files

       start(String mainclass,String source,String title)

     */

    public static boolean start(String mainclass,String source,String title) {
	/*	
      OnlineCompiler onc = new OnlineCompiler(InitialNaming.getInitialNaming());

      onc.compile(source+".jll",source+".jln",source+".zip","int");

      Debug.out.println("create domain "+title+" lib: "+source+".jll main: "+mainclass);

      domainManager.createDomain(title,
						    null, // CPU Objects
						    null, // HighLevel-Scheduler Objects
						    null, null,
						    source+".jll",
						    null,
						    mainclass,
						    3 * 1024 * 1024);

      return true;
	*/
	return false;
    }
}
