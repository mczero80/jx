package java.util;

public abstract class AbstractList extends AbstractCollection implements List 
{
    protected int capacityIncrement;
    protected int elementCount;
    protected Object[] elementData;

    protected final void ensureCapacity(int minCapacity)
    {
	if (minCapacity <= elementData.length) return;

	Object[] newData=null;;

	try {
	    int newSize =
		capacityIncrement > 0
		? elementData.length + capacityIncrement
		: elementData.length * 2;
	    
	    if (newSize < minCapacity) newSize = minCapacity;
	    newData = new Object[newSize];
	} catch (Error error) {
	    newData = new Object[minCapacity]; 
	}

	copyInto(newData);
	elementData = newData;
    }

    public final void copyInto(Object[] array)
    {
	System.arraycopy(
			 (Object)elementData, 0,
			 (Object)array, 0,
			 elementCount
			 );
    }


    public boolean add(Object obj) {
	ensureCapacity(elementCount + 1);
	elementData[elementCount] = obj;
	elementCount++;
	return true;
    }
    public Iterator iterator() {
	throw new Error("ITERATOR");
    }

}
