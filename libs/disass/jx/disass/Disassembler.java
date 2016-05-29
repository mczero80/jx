/*
 * Based on the Bochs disassembler
 */
package jx.disass;

import jx.zero.Debug;
import jx.zero.debug.*;
import jx.compiler.LineInfo;

public class Disassembler {
    static String[] sreg_mod01_rm32   = { "DS", "DS", "DS", "DS", "??", "SS", "DS", "DS"};
    static String[] sreg_mod10_rm32   = { "DS", "DS", "DS", "DS", "??", "SS", "DS", "DS"};
    static String[] sreg_mod00_base32 = { "DS", "DS", "DS", "DS", "SS", "DS", "DS", "DS"};
    static String[] sreg_mod01_base32 = { "DS", "DS", "DS", "DS", "SS", "SS", "DS", "DS"};
    static String[] sreg_mod10_base32 = { "DS", "DS", "DS", "DS", "SS", "SS", "DS", "DS"};
    static String[] sreg_mod00_rm16   = { "DS", "SS", "SS", "DS", "DS", "DS", "DS"};
    static String[] sreg_mod01_rm16   = { "DS", "DS", "SS", "SS", "DS", "DS", "DS", "DS"};
    static String[] sreg_mod10_rm16   = { "DS", "DS", "SS", "SS", "DS", "DS", "SS", "DS"};
    static String[] segment_name = {"ES", "CS", "SS", "DS", "FS", "GS", "??", "??"};
    static String[] general_8bit_reg_name = { "AL", "CL", "DL", "BL", "AH", "CH", "DH", "BH"};
    static String[] general_16bit_reg_name = { "AX", "CX", "DX", "BX", "SP", "BP", "SI", "DI"};
    static String[] general_32bit_reg_name = {"EAX", "ECX", "EDX", "EBX", "ESP", "EBP", "ESI", "EDI"};
    static String[] base_name16 =  {"BX", "BX", "BP", "BP", "??", "??", "BP", "BX"};
    static String[] index_name16 = {"SI", "DI", "SI", "DI", "SI", "DI", "??", "??"};
    static String[] index_name32 =  {"EAX", "ECX", "EDX", "EBX", "???", "EBP", "ESI", "EDI"};

    static final int  BX_SEGMENT_REG   =    10;
    static final int BX_GENERAL_8BIT_REG  = 11;
    static final int BX_GENERAL_16BIT_REG =12;
    static final int BX_GENERAL_32BIT_REG =13;
    static final int BX_NO_REG_TYPE       =14;

    String instruction = "";
    String seg_override = null;
    byte[] code;
    int ofs, len;
    int codePosition;
    boolean printDecoding = false;

    boolean db_32bit_opsize = true;
    boolean db_32bit_addrsize = true;
  
    int db_rep_prefix = 0;
    int db_repne_prefix = 0;


    // used for BX_DECODE_MODRM
    int mod, opcode, rm;

    DebugPrintStream dout;
    DebugPrintStream asmout;

    public Disassembler(byte[] code, int ofs, int len,
			DebugPrintStream dout,
			DebugPrintStream asmout) {
	this.code = code;
	this.ofs = ofs;
	this.len = len;
	this.asmout = asmout;
	this.dout = dout;
	codePosition = ofs;
    }

    private void panic(String msg) {
	throw new Error(msg);
    }

    private void addInstr(String ins) {
	instruction += ins;
    }

    void invalidOpcode() {
	instruction += "???";
    }

    private int fetch_byte() {
	return code[codePosition++] & 0xff;
    }

    private int fetch_word() {
	int x;
	x = (code[codePosition++] & 0xff);
	x = (x << 8) | (code[codePosition++] & 0xff);
	return x;
    }

    private int fetch_dword() {
	int x;
	x = (code[codePosition+3] & 0xff);
	x = (x << 8) | (code[codePosition+2] & 0xff);
	x = (x << 8) | (code[codePosition+1] & 0xff);
	x = (x << 8) | (code[codePosition+0] & 0xff);
	codePosition += 4;
	return x;
    }

    private byte peek_byte() {
	return code[codePosition];
    }

    private void BX_DECODE_MODRM(int mod_rm_byte) {
	mod    = (mod_rm_byte >> 6) & 0x03; 
	opcode = (mod_rm_byte >> 3) & 0x07; 
	rm     =  mod_rm_byte & 0x07; 
    }

    public void disasm() {
	while(codePosition < ofs+len) {
	    instruction = toHexInt(codePosition)+"  ";
	    asmout.println(disasmInstr());
	}
    }

    public void disasm(LineInfo[] lineTable) {
	int lineTablePos = 0;
	while(codePosition < ofs+len) {
	    while (lineTablePos < lineTable.length && lineTable[lineTablePos].start <= codePosition) {
		asmout.println("Bytecode: "+lineTable[lineTablePos].bytecodePos);
		lineTablePos++;
	    }
	    instruction = toHexInt(codePosition)+"  ";
	    asmout.println(disasmInstr());
	}
    }

    public String disasmInstr() {
	int byte_count;
	int next_byte;
	int mod_rm_byte;

	
	/* On the 386 and above, instructions must be a maximum of 15 bytes long.
	 * this means redundant prefix codes can put the byte count over 15 and
	 * cause an illegal instruction.
	 */
	for (byte_count=0; byte_count<15; byte_count++) {
	    next_byte = fetch_byte();
	    switch (next_byte) {
		
	    case 0x00: addInstr("add "); EbGb(); return instruction;
	    case 0x01: addInstr("add "); EvGv(); return instruction;
	    case 0x02: addInstr("add "); GbEb(); return instruction;
	    case 0x03: addInstr("add "); GvEv(); return instruction;
	    case 0x04: addInstr("add AL, "); Ib(); return instruction;
	    case 0x05: addInstr("add "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x06: addInstr("push es"); return instruction;
	    case 0x07: addInstr("pop es"); return instruction;
	    case 0x08: addInstr("or "); EbGb(); return instruction;
	    case 0x09: addInstr("or "); EvGv(); return instruction;
	    case 0x0A: addInstr("or "); GbEb(); return instruction;
	    case 0x0B: addInstr("or "); GvEv(); return instruction;
	    case 0x0C: addInstr("or AL, "); Ib(); return instruction;
	    case 0x0D: addInstr("or "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x0E: addInstr("push cs"); return instruction;
	    case 0x0F: /* 2-byte escape */
		next_byte = fetch_byte();
		if ( ++byte_count >= 15 ) {
		    invalidOpcode();
		    return instruction;
		}
		switch (next_byte) {
		case 0x00: /* Group 6 */
		    mod_rm_byte = peek_byte();
		    BX_DECODE_MODRM(mod_rm_byte);
		    switch (opcode) {
		    case 0x00: addInstr("sldt "); Ew(); return instruction;
		    case 0x01: addInstr("str "); Ew(); return instruction;
		    case 0x02: addInstr("lldt "); Ew(); return instruction;
		    case 0x03: addInstr("ltr "); Ew(); return instruction;
		    case 0x04: addInstr("verr "); Ew(); return instruction;
		    case 0x05: addInstr("verw "); Ew(); return instruction;
		    case 0x06: invalidOpcode(); return instruction;
		    case 0x07: invalidOpcode(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		case 0x01: /* Group 7 */
		    mod_rm_byte = peek_byte();
		    BX_DECODE_MODRM(mod_rm_byte);
		    switch (opcode) {
		    case 0x00: addInstr("sgdt "); Ms(); return instruction;
		    case 0x01: addInstr("sidt "); Ms(); return instruction;
		    case 0x02: addInstr("lgdt "); Ms(); return instruction;
		    case 0x03: addInstr("lidt "); Ms(); return instruction;
		    case 0x04: addInstr("smsw "); Ew(); return instruction;
		    case 0x05: invalidOpcode(); return instruction;
		    case 0x06: addInstr("lmsw "); Ew(); return instruction;
		    case 0x07: invalidOpcode(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }

		case 0x02: addInstr("lar "); GvEw(); return instruction;
		case 0x03: addInstr("lsl "); GvEw(); return instruction;
		case 0x04: invalidOpcode(); return instruction;
		case 0x05: invalidOpcode(); return instruction;
		case 0x06: addInstr("clts"); return instruction;
		case 0x07: invalidOpcode(); return instruction;
		case 0x08: addInstr("invd"); return instruction;
		case 0x09: addInstr("wbinvd"); return instruction;
		case 0x0A:
		case 0x0B:
		case 0x0C:
		case 0x0D:
		case 0x0E:
		case 0x0F: invalidOpcode(); return instruction;

		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x14:
		case 0x15:
		case 0x16:
		case 0x17:
		case 0x18:
		case 0x19:
		case 0x1A:
		case 0x1B:
		case 0x1C:
		case 0x1D:
		case 0x1E:
		case 0x1F: invalidOpcode(); return instruction;

		case 0x20: addInstr("mov "); RdCd(); return instruction;
		case 0x21: addInstr("mov "); RdDd(); return instruction;
		case 0x22: addInstr("mov "); CdRd(); return instruction;
		case 0x23: addInstr("mov "); DdRd(); return instruction;
		case 0x24: addInstr("mov "); RdTd(); return instruction;
		case 0x25: invalidOpcode(); return instruction;
		case 0x26: addInstr("mov "); TdRd(); return instruction;
		case 0x27:
		case 0x28:
		case 0x29:
		case 0x2A:
		case 0x2B:
		case 0x2C:
		case 0x2D:
		case 0x2E:
		case 0x2F: invalidOpcode(); return instruction;

		case 0x30:
		case 0x31:
		case 0x32:
		case 0x33:
		case 0x34:
		case 0x35:
		case 0x36:
		case 0x37:
		case 0x38:
		case 0x39:
		case 0x3A:
		case 0x3B:
		case 0x3C:
		case 0x3D:
		case 0x3E:
		case 0x3F: invalidOpcode(); return instruction;

		case 0x40:
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
		case 0x45:
		case 0x46:
		case 0x47:
		case 0x48:
		case 0x49:
		case 0x4A:
		case 0x4B:
		case 0x4C:
		case 0x4D:
		case 0x4E:
		case 0x4F: invalidOpcode(); return instruction;

		case 0x50:
		case 0x51:
		case 0x52:
		case 0x53:
		case 0x54:
		case 0x55:
		case 0x56:
		case 0x57:
		case 0x58:
		case 0x59:
		case 0x5A:
		case 0x5B:
		case 0x5C:
		case 0x5D:
		case 0x5E:
		case 0x5F: invalidOpcode(); return instruction;

		case 0x60:
		case 0x61:
		case 0x62:
		case 0x63:
		case 0x64:
		case 0x65:
		case 0x66:
		case 0x67:
		case 0x68:
		case 0x69:
		case 0x6A:
		case 0x6B:
		case 0x6C:
		case 0x6D:
		case 0x6E:
		case 0x6F: invalidOpcode(); return instruction;

		case 0x70:
		case 0x71:
		case 0x72:
		case 0x73:
		case 0x74:
		case 0x75:
		case 0x76:
		case 0x77:
		case 0x78:
		case 0x79:
		case 0x7A:
		case 0x7B:
		case 0x7C:
		case 0x7D:
		case 0x7E:
		case 0x7F: invalidOpcode(); return instruction;

		case 0x80: addInstr("jo "); Jv(); return instruction;
		case 0x81: addInstr("jno "); Jv(); return instruction;
		case 0x82: addInstr("jb "); Jv(); return instruction;
		case 0x83: addInstr("jnb "); Jv(); return instruction;
		case 0x84: addInstr("jz "); Jv(); return instruction;
		case 0x85: addInstr("jnz "); Jv(); return instruction;
		case 0x86: addInstr("jbe "); Jv(); return instruction;
		case 0x87: addInstr("jnbe "); Jv(); return instruction;
		case 0x88: addInstr("js "); Jv(); return instruction;
		case 0x89: addInstr("jns "); Jv(); return instruction;
		case 0x8A: addInstr("jp "); Jv(); return instruction;
		case 0x8B: addInstr("jnp "); Jv(); return instruction;
		case 0x8C: addInstr("jl "); Jv(); return instruction;
		case 0x8D: addInstr("jnl "); Jv(); return instruction;
		case 0x8E: addInstr("jle "); Jv(); return instruction;
		case 0x8F: addInstr("jnle "); Jv(); return instruction;

		case 0x90: addInstr("seto "); Eb(); return instruction;
		case 0x91: addInstr("setno "); Eb(); return instruction;
		case 0x92: addInstr("setb "); Eb(); return instruction;
		case 0x93: addInstr("setnb "); Eb(); return instruction;
		case 0x94: addInstr("setz "); Eb(); return instruction;
		case 0x95: addInstr("setnz "); Eb(); return instruction;
		case 0x96: addInstr("setbe "); Eb(); return instruction;
		case 0x97: addInstr("setnbe "); Eb(); return instruction;
		case 0x98: addInstr("sets "); Eb(); return instruction;
		case 0x99: addInstr("setns "); Eb(); return instruction;
		case 0x9A: addInstr("setp "); Eb(); return instruction;
		case 0x9B: addInstr("setnp "); Eb(); return instruction;
		case 0x9C: addInstr("setl "); Eb(); return instruction;
		case 0x9D: addInstr("setnl "); Eb(); return instruction;
		case 0x9E: addInstr("setle "); Eb(); return instruction;
		case 0x9F: addInstr("setnle "); Eb(); return instruction;

		case 0xA0: addInstr("push fs"); return instruction;
		case 0xA1: addInstr("pop fs"); return instruction;
		case 0xA2: invalidOpcode(); return instruction;
		case 0xA3: addInstr("bt "); EvGv(); return instruction;
		case 0xA4: addInstr("shld "); EvGv(); addInstr(", "); Ib(); return instruction;
		case 0xA5: addInstr("shld "); EvGv(); addInstr(", CL"); return instruction;
		case 0xA6: addInstr("cmpxchg "); XBTS(); return instruction;
		case 0xA7: addInstr("cmpxchg "); IBTS(); return instruction;
		case 0xA8: addInstr("push gs"); return instruction;
		case 0xA9: addInstr("pop gs"); return instruction;
		case 0xAA: invalidOpcode(); return instruction;
		case 0xAB: addInstr("bts "); EvGv(); return instruction;
		case 0xAC: addInstr("shrd "); EvGv(); addInstr(", "); Ib(); return instruction;
		case 0xAD: addInstr("shrd "); EvGv(); addInstr(", CL"); return instruction;
		case 0xAE: invalidOpcode(); return instruction;
		case 0xAF: addInstr("imul "); GvEv(); return instruction;

		case 0xB0: addInstr("cmpxchg "); EbGb(); return instruction;
		case 0xB1: addInstr("cmpxchg "); EvGv(); return instruction;
		case 0xB2: addInstr("lss "); Mp(); return instruction;
		case 0xB3: addInstr("btr "); EvGv(); return instruction;
		case 0xB4: addInstr("lfs "); Mp(); return instruction;
		case 0xB5: addInstr("lgs "); Mp(); return instruction;
		case 0xB6: addInstr("movzx "); GvEb(); return instruction;
		case 0xB7: addInstr("movzx "); GvEw(); return instruction;
		case 0xB8: invalidOpcode(); return instruction;
		case 0xB9: invalidOpcode(); return instruction;
		case 0xBA: /* Group 8 Ev,Ib */
		    mod_rm_byte = peek_byte();
		    BX_DECODE_MODRM(mod_rm_byte);
		    switch (opcode) {
		    case 0x00:
		    case 0x01:
		    case 0x02:
		    case 0x03: invalidOpcode(); return instruction;
		    case 0x04: addInstr("bt "); EvIb(); return instruction;
		    case 0x05: addInstr("bts "); EvIb(); return instruction;
		    case 0x06: addInstr("btr "); EvIb(); return instruction;
		    case 0x07: addInstr("btc "); EvIb(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }

		case 0xBB: addInstr("btc "); EvGv(); return instruction;
		case 0xBC: addInstr("bsf "); GvEv(); return instruction;
		case 0xBD: addInstr("bsr "); GvEv(); return instruction;
		case 0xBE: addInstr("movsx "); GvEb(); return instruction;
		case 0xBF: addInstr("movsx "); GvEw(); return instruction;

		case 0xC0: addInstr("xadd "); EbGb(); return instruction;
		case 0xC1: addInstr("xadd "); EvGv(); return instruction;
		case 0xC2:
		case 0xC3:
		case 0xC4:
		case 0xC5:
		case 0xC6:
		case 0xC7: invalidOpcode(); return instruction;
		case 0xC8: addInstr("bswap "); eAX(); return instruction;
		case 0xC9: addInstr("bswap "); eCX(); return instruction;
		case 0xCA: addInstr("bswap "); eDX(); return instruction;
		case 0xCB: addInstr("bswap "); eBX(); return instruction;
		case 0xCC: addInstr("bswap "); eSP(); return instruction;
		case 0xCD: addInstr("bswap "); eBP(); return instruction;
		case 0xCE: addInstr("bswap "); eSI(); return instruction;
		case 0xCF: addInstr("bswap "); eDI(); return instruction;

		case 0xD0:
		case 0xD1:
		case 0xD2:
		case 0xD3:
		case 0xD4:
		case 0xD5:
		case 0xD6:
		case 0xD7:
		case 0xD8:
		case 0xD9:
		case 0xDA:
		case 0xDB:
		case 0xDC:
		case 0xDD:
		case 0xDE:
		case 0xDF: invalidOpcode(); return instruction;

		case 0xE0:
		case 0xE1:
		case 0xE2:
		case 0xE3:
		case 0xE4:
		case 0xE5:
		case 0xE6:
		case 0xE7:
		case 0xE8:
		case 0xE9:
		case 0xEA:
		case 0xEB:
		case 0xEC:
		case 0xED:
		case 0xEE:
		case 0xEF: invalidOpcode(); return instruction;

		case 0xF0:
		case 0xF1:
		case 0xF2:
		case 0xF3:
		case 0xF4:
		case 0xF5:
		case 0xF6:
		case 0xF7:
		case 0xF8:
		case 0xF9:
		case 0xFA:
		case 0xFB:
		case 0xFC:
		case 0xFD:
		case 0xFE:
		case 0xFF: invalidOpcode(); return instruction;

		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0x10: addInstr("adc "); EbGb(); return instruction;
	    case 0x11: addInstr("adc "); EvGv(); return instruction;
	    case 0x12: addInstr("adc "); GbEb(); return instruction;
	    case 0x13: addInstr("adc "); GvEv(); return instruction;
	    case 0x14: addInstr("adc AL, "); Ib(); return instruction;
	    case 0x15: addInstr("adc "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x16: addInstr("push ss"); return instruction;
	    case 0x17: addInstr("pop ss"); return instruction;
	    case 0x18: addInstr("sbb "); EbGb(); return instruction;
	    case 0x19: addInstr("sbb "); EvGv(); return instruction;
	    case 0x1A: addInstr("sbb "); GbEb(); return instruction;
	    case 0x1B: addInstr("sbb "); GvEv(); return instruction;
	    case 0x1C: addInstr("sbb AL, "); Ib(); return instruction;
	    case 0x1D: addInstr("sbb "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x1E: addInstr("push ds"); return instruction;
	    case 0x1F: addInstr("pop ds"); return instruction;


	    case 0x20: addInstr("and "); EbGb(); return instruction;
	    case 0x21: addInstr("and "); EvGv(); return instruction;
	    case 0x22: addInstr("and "); GbEb(); return instruction;
	    case 0x23: addInstr("and "); GvEv(); return instruction;
	    case 0x24: addInstr("and AL, "); Ib(); return instruction;
	    case 0x25: addInstr("and "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x26:
		seg_override = "ES";
		addInstr("ES: ");
		break;
	    case 0x27: addInstr("daa"); return instruction;
	    case 0x28: addInstr("sub "); EbGb(); return instruction;
	    case 0x29: addInstr("sub "); EvGv(); return instruction;
	    case 0x2A: addInstr("sub "); GbEb(); return instruction;
	    case 0x2B: addInstr("sub "); GvEv(); return instruction;
	    case 0x2C: addInstr("sub AL, "); Ib(); return instruction;
	    case 0x2D: addInstr("sub "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x2E:
		seg_override = "CS";
		addInstr("CS: ");
		break;
	    case 0x2F: addInstr("das"); return instruction;

	    case 0x30: addInstr("xor "); EbGb(); return instruction;
	    case 0x31: addInstr("xor "); EvGv(); return instruction;
	    case 0x32: addInstr("xor "); GbEb(); return instruction;
	    case 0x33: addInstr("xor "); GvEv(); return instruction;
	    case 0x34: addInstr("xor AL, "); Ib(); return instruction;
	    case 0x35: addInstr("xor "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x36:
		seg_override = "SS";
		addInstr("SS: ");
		break;
	    case 0x37: addInstr("aaa"); return instruction;
	    case 0x38: addInstr("cmp "); EbGb(); return instruction;
	    case 0x39: addInstr("cmp "); EvGv(); return instruction;
	    case 0x3A: addInstr("cmp "); GbEb(); return instruction;
	    case 0x3B: addInstr("cmp "); GvEv(); return instruction;
	    case 0x3C: addInstr("cmp AL, "); Ib(); return instruction;
	    case 0x3D: addInstr("cmp "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0x3E:
		seg_override = "DS";
		addInstr("DS: ");
		break;
	    case 0x3F: addInstr("aas"); return instruction;

	    case 0x40: addInstr("inc "); eAX(); return instruction;
	    case 0x41: addInstr("inc "); eCX(); return instruction;
	    case 0x42: addInstr("inc "); eDX(); return instruction;
	    case 0x43: addInstr("inc "); eBX(); return instruction;
	    case 0x44: addInstr("inc "); eSP(); return instruction;
	    case 0x45: addInstr("inc "); eBP(); return instruction;
	    case 0x46: addInstr("inc "); eSI(); return instruction;
	    case 0x47: addInstr("inc "); eDI(); return instruction;
	    case 0x48: addInstr("dec "); eAX(); return instruction;
	    case 0x49: addInstr("dec "); eCX(); return instruction;
	    case 0x4A: addInstr("dec "); eDX(); return instruction;
	    case 0x4B: addInstr("dec "); eBX(); return instruction;
	    case 0x4C: addInstr("dec "); eSP(); return instruction;
	    case 0x4D: addInstr("dec "); eBP(); return instruction;
	    case 0x4E: addInstr("dec "); eSI(); return instruction;
	    case 0x4F: addInstr("dec "); eDI(); return instruction;


	    case 0x50: addInstr("push "); eAX(); return instruction;
	    case 0x51: addInstr("push "); eCX(); return instruction;
	    case 0x52: addInstr("push "); eDX(); return instruction;
	    case 0x53: addInstr("push "); eBX(); return instruction;
	    case 0x54: addInstr("push "); eSP(); return instruction;
	    case 0x55: addInstr("push "); eBP(); return instruction;
	    case 0x56: addInstr("push "); eSI(); return instruction;
	    case 0x57: addInstr("push "); eDI(); return instruction;
	    case 0x58: addInstr("pop "); eAX(); return instruction;
	    case 0x59: addInstr("pop "); eCX(); return instruction;
	    case 0x5A: addInstr("pop "); eDX(); return instruction;
	    case 0x5B: addInstr("pop "); eBX(); return instruction;
	    case 0x5C: addInstr("pop "); eSP(); return instruction;
	    case 0x5D: addInstr("pop "); eBP(); return instruction;
	    case 0x5E: addInstr("pop "); eSI(); return instruction;
	    case 0x5F: addInstr("pop "); eDI(); return instruction;


	    case 0x60: addInstr("pushad"); return instruction;
	    case 0x61: addInstr("popad"); return instruction;
	    case 0x62: addInstr("bound "); GvMa(); return instruction;
	    case 0x63: addInstr("arpl "); EwRw(); return instruction;
	    case 0x64:
		seg_override = "FS";
		addInstr("FS: ");
		break;
	    case 0x65:
		seg_override = "GS";
		addInstr("GS: ");
		break;
	    case 0x66:
		db_32bit_opsize = !db_32bit_opsize;
		addInstr("OPSIZE: ");
		break;
	    case 0x67:
		db_32bit_addrsize = !db_32bit_addrsize;
		addInstr("ADDRSIZE: ");
		break;
	    case 0x68: addInstr("push "); Iv(); return instruction;
	    case 0x69: addInstr("imul "); GvEv(); addInstr(", "); Iv(); return instruction;
	    case 0x6A: addInstr("push "); Ib(); return instruction;
	    case 0x6B: addInstr("imul "); GvEv(); addInstr(", "); Ib(); return instruction;
	    case 0x6C: addInstr("insb "); YbDX(); return instruction;
	    case 0x6D: addInstr("insw "); YvDX(); return instruction;
	    case 0x6E: addInstr("outsb "); DXXb(); return instruction;
	    case 0x6F: addInstr("outsw "); DXXv(); return instruction;


	    case 0x70: addInstr("jo "); Jb(); return instruction;
	    case 0x71: addInstr("jno "); Jb(); return instruction;
	    case 0x72: addInstr("jb "); Jb(); return instruction;
	    case 0x73: addInstr("jnb "); Jb(); return instruction;
	    case 0x74: addInstr("jz "); Jb(); return instruction;
	    case 0x75: addInstr("jnz "); Jb(); return instruction;
	    case 0x76: addInstr("jbe "); Jb(); return instruction;
	    case 0x77: addInstr("jnbe "); Jb(); return instruction;
	    case 0x78: addInstr("js "); Jb(); return instruction;
	    case 0x79: addInstr("jns "); Jb(); return instruction;
	    case 0x7A: addInstr("jp "); Jb(); return instruction;
	    case 0x7B: addInstr("jnp "); Jb(); return instruction;
	    case 0x7C: addInstr("jl "); Jb(); return instruction;
	    case 0x7D: addInstr("jnl "); Jb(); return instruction;
	    case 0x7E: addInstr("jle "); Jb(); return instruction;
	    case 0x7F: addInstr("jnle "); Jb(); return instruction;

	    case 0x80: /* Immdediate Grp 1 EbIb */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("add "); EbIb(); return instruction;
		case 0x01: addInstr("or "); EbIb();  return instruction;
		case 0x02: addInstr("adc "); EbIb(); return instruction;
		case 0x03: addInstr("sbb "); EbIb(); return instruction;
		case 0x04: addInstr("and "); EbIb(); return instruction;
		case 0x05: addInstr("sub "); EbIb(); return instruction;
		case 0x06: addInstr("xor "); EbIb(); return instruction;
		case 0x07: addInstr("cmp "); EbIb(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0x81: /* Immdediate Grp 1 EvIv */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("add "); EvIv(); return instruction;
		case 0x01: addInstr("or "); EvIv(); return instruction;
		case 0x02: addInstr("adc "); EvIv(); return instruction;
		case 0x03: addInstr("sbb "); EvIv(); return instruction;
		case 0x04: addInstr("and "); EvIv(); return instruction;
		case 0x05: addInstr("sub "); EvIv(); return instruction;
		case 0x06: addInstr("xor "); EvIv(); return instruction;
		case 0x07: addInstr("cmp "); EvIv(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0x82: invalidOpcode(); return instruction;

	    case 0x83: /* Immdediate Grp 1 EvIb */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("add "); EvIb(); return instruction;
		case 0x01: addInstr("or "); EvIb(); return instruction;
		case 0x02: addInstr("adc "); EvIb(); return instruction;
		case 0x03: addInstr("sbb "); EvIb(); return instruction;
		case 0x04: addInstr("and "); EvIb(); return instruction;
		case 0x05: addInstr("sub "); EvIb(); return instruction;
		case 0x06: addInstr("xor "); EvIb(); return instruction;
		case 0x07: addInstr("cmp "); EvIb(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0x84: addInstr("test "); EbGb(); return instruction;
	    case 0x85: addInstr("test "); EvGv(); return instruction;
	    case 0x86: addInstr("xchg "); EbGb(); return instruction;
	    case 0x87: addInstr("xchg "); EvGv(); return instruction;
	    case 0x88: addInstr("mov "); EbGb(); return instruction;
	    case 0x89: addInstr("mov "); EvGv(); return instruction;
	    case 0x8A: addInstr("mov "); GbEb(); return instruction;
	    case 0x8B: addInstr("mov "); GvEv(); return instruction;
	    case 0x8C: addInstr("mov "); EwSw(); return instruction;
	    case 0x8D: addInstr("lea "); GvM(); return instruction;
	    case 0x8E: addInstr("mov "); SwEw(); return instruction;
	    case 0x8F: addInstr("pop "); Ev(); return instruction;


	    case 0x90: addInstr("nop"); return instruction;
	    case 0x91: addInstr("xchg "); eCX(); addInstr(", "); eAX(); return instruction;
	    case 0x92: addInstr("xchg "); eDX(); addInstr(", "); eAX(); return instruction;
	    case 0x93: addInstr("xchg "); eBX(); addInstr(", "); eAX(); return instruction;
	    case 0x94: addInstr("xchg "); eSP(); addInstr(", "); eAX(); return instruction;
	    case 0x95: addInstr("xchg "); eBP(); addInstr(", "); eAX(); return instruction;
	    case 0x96: addInstr("xchg "); eSI(); addInstr(", "); eAX(); return instruction;
	    case 0x97: addInstr("xchg "); eDI(); addInstr(", "); eAX(); return instruction;
	    case 0x98: addInstr("cbw"); return instruction;
	    case 0x99: addInstr("cwd"); return instruction;
	    case 0x9A: addInstr("call "); Ap(); return instruction;
	    case 0x9B: addInstr("wait"); return instruction;
	    case 0x9C: addInstr("pushf"); return instruction;
	    case 0x9D: addInstr("popf"); return instruction;
	    case 0x9E: addInstr("sahf"); return instruction;
	    case 0x9F: addInstr("lahf"); return instruction;


	    case 0xA0: addInstr("mov "); ALOb(); return instruction;
	    case 0xA1: addInstr("mov "); eAXOv(); return instruction;
	    case 0xA2: addInstr("mov "); ObAL(); return instruction;
	    case 0xA3: addInstr("mov "); OveAX(); return instruction;
	    case 0xA4: addInstr("movsb "); XbYb(); return instruction;
	    case 0xA5: addInstr("movsw "); XvYv(); return instruction;
	    case 0xA6: addInstr("cmpsb "); XbYb(); return instruction;
	    case 0xA7: addInstr("cmpsw "); XvYv(); return instruction;
	    case 0xA8: addInstr("test AL, "); Ib(); return instruction;
	    case 0xA9: addInstr("test "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0xAA: addInstr("stosb "); YbAL(); return instruction;
	    case 0xAB: addInstr("stosw "); YveAX(); return instruction;
	    case 0xAC: addInstr("lodsb "); ALXb(); return instruction;
	    case 0xAD: addInstr("lodsw "); eAXXv(); return instruction;
	    case 0xAE: addInstr("scasb "); ALXb(); return instruction;
	    case 0xAF: addInstr("scasw "); eAXXv(); return instruction;


	    case 0xB0: addInstr("mov AL, "); Ib(); return instruction;
	    case 0xB1: addInstr("mov CL, "); Ib(); return instruction;
	    case 0xB2: addInstr("mov DL, "); Ib(); return instruction;
	    case 0xB3: addInstr("mov BL, "); Ib(); return instruction;
	    case 0xB4: addInstr("mov AH, "); Ib(); return instruction;
	    case 0xB5: addInstr("mov CH, "); Ib(); return instruction;
	    case 0xB6: addInstr("mov DH, "); Ib(); return instruction;
	    case 0xB7: addInstr("mov BH, "); Ib(); return instruction;
	    case 0xB8: addInstr("mov "); eAX(); addInstr(", "); Iv(); return instruction;
	    case 0xB9: addInstr("mov "); eCX(); addInstr(", "); Iv(); return instruction;
	    case 0xBA: addInstr("mov "); eDX(); addInstr(", "); Iv(); return instruction;
	    case 0xBB: addInstr("mov "); eBX(); addInstr(", "); Iv(); return instruction;
	    case 0xBC: addInstr("mov "); eSP(); addInstr(", "); Iv(); return instruction;
	    case 0xBD: addInstr("mov "); eBP(); addInstr(", "); Iv(); return instruction;
	    case 0xBE: addInstr("mov "); eSI(); addInstr(", "); Iv(); return instruction;
	    case 0xBF: addInstr("mov "); eDI(); addInstr(", "); Iv(); return instruction;

	    case 0xC0: /* Group 2 Eb,Ib */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); EbIb(); return instruction;
		case 0x01: addInstr("ror "); EbIb(); return instruction;
		case 0x02: addInstr("rcl "); EbIb(); return instruction;
		case 0x03: addInstr("rcr "); EbIb(); return instruction;
		case 0x04: addInstr("shl "); EbIb(); return instruction;
		case 0x05: addInstr("shr "); EbIb(); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); EbIb(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xC1: /* Group 2 Ev,Ib */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); EvIb(); return instruction;
		case 0x01: addInstr("ror "); EvIb(); return instruction;
		case 0x02: addInstr("rcl "); EvIb(); return instruction;
		case 0x03: addInstr("rcr "); EvIb(); return instruction;
		case 0x04: addInstr("shl "); EvIb(); return instruction;
		case 0x05: addInstr("shr "); EvIb(); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); EvIb(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xC2: addInstr("ret_near "); Iw(); return instruction;
	    case 0xC3: addInstr("ret_near"); return instruction;
	    case 0xC4: addInstr("les "); GvMp(); return instruction;
	    case 0xC5: addInstr("lds "); GvMp(); return instruction;
	    case 0xC6: addInstr("mov "); EbIb(); return instruction;
	    case 0xC7: addInstr("mov "); EvIv(); return instruction;
	    case 0xC8: addInstr("enter "); Iw(); addInstr(", "); Ib(); return instruction;
	    case 0xC9: addInstr("leave"); return instruction;
	    case 0xCA: addInstr("ret_far "); Iw(); return instruction;
	    case 0xCB: addInstr("ret_far"); return instruction;
	    case 0xCC: addInstr("int3"); return instruction;
	    case 0xCD: addInstr("int "); Ib(); return instruction;
	    case 0xCE: addInstr("into"); return instruction;
	    case 0xCF: addInstr("iret"); return instruction;


	    case 0xD0: /* Group 2 Eb,1 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); Eb1(); return instruction;
		case 0x01: addInstr("ror "); Eb1(); return instruction;
		case 0x02: addInstr("rcl "); Eb1(); return instruction;
		case 0x03: addInstr("rcr "); Eb1(); return instruction;
		case 0x04: addInstr("shl "); Eb1(); return instruction;
		case 0x05: addInstr("shr "); Eb1(); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); Eb1(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xD1: /* group2 Ev,1 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); Ev1(); return instruction;
		case 0x01: addInstr("ror "); Ev1(); return instruction;
		case 0x02: addInstr("rcl "); Ev1(); return instruction;
		case 0x03: addInstr("rcr "); Ev1(); return instruction;
		case 0x04: addInstr("shl "); Ev1(); return instruction;
		case 0x05: addInstr("shr "); Ev1(); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); Ev1(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xD2: /* group2 Eb,CL */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); Eb(); addInstr(", CL"); return instruction;
		case 0x01: addInstr("ror "); Eb(); addInstr(", CL"); return instruction;
		case 0x02: addInstr("rcl "); Eb(); addInstr(", CL"); return instruction;
		case 0x03: addInstr("rcr "); Eb(); addInstr(", CL"); return instruction;
		case 0x04: addInstr("shl "); Eb(); addInstr(", CL"); return instruction;
		case 0x05: addInstr("shr "); Eb(); addInstr(", CL"); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); Eb(); addInstr(", CL"); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xD3: /* group2 Ev,CL */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("rol "); Ev(); addInstr(", CL"); return instruction;
		case 0x01: addInstr("ror "); Ev(); addInstr(", CL"); return instruction;
		case 0x02: addInstr("rcl "); Ev(); addInstr(", CL"); return instruction;
		case 0x03: addInstr("rcr "); Ev(); addInstr(", CL"); return instruction;
		case 0x04: addInstr("shl "); Ev(); addInstr(", CL"); return instruction;
		case 0x05: addInstr("shr "); Ev(); addInstr(", CL"); return instruction;
		case 0x06: invalidOpcode(); return instruction;
		case 0x07: addInstr("sar "); Ev(); addInstr(", CL"); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xD4: addInstr("aam"); return instruction;
	    case 0xD5: addInstr("aad"); return instruction;
	    case 0xD6: invalidOpcode(); return instruction;
	    case 0xD7: addInstr("xlat"); return instruction;

	    case 0xD8: /* ESC0 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fadd "); Es(); return instruction;
		    case 0x01: addInstr("fmul "); Es(); return instruction;
		    case 0x02: addInstr("fcom "); Es(); return instruction;
		    case 0x03: addInstr("fcomp "); Es(); return instruction;
		    case 0x04: addInstr("fsub "); Es(); return instruction;
		    case 0x05: addInstr("fsubr "); Es(); return instruction;
		    case 0x06: addInstr("fdiv "); Es(); return instruction;
		    case 0x07: addInstr("fdivr "); Es(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00: addInstr("fadd "); ST_STi(); return instruction;
		    case 0x01: addInstr("fmul "); ST_STi(); return instruction;
		    case 0x02: addInstr("fcom "); ST_STi(); return instruction;
		    case 0x03: addInstr("fcomp "); ST_STi(); return instruction;
		    case 0x04: addInstr("fsub "); ST_STi(); return instruction;
		    case 0x05: addInstr("fsubr "); ST_STi(); return instruction;
		    case 0x06: addInstr("fdiv "); ST_STi(); return instruction;
		    case 0x07: addInstr("fdivr "); ST_STi(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}

	    case 0xD9: /* ESC1 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fld "); Es(); return instruction;
		    case 0x01: invalidOpcode(); return instruction;
		    case 0x02: addInstr("fst "); Es(); return instruction;
		    case 0x03: addInstr("fstp "); Es(); return instruction;
		    case 0x04: addInstr("fldenv "); Ea(); return instruction;
		    case 0x05: addInstr("fldcw "); Ew(); return instruction;
		    case 0x06: addInstr("fstenv "); Ea(); return instruction;
		    case 0x07: addInstr("fstcw "); Ew(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00:
			addInstr("fld "); STi(); return instruction;
		    case 0x01:
			addInstr("fxch "); STi(); return instruction;
		    case 0x02:
			if (rm == 0) {
			    addInstr("fnop"); return instruction;
			}
			else {
			    invalidOpcode(); return instruction;
			}
		    case 0x03:
			addInstr("fstp "); STi(); return instruction;

		    case 0x04:
			switch (rm) {
			case 0x00: addInstr("fchs"); return instruction;
			case 0x01: addInstr("fabs"); return instruction;
			case 0x02:
			case 0x03: invalidOpcode(); return instruction;
			case 0x04: addInstr("ftst"); return instruction;
			case 0x05: addInstr("fxam"); return instruction;
			case 0x06:
			case 0x07: invalidOpcode(); return instruction;
			}
		    case 0x05:
			switch (rm) {
			case 0x00: addInstr("fld1"); return instruction;
			case 0x01: addInstr("fldl2t"); return instruction;
			case 0x02: addInstr("fldl2e"); return instruction;
			case 0x03: addInstr("fldpi"); return instruction;
			case 0x04: addInstr("fldlg2"); return instruction;
			case 0x05: addInstr("fldln2"); return instruction;
			case 0x06: addInstr("fldz"); return instruction;
			case 0x07: invalidOpcode(); return instruction;
			}
		    case 0x06:
			switch (rm) {
			case 0x00: addInstr("f2xm1"); return instruction;
			case 0x01: addInstr("fyl2x"); return instruction;
			case 0x02: addInstr("fptan"); return instruction;
			case 0x03: addInstr("fpatan"); return instruction;
			case 0x04: addInstr("fxtract"); return instruction;
			case 0x05: addInstr("fprem1"); return instruction;
			case 0x06: addInstr("fdecstp"); return instruction;
			case 0x07: addInstr("fincstp"); return instruction;
			}
		    case 0x07:
			switch (rm) {
			case 0x00: addInstr("fprem"); return instruction;
			case 0x01: addInstr("fyl2xp1"); return instruction;
			case 0x02: addInstr("fsqrt"); return instruction;
			case 0x03: addInstr("fsincos"); return instruction;
			case 0x04: addInstr("frndint"); return instruction;
			case 0x05: addInstr("fscale"); return instruction;
			case 0x06: addInstr("fsin"); return instruction;
			case 0x07: addInstr("fcos"); return instruction;
			}
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}

	    case 0xDA: /* ESC2 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fiadd "); Ed(); return instruction;
		    case 0x01: addInstr("fimul "); Ed(); return instruction;
		    case 0x02: addInstr("ficom "); Ed(); return instruction;
		    case 0x03: addInstr("ficomp "); Ed(); return instruction;
		    case 0x04: addInstr("fisub "); Ed(); return instruction;
		    case 0x05: addInstr("fisubr "); Ed(); return instruction;
		    case 0x06: addInstr("fidiv "); Ed(); return instruction;
		    case 0x07: addInstr("fidivr "); Ed(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x05:
			if (rm == 1) {
			    addInstr("fucompp"); return instruction;
			}
			else {
			    invalidOpcode(); return instruction;
			}
		    default: invalidOpcode(); return instruction;
		    }
		}

	    case 0xDB: /* ESC3 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fild "); Ed(); return instruction;
		    case 0x01: invalidOpcode(); return instruction;
		    case 0x02: addInstr("fist "); Ed(); return instruction;
		    case 0x03: addInstr("fistp "); Ed(); return instruction;
		    case 0x04: invalidOpcode(); return instruction;
		    case 0x05: addInstr("fld "); Et(); return instruction;
		    case 0x06: invalidOpcode(); return instruction;
		    case 0x07: addInstr("fstp "); Et(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x04:
			switch (rm) {
			case 0x00: addInstr("feni"); return instruction;
			case 0x01: addInstr("fdisi"); return instruction;
			case 0x02: addInstr("fclex"); return instruction;
			case 0x03: addInstr("finit"); return instruction;
			case 0x04: addInstr("fsetpm"); return instruction;
			default: invalidOpcode(); return instruction;
			}
		    default: invalidOpcode(); return instruction;
		    }
		}

	    case 0xDC: /* ESC4 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fadd "); El(); return instruction;
		    case 0x01: addInstr("fmul "); El(); return instruction;
		    case 0x02: addInstr("fcom "); El(); return instruction;
		    case 0x03: addInstr("fcomp "); El(); return instruction;
		    case 0x04: addInstr("fsub "); El(); return instruction;
		    case 0x05: addInstr("fsubr "); El(); return instruction;
		    case 0x06: addInstr("fdiv "); El(); return instruction;
		    case 0x07: addInstr("fdivr "); El(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00: addInstr("fadd "); STi_ST(); return instruction;
		    case 0x01: addInstr("fmul "); STi_ST(); return instruction;
		    case 0x02: addInstr("fcom "); STi_ST(); return instruction;
		    case 0x03: addInstr("fcomp "); STi_ST(); return instruction;
		    case 0x04: addInstr("fsubr "); STi_ST(); return instruction;
		    case 0x05: addInstr("fsub "); STi_ST(); return instruction;
		    case 0x06: addInstr("fdivr "); STi_ST(); return instruction;
		    case 0x07: addInstr("fdiv "); STi_ST(); return instruction;
		    default: invalidOpcode(); return instruction;
		    }
		}


	    case 0xDD: /* ESC5 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fld "); El(); return instruction;
		    case 0x01: invalidOpcode(); return instruction;
		    case 0x02: addInstr("fst "); El(); return instruction;
		    case 0x03: addInstr("fstp "); El(); return instruction;
		    case 0x04: addInstr("frstor "); Ea(); return instruction;
		    case 0x05: invalidOpcode(); return instruction;
		    case 0x06: addInstr("fsave "); Ea(); return instruction;
		    case 0x07: addInstr("fstsw "); Ew(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00: addInstr("ffree "); STi(); return instruction;
		    case 0x01: addInstr("fxch "); STi(); return instruction;
		    case 0x02: addInstr("fst "); STi(); return instruction;
		    case 0x03: addInstr("fstp "); STi(); return instruction;
		    case 0x04: addInstr("fucom "); STi_ST(); return instruction;
		    case 0x05: addInstr("fucomp "); STi(); return instruction;
		    case 0x06: invalidOpcode(); return instruction;
		    case 0x07: invalidOpcode(); return instruction;
		    default: invalidOpcode(); return instruction;
		    }
		}

	    case 0xDE: /* ESC6 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fiadd "); Ew(); return instruction;
		    case 0x01: addInstr("fimul "); Ew(); return instruction;
		    case 0x02: addInstr("ficom "); Ew(); return instruction;
		    case 0x03: addInstr("ficomp "); Ew(); return instruction;
		    case 0x04: addInstr("fisub "); Ew(); return instruction;
		    case 0x05: addInstr("fisubr "); Ew(); return instruction;
		    case 0x06: addInstr("fidiv "); Ew(); return instruction;
		    case 0x07: addInstr("fidivr "); Ew(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00: addInstr("faddp "); STi_ST(); return instruction;
		    case 0x01: addInstr("fmulp "); STi_ST(); return instruction;
		    case 0x02: addInstr("fcomp "); STi(); return instruction;
		    case 0x03:
			switch (rm) {
			case 0x01: addInstr("fcompp"); return instruction;
			default: invalidOpcode(); return instruction;
			}
		    case 0x04: addInstr("fsubrp "); STi_ST(); return instruction;
		    case 0x05: addInstr("fsubp "); STi_ST(); return instruction;
		    case 0x06: addInstr("fdivrp "); STi_ST(); return instruction;
		    case 0x07: addInstr("fdivp "); STi_ST(); return instruction;
		    default: invalidOpcode(); return instruction;
		    }
		}

	    case 0xDF: /* ESC7 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		if (mod != 3) {
		    switch (opcode) {
		    case 0x00: addInstr("fild "); Ew(); return instruction;
		    case 0x01: invalidOpcode(); return instruction;
		    case 0x02: addInstr("fist "); Ew(); return instruction;
		    case 0x03: addInstr("fistp "); Ew(); return instruction;
		    case 0x04: addInstr("fbld "); Eb(); return instruction;
		    case 0x05: addInstr("fild "); Eq(); return instruction;
		    case 0x06: addInstr("fbstp "); Eb(); return instruction;
		    case 0x07: addInstr("fistp "); Eq(); return instruction;
		    default: panic("debugger: invalid opcode\n"); return instruction;
		    }
		}
		else { /* mod == 3 */
		    switch (opcode) {
		    case 0x00: addInstr("ffree "); STi(); return instruction;
		    case 0x01: addInstr("fxch "); STi(); return instruction;
		    case 0x02: addInstr("fst "); STi(); return instruction;
		    case 0x03: addInstr("fstp "); STi(); return instruction;
		    case 0x04:
			switch (rm) {
			case 0x00: addInstr("fstsw ax"); return instruction;
			default: invalidOpcode(); return instruction;
			}
		    default: invalidOpcode(); return instruction;
		    }
		}


	    case 0xE0: addInstr("loopne "); Jb(); return instruction;
	    case 0xE1: addInstr("loope "); Jb(); return instruction;
	    case 0xE2: addInstr("loop "); Jb(); return instruction;
	    case 0xE3: addInstr("jcxz "); Jb(); return instruction;
	    case 0xE4: addInstr("in AL, "); Ib(); return instruction;
	    case 0xE5: addInstr("in "); eAX(); addInstr(", "); Ib(); return instruction;
	    case 0xE6: addInstr("out "); Ib(); addInstr(", AL"); return instruction;
	    case 0xE7: addInstr("out "); Ib(); addInstr(", "); eAX(); return instruction;
	    case 0xE8: addInstr("call "); Av(); return instruction;
	    case 0xE9: addInstr("jmp "); Jv(); return instruction;
	    case 0xEA: addInstr("jmp "); Ap(); return instruction;
	    case 0xEB: addInstr("jmp "); Jb(); return instruction;
	    case 0xEC: addInstr("in AL, DX"); return instruction;
	    case 0xED: addInstr("in "); eAX(); addInstr(", DX"); return instruction;
	    case 0xEE: addInstr("out DX, AL"); return instruction;
	    case 0xEF: addInstr("out DX, "); eAX(); return instruction;

	    case 0xF0: /* LOCK */
		addInstr("LOCK: ");
		break;
	    case 0xF1: invalidOpcode(); return instruction;
	    case 0xF2: /* REPNE/REPNZ */
		db_repne_prefix = 1;
		addInstr("REPNE: ");
		break;
	    case 0xF3: /* REP/REPE/REPZ */
		db_rep_prefix = 1;
		addInstr("REP: ");
		break;
	    case 0xF4: addInstr("hlt"); return instruction;
	    case 0xF5: addInstr("cmc"); return instruction;
	    case 0xF6: /* Group 3 Eb */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("test "); EbIb(); return instruction;
		case 0x01: invalidOpcode(); return instruction;
		case 0x02: addInstr("not "); Eb(); return instruction;
		case 0x03: addInstr("neg "); Eb(); return instruction;
		case 0x04: addInstr("mul AL, "); Eb(); return instruction;
		case 0x05: addInstr("imul AL, "); Eb(); return instruction;
		case 0x06: addInstr("div AL, "); Eb(); return instruction;
		case 0x07: addInstr("idiv AL, "); Eb(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}

	    case 0xF7: /* GROUP3 Ev */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("test "); EvIv(); return instruction;
		case 0x01: invalidOpcode(); return instruction;
		case 0x02: addInstr("not "); Ev(); return instruction;
		case 0x03: addInstr("neg "); Ev(); return instruction;
		case 0x04: addInstr("mul "); eAXEv(); return instruction;
		case 0x05: addInstr("imul "); eAXEv(); return instruction;
		case 0x06: addInstr("div "); eAXEv(); return instruction;
		case 0x07: addInstr("idiv "); eAXEv(); return instruction;
		default: panic("debugger: invalid opcode\n"); return instruction;
		}
	    case 0xF8: addInstr("clc"); return instruction;
	    case 0xF9: addInstr("stc"); return instruction;
	    case 0xFA: addInstr("cli"); return instruction;
	    case 0xFB: addInstr("sti"); return instruction;
	    case 0xFC: addInstr("cld"); return instruction;
	    case 0xFD: addInstr("std"); return instruction;
	    case 0xFE: /* GROUP4 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("inc "); Eb(); return instruction;
		case 0x01: addInstr("dec "); Eb(); return instruction;
		default: invalidOpcode(); return instruction;
		}

	    case 0xFF: /* GROUP 5 */
		mod_rm_byte = peek_byte();
		BX_DECODE_MODRM(mod_rm_byte);
		switch (opcode) {
		case 0x00: addInstr("inc "); Ev(); return instruction;
		case 0x01: addInstr("dec "); Ev(); return instruction;
		case 0x02: addInstr("call "); Ev(); return instruction;
		case 0x03: addInstr("call "); Ep(); return instruction;
		case 0x04: addInstr("jmp "); Ev(); return instruction;
		case 0x05: addInstr("jmp "); Ep(); return instruction;
		case 0x06: addInstr("push "); Ev(); return instruction;
		default: invalidOpcode(); return instruction;
		}

	    default: /* only invalid instructions left */
		panic("debugger: invalid opcode : " + next_byte);
		return instruction;
	    } /* switch (next_byte) */
	} /* for (byte_count... */
	invalidOpcode();

	return "???";
    }






    // Floating point stuff
    void ST_STi() {Debug.throwError("*** ST_STi() unfinished ***");}
    void STi_ST() {Debug.throwError("*** STi_ST() unfinished ***");}
    void
	STi() {Debug.throwError("*** STi() unfinished ***");}


    // Debug, Test, and Control Register stuff
    void
	RdCd() {Debug.throwError("*** RdCd() unfinished ***");}
    void
	RdDd() {Debug.throwError("*** RdDd() unfinished ***");}
    void
	CdRd() {Debug.throwError("*** CdRd() unfinished ***");}
    void
	DdRd() {Debug.throwError("*** DdRd() unfinished ***");}
    void
	RdTd() {Debug.throwError("*** RdTd() unfinished ***");}
    void
	TdRd() {Debug.throwError("*** TdRd() unfinished ***");}


    // Other un-implemented operand signatures
    void
	Ms() {Debug.throwError("*** Ms() unfinished ***");}
    void
	XBTS() {Debug.throwError("*** XBTS() unfinished ***");}
    void
	IBTS() {Debug.throwError("*** IBTS() unfinished ***");}
    void
	Mp() {Debug.throwError("*** Mp() unfinished ***");}
    void
	GvMa() {Debug.throwError("*** GvMa() unfinished ***");}
    void
	EwRw() {Debug.throwError("*** EwRw() unfinished ***");}
    void
	YbDX() {Debug.throwError("*** YbDX() unfinished ***");}
    void
	YvDX() {Debug.throwError("*** YvDX() unfinished ***");}
    void
	DXXb() {Debug.throwError("*** DXXb() unfinished ***");}
    void
	DXXv() {Debug.throwError("*** DXXv() unfinished ***");}
    void
	ALOb() {Debug.throwError("*** ALOb() unfinished ***");}

    void
	eAXOv()
    {
	if (db_32bit_opsize) {
	    addInstr("EAX, ");
	}
	else {
	    addInstr("AX, ");
	}

	if (db_32bit_addrsize) {
	    int imm32;

	    imm32 = fetch_dword();
	    addInstr("["+toHexInt(imm32)+"]");
	}
	else {
	    int imm16;

	    imm16 = fetch_word();
	    addInstr("["+ toHexInt(imm16)+"]");
	}
    }

    void
	OveAX()
    {
	if (db_32bit_addrsize) {
	    int imm32;

	    imm32 = fetch_dword();
	    addInstr("["+toHexInt(imm32)+"]");
	}
	else {
	    int imm16;

	    imm16 = fetch_word();
	    addInstr("["+ toHexInt(imm16)+"]");
	}

	if (db_32bit_opsize) {
	    addInstr("EAX");
	}
	else {
	    addInstr("AX");
	}

    }

    void
	ObAL() {Debug.throwError("*** ObAL() unfinished ***");}

    void
	XvYv() {Debug.throwError("*** XvYv() unfinished ***");}
    void
	YbAL() {Debug.throwError("*** YbAL() unfinished ***");}
    void
	ALXb() {Debug.throwError("*** ALXb() unfinished ***");}
    void
	eAXXv() { Debug.throwError("*** eAXXv() unfinished ***"); }
    void
	Es() {Debug.throwError("*** Es() unfinished ***");}
    void
	Ea() {Debug.throwError("*** Ea() unfinished ***");}
    void
	Et() {Debug.throwError("*** Et() unfinished ***");}
    void
	Ed() {Debug.throwError("*** Ed() unfinished ***");}
    void
	El() {Debug.throwError("*** El() unfinished ***");}
    void
	Eq() {Debug.throwError("*** Eq() unfinished ***");}

    void GvEb() {
	if (db_32bit_opsize)
	    decode_gxex(BX_GENERAL_32BIT_REG, BX_GENERAL_8BIT_REG);
	else
	    decode_gxex(BX_GENERAL_16BIT_REG, BX_GENERAL_8BIT_REG);
    }


    void
	Av()
    {
	if (db_32bit_opsize) {
	    int imm32;
	    imm32 = fetch_dword();
	    addInstr("["+toHexInt(imm32)+"]");
	}
	else {
	    int imm16;
	    imm16 = fetch_word();
	    addInstr("["+toHexInt(imm16)+"]");
	}
    }

    void
	Eb()
    {
	decode_exgx(BX_GENERAL_8BIT_REG, BX_NO_REG_TYPE);
    }

    void
	Eb1()
    {
	decode_exgx(BX_GENERAL_8BIT_REG, BX_NO_REG_TYPE);
	addInstr(", 1");
    }

    void
	Ev1()
    {
	if (db_32bit_opsize)
	    decode_exgx(BX_GENERAL_32BIT_REG, BX_NO_REG_TYPE);
	else
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
	addInstr(", 1");
    }



    void
	Iw()
    {
	int imm16;

	imm16 = fetch_word();
	addInstr("#"+toHexInt(imm16));
    }



    void
	EbGb()
    {
	decode_exgx(BX_GENERAL_8BIT_REG, BX_GENERAL_8BIT_REG);
    }

    void
	EvGv()
    {
	if (db_32bit_opsize)
	    decode_exgx(BX_GENERAL_32BIT_REG, BX_GENERAL_32BIT_REG);
	else
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_GENERAL_16BIT_REG);
    }

    void
	GbEb()
    {
	decode_gxex(BX_GENERAL_8BIT_REG, BX_GENERAL_8BIT_REG);
    }

    void
	GvEv()
    {
	if (db_32bit_opsize)
	    decode_gxex(BX_GENERAL_32BIT_REG, BX_GENERAL_32BIT_REG);
	else
	    decode_gxex(BX_GENERAL_16BIT_REG, BX_GENERAL_16BIT_REG);
    }


    void
	Ew()
    {
	decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
    }

    void
	GvEw()
    {
	if (db_32bit_opsize)
	    decode_gxex(BX_GENERAL_32BIT_REG, BX_GENERAL_16BIT_REG);
	else
	    decode_gxex(BX_GENERAL_16BIT_REG, BX_GENERAL_16BIT_REG);
    }


    void Jv() {
	if (db_32bit_opsize) {
	    int imm32;
	    
	    imm32 = fetch_dword();
	    addInstr("+#"+toHexInt(imm32) + "  (->"+toHexInt(codePosition+imm32)+")");
	}
	else
	    {
		int imm16;

		imm16 = fetch_word();
		addInstr("+#"+toHexInt(imm16));
	    }
    }


    void
	EvIb()
    {
	int imm8;

	if (db_32bit_opsize) {
	    decode_exgx(BX_GENERAL_32BIT_REG, BX_NO_REG_TYPE);
	    imm8 = fetch_byte();
	    addInstr(", #"+ toHexInt(imm8));
	}
	else {
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
	    imm8 = fetch_byte();
	    addInstr(", #"+ toHexInt(imm8));
	}
    }


    void
	Iv()
    {
	if (db_32bit_opsize) {
	    int imm32;

	    imm32 = fetch_dword();
	    addInstr("#"+ toHexInt(imm32));
	}
	else {
	    int imm16;

	    imm16 = fetch_word();
	    addInstr("#"+ toHexInt(imm16));
	}
    }


    void
	Ib()
    {
	int imm8;

	imm8 = fetch_byte();
	addInstr("#"+toHexInt(imm8));
    }


    void
	Jb()
    {
	int imm8;

	imm8 = fetch_byte();
	addInstr("+#"+toHexInt(imm8));
    }

    void
	EbIb()
    {
	int imm8;

	decode_exgx(BX_GENERAL_8BIT_REG, BX_NO_REG_TYPE);
	imm8 = fetch_byte();
	addInstr(", #"+toHexInt(imm8));
    }

    void
	EvIv()
    {
	int imm16;

	if (db_32bit_opsize) {
	    int imm32;

	    decode_exgx(BX_GENERAL_32BIT_REG, BX_NO_REG_TYPE);
	    imm32 = fetch_dword();
	    addInstr(", #"+toHexInt(imm32));
	}
	else {
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
	    imm16 = fetch_word();
	    addInstr(", #"+toHexInt(imm16));
	}
    }

    void
	EwSw()
    {
	decode_exgx(BX_GENERAL_16BIT_REG, BX_SEGMENT_REG);
    }

    void
	GvM()
    {
	if (db_32bit_opsize)
	    decode_gxex(BX_GENERAL_32BIT_REG, BX_GENERAL_32BIT_REG);
	else
	    decode_gxex(BX_GENERAL_16BIT_REG, BX_GENERAL_16BIT_REG);
    }

    void
	SwEw()
    {
	decode_gxex(BX_SEGMENT_REG, BX_GENERAL_16BIT_REG);
    }

    void
	Ev()
    {
	if (db_32bit_opsize) {
	    decode_exgx(BX_GENERAL_32BIT_REG, BX_NO_REG_TYPE);
	}
	else {
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
	}
    }

    void
	Ap()
    {

	if (db_32bit_opsize) {
	    int imm32;
	    int cs_selector;

	    imm32 = fetch_dword();
	    cs_selector = fetch_word();
	    addInstr(toHexInt(cs_selector)+":"+toHexInt(imm32));
	}
	else
	    {
		int imm16;
		int cs_selector;

		imm16 = fetch_word();
		cs_selector = fetch_word();
		addInstr(toHexInt(cs_selector)+":"+toHexInt(imm16));
	    }
    }


    void
	XbYb()
    {
	String esi, edi;
	String seg;

	if (db_32bit_addrsize) {
	    esi = "ESI";
	    edi = "EDI";
	}
	else {
	    esi = "SI";
	    edi = "DI";
	}

	if(seg_override != null)
	    seg = seg_override;
	else
	    seg = "DS";

	addInstr("ES:["+edi+"], "+seg+":["+esi+"]");
    }


    void
	YveAX()
    {
	String eax, edi;

	if (db_32bit_opsize)
	    eax = "EAX";
	else
	    eax = "AX";

	if (db_32bit_addrsize)
	    edi = "EDI";
	else
	    edi = "DI";

	addInstr("ES:["+edi+"], "+eax);
    }

    void
	GvMp()
    {
	if (db_32bit_opsize)
	    decode_gxex(BX_GENERAL_32BIT_REG, BX_GENERAL_32BIT_REG);
	else
	    decode_gxex(BX_GENERAL_16BIT_REG, BX_GENERAL_16BIT_REG);
    }

    void
	eAXEv()
    {
	if (db_32bit_opsize) {
	    addInstr("EAX, ");
	    decode_gxex(BX_NO_REG_TYPE, BX_GENERAL_32BIT_REG);
	}
	else {
	    addInstr("AX, ");
	    decode_gxex(BX_NO_REG_TYPE, BX_GENERAL_16BIT_REG);
	}
    }

    void
	Ep()
    {
	if (db_32bit_opsize) {
	    decode_exgx(BX_GENERAL_32BIT_REG, BX_NO_REG_TYPE);
	}
	else {
	    decode_exgx(BX_GENERAL_16BIT_REG, BX_NO_REG_TYPE);
	}
    }

    void
	eAX()
    {
	if (db_32bit_opsize) {
	    addInstr("EAX");
	}
	else {
	    addInstr("AX");
	}
    }

    void
	eCX()
    {
	if (db_32bit_opsize) {
	    addInstr("ECX");
	}
	else {
	    addInstr("CX");
	}
    }

    void
	eDX()
    {
	if (db_32bit_opsize) {
	    addInstr("EDX");
	}
	else {
	    addInstr("DX");
	}
    }

    void
	eBX()
    {
	if (db_32bit_opsize) {
	    addInstr("EBX");
	}
	else {
	    addInstr("BX");
	}
    }

    void
	eSP()
    {
	if (db_32bit_opsize) {
	    addInstr("ESP");
	}
	else {
	    addInstr("SP");
	}
    }

    void
	eBP()
    {
	if (db_32bit_opsize) {
	    addInstr("EBP");
	}
	else {
	    addInstr("BP");
	}
    }

    void
	eSI()
    {
	if (db_32bit_opsize) {
	    addInstr("ESI");
	}
	else {
	    addInstr("SI");
	}
    }

    void
	eDI()
    {
	if (db_32bit_opsize) {
	    addInstr("EDI");
	}
	else {
	    addInstr("DI");
	}
    }


    /*
     * decode
     */

    void decode_exgx(int modrm_reg_type, int reg_type) {
	int modrm, ttt;
	
	modrm = fetch_byte();
	decode_ex(modrm, modrm_reg_type);
	ttt = (modrm >> 3) & 0x07;
	
	if (reg_type != BX_NO_REG_TYPE) {
	    addInstr(", ");
	    out_reg_name(ttt, reg_type);
	}
    }
    


    void decode_gxex(int reg_type, int modrm_reg_type) {
	int modrm, ttt;
	
	modrm = fetch_byte();
	ttt = (modrm >> 3) & 0x07;
	
	if (reg_type != BX_NO_REG_TYPE) {
	  out_reg_name(ttt, reg_type);
	  addInstr(", ");
	}
      
	decode_ex(modrm, modrm_reg_type);
    }
    

    void decode_ex(int modrm, int modrm_reg_type) {
	int mod_rm_addr;
	String mod_rm_seg_reg;
	
	int  mod, ttt, rm;
	int  displ8;
	int displ16;
	
	mod = modrm >> 6;
	ttt = (modrm >> 3) & 0x07;
	rm = modrm & 0x07;

	if (db_32bit_addrsize) {
	    int sib, ss, index, base;
	    int displ32;
	    
	    /* use 32bit addressing modes.  orthogonal base & index registers,
	       scaling available, etc. */
	    if (printDecoding) 
		addInstr("|MOD"+mod+"|REG"+ttt+"|RM"+rm+"| ");
	    
	    
	    if (mod == 3) { /* mod, reg, reg */
		out_reg_name(rm, modrm_reg_type);
	    }
	    else { /* mod != 3 */
		if (rm != 4) { /* rm != 100b, no s-i-b byte */
		    // one byte modrm
		    switch (mod) {
		    case 0:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = "DS";
			if (rm == 5) { /* no reg, 32-bit displacement */
			    mod_rm_addr = fetch_dword();
			    addInstr( mod_rm_seg_reg+":"+ mod_rm_addr);
			}
			else {
			    addInstr( mod_rm_seg_reg+":["+general_32bit_reg_name[rm]+"]");
			}
			break;
		    case 1:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = sreg_mod01_rm32[rm];
			/* reg, 8-bit displacement, sign extend */
			displ8 = fetch_byte();
			addInstr(mod_rm_seg_reg+":["+general_32bit_reg_name[rm]+" + "+displ8+"]");
			break;
		    case 2:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = sreg_mod10_rm32[rm];
			/* reg, 32-bit displacement */
			displ32 = fetch_dword();
			addInstr(mod_rm_seg_reg+":["+general_32bit_reg_name[rm]+" + "+displ32+"]");
			break;
		    } /* switch (mod) */
		} /* if (rm != 4) */
		else { /* rm == 4, s-i-b byte follows */
		    sib = fetch_byte();
		    ss = sib >> 6;
		    index = (sib >> 3) & 0x07;
		    base = sib & 0x07;
		    addInstr("|SS"+ss+"|IND"+index+"|BASE"+base+"| ");
		    switch (mod) {
		    case 0:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = sreg_mod00_base32[base];
			addInstr(mod_rm_seg_reg+":[");
			if (base != 5)
			    addInstr(general_32bit_reg_name[base]);
			else {
			    displ32 = fetch_dword();
			    addInstr(""+displ32);
			}

			if (index != 4)
			    addInstr(" + "+index_name32[index]+"<<"+ ss);
			addInstr("]");
			break;
		    case 1:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = sreg_mod01_base32[base];
			displ8 = fetch_byte();
			addInstr(mod_rm_seg_reg+":["+ general_32bit_reg_name[base]);

			if (index != 4)
			    addInstr(" + "+index_name32[index]+"<<"+ ss);
			addInstr(" + "+ toHexInt(displ8));
			break;
		    case 2:
			if (seg_override != null)
			    mod_rm_seg_reg = seg_override;
			else
			    mod_rm_seg_reg = sreg_mod10_base32[base];
			displ32 = fetch_dword();
			addInstr(mod_rm_seg_reg+":["+ general_32bit_reg_name[base]);

			if (index != 4)
			    addInstr(" + "+index_name32[index]+"<<"+ss);
			addInstr(" + "+toHexInt(displ32)+"]");
			break;
		    }
		} /* s-i-b byte follows */
	    } /* if (mod != 3) */
	}

	else {
	    /* 16 bit addressing modes. */

	    switch (mod) {
	    case 0:
		if (seg_override != null)
		    mod_rm_seg_reg = seg_override;
		else
		    mod_rm_seg_reg = sreg_mod00_rm16[rm];
		switch (rm) {
		case 0: // DS:[BX+SI]
		    addInstr(mod_rm_seg_reg+":[BX+SI]");
		    break;
		case 1: // DS:[BX+DI]
		    addInstr(mod_rm_seg_reg+":[BX+DI]");
		    break;
		case 2: // SS:[BP+SI]
		    addInstr(mod_rm_seg_reg+":[BP+SI]");
		    break;
		case 3: // SS:[BP+DI]
		    addInstr(mod_rm_seg_reg+":[BP+DI]");
		    break;
		case 4: // DS:[SI]
		    addInstr(mod_rm_seg_reg+":[SI]");
		    break;
		case 5: // DS:[DI]
		    addInstr(mod_rm_seg_reg+":[DI]");
		    break;
		case 6: // DS:d16
		    displ16 = fetch_word();
		    addInstr(mod_rm_seg_reg+":"+toHexInt(displ16));
		    break;
		case 7: // DS:[BX]
		    addInstr(mod_rm_seg_reg+":[BX]");
		    break;
		}
		break;

	    case 1:
		displ8 = fetch_byte();
		if (seg_override != null)
		    mod_rm_seg_reg = seg_override;
		else
		    mod_rm_seg_reg = sreg_mod01_rm16[rm];
		switch (rm) {
		case 0: // DS:[BX+SI+d8]
		    addInstr(mod_rm_seg_reg+":[BX+SI+"+toHexInt(displ8)+"]");
		    break;
		case 1: // DS:[BX+DI+d8]
		    addInstr(mod_rm_seg_reg+":[BX+DI+"+toHexInt(displ8)+"]");
		    break;
		case 2: // SS:[BP+SI+d8]
		    addInstr(mod_rm_seg_reg+":[BP+SI+"+toHexInt(displ8)+"]");
		    break;
		case 3: // SS:[BP+DI+d8]
		    addInstr(mod_rm_seg_reg+":[BP+DI+"+toHexInt(displ8)+"]");
		    break;
		case 4: // DS:[SI+d8]
		    addInstr(mod_rm_seg_reg+":[SI+"+toHexInt(displ8)+"]");
		    break;
		case 5: // DS:[DI+d8]
		    addInstr(mod_rm_seg_reg+":[DI+"+toHexInt(displ8)+"]");
		    break;
		case 6: // SS:[BP+d8]
		    addInstr(mod_rm_seg_reg+":[BP+"+toHexInt(displ8)+"]");
		    break;
		case 7: // DS:[BX+d8]
		    addInstr(mod_rm_seg_reg+":[BX+"+toHexInt(displ8)+"]");
		    break;
		}
		break;

	    case 2:
		displ16 = fetch_word();
		if (seg_override != null)
		    mod_rm_seg_reg = seg_override;
		else
		    mod_rm_seg_reg = sreg_mod10_rm16[rm];
		switch (rm) {
		case 0: // DS:[BX+SI+d16]
		    addInstr(mod_rm_seg_reg+":[BX+SI+"+toHexInt(displ16)+"]");
		    break;
		case 1: // DS:[BX+DI+d16]
		    addInstr(mod_rm_seg_reg+":[BX+DI+"+toHexInt(displ16)+"]");
		    break;
		case 2: // SS:[BP+SI+d16]
		    addInstr(mod_rm_seg_reg+":[BP+SI+"+toHexInt(displ16)+"]");
		    break;
		case 3: // SS:[BP+DI+d16]
		    addInstr(mod_rm_seg_reg+":[BP+DI+"+toHexInt(displ16)+"]");
		    break;
		case 4: // DS:[SI+d16]
		    addInstr(mod_rm_seg_reg+":[SI+"+toHexInt(displ16)+"]");
		    break;
		case 5: // DS:[DI+d16]
		    addInstr(mod_rm_seg_reg+":[DI+"+toHexInt(displ16)+"]");
		    break;
		case 6: // SS:[BP+d16]
		    addInstr(mod_rm_seg_reg+":[BP+"+toHexInt(displ16)+"]");
		    break;
		case 7: // DS:[BX+d16]
		    addInstr(mod_rm_seg_reg+":[BX+"+toHexInt(displ16)+"]");
		    break;
		}
		break;

	    case 3: /* mod, reg, reg */
		out_reg_name(rm, modrm_reg_type);
		break;

	    } /* switch (mod) ... */
	}
    }


    ////////

    void out_reg_name(int reg, int reg_type)
    {
	switch (reg_type) {
	case BX_SEGMENT_REG:
	    addInstr(segment_name[reg]);
	    break;
	case BX_GENERAL_8BIT_REG:
	    addInstr(general_8bit_reg_name[reg]);
	    break;
	case BX_GENERAL_16BIT_REG:
	    addInstr(general_16bit_reg_name[reg]);
	    break;
	case BX_GENERAL_32BIT_REG:
	    addInstr(general_32bit_reg_name[reg]);
	    break;
	}
    }

    void out_16bit_base(int base)
    {
	if (base_name16[base] != null)
	    addInstr("["+  base_name16[base] + "]");
    }

    void
	out_16bit_index(int index)
    {
	if (index_name16[index] != null)
	    addInstr("["+ index_name16[index]+"]");
    }


  String toHexInt(int value) {
    String hex = Long.toHexString(value & 0xffffffffL);         
    return  "00000000".substring(Math.min(hex.length(),8)) + hex + " "; 
  }



    
}
