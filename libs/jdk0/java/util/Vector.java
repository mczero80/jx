package java.util;

import java.io.Serializable;

class ArrayEnumeration implements Enumeration, Serializable
{
    protected Object[] data;
    protected int index;

    // Methods

    public boolean hasMoreElements()
    {
	return index < data.length;
    }

    public Object nextElement() throws NoSuchElementException
    {
	if (index >= data.length)
	    throw new NoSuchElementException();
	return data[index++];
    }

    // Constructors

    ArrayEnumeration(Object[] data)
    {
	this.data = data;
	index = 0;
    }
}

public class Vector extends AbstractList implements List, Cloneable, Serializable
{ 
    // Fields


    // Constructors
    
    public Vector(int initialCapacity, int capacityIncrement)
    {
	this.capacityIncrement = capacityIncrement;
	elementData = new Object[initialCapacity];
	elementCount = 0;
    }
    
    public Vector(int initialCapacity)
    {
	this(initialCapacity, 0);
    }
    
    public Vector()
    {
	this(10);
    }

    // Methods

    public final int capacity()
    {
	return elementData.length;
    }


    /*
    public Object clone()
    {
	try
	    {
		Vector v = (Vector) super.clone();
		v.elementData = (Object[]) elementData.clone();

		return v;
	    }
	catch (CloneNotSupportedException e)
	    {
		return null;
	    }
    }
    */
    // TEST
    public Object clone()
    {
	Vector c = new Vector();
	for(int i=0; i<size(); i++) {
	    c.addElement(elementAt(i));
	}
	return c;
    }

    public final void trimToSize()
    {
	Object[] newData = new Object[elementCount];
	copyInto(newData);
	elementData = newData;
    }

    public final void addElement(Object obj)
    {
	add(obj);
    }

    public final void insertElementAt(Object obj, int index)
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

    public final Object firstElement()
	throws NoSuchElementException
    {
	if (isEmpty())
	    throw new NoSuchElementException();
	return elementData[0];
    }

    public final Object lastElement()
	throws NoSuchElementException
    {
	if (isEmpty())
	    throw new NoSuchElementException();
	return elementData[elementCount - 1];
    }

    public final Object elementAt(int index)
    {
	if (index >= elementCount)
	    throw new IndexOutOfBoundsException();
	return elementData[index];
    }

    public final Object get(int index) {
	return elementAt(index);
    }

    public final Enumeration elements()
    {
	Object[] data = new Object[elementCount];
	copyInto(data);
	return new ArrayEnumeration(data);
    }

    public final int indexOf(Object obj, int index)
    {
	for (int i = index; i < elementCount; i++)
	    if (elementData[i] != null && elementData[i].equals(obj))
		return i;
	return -1;
    }

    public final int indexOf(Object obj)
    {
	return indexOf(obj, 0);
    }

    public final boolean contains(Object obj)
    {
	return (indexOf(obj) >= 0);
    }

    public final int lastIndexOf(Object obj, int index)
    {
	int k = -1;
	for (int i = 0; i <= index; i++)
	    if (elementData[i].equals(obj))
		k = i;
	return k;
    }

    public final int lastIndexOf(Object obj)
    {
	return lastIndexOf(obj, elementCount - 1);
    }

    public final void removeAllElements()
    {
	while (elementCount > 0)
	    elementData[--elementCount] = null;
    }

    public final void clear() {
	removeAllElements();
    }

    public final void removeElementAt(int index)
    {
	for (int i = index + 1; i < elementCount; i++)
	    elementData[i - 1] = elementData[i];
	elementCount--;
    }

    public final Object remove(int index) {
	Object o = elementData[index];

	for (int i = index + 1; i < elementCount; i++)
	    elementData[i - 1] = elementData[i];
	elementCount--;

	return o;
    }

    public final boolean removeElement(Object obj)
    {
    for (int i = 0; i < elementCount; i++)
	if (elementData[i].equals(obj))
	    {
		removeElementAt(i);
		return true;
	    }
    return false;
    }

    public final boolean remove(Object o) {
	return removeElement(o);
    }

    public final void setElementAt(Object obj, int index)
    {
	if (index >= elementCount)
	    throw new IndexOutOfBoundsException();
	elementData[index] = obj;
    }

    public final void setSize(int newSize)
    {
	while (elementCount > newSize)
	    elementData[--elementCount] = null;
	ensureCapacity(newSize);
	while (elementCount < newSize)
	    elementData[elementCount++] = null;
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

    public Object[] toArray() {
	Object[] ret = new Object[elementCount];
	for (int i = 0; i < elementCount; i++) {
	    ret[i] = elementData[i];
	}
	return ret;
    }

}

