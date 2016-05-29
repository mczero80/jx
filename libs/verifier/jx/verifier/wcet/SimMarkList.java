package jx.verifier.wcet;

//contains a list of ifs which caused the bytecode with the list to be marked.
public class SimMarkList {
    int[] list = new int[0];
    
    public boolean addElement(int newElm) {
	int[] newList = new int[list.length+1];
	for (int i = 0; i < list.length; i++) {
	    if (list[i] == newElm) 
		return false;
	    newList[i] = list[i];
	}
	newList[newList.length-1] = newElm;
	list = newList;
	return true;
    }

    public boolean contains(int element) {
	for (int i = 0; i < list.length; i++) {
	    if (list[i] == element)
		return true;
	}
	return false;
    }
}
