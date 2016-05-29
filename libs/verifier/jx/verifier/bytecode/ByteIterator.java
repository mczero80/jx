package jx.verifier.bytecode;


public class ByteIterator {
    public byte[] data;
    private int index;
    public ByteIterator(byte[] data) {
	this.data = data; 
	index = 0;
    }
    public byte getNext(){
	return data[index++];
    }
    public void reset() { reset(0);}
    public void reset(int index) {
	this.index = index;}
    public boolean hasMore() { return (index < data.length);}
    public int getIndex() {return index - 1;} //index des zuletzt gelieferten Elements
    
}
