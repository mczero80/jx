package jx.verifier;

import jx.classstore.ClassFinder;
import jx.classfile.*;
import java.util.Enumeration;
import java.util.Vector;

/**Class Tree holding all available Classes.
 * The <code>ClassTree</code> extends <code>ClassTreeElement</code> becaust it is the top element of the tree.<br>
 * As a <code>ClassTreeElement</code>, a <code>ClassTree</code> behaves like the entry for <cde>java/lang/Object</code>
 * @see ClassTreeElement
 */
public class ClassTree extends ClassTreeElement{
    ClassFinder classFinder;
    Vector domClasses;
    
    /** Create new <code>ClassTree</code>.
     * All classes in <code>vClasses</code> and their ancestors are put into the tree. Classes in the classfinder that are neither in vClasses nor ancestors of classes in vClasses are not in the tree.
     * @param cFinder ClassFinder containing all available classes.
     * @param vClasses All Classes that should be in the tree.
     */
    public ClassTree(ClassFinder cFinder, Enumeration vClasses) {
	super();
	this.classFinder = cFinder;
	domClasses = new Vector();
	//build Class tree
	//top Element: java/lang/Object
	
	
       while(vClasses.hasMoreElements()) {
	   ClassData actClass = (ClassData) vClasses.nextElement();
	   domClasses.addElement(actClass);
	   if (actClass.isInterface()) {
	       continue;
	   }
	   addClass(actClass);
       }
	
    }
    
    /**Creates new ClassTreeElement for ClassData and adds it to the tree.
     * if the superclass is not in the tree yet, it is added recursively.
     * @param classData the ClassData for the class that should be added.
     * @return the newly created ClassTreeElement.
     */
    public ClassTreeElement addClass(ClassData classData) {
	ClassTreeElement superElm = findClassTreeElement(classData.getSuperClassName());
	if (superElm == null) {
	    ClassData superClassData = classFinder.findClass(classData.getSuperClassName());
	    if (superClassData == null) {
		//superClass not existant! (cannot be java/lang/Object, because that would
		//hav been found byte findClassTreeElement!
		//FEHLER Eher VerifyException?
		throw new Error("SuperClass not found while building Class Tree: " + 
				 classData.getSuperClassName());
	    }
	    superElm = addClass(superClassData);
	}
	ClassTreeElement thisElm = new ClassTreeElement(superElm, classData);
	
	return thisElm;
    }


    /** get all Classes for which the tree was build (<code>vClasses</code> in the constructor)
     *@return Enumeration of all Classes for which the tree was build (see constructor, <code>vClasses</code>).
     *@see ClassTree#ClassTree
     *@see java.util.Enumeration
     */
    public Enumeration getDomClasses() {
	return domClasses.elements();
    }


    ///////////////////// from ClassTreeElement /////////////
    // top Element is java/lang/Object
    /** returns the Name of the top-Element
     *@return top element is always "java/lang/Object"
     */
    public String getClassName() {
	return "java/lang/Object";
    }

    /** Cannot be used for topmost ClassTreeElement, so it throws an Error*/
    public void setSuperClass(ClassTreeElement superClass) {
	throw new Error("Internal Error: trying to set Superclass of ClassTree top!");
    }

    /**holds name and signature of all of "java/lang/Object"'s Methods*/
    private static final String[][] oMethods = {
	{"getClass", "()Ljava/lang/Class;"},
	{"hashCode", "()I"},
	{"clone", "()Ljava/lang/Object;"},
	{"wait", "()V"},
	{"wait", "(J)V"},
	{"wait", "(JI)V"},
	{"notify", "()V"},
	{"notifyAll", "()V"},
	{"toString", "()Ljava/lang/String;"},
	{"equals", "(Ljava/lang/Object;)Z"},
	{"finalize", "()V"}
    };


    /** the same as in ClassTreeElement, methods are those of "java/lang/Object"
     * @return true if this very ClassTreeElement implements the specified method, else false - even if a superclass implements the method.
     * @see ClassTreeElement
     */
    public boolean implementsMethod(String methodName, String typeDesc) {
	for (int i = 0; i<oMethods.length;i++) {
	    if (oMethods[i][0].equals(methodName) &&
		oMethods[i][1].equals(typeDesc) )
		return true;
	}
	return false;
    }

    /** for ClassTree this method is equivalent to <code>implementsMethod</code>
     * @see ClassTree#implementsMethod
     * @see ClassTreeElement
     */
    public boolean hasMethod(String methodName, String typeDesc) {
	return implementsMethod(methodName, typeDesc);
    }
}
