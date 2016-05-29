package jx.verifier;

import jx.classstore.ClassFinder;
import jx.classfile.*;
import jx.verifier.typecheck.*;
import jx.verifier.npa.NullPointerAnalysis;
import jx.verifier.fla.FinalAndLeafAnalysis;
import jx.verifier.wcet.WCETAnalysis;
import jx.verifier.wcet.*;
import jx.verifier.bytecode.*;

import java.util.Enumeration;
import java.util.Vector;


/**Main Class for Verifier.
 * Immediately starts verifying when a new instance is created. 
 *@see Verifier#Verifier*/
public class Verifier {
    /** Create a new Verifier.<br>
     * The Analyses performed depend on the settings in <code>VerifierOptions</code>. <br>
     * It goes through all classes and calls Class analyzers (<code>TypeCheck.verify</code>).<br> 
     * For all methods of each class it calls Method analyzers (<code>TypeCheck.verifyMethod</code>, <code>NullPointerAnalysis.verifyMethod</code>, ...).
     * @see VerifierOptions
     * @param classFinder a <code>classFinder</code> that provides all available classes, including those to be verified (in <code>vClasses</code>)!
     * @param vClasses all Classes that should be verified. 
     * @exception VerifyException if the class fails verification.
     * @exception java.lang.Error if there is an Error verifying the class
     * @see jx.classstore.ClassFinder

     */
    public Verifier(ClassFinder classFinder, Enumeration vClasses) throws VerifyException {
	this(classFinder, vClasses, new VerifierOptions());
    }

    public Verifier(ClassFinder classFinder, Enumeration vClasses, VerifierOptions options) throws VerifyException {
	//classFinder.dump();

	debugMode = options.debugMode;
	stdPrintln(1, "doing \n" + 
		   ((options.doTypecheck)? "  typecheck\n":"") +
		   ((options.doNPA)? "  Null Pointer Analysis\n":"") +
		   ((options.doFLA)? "  System Final and Leaf Analysis\n":"") +
 		   ((options.doWCET)? "  Worst Case Execution Time Analysis\n":"") +
		   "Debuglevel: "+options.debugMode);
	
	//build class Tree
	stdPrint(1, "Building Class Tree.....");
	ClassTree classTree = new ClassTree(classFinder, vClasses);
	stdPrintln(1, "done");
	stdPrintln(2, "ClassTree:\n" + classTree.toString());
	
	//Initialize Type system
	TypeCheck.init(classFinder);
	
	
	//start verifying
	vClasses = classTree.getDomClasses();
	int numClasses = 0, numMethods = 0;

	//check all classes
    CLASSWHILE:
	while(vClasses.hasMoreElements()) {
	    ClassData actClass = (ClassData) vClasses.nextElement();
	    if (actClass.isInterface()) {
		continue CLASSWHILE;
	    }
	    stdPrintln(0,"Verifying class " + actClass.getClassName());
	    numClasses++;
	    
	    if (options.doTypecheck) {
		TypeCheck.verify(actClass, classTree);
	    }
	    MethodData[] methods = actClass.getMethodData();
	    for (int i=0; i < methods.length; i++) {
		if (methods[i] == null) {
		    stdPrintln(1,"methods["+i+"] is null!");
		} else {
		    if (methods[i].getCode() == null) {
			stdPrintln(1,"methods[" + 
				   i +
				   "].getCode() is null! (Name: " + 
				   methods[i].getMethodName() +
				   ")");
		    } else {
			numMethods++;
			try {
			    if (options.doTypecheck) {
				TypeCheck.verifyMethod(
						       methods[i], 
						       actClass.getClassName(), 
						       actClass.getConstantPool());
			    }
			    if (options.doNPA) {
				NullPointerAnalysis.verifyMethod(
								 methods[i], 
								 actClass.getClassName(), 
								 actClass.getConstantPool());
			    }
			    if (options.doFLA) {
				FinalAndLeafAnalysis.verifyMethod(
								  methods[i],
								  actClass.getClassName(),
								  actClass.getConstantPool(),
								  classTree);
			    }
			    if (options.doWCET) {
				if (options.wcetMethodArg == null) {
				    WCETAnalysis.verifyMethod(
							      methods[i],
							      actClass.getClassName(),
							      actClass.getConstantPool(),
							      classFinder,
							      options.WCETmaxTime);
				} else {
				    String cName = options.wcetMethodArg.substring(0,options.wcetMethodArg.indexOf('.'));
				    String mName = options.wcetMethodArg.substring(options.wcetMethodArg.indexOf('.') + 1);

				    if (cName.equals(actClass.getClassName()) &&
					mName.equals(methods[i].getMethodName())) {

					WCETAnalysis.verifyMethod(
								  methods[i],
								  actClass.getClassName(),
								  actClass.getConstantPool(),
								  classFinder,
								  options.WCETmaxTime);
				    }					
				}
			    }
			    
			} catch (VerifyException e) {
			    //System.err.println(e);
			    e.printStackTrace();
			    System.exit(1);
			}
			
		    }
		}
	    }
	}
	
	stdPrintln(1, "Analyzed " + numClasses + " classes with " + numMethods + " methods");
    }

    static public final int DEBUG_QUIET = -1;
    static public final int DEBUG_NORMAL = 0;
    static public final int DEBUG_VERBOSE = 1;
    static public final int DEBUG_DEBUG = 2;
    static public int debugMode = DEBUG_NORMAL;
    
    /** println <code>out</code> to <code>System.err</code> only if <code>options.debugMode >= mode</code>.
     *@param mode Debugmode.
     *@param out Text to be printed.
     */
    static public void errPrintln(int mode, String out) {
	if (debugMode >= mode)
	    System.err.println(out);
    }
    /** println <code>out</code> to <code>System.out</code> only if <code>options.debugMode >= mode</code>.
     *@param mode Debugmode.
     *@param out Text to be printed.
     */
    static public void stdPrintln(int mode, String out) {
	if (debugMode >= mode)
	    System.out.println(out);
    }
    /** print <code>out</code> to <code>System.err</code> only if <code>options.debugMode >= mode</code>.
     *@param mode Debugmode.
     *@param out Text to be printed.
     */
    static public void errPrint(int mode, String out) {
	if (debugMode >= mode)
	    System.err.print(out);
    }
    /** print <code>out</code> to <code>System.out</code> only if <code>options.debugMode >= mode</code>.
     *@param mode Debugmode.
     *@param out Text to be printed.
     */
    static public void stdPrint(int mode, String out) {
	if (debugMode >= mode)
	    System.out.print(out);
    }
}
