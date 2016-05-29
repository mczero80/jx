package jx.verifier;

import jx.classfile.*;
import java.util.Vector;
import java.util.Enumeration;

/** Elements of a ClassTree.
 * Every element has exactly one superClass (with exception of the top element, which is the ClassTree-Instance itself and has no parents), a <code>Vector</code> of subClasses and a reference to the classData of its class.
 * @see ClassTree
 * @see java.util.Vector
 */
public class ClassTreeElement {
    protected ClassData classData;
    protected ClassTreeElement superClass;
    protected Vector subClasses;
    /**distance from java/lang/Object*/
    protected int height;  

    /** Create new Element for class <code>classData</code>.
     * subClasses becomes a Vector of size 0, and superClass is null.
     * @param classData the ClassData for the class; must be nonnull, else an error is thrown.
     */
    public ClassTreeElement(ClassData classData) {
	this();
	if (classData == null) {
	    throw new Error("Internal Error: Cannot instantiate ClassTreeElement with classData 'null'");
	}
	this.classData = classData;
    }

    /** Create new Element for class <code>classData</code>.
     * Registers superClass as superclass for this, and this as subclass for superClass.
     * @param superClass superclass of the new element. The new element will be added to superClass.subClasses. Must be nonnull, else an error is thrown
     * @param classData the classdata for the new element. must be nonnull, else an error is thrown.
     */
    public ClassTreeElement(ClassTreeElement superClass, ClassData classData) {
	this(classData);
	if (superClass == null) {
	    throw new Error("Internal Error: superClass is null int constructor for ClassTreeEntry " + getClassName());
	}
	this.superClass = superClass;
	superClass.addSubClass(this);
	height=superClass.getHeight()+1;
    }

    /** Special Constructor for class <code>ClassTree</code>.
     * there is no Parameter, because there is no ClassData for "java/lang/Object"
     */
    protected ClassTreeElement() {	
	subClasses = new Vector(0);
    }

    /** get the Classname of this element.
	@return classname.
    */
    public String getClassName() {
	return classData.getClassName();
    }

    /** get all subclasses.
     * @return Enumeration of all subclasses.
     */
    public Enumeration getSubClassEnum() {
	return subClasses.elements();
    }

    /** check if <code>subClass</code> is a direct subclass of this element.
     * @see ClassTreeElement#isSubClass(String)
     */
    public boolean isSubClass(ClassData subClass) {
	return isSubClass(subClass.getClassName());
    }

    /** check if class with name <code>subClassName</code> is a direct subclass of this 
     * @param subClassName the name of the class which should be a direct subclass of this element.
     * @return true if subClassName is the name of one of this element's subclasses, else false. 
     */
    public boolean isSubClass(String subClassName) {
	for (Enumeration e = getSubClassEnum(); e.hasMoreElements() ;) {
             if (((ClassTreeElement)e.nextElement()).getClassData().
		 getClassName().equals(subClassName))
		 return true;

         }
	return false;
    }

    /** adds <code>subClass</code> to the list of subclasses of this element.
     * @param subClass the ClassTreeElement which should be added.
     */
    public void addSubClass(ClassTreeElement subClass) {
	if (isSubClass(subClass.getClassData())) return;
	subClasses.addElement(subClass);
    }

    /** set the superclass of this element.
     * @param superClass the ClassTreeElement which represents the superclass of this element.
     */
    public void setSuperClass(ClassTreeElement superClass) {
	this.superClass = superClass;
	height = superClass.getHeight() +1;
    }

    /** get the superclass of this element.
     * @return the ClassTreeElement which represents the superclass of this element.
     */
    public ClassTreeElement getSuperclass() {
	return superClass;
    }
    
    /** get the ClassData of this element.
     * @return the ClassData of this element.
     */
    public ClassData getClassData() {
	return classData;
    }

    /**search class with name className in subtree beginning at this element.
     * @param className Name of the requested class.
     * @return the ClassTreeElement for class with name <code>className</code>, or <code>null</code> if no such class was found.
     */
    public ClassTreeElement findClassTreeElement(String className) {
	if (getClassName().equals(className)) {return this;}
	ClassTreeElement ret = null;
	for (Enumeration e = getSubClassEnum(); e.hasMoreElements() ;) {
	    ret = ((ClassTreeElement)e.nextElement()).findClassTreeElement(className);
	    if (ret != null) return ret;
         }
	return ret;
	
    }
    
    public String toString() {
	return toString(0);
    }

    public String toString(int indent) {
	StringBuffer ret = new StringBuffer(512);
	StringBuffer space = new StringBuffer(indent);
	for (int i =0; i<indent-1;i++) {
	    space.append("|");
	}
	if (indent > 0) 
	    space.append("+");

	ret.append(space + getClassName() + "\n");
	for (Enumeration e = getSubClassEnum(); e.hasMoreElements() ;) {
	    ret.append(((ClassTreeElement)e.nextElement()).toString(indent+1));
	}
	return ret.toString();

    }

    /** test if this element is a leaf or not.
     *@return true if this element has subclasses (i.e. it is not a leaf of the tree), else false.
     */
    public boolean hasSubClass() {
	return (subClasses.size()>0);
    }

    /** returns the height of this element, i.e. the distance from the root-element.
     * the root has height 0.
     * @return the height of this element.
     */
    public int getHeight() {
	return height;
    }
    
    /**check if this very ClassTreeElement implements the specified method.
     * @param methodName name of the method.
     * @param typeDesc typedescriptor for the method.
     * @return true if this very ClassTreeElement implements the specified method, else false - even if a superclass implements the method.
     */
    public boolean implementsMethod(String methodName, String typeDesc) {
	if (classData.getMethodDataNE(methodName, typeDesc) != null)
	    return true;
	else
	    return false;
    }

    /**check if this class or any superClasses implement the specified method.
     * @param methodName name of the method.
     * @param typeDesc typedescriptor for the method.
     * @return true if this class or any superClasses implement the specified method.
     */
    public boolean hasMethod(String methodName, String typeDesc) {
	if (implementsMethod(methodName, typeDesc))
	    return true;
	if (superClass != null)
	    return superClass.hasMethod(methodName, typeDesc);
	else
	    return false;
		
    }

    /**search the methodData for a  method 
     * @param methodName name of the method.
     * @param typeDesc typedescriptor for the method.
     *return the methodData Object for specified method or null if method not found or one of java/lang/Object's methods.
     */
    public MethodData getMethod(String methodName, String typeDesc) {
	MethodData mData = classData.getMethodDataNE(methodName, typeDesc);
	if (mData != null)
	    return mData; //method found!
	if (superClass != null) {
	    return superClass.getMethod(methodName, typeDesc);
	} else
	    return null;
	
    }
    
    /** check if the specified Method is implemented by any subclasses.
     *Note: has nothing to do with final ATTRIBUTE - a method can be "SystemFinal" without beeing declared final!
     * @param methodName name of the method.
     * @param typeDesc typedescriptor for the method.
     *@return true, if the specified Method is NOT implemented by any subclasses, else false.
     */
    public boolean isSystemFinalMethod(String methodName, String typeDesc) {
	ClassTreeElement actSubClass;
	for (Enumeration e = getSubClassEnum(); e.hasMoreElements() ;) {
	    actSubClass = (ClassTreeElement)e.nextElement();
	    if (actSubClass.implementsMethod(methodName, typeDesc))
		 return false;
	    if (!actSubClass.isSystemFinalMethod(methodName, typeDesc))
		return false;
	    
	}
	return true;
    }
    /** check if the specified Method is implemented by any subclasses.
     * @see ClassTreeElement#isSystemFinalMethod(java.lang.String, java.lang.String)
     */
    public boolean isSystemFinalMethod(MethodSource method) {
	return isSystemFinalMethod(method.getMethodName(), method.getMethodType());

    }
}
