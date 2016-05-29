package jx.rdp;

import java.io.*;
import java.net.*;
import jx.net.IPAddress;
import jx.zero.*;

/**
 * Diese Klasse implementiert die Klasse 0 des ISO Transport Protokolls
 * (RFC 905) mit den Modifikationen aus RFC 2126 (ISO Transport Service
 * on top of TCP). MS hat einige kleine Veraenderungen am Protokoll vorgenommen.
 * <p>
 * In Klasse 0 werden folgende Aktionen unterstuetzt:
 * - CONNECTION_REQUEST
 * - DISCONNECT_REQUEST
 * - DATA_TRANSFER
 *
 * Erlaubte Antworten sind:
 * CONNECTION_CONFIRM
 * DATA_TRANSFER
 * ERROR
 *
 * Der DISCONNECT_REQUEST wird nicht bestaetigt.
 *
 * Der Aufbau des Headers ist folgendermassen:
 *<p>
 * _________________________________
 * | fester Teil | optionaler Teil |
 * ---------------------------------
 * 0             7                254       
 *<p>
 * Die ersten sieben byte sind fest vorgegeben der Rest ist fuer optionale Argumente
 * reserviert. In der Klasse 0 des ISO-Protokolls, die von RDP verwendet wird,
 * wird auf den optionalen Teil verzichtet. 
 *<p>
 * DIE CR, CC und DR PDUs sehen folgendermassen aus:
 *
 * __________________________________________________________________________________
 * |Version |reserviert |laenge |laenge des headers |typ |ziel |quelle |dienstklasse|
 * ----------------------------------------------------------------------------------
 * 0        1           2       4                   5    6     8       10           11
 *<p>
 * Version: Die Version des Protokolls (momentan V.3)
 * reserviert: Dieses byte ist fuer zukuenftige Erweiterungen reserviert
 * Laenge: Laenge des Pakets inklusive Header in byte
 * Laenge des Headers: Laenge des Headers minus Oktett 1-5
 * Typ: Typ des Paketes (CR, CC, DR)
 * Ziel: Addresse des Ziels (bei CR 0x00)
 * Quelle: Adresse der Quelle
 * Dienstklasse: Welche Dienstklasse soll verwendet werden (RFC 905 erlaubt 0-4
 * wir verwenden 0) Dies wird bei DR ignoriert
 * <p>
 * Die DT PDU sieht folgendermassen aus:
 *
 * ___________________________________________________________
 * |Version |reserviert| Laenge| Laenge des Headers |Typ| EOT|
 * -----------------------------------------------------------
 * 0        1          2       4                    5   6    7
 *<p>
 * EOT: End of Transmission. Die Klassen 1-4 unterstuetzen Sequenznummern, das letzte
 * Paket wird mit EOT bestaetigt. Klasse 0 kennt keine Sequenznummern, daher wird immer
 * EOT (0x80) gesendet.
 */

public class ISO {
    //debug
    private boolean debug = false;
    private HexDump dump = null;

    private Socket rdpsock=null;
    private DataInputStream in=null;
    private DataOutputStream out=null;
    private MemoryManager memorymanager=null;
    private Naming naming = null;
    
    /* this for the ISO Layer */
    private static final int CONNECTION_REQUEST=0xE0;
    private static final int CONNECTION_CONFIRM=0xD0;
    private static final int DISCONNECT_REQUEST=0x80;
    private static final int DATA_TRANSFER=0xF0;
    private static final int ERROR=0x70;
    private static final int PROTOCOL_VERSION=0x03;
    private static final int EOT=0x80;
    
    public ISO() {
	naming = InitialNaming.getInitialNaming();
	memorymanager = (MemoryManager)naming.lookup("MemoryManager");
	dump = new HexDump();
    }

    public Packet init(int length) {
	Packet data = getMemory(length+7);
	data.incrementPosition(7);
	data.setStart(data.getPosition());
	return data;
    }
	
    public void connect(IPAddress host, int port) throws IOException, SocketException, RdesktopException {
	int code = 0;
	this.rdpsock=new Socket(host, port);
	//rdpsock.setTcpNoDelay(true);
	this.in = new DataInputStream(rdpsock.getInputStream());
	this.out= new DataOutputStream(rdpsock.getOutputStream());
	sendMessage(this.CONNECTION_REQUEST);
	code=receiveMessage();
	if (code != this.CONNECTION_CONFIRM) {
	    throw new RdesktopException("Expected CC got:" + Integer.toHexString(code).toUpperCase());
	} 
    }
    
    /*public void connect(String host, int port) throws UnknownHostException, IOException, RdesktopException, SocketException {
	connect(InetAddress.getByName(host), port);
    }
    
    public void connect(InetAddress host) throws SocketException, RdesktopException, IOException {
	connect(host, Constants.PORT);
    }
    
    public void connect(String host) throws UnknownHostException, RdesktopException, IOException, SocketException {
	connect(host, Constants.PORT);
	}*/
    
    /**
     * Send a self contained iso-pdu
     *
     * @param type one of the following CONNECT_RESPONSE, DISCONNECT_REQUEST
     * @exception IOException when an I/O Error occurs
     */
    private void sendMessage(int type) throws IOException {
	Packet buffer = getMemory(11);
	byte[] packet=new byte[11];

	buffer.set8(this.PROTOCOL_VERSION); // send Version Info
	buffer.set8(0); // reserved byte
	buffer.setBigEndian16(11); // Length
	buffer.set8(6); // Length of Header 
       
	buffer.set8(type); //where code = CR or DR
	buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)
   
	buffer.setBigEndian16(0); // source reference should be a reasonable address we use 0
	buffer.set8(0); //service class 
	buffer.copyToByteArray(packet, 0, 0, packet.length);
	out.write(packet);
	out.flush();
    }
    
    /** receive a self contained ISO-PDU
     * @return type of message (CC, DT, ERR)
     * @exception IOException when an I/O Error occurs
     * @exception RdesktopException when we get unexpexted protocol data
     */
    private int receiveMessage() throws IOException, RdesktopException {
	int type=0;
	int length=0;
	int version=0;
	
	version=in.read();
	if (version != this.PROTOCOL_VERSION) {
	    throw new RdesktopException("Version " + version + " not supported!");
	}

	in.skipBytes(1); //skip the reserved byte
	length = (((in.read()<< 8) &0x0000ff00)|(in.read()&0x000000ff));

	in.read(); //skip the header length Info

	type = in.read();
	
	if (type == this.DATA_TRANSFER) {
	    if(debug) Debug.out.println("Data Transfer");
	    in.skipBytes(1); //skip EOT
	}
	else {
	    in.skipBytes(5); //skip rest of header
	    if(debug) Debug.out.println("NO Data Transfer");
	}
	
	return type;
    }
    
    public void send(Packet buffer) throws RdesktopException, IOException {
	int counter=0;
	if (buffer.getEnd() < 0) {
	    throw new RdesktopException("No End Mark!");
	} else {
	    int length = buffer.getEnd();
	    byte[] packet = new byte[length];
	    //Packet data = this.getMemory(length+7);
	    buffer.setPosition(0);
	    buffer.set8(this.PROTOCOL_VERSION); // Version
	    buffer.set8(0); // reserved
	    buffer.setBigEndian16(length); //length of packet
	    
	    buffer.set8(2); //length of header
	    buffer.set8(this.DATA_TRANSFER);
	    buffer.set8(this.EOT);
	    buffer.copyToByteArray(packet, 0, 0, buffer.getEnd());
	    if(debug) dump.encode(packet, Debug.out);
	    out.write(packet);
	    out.flush();
	}
    }
   
    public Packet receive() throws IOException, RdesktopException {
	Packet buffer = null;
	byte[] packet = null;
	int type = 0;
	int length=0;
	int version=0;
	
	version=in.read();
	
	if(version == -1) {
	    throw new EOFException();
	} else if (version != this.PROTOCOL_VERSION) {
	    throw new RdesktopException("Version " + version + " not supported!");
	}
	in.skipBytes(1); //skip the reserved byte

	length = (((in.read()<< 8) &0x0000ff00)|(in.read()&0x000000ff));
		
	in.skipBytes(1); //skip the header length Info
	type = in.read();
	
	if (type == this.DATA_TRANSFER) {
	    in.skipBytes(1); //skip EOT
	}
	else {
	    in.skipBytes(5); //skip rest of header
	}

	if (type != this.DATA_TRANSFER) {
	    throw new RdesktopException("Expected DT got:" + type);
	}
	packet = new byte[length-7];
	in.readFully(packet);
	if(debug) dump.encode(packet, Debug.out);

	buffer = this.getMemory(packet.length);
	buffer.copyFromByteArray(packet, 0, 0, packet.length);
	buffer.markEnd(packet.length);
	buffer.setStart(buffer.getPosition());
	return buffer;
    }

    public Packet getMemory(int size) {
	Memory buffer = memorymanager.alloc(size);
	Packet packet = new Packet(buffer);
	return packet;
    }

    public void disconnect() {
	try { 
	    sendMessage(this.DISCONNECT_REQUEST);
	    in.close();
	    out.close();
	    rdpsock.close();
	} catch(IOException e) {
	    in=null;
	    out=null;
	    rdpsock=null;
	    return;
	}
	in=null;
	out=null;
	rdpsock=null;
    }
    
}
