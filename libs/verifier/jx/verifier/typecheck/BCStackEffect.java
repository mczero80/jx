package jx.verifier.typecheck;

import jx.verifier.bytecode.*;
import jx.verifier.VerifyException;
import java.util.Vector;
import jx.classfile.constantpool.*;
import jx.classfile.ClassData;


public final class BCStackEffect {

    // which types are popped from the stack when executing this bc
    public static final TCTypes POP[][] = { 
	/*0x00 nop*/ {},
	/*0x01 aconst_null*/ {},
	/*0x02 iconst_m1*/ {},
	/*0x03 iconst_0*/ {},
	/*0x04 iconst_1*/ {},
	/*0x05 iconst_2*/ {},
	/*0x06 iconst_3*/ {},
	/*0x07 iconst_4*/ {},
	/*0x08 iconst_5*/ {},
	/*0x09 lconst_0*/ {},
	/*0x0a lconst_1*/ {},
	/*0x0b fconst_0*/ {},
	/*0x0c fconst_1*/ {},
	/*0x0d fconst_2*/ {},
	/*0x0e dconst_0*/ {},
	/*0x0f dconst_1*/ {},
	/*0x10 bipush*/ {},
	/*0x11 sipush*/ {},
	/*0x12 ldc*/ {},
	/*0x13 ldc_w*/ {},
	/*0x14 ldc2_w*/ {},
	/*0x15 iload*/ {},
	/*0x16 lload*/ {},
	/*0x17 fload*/ {},
	/*0x18 dload*/ {},
	/*0x19 aload*/ {},
	/*0x1a iload_0*/ {},
	/*0x1b iload_1*/ {},
	/*0x1c iload_2*/ {},
	/*0x1d iload_3*/ {},
	/*0x1e lload_0*/ {},
	/*0x1f lload_1*/ {},
	/*0x20 lload_2*/ {},
	/*0x21 lload_3*/ {},
	/*0x22 fload_0*/ {},
	/*0x23 fload_1*/ {},
	/*0x24 fload_2*/ {},
	/*0x25 fload_3*/ {},
	/*0x26 dload_0*/ {},
	/*0x27 dload_1*/ {},
	/*0x28 dload_2*/ {},
	/*0x29 dload_3*/ {},
	/*0x2a aload_0*/ {},
	/*0x2b aload_1*/ {},
	/*0x2c aload_2*/ {},
	/*0x2d aload_3*/ {},
	/*0x2e iaload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x2f laload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x30 faload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x31 daload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x32 aaload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x33 baload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x34 caload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x35 saload*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT)},
	/*0x36 istore*/ {new TCTypes(TCTypes.INT)},
	/*0x37 lstore*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x38 fstore*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x39 dstore*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x3a astore*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x3b istore_0*/ {new TCTypes(TCTypes.INT)},
	/*0x3c istore_1*/ {new TCTypes(TCTypes.INT)},
	/*0x3d istore_2*/ {new TCTypes(TCTypes.INT)},
	/*0x3e istore_3*/ {new TCTypes(TCTypes.INT)},
	/*0x3f lstore_0*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x40 lstore_1*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x41 lstore_2*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x42 lstore_3*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x43 fstore_0*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x44 fstore_1*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x45 fstore_2*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x46 fstore_3*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x47 dstore_0*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x48 dstore_1*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x49 dstore_2*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x4a dstore_3*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x4b astore_0*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x4c astore_1*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x4d astore_2*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x4e astore_3*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x4f iastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x50 lastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x51 fastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.FLOAT)},
	/*0x52 dastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), 
			  new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x53 aastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.ANY_REF)},
	/*0x54 bastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x55 castore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x56 sastore*/ {new TCTypes(TCTypes.ARRAY_REF), new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x57 pop*/ {new TCTypes(TCTypes.ANY)},
	/*0x58 pop2*/ {new TCTypes(TCTypes.ANY), new TCTypes(TCTypes.ANY)},
	/*0x59 dup*/ {},
	/*0x5a dup_x1*/ {},
	/*0x5b dup_x2*/ {},
	/*0x5c dup2*/ {},
	/*0x5d dup2_x1*/ {},
	/*0x5e dup2_x2*/ {},
	/*0x5f swap*/ {},
	/*0x60 iadd*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x61 ladd*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x62 fadd*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x63 dadd*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
		       new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x64 isub*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x65 lsub*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x66 fsub*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x67 dsub*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
		       new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x68 imul*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x69 lmul*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x6a fmul*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x6b dmul*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
		       new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x6c idiv*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
 	/*0x6d ldiv*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x6e fdiv*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x6f ddiv*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
		       new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x70 irem*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x71 lrem*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x72 frem*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x73 drem*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
		       new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x74 ineg*/ {new TCTypes(TCTypes.INT)},
	/*0x75 lneg*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x76 fneg*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x77 dneg*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x78 ishl*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x79 lshl*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.INT)},
	/*0x7a ishr*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x7b lshr*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.INT)},
	/*0x7c iushr*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x7d lushr*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.INT)},
	/*0x7e iand*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x7f land*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x80 ior*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x81 lor*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x82 ixor*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0x83 lxor*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x84 iinc*/ {},
	/*0x85 i2l*/ {new TCTypes(TCTypes.INT)},
	/*0x86 i2f*/ {new TCTypes(TCTypes.INT)},
	/*0x87 i2d*/ {new TCTypes(TCTypes.INT)},
	/*0x88 l2i*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x89 l2f*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x8a l2d*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x8b f2i*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x8c f2l*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x8d f2d*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x8e d2i*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x8f d2l*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x90 d2f*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x91 i2b*/ {new TCTypes(TCTypes.INT)},
	/*0x92 i2c*/ {new TCTypes(TCTypes.INT)},
	/*0x93 i2s*/ {new TCTypes(TCTypes.INT)},
	/*0x94 lcmp*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U), 
		       new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x95 fcmpl*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x96 fcmpg*/ {new TCTypes(TCTypes.FLOAT), new TCTypes(TCTypes.FLOAT)},
	/*0x97 dcmpl*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U), 
			new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x98 dcmpg*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U),
			new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x99 ifeq*/ {new TCTypes(TCTypes.INT)},
	/*0x9a ifne*/ {new TCTypes(TCTypes.INT)},
	/*0x9b iflt*/ {new TCTypes(TCTypes.INT)},
	/*0x9c ifge*/ {new TCTypes(TCTypes.INT)},
	/*0x9d ifgt*/ {new TCTypes(TCTypes.INT)},
	/*0x9e ifle*/ {new TCTypes(TCTypes.INT)},
	/*0x9f if_icmpeq*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa0 if_icmpne*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa1 if_icmplt*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa2 if_icmpge*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa3 if_icmpgt*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa4 if_icmple*/ {new TCTypes(TCTypes.INT), new TCTypes(TCTypes.INT)},
	/*0xa5 if_acmpeq*/ {new TCTypes(TCTypes.ANY_REF), new TCTypes(TCTypes.ANY_REF)},
	/*0xa6 if_acmpne*/ {new TCTypes(TCTypes.ANY_REF), new TCTypes(TCTypes.ANY_REF)},
	/*0xa7 goto*/ {},
	/*0xa8 jsr*/ {},
	/*0xa9 ret*/ {},
	/*0xaa tableswitch*/ {new TCTypes(TCTypes.INT)},
	/*0xab lookupswitch*/ {new TCTypes(TCTypes.INT)},
	/*0xac ireturn*/ {new TCTypes(TCTypes.INT)},
	/*0xad lreturn*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0xae freturn*/ {new TCTypes(TCTypes.FLOAT)},
	/*0xaf dreturn*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0xb0 areturn*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xb1 return*/ {},
	/*0xb2 getstatic*/ {},
	/*0xb3 putstatic*/ {},
	/*0xb4 getfield*/ {},
	/*0xb5 putfield*/ {},
	/*0xb6 invokevirtual*/ {},
	/*0xb7 invokespecial*/ {},
	/*0xb8 invokestatic*/ {},
	/*0xb9 invokeinterface*/ {},
	/*0xba xxxunusedxxx*/ {},
	/*0xbb new*/ {},
	/*0xbc newarray*/ {new TCTypes(TCTypes.INT)},
	/*0xbd anewarray*/ {new TCTypes(TCTypes.INT)},
	/*0xbe arraylength*/ {new TCTypes(TCTypes.ARRAY_REF)},
	/*0xbf athrow*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc0 checkcast*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc1 instanceof*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc2 monitorenter*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc3 monitorexit*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc4 wide*/ {},
	/*0xc5 multianewarray*/ {},
	/*0xc6 ifnull*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc7 ifnonnull*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc8 goto_w*/ {},
	/*0xc9 jsr_w*/ {} 
    };


    // which types are pushed onto the stack when executing this bc
    public static final TCTypes PUSH[][] = {
	/*0x00 nop*/ {},
	/*0x01 aconst_null*/ {new TCObjectTypes(TCObjectTypes.nullString)},
	/*0x02 iconst_m1*/ {new TCTypes(TCTypes.INT)},
	/*0x03 iconst_0*/ {new TCTypes(TCTypes.INT)},
	/*0x04 iconst_1*/ {new TCTypes(TCTypes.INT)},
	/*0x05 iconst_2*/ {new TCTypes(TCTypes.INT)},
	/*0x06 iconst_3*/ {new TCTypes(TCTypes.INT)},
	/*0x07 iconst_4*/ {new TCTypes(TCTypes.INT)},
	/*0x08 iconst_5*/ {new TCTypes(TCTypes.INT)},
	/*0x09 lconst_0*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x0a lconst_1*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x0b fconst_0*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x0c fconst_1*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x0d fconst_2*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x0e dconst_0*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x0f dconst_1*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x10 bipush*/ {new TCTypes(TCTypes.INT)},
	/*0x11 sipush*/ {new TCTypes(TCTypes.INT)},
	/*0x12 ldc*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x13 ldc_w*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x14 ldc2_w*/ {new TCTypes(TCTypes.ANY),new TCTypes(TCTypes.ANY)},
	/*0x15 iload*/ {new TCTypes(TCTypes.INT)},
	/*0x16 lload*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x17 fload*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x18 dload*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x19 aload*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x1a iload_0*/ {new TCTypes(TCTypes.INT)},
	/*0x1b iload_1*/ {new TCTypes(TCTypes.INT)},
	/*0x1c iload_2*/ {new TCTypes(TCTypes.INT)},
	/*0x1d iload_3*/ {new TCTypes(TCTypes.INT)},
	/*0x1e lload_0*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x1f lload_1*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x20 lload_2*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x21 lload_3*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x22 fload_0*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x23 fload_1*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x24 fload_2*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x25 fload_3*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x26 dload_0*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x27 dload_1*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x28 dload_2*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x29 dload_3*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x2a aload_0*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x2b aload_1*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x2c aload_2*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x2d aload_3*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0x2e iaload*/ {new TCTypes(TCTypes.INT)},
	/*0x2f laload*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x30 faload*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x31 daload*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x32 aaload*/ {new TCTypes(TCTypes.ANY)},
	/*0x33 baload*/ {new TCTypes(TCTypes.INT)},
	/*0x34 caload*/ {new TCTypes(TCTypes.INT)},
	/*0x35 saload*/ {new TCTypes(TCTypes.INT)},//byte, char, short all pushed as ints!
	/*0x36 istore*/ {},
	/*0x37 lstore*/ {},
	/*0x38 fstore*/ {},
	/*0x39 dstore*/ {},
	/*0x3a astore*/ {},
	/*0x3b istore_0*/ {},
	/*0x3c istore_1*/ {},
	/*0x3d istore_2*/ {},
	/*0x3e istore_3*/ {},
	/*0x3f lstore_0*/ {},
	/*0x40 lstore_1*/ {},
	/*0x41 lstore_2*/ {},
	/*0x42 lstore_3*/ {},
	/*0x43 fstore_0*/ {},
	/*0x44 fstore_1*/ {},
	/*0x45 fstore_2*/ {},
	/*0x46 fstore_3*/ {},
	/*0x47 dstore_0*/ {},
	/*0x48 dstore_1*/ {},
	/*0x49 dstore_2*/ {},
	/*0x4a dstore_3*/ {},
	/*0x4b astore_0*/ {},
	/*0x4c astore_1*/ {},
	/*0x4d astore_2*/ {},
	/*0x4e astore_3*/ {},
	/*0x4f iastore*/ {},
	/*0x50 lastore*/ {},
	/*0x51 fastore*/ {},
	/*0x52 dastore*/ {},
	/*0x53 aastore*/ {},
	/*0x54 bastore*/ {},
	/*0x55 castore*/ {},
	/*0x56 sastore*/ {},
	/*0x57 pop*/ {},
	/*0x58 pop2*/ {},
	/*0x59 dup*/ {}, //Special treatment necessary for dups and swap!
	/*0x5a dup_x1*/ {},
	/*0x5b dup_x2*/ {},
	/*0x5c dup2*/ {},
	/*0x5d dup2_x1*/ {},
	/*0x5e dup2_x2*/ {},
	/*0x5f swap*/ {},
	/*0x60 iadd*/ {new TCTypes(TCTypes.INT)},
	/*0x61 ladd*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x62 fadd*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x63 dadd*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x64 isub*/ {new TCTypes(TCTypes.INT)},
	/*0x65 lsub*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x66 fsub*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x67 dsub*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x68 imul*/ {new TCTypes(TCTypes.INT)},
	/*0x69 lmul*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x6a fmul*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x6b dmul*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x6c idiv*/ {new TCTypes(TCTypes.INT)},
 	/*0x6d ldiv*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x6e fdiv*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x6f ddiv*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x70 irem*/ {new TCTypes(TCTypes.INT)},
	/*0x71 lrem*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x72 frem*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x73 drem*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x74 ineg*/ {new TCTypes(TCTypes.INT)},
	/*0x75 lneg*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x76 fneg*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x77 dneg*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x78 ishl*/ {new TCTypes(TCTypes.INT)},
	/*0x79 lshl*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x7a ishr*/ {new TCTypes(TCTypes.INT)},
	/*0x7b lshr*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x7c iushr*/ {new TCTypes(TCTypes.INT)},
	/*0x7d lushr*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x7e iand*/ {new TCTypes(TCTypes.INT)},
	/*0x7f land*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x80 ior*/ {new TCTypes(TCTypes.INT)},
	/*0x81 lor*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x82 ixor*/ {new TCTypes(TCTypes.INT)},
	/*0x83 lxor*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x84 iinc*/ {},
	/*0x85 i2l*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x86 i2f*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x87 i2d*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x88 l2i*/ {new TCTypes(TCTypes.INT)},
	/*0x89 l2f*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x8a l2d*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x8b f2i*/ {new TCTypes(TCTypes.INT)},
	/*0x8c f2l*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x8d f2d*/ {new TCTypes(TCTypes.DOUBLE_L), new TCTypes(TCTypes.DOUBLE_U)},
	/*0x8e d2i*/ {new TCTypes(TCTypes.INT)},
	/*0x8f d2l*/ {new TCTypes(TCTypes.LONG_L), new TCTypes(TCTypes.LONG_U)},
	/*0x90 d2f*/ {new TCTypes(TCTypes.FLOAT)},
	/*0x91 i2b*/ {new TCTypes(TCTypes.INT)},
	/*0x92 i2c*/ {new TCTypes(TCTypes.INT)},
	/*0x93 i2s*/ {new TCTypes(TCTypes.INT)},
	/*0x94 lcmp*/ {new TCTypes(TCTypes.INT)},
	/*0x95 fcmpl*/ {new TCTypes(TCTypes.INT)},
	/*0x96 fcmpg*/ {new TCTypes(TCTypes.INT)},
	/*0x97 dcmpl*/ {new TCTypes(TCTypes.INT)},
	/*0x98 dcmpg*/ {new TCTypes(TCTypes.INT)},
	/*0x99 ifeq*/ {},
	/*0x9a ifne*/ {},
	/*0x9b iflt*/ {},
	/*0x9c ifge*/ {},
	/*0x9d ifgt*/ {},
	/*0x9e ifle*/ {},
	/*0x9f if_icmpeq*/ {},
	/*0xa0 if_icmpne*/ {},
	/*0xa1 if_icmplt*/ {},
	/*0xa2 if_icmpge*/ {},
	/*0xa3 if_icmpgt*/ {},
	/*0xa4 if_icmple*/ {},
	/*0xa5 if_acmpeq*/ {},
	/*0xa6 if_acmpne*/ {},
	/*0xa7 goto*/ {},
	/*0xa8 jsr*/ {},
	/*0xa9 ret*/ {},
	/*0xaa tableswitch*/ {},
	/*0xab lookupswitch*/ {},
	/*0xac ireturn*/ {},
	/*0xad lreturn*/ {},
	/*0xae freturn*/ {},
	/*0xaf dreturn*/ {},
	/*0xb0 areturn*/ {},
	/*0xb1 return*/ {},
	/*0xb2 getstatic*/ {}, //special handling necessary
	/*0xb3 putstatic*/ {},
	/*0xb4 getfield*/ {},
	/*0xb5 putfield*/ {},
	/*0xb6 invokevirtual*/ {},
	/*0xb7 invokespecial*/ {},
	/*0xb8 invokestatic*/ {},
	/*0xb9 invokeinterface*/ {},
	/*0xba xxxunusedxxx*/ {},
	/*0xbb new*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xbc newarray*/ {new TCTypes(TCTypes.ARRAY_REF)},
	/*0xbd anewarray*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xbe arraylength*/ {new TCTypes(TCTypes.INT)},
	/*0xbf athrow*/ {},
	/*0xc0 checkcast*/ {new TCTypes(TCTypes.ANY_REF)},
	/*0xc1 instanceof*/ {new TCTypes(TCTypes.INT)},
	/*0xc2 monitorenter*/ {},
	/*0xc3 monitorexit*/ {},
	/*0xc4 wide*/ {},
	/*0xc5 multianewarray*/ {},//special handling necessary
	/*0xc6 ifnull*/ {},
	/*0xc7 ifnonnull*/ {},
	/*0xc8 goto_w*/ {},
	/*0xc9 jsr_w*/ {}
    };

    static public void simulateBC(TCState state, ByteCode bCode) 
	throws VerifyException {
	int bc = bCode.getOpCode();

	if (bc == ByteCode.JSR || bc == ByteCode.JSR_W) { 
	    jsr(bc, bCode, state); 
	} else if (bc <= 0x11) { //<t>consts, [b|s]ipush
	    genericOp(bc, bCode, state);
	} else if (bc <= 0x14) { //ldcs
	    ldcOp(bc, (BCCPArgOp) bCode, state);
	} else if (bc <= 0x2d) { //<t>loads
	    lVarOp(bc, bCode, state);
	} else if (bc <= 0x35) { //<t>aloads
	    genericOp(bc, bCode, state);
	} else if (bc <= 0x4e) { //<t>stores
	    lVarOp(bc, bCode, state);
	} else if (bc <= 0x56) { //<t>astores
	    genericOp(bc, bCode, state);
	} else if (bc <= 0x5f) { //Stack Ops (pop, dup, swap)
	    stackOp(bc, bCode, state); 
	} else if (bc <= 0xb1) { //AL-ops, casts, cmps, ifs, if_cmps, goto, ret, switches, returns
	    //FEHLER ret muss noch extra behandelt werden!!!
	    genericOp(bc, bCode, state);
	} else if (bc <= 0xb5) { //[get|put][static|field]
	    fieldOp(bc, (BCCPArgOp) bCode, state);
  	} else if (bc <= 0xb9) { //invokes
	    invoke(bc, (BCCPArgOp) bCode, state);
	} else if (bc <= 0xba) { //xxxUNUSEDxxx
	    throw new VerifyException("BCStackEffect.simulateBC: Bytecode " +bCode+"found!");
	} else if (bc <= 0xbd) { //<a>new<array>
	    objectCreationOp(bc, bCode, state);
	} else if (bc <= 0xc4) { //arraylength, athrow, checkcast, instanceof, monitorenter/exit, wide
	    genericOp(bc, bCode, state);
	} else if (bc <= 0xc5) { //multianewarray
	    objectCreationOp(bc, bCode, state);
	} else { //if<non>null, goto_w
	    genericOp(bc, bCode, state);
	}

    }

    
    static private void notUpperType(TCTypes type) throws VerifyException {
	if (type.getType() == TCTypes.DOUBLE_U ||
	    type.getType() == TCTypes.LONG_U ) {
	    throw new VerifyException("StackOp used on only one word of two-word Datatype ("
				      + type + ")!");
	}
    }
    static private void notLowerType(TCTypes type) throws VerifyException {
	if (type.getType() == TCTypes.DOUBLE_L ||
	    type.getType() == TCTypes.LONG_L ) {
	    throw new VerifyException("StackOp used on only one word of two-word Datatype ("
				      + type + ")!");
	}
    }

    static private void notDoubleType(TCTypes type) throws VerifyException {
	notLowerType(type);
	notUpperType(type);
    }

    //Stack Ops (pop, dup, swap)
    static private void stackOp(int bc, ByteCode bCode, TCState state) 
	throws VerifyException{

	TCTypes w1=null, w2=null, w3=null, w4=null;
	switch (bc) { //pops
	case ByteCode.POP2:
	    state.TCgetStack().TCpop();
	    state.TCgetStack().TCpop();
	    break;
	case ByteCode.POP:
	    notDoubleType(state.TCgetStack().TCpop());
	    break;
	case ByteCode.DUP:
	    break;
	case ByteCode.DUP_X1:
	case ByteCode.DUP2:
	case ByteCode.SWAP:
	    w1 = state.TCgetStack().TCpop();
	    w2 = state.TCgetStack().TCpop();
	    break;
	case ByteCode.DUP_X2:
	case ByteCode.DUP2_X1:
	    w1 = state.TCgetStack().TCpop();
	    w2 = state.TCgetStack().TCpop();
	    w3 = state.TCgetStack().TCpop();
	    break;
	case ByteCode.DUP2_X2:
	    w1 = state.TCgetStack().TCpop();
	    w2 = state.TCgetStack().TCpop();
	    w3 = state.TCgetStack().TCpop();
	    w4 = state.TCgetStack().TCpop();
	    break;
	default:
	    throw new Error("Internal Error: BCStackEffect.stackOp called with wrong bytecode! - " + 
			       Integer.toHexString(bc) + " - "+ bCode);
	}
	switch (bc) { //pushs
	case ByteCode.POP:
	case ByteCode.POP2:
	    break;
	case ByteCode.DUP:
	    notDoubleType(state.TCgetStack().TCpeek());
	    state.TCgetStack().push(state.TCgetStack().TCpeek(), bCode.getAddress());
	    break;
	case ByteCode.DUP_X1:
	    notDoubleType(w1);
	    notDoubleType(w2);
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    break;
	case ByteCode.DUP_X2:
	    notDoubleType(w1);
	    notDoubleType(w2);
	    notDoubleType(w3);
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w3, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    break;
	case ByteCode.DUP2:
	    notLowerType(w1);
	    notUpperType(w2);
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    break;
	case ByteCode.DUP2_X1:
	    notLowerType(w1);
	    notUpperType(w2);
	    notDoubleType(w3);
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w3, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    break;
	case ByteCode.DUP2_X2:
	    notLowerType(w1);
	    notUpperType(w2);
	    notLowerType(w3);
	    notUpperType(w4);
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w4, bCode.getAddress());
	    state.TCgetStack().push(w3, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    state.TCgetStack().push(w1, bCode.getAddress());
	    break;
	case ByteCode.SWAP:
	    notDoubleType(w1);
	    notDoubleType(w2);
	    state.TCgetStack().push(w1, bCode.getAddress());
	    state.TCgetStack().push(w2, bCode.getAddress());
	    break;
	default:
	    throw new Error("Internal Error: BCStackEffect.stackOp called with wrong bytecode!");
	}

    }

    //[get|put][static|field]
    static private void fieldOp(int bc, BCCPArgOp bCode, TCState state) 
	throws VerifyException {
	TCTypes objType;
	TCObjectTypes cType;
	String typeDesc;
	int cPoolIndex;

	switch(bc) {
	case ByteCode.GETFIELD:
	    objType = state.TCgetStack().TCpop();
	    if (!(objType instanceof TCObjectTypes)) {
		throw new VerifyException("Trying to use Getfield on a " + objType);
	    }
	    cType = (TCObjectTypes) objType;

	    /*if (cType.getClassName().equals(TCObjectTypes.nullString)) {
	      VerifyException e = new VerifyException();
	      e.foundNull = true;
	      e.bCode = bCode;
	      throw e;
	      }*/
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8)&0xff00) |
		(((int) bCode.getByteArgs()[1])&0xff);
	    TCObjectTypes.checkMemberAccess(state.getMv().getClassName(),//accessing class
					    cType, //class to be accessed
					    bCode.getCP().entryAt(cPoolIndex),
					    false); // non-static access
	    typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    state.TCgetStack().push(TCTypes.typeFromTypeDesc(typeDesc), bCode.getAddress());
	    break;
	case ByteCode.GETSTATIC:
	    //FEHLER: objType pruefen!
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8)&0xff00) |
		(((int) bCode.getByteArgs()[1])&0xff);
	    typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    state.TCgetStack().push(TCTypes.typeFromTypeDesc(typeDesc), bCode.getAddress());
	    break;
	case ByteCode.PUTFIELD:
	case ByteCode.PUTSTATIC:
	    cPoolIndex = ((bCode.getByteArgs()[0] <<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    typeDesc = bCode.getCP().fieldRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    TCTypes fieldType[] = TCTypes.typeFromTypeDesc(typeDesc);
	    for (int i = fieldType.length-1;i >=0; i-- ) {
		state.TCgetStack().TCpeek().consistentWith(fieldType[i]);
		state.TCgetStack().TCpop();

	    }
	    if (bc == ByteCode.PUTSTATIC) break;
	    //FEHLER: objType pruefen!
	    objType = state.TCgetStack().TCpop();
	    if (!(objType instanceof TCObjectTypes)) {
		throw new VerifyException("Trying to use Putfield on a " + objType);
	    }
	    cType = (TCObjectTypes) objType;
	    /* -- null.putfield is OK! see TCObjectTypes.consistentWith
	      if (cType.getClassName().equals(TCObjectTypes.nullString)) {
	      throw new VerifyException("Trying to use Putfield on " +TCObjectTypes.nullString);
	      }*/
	    break;
	default:
	    throw new Error("Internal Error: BCStackEffect.fieldOp called with wrong bytecode!");
	}
    }

    //invokes
    static private void invoke (int bc, BCCPArgOp bCode, TCState state) 
	throws VerifyException {
	//FEHLER: objType pruefen!
	//FEHLER: Behandlung von <init> und <clinit> fehlt!
	int cPoolIndex;
	String typeDesc, methodName;
	TCTypes objType;
	cPoolIndex = (bCode.getByteArgs()[0]<<8)|
	    (((int)bCode.getByteArgs()[1])&0xff);

	//get typedesc for method
	if (bc !=ByteCode.INVOKEINTERFACE) {
	    typeDesc = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberTypeDesc();
	    methodName = bCode.getCP().methodRefEntryAt(cPoolIndex).getMemberName();
	} else { //invokeinterface
	    typeDesc = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		getMemberTypeDesc();
	    methodName = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).
		getMemberName();
	}
	TCTypes[] args = TCTypes.argTypeFromMethod(typeDesc);

	if (bc == ByteCode.INVOKEINTERFACE) {
	    //check if nargs in bytecode and actual number of parameters are equal
	    if (bCode.getByteArgs()[2]-1 != args.length) {
		throw new VerifyException("Number of arguments in bytecode (" + 
					  (bCode.getByteArgs()[2]-1) +
					  ") and MethodrefEntry (" + args.length + 
					  ")differ!");
	    }
	}
	

	for (int k = 0; k < args.length; k++) {
	    if (args[k].getType()==TCTypes.BYTE||
		args[k].getType()==TCTypes.CHAR||
		args[k].getType()==TCTypes.SHORT){
		args[k] = TCTypes.T_INT;
	    }
	}
	for (int i = args.length-1;i >=0; i-- ) {
	    try {
		state.TCgetStack().TCpeek().consistentWith(args[i]);
		state.TCgetStack().TCpop();
	    } catch (VerifyException e) {
		e.append("Parameter " + i + " has wrong type!");
		throw e;
	    }
	}
	
	if (bc != ByteCode.INVOKESTATIC) {
	    //pop target object from stack and check if method exists!
	    objType = state.TCgetStack().TCpop();
	    if (bc == ByteCode.INVOKEVIRTUAL) {
		//normal methods....
		if (!(objType instanceof TCObjectTypes)) {
		    throw new VerifyException("Method " + methodName + typeDesc +
					      " invoked on " + objType + 
					      " which is not an Object!");
		}
		if (objType instanceof TCArrayTypes) {
		    //Arrays have the same methods as java/lang/Object
		    objType = TCTypes.T_OBJECT;
		}
		TCObjectTypes cType = (TCObjectTypes) objType;
		
		TCObjectTypes.
		    checkMemberAccess(state.getMv().getClassName(),
				      cType, 
				      bCode.getCP().entryAt(cPoolIndex), 
				      false);
		
	    } else if (bc == ByteCode.INVOKEINTERFACE) {
		
		String intName = bCode.getCP().InterfaceMethodRefEntryAt(cPoolIndex).getClassName();
		if (objType instanceof TCObjectTypes) {
		    if (!(((TCObjectTypes)objType).getClassName().equals(TCObjectTypes.nullString) ||
			  ((TCObjectTypes)objType).getClassName().equals(TCObjectTypes.objectString) ||
			  ((TCObjectTypes)objType).hasInterface(new TCInterfaceTypes(intName)))) 
			throw new VerifyException("Interfacemethod " + methodName + typeDesc +
						  " invoked on " + objType + 
						  " which does not implement Interface " + intName + "!");
		} else {
			throw new VerifyException("Interfacemethod " + methodName + typeDesc +
						  " invoked on " + objType );
		}
	    }
	    
	    
	}//if (!INVOKESTATIC)
	TCTypes[] ret = TCTypes.returnTypeFromMethod(typeDesc);
	if (ret.length > 0 && (ret[0].getType() == TCTypes.CHAR ||
			       ret[0].getType() == TCTypes.BYTE ||
			       ret[0].getType() == TCTypes.SHORT) ){
	    ret[0] = new TCTypes(TCTypes.INT);
	}
	    
	state.TCgetStack().push(ret, bCode.getAddress());
    }

    //<a>new<array>
    //multianewarray
    static private void objectCreationOp(int bc, ByteCode bCode, TCState state) 
	throws VerifyException {
	BCCPArgOp bCodeCP;
	int cPoolIndex;
	String typeDesc;

	switch(bc) {
	case ByteCode.NEWARRAY:
	    state.TCgetStack().TCpop().consistentWith(TCTypes.T_INT); //length of array
	    state.TCgetStack().push(new TCArrayTypes(bCode.getByteArgs()[0]), 
				  bCode.getAddress());
	    break;
	case ByteCode.ANEWARRAY:
	    state.TCgetStack().TCpop().consistentWith(TCTypes.T_INT); //length of array
	    
	    cPoolIndex = (( bCode.getByteArgs()[0]<<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    bCodeCP = (BCCPArgOp) bCode;
	    TCTypes baseType = null;
	    if (bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName().charAt(0) == '[') {
		//array type
		baseType = TCTypes.
		    typeFromTypeDesc(bCodeCP.getCP().
				     classEntryAt(cPoolIndex).getClassName())[0]; 
	    } else {
		baseType = new TCObjectTypes(bCodeCP.
					      getCP().classEntryAt(cPoolIndex).getClassName());
	    }
	    state.TCgetStack().push(new TCArrayTypes(baseType),
				  bCodeCP.getAddress());
	    break;
	case ByteCode.MULTIANEWARRAY:
	    cPoolIndex = (( bCode.getByteArgs()[0]<<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    bCodeCP = (BCCPArgOp) bCode;
	    //i dimensions
	    for (int i = 0; i < bCodeCP.getByteArgs()[2]; i++) {
		state.TCgetStack().TCpop().consistentWith(TCTypes.T_INT);
	    }
	    
	    state.TCgetStack().push(
              TCTypes.typeFromTypeDesc(
                bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName()), 
	      bCodeCP.getAddress());
	    break;
	case ByteCode.NEW:
	    cPoolIndex = (( bCode.getByteArgs()[0]<<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    bCodeCP = (BCCPArgOp) bCode;
	    state.TCgetStack().push(
              new TCObjectTypes(
                bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName()), 
	      bCodeCP.getAddress());
	    break;
	default:
	}
    }
    
    //ldcs
    static private void ldcOp(int bc, BCCPArgOp bCode, TCState state) throws VerifyException{
	int cPoolIndex = ((int) bCode.getByteArgs()[0]) & 0xff;
	if (bc == ByteCode.LDC_W|| bc == ByteCode.LDC2_W) 
	    cPoolIndex = (cPoolIndex <<8) | (((int) bCode.getByteArgs()[1])&0xff);
	ConstantPoolEntry cpEntry = bCode.getCP().entryAt(cPoolIndex);
	switch (cpEntry.getTag()) {
	case ConstantPoolEntry.CONSTANT_STRING:
	    state.TCgetStack().push(new TCObjectTypes("java/lang/String"), 
				  bCode.getAddress());
	    break;
	case ConstantPoolEntry.CONSTANT_INTEGER:
	    state.TCgetStack().push(TCTypes.T_INT, bCode.getAddress());
	    break;
	case ConstantPoolEntry.CONSTANT_LONG:
	    state.TCgetStack().push(TCTypes.T_LONG_L, bCode.getAddress());
	    state.TCgetStack().push(TCTypes.T_LONG_U, bCode.getAddress());
	    break;
	case ConstantPoolEntry.CONSTANT_FLOAT:
	    state.TCgetStack().push(TCTypes.T_FLOAT, bCode.getAddress());
	    break;
	case ConstantPoolEntry.CONSTANT_DOUBLE:
	    state.TCgetStack().push(TCTypes.T_DOUBLE_L, bCode.getAddress());
	    state.TCgetStack().push(TCTypes.T_DOUBLE_U, bCode.getAddress());
	    break;
	default:
	    throw new Error("Internal Error: ldc(_w) called with index that is not of proper type!");
	}

    }

    //<t>loads, <t>stores
    //FEHLER iinc fehlt noch!!!
    static private void lVarOp(int bc, ByteCode bCode, TCState state) 
	throws VerifyException {
	int index = -1;
	TCTypes type;
	switch (bc) {
	case ByteCode.ILOAD:
	case ByteCode.ISTORE:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_INT;
	    break;
	case ByteCode.ILOAD_0:
	case ByteCode.ISTORE_0:
	    index = 0;
	    type = TCTypes.T_INT;
	    break;
	case ByteCode.ILOAD_1:
	case ByteCode.ISTORE_1:
	    index = 1;
	    type = TCTypes.T_INT;
	    break;
	case ByteCode.ILOAD_2:
	case ByteCode.ISTORE_2:
	    index = 2;
	    type = TCTypes.T_INT;
	    break;
	case ByteCode.ILOAD_3:
	case ByteCode.ISTORE_3:
	    index = 3;
	    type = TCTypes.T_INT;
	    break;
	case ByteCode.LLOAD:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_LONG_L;
	    break;
	case ByteCode.LSTORE:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_LONG_U;
	    break;
	case ByteCode.LLOAD_0:
	    index = 0;
	    type = TCTypes.T_LONG_L;
	    break;
	case ByteCode.LSTORE_0:
	    index = 0;
	    type = TCTypes.T_LONG_U;
	    break;
	case ByteCode.LLOAD_1:
	    index = 1;
	    type = TCTypes.T_LONG_L;
	    break;
	case ByteCode.LSTORE_1:
	    index = 1;
	    type = TCTypes.T_LONG_U;
	    break;
	case ByteCode.LLOAD_2:
	    index = 2;
	    type = TCTypes.T_LONG_L;
	    break;
	case ByteCode.LSTORE_2:
	    index = 2;
	    type = TCTypes.T_LONG_U;
	    break;
	case ByteCode.LLOAD_3:
	    index = 3;
	    type = TCTypes.T_LONG_L;
	    break;
	case ByteCode.LSTORE_3:
	    index = 3;
	    type = TCTypes.T_LONG_U;
	    break;
	case ByteCode.FLOAD:
	case ByteCode.FSTORE:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_FLOAT;
	    break;
	case ByteCode.FLOAD_0:
	case ByteCode.FSTORE_0:
	    index = 0;
	    type = TCTypes.T_FLOAT;
	    break;
	case ByteCode.FLOAD_1:
	case ByteCode.FSTORE_1:
	    index = 1;
	    type = TCTypes.T_FLOAT;
	    break;
	case ByteCode.FLOAD_2:
	case ByteCode.FSTORE_2:
	    index = 2;
	    type = TCTypes.T_FLOAT;
	    break;
	case ByteCode.FLOAD_3:
	case ByteCode.FSTORE_3:
	    index = 3;
	    type = TCTypes.T_FLOAT;
	    break;
	case ByteCode.DLOAD:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_DOUBLE_L;
	    break;
	case ByteCode.DSTORE:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_DOUBLE_U;
	    break;
	case ByteCode.DLOAD_0:
	    index = 0;
	    type = TCTypes.T_DOUBLE_L;
	    break;
	case ByteCode.DSTORE_0:
	    index = 0;
	    type = TCTypes.T_DOUBLE_U;
	    break;
	case ByteCode.DLOAD_1:
	    index = 1;
	    type = TCTypes.T_DOUBLE_L;
	    break;
	case ByteCode.DSTORE_1:
	    index = 1;
	    type = TCTypes.T_DOUBLE_U;
	    break;
	case ByteCode.DLOAD_2:
	    index = 2;
	    type = TCTypes.T_DOUBLE_L;
	    break;
	case ByteCode.DSTORE_2:
	    index = 2;
	    type = TCTypes.T_DOUBLE_U;
	    break;
	case ByteCode.DLOAD_3:
	    index = 3;
	    type = TCTypes.T_DOUBLE_L;
	    break;
	case ByteCode.DSTORE_3:
	    index = 3;
	    type = TCTypes.T_DOUBLE_U;
	    break;
	case ByteCode.ALOAD:
	case ByteCode.ASTORE:
	    index = ((int)bCode.getByteArgs()[0]) &0xff;
	    type = TCTypes.T_ANY_REF;
	    break;
	case ByteCode.ALOAD_0:
	case ByteCode.ASTORE_0:
	    index = 0;
	    type = TCTypes.T_ANY_REF;
	    break;
	case ByteCode.ALOAD_1:
	case ByteCode.ASTORE_1:
	    index = 1;
	    type = TCTypes.T_ANY_REF;
	    break;
	case ByteCode.ALOAD_2:
	case ByteCode.ASTORE_2:
	    index = 2;
	    type = TCTypes.T_ANY_REF;
	    break;
	case ByteCode.ALOAD_3:
	case ByteCode.ASTORE_3:
	    index = 3;
	    type = TCTypes.T_ANY_REF;
	    break;
	default:
	    type = TCTypes.T_UNKNOWN;
	    
	}
	
	if (bc < 0x2e) { //loads
	    state.TCgetStack().push(state.TCgetlVars().read(index, type), bCode.getAddress());
	    
	    if (type.getType() == TCTypes.LONG_L) {
		state.TCgetStack().push(state.TCgetlVars().read(index+1, TCTypes.T_LONG_U), 
				      bCode.getAddress());
	    }
	    
	    if (type.getType() == TCTypes.DOUBLE_L) {
		state.TCgetStack().push(state.TCgetlVars().read(index+1, TCTypes.T_DOUBLE_U), 
				      bCode.getAddress());
	    }
	} //if (bc < 0x2e)
	else { //STORES
	    state.TCgetStack().TCpeek().consistentWith(type);
	    state.TCgetlVars().write(index, state.TCgetStack().TCpop(), bCode.getAddress());
	    if (type.getType() == TCTypes.LONG_U) {
		state.TCgetStack().TCpeek().consistentWith(TCTypes.T_LONG_L);
		state.TCgetlVars().write(index, state.TCgetStack().TCpop(), bCode.getAddress());
		state.TCgetlVars().write(index+1,TCTypes.T_LONG_U, bCode.getAddress());
	    }		    
	    if (type.getType() == TCTypes.DOUBLE_U) {
		state.TCgetStack().TCpeek().consistentWith(TCTypes.T_DOUBLE_L);
		state.TCgetlVars().write(index, state.TCgetStack().TCpop(), bCode.getAddress());
		state.TCgetlVars().write(index+1, TCTypes.T_DOUBLE_U, bCode.getAddress());
	    }
	}
    }
    

    //<t>consts, [b|s]ipush
    //<t>aloads
    //<t>astores
    //AL-ops, casts, cmps, ifs, if_cmps, goto, ret, switches, returns
    //arraylength, athrow, checkcast, instanceof, monitorenter/exit, wide
    //if<non>null, goto_w
    static private void genericOp(int bc, ByteCode bCode, TCState state) 
	throws VerifyException{
	TCTypes pop[], push[], tmpType;
	pop = POP[bc];
	push = PUSH[bc];
	tmpType = null;
	for (int i = pop.length-1; i >=0; i--) {
	    state.TCgetStack().TCpeek().consistentWith(pop[i]);
	    tmpType = state.TCgetStack().TCpop();
	}
	
	if ((bc >= 0x2e && bc <= 0x35) || (bc >= 0x4f && bc <= 0x56)){ //<t>aload/store 
	    //check if array is of proper type
	    TCTypes opType =null;
	    switch(bc) {
	    case ByteCode.IALOAD:
	    case ByteCode.IASTORE:
		opType = TCTypes.T_INT;
		break; 
	    case ByteCode.LALOAD:
	    case ByteCode.LASTORE:
		opType = TCTypes.T_LONG_U;
		break; 
	    case ByteCode.FALOAD:
	    case ByteCode.FASTORE:
		opType = TCTypes.T_FLOAT;
		break; 
	    case ByteCode.DALOAD:
	    case ByteCode.DASTORE:
		opType = TCTypes.T_DOUBLE_U;
		break; 
	    case ByteCode.AALOAD:
	    case ByteCode.AASTORE:
		opType = TCTypes.T_ANY_REF;
		break;
	    case ByteCode.BALOAD:
	    case ByteCode.BASTORE:
		opType = TCTypes.T_BYTE;
		break; 
	    case ByteCode.CALOAD:
	    case ByteCode.CASTORE:
		opType = TCTypes.T_CHAR;
		break; 
	    case ByteCode.SALOAD:
	    case ByteCode.SASTORE:
		opType = TCTypes.T_SHORT;
		break; 
	    default:
		throw new Error("'Unpossible Error' in BCStackEffect.genericOp!");
	    }

	    if (tmpType instanceof TCObjectTypes) {
		if(((TCObjectTypes)tmpType).getClassName().
		   equals(TCObjectTypes.nullString)) {
		    /*//signal that null has been found were it will raise an exception
		    //--> no need to continue verifying.
		    VerifyException e = new VerifyException();
		    e.foundNull = true;
		    e.bCode = bCode;
		    throw e;*/
		}
	    }
	    TCArrayTypes tmpAType;
	    tmpAType = (TCArrayTypes) tmpType;
	    tmpAType.getBaseType().consistentWith(opType);
	    if (bc == ByteCode.AALOAD) {
		push[0] = tmpAType.getBaseType();
	    }


	} //<t>aload/store
	else if (bc == 0xc0) { //checkcast
	    BCCPArgOp bCodeCP;
	    int cPoolIndex;
	    String typeDesc;
	    
	    cPoolIndex = (( bCode.getByteArgs()[0]<<8) & 0xff00) |
		(((int)bCode.getByteArgs()[1])&0xff);
	    bCodeCP = (BCCPArgOp) bCode;
	    String tmpName = 
		(bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName().charAt(0) == '[')?
		bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName() :
		"L" + bCodeCP.getCP().classEntryAt(cPoolIndex).getClassName() + ";";	
	    push[0] = 
		TCTypes.typeFromTypeDesc(tmpName)[0];
	}
	
	
	state.TCgetStack().push(push, bCode.getAddress());
    }

    static private void jsr(int bc, ByteCode bCode, TCState state) {
	//do nothing. all done by JVMState.
    }
}


