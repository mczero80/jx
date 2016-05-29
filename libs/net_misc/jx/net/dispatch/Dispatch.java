package jx.net.dispatch;

import jx.buffer.separator.*;
import jx.zero.Memory;
import jx.zero.Debug;

public class Dispatch {
    UpperLayer[] upperLayers;
    int count=0;
    public static final boolean verbose = false;
    public Dispatch(int numberOfClients) {
	upperLayers = new UpperLayer[numberOfClients];
    }

    public void add(int id, String name) {
	if (verbose) {
	    Debug.out.println("Count: "+count);
	    Debug.out.println("Upper: "+upperLayers);
	    Debug.out.println("Upper.length: "+upperLayers.length);
	}
	upperLayers[count++] = new UpperLayer(id, name);
    }

    public boolean registerConsumer(MemoryConsumer consumer, String name) {
	for(int i=0; i<upperLayers.length; i++) {
	    if (upperLayers[i].name.equals(name)) {
		if (upperLayers[i].consumer != null) return false; // only one consumer can register
		upperLayers[i].consumer = consumer;
		return true;
	    }
	}
	return false;
    }

    public int findID(String name) {
	for(int i=0; i<upperLayers.length; i++) {
	    if (upperLayers[i].name.equals(name)) {
		return upperLayers[i].id;
	    }
	}
	Debug.out.println("Dispatch: Name "+name+"not found");
	return -1;
    }

    public String findName(int id) {
	for(int i=0; i<upperLayers.length; i++) {
	    if (upperLayers[i].id == id) {
		return upperLayers[i].name;
	    }
	}
	Debug.out.println("Dispatch: ID "+id+"not found");
	return "???";
    }
    
    public Memory dispatch(int id, Memory buf) {
	for(int i=0; i<upperLayers.length; i++) {
	    if (upperLayers[i].id == id) {
		if (verbose) Debug.out.println("Dispatch: " + upperLayers[i].name + " packet received!");
		if (upperLayers[i].consumer != null)
		    return  upperLayers[i].consumer.processMemory(buf);
		return buf;
	    }
	}
	return buf; // nobody is interested in this packet
    }
}
