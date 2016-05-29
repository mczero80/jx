package jx.emulation;

import jx.zero.*;

import java.util.Vector;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class Mapping {
    Class objectType;
    int start;
    Field [] fields;
    int [] offsets;
    int [] sizes;
    Object object;
    Mapping(Class objectType, int start) {
	this.objectType = objectType;
	this.start = start;
    }
}

public class MemoryImpl implements Memory,DeviceMemory {
    int start,size;
    byte core[];
    Vector mappings=new Vector();

    MemoryImpl() {
	//Debug.out.println("Memory private! ");
    }
    
    // package access; called by MemoryManager
    MemoryImpl(int size){
	//Debug.out.println("Memory: "+size);
	core = new byte[size];
	if (core == null) {
	    Debug.throwError("could not allocate memory");
	}
	start = 0;
	this.size = size;

    }

    public void copy(int from, int to, int length) {
	if ((from+length > size) || (to+length > size)) { 
	    Debug.message("copy: memory("+start+","+size+") accessed out of range from="+from+", to="+to+", length="+length);
	    return; 
	}
	coreCopy(start+from, start+to, length);
    }

    public void move(int dst, int src, int count) {
	throw new Error("not implemented!");
    }

    public void clear() {throw new Error("not emulated");}

    public void fill16(short what, int offset, int length) {
	if ((offset+length)*2 > size) { Debug.message("copy: memory accessed out of range"); return; }
	coreFill16(what, start+(offset<<1), length);
    }
    public void fill8(byte what, int offset, int length) {
	if ((offset+length)*2 > size) { Debug.message("copy: memory accessed out of range"); return; }
	
    }
    public void fill32(int what, int offset, int length) { throw new Error("not implemented");}
    /**
     * @param where a 8-bit offset into this memory
     */
    public void set8(int where, byte what) {
	if (where < 0 || where >= size) { Debug.message("copy: memory accessed out of range"); return; }
	coreSet8(start+where, what);
    }
    // where is a 16-bit offset
    public void set16(int where, short what) {
	if (where < 0 || (where<<1) >= size) { Debug.message("copy: memory accessed out of range"); return; }
	coreSet16(start+(where<<1), what);
    }
    // where is a 32-bit offset
    public void set32(int where, int what) {
	if (where < 0 || (where<<2) >= size) { Debug.message("copy: memory accessed out of range"); return; }
	coreSet32(start+(where<<2), what);
    }
    // where is a 8-bit offset
    public byte get8(int where) {
	if (where < 0 || where >= size) { 
	  Debug.message("get8: memory accessed out of range " + where + "/" + size);
          return -1;
        }
	return coreGet8(start+where); 
    }
    // where is a 16-bit offset
    public short get16(int where) {
	if (where < 0 || (where<<1) >= size) { Debug.message("copy: memory accessed out of range"); return -1; }
	return coreGet16(start+(where<<1)); 
    }
    // where is a 32-bit offset
    public int get32(int where) {
	if (where < 0 || (where<<2) >= size) {Debug.message("get32: " + where + "/" + size); return -1; }/* error message ? */
	return coreGet32(start+(where<<2));
    }

    public void copyFromByteArray(byte[] array, int array_offset, int mem_offset, int len) {
	if (array_offset >= array.length)
	    return;
	if (array_offset + len > array.length)
	    len = array.length - array_offset;
	if (mem_offset >= size)
	    return;
	if (mem_offset + len > size)
	    len = size - mem_offset;
	for (int i = 0; i < len; i++)
	    set8(mem_offset+i, array[array_offset+i]);
    }

    public void copyToByteArray(byte[] array, int array_offset, int mem_offset, int len) {
	//Debug.out.println("copyToByteArray: mem_offset="+mem_offset);
	if (array_offset >= array.length)
	    return;
	if (array_offset + len > array.length)
	    len = array.length - array_offset;
	if (mem_offset >= size)
	    return;
	if (mem_offset + len > size)
	    len = size - mem_offset;
	for (int i = 0; i < len; i++)
	    array[array_offset+i] = get8(mem_offset+i);
    }

    public int copyToMemory(Memory dst, int srcOffset, int dstOffset, int len) {
	if (srcOffset < 0
	    || len < 0
	    || srcOffset + len > size
	    || dstOffset < 0 
	    || dstOffset + len > ((MemoryImpl)dst).size) { Debug.message("copyInto: memory accessed out of range"); return-1; }
	for(int i=0; i<len; i++) dst.set8(dstOffset+i, get8(srcOffset+i));
	return 0;
    }

    public int copyFromMemory(Memory src, int srcOffset, int dstOffset, int len) {
	for(int i=0; i<len; i++) this.set8(dstOffset+i, src.get8(srcOffset+i));
	return 0;
    }

    // return size in bytes
    public int size() {
	return size;
    }

    public int getStartAddress() {
	return start;
    }

    protected void littleSet32(int where, int what) {
	core[where   +  3 ] = (byte)((what >>> 24) & 0xff); 
	core[(where) + 2] = (byte)((what >>> 16) & 0xff); 
	core[(where) + 1] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 0] = (byte)(what & 0xff); 
    }

    protected int littleGet32(int where) { 
	return ((core[where + 3] << 24) & 0xff000000) 
	    |  ((core[where + 2] << 16) & 0x00ff0000) 
	    |  ((core[where + 1] <<  8) & 0x0000ff00) 
	    |   (core[where + 0] & 0xff); 
    }

    public int getLittleEndian32(int where) { 
	where = where + start;
	return ((core[where + 3] << 24) & 0xff000000) 
	    |  ((core[where + 2] << 16) & 0x00ff0000) 
	    |  ((core[where + 1] <<  8) & 0x0000ff00) 
	    |   (core[where + 0] & 0xff); 
    }
	
    public void setLittleEndian32(int where, int what)  {
	where = where + start;
	core[where   +  3 ] = (byte)((what >>> 24) & 0xff); 
	core[(where) + 2] = (byte)((what >>> 16) & 0xff); 
	core[(where) + 1] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 0] = (byte)(what & 0xff); 
    }

    protected void littleSet16(int where, short what) {
	core[(where) + 1] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 0] = (byte)(what & 0xff); 
    }
    protected short littleGet16(int where) { 
	return (short)( ((core[where + 1] <<  8) & 0xff00) 
	    |   (core[where + 0] & 0xff)); 
    }

    public short getLittleEndian16(int where) { 
	where = where + start;
	return (short)( ((core[where + 1] <<  8) & 0xff00) 
			|   (core[where + 0] & 0xff)); 
    }
    
    public void setLittleEndian16(int where, short what)  {
	where = where + start;
	core[(where) + 1] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 0] = (byte)(what & 0xff); 
    }

    /**** big endian ***/

    public int getBigEndian32(int where) { 
	where = where + start;
	return ((core[where + 0] << 24) & 0xff000000) 
	    |  ((core[where + 1] << 16) & 0x00ff0000) 
	    |  ((core[where + 2] <<  8) & 0x0000ff00) 
	    |   (core[where + 3] & 0xff); 
    }
	
    public void setBigEndian32(int where, int what)  {
	where = where + start;
	core[where   +  0 ] = (byte)((what >>> 24) & 0xff); 
	core[(where) + 1] = (byte)((what >>> 16) & 0xff); 
	core[(where) + 2] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 3] = (byte)(what & 0xff); 
    }

    public short getBigEndian16(int where) { 
	where = where + start;
	return (short)( ((core[where + 0] <<  8) & 0xff00) 
			|   (core[where + 1] & 0xff)); 
    }
    
    public void setBigEndian16(int where, short what)  {
	where = where + start;
	core[(where) + 0] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 1] = (byte)(what & 0xff); 
    }

    /**
     * o must be != null when createArrays is true
     */
    private Mapping createMapping(Object o, Class objectType, int start, boolean createArrays) {
	try {
	    // objectType must implemend MappedLittleEndianObject or MappedBigEndianObject
	    if (! (MappedLittleEndianObject.class).isAssignableFrom(objectType)) throw new Exception();
	    Mapping mapping = new Mapping(objectType, start);
	    Field[] fields = objectType.getFields();
	    int numFields = 0;
	    for(int i=0; i<fields.length; i++) {
		int mod = fields[i].getModifiers();
		if (! Modifier.isStatic(mod)) {
		    numFields ++;
		}
	    }
	    mapping.fields = new Field[numFields];
	    mapping.offsets = new int[numFields];
	    mapping.sizes = new int[numFields];
	    int offset = 0;
	    int j=-1;
	    for(int i=0; i<fields.length; i++) {
		int mod = fields[i].getModifiers();
		if (! Modifier.isStatic(mod)) {
		    j++;
		    mapping.offsets[j] = offset;
		    mapping.fields[j] = fields[i];
		    Class fieldType = fields[i].getType();
		    if (! fieldType.isPrimitive()) {
			if (! fieldType.isArray()) {
			    throw new Exception();
			}
			// find size spec
			Field sizeField = objectType.getField("SIZE_"+fields[i].getName());
			int size = sizeField.getInt(objectType);
			// check array type
			String typeName = fieldType.getName();
			if (typeName.charAt(1) == 'B') {
			    if (createArrays) {
				byte[] array = new byte[size];
				mapping.fields[j].set(o, array);
			    }
			} else if (typeName.charAt(1) == 'I') {
			    if (createArrays) {
				int[] array = new int[size];
				mapping.fields[j].set(o, array);
			    }
			    size *= 4;
			} else {
			    throw new Error("Only byte and int arrays are supported for object mapping");
			}
			offset += size;
			mapping.sizes[j] = size;
		    } else {
			// primitive
			if (fieldType.equals(Boolean.TYPE)) {
			    offset += 1;
			    mapping.sizes[j] = 1;
			} else if (fieldType.equals(Integer.TYPE)) {
			    offset += 4;
			    mapping.sizes[j] = 4;
			} else if (fieldType.equals(Short.TYPE)) {
			    offset += 2;
			    mapping.sizes[j] = 2;
			} else if (fieldType.equals(Byte.TYPE)) {
			    offset += 1;
			    mapping.sizes[j] = 1;
			}
		    }
		}
	    }
	    return mapping;
	} catch(Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public Object map(VMClass vmclass) {
	throw new Error("map not emulated");
    }

    public Object map(String classname, int start) {
	try {
	    Class objectType = Class.forName(classname);
	    Object o = objectType.newInstance();
	    Mapping mapping = createMapping(o, objectType, start, true);
	    mapping.object = o;
	    mappings.addElement(mapping);
	    fillMapping(mapping);
	    return o;
	} catch(Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    private void fillMapping(Mapping mapping) {
	try {
	for(int i=0; i<mapping.fields.length; i++) {
	    Field field = mapping.fields[i];
	    Class fieldType = mapping.fields[i].getType();
	    if (! fieldType.isPrimitive()) {
		if (! fieldType.isArray()) {
		    throw new Exception();
		}
		// array type
		String typeName = fieldType.getName();
		if (typeName.charAt(1) == 'B') {
		    byte[] array = new byte[mapping.sizes[i]];
		    mapping.fields[i].set(mapping.object, array);
		    for(int j=0; j<array.length; j++) {
			array[j] = coreGet8(mapping.start + mapping.offsets[i] + j);
		    }		    
		} else if (typeName.charAt(1) == 'I') {
		    int[] array = new int[mapping.sizes[i]/4];
		    mapping.fields[i].set(mapping.object, array);
		    for(int j=0; j<array.length; j++) {
			array[j] = coreGet32(mapping.start + mapping.offsets[i] + j*4);
		    }		    		    
		} else {
		    throw new Error("Only byte and int arrays are supported for object mapping");
		}

	    } else {
		try {
		    if (fieldType.equals(Integer.TYPE)) {
			mapping.fields[i].setInt(mapping.object, littleGet32(mapping.start + mapping.offsets[i]));
		    } else if (fieldType.equals(Short.TYPE)) {
			mapping.fields[i].setShort(mapping.object, littleGet16(mapping.start + mapping.offsets[i]));
		    } else if (fieldType.equals(Byte.TYPE)) {
			mapping.fields[i].setByte(mapping.object, coreGet8(mapping.start + mapping.offsets[i]));
		    }
		} catch(Exception ex) {
		    ex.printStackTrace();
		}
	    }
	}
	} catch(Exception ex) {
	    ex.printStackTrace();
	    Debug.throwError("");
	}
    }
    public void fillMappings() {
	for(int i=0; i<mappings.size(); i++) {
	    Mapping mapping = (Mapping)mappings.elementAt(i);
	    fillMapping(mapping);
	}
    }

    private void syncMapping(Mapping mapping) {
	/*	for(int i=0; i<16; i++) {
	    Debug.out.println("MEM:" +coreGet8(i)); 
	    }*/
	try {
	for(int i=0; i<mapping.fields.length; i++) {
	    Field field = mapping.fields[i];
	    Class fieldType = mapping.fields[i].getType();
	    if (! fieldType.isPrimitive()) {
		if (! fieldType.isArray()) {
		    throw new Exception();
		}
		// array type
		String typeName = fieldType.getName();
		if (typeName.charAt(1) == 'B') {
		    byte[] array = (byte[])mapping.fields[i].get(mapping.object);
		    //Debug.out.println("ALEN: "+array.length);
		    for(int j=0; j<array.length/*mapping.sizes[i]*/; j++) {
			coreSet8(mapping.start + mapping.offsets[i] + j, array[j]);
		    }
		} else if (typeName.charAt(1) == 'I') {
		    int[] array = (int[])mapping.fields[i].get(mapping.object);
		    //Debug.out.println("ALEN: "+array.length);
		    for(int j=0; j<array.length/*mapping.sizes[i]*/; j++) {
			coreSet32(mapping.start + mapping.offsets[i] + j*4, array[j]);
		    }
		} else {
		    throw new Error("Only byte and int arrays are supported for object mapping");
		}
	    } else {
		try {
		    if (fieldType.equals(Integer.TYPE)) {
			//Debug.out.println("INT: "+(mapping.start + mapping.offsets[i])+"="+mapping.fields[i].getInt(mapping.object));
			littleSet32((mapping.start + mapping.offsets[i]), mapping.fields[i].getInt(mapping.object)); 
		    } else if (fieldType.equals(Short.TYPE)) {
			littleSet16(mapping.start + mapping.offsets[i], mapping.fields[i].getShort(mapping.object)); 
		    } else if (fieldType.equals(Byte.TYPE)) {
			coreSet8(mapping.start + mapping.offsets[i], mapping.fields[i].getByte(mapping.object)); 
		    }
		} catch(Exception ex) {
		    ex.printStackTrace();
		}
	    }
	}
	} catch(Exception ex) {
	    ex.printStackTrace();
	    Debug.throwError("");
	}
    }

    public void syncMappings() {
	for(int i=0; i<mappings.size(); i++) {
	    Mapping mapping = (Mapping)mappings.elementAt(i);
	    syncMapping(mapping);
	}
    }

    public void copyFromObject(MappedObject o, int start) {
	Mapping mapping = createMapping(null, o.getClass(), start, false);
	mapping.object = o;
	fillMapping(mapping);	
    }
    
    protected  void coreCopy(int from, int to, int length) {
	for(int i=0; i<length; i++) {
	    core[to+i] = core[from+i];
	}
    }
    protected void coreFill16(short what, int offset, int length) {
	for(int i=0; i<length; i++) {
	    coreSet16(offset+i, what);
	}
    }
    protected void coreSet8(int where, byte what) {
	core[where] = what;
    }
    protected void coreSet16(int where, short what) {
	core[where] = (byte)(what >>> 8); 
	core[(where) + 1] = (byte)(what & 0xff); 
    }
    protected void coreSet32(int where, int what) {
	core[where      ] = (byte)((what >>> 24) & 0xff); 
	core[(where) + 1] = (byte)((what >>> 16) & 0xff); 
	core[(where) + 2] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 3] = (byte)(what & 0xff); 
    }
    protected byte coreGet8(int where) { return core[where]; }
    protected short coreGet16(int where) { 
	return (short)(((core[where] << 8) & 0xff00) |  (core[(where) + 1] & 0xff)); 
    }
    protected int coreGet32(int where) { 
	return ((core[where] << 24) & 0xff000000) 
	    | ((core[(where) + 1] << 16) & 0x00ff0000) 
	    | ((core[(where) + 2] <<  8) & 0x0000ff00) 
	    |  (core[(where) + 3] & 0xff); 
    }

    public Memory getSubRange(int start, int size) {
	if (start+size > this.size) {
	    Debug.message("getSubRange: memory("+this.start+","+this.size+") accessed out of range start="+start+", size="+size);
	    return null; 
	}

	MemoryImpl m = new SubMemory();
	m.core = this.core;
	m.start = this.start + start;
	m.size = size;

	return m;
    }

    public ReadOnlyMemory getReadOnlySubRange(int start, int size) {
	if (start+size > this.size) {
	    Debug.message("getSubRange: memory("+this.start+","+this.size+") accessed out of range start="+start+", size="+size);
	    return null; 
	}
	MemoryImpl m = new SubMemory();
	m.core = this.core;
	m.start = this.start + start;
	m.size = size;
	return m;
    }

    public Memory extendRange(int atBeginning, int atEnd) {
	throw new Error("not implemented");
    }

    public Memory extendFullRange() {
	throw new Error("not implemented");
    }

    public void split2(int offset, Memory[] parts) {throw new Error();}

    public void split3(int offset, int size, Memory[] parts) {throw new Error();}

    public Memory joinPrevious() {throw new Error();}

    public Memory joinNext() {throw new Error();}

    public Memory joinAll() {throw new Error();}

    // represents a subrange of Memory
    class SubMemory extends MemoryImpl {

	protected  void coreCopy(int from, int to, int length) {
	    super.coreCopy(from+start, to+start, length);
	}
	protected void coreFill16(short what, int offset, int length) {
	    super.coreFill16(what, start+offset, length);
	}
	protected void coreSet8(int where, byte what) {
	    super.coreSet8(where, what);
	}
	protected void coreSet16(int where, short what) {
	    super.coreSet16(start+where, what);
	}
	protected void coreSet32(int where, int what) {
	    super.coreSet32(start+where, what);
	}
	protected byte coreGet8(int where) { return super.coreGet8(where); }
	protected short coreGet16(int where) { return super.coreGet16(start+where); }
	protected int coreGet32(int where) { return super.coreGet32(start+where);	}
    }

    public Memory revoke() { throw new Error(); }
    public Memory extendAndRevoke() { throw new Error(); }
    public int getOffset() { throw new Error(); }
    public boolean isValid() { throw new Error();}

    public boolean equals(Object o) {return this == o; }
    public Object clone() { throw new Error(); }
}



