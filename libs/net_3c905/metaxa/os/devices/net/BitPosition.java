package metaxa.os.devices.net;

import jx.zero.Debug;

/* Klasse BitPosition enthält Definitionen aller Bits in einem int, sowie Funktionen zum
   binären Ausgeben von verschiedenen primitiven Datentypen sowie Funktionen zum Setzen und
   Löschen von Bits in primitiven Datentypen sowie zum Abschneiden bestimmter Bytes 
*/

class BitPosition {
    
    private static final int BIT_0=(1 << 0);
    private static final int BIT_1=(1 << 1);
    private static final int BIT_2=(1 << 2);
    private static final int BIT_3=(1 << 3);
    private static final int BIT_4=(1 << 4);
    private static final int BIT_5=(1 << 5);
    private static final int BIT_6=(1 << 6);
    private static final int BIT_7=(1 << 7);
    private static final int BIT_8=(1 << 8);
    private static final int BIT_9=(1 << 9);
    private static final int BIT_10=(1 << 10);
    private static final int BIT_11=(1 << 11);
    private static final int BIT_12=(1 << 12);
    private static final int BIT_13=(1 << 13);
    private static final int BIT_14=(1 << 14);
    private static final int BIT_15=(1 << 15);
    private static final int BIT_16=(1 << 16);
    private static final int BIT_17=(1 << 17);
    private static final int BIT_18=(1 << 18);
    private static final int BIT_19=(1 << 19);
    private static final int BIT_20=(1 << 20);
    private static final int BIT_21=(1 << 21);
    private static final int BIT_22=(1 << 22);
    private static final int BIT_23=(1 << 23);
    private static final int BIT_24=(1 << 24);
    private static final int BIT_25=(1 << 25);
    private static final int BIT_26=(1 << 26);
    private static final int BIT_27=(1 << 27);
    private static final int BIT_28=(1 << 28);
    private static final int BIT_29=(1 << 29);
    private static final int BIT_30=(1 << 30);
    private static final int BIT_31=(1 << 31);
    
    public static final int bit_0() {return BIT_0;}
    public static final int bit_1() {return BIT_1;}
    public static final int bit_2() {return BIT_2;}  
    public static final int bit_3() {return BIT_3;}
    public static final int bit_4() {return BIT_4;}
    public static final int bit_5() {return BIT_5;}
    public static final int bit_6() {return BIT_6;} 
    public static final int bit_7() {return BIT_7;} 
    public static final int bit_8() {return BIT_8;} 
    public static final int bit_9() {return BIT_9;}
    public static final int bit_10() {return BIT_10;} 
    public static final int bit_11() {return BIT_11;} 
    public static final int bit_12() {return BIT_12;} 
    public static final int bit_13() {return BIT_13;}  
    public static final int bit_14() {return BIT_14;}
    public static final int bit_15() {return BIT_15;}  
    public static final int bit_16() {return BIT_16;}
    public static final int bit_17() {return BIT_17;}  
    public static final int bit_18() {return BIT_18;}
    public static final int bit_19() {return BIT_19;} 
    public static final int bit_20() {return BIT_20;}
    public static final int bit_21() {return BIT_21;}
    public static final int bit_22() {return BIT_22;}
    public static final int bit_23() {return BIT_23;}
    public static final int bit_24() {return BIT_24;}
    public static final int bit_25() {return BIT_25;}
    public static final int bit_26() {return BIT_26;}
    public static final int bit_27() {return BIT_27;}  
    public static final int bit_28() {return BIT_28;}  
    public static final int bit_29() {return BIT_29;} 
    public static final int bit_30() {return BIT_30;}
    public static final int bit_31() {return BIT_31;}

    /* Zur Ausgabe als Bitstring  
       Version für int
    */
    
    public void printBinary(String s, int i) {
	
	System.out.println(s + ", int: " + i +", binaer: ");
	System.out.print(" ");
	for (int j = 31; j >= 0; j--) {
	    if (( (1 << j) & i) != 0) {
		System.out.print("1");
	    }
	    else {
		System.out.print("0");
	    }
	}
	System.out.println();
    }
    
  public void printBinaryDebug(String s, int i) {
	
      Debug.out.println(s + ", int: " + i +", binaer: ");
      Debug.out.print(" ");
      
      for (int j = 31; j >= 0; j--) {
	  if (( (1 << j) & i) != 0) {      
	      Debug.out.print("1");
	  }
	  else {
	      Debug.out.println("0");
	      System.out.print("0");
	  }
      }
      Debug.out.println();
  }


    
    /* Zur Ausgabe als Bitstring  
       Version für short
    */
    
    public void printBinary(String s, short i) {
	
	System.out.println(s + ", short: " + i +", binaer: ");
	System.out.print(" ");
	for (int j = 15; j >= 0; j--) {
	    if (( (1 << j) & i) != 0) {
		System.out.print("1");
	    }
	    else {
		System.out.print("0");
	    }
	}
	System.out.println();
	
    }

    
    /*
     * Zur Ausgabe als Bitstring  
     * Version für long
     */
    /*
    public void printBinary(String s, long i) {
	
	System.out.println(s + ", long: " + i +", binaer: ");
	System.out.print(" ");
	for (int j = 63; j >= 0; j--) {
	    if ((((long)1 << j) & i) != (long)0) {
		System.out.print("1");
	    }
	    else {
		System.out.print("0");
	    }
	}
	System.out.println();
	
    }
    */
    /* Zur Ausgabe als Bitstring  
       Version für byte
    */

    public void printBinary(String s, byte i) {
    
	System.out.println(s + ", byte: " + i +", binaer: ");
	System.out.print(" ");
	for (int j = 7; j >= 0; j--) {
	    if (( (1 << j) & i) != 0) {
		System.out.print("1");
	    }
	    else {
		System.out.print("0");
	    }
	}
	System.out.println();
	
    }

    /* Zur Überprüfung, ob ein Bit gesetzt ist
       Version für int
    */
    
    public boolean isSet(int arg, int welches) throws BitNotExistingException {
	if (welches >= 32)
	    throw new BitNotExistingException();
	if (((1 << welches) & arg) != 0)
	    return true;
	else 
	    return false;
    }

    /* Zum Setzen eines einzelnen Bits
       Version für int
    */
    
    public int setBit(int arg, int welches) throws BitNotExistingException {
	if (welches >= 32)
	    throw new BitNotExistingException();
	arg = arg | (1 << welches);
	return arg;
    }
    
    /* Zum Löschen eines einzelnen Bits
       Version für int
    */
    
    public int eraseBit(int arg, int welches) throws BitNotExistingException {
	if (welches >= 32)
	    throw new BitNotExistingException();
	if (isSet(arg,welches))
	    arg = arg ^ (1 << welches);
	return arg;
    }
    
    /* Zur Überprüfung, ob ein Bit gesetzt ist
       Version für short
    */
    
    public boolean isSet(short arg, int welches) throws BitNotExistingException {
	if (welches >= 16)
	    throw new BitNotExistingException();
	if ( ((1 << welches) & arg) != 0)
	    return true;
	else 
	    return false;
    }
    
    /* Zum Setzen eines einzelnen Bits
       Version für short
    */
    
    public short setBit(short arg, int welches) throws BitNotExistingException {
	if (welches >= 16)
	    throw new BitNotExistingException();
	arg = (short)(arg | (1 << welches));
	return arg;
    }
    
    /* Zum Löschen eines einzelnen Bits
       Version für short
    */
    
    public short eraseBit(short arg, int welches) throws BitNotExistingException {
	if (welches >= 16)
	    throw new BitNotExistingException();
	if (isSet(arg,welches))
	    arg = (short)(arg ^ (1 << welches));
	return arg;
    }
    
    /* Die folgenden Funktionen liefert das oberste oder das niedrigste Byte eines short
       oder eines int zurück
       Noch keine Funktion vorhanden für die inneren zwei Bytes eines int
    */
    
    public byte hibyte(short arg) {
	return (byte)(arg >> 8);
    }
    
    public byte hibyte(int arg) {
	return (byte)(arg >> 24);
    }
    
    public byte lobyte(short arg) {
	return (byte)(arg & 0xFF);
    }
    
    public byte lobyte(int arg) {
	return (byte)(arg & 0xFF);
    }
    
    /* unsigned Interpretation eines Byte-Wertes und Rückgabe als Dezimal-Wert als String 
     * wird unter anderem in EthernetAdress.java gebraucht
     */
    
    public String byte_to_unsigned(byte wert) {
	if (((byte)(1<<7) & wert) == 0) {
	    return "" + wert;
	}
	int hilf = 128 + ((byte)0x7F & wert);
	return "" + hilf;
    }
       
}

