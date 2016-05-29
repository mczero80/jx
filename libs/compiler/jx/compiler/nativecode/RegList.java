package jx.compiler.nativecode;

public class RegList {
    
    class RegNode {
	public Reg reg;
	public RegNode next;
	public RegNode prev;
	public RegNode fnext;
	public RegNode(Reg reg) {
	    this.reg  = reg;
	    this.next = null;
	    this.prev = null;
	}
    }

    private RegNode top;
    private RegNode end;
    private RegNode curr;
    private RegNode freeList;

    private int numberOfElements;

    public RegList() {
	top      = null;
	end      = null;
	curr     = null;
	freeList = null;
	numberOfElements=0;
    }

    public void append(Reg reg) {
	//System.err.println("      append "+reg);

	RegNode node = newNode(reg);

	numberOfElements++;

	if (end==null) {
	    top = node;
	    end = node;
	    return;
	}

	node.prev = end;
	end.next  = node;
	end       = node;
    }

    public Reg top() {
	curr=top;
	if (curr==null) return null;
	return curr.reg;
    }

    public boolean hasMore() {
	if (curr==null) return false;
	return curr.next!=null;
    }

    public Reg next() {
	if (curr!=null) curr=curr.next;
	if (curr!=null) return curr.reg;
	return null;
    }

    public int size() {
	return numberOfElements;
    }

    public void remove(Reg reg) {
	RegNode node;

	if (top==null) return;

	for (node=top;node!=null;node=node.next) {
	    if (node.reg == reg) {

		numberOfElements--;

		if (node.prev==null) {
		    top=node.next;
		    if (top!=null) top.prev=null;
		} else {
		    node.prev.next = node.next;
		}
		if (node.next==null) {
		    end=node.prev;
		    if (end!=null) end.next=null;
		} else {
		    node.next.prev = node.prev;
		}
		freeNode(node);
		return;
	    }
	}
    }

    public Reg remove() {
	if (curr==null) return null;

	//System.err.println(this + " del " + curr.reg);

	numberOfElements--;

	if (curr==top) {
	    top = top.next;
	    if (top!=null) {
		top.prev = null;
	    } else {
		end=null;
		curr=null;
		return null;
	    }
	    curr = top;
	    return curr.reg;
	}
	if (curr==end) {
	    end = end.prev;
	    if (end!=null) {
		end.next = null;
	    } else {
		curr = null;
		return null;
	    }
	    curr = end;
	    return curr.reg;
	}
	curr.prev.next = curr.next;
	curr.next.prev = curr.prev;
	freeNode(curr);
	curr = curr.next;

	//System.err.println(this);
	if (curr==null) return null;
	return curr.reg;
    }

    public boolean knows(Reg reg) {
	RegNode node = top;
	while (node!=null && node.reg!=reg) {
	    node=node.next;
	}
	return (node!=null);
    }

    public void clear() {
	RegNode node = top;
	top=null;
	end=null;
	while (node!=null) {
	    freeNode(node);
	    node=node.next;
	}
	numberOfElements=0;
    }

    public String toString() {
	RegNode node = top;
	String rstr = "#"+Integer.toString(numberOfElements);
	while (node!=null) {
	    rstr = rstr + " "+node.reg.toString();
	    node=node.next;
	}
	return rstr;
    }

    //===================================
    // helpers
    //===================================

    private RegNode locateNode(Reg reg) {
	RegNode node = top;
	while (node != null && node.reg != reg) node=node.next;
	return node;
    }

    private void freeNode(RegNode node) {
	node.reg  = null;
	node.fnext = freeList;
	freeList  = node;	
    }
    
    private RegNode newNode(Reg reg) {
	if (freeList==null) {
	    return new RegNode(reg);
	}

	RegNode node = freeList;
	freeList=node.fnext;

	node.reg   = reg;
	node.next  = null;
	node.prev  = null;
	node.fnext = null;

	return node;
    }
}
