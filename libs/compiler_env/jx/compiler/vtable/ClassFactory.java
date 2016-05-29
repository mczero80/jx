package jx.compiler.vtable;
/**
 * Must be implemneted by the user of the service
 * method table factory.
 */
import jx.classfile.ClassSource;

public interface ClassFactory {
    void reset();
    boolean hasMoreClasses();
    ClassSource nextClass();
}
