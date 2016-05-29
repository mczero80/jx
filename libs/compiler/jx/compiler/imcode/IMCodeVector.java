package jx.compiler.imcode;

public class IMCodeVector {

    private int size;
    private IMNode[] code;

    public IMCodeVector() {
	size = 0;
	code = new IMNode[5];
    }

    public IMNode element(int i) {
	return code[i];
    }

    public void add(IMNode aCode) {
	if (aCode==null) return;
	realloc(size);
	code[size++] = aCode;
    }

    public int size() {
	return size;
    }

    private void realloc(int nSize) {
	if (nSize>code.length) {
	    IMNode[] newArray = new IMNode[code.length+5];
	    for (int i=0;i<code.length;i++) newArray[i] = code[i];
	    code = newArray;
	}
    }
    
}
