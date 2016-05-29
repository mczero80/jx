package java.util;

class HashtableEnumeration implements Enumeration
{
	// Fields

	private Object[] source;
	private int index;
	private Object next;

	// Private Methods

	private void next_element()
	{
		while (index < source.length)
		{
			Object obj = source[index++];
			if (obj == null)
				continue;

			next = obj;
			return;
		}
		next = null;
	}

	// Public Methods

	public boolean hasMoreElements()
	{
		return (next != null);
	}

	public Object nextElement() throws NoSuchElementException
	{
		if (next == null)
			throw new NoSuchElementException();
		Object obj = next;
		next_element();
		return obj;
	}

	public HashtableEnumeration(Object[] src)
	{
		source = src;
		index = 0;
		next_element();
	}
}

public class Hashtable extends Dictionary implements Cloneable
{ 
	// Fields

	Object[] keys;
	Object[] values;
	int capacity;
  private int load; // "real load" * 100 
	private int mask;
	private int size;

	// Protected Methods

	protected void rehash()
	{
		while (size*100 > capacity * load)
			capacity <<= 1;
		mask = (capacity - 1);

		Object[] oldkeys = keys;
		Object[] oldvalues = values;
		keys = new Object[capacity];
		values = new Object[capacity];
		size = 0;

		for (int i = 0; i < oldkeys.length; i++)
			if (oldkeys[i] != null)
				put(oldkeys[i], oldvalues[i]);
	}

	// Public Methods

	public String toString()
	{
		StringBuffer buff = new StringBuffer("{");
		boolean first = true;

		for (int i = 0; i < keys.length; i++)
			if (keys[i] != null)
			{
				if (first)
					first = false;
				else
					buff.append(", ");
				buff.append(String.valueOf(keys[i]));
				buff.append("=");
				buff.append(String.valueOf(values[i]));
			}
		buff.append("}");

		return buff.toString();
	}

	public Object clone()
	{
		try
		{
			Hashtable h = (Hashtable)super.clone();

			h.keys = (Object[]) keys.clone();
			h.values = (Object[]) values.clone();

			return h;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	public void clear()
	{
		for (int i = 0; i < keys.length; i++)
			keys[i] = values[i] = null;
		size = 0;
	}

	public Object get(Object key)
	{
		if (key == null)
			throw new NullPointerException();

		int hash = key.hashCode() & mask;

		for (int i = 0; i < capacity; i++)
		{
			int index = (hash + i) & mask;

			if (keys[index] != null) {
				if (keys[index].equals(key))
					// Found it, all done.
					return values[index];

				// Continue looking through collisions.
				continue;
			}

			// Fall through if nothing found at index
			int k = index;
			int diff = k - i;

			for (int j = i + 1; j < capacity; j++)
			{
				index = (hash + j) & mask;

				if ((keys[index] == null)
				    || (! keys[index].equals(key)))
					continue;

				keys[k] = keys[index];
				values[k] = values[index];

				keys[index] = null;
				values[index] = null;

				return values[k];
			}

			return null;
		}

		return null;
	}

	public Object put(Object key, Object value)
	{
		if (size*100 > capacity * load)
			rehash();

		if ((key == null) || (value == null))
			throw new NullPointerException();

		// XXX this causes javac to fault...
		/*
		  kore.util.NativeDebug.printStrln(kore.util.NativeDebug.HASHTABLE, 
		    "Hashtable.put: key= " +key.toString()+
		    "; val= " +value.toString());
		*/
		int hash = key.hashCode() & mask;

		for (int i = 0; i < capacity; i++)
		{
			int n = (hash + i) & mask;

			if (keys[n] != null) {
				if (keys[n].equals(key)) {
				Object obj = values[n];

				keys[n] = key;
				values[n] = value;

				return obj;
			}

				continue;
			}

			keys[n] = key;
			values[n] = value;

			size++;

			for (i++; i < capacity; i++)
			{
				n = (hash + i) & mask;

				if ((keys[n] == null) ||
				    (! keys[n].equals(key)))
					continue;

				Object obj = values[n];

				keys[n] = null;
				values[n] = null;

				size--;

				return obj;
			}

			return null;
		}

		return null;
	}

	public Object remove(Object key)
	{
		int hash = key.hashCode() & mask;

		for (int i = 0; i < capacity; i++)
		{
			int n = (hash + i) & mask;

			if (keys[n] == null ||
			    (! keys[n].equals(key)))
				continue;

			Object value = values[n];
			
			keys[n] = null;
			values[n] = null;

			size--;

			return value;
		}

		return null;
	}

	public Enumeration keys()
	{
		return new HashtableEnumeration(keys);
	}

	public Enumeration elements()
	{
		return new HashtableEnumeration(values);
	}

	public int size()
	{
		return size;
	}

	public boolean isEmpty()
	{
		return (size == 0);
	}

	public boolean containsKey(Object key)
	{
		int hash = key.hashCode() & mask;

		if (key == null)
			return false;

		for (int i = 0; i < capacity; i++)
		{
			int n = (hash + i) & mask;

			if ((keys[n] != null)
			    && (keys[n].equals(key)))
				return true;
		}
		return false;
	}

	public boolean contains(Object value)
	{
		if (value == null)
			return false;

		for (int i = 0; i < capacity; i++)
			if ((values[i] != null)
			    && (values[i].equals(value)))
				return true;
		return false;
	}

	// Constructors 
  
	public Hashtable(int initialCapacity, int loadFactor)
	{
		this.load = loadFactor;
		size = 0;
		for (capacity = 1; capacity <= initialCapacity; capacity <<= 1)
			;
		keys = new Object[capacity];
		values = new Object[capacity];
		mask = (capacity - 1);
	}

	public Hashtable(int initialCapacity)
	{
		this(initialCapacity, 75);
	}

	public Hashtable()
	{
		this(32);
	}
}

