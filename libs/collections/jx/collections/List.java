package jx.collections;

import java.util.Vector;
import java.util.Enumeration;

public class List implements Collection {
    Vector elements=new Vector();

    public List() {
    }

    public void add(Object o) {
	elements.addElement(o);
    }

    public int size() {
	return elements.size();
    }

    public Iterator iterator() {
	return new Iterator() {
	    int counter;
	    public boolean hasNext() {
		return counter < elements.size();
	    }
	    public Object next() {
		return elements.elementAt(counter++);
	    }
	};
    }

    public Enumeration elements() {
	return new Enumeration() {
	    int counter;
	    public boolean hasMoreElements() {
		return counter < elements.size();
	    }
	    public Object nextElement() {
		return elements.elementAt(counter++);
	    }
	};
    }
}
