package java.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

//import java.util.StringTokenizer;

/*import jx.zero.Debug;*/

public class Runtime
{
    private static Runtime runtime;
    // private static Runtime runtime = new Runtime();

    public static Runtime getRuntime()
    {
	return runtime;
    }

    public void exit(int status)
    {
      /*
	SecurityManager s = System.getSecurityManager();
	if (s != null)
	    s.checkExit(status);
	    */
	exitInternal(status);
    }

  private  void exitInternal(int status) { 
      /*Debug.out.println("EXITINTERNAL!!");*/
    for(;;);
  }

    public Process exec(String[] cmdarray, String[] envp) throws IOException {
	// not supported
	/*Debug.out.println("exec not supported until now");*/
	exit(1);
	return null;
    }

/*
    public Process exec(String[] cmdarray, String[] envp) throws IOException
    {
	String command = cmdarray[0];

	SecurityManager s = System.getSecurityManager();
	if (s != null)
	    s.checkExec(command);

	if (envp == null)
	    envp = new String[0];

	Process p = NativeLang.exec(cmdarray, envp);
	if (p == null)
	    throw new IOException("Exec error");

	return p;
    }
*/

    public Process exec(String[] cmdarray) throws IOException
    {
	return exec(cmdarray, null);
    }

    public Process exec(String command, String[] envp) throws IOException
    {
      /*
	StringTokenizer tokenizer = new StringTokenizer(command);
	String[] cmdarray = new String[tokenizer.countTokens()];
	for (int n = 0; tokenizer.hasMoreTokens(); n++)
	    cmdarray[n] = tokenizer.nextToken();
	return exec(cmdarray, envp);
	*/
      return null;
    }

    public Process exec(String command) throws IOException
    {
	return exec(command, null);
    }

    public void load(String filename)
    {
	/*
	SecurityManager s = System.getSecurityManager();
	if (s != null)
	    s.checkLink(filename);

	if (!NativeLang.load(filename))
	    throw new UnsatisfiedLinkError(filename);
	*/
	/*Debug.out.println("load not supported until now");*/
	exit(1);
	
    }
  
    public void loadLibrary(String libname)
    {
	/*
	SecurityManager s = System.getSecurityManager();
	if (s != null)
	    s.checkLink(libname);

	if (!NativeLang.loadLibrary(libname))
	    throw new UnsatisfiedLinkError(libname);
*/
	/*Debug.out.println("Runtime::loadLibrary("+libname+")");*/
    }

    public long freeMemory()
    {
	return 0; // to be implemented
    }

    public long totalMemory()
    {
	return 0; // to be implemented
    }

    public void gc()
    {
	// to be implemented
    }

    public void runFinalization()
    {
	// to be implemented
    }

    public void traceInstructions(boolean on)
    {
	// to be implemented
    }

    public void traceMethodCalls(boolean on)
    {
	// to be implemented
    }

    public InputStream getLocalizedInputStream(InputStream in)
    {
	return in;// to be implemented
    }

    public OutputStream getLocalizedOutputStream(OutputStream out)
    {
	return out;// to be implemented
    }

    private Runtime()
    {
    }
}

