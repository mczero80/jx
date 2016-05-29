package javafs;

import jx.zero.Memory;
import jx.zero.*;
import jx.fs.buffercache.*;

/**
 * Diese Klasse bietet Methoden zum Zugriff auf primitive Java-Datentypen, die in einem Memory-Objekt kodiert sind. Sie dient
 * als Basisklasse f&uuml;r die <code>Data</code>-Klassen, die Zugriff auf die verschiedenen Dateisystem-Strukturen erlauben
 * (z.B. <code>InodeData</code> f&uuml; den Zugriff auf die Daten einer Inode). Der Zugriff auf die Festplatte erfolgt &uuml;ber
 * das Lesen und Schreiben von Memory-Objekten. Um Daten in diesen Objekten (die in den <code>BufferHead</code>s enthalten sind)
 * zu sichern, werden diese mit den Funktionen dieser Klasse kodiert und zum Auslesen wieder dekodiert.
 */
public  class BufferHeadAccess {
    /**
     * Das Memory-Objekt, auf dessen Inhalt zugegriffen werden soll
     */
    protected Memory     memory;
    /**
     * Die Position des ersten Datums im Objekt
     */
    public    int        offset;
    /**
     * Die L&auml;nge der Struktur innerhalb des Objekts
     */
    public    int        length;
    /**
     * Der <code>BufferHead</code>, der das Memory-Objekt enth&auml;lt
     */
    public    BufferHead bh;

    public BufferHeadAccess() {
	offset = 0;
    }

    /**
     * Schreibt den &uuml;bergebenen Wert als vorzeichenbehaftete 8 Bit-Zahl an die angegebene Position in der Datenstruktur
     * (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value der Wert, der als <code>byte</code> geschrieben werden soll
     */
    final protected void writeByte(int pos, byte value) {
	memory.set8(offset+pos, value);
    }

    /**
     * Schreibt den &uuml;bergebenen Wert als vorzeichenbehaftete 16 Bit-Zahl an die angegebene Position in der Datenstruktur
     * (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value der Wert, der als <code>short</code> geschrieben werden soll
     */
    final protected void writeShort(int pos, short value) {
	memory.set8(offset+pos+1, (byte)((value>>8) & 255));
	memory.set8(offset+pos,   (byte)(value      & 255));
	// oder: memory.set16(...Offset umrechnen..., value);
    }

    /**
     * Schreibt den &uuml;bergebenen Wert als vorzeichenbehaftete 32 Bit-Zahl an die angegebene Position in der Datenstruktur
     * (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value der Wert, der als <code>int</code> geschrieben werden soll
     */
    final protected void writeInt(int pos, int value) {
	memory.setLittleEndian32(offset+pos, value);
    }

    /**
     * Schreibt den &uuml;bergebenen Wert als vorzeichenbehaftete 64 Bit-Zahl an die angegebene Position in der Datenstruktur
     * (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value der Wert, der als <code>long</code> geschrieben werden soll
     */
    /*
    protected void writeLong(int pos, long value) {
	throw new Error("not implemented");
	//writeInt(pos+4, (int)(value & 0xffffffff));
	//writeInt(pos, (int)((value >> 32) & 0xffffffff));
    }
    */


    /**
     * Schreibt den &uuml;bergebenen Zeitstempel als vorzeichenbehaftete 32 Bit-Zahl an die angegebene Position in der
     * Datenstruktur (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     * Um die 64 Bit der <code>long</code>-Variable, die die verstrichenen Millisekunden seit 1.1.1970 darstellt, auf einen
     * 32 Bit-Wert zu reduzieren, wird der Wert durch 1000 dividiert. Der <code>int</code>-Wert gibt die Zahl der Sekunden seit
     * 1.1.1970 an, was dem Format f&uuml;r Zeitstempel in C entspricht.
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value der Wert, der als <code>int</code> geschrieben werden soll
     */
    final protected void writeTime(int pos, int value) {
	writeInt(pos, value); 
    }

    /**
     * Schreibt den &uuml;bergebenen <code>String</code> als Folge von Bytes an die angegebene Position in der Datenstruktur
     * (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code> und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @param value die Zeichenkette, die als Folge von Bytes geschrieben werden soll
     * @param len   die maximale L&auml;nge der Zeichenkette. Ist der <code>String</code> k&uuml;rzer, wird der &uuml;brige
     *              Platz mit 0 aufgef&uuml;llt.
     */
    final protected void writeString(int pos, String value, int len) {
	byte strarray[] = value.getBytes();
	int strlen = strarray.length;

	//Debug.out.println("pos: "+pos+" strlen: "+strlen+" len: "+len);

	if (strlen > len)
	    strlen = len;
	for (int i = 0; i < strlen; i++)
	    memory.set8(offset+pos+i, strarray[i]);
	while (strlen < len) {
	    memory.set8(offset+pos+strlen, (byte)0);
	    strlen++;
	}
    }

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine vorzeichenbehaftete 8 Bit-Zahl und liefert diese (als
     * <code>byte</code>-Wert) zur&uuml;ck (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code>
     * und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return die 8 Bit-Zahl, die an dieser Position in der Datenstruktur steht (als <code>byte</code>)
     */
    final protected byte readByte(int pos) {
	return memory.get8(offset+pos);
    }

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine vorzeichenbehaftete 16 Bit-Zahl und liefert diese (als
     * <code>short</code>-Wert) zur&uuml;ck (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code>
     * und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return die 16 Bit-Zahl, die an dieser Position in der Datenstruktur steht (als <code>short</code>)
     */
    protected short readShort(int pos) { // FIXME: making this final crashes the translator
	return memory.getLittleEndian16(offset+pos);
    }

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine vorzeichenbehaftete 32 Bit-Zahl und liefert diese (als
     * <code>int</code>-Wert) zur&uuml;ck (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code>
     * und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return die 32 Bit-Zahl, die an dieser Position in der Datenstruktur steht (als <code>int</code>)
     */
     final protected int readInt(int pos) {
	return memory.getLittleEndian32(offset+pos);
    }

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine vorzeichenbehaftete 64 Bit-Zahl und liefert diese (als
     * <code>long</code>-Wert) zur&uuml;ck (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code>
     * und <code>pos</code>).
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return die 64 Bit-Zahl, die an dieser Position in der Datenstruktur steht (als <code>long</code>)
     */
    /*
    protected long readLong(int pos) {
        long part[] = new long[7];
	for (int i = 0; i < 7; i++) {
	    part[i] = (long)memory.get8(offset+pos+i+1);
	    if (part[i] < 0) part[i] += 256;
	}

	return (long)(memory.get8(offset+pos)<<56 | part[0]<<48 | part[1]<<40 | part[2]<<32 |
		      part[3]<<24 | part[4]<<16 | part[5]<<8 | part[6]);
    }
    */

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine vorzeichenbehaftete 32 Bit-Zahl und liefert diese (als
     * <code>long</code>-Wert) zur&uuml;ck (die Position im Memory-Objekt ist die Summe der beiden Variablen <code>offset</code>
     * und <code>pos</code>). Der gelesene Wert wird als Zahl der Sekunden seit 1.1.1970 interpretiert (dem Format f&uuml;r
     * Zeitstempel in C) pund f&uuml;r die Verwendung mit Java mit 1000 multipliziert, um Millisekunden zu erhalten.
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return ein Zeitstempel als 64 Bit-Zahl (<code>long</code>)
     */
    final protected int readTime(int pos) {
	return readInt(pos);
    }

    /**
     * Liest an der angegebenen Position in der Datenstruktur eine Zeichenkette. Die Zeichenkette wird durch ein Nullbyte oder
     * durch den Parameter <code>len</code> begrenzt.
     *
     * @param pos   die Position innerhalb der Datenstruktur
     * @return ein neues <code>String</code>-Objekt, das die Zeichenkette an dieser Position in der Datenstruktur enth&auml;lt
     */
    final protected String readString(int pos, int len) {
	byte[] array = new byte[len];
	for (int i = 0; i < len; i++)
	    array[i] = memory.get8(offset+pos+i);
	return new String(array);
	//return retval.trim();
    }

    /**
     * Initialisiert das Objekt durch Angabe eines BufferHead und die Position innerhalb dessen Memory-Objekts.
     *
     * @param bh     der <code>BufferHead</code>, dessen Memory-Objekt verwendet werden soll
     * @param offset der Offset innerhalb des Memory-Objekts
     */
    final public void init(BufferHead bh, int offset) {
	this.bh = bh;
	this.offset = offset;
	memory = bh.getData();
    }

    /**
     * Initialisert das Objekt durch Angabe der Position innerhalb des Memory-Objekts.
     *
     * @param offset der (neue) Offset innerhalb des Memory-Objekts
     */
    final public void init(int offset) {
	this.offset = offset;
    }

    /**
     * Liefert die L&auml;nge der Datenstruktur (mu&szlig; nicht mit der L&auml;nge des Memory-Objekts &uuml;bereinstimmen).
     *
     * @return die L&auml;nge der Datenstruktur
     */
    final public int length() {
	return length;
    }

    /**
     * Setzt den Inhalt des Memory-Objekts auf 0.
     */
    final public void clear() {
	for (int i = offset; i < offset + length; i++)
	    memory.set8(i, (byte)0);
    }
}
