package jx.compiler.symbols; 

import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

import jx.zero.Debug; 

import java.io.*;  
import java.util.Hashtable;

/** 
    Some values that are needed in the compiled program are not
    known at compile time, e.g. the jump addresses for forward jumps. 
    The compiler has to remember the position of such values in 
    the code, and insert the correct value once it is known. 
    This class represents such an unresolved value. 
    The different types of unresolved values are organized in a
    class hierarchy. 
    (Perhaps it would be better to call this class FixupValue ?) 
    Note: This is actually an abstract class, since some methods 
    are not implemented (they throw errors). 
*/ 

public abstract class SymbolTableEntryBase {

    public static final String[] ENTRY_TYPES = { 
	/* 0  */ "Undefined",
	/* 1  */ "jx.compiler.persistent.DomainZeroSTEntry",
	/* 2  */ "jx.compiler.persistent.ExceptionHandlerSTEntry",
	/* 3  */ "jx.compiler.persistent.DEPFunctionSTEntry" ,
	/* 4  */ "jx.compiler.persistent.StaticFieldSTEntry",
	/* 5  */ "jx.compiler.persistent.AllocObjectSTEntry",
	/* 6  */ "jx.compiler.persistent.ClassSTEntry",
	/* 7  */ "jx.compiler.persistent.DirectMethodCallSTEntry",
	/* 8  */ "jx.compiler.persistent.StringSTEntry" ,
	/* 9  */ "jx.compiler.persistent.AllocArraySTEntry",
	/* 10 */ "jx.compiler.symbols.AllocMultiArraySTEntry",
	/* 11 */ "jx.compiler.persistent.LongArithmeticSTEntry",
	/* 12 */ "jx.compiler.persistent.VMSupportSTEntry",
	/* 13 */ "jx.compiler.persistent.PrimitiveClassSTEntry",
	/* 14 */ "jx.compiler.symbols.UnresolvedJump",
	/* 15 */ "jx.compiler.persistent.VMAbsoluteSTEntry",
	// old version
	/* 16 */ "<not in use>",
	// new compiler
	/* 17 */ "jx.compiler.symbols.StackMapSTEntry",
	/* 18 */ "jx.compiler.symbols.ExceptionTableSTEntry",
	/* 19 */ "jx.compiler.symbols.CurrentThreadPointerSTEntry",
	/* 20 */ "jx.compiler.symbols.StackChunkSizeSTEntry",
	/* 21 */ "jx.compiler.symbols.ProfileSTEntry",
	/* 22 */ "jx.compiler.symbols.MethodeDescSTEntry",
	/* 23 */ "jx.compiler.symbols.TCBOffsetSTEntry",
    };
    
    //private static Hashtable type2Number = new Hashtable();
    /*
    private static Hashtable number2Type = new Hashtable();

    static {
	for(int i=0; i<ENTRY_TYPES.length; i++) {
	    //type2Number.put(ENTRY_TYPES[i].intern(), new Integer(i));
	    number2Type.put(new Integer(i), ENTRY_TYPES[i].intern());
	}
    }
    */
    
    // not used 
    // protected static final int STATIC_FIELD_TYPE_ID = 1; 
    // protected static final int INTERNAL_ADDR_TYPE_ID = 2; 

    protected boolean validID;
    protected int     stringID;
    
    private int immediateNCIndex;        // position of constant in code 
    private int numBytes;                // number of bytes of constant  
    private int nextInstrNCIndex=-1;        // index of next instruction  
    private boolean isImmediateRelative; // relative to next instruction 
    
    private int resolvedValue; // remember it for debugging purposes
    private int resolvedCodeBase; // remember it for debugging purposes
    
    public SymbolTableEntryBase() {
	immediateNCIndex = -1; 
    }
    
    public String getDescription() {
	return "STEntry("+
	    resolvedCodeBase+":"+
	    (isImmediateRelative?(nextInstrNCIndex+":"):"-:")+
	    resolvedValue+")";
    }
    
    /** 
	@return true, if the value that should be inserted is known 
    */ 
    public boolean isResolved() {return false;} 
    
    /** 
	@return true, if the location of insertion is known, i.e. 
	if one of the initNCIndex...()-Methods has been called 
    */ 
    public final boolean isNCIndexInitialized() {
	return immediateNCIndex != -1;
    }
    
    /** 
	if this method returns false, the codeBase-Parameter of
	apply() has not effect (the entry can be resolved before 
	the codebase is known) 
    */ 
    public final boolean needsCodeBase() {
	return isImmediateRelative != isValueRelativeToCodeBase();
    }
    
    /** 
	@return true, if all data for resolution is known 
    */ 
    public final boolean isReadyForApply() {
	//    return isResolved() && isNCIndexInitialized(); 
	return true;
    }
    
    /** 
	@return value of this entry. 
	@see isValueRelativeToCodeBase
    */ 
    public int getValue() {
	Debug.assert(isResolved()); 
	Debug.throwError("Not implemented"); 
	return -1234567; 
    }

    /** 
	True, if the value returned by getValue() 
	is relative to the code base. 
    */ 
    public boolean isValueRelativeToCodeBase() {
	return false; 
    }
    
    /** 
	@return the absolute value of this entry. 
	(favor this method over getValue() !!!) 
    */ 
    public final int getValue(int codeBase) {
	if (isValueRelativeToCodeBase()) 
	    return getValue() + codeBase; 
	else 
	    return getValue(); 
    }
    
    /** 
	make an entry that is relative to the next 
	instructions position (call, jge, jmp ...) 
    */ 
    public void initNCIndexRelative(int immediateNCIndex,
				    int numBytes,
				    int nextInstrNCIndex) {
	this.immediateNCIndex = immediateNCIndex; 
	this.numBytes = numBytes; 
	this.nextInstrNCIndex = nextInstrNCIndex; 
	this.isImmediateRelative = true; 
    }
    
    /** 
	make an entry with absolute value
    */  
    public void initNCIndexAbsolute(int immediateNCIndex,
				    int numBytes) {
	this.immediateNCIndex = immediateNCIndex; 
	this.numBytes = numBytes; 
	this.nextInstrNCIndex = -1;
	this.isImmediateRelative = false; 
    }
    
    /** 
     *   same as initNCIndexAbsolute
     *  (for preproc-version) 
     *  (useful for entries like UnresolvedJump)
     * @param immediateNCIndex code position to be patched
     * @param numBytes number of bytes to be inserted
     */ 
    public void initNCIndex(int immediateNCIndex, int numBytes) {
	this.immediateNCIndex = immediateNCIndex; 
	this.numBytes = numBytes; 
    }
    
    /** 
     * for preproc-version 
     * call after initNCIndex 
     *  (useful for entries like UnresolvedJump)
     * @param nextInstrNCIndex the ip of the jump target  
     */ 
    public void makeRelative(int nextInstrNCIndex) {
	this.nextInstrNCIndex = nextInstrNCIndex; 
	this.isImmediateRelative = true; 
    }    
    
    public boolean isRelative() {
	return isImmediateRelative;
    }    
    
    /** 
	insert the value of the entry into the code 
    */ 
    public abstract void apply(byte[] code, int codeBase);   
    
    public void writeEntry(ExtendedDataOutputStream out) throws IOException {
	String classname = this.getClass().getName();
	int nr = 0;	
	
	for (int i=0;i<ENTRY_TYPES.length;i++) {
	    if (ENTRY_TYPES[i].equals(classname)) {
		nr = i;
		break;
	    }
	}
	
	Debug.assert(nr!=0,"Symbol not registered in SymbolTableEntryBase: "+this.getClass().getName());
	
	out.writeInt(nr);
	out.writeInt(immediateNCIndex);
	out.writeInt(numBytes);
	out.writeInt(nextInstrNCIndex);
	/*
	out.writeShort((short)nr);
	out.writeShort((short)immediateNCIndex);
	out.writeShort((short)numBytes);
	out.writeShort((short)nextInstrNCIndex);
	*/
    }
    
    public void readEntry(ExtendedDataInputStream in) throws IOException {
	
	immediateNCIndex = in.readInt();
	numBytes = in.readInt();
	nextInstrNCIndex = in.readInt();
	/*
	immediateNCIndex = in.readShort();
	numBytes = in.readShort();
	nextInstrNCIndex = in.readShort();
	*/
    }
    
    public static SymbolTableEntryBase readUnknownEntry(ExtendedDataInputStream in)  throws Exception {
	int nr = in.readInt();
	//int nr = in.readShort();
	String entryName = ENTRY_TYPES[nr];
	Class entryClass = Class.forName(entryName);
	SymbolTableEntryBase entryInstance = (SymbolTableEntryBase)entryClass.newInstance();
	entryInstance.readEntry(in);
	return entryInstance;
    }
    
    /** 
	insert the value of 'absoluteValue' into the code 
    */ 
    protected final void applyValue(byte[] code, int codeBase, int absoluteValue) {
	resolvedValue = absoluteValue; 
	resolvedCodeBase = codeBase;
	if (isImmediateRelative) resolvedValue -= (nextInstrNCIndex + codeBase); 
	insertInteger(code, immediateNCIndex, resolvedValue, numBytes); 
    }
    
    /** 
	insert the value of 'absoluteAddress' into the code 
    */ 
    protected final void myApplyValue(byte[] code, int codeBase, int absoluteAddress) {
	resolvedCodeBase = codeBase;
	resolvedValue = absoluteAddress - (nextInstrNCIndex + codeBase); 
	insertInteger(code, immediateNCIndex, resolvedValue, numBytes); 
    }
    
    /** 
     *  reolve a relative address
     * (useful for entries like UnresolvedJump)
     */ 
    protected final void myApplyValue(byte[] code, int targetNCIndex) {
	resolvedValue = getRelativeOffset(targetNCIndex);
	insertInteger(code, immediateNCIndex, resolvedValue, numBytes); 
    }

    protected final int getRelativeOffset(int targetNCIndex) {
	return targetNCIndex - (immediateNCIndex + numBytes);
    }

    protected final int getNextInstrNCIndex() {
	return nextInstrNCIndex;
    }
    
    /** 
	insert a value that is relative to the codebase into 
	the code. 
    */ 
    protected final void applyRelativeValue(byte[] code, int relativeToCodeBaseValue) {
	Debug.assert(isImmediateRelative); 
	resolvedValue = relativeToCodeBaseValue - nextInstrNCIndex; 
	resolvedCodeBase = 0;
	insertInteger(code, immediateNCIndex, resolvedValue, numBytes); 
    }
    
    
    /** 
	Insert an integer into the code. 
    */ 
    protected final void insertInteger(byte[] code, int value) {
	// Debug.assert(numBytes==4 || (value < 128 && value >= -128)); 
	insertInteger(code, immediateNCIndex, value, numBytes); 
    }
    
    /** 
	Insert an integer into the code. 
    */       
    protected void insertInteger(byte[] code, int ncIndex,
				 int value, int numBytes) {
	code[ncIndex] = (byte)(value >> 0); 
	if (numBytes == 1) return; 
	code[ncIndex+1] = (byte)(value >> 8); 
	if (numBytes == 2) return;     
	code[ncIndex+2] = (byte)(value >> 16); 
	code[ncIndex+3] = (byte)(value >> 24);
    } 

    public String toString() {
	return getDescription();
    }


    public String toGASFormat() {
	return ".L"+nextInstrNCIndex;
    }

    public void dump() {
	Debug.out.println(" SymbolTableEntryBase");
	Debug.out.println("  immediateNCIndex: "+immediateNCIndex);
	Debug.out.println("  numBytes: "+numBytes);
	Debug.out.println("  nextInstrNCIndex: "+nextInstrNCIndex);
	Debug.out.println("  isImmediateRelative: "+isImmediateRelative);
	
	Debug.out.println("  resolvedValue: "+resolvedValue);
	Debug.out.println("  resolvedCodeBase: "+resolvedCodeBase);
    }


    public void registerStrings(StringTable stringTable) {}
}
  

