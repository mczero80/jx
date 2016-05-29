package jx.compiler.nativecode; 

import java.util.Vector; 
import java.util.Enumeration; 

import java.io.PrintStream;

import jx.compiler.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.*;

import jx.zero.Debug; 

/** 
    Parallel to this class there is a class 
    nativeCode.Binarycode. 
    In this version of the compiler, the second class 
    is used as a mere container, while this class 
    is used to assemble the binary code. 
*/ 

public final class BinaryCodeIA32 {


    private boolean doAlignJumpTargets=false;

    // not private, so that javac can do inlining 
    // not accessed by any other classes (they are used as if they were private)
    private byte[] code; 
    private int ip;

    int numBytesMachinecode;

    // mapping from instruction addresses to bytecode
    Vector instructionTable = new Vector();
    int bcIndex, startIP;

    // native code array reallocation 
    private static final int INITSIZE  = 100; 
    private static final int CHUNKSIZE = 200;

    /** 
	After compiling a method, symbolTable contains _all_ 
	unresolved constants of the code. 
	These include
	- jump offsets of jumps inside the code
	- invocations of functions 
	- constant pool entries that should be stored to 
        allow the storing of compiled code between JVM invocations 
	- actually all subclasses of nativecode.SymbolTableEntryBase
    */ 
    private Vector symbolTable; 
  
    /** 
	contains the native exception handlers
    */ 
    private Vector exceptionHandlers; 

    public BinaryCodeIA32() {
	code = new byte[INITSIZE]; 
	ip = 0;
	symbolTable = new Vector(); 
	exceptionHandlers = new Vector(); 
    }

    /** 
	The methods in the frontend expect the compiled code
	stored inside of a object of class nativecode.BinaryCode. 
	-> Convert a object of preproc.BinaryCodePreproc into a object of 
	nativecode.BinaryCode.
	Note: Exceptionhandlers are not copied. 
    */ 
    /*    public jx.jit.nativecode.BinaryCode getOldBinaryCode() {*/
    public BinaryCode getOldBinaryCode() {

	Enumeration enum = symbolTable.elements(); 
	Vector unresolvedEntries = new Vector(); 
	while(enum.hasMoreElements()) {
	    SymbolTableEntryBase entry = (SymbolTableEntryBase)enum.nextElement();
	    if (entry instanceof IntValueSTEntry) {
		((IntValueSTEntry)entry).applyValue(code);
		//entry.apply(code, codeBase);
	    } else {
		unresolvedEntries.addElement(entry); 
	    }
	}
	symbolTable = unresolvedEntries;	

	//return new jx.jit.nativecode.BinaryCode(code, ip, symbolTable); 
	return new BinaryCode(code, ip, symbolTable); 
    }

    public int getCurrentIP() { return ip; }

    public void realloc() {
	realloc(CHUNKSIZE); 
    }

    /** 
	Realloc memory in the byte code array. 
	After calling this method, there are at least 
	'requiredSpace' free bytes in the array. 
    */ 
    public void realloc(int requiredSpace) {
	//System.err.println(" --- realloc --- "+Integer.toString(ip)+" "+this);
	if (ip + requiredSpace > code.length) {
	    int newSize = code.length;

	    if (code.length>requiredSpace && code.length<8000) {
		newSize += code.length;
	    } else {
		newSize += requiredSpace;
	    }
	    byte[] newCode = new byte[newSize];
	    for(int i=0;i<ip;i++) newCode[i] = code[i];
	    code = newCode;
	}
    }
    
    // ***** Code Generation ***** 
    
    /** 
	Insert a single byte
    */ 

    void insertByte(int value) {
	code[ip++] = (byte)value;
    }

    void insertByte(SymbolTableEntryBase entry) {
	realloc();
	// size is always 1 bytes
	entry.initNCIndex(ip, 1);
	symbolTable.addElement(entry); 
	ip+=1; 
    }

    void insertBytes(int a,int b) {
	code[ip++] = (byte)a;
	code[ip++] = (byte)b;
    }

    /**
       Insert ModRM and SIB byte 
    */

    private void insertModRM(int reg,Opr rm) {
	reg = reg & 0x07;
	rm.value  = rm.value & 0x07;
	if (rm.tag == Opr.REF) {
	    Ref ref = (Ref)rm;
	    if (ref.sym_disp!=null) {
		insertByte(0x80 | (reg<<3) | ref.value);
		if (ref.value==4) insertByte(0x24);
		insertConst4(ref.sym_disp);
		return;
	    }
	    if (ref.disp==0) {
		if (ref.hasIndex) {
		    insertByte(0x04 | (reg << 3));
		    insertByte(ref.sib);
		} else {
		    insertByte(0x00 | (reg<<3) | ref.value);
		    if (ref.value==4) insertByte(0x24);
		}
	    } else if (is8BitValue(ref.disp)) { 
		if (ref.hasIndex) {
		    insertByte(0x44 | (reg << 3));
		    insertByte(ref.sib);
		} else {
		    insertByte(0x40 | (reg<<3) | ref.value);
		    if (ref.value==4) insertByte(0x24);
		}
		insertByte((byte)ref.disp);
	    } else {
		if (ref.hasIndex) {
		    insertByte(0x84 | (reg << 3));
		    insertByte(ref.sib);
		} else {		    
		    insertByte(0x80 | (reg<<3) | ref.value);
		    if (ref.value==4) insertByte(0x24);
		}
		insertConst4(ref.disp);
	    }
	    return;
	}
	insertByte(0xc0 | (reg<<3) | rm.value);
    }	
	
    private void insertModRM(Reg reg,Opr rm) {
	insertModRM(reg.value,rm);
    }

    /**
       Insert call near indirect (reg/mem) (2 clks)
    */

    public void call(Opr opr) {
	realloc();
	insertByte(0xff);
	insertModRM(2,opr);
    }

    /**
       Insert call near (Symbol) (1 clks)
    */

    public void call(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0xe8); 
	entry.initNCIndexRelative(ip, 4,ip+4);  // size is always 4 bytes 
	symbolTable.addElement(entry); 
	ip+=4; 	    
    }

    /**
       Convert byte to word (3 clks) + (.. clks)
    */

    public void cbw() {
	realloc();
	insertByte(0x66);
	insertByte(0x98);
    }

    /**
       Convert double to quad word (2 clks)

       fill edx with sign bit of eax
    */
    public void cdq() {
	realloc();
	insertByte(0x99);
    }

    /**
       Convert word to double word (3 clks)
       fill dx with sign bit of ax
    */

    public void cwde() {
	realloc();
	insertByte(0x98);
    }

    /**
       Convert word to double (2 clks)
       fill dx with sign bit of ax
    */
    public void cwd() {
	realloc();
	insertByte(0x66);
	insertByte(0x99);
    }


    /**
       Insert return
    */

    public void ret() {
	realloc();
        insertByte(0xc3);
    }

    /**
       clear interrupt flag (7 clks)
    */

    public void cli() { 
	realloc();
	insertByte(0xfa);
    }

    /**
       decrement byte value by 1 (1/3 clks)
    */

    public void decb(Opr opr) {
	realloc();
	insertByte(0xfe);
	insertModRM(1,opr);
    }

    /**
       decrement long value by 1 (1/3 clks)
    */

    public void decl(Ref ref) {
	realloc();
	insertByte(0xff);
	insertModRM(1,ref);
    }

    /** 
       decrement register by 1 (1 clks)
    */
    
    public void decl(Reg reg) {
	realloc();
	insertByte(0x48+reg.value);
    }

    /**
       Insert a pushl(reg)
    */

    public void pushl(Reg reg) {
	realloc();
	insertByte(0x50+reg.value);
    }

    public void pushl(Ref ref) {
	realloc();
        insertByte(0xff);
        insertModRM(6,ref);
    }

    public void pushl(int immd) {
	realloc();
        insertByte(0x68);
        insertConst4(immd);
    }

    public void pushl(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x68);
	insertConst4(entry);
    }
	
    public void pushfl() { 
	realloc();
        insertByte(0x9c);
    }

    /**
       push all general registers
       (eax,ecx,edx,ebx,esp,ebp,esi,edi) 
       (5 clks)
    */

    public void pushal() { /* 5 clks */
	realloc();
        insertByte(0x60);
    }

    /** 
       Insert a popl(reg)
    */

    public void popl(Reg reg) {
	realloc();
	insertByte(0x58+reg.value);
    }

    /**
       pop stack into eflags register (4 clks)
    */

    public void popfl() { 
	realloc();
        insertByte(0x9d);
    }

    /**
       pop all general register
    */

    public void popal() {
	realloc();
        insertByte(0x61);
    }

  /** 
      lock prefix
  */

  public void lock() {
    insertByte(0xf0);
  }

  /**
     rep prefix
  */

  public void repz() {
    insertByte(0xf3);
  }


  /** 
      spinlocks
  */

  public void spin_lock(Ref lock) {
    realloc();
    lock();decb(lock);
    js(2);
    jmp(9);
    cmpb(0,lock);
    repz();nop();
    insertByte(0x7e);insertByte(0xf9);
    insertByte(0xeb);insertByte(0xf0);
  }

  public void spin_unlock(Ref lock) {
    movb(1,lock);
  }

    /**
       Integer Subtraction
    */

    public void subl(Opr src,Reg des) {
	realloc();
	insertByte(0x2b);
	insertModRM(des,src);
    }

    public void subl(Reg src,Ref des) {
	realloc();
	insertByte(0x29);
	insertModRM(src,des);
    }

    public void subl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG) && (des.value==0)) {
	    insertByte(0x2D);
	    insertConst4(immd);
	} else if (is8BitValue(immd)) { /* FIXME */
	    insertByte(0x83);
	    insertModRM(5,des);
	    insertByte(immd);	    
	} else {
	    insertByte(0x81);
	    insertModRM(5,des);
	    insertConst4(immd);
	}
    }

    public void subl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG) && (des.value==0)) {
	    insertByte(0x2D);
	    insertConst4(entry);
	    /* FIXME: no 8 bit support yet 
	       } else if (is8BitValue(immd)) {
	       insertByte(0x83);
	       insertModRM(5,des);
	       insertByte(immd);
	    */
	} else {
	    insertByte(0x81);
	    insertModRM(5,des);
	    insertConst4(entry);
	}
    }

    /**
       Integer Subtraction with Borrow
    */

    public void sbbl(Opr src,Reg des) {
	realloc();
	insertByte(0x1B);
	insertModRM(des,src);
    }

    public void sbbl(Reg src,Ref des) {
	realloc();
	insertByte(0x19);
	insertModRM(src,des);
    }

    /**
       Integer Unsigned Multiplication of eax  (10 clk)
    */

    public void mull(Opr src) {
	realloc();
	insertByte(0xF7);
	insertModRM(4,src);
    }

    /**
       Integer Signed Multiplication (10 clk)
    */


    public void imull(Opr src,Reg des) {
	realloc();
	insertByte(0x0f); 
	insertByte(0xaf);
	insertModRM(des,src);
    }

    /* imull(Reg src, Ref des) no x86-code */

    public void imull(int immd,Reg des) {
	realloc();
	if (is8BitValue(immd)) {
	    insertByte(0x6b);
	    insertModRM(des,des);
	    insertByte(immd);
	} else {
	    insertByte(0x69);
	    insertModRM(des,des);
	    insertConst4(immd);
	}
    }

    public void imull(int immd,Opr src,Reg des) {
	realloc();
	if (is8BitValue(immd)) {
	    insertByte(0x6b);
	    insertModRM(des,src);
	    insertByte(immd);
	} else {
	    insertByte(0x69);
	    insertModRM(des,src);
	    insertConst4(immd);
	}
    }

    public void imull(SymbolTableEntryBase entry,Reg des) {
	realloc();
	insertByte(0x69);
	insertModRM(des,des);
	insertConst4(entry);
    }

    /**
       increment by 1 (1/3 clks)
    */

    public void incb(Opr opr) {
	realloc();
	insertByte(0xfe);
	insertModRM(0,opr);
    }

    /** 
       increment by 1 (1/3 clks)
    */

    public void incl(Ref ref) {
	realloc();
	insertByte(0xff);
	insertModRM(0,ref);
    }

    /**
       increment register by 1 (1 clks)
    */

    public void incl(Reg reg) {
	realloc();
	insertByte(0x40+reg.value);
    }


    /** 
	lea Load Effective Address (1 clk)
    */

    public void lea(Opr opr,Reg reg) {
	realloc();
	insertByte(0x8D);
	insertModRM(reg,opr);
    }

    /**
       SHL/SAL Shift left (1/3 clks)
    */

    public void shll(int immd, Opr des) {
	realloc();
	if (immd == 1) {
	    insertByte(0xd1);
	    insertModRM(4,des);
	} else {
	    insertByte(0xc1);
	    insertModRM(4,des);
	    insertByte(immd);
	}
    }

    /**
       SHL/SAL Shift left by %cl (4 clks)
    */

    public void shll(Opr des) {
	realloc();
	insertByte(0xd3);
	insertModRM(4,des);
    }


    /**
       SHLD Double Precision Shift left (4 clks)
    */

    public void shld(int immd,Reg low, Opr des) {
	realloc();	
	insertByte(0x0F);
	insertByte(0xA4);
	insertModRM(low,des);
	insertByte(immd);
    }

    /**
       SHLD Double Precision Shift left by %cl (4/5 clks)
    */

    public void shld(Reg low,Opr des) {
	realloc();
	insertByte(0x0F);
	insertByte(0xA5);
	insertModRM(low,des);
    }  

    /**
       SHR Shift right (1/3 clks)
    */

    public void shrl(int immd, Opr des) {
	realloc();
	if (immd == 1) {
	    insertByte(0xd1);
	    insertModRM(5,des);
	} else {
	    insertByte(0xc1);
	    insertModRM(5,des);
	    insertByte(immd);
	}
    }

    public void shrl(SymbolTableEntryBase entry, Opr des) {
	realloc();
	insertByte(0xc1);
	insertModRM(5,des);
	insertByte(entry);
    }

   /**
       SHL/SAL Shift left by %cl (4 clks)
    */

    public void shrl(Opr des) {
	realloc();
	insertByte(0xd3);
	insertModRM(5,des);
    }

    /**
       SAR Shift right (signed) (1/3 clks)
    */

    public void sarl(int immd, Opr des) {
	realloc();
	if (immd == 1) {
	    insertByte(0xd1);
	    insertModRM(7,des);
	} else {
	    insertByte(0xc1);
	    insertModRM(7,des);
	    insertByte(immd);
	}
    }

    /**
       SAR Shift right by %cl (signed) (4 clks)
    */

    public void sarl(Opr des) {
	realloc();
	insertByte(0xd3);
	insertModRM(7,des);
    }

    /**
       DIV Signed Divide
    */

    public void idivl(Opr src) {
	realloc();
	insertByte(0xf7);
	insertModRM(7,src);
    }

    /**
       DIV Unsigned Divide
    */

    public void divl(Opr src) {
	realloc();
	insertByte(0xf7);
	insertModRM(6,src);
    }

    /**
       Add
    */

    public void addl(Opr src,Reg des) {
	realloc();
	insertByte(0x03); 
	insertModRM(des,src);
    }

    public void addl(Reg src,Ref des) {
	realloc();
	insertByte(0x01);
	insertModRM(src,des);
    }
    
    public void addl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(immd==1)) {
	    insertByte(0x40+des.value);
	} else if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x05);
	    insertConst4(immd);
	} else if (is8BitValue(immd)) { 
	    insertByte(0x83);
	    insertModRM(0,des);
	    insertByte(immd);	    
	} else {
	    insertByte(0x81);
	    insertModRM(0,des);
	    insertConst4(immd);
	}
    }

    public void addl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x05);
	    insertConst4(entry);
	} else {
	    insertByte(0x81);
	    insertModRM(0,des);
	    insertConst4(entry);
	}
    }
 
    /**
       And (1/3 clks)
    */

    public void andl(Opr src,Reg des) {
	realloc();
	insertByte(0x23); 
	insertModRM(des,src);
    }

    public void andl(Reg src,Ref des) {
	realloc();
	insertByte(0x21);
	insertModRM(src,des);
    }
    
    public void andl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x25);
	    insertConst4(immd);
	} else {
	    insertByte(0x81);
	    insertModRM(4,des);
	    insertConst4(immd);
	}
    }

    public void andl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x25);
	    insertConst4(entry);
	} else {
	    insertByte(0x81);
	    insertModRM(4,des);
	    insertConst4(entry);
	}
    }

    /**
       Or (1/3 clks)
    */

    public void orl(Opr src,Reg des) {
	realloc();
	insertByte(0x0b); 
	insertModRM(des,src);
    }

    public void orl(Reg src,Ref des) {
	realloc();
	insertByte(0x09);
	insertModRM(src,des);
    }
    
    public void orl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x0d);
	    insertConst4(immd);
	} else {
	    insertByte(0x81);
	    insertModRM(1,des);
	    insertConst4(immd);
	}
    }

    public void orl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x0d);
	    insertConst4(entry);
	} else {
	    insertByte(0x81);
	    insertModRM(1,des);
	    insertConst4(entry);
	}
    }
    /**
       Or (1/3 clks)
    */

    public void xorl(Opr src,Reg des) {
	realloc();
	insertByte(0x33); 
	insertModRM(des,src);
    }

    public void xorl(Reg src,Ref des) {
	realloc();
	insertByte(0x31);
	insertModRM(src,des);
    }
    
    public void xorl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x35);
	    insertConst4(immd);
	} else {
	    insertByte(0x81);
	    insertModRM(6,des);
	    insertConst4(immd);
	}
    }

    public void xorl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0x35);
	    insertConst4(entry);
	} else {
	    insertByte(0x81);
	    insertModRM(6,des);
	    insertConst4(entry);
	}
    }

    /**
       Not (1/3 clks)
    */

    public void notl(Opr opr) {
	realloc();
	insertByte(0xf7);
	insertModRM(2,opr);
    }

    /**
       Neg (1/3 clks)
    */

    public void negl(Opr opr) {
	realloc();
	insertByte(0xf7);
	insertModRM(3,opr);
    }

    /**
       Add with Carry
    */

    public void adcl(Opr src,Reg des) {
	realloc();
	insertByte(0x13); 
	insertModRM(des,src);
    }

    public void adcl(Reg src,Ref des) {
	realloc();
	insertByte(0x11);
	insertModRM(src,des);
    }

    public void adcl(int immd, Opr des) {
	realloc();
	if (is8BitValue(immd) && immd>0) {
	    if ((des.tag==Opr.REG) && (des.value==0)) {
		insertByte(0x14);
		insertByte(immd);
	    } else {
		insertByte(0x80);
		insertModRM(2,des);
		insertByte(immd);
	    }
	} else {
	    if ((des.tag==Opr.REG) && (des.value==0)) {
		insertByte(0x15);
		insertConst4(immd);
	    } else if (is8BitValue(immd)) {
		insertByte(0x83);
		insertModRM(2,des);
		insertByte(immd);
	    } else {
		insertByte(0x81);
		insertModRM(2,des);
		insertConst4(immd);
	    }
	}
    }

    /**
       Compare Two Operands
    */

    public void cmpb(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG) && (des.value==0)) {
	    insertByte(0x3C);
	    insertByte(immd);
	} else {
	    insertByte(0x80);
	    insertModRM(7,des);
	    insertByte(immd);
	}
    } 

    public void cmpl(Opr src,Reg des) {
	realloc();
	insertByte(0x3B);
	insertModRM(des,src);
    }

    public void cmpl(Reg src,Ref des) {
	realloc();
	insertByte(0x39);
	insertModRM(src,des);
    }

    public void cmpl(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG) && (des.value==0)) {
	    insertByte(0x3D);
	    insertConst4(immd);
	} else if (is8BitValue(immd)) { /* FIXME */
	    insertByte(0x83);
	    insertModRM(7,des);
	    insertByte(immd);	    
	} else {
	    insertByte(0x81);
	    insertModRM(7,des);
	    insertConst4(immd);
	}
    }
    
    public void cmpl(SymbolTableEntryBase entry,Opr des) {
	realloc();
	if ((des.tag==Opr.REG) && (des.value==0)) {
	    insertByte(0x3D);
	    insertConst4(entry);
	} else {
	    insertByte(0x81);
	    insertModRM(7,des);
	    insertConst4(entry);
	}
    }

    /**
       Compare and Exchange (6 clks)
    */
    
    public void cmpxchg(Reg eax, Reg src, Opr des) {
	realloc();
	if (eax.value!=0) throw new Error("wrong Register");
	insertByte(0x0F);
	insertByte(0xB1);
	insertModRM(src, des);
    }

    /**

     */

    public void sete(Opr des) {
	realloc();
	insertByte(0x0f);
	insertByte(0x94);
	insertModRM(0,des);
    }

    public void setne(Opr des) {
	realloc();
	insertByte(0x0f);
	insertByte(0x95);
	insertModRM(0,des);
    }

    public void intr(int nr) {
	realloc();
	insertByte(0xCD);
	insertByte(nr);
    }


    /**
       Jump short/near if equal
    */

    public void je(int rel) {
	realloc();
	if (is8BitValue(rel)) {
	    insertByte(0x74);
	    insertByte(rel);
	} else {
	    insertByte(0x0F);
	    insertByte(0x84);
	    insertConst4(rel);
	}
    }

    public void je(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x84);
	insertConst4(entry);
	makeRelative(entry);
    }

    /**
       Jump short/near if not equal
    */

    public void jne(int rel) {
	realloc();
	if (is8BitValue(rel)) {
	    insertByte(0x75);
	    insertByte(rel);
	} else {
	    insertByte(0x0F);
	    insertByte(0x85);
	    insertConst4(rel);
	}
    }

    public void jne(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x85);
	insertConst4(entry);
	makeRelative(entry);
    }

    public void jnae(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x82);
	insertConst4(entry);
	makeRelative(entry);
    }

    /**
       Jump short/near if less
    */

    public void jl(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x8c);
	insertConst4(entry);
	makeRelative(entry);
    }   
    
    /**
       Jump short/near if greater or equal
    */

    public void jge(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x8d);
	insertConst4(entry);
	makeRelative(entry);
    }
    
    /**
       Jump short/near if greater
    */
    
    public void jg(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x8f);
	insertConst4(entry);
	makeRelative(entry);
    }   

    /**
       Jump short/near if less or equal
    */

    public void jle(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x8e);
	insertConst4(entry);
	makeRelative(entry);
    }

    /**
       Jump short/near if unsigned greater
    */

    public void ja(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x87);
	insertConst4(entry);
	makeRelative(entry);
    }  

    /**
       Jump short/near if unsigned greater or equal
    */

    public void jae(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0x0f);
	insertByte(0x83);
	insertConst4(entry);
	makeRelative(entry);
    }

   /**
       Jump short/near if sign
    */

    public void js(int rel) {
	realloc();
	if (is8BitValue(rel)) {
	    insertByte(0x78);
	    insertByte(rel);
	} else {
	    insertByte(0x0F);
	    insertByte(0x88);
	    insertConst4(rel);
	}
    }
  
    /**
       Jump short/near 
    */

    public void jmp(int rel) {
	realloc();
	if (is8BitValue(rel)) {
	    /* short */
	    insertByte(0xEB);
	    insertByte(rel);
	} else {
	    /* near */
	    insertByte(0xE9);
	    insertConst4(rel);
	}
    }

    public void jmp(Opr des) {
	realloc();
	insertByte(0xff);
	insertModRM(4,des);
    }

    public void jmp(SymbolTableEntryBase entry) {
	realloc();
	insertByte(0xE9);
	insertConst4(entry);
	makeRelative(entry);
    }

    public void jmp(Reg index,SymbolTableEntryBase[] table) {
	UnresolvedJump tableStart = new UnresolvedJump();
	realloc(50 + table.length*4);

	insertByte(0xff);
	insertByte(0x24);
	insertByte(0x85 | (index.value<<3));
	insertConst4(tableStart);

	addJumpTarget(tableStart);
	for (int i=0;i<table.length;i++) {
	    insertConst4(table[i]);
	}
    }

    /**
       Move 8 Bit Data
    */

    public void movb(Opr src,Reg des) {
	realloc();
	insertByte(0x8A);
	insertModRM(des,src);
    }

    public void movb(Reg src,Ref des) {
	realloc();
	insertByte(0x88);
	insertModRM(src,des);	
    }

    public void movb(int immd, Opr des) {
	realloc();
	if (des.tag==Opr.REG) {
	    insertByte(0xb0 + des.value);
	    insertByte(immd);
	} else {
	    insertByte(0xc6);
	    insertModRM(0,des);
	    insertByte(immd);
	}
    }
	    
    /** 
	Move 16 Bit Data
    */

    public void movw(Opr src,Reg des) {
	realloc();
	insertByte(0x66);
	insertByte(0x8b);
	insertModRM(des,src);	
    }

    public void movw(Reg src,Ref des) {
	realloc();
	insertByte(0x66);
	insertByte(0x89);
	insertModRM(src,des);	
    }

    /**
       Move 32 Bit Data
    */

    public void movl(Opr src,Reg des) {
	realloc();
	insertByte(0x8b);
	insertModRM(des,src);
    }

    public void movl(Reg src,Ref des) {
	realloc();
	insertByte(0x89);
	insertModRM(src,des);
    }

    public void movl(int immd, Opr des) {
	realloc();
	if (des.tag==Opr.REG) {
	    insertByte(0xb8 + des.value);
	    insertConst4(immd);
	} else {
	    insertByte(0xc7);
	    insertModRM(0,des);
	    insertConst4(immd);
	}
    }

    public void movl(SymbolTableEntryBase entry, Opr des) {
	realloc();
	if (des.tag==Opr.REG) {
	    insertByte(0xb8 + des.value);
	    insertConst4(entry);
	} else {
	    insertByte(0xc7);
	    insertModRM(0,des);
	    insertConst4(entry);
	}
    }

    /**
       Move with Zero-Extend (short) (3 clks)
    */
    public void movzwl(Opr src,Reg des) {
	realloc();
	insertByte(0x0f);
	insertByte(0xb7);
	insertModRM(des,src);
    }

    /**
       move with Zero-Extend (byte) (3 clks)
    */
    public void movzbl(Opr src,Reg des) {
	realloc();
	insertByte(0x0f);
	insertByte(0xb6);
	insertModRM(des,src);
    }

    /**
       Move with Sign-Extend (short -> register) (3 clks)
    */
    public void movswl(Opr src,Reg des) {
	realloc();
	insertByte(0x0f);
	insertByte(0xbf);
	insertModRM(des,src);
    }

    /**
       Move with Sign-Extend (byte -> register) (3 clks)
    */
    public void movsbl(Opr src,Reg des) {
	realloc();
	insertByte(0x0f);
	insertByte(0xbe);
	insertModRM(des,src);
    }  

    /**
       No Operation (1 clks)
    */

    public void nop() {
	realloc();
	insertByte(0x90);
    }

    /**
       Input from Port (7 clks)
    */

    public void inb(byte ib, Reg al) {
	realloc();
	if (al.value!=0) throw new Error("wrong Register");
	insertByte(0xE4);
	insertByte(ib);
    }

    public void inb(Reg dx, Reg al) {
	realloc();
	if (al.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0xEC);	
    }

    public void inl(byte ib, Reg eax) {
	realloc();
	if (eax.value!=0) throw new Error("wrong Register");
	insertByte(0xE5);
	insertByte(ib);
    }

    public void inl(Reg dx, Reg eax) {
	realloc();
	if (eax.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0xED);
    }

    public void inw(byte ib, Reg ax) {
	realloc();
	if (ax.value!=0) throw new Error("wrong Register");
	insertByte(0x66);
	insertByte(0xE5);
	insertByte(ib);
    }

    public void inw(Reg dx, Reg ax) {
	realloc();
	if (ax.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0x66);
	insertByte(0xED);
    }

    /**
       Output to Port (12 clks)
    */

    public void outb(Reg al, byte ib) {
	realloc();
	if (al.value!=0) throw new Error("wrong Register");
	insertByte(0xE6);
	insertByte(ib);
    }

    public void outb(Reg al, Reg dx) {
	realloc();
	if (al.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0xEE);
    }

    public void outw(Reg ax, byte ib) {
	realloc();
	if (ax.value!=0) throw new Error("wrong Register");
	insertByte(0x66);
	insertByte(0xE7);
	insertByte(ib);
    }

    public void outw(Reg ax, Reg dx) {
	realloc();
	if (ax.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0x66);
	insertByte(0xEF);
    }

    public void outl(Reg eax, byte ib) {
	realloc();
	if (eax.value!=0) throw new Error("wrong Register");
	insertByte(0xE7);
	insertByte(ib);
    }

    public void outl(Reg eax, Reg dx) {
	realloc();
	if (eax.value!=0) throw new Error("wrong Register");
	if (dx.value!=2) throw new Error("wrong Register");
	insertByte(0xEF);
    }

    /**
       write to model specific register (30-45 clks)

       ecx  | register
       =============================
       0x00 | machine check address
       0x01 | machine check type
       =============================
       0x10 | time stamp counter
       0x11 | control and event select
       0x12 | counter 0
       0x13 | counter 1

    */
    
    public void wrmsr() {
	realloc();
	insertByte(0x0f);
	insertByte(0x30);
    }

    /**
       read from model specific register (20-24 clks)

       see wrmsr() for register selection
     */

    public void rdmsr() {
	realloc();
	insertByte(0x0f);
	insertByte(0x32);
    }
	

    /**
       Read from Time Stamp Counter 
       return EDX:EAX
    */

    public void rdtsc() {
	realloc();
	insertByte(0x0f);
	insertByte(0x31);
    }

    /** 
       read performance monitor counter
       (only P6)
       
       ecx = 0 : return EDX:EAX counter0
       ecx = 1 : return EDX:EAX counter1

    */

    public void rdpmc() {
	realloc();
	insertByte(0x0f);
	insertByte(0x33);
    }



    /**
       test - logical compare (1/2 clks)
    */

    public void test(Opr src,Reg des) {
	realloc();
	insertByte(0x85); 
	insertModRM(des,src);
    }

    public void test(int immd,Opr des) {
	realloc();
	if ((des.tag==Opr.REG)&&(des.value==0)) {
	    insertByte(0xA9);
	    insertConst4(immd);
	} else {
	    insertByte(0xF7);
	    insertModRM(0,des);
	    insertConst4(immd);
	}
    }

    /** 
	Insert a single byte constant 
    */ 
    public void insertConst1(int value) {
	realloc();
	code[ip++] = (byte)value;
    }    
  
    /** 
	Insert a four byte constant 
    */ 
    public void insertConst4(int value) {
	realloc();
	code[ip++] = (byte)(value >> 0); 
	code[ip++] = (byte)(value >> 8); 
	code[ip++] = (byte)(value >> 16); 
	code[ip++] = (byte)(value >> 24);
    }

    private void insertConst4At(int ncIndex, int value) {
	code[ncIndex++] = (byte)(value >> 0); 
	code[ncIndex++] = (byte)(value >> 8); 
	code[ncIndex++] = (byte)(value >> 16); 
	code[ncIndex++] = (byte)(value >> 24);
    }

    /** 
	Insert a four byte constant with an unknown value. 
	(must be resolved before the code is installed) 
    */ 
    public void insertConst4(SymbolTableEntryBase entry) {
	realloc();
	entry.initNCIndex(ip, 4);  // size is always 4 bytes 
	symbolTable.addElement(entry); 
	ip+=4; 
    }
    
    // (immd>>8)==0
    public boolean is8BitValue(int value) {
	if (value<0) value=-value;
	return ((value>>7)==0);
    }

    /** 
	Insert a 0 byte constant with an unknown value. 
	(contains information about current code position, i.e., a stack map) 
    */ 
    public void insertConst0(SymbolTableEntryBase entry) {
	entry.initNCIndex(ip, 0);  // size is always 0 bytes 
	symbolTable.addElement(entry); 
    }


    /**
       Intel Architecture Optimization. Reference Manual (chapter 2,page 11)
       "Pentium II and III processors have a cache line size of 32 byte.
       Since the instruction prefetch buffers fetch 16-byte boundaries,
       code alignment has a direct impact on prefetch buffer efficiency"
       
       * Loop entry labels should be 16-byte-aligned when less then 
       eight byte away from a 16-byte boundary.
       
       * Labels that follow an unconditional branch of function call
       should be aligend as above.
       
       * Labels that follow a conditional branch need _not_ be aligned.
    */
    
    public void alignCode() {
	int drift = ip % 16;
	if (drift<8) {
	    for (int i=0; i<drift;i++) nop();
	}
    }
    
    /** 
	Initialized the target position of 'jumpObject'. 
	(Call insertConst4() for corresponding jump instruction) 
    */ 
    public void addJumpTarget(UnresolvedJump jumpObject) {
	if (doAlignJumpTargets) while ((ip%4)!=0) nop();
	jumpObject.setTargetNCIndex(ip); 
    }

    public void alignIP() {
	int distance = (ip%16);
	if (distance<8) alignIP_16_Byte();
    }

    public void alignIP_4_Byte() {
	while ((ip % 4)!=0) nop();
    }

    public void alignIP_16_Byte() {
	while ((ip % 16)!=0) nop();
    }

    public void alignIP_32_Byte() {
	while ((ip % 32)!=0) nop();
    }

    public void addExceptionTarget(UnresolvedJump handler) {
	realloc();
	//entry.initNCIndex(ip, 4);
	symbolTable.addElement(handler); 
	handler.setTargetNCIndex(ip);
    }

    /** 
	Make a symbol table entry relative. 
	If you use insertConst4(), this class assumes that 
	the value to be inserted is absolute. But if the 
	inserted value is a jump offset it is relative to 
	the instruction pointer of the next instruction. 
	That is what you can tell the compiler with this 
	method. 
    */ 
    public void makeRelative(SymbolTableEntryBase entry) {
	entry.makeRelative(ip); 
    }
    
    /**
       Called after each instruction. 
    */ 
    public void endInstr() {
	//if (DebugConf.doPrintBinaryCode) Debug.out.println(""); 
	//      currentInstruction.machinecode = new byte[numBytesMachinecode];
	//      System.arraycopy(currentMachinecode, 0, currentInstruction.machinecode, 0, numBytesMachinecode);
	//      numBytesMachinecode = 0;
    }

    // ***** Management stuff ***** 
    
    public void finishCode() {
    }
    
    /** 
	Apply all resolveable symbol table entries.
	(e.g. insert jump offsets ....)
	After calling this method, the vector 'symbolTable' 
	contains all symbol table entries that are not resolveable.
	If you want to install the compiled code after calling this 
	method, this vector should be empty. 
    */ 
    public void resolve(int codeBase) {
	Enumeration enum = symbolTable.elements(); 
	Vector unresolvedEntries = new Vector(); 
	while(enum.hasMoreElements()) {
	    SymbolTableEntryBase entry = (SymbolTableEntryBase)enum.nextElement(); 
	    if (entry.isReadyForApply()) 
		entry.apply(code, codeBase);
	    else 
		unresolvedEntries.addElement(entry); 
	}
	symbolTable = unresolvedEntries; 
    }

    // ***** Building of Debug messages ****** 
    
    String constToString(int value) {
	return String.valueOf(value);
    }

    String constToString(SymbolTableEntryBase entry) {
	return entry.getDescription(); 
    }
    
    String const1ToString(int value) {
	return String.valueOf(value);
    }
    
    private static final String[] REGNAME = {
	"ax", "cx", "dx", "bx", "sp", "bp", "si", "di"
    }; 
    
    public static String regToString(int reg) {
	return "%e" + REGNAME[reg]; 
    }
    
    public static final int EAX = 0; 
    public static final int ECX = 1; 
    public static final int EDX = 2; 
    public static final int EBX = 3; 
    public static final int ESP = 4; 
    public static final int EBP = 5; 
    public static final int ESI = 6; 
    public static final int EDI = 7; 
    
    // ***** Exceptions ***** 
    /*
    public void addExceptionRangeStart(NCExceptionHandler handler) {
	handler.setRangeStart(ip); 
    }

    public void addExceptionRangeEnd(NCExceptionHandler handler) {
	handler.setRangeEnd(ip); 
    }
    */
    /** 
	add a start of an exception handler. 
    */
    /* 
    public void addExceptionHandler(NCExceptionHandler handler) {
	handler.setHandlerStart(ip); 
	exceptionHandlers.addElement(handler); 
    }
    */
    
    /** 
	return an array of all exception handlers of this 
	method. (these handlers contain the native code indices 
	of the range start, range end and of the handler start 
    */ 
    public NCExceptionHandler[] getExceptionHandlers() {
	NCExceptionHandler[] handlerArray = 
	new NCExceptionHandler[exceptionHandlers.size()]; 
	for(int i=0;i<exceptionHandlers.size();i++) {
	    handlerArray[i] = (NCExceptionHandler)exceptionHandlers.elementAt(i); 
	    Debug.assert(handlerArray[i].isFinished()); 
	}
	return handlerArray; 
    }

    // ***** Printing ***** 
    
    public String getBinaryCodeAsHex(int firstByte, int stopByte) {
	String s = ""; 
	for(int i=firstByte; i<stopByte; i++) {
	    String hex = Integer.toHexString(code[i] & 0xff); 
	    if (hex.length()==1) hex = "0" + hex; 
	    s = s + hex  + " "; 
	}
	return s; 
    }
    
    // returns a hexdump of the compiled function 
    public String getBinaryCodeAsHex() {
	return getBinaryCodeAsHex(0, ip); 
    }
    
    private String getBinaryCodeAsAssembler(int firstByte, int stopByte) {
	String s = ""; 
	for(int i=firstByte; i<stopByte; i++) {
	  String hex = Integer.toHexString(code[i] & 0xff); 
	  if (hex.length()==1) hex = "0" + hex; 
	  s = s + hex  + " "; 
	}
	return s; 
    }
    
    // returns a hexdump of the compiled function 
    public String getBinaryCodeAsAssembler() {
	return getBinaryCodeAsAssembler(0, ip); 
    }
    
    /** 
	excpected maximal length of a printed assembler instruction 
    */ 
    private final static int STRLEN = 30; 
    
    public void printInstr(String instr, String arg1, SymbolTableEntryBase arg2) {
	//      currentInstruction = new DisassInstr(ip, instr, arg1, arg2);
      //      instructions.addElement(currentInstruction);
    }

    public void printInstr(String instr, SymbolTableEntryBase arg1, String arg2) {
	//      currentInstruction = new DisassInstr(ip, instr, arg1, arg2);
	//      instructions.addElement(currentInstruction);
    }
    
    public void printInstr(String instr, String arg1, SymbolTableEntryBase arg2, String arg3) {
	//      currentInstruction = new DisassInstr(ip, instr, arg1, arg2, arg3);
	//      instructions.addElement(currentInstruction);
    }

    public void printInstr(String instr) {
	//      currentInstruction = new DisassInstr(ip, instr);
	//      instructions.addElement(currentInstruction);
    }
    
    public void printInstr(String instr, String arg1, String arg2) {
	//      currentInstruction = new DisassInstr(ip, instr, arg1, arg2);
	//      instructions.addElement(currentInstruction);
    }
    
    public void printInstr(String instr, SymbolTableEntryBase arg1) {
      //      currentInstruction = new DisassInstr(ip, instr, arg1);
	//      instructions.addElement(currentInstruction);
    }
    public void printJumpTarget(UnresolvedJump entry) {
	//      currentInstruction = new DisassInstr(ip, entry);
	//      instructions.addElement(currentInstruction);
      endInstr();
    }
    
    public void printHexByte(int value) {
	String hex = Integer.toHexString(value & 0xff); 
	if (hex.length()==1) hex = "0" + hex; 
	Debug.out.print(hex + " "); 
    }    
    
    public void printHexInt(int value) {
	String hex = Long.toHexString(value & 0xffffffffL);         
	Debug.out.print( "00000000".substring(Math.min(hex.length(),8)) + hex + " "); 
    }
    

    public void printInstructions() {
	//	for(int i=0; i<instructions.size(); i++) {
	//	    Debug.out.println(instructions.elementAt(i));
	//	}
    }
    
    public void printGASInstructions(PrintStream out) {
	//	for(int i=0; i<instructions.size(); i++) {
	//	    out.println(((DisassInstr)instructions.elementAt(i)).toGASFormat());
	//	}
    }
    
    public void startBC(int bcPosition) {
	bcIndex = bcPosition;
	startIP = ip;
    }
    
    public void endBC() {
	instructionTable.addElement(new int[] { bcIndex, startIP, ip});	
    }
    
    public Vector getInstructionTable() {
	return instructionTable;
    }

}
  
