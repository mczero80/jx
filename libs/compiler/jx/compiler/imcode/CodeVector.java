package jx.compiler.imcode;

public class CodeVector {

    private int size;
    private CodeContainer[] code;

    public CodeVector() {
	size = 0;
	code = new CodeContainer[5];
    }

    public CodeContainer element(int i) {
	return code[i];
    }

    public void add(CodeContainer aCode) {
	if (aCode==null) return;
	realloc(size);
	code[size++] = aCode;
    }

    public void add(CodeVector vec) {
	if (vec==null) return;
	realloc(size+vec.size());
	for (int i=0;i<vec.size();i++) {
	    code[size++]=vec.element(i);
	}
    }

    public int size() {
	return size;
    }

    private void realloc(int nSize) {
	if (nSize>code.length) {
	    CodeContainer[] newArray = new CodeContainer[code.length+5];
	    for (int i=0;i<code.length;i++) newArray[i] = code[i];
	    code = newArray;
	}
    }
    
}
