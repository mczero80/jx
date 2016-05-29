/*
 * representation of native compiled class
 *
 * Copyright 2000 Michael Golm
 */
package jx.compiler.persistent;

import java.util.*;
import jx.compiler.*;

public class CompiledClass {
    String className;
    String superName;
    int objectSize;
    int classSize;
    CompiledMethod[] methods;

    public CompiledClass(String className, String superName, int objectSize, int classSize, CompiledMethod[] methods) {
	this.className = className;
	this.superName = superName;
	this.objectSize = objectSize;
	this.classSize = classSize;
	this.methods = methods;
    }
    
    public String getName() { 
	return className;
    }

    public CompiledMethod[] getMethods() {
	return methods;
    }
}
