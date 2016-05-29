package jx.compiler.symbols;

import java.io.IOException;
import jx.zero.Debug;

import jx.compiler.imcode.MethodStackFrame;
import jx.compiler.imcode.IMNode;

import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;
import jx.compiler.execenv.TypeMap;

public class StackMapSTEntry extends SymbolTableEntryBase {

    private MethodStackFrame frame;  

    private boolean[] lvars;
    private boolean[] vars;
    private boolean[] oprs;
    private int       immediateNCIndexPre;
    private IMNode    node;

    public StackMapSTEntry(IMNode node,int preIP,MethodStackFrame frame) {
	this.frame     = frame;
	this.vars      = frame.getVarMap();
	this.lvars     = node.getVarStackMap();
        this.node      = node;
	this.oprs      = frame.getOprMap();
	this.immediateNCIndexPre = preIP;
    }

    public String getDescription() {
	return super.getDescription()+",symbols.StackMapSTEntry";
    }
    
    public boolean isResolved() {
	return false;
    }

    public int getValue() {
	Debug.throwError();
	return 0;
    }

    public void apply(byte[] code, int codeBase) {
	Debug.throwError();
    }

    public void writeEntry(ExtendedDataOutputStream out) throws IOException {
	super.writeEntry(out);
	out.writeInt(immediateNCIndexPre);      
	TypeMap.writeMap(out, stackMap(), true);
    }    

    private boolean[] stackMap() {
	int extraSpace = (frame.getExtraSpace()-4)/4;
	int lvarSize   = frame.getLVarMapSize();
	int varSize    = frame.getVarMapSize();
	int m;

	boolean[] currStackMap = new boolean[extraSpace + lvarSize + varSize + oprs.length];

	for (int i=0;i<extraSpace;i++) {
	    currStackMap[i] = false;
	}

	m=extraSpace;
	if (lvars!=null) {
	    for (int i=0;i<lvars.length;i++) {
		currStackMap[m + i] = lvars[i];
	    }
	}

	m+=lvarSize;
	for (int i=0;i<vars.length;i++) {
	    currStackMap[m + i] = vars[i];
	}

	m+=varSize;
	for (int i=0;i<oprs.length;i++) {
	    currStackMap[m + i] = oprs[i];
	}

	if (node.getCompilerOptions().isOption("dumpStackMaps")) {Debug.out.println(toString(currStackMap));}
	    
	return currStackMap;
    }

    public String toString(boolean[] map) {
        String ret="StackMapSTEntry:"+node.getLineInfo();
        for (int i=0;i<map.length;i++) {
           if (map[i]) ret+="R"; else ret+=".";
        } 
	return ret+" "+node.toReadableString();
    }
}

