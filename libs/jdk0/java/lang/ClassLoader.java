package java.lang;

public abstract class ClassLoader {
	protected ClassLoader() throws SecurityException {
	    throw new Error("NOT IMPLEMENTED");
	}

	public Class loadClass(String name) throws ClassNotFoundException {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected abstract Class loadClass(String name, boolean resolve) throws ClassNotFoundException;

    /*
        public URL getResource(String name) {
	    throw new Error("NOT IMPLEMENTED");
	    }

	public InputStream getResourceAsStream(String name) {
	    throw new Error("NOT IMPLEMENTED");
	}
    */

	protected final Class defineClass(byte[] data, int offset, int len) throws ClassFormatError {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected final Class defineClass(String name, byte[] data, int offset, int len) throws ClassFormatError {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected final void resolveClass(Class c) {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected final Class findSystemClass(String name) throws ClassNotFoundException {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected final void setSigners(Class c, Object[] signers) {
	    throw new Error("NOT IMPLEMENTED");
	}

	protected final Class findLoadedClass(String name) {
	    throw new Error("NOT IMPLEMENTED");
	}

    /*	public static final URL getSystemResource(String name) {
	    throw new Error("NOT IMPLEMENTED");
	    }

	public static final InputStream getSystemResourceAsStream(String name) {
	    throw new Error("NOT IMPLEMENTED");
	}
    */
}
