package jx.compiler.persistent;

import jx.zero.Debug;

import java.io.*;  

import jx.compiler.symbols.*;
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;

public class VMSupportSTEntry extends SymbolTableEntryBase {

	public static final String VM_CHECKCAST       = "vm_checkcast";
	public static final String VM_INSTANCEOF      = "vm_instanceof";
	public static final String VM_GETSTATICS_ADDR = "vm_getStaticsAddr";
	public static final String VM_GETSTATICS_ADDR2= "vm_getStaticsAddr2";
	public static final String VM_ISPRIMITIVE     = "vm_isprimitive";
	public static final String VM_GETINSTANCESIZE = "vm_getinstancesize";
	public static final String VM_BREAKPOINT      = "vm_breakpoint";
	public static final String VM_MONITORENTER    = "vm_monitorenter";
	public static final String VM_MONITOREXIT     = "vm_monitorexit";
	public static final String VM_GETCLASSNAME    = "vm_getclassname";
	public static final String VM_SPINLOCK        = "vm_spinlock";
	public static final String VM_ARRAYCOPY_RIGHT = "vm_arraycopy_right";
	public static final String VM_ARRAYCOPY_LEFT  = "vm_arraycopy_left";
	public static final String VM_GETNAMING       = "vm_getnaming";
	public static final String VM_CINIT           = "vm_test_cinit";

	public static final String VM_PUTFIELD32  = "vm_put_field32";
	public static final String VM_PUTSFIELD32 = "vm_put_static_field32";
	public static final String VM_PUTAFIELD32 = "vm_put_array_field32";

	public static final String VM_MAP_GET32 = "vm_map_get32";
	public static final String VM_MAP_PUT32 = "vm_map_put32";

	private static VMSupportSTEntry name_table=null;
	private static int name_table_size=0;
	private VMSupportSTEntry next=null;

	private String name;
	private int operation;

	public VMSupportSTEntry() {}

	public VMSupportSTEntry(String name) {
		this.name = name;
		if (name_table==null) {
			operation  = 1;
			name_table = this;
			name_table_size++;
		} else if (name_table.name.equals(name)) {
			operation = 1;
		} else {
			VMSupportSTEntry curr = name_table;
			int i=2;
			while (curr.next!=null) {
				if (curr.next.name.equals(name)) {
					operation = i; 
					return;
				}
				curr=curr.next;
				i++;
			}
			operation = i;
			curr.next = this;
			name_table_size++;
		}
	}

	public String getDescription() {
		return super.getDescription()+",VMSupport";
	}

	public void apply(byte[] code, int codeBase) {
		Debug.assert(isReadyForApply()); 
		myApplyValue(code, codeBase, getValue()); 
	}

	public String toGASFormat() {
		return "0x"+Integer.toHexString(getValue());
	}

	public static void writeSymTable(ExtendedDataOutputStream out) throws IOException {
		out.writeInt(name_table_size+1);
		out.writeString("vm_unsupported");
		VMSupportSTEntry curr = name_table;
		int i=1;
		while (curr!=null) {
			out.writeString(curr.name);
			curr=curr.next;
			i++;
		}
	}

	public void writeEntry(ExtendedDataOutputStream out) throws IOException {
		super.writeEntry(out);
		out.writeInt(operation);
	}

	public void readEntry(ExtendedDataInputStream in) throws IOException {
		super.readEntry(in);
		operation = in.readInt();
	}
}
  
  
