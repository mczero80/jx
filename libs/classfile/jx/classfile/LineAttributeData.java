package jx.classfile; 

public class LineAttributeData {
    public int startBytecodepos;
    public int lineNumber;
    public LineAttributeData(int startBytecodepos, int lineNumber) {
	this.startBytecodepos = startBytecodepos;
	this.lineNumber = lineNumber;
    }
    public LineAttributeData copy() {
	return new LineAttributeData(startBytecodepos, lineNumber);
    }
}
