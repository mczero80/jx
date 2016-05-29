package jx.compiler; 

import jx.compiler.execenv.BCClass;

public interface ClassFinder {
    public BCClass findClass(String className);

    /**
     * @return true if instances of "className" can be assigned to
     * variables of type superName
     */
    public boolean isAssignableTo(String className, String superName);
    public boolean implementsInterface(BCClass ifClass, BCClass clClass);
}
