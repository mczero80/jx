package jx.classfile.datatypes; 

public class DataType {
    String typeDesc;
    public DataType(String t) { typeDesc = t; }
    public boolean isClass() {
	return typeDesc.charAt(0)=='L';
    }
    public boolean isArray() {
	return typeDesc.charAt(0)=='[';
    }
    public String getClassName() throws NoClassTypeException {
	if (isClass()) return  typeDesc.substring(1, typeDesc.length()-1 ); 
	throw new NoClassTypeException();
    }
    public DataType getElementType() throws NoClassTypeException {
	if (!isArray())	throw new NoClassTypeException();
	int i=0;
	while(typeDesc.charAt(i) == '[') i++;
	return  new DataType(typeDesc.substring(i, typeDesc.length())); 
    }
    /** get any reference type (class, element class of array) */
    public String getReferenceType() {
	try {
	if (! isClass() && ! isArray()) return null;;
	String className;
	if (isClass()) {
	    className = getClassName();
	} else {
	    DataType ty1 = getElementType();
	    if (! ty1.isClass()) return null;
	    className = ty1.getClassName();
	}
	return className;
	} catch(NoClassTypeException e){return null;}
    }

}
