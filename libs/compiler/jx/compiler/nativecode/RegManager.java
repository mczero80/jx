package jx.compiler.nativecode;

import jx.compiler.*;
import jx.compiler.symbols.*;
import jx.compiler.execenv.*;
import jx.compiler.imcode.*;
import jx.classfile.datatypes.*;

/**
 * 
 */

public class RegManager {

    private CodeContainer    container;
    private BinaryCodeIA32   code;
    private String           method_name;
    private MethodStackFrame frame;

    private boolean dbg_msg         = false;
    private boolean dbg_free        = false;
    private boolean stat_flag       = true;
    private boolean optimize_read   = false;
    private boolean clear_temps     = true;
    private boolean paranoid_checks = false;
    private boolean clear_stack     = true;

    private int     readsIntoAny;
    private int     foundInReg;
    private int     numberOfReads;
    private int     numberOfMoveIn;
    private int     numberOfWrites;
    private int     numberOfSwaps;

    // all known int register
    private Reg[]           regList;
    // all register objects active in register
    private RegList         active;

    private int uniID=0;

    public RegManager() {
	this.regList  = new Reg[6];
	this.active   = new RegList(); 
	uniID=0;
    }

    public void init(CodeContainer container) {
	this.regList      = new Reg[6];
	regList[0]        = Reg.eax;
	regList[1]        = Reg.ecx;
	regList[2]        = Reg.edx;
	regList[3]        = Reg.ebx;
	regList[4]        = Reg.esi;
	regList[5]        = Reg.edi;
	this.container = container;
	this.code = container.getIA32Code();
	this.frame = container.getMethodStackFrame();
	this.method_name = container.getBCMethod().getName();
	numberOfReads = 0;
	numberOfMoveIn = 0;
	numberOfWrites = 0;
	numberOfSwaps = 0;
	readsIntoAny = 0;
	foundInReg = 0;	
    }

    /**
       chooseAnyIntRegister(any)

       Waehlt ein physikalisches Register fuer den Platzhalter "any" aus.
    */

    public void chooseAnyIntRegister(Reg any) throws CompileException {
	Reg choose=chooseIntRegister(null,null);	
	any.value=choose.value;
    }

    /**
       Reg chooseIntRegisterForSlot

       Waehlt ein physikalisches Register.
    */

    public Reg chooseIntRegisterForSlot(LocalVariable slot,Reg blocked) throws CompileException {
	Reg choose = chooseIntRegister(blocked);
	choose.slot=slot;
	return choose;
    }

    /**
       Reg chooseIntRegister

       Waehlt ein physikalisches Register fuer ein virtuelles Register aus.
    */    

    public Reg chooseIntRegister() throws CompileException {
	return chooseIntRegister(null,null);
    }

    /**
       Reg chooseIntRegister

       Waehlt ein physikalisches Register fuer ein virtuelles Register aus, mit
       der Einschraenke nicht das Register vom Argumenten zu nehmen.
    */    

    public Reg chooseIntRegister(Reg blocked) throws CompileException {
	if (dbg_msg && blocked!=null && blocked.any()) {
	    System.err.println("     !! can`t block any");
	}
	return chooseIntRegister(blocked,null);
    }  

    /**
       Reg chooseIntRegister

       Waehlt ein physikalisches Register fuer ein virtuelles Register aus, mit
       der Einschraenke nicht die Register der Argumente zu nehmen.
    */ 

    public Reg chooseIntRegister(Reg blocked1,Reg blocked2) throws CompileException {
	return chooseIntRegister(blocked1,blocked2,null);
    }

    public Reg chooseIntRegister(Reg blocked1,Reg blocked2, Reg blocked3) throws CompileException {
	Reg choose =null;
	int badness=1000;
	
	for (int i=0;(i<6 && badness!=0);i++) {
	    Reg reg = regList[i];
	    
	    // don`t choose blocked register at all
	    if (blocked1!=null && blocked1.conflict(reg)) continue;
	    if (blocked2!=null && blocked2.conflict(reg)) continue;
	    if (blocked3!=null && blocked3.conflict(reg)) continue;
	    
	    int value = computeBadness(reg,6);
	    
	    if (choose==null || value < badness) {
		choose   = reg;		    
		badness  = value;
	    }
	    
	}
	
	choose    = choose.getClone();
	choose.id = uniID++;
	
	if (dbg_msg) {	    
	    System.err.println("     choose "+choose);
	    //Thread.dumpStack();
	}
	return choose;
    }

    public Reg64 chooseLongRegister() throws CompileException {
	return new Reg64(chooseIntRegister());
    }

    public Reg64 chooseLongRegister(Reg64 blocked) throws CompileException {
	return new Reg64(chooseIntRegister(blocked.low,blocked.high));
    }


    /**
       Reg chooseAndAllocIntRegister

       Waehlt ein physikalisches Register fuer ein virtuelles Register aus, mit
       der Einschraenke nicht die Register der Argumente zu nehmen.
    */ 

    public Reg chooseAndAllocIntRegister(Reg blocked,int datatype) throws CompileException {
	Reg choose = chooseIntRegister(blocked,null);
	allocIntRegister(choose,datatype);
	return choose;
    }

    /**
       getIntRegister

       Gibt ein zu einem festgelegten physikalischen Register passendes virtuelles Register
       zurueck.
    */

    public Reg getIntRegister(Reg reg) {
	Reg choose = reg.getClone();
	choose.id = uniID++;
	if (dbg_msg) {
	    System.err.println("     choose "+reg+" (get)");
	    //Thread.dumpStack();
	}
	return choose;
    }

    /**
       getLongRegister

       Gibt ein zu einem festgelegten physikalischen Register passendes virtuelles Register
       zurueck.
    */

    public Reg64 getLongRegister(Reg64 reg) {
	Reg64 nreg = reg.getDeepClone();
	return nreg;
    }    

    /**
       Legt ein physikalisches Register fuer den Platzhalter "any" fest.
    */

    public void setAnyIntRegister(Reg any,Reg reg) {
	if (!any.any()) throw new Error("can`t rename register");
	any.value=reg.value;
    }

    /**
       Wird am Anfang eines Basisblockes aufgerufen.
    */

    public void startBasicBlock() {
	active.clear();
    }

    /**
       Wird am Ende eines jeden Basisblockes aufgerufen.
    */

    public void endBasicBlock() {
	if (dbg_msg) 
	    System.err.println(method_name+": end of basic block active: "+active);

	if (clear_temps) { frame.freeAllTemps(); }
	
	clearActives();
    }

    /**
       Dient zur Abfrage ob noch freie Register zur Verfuehgung stehen.
    */

    public boolean hasFreeIntRegister() {
	int used = 0;	
	for (Reg reg=active.top();reg!=null;reg=active.next()) {
	    if (reg.slot==null && !reg.free) used++;
	}
	return (used<6);
    }

    /**
       allocIntRegister(Reg reg,int datatype) 

       Belegt das physikalische Register fuer ein virtuelles Register (reg). Ist das
       physikalische Register bereits Belegt wird der Inhalt des belegenden Registers 
       auf dem Stack gesichert. Handelt es sich beim virtuellen Register um den 
       Platzhalter "any" so wird automatisch ein Register ausgewaehlt.

    */
       

    public void allocIntRegister(Reg reg,int datatype) throws CompileException {
	if (reg.any()) chooseAnyIntRegister(reg);
	
	if (dbg_msg) System.err.println("     alloc "+reg);
	
	if (reg.free) throw new CompileException("alloc freed register ?!?");
	
	reg.setDatatype(datatype);
	
	swapEqualIntRegister(reg);
	if (active.knows(reg)) {
	    if (dbg_msg) System.err.println("2nd alloc "+reg);
	} else {
	    active.append(reg);
	}
	modifyIntRegister(reg);
    }

    /** 
	allocIntRegister(Reg reg,Reg prefer,int datatype)

	Verhaelt sich wie allocIntRegister(Reg reg,int datatype) mit dem Unterschied, dass fuer
	den Platzhalter "any" ein bevorzugtes Register angegeben werden kann.
    */

    public void allocIntRegister(Reg reg,Reg prefer,int datatype) throws CompileException {
	if (reg.any()) setAnyIntRegister(reg,prefer);
	allocIntRegister(reg,datatype);
    }

    /** 
	allocLongRegister(Reg64 reg, Reg64 prefer)

	Verhaelt sich wie allocLongRegister(Reg reg) mit dem Unterschied, dass fuer
	den Platzhalter "any" ein bevorzugtes Register angegeben werden kann.
    */

    public void allocLongRegister(Reg64 reg,Reg64 prefer) throws CompileException {
	//if (reg.equals(Reg64.any())) setAnyLongRegister(reg,prefer);
	allocLongRegister(reg);
    } 

    /**
       allocLongRegister(Reg64 reg) 

       Belegt das physikalische Register fuer ein virtuelles Register (reg). Ist das
       physikalische Register bereits Belegt wird der Inhalt des belegenden Registers 
       auf dem Stack gesichert. Handelt es sich beim virtuellen Register um den 
       Platzhalter "any" so wird automatisch ein Register ausgewaehlt.

    */
       

    public void allocLongRegister(Reg64 reg) throws CompileException {
	if (reg.equals(Reg64.any)) {
	    throw new CompileException(" 64 Bit any not implemented");
	}
	allocIntRegister(reg.low,BCBasicDatatype.INT);
	allocIntRegister(reg.high,BCBasicDatatype.INT);
    }

    /** 
	readIntRegister(Reg reg)

	Muss immer aufgerufen werden bevor auf den Inhalt eines virtuellen Registers
	lesend Zugegriffen werden soll. Die Methode stellt sicher das verdraengte 
	Register wieder vom Stack in das Register geladen werden.
    */    

    public void readIntRegister(Reg reg) throws CompileException {
	if (reg.any()) throw new Error("can`t read from register 'any'");
	//if (dbg_msg) System.err.println("     read "+reg+" ["+active+"]");
	if (!active.knows(reg)) {
	    if (reg.slot!=null) {
		active.append(reg);
		readIntRegisterFromSlot(reg.slot,reg,reg.slot.getDatatype());
	    } else {
		if (dbg_msg) System.err.println(active);
		throw new CompileException("unknown register "+reg);
	    }
	}
	reg.unfree();
	//reg.free=false;
    }

    public boolean known(Reg reg) {
	if (active.knows(reg)) return true;
	if (reg.slot!=null) return true;
	return false;
    }

   /** 
	readLongRegister(Reg reg)

	Muss immer aufgerufen werden bevor auf den Inhalt eines virtuellen Registers
	lesend Zugegriffen werden soll. Die Methode stellt sicher das verdraengte 
	Register wieder vom Stack in das Register geladen werden.
    */

    public void readLongRegister(Reg64 reg) throws CompileException {
	readIntRegister(reg.low);
	readIntRegister(reg.high);
    }

    /**
       writeIntRegiter(Reg reg)

       Muss immer vor einem Schreibzugriff auf ein virtuelles Register aufgerufen werden.
       Die Methode sorgt dafuer, dass der Inhalt bei verdraengten Registern wieder vom 
       Stack gelesen wird und kennzeichnet das Register als veraendert.

    */

    public void writeIntRegister(Reg reg) throws CompileException {
	if (reg.any()) throw new Error("can`t write to register 'any'");
	if (dbg_msg) System.err.println("     write "+reg);
	if (reg.free) throw new CompileException("write freed register");
	readIntRegister(reg);
	modifyIntRegister(reg);
    }

    /**
       writeLongRegiter(Reg64 reg)

       Muss immer vor einem Schreibzugriff auf ein virtuelles Register aufgerufen werden.
       Die Methode sorgt dafuer, dass der Inhalt bei verdraengten Registern wieder vom 
       Stack gelesen wird und kennzeichnet das Register als veraendert.

    */

    public void writeLongRegister(Reg64 reg) throws CompileException {
	writeIntRegister(reg.low);
	writeIntRegister(reg.high);
    }


    /**
       freeIntRegister

       Kennzeichnet die von einem virtuellen Register belegten Resourcen als freigegeben.
    */

    public void freeIntRegister(Reg reg) throws CompileException {
	if (dbg_msg && dbg_free && !reg.any()) System.err.println("     free "+reg);

	reg.free();

	if (!reg.valid) active.remove(reg);

	if (reg.slot!=null) {
	    if (clear_stack && reg.slot.isType(LocalVariable.TMPS)) {
		code.movl(0,Ref.ebp.disp(frame.getOffset(reg.slot)));
	    }
	    frame.freeSlot(reg.slot);
	    reg.slot=null;
	}
    }

    /**
       freeLongRegister

       Kennzeichnet die von einem virtuellen Register belegten Resourcen als freigegeben.
    */

    public void freeLongRegister(Reg64 reg) throws CompileException {
	if (reg.low!=null) freeIntRegister(reg.low);
	if (reg.high!=null) freeIntRegister(reg.high);
    }




    /**
       readLongRegisterFromSlot(slot,reg,datatype)
       Liest den Inhalt von einer Stackposition in ein Register
    */

    public void readLongRegisterFromSlot(LocalVariable slot,Reg64 reg) throws CompileException {
	if (stat_flag) numberOfReads+=2;

	if (dbg_msg) System.err.println("     read "+slot.addrString()+"->"+reg);

	allocLongRegister(reg);

	if (stat_flag) numberOfMoveIn+=2;

	int offset = frame.getOffset(slot);
	if (offset==0) throw new Error("zero offset");

	code.movl(Ref.ebp.disp(offset),reg.low);
	code.movl(Ref.ebp.disp(offset-4),reg.high);

	//reg.low.slot  = slot;
	//reg.high.slot = slot;
	reg.low.slot  = null;
	reg.high.slot = null;
	//reg.low.free  = false;
	//reg.high.free = false;
	//reg.low.valid  = true;
	//reg.high.valid = true;
    }

    /**
       readIntRegisterFromSlot(slot,reg,datatype)

       Liest den Inhalt von einer Stackposition in ein Register

    */

    public void readIntRegisterFromSlot(LocalVariable slot,Reg reg,int datatype) throws CompileException {
	if (stat_flag) numberOfReads++;

	Reg inReg = findSlotInReg(slot);

	if (stat_flag && reg.any()) readsIntoAny++;
	if (stat_flag && inReg!=null) foundInReg++;

	if (optimize_read && inReg!=null) {
	    if (reg.any()) setAnyIntRegister(reg,inReg);
	    if (reg.equals(inReg)) {
		if (dbg_msg) System.err.println("     reread "+slot.addrString()+"->"+reg);
		if (paranoid_checks) {
		    int offset = frame.getOffset(slot);
		    //if (offset==0) throw new Error("zero offset");
		    code.cmpl(Ref.ebp.disp(offset),reg);
		    code.jne(container.getExecEnv().createExceptionCall(-8,code.getCurrentIP()));
		}
		reg.unfree();
		//reg.free=false;
		if (datatype==-1) {
		    allocIntRegister(reg,slot.getDatatype());
		} else {
		    allocIntRegister(reg,datatype);
		}
		reg.slot=slot;
		reg.valid=true;
		return;
	    }
	} 

	if (dbg_msg) System.err.println("     read "+slot.addrString()+"->"+reg);

	//if (!active.knows(reg)) {
	//if (dbg_msg) System.err.println("    !!! auto alloc "+reg+" !!!");
	if (datatype==-1) {
	    allocIntRegister(reg,slot.getDatatype());
	} else {
	    allocIntRegister(reg,datatype);
	}
	//}

	if (stat_flag) numberOfMoveIn++;
	int offset = frame.getOffset(slot);
	if (offset==0) throw new Error("zero offset");
	code.movl(Ref.ebp.disp(offset),reg);
	if (!slot.isType(LocalVariable.TMPS)) reg.slot  = slot;
	//reg.free  = false;
	reg.unfree();
	reg.valid = true;
    }

    /**
       writeIntToSlot(value,slot)

       Veraendert den Wert einer Stackposition.
    */

    public void writeIntToSlot(int value, LocalVariable slot) throws CompileException {
	//if (stat_flag) numberOfWrites++;

	if (optimize_read) {
	    Reg inReg = findSlotInReg(slot);
	    if (inReg!=null) inReg.valid=false;
	}

	int offset = frame.getOffset(slot);
	//if (offset==0) throw new Error("zero offset");
	slot.modify();
	code.movl(value,Ref.ebp.disp(offset));	
    }

    /**
       writeIntRegisterToSlot

       Schreibt den Wert eines Registers auf den Stack.
    */

    public void writeIntRegisterToSlot(Reg reg,LocalVariable slot) throws CompileException {
	if (stat_flag) numberOfWrites++;
	if (reg.any()) throw new Error("can`t write to register 'any'");
	if (reg.free) throw new CompileException("write out freed register");

	if (optimize_read) {
	    Reg inReg = findSlotInReg(slot);
	    if (inReg!=null) inReg.valid=false;
	}	
        
	int offset = frame.getOffset(slot);
	if (dbg_msg) System.err.println("     write "+reg+"->"+slot.addrString());
	//if (offset==0) throw new Error("zero offset");
	code.movl(reg,Ref.ebp.disp(offset));

	slot.modify();
	reg.slot = slot;
    }

 
    /**
       writeLongRegisterToSlot

       Schreibt den Wert eines Registers auf den Stack.
    */

    public void writeLongRegisterToSlot(Reg64 reg,LocalVariable slot) throws CompileException {
	if (stat_flag) numberOfWrites++;
	//if (reg.any()) throw new Error("can`t write to register 'any'");
	//if (reg.low.free || reg.high.free) throw new CompileException("write out freed register");
	if (reg.low.isFree() || reg.high.isFree()) throw new CompileException("write out freed register");

	if (optimize_read) {
	    Reg inReg = findSlotInReg(slot);
	    if (inReg!=null) inReg.valid=false;
	}	
        
	int offset = frame.getOffset(slot);
	if (dbg_msg) System.err.println("     write "+reg+"->"+slot.addrString());

	code.movl(reg.low,Ref.ebp.disp(offset));
	code.movl(reg.high,Ref.ebp.disp(offset+4));
	slot.modify();

	reg.low.slot = slot;
	reg.high.slot = slot;
    }

    /**
       saveIntRegister

       Sichert alle benutzten Register auf dem Stack. Dieses ist zum Beispiel vor jedem
       Methodenaufruf noetig.       
    */

    public void saveIntRegister() throws CompileException {
	if (dbg_msg) System.err.println("     save "+active);
	Reg reg = active.top();
	while (reg!=null) {
	    //if (dbg_msg) System.err.println("       rm "+reg);
	    if (reg.slot==null && !reg.free) {		
		reg.slot=frame.getFreeTempSlot(reg.getDatatype());
		writeIntRegisterToSlot(reg,reg.slot);
		reg.valid = false;
	    }
	    active.remove();
	    reg = active.top();
	}
    }

    /**
       saveOtherIntRegister

       Sichert alle benutzten Register auf dem Stack. Dieses ist zum Beispiel vor jedem
       Methodenaufruf noetig.       
    */

    public void saveOtherIntRegister(Reg doNotSave) throws CompileException {
	if (dbg_msg) System.err.println("     save "+active);
	Reg reg = active.top();
	while (reg!=null) {
	    //if (reg.slot==null && !reg.free && !reg.equals(doNotSave)) {		
	    if (reg.slot==null && !reg.isFree() && !reg.equals(doNotSave)) {
		reg.slot=frame.getFreeTempSlot(reg.getDatatype());
		writeIntRegisterToSlot(reg,reg.slot);
		reg.valid = false;
	    }
	    active.remove();
	    reg = active.top();
	}
    }

    /**
       Liefert statistische Information zurueck
    */

    public String getStatistics() {
	if (stat_flag) {
	    String out = "rd " + numberOfMoveIn + "/" + numberOfReads +
		" wr " + numberOfWrites +
		" swaps " + numberOfSwaps +
		" any " + readsIntoAny +
		" found " + foundInReg;
	    return out;
	} else {
	    return null;
	}
    }

    public String getActives() {
	return active.toString();
    }

    /*
      helper
    */

    public void clearActives() {
	for (Reg reg=active.top();reg!=null;reg=active.top()) {
	    modifyIntRegister(reg);
	    active.remove();
	}	
    }

    private void modifyIntRegister(Reg reg) {
	frame.freeSlot(reg.slot);
	reg.slot=null;
    }

    private int computeBadness(Reg choose,int depth) {
	int value=0;

	if (choose.equals(Reg.eax)) value++;
	if (choose.equals(Reg.ecx)) value++;

	int i=1;
	for (Reg reg=active.top();(reg!=null && i<=depth);reg=active.next()) {
	    //if (dbg_msg) System.err.println(reg+"?="+choose);
	    if (reg.equals(choose)) {
		//if (dbg_msg) System.err.println(reg+"=="+choose);
		// use the newer regs less
		value+=i;
		// some regs are already freed
		if (!reg.free) value+=10;
		break;
	    }
	    i++;
	}

	return value;
    }

    private Reg findSlotInReg(LocalVariable slot) {
	for (Reg reg=active.top();reg!=null;reg=active.next()) {
	    if (reg.valid && reg.slot==slot) return reg;
	}
	return null;
    }

    private void swapEqualIntRegister(Reg nReg) throws CompileException {
	Reg reg=active.top();
	while (reg!=null) {
	    if (reg.equals(nReg) && !(reg.id==nReg.id)) {
		reg.valid = false;
		if (reg.slot==null && !reg.free) {
		    if (dbg_msg) System.err.println("     swap "+reg);
		    if (stat_flag) numberOfSwaps++;
		    reg.slot=frame.getFreeTempSlot(BCBasicDatatype.INT);
		    writeIntRegisterToSlot(reg,reg.slot);
		    reg=active.remove();
		    
		} else {
		    if (dbg_msg) System.err.println("     del "+reg);
		    reg=active.remove();
		}
	    } else {
		//System.err.println(active);
		reg=active.next();
	    }
	}
    }
}
