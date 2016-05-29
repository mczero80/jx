package jx.compiler;

import jx.zero.*;
import java.util.*;
import jx.classfile.FieldData;

class CompactFieldDescription {
    private String fieldName;
    private String fieldType;
    private int offset; // in bytes
    private int size;
    CompactFieldDescription(String fieldName, String fieldType, int offset, int size) {
	this.fieldName=fieldName;
	this.fieldType=fieldType;
	this.offset=offset;
	this.size = size;
    }
    public String getFieldName() { return fieldName; }
    public String getFieldType() { return fieldType; }
    /**
     * returns offset in bytes
     */
    public int getOffset() { return offset; } 
    public int getSize() { return size; } 
}

public final class CompactFieldLayout {
    int numBytes;
    Vector fields = new Vector();
    Hashtable fieldFinder = new Hashtable();

    public CompactFieldLayout() {
	numBytes = 0;
    }

    public void addFields(CompactFieldLayout l) {
	for(int i=0; i<l.fields.size(); i++) {
	    CompactFieldDescription field = (CompactFieldDescription)l.fields.elementAt(i);
	    addField(new CompactFieldDescription(field.getFieldName(), field.getFieldType(), numBytes, field.getSize()));
	}
    }

    public void addField(FieldData [] fields, String fieldName, String fieldType) {
	int size=0;
	if (fieldType.equals("D") || fieldType.equals("J")) {
	    size = 8;
	} else if (fieldType.equals("I")) {
	    size = 4;
	} else if (fieldType.equals("S")) {
	    size = 2;
	} else if (fieldType.equals("B")) {
	    size = 1;
	} else if (fieldType.equals("C")) {
	    size = 2;
	} else if (fieldType.equals("[B")) {
	    /*
	    for(int i=0; i<fields.length; i++) {
		if (fields[i].isStatic()) {
		    if (fields[i].getName().startsWith("SIZE_")) {
			String memberName = fields[i].getName().substring(5);
			if (memberName.equals(fieldName)) {
			    size = 16;
			    break;
			}
		    } else {
			throw new Error("mapped objects must only contain static fields starting with SIZE_");
		    }
		}
	    }
	    */
	    throw new Error("arrays not supported in mapped objects");
	}
	if (size==0) throw new Error("Unable to compute field size");
	CompactFieldDescription field = new CompactFieldDescription(fieldName, fieldType, numBytes, size);
	addField(field);
    }

    // returns field offset in bytes
    public int getFieldOffset(String fieldName) {
	CompactFieldDescription field = (CompactFieldDescription) fieldFinder.get(fieldName);
	if (field==null) {
	    Debug.out.println("CompactFieldLayout::getFieldOffset field==null: " + fieldName);
	    return -1;
	}

	return field.getOffset();
    }

    public int wordsNeeded() {
	return (numBytes+3)>>2;
    }
    public int bytesNeeded() {
	return numBytes;
    }

    private void addField(CompactFieldDescription field) {
	numBytes += field.getSize();
	fieldFinder.put(field.getFieldName(), field);
    }

}
