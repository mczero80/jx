package jx.verifier.typecheck;

import java.lang.Thread;
import jx.classfile.*;
import jx.classfile.constantpool.*;
import jx.classstore.ClassFinder;
import jx.verifier.bytecode.*;
import jx.verifier.*;

public class TypeCheck {
    static public void init(ClassFinder cFinder) {
	TCObjectTypes.setClassFinder(cFinder);
	TCInterfaceTypes.setClassFinder(cFinder);
    }

    
    static public void verifyMethod(MethodSource method, 
				    String className, 
				    ConstantPool cPool) throws VerifyException {
	
	MethodVerifier m = new MethodVerifier(method,
					      className,
					      cPool);
	m.setInitialState(new TCState(m.getCode().getFirst(), 
				      m.getMethod().getNumLocalVariables(),
				      m.getMethod().getNumStackSlots(),
				      m));

	m.runChecks();
    }

    static public void verify(ClassData classData,
			      ClassTree classTree) throws VerifyException {
	//FEHLER schöner machen: lieber gleich von hier aus verifyMethod aufrufen.
	MethodData[] methods = classData.getMethodData();
	ClassTreeElement cte = classTree.findClassTreeElement(classData.getClassName());
	for (int i =0; i< methods.length; i++) {
	    if (methods[i].isFinal() && !cte.isSystemFinalMethod(methods[i].getMethodName(),
							   methods[i].getMethodType()))
		throw new VerifyException("Verify Error: final method " +
					  cte.getClassName() + "." +
					  methods[i].getMethodName() +
					  " (" + methods[i].getMethodType() +") overridden!");
	}
    }
}
