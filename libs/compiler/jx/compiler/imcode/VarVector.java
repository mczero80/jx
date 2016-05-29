package jx.compiler.imcode;

import java.util.NoSuchElementException;
//import java.lang.IndexOutOfBoundsException;

public final class VarVector { 

    private  int capacityIncrement;
    private  int elementCount;
    private  LocalVariable[] elementData;

    public VarVector()
    {
	this(10);
    }

    public VarVector(int initialCapacity)
    {
	this(initialCapacity, 5);
    }
    
    public VarVector(int initialCapacity, int capacityIncrement)
    {
	this.capacityIncrement = capacityIncrement;
	elementData = new LocalVariable[initialCapacity];
	elementCount = 0;
    }
    
    public final int capacity()
    {
	return elementData.length;
    }

    public final void ensureCapacity(int minCapacity)
    {
	if (minCapacity <= elementData.length)
	    return;
       
	int newSize = 
	    capacityIncrement > 0
	    ? elementData.length + capacityIncrement
	    : elementData.length * 2;

	if (newSize < minCapacity) newSize = minCapacity;

	LocalVariable[] newData = new LocalVariable[newSize];
	copyInto(newData);
	elementData = newData;
    }

    public final void copyInto(LocalVariable[] array)
    {
	System.arraycopy(
			 (Object)elementData, 0,
			 (Object)array, 0,
			 elementCount
			 );
    }

    public final void trimToSize()
    {
	LocalVariable[] newData = new LocalVariable[elementCount];
	copyInto(newData);
	elementData = newData;
    }

    public final LocalVariable[] toArray() {
	LocalVariable[] newData = new LocalVariable[elementCount];
	copyInto(newData);
	return newData;
    }

    public final void add(LocalVariable obj)
    {
	ensureCapacity(elementCount + 1);
	elementData[elementCount] = obj;
	elementCount++;
    }

    public final void addElement(LocalVariable obj)
    {
	ensureCapacity(elementCount + 1);
	elementData[elementCount] = obj;
	elementCount++;
    }

    public final void insertElementAt(LocalVariable obj, int index)
    {
	if (index > elementCount)
	    throw new IndexOutOfBoundsException();
	ensureCapacity(elementCount + 1);
	for (int i = elementCount; i > index; i--)
	    elementData[i] = elementData[i - 1];
	elementData[index] = obj;
	elementCount++;
    }

    public final int size()
    {
	return elementCount;
    }

    public final boolean isEmpty()
    {
	return (elementCount == 0);
    }

    public final LocalVariable firstElement() throws NoSuchElementException
    {
	if (isEmpty()) throw new NoSuchElementException();
	return elementData[0];
    }

    public final LocalVariable lastElement() throws NoSuchElementException
    {
	if (isEmpty()) throw new NoSuchElementException();
	return elementData[elementCount - 1];
    }

    public final LocalVariable elementAt(int index)
    {
	if (index >= elementCount) throw new IndexOutOfBoundsException();
	return elementData[index];
    }

    public final int indexOf(LocalVariable obj, int index)
    {
	for (int i = index; i < elementCount; i++)
	    if (elementData[i] != null && elementData[i].equals(obj))
		return i;
	return -1;
    }

    public final int indexOf(LocalVariable obj)
    {
	return indexOf(obj, 0);
    }

    public final boolean contains(LocalVariable obj)
    {
	return (indexOf(obj) >= 0);
    }

    public final int lastIndexOf(LocalVariable obj, int index)
    {
	int k = -1;
	for (int i = 0; i <= index; i++)
	    if (elementData[i].equals(obj))
		k = i;
	return k;
    }

    public final int lastIndexOf(LocalVariable obj)
    {
	return lastIndexOf(obj, elementCount - 1);
    }

    public final void removeAllElements()
    {
	while (elementCount > 0) elementData[--elementCount] = null;
    }
    
    public final void removeElementAt(int index)
    {
	for (int i = index + 1; i < elementCount; i++)
	    elementData[i - 1] = elementData[i];
	elementCount--;
    }

    public final boolean removeElement(LocalVariable obj)
    {
	for (int i = 0; i < elementCount; i++)
	    if (elementData[i].equals(obj))
		{
		    removeElementAt(i);
		    return true;
		}
	return false;
    }

    public final void setElementAt(LocalVariable obj, int index)
    {
	if (index >= elementCount) throw new IndexOutOfBoundsException();
	elementData[index] = obj;
    }
    
    public final void setSize(int newSize)
    {
	while (elementCount > newSize) elementData[--elementCount] = null;
	ensureCapacity(newSize);
	while (elementCount < newSize) elementData[elementCount++] = null;
    }

    public final String toString()
    {
	StringBuffer buff = new StringBuffer("[");
	for (int i = 0; i < elementCount; i++)
	    {
		if (i > 0)
		    buff.append(", ");
		
		if (elementData[i] != null)
		    buff.append(elementData[i].toString());
		else
		    buff.append("null");
	}
	buff.append("]");
	
	return buff.toString();
    }
}

