package jx.compiler.imcode;

final public class IMNodeList {

    private static final int block=20;
    private IMNode[] nodes;
    private int free;

    public IMNodeList() {
	nodes = new IMNode[block];
	free = 0;
    }

    public void add(IMNode node) {
	if (free<nodes.length) {
	    nodes[free++]=node;
	} else {
	    IMNode[] nnodes = new IMNode[nodes.length+block];
	    System.arraycopy(nodes,0,nnodes,0,nodes.length);
	    nodes = nnodes;
	    nodes[free++]=node;
	}
    }

    public IMNode at(int i) {
	if (i>=free) throw new ArrayIndexOutOfBoundsException();
	return nodes[i];
    }

    public int size() {
	return free;
    }
}
