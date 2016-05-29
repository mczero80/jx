package jx.rdp;

import java.io.*;
import java.net.*;
import jx.net.*;
import jx.rdp.crypto.*;
import jx.zero.Debug;
import java.awt.*;
import java.awt.image.*;

public class Rdp {

    /* constants for RDP Layer */
    public static final int RDP_LOGON_NORMAL = 0x33;
    public static final int RDP_LOGON_AUTO = 0x8;
    
    // PDU Types
    private static final int RDP_PDU_DEMAND_ACTIVE = 1;
    private static final int RDP_PDU_CONFIRM_ACTIVE = 3;
    private static final int RDP_PDU_DEACTIVATE = 6;
    private static final int RDP_PDU_DATA = 7;
    
    // Data PDU Types
    private static final int RDP_DATA_PDU_UPDATE = 2;
    private static final int RDP_DATA_PDU_CONTROL = 20;
    private static final int RDP_DATA_PDU_POINTER = 27;
    private static final int RDP_DATA_PDU_INPUT = 28;
    private static final int RDP_DATA_PDU_SYNCHRONISE = 31;
    private static final int RDP_DATA_PDU_BELL = 34;
    private static final int RDP_DATA_PDU_LOGON = 38;
    private static final int RDP_DATA_PDU_FONT2 = 39;

    // Control PDU types
    private static final int RDP_CTL_REQUEST_CONTROL = 1;
    private static final int RDP_CTL_GRANT_CONTROL = 2;
    private static final int RDP_CTL_DETACH = 3;
    private static final int RDP_CTL_COOPERATE = 4;

    // Update PDU Types
    private static final int RDP_UPDATE_ORDERS = 0;
    private static final int RDP_UPDATE_BITMAP = 1;
    private static final int RDP_UPDATE_PALETTE = 2;
    private static final int RDP_UPDATE_SYNCHRONIZE = 3;

    // Pointer PDU Types
    private static final int RDP_POINTER_MOVE = 3;
    private static final int RDP_POINTER_COLOR = 6;
    private static final int RDP_POINTER_CACHED = 7;

    // Input Devices
    private static final int RDP_INPUT_SYNCHRONIZE = 0;
    private static final int RDP_INPUT_CODEPOINT = 1;
    private static final int RDP_INPUT_VIRTKEY = 2;
    private static final int RDP_INPUT_SCANCODE = 4;
    private static final int RDP_INPUT_MOUSE = 0x8001;

    /* RDP capabilities */
    private static final int RDP_CAPSET_GENERAL = 1;
    private static final int RDP_CAPLEN_GENERAL = 0x18;
    private static final int OS_MAJOR_TYPE_UNIX = 4;
    private static final int OS_MINOR_TYPE_XSERVER = 7;
	
    private static final int RDP_CAPSET_BITMAP = 2;
    private static final int RDP_CAPLEN_BITMAP = 0x1C;

    private static final int RDP_CAPSET_ORDER = 3;
    private static final int RDP_CAPLEN_ORDER = 0x58;
    private static final int ORDER_CAP_NEGOTIATE = 2;
    private static final int ORDER_CAP_NOSUPPORT = 4;
    
    private static final int RDP_CAPSET_BMPCACHE = 4;
    private static final int RDP_CAPLEN_BMPCACHE = 0x28;

    private static final int RDP_CAPSET_CONTROL = 5;
    private static final int RDP_CAPLEN_CONTROL = 0x0C;

    private static final int RDP_CAPSET_ACTIVATE = 7;
    private static final int RDP_CAPLEN_ACTIVATE = 0x0C;

    private static final int RDP_CAPSET_POINTER = 8;
    private static final int RDP_CAPLEN_POINTER = 0x08;

    private static final int RDP_CAPSET_SHARE = 9;
    private static final int RDP_CAPLEN_SHARE = 0x08;

    private static final int RDP_CAPSET_COLCACHE = 10;
    private static final int RDP_CAPLEN_COLCACHE = 0x08;

    private static final int RDP_CAPSET_UNKNOWN = 13;
    private static final int RDP_CAPLEN_UNKNOWN = 0x9C;

    private static final byte[] RDP_SOURCE = {(byte)0x4D, (byte)0x53, (byte)0x54, (byte)0x53, (byte)0x43, (byte)0x00}; //string MSTSC encoded as 7 byte US-Ascii
    
    private Secure SecureLayer= null;
    private RdesktopFrame frame = null;
    private RdesktopCanvas surface = null;
    private Orders orders = null;
    private Cache cache = null;
    
    private int next_packet=0;
    private int rdp_shareid=0; 

    private Packet stream=null;

    private final byte[] canned_caps = {
	(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x04, (byte)0x00, (byte)0x00,
	(byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x0C, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x00, (byte)0x08, (byte)0x00,
	(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x08, (byte)0x00,
	(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x34, (byte)0x00,
	(byte)0xFE, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0xFE, (byte)0x00, (byte)0x04, (byte)0x00,
	(byte)0xFE, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0xFE, (byte)0x00, (byte)0x08, (byte)0x00,
	(byte)0xFE, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0xFE, (byte)0x00, (byte)0x20, (byte)0x00,
	(byte)0xFE, (byte)0x00, (byte)0x40, (byte)0x00, (byte)0xFE, (byte)0x00, (byte)0x80, (byte)0x00,
	(byte)0xFE, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x40, (byte)0x00, (byte)0x00, (byte)0x08,
	(byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00
    };

    public Rdp() {
	this.SecureLayer= new Secure();
	this.orders = new Orders();
	this.cache = new Cache();
	orders.registerCache(cache);
    }

    public static void sendUnicodeString(Packet data, String hostname, int hostlen) {
	int i=0, j=0;
	
	if (hostname.length() != 0) {
	    char[] name=hostname.toCharArray();
	    while (i < hostlen) {
		data.setLittleEndian16((short)name[j++]);
		i+=2;
	    }
	    data.setLittleEndian16(0); //Terminating Null Character
	} else {
	    data.setLittleEndian16(0);
	}
    }

    private Packet init(int size) throws RdesktopException {
	Packet buffer = null;

	buffer = SecureLayer.init(Constants.getEncryptionStatus() ? Secure.SEC_ENCRYPT:0, size + 6);
	buffer.setHeader(Packet.RDP_HEADER);
	buffer.incrementPosition(6);
	buffer.setStart(buffer.getPosition());
	return buffer;
    }

    private void send(Packet data, int pdu_type) throws RdesktopException, IOException, CryptoException {
	int length=0;
	data.setPosition(data.getHeader(Packet.RDP_HEADER));
	length=data.getEnd()-data.getPosition();
	data.setLittleEndian16(length);
	data.setLittleEndian16(pdu_type | 0x10);
	data.setLittleEndian16(SecureLayer.getUserID()+1001);
	SecureLayer.send(data, Constants.getEncryptionStatus() ? Secure.SEC_ENCRYPT : 0);
    }

    private Packet initData(int size) throws RdesktopException {
	Packet buffer = null;

	buffer = SecureLayer.init(Constants.getEncryptionStatus() ? Secure.SEC_ENCRYPT:0, size + 18);
	buffer.setHeader(Packet.RDP_HEADER);
	buffer.incrementPosition(18);
	buffer.setStart(buffer.getPosition());
	return buffer;
    }
	
    private void sendData(Packet data, int data_pdu_type) throws RdesktopException, IOException, CryptoException {
	
	int length;
	
	data.setPosition(data.getHeader(Packet.RDP_HEADER));
	length = data.getEnd() - data.getPosition();

	data.setLittleEndian16(length);
	data.setLittleEndian16(this.RDP_PDU_DATA | 0x10);
	data.setLittleEndian16(SecureLayer.getUserID()+1001);

	data.setLittleEndian32(this.rdp_shareid);
	data.set8(0); // pad
	data.set8(1); // stream id
	data.setLittleEndian16(length - 14);
	data.set8(data_pdu_type);
	data.set8(0); // compression type
	data.setLittleEndian16(0); // compression length

	SecureLayer.send(data, Constants.getEncryptionStatus() ? Secure.SEC_ENCRYPT : 0);
    }
	
 
    private Packet receive() throws  IOException,  RdesktopException, CryptoException {
	int length=0;

	if ((this.stream==null) || (this.next_packet >= this.stream.getEnd())) {
	    this.stream = SecureLayer.receive();
	    this.next_packet = this.stream.getPosition();
	} else {
	    this.stream.setPosition(this.next_packet);
	}
	length = this.stream.getLittleEndian16();
	this.next_packet += length;
	return stream;
    }
    
    public void connect(String username, IPAddress server, int flags, String domain, String password, String command, String directory) throws SocketException, IOException, UnknownHostException, RdesktopException, CryptoException {
	
	SecureLayer.connect(username, server);
	this.sendLogonInfo(flags, domain, username, password, command, directory);
    }

    public void disconnect() {
	SecureLayer.disconnect();
    }


    public void mainLoop() throws IOException, RdesktopException, OrderException, CryptoException {
	int type=0;
	Packet data=null;
	
	while(true) {
	    try {
		data = this.receive();
	    } catch(EOFException e) {
		return;
	    }
	    type = data.getLittleEndian16();
	    
	    if(data.getPosition() != data.getEnd()) {
		data.incrementPosition(2);
	    }
	    
	    type&=0xf;
	    switch(type) {
		
	    case (Rdp.RDP_PDU_DEMAND_ACTIVE):
		this.processDemandActive(data);
		break;

	    case (Rdp.RDP_PDU_DEACTIVATE):
		break;
		
	    case (Rdp.RDP_PDU_DATA):
		this.processData(data);
		break;
		
	    default:
		throw new RdesktopException();
		
	    }
	}
    }	    

    private void sendLogonInfo(int flags, String domain, String username, String password, String command, String directory) throws RdesktopException, IOException, CryptoException {
	int sec_flags = Constants.getEncryptionStatus() ? (Secure.SEC_LOGON_INFO | Secure.SEC_ENCRYPT) : Secure.SEC_LOGON_INFO;
	int domainlen = 2*domain.length();
	int userlen = 2*username.length();
	int passlen = 2*password.length();
	int commandlen = 2*command.length();
	int dirlen = 2*directory.length();



	Packet data = SecureLayer.init(sec_flags, 18 + domainlen + userlen + passlen + commandlen + dirlen + 10);
	
	data.setLittleEndian32(0);
	data.setLittleEndian32(flags);
	data.setLittleEndian16(domainlen);
	data.setLittleEndian16(userlen);
	data.setLittleEndian16(passlen);
	data.setLittleEndian16(commandlen);
	data.setLittleEndian16(dirlen);
	this.sendUnicodeString(data, domain, domainlen);
	this.sendUnicodeString(data, username, userlen);
	this.sendUnicodeString(data, password, passlen);
	this.sendUnicodeString(data, command, commandlen);
	this.sendUnicodeString(data, directory, dirlen);

	data.markEnd();
	byte[] buffer = new byte[data.getEnd()];
	data.copyToByteArray(buffer, 0, 0, data.getEnd());
	SecureLayer.send(data, sec_flags);
    }

    private void processDemandActive(Packet data) throws RdesktopException, IOException, CryptoException {
	int type;
	Packet buffer=null;

	this.rdp_shareid = data.getLittleEndian32();

	this.sendConfirmActive();

	this.sendSynchronize();
	this.sendControl(this.RDP_CTL_COOPERATE);
	this.sendControl(this.RDP_CTL_REQUEST_CONTROL);

	buffer = this.receive(); // Receive RDP_PDU_SYNCHRONIZE
	buffer = this.receive(); // Receive RDP_CTL_COOPERATE
	buffer = this.receive(); // Receive RDP_CTL_GRANT_CONTROL

	this.sendInput(0, this.RDP_INPUT_SYNCHRONIZE, 0, 0, 0);
	this.sendFonts(1);
	this.sendFonts(2);

	buffer = this.receive(); // Receive an unknown PDU Code = 0x28

	this.orders.resetOrderState();
    }
	
    private void processData(Packet data) throws RdesktopException, OrderException {
	int data_type=0;
	
	data.incrementPosition(8); // skip shareid, pad, streamid, length
	data_type = data.get8();
	data.incrementPosition(3); // skip compression type, compression length

	switch(data_type) {

	case (Rdp.RDP_DATA_PDU_UPDATE):
	    this.processUpdate(data);
	    break;
	case (Rdp.RDP_DATA_PDU_POINTER):
	    this.processPointer(data);
	    break;
	case (Rdp.RDP_DATA_PDU_BELL):
	    Toolkit tx = Toolkit.getDefaultToolkit();
	    tx.beep();
	    break;
	case (Rdp.RDP_DATA_PDU_LOGON):
	    break; // User is logged on therfore end
	default:
	    throw new RdesktopException("Unimplemented!");

	}

    }

    private void processUpdate(Packet data) throws OrderException, RdesktopException {
	int update_type=0;
	
	update_type = data.getLittleEndian16();

	switch(update_type) {

	case (Rdp.RDP_UPDATE_ORDERS):
	    this.orders.processOrders(data, next_packet);
	    break;
	case (Rdp.RDP_UPDATE_BITMAP):
	    this.processBitmapUpdates(data);
	    break;
	case (Rdp.RDP_UPDATE_PALETTE):
	    this.processPalette(data);
	    break;
	case (Rdp.RDP_UPDATE_SYNCHRONIZE):
	    break;
	default:
	    throw new RdesktopException("Unimplemented!");
	}
    }
    
    private void sendConfirmActive() throws RdesktopException, IOException, CryptoException {
	int caplen = this.RDP_CAPLEN_GENERAL + this.RDP_CAPLEN_BITMAP + this.RDP_CAPLEN_ORDER +
	    this.RDP_CAPLEN_BMPCACHE + this.RDP_CAPLEN_COLCACHE + this.RDP_CAPLEN_ACTIVATE + 
	  this.RDP_CAPLEN_CONTROL + this.RDP_CAPLEN_POINTER + this.RDP_CAPLEN_SHARE + this.RDP_CAPLEN_UNKNOWN + 4; //this is a fix for W2k. Purpose unknown

	Packet data = this.init(14 + caplen + this.RDP_SOURCE.length);

	data.setLittleEndian32(this.rdp_shareid);
	data.setLittleEndian16(0x3ea); // user id
	data.setLittleEndian16(this.RDP_SOURCE.length);
	data.setLittleEndian16(caplen);
	
	data.copyFromByteArray(this.RDP_SOURCE, 0, data.getPosition(), this.RDP_SOURCE.length);
	data.incrementPosition(this.RDP_SOURCE.length);
	data.setLittleEndian16(0xd); // num_caps
	data.incrementPosition(2); //pad

	this.sendGeneralCaps(data);
	//ta.incrementPosition(this.RDP_CAPLEN_GENERAL);
	this.sendBitmapCaps(data);
	this.sendOrderCaps(data);
	this.sendBitmapcacheCaps(data);
	this.sendColorcacheCaps(data);
	this.sendActivateCaps(data);
	this.sendControlCaps(data);
	this.sendPointerCaps(data);
	this.sendShareCaps(data);
	this.sendUnknownCaps(data);
	
	data.markEnd();
	this.send(data, this.RDP_PDU_CONFIRM_ACTIVE);
    }

    private void sendGeneralCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_GENERAL);
	data.setLittleEndian16(this.RDP_CAPLEN_GENERAL);
	
	data.setLittleEndian16(1);	/* OS major type */
	data.setLittleEndian16(3);	/* OS minor type */
	data.setLittleEndian16(0x200);	/* Protocol version */
	data.setLittleEndian16(0);	/* Pad */
	data.setLittleEndian16(0);	/* Compression types */
	data.setLittleEndian16(0);	/* Pad */
	data.setLittleEndian16(0);	/* Update capability */
	data.setLittleEndian16(0);	/* Remote unshare capability */
	data.setLittleEndian16(0);	/* Compression level */
	data.setLittleEndian16(0);	/* Pad */
    }

    private void sendBitmapCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_BITMAP);
	data.setLittleEndian16(this.RDP_CAPLEN_BITMAP);
	
	data.setLittleEndian16(8);	/* Preferred BPP */
	data.setLittleEndian16(1);	/* Receive 1 BPP */
	data.setLittleEndian16(1);	/* Receive 4 BPP */
	data.setLittleEndian16(1);	/* Receive 8 BPP */
	data.setLittleEndian16(800);	/* Desktop width */ /* FIXME */
	data.setLittleEndian16(600);	/* Desktop height */ /* FIXME */
	data.setLittleEndian16(0);	/* Pad */
	data.setLittleEndian16(0);	/* Allow resize */
	data.setLittleEndian16(Constants.bitmap_compression ? 1 : 0);	/* Support compression */
	data.setLittleEndian16(0);	/* Unknown */
	data.setLittleEndian16(1);	/* Unknown */
	data.setLittleEndian16(0);	/* Pad */
    }
    
    private void sendOrderCaps(Packet data) {

	byte[] order_caps = new byte[32];

	order_caps[0] = 0;	/* dest blt */
	order_caps[1] = 0;	/* pat blt */
	order_caps[2] = 0;	/* screen blt */
	order_caps[8] = 0;	/* line */
	order_caps[9] = 0;	/* line */
	order_caps[10] = 0;	/* rect */
	order_caps[11] = (Constants.desktop_save ? 1 : 0);	/* desksave */
	order_caps[13] = 0;	/* memblt */
	order_caps[14] = 0;	/* triblt */
	order_caps[22] = 0;	/* polyline */
	order_caps[27] = 0;	/* text2 */
	data.setLittleEndian16(this.RDP_CAPSET_ORDER);
	data.setLittleEndian16(this.RDP_CAPLEN_ORDER);

	data.incrementPosition(20);	/* Terminal desc, pad */
	data.setLittleEndian16(1);	/* Cache X granularity */
	data.setLittleEndian16(20);	/* Cache Y granularity */
	data.setLittleEndian16(0);	/* Pad */
	data.setLittleEndian16(1);	/* Max order level */
	data.setLittleEndian16(0x147);	/* Number of fonts */
	data.setLittleEndian16(0x2a);	/* Capability flags */
	data.copyFromByteArray(order_caps,0, data.getPosition(), 32);	/* Orders supported */
	data.incrementPosition(32);
	data.setLittleEndian16(0x6a1);	/* Text capability flags */
	data.incrementPosition(6);	/* Pad */
	data.setLittleEndian32(Constants.desktop_save ? 0x38400 : 0);	/* Desktop cache size */
	data.setLittleEndian32(0);	/* Unknown */
	data.setLittleEndian32(0x4e4);	/* Unknown */
    }

    private void sendBitmapcacheCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_BMPCACHE);
	data.setLittleEndian16(this.RDP_CAPLEN_BMPCACHE);

	data.incrementPosition(24);	/* unused */
	data.setLittleEndian16(0x258);	/* entries */
	data.setLittleEndian16(0x100);	/* max cell size */
	data.setLittleEndian16(0x12c);	/* entries */
	data.setLittleEndian16(0x400);	/* max cell size */
	data.setLittleEndian16(0x106);	/* entries */
	data.setLittleEndian16(0x1000);	/* max cell size */
    }

    private void sendColorcacheCaps(Packet data) {
	
	data.setLittleEndian16(this.RDP_CAPSET_COLCACHE);
	data.setLittleEndian16(this.RDP_CAPLEN_COLCACHE);

	data.setLittleEndian16(6);	/* cache size */
	data.setLittleEndian16(0);	/* pad */
    }

    private void sendActivateCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_ACTIVATE);
	data.setLittleEndian16(this.RDP_CAPLEN_ACTIVATE);
	
	data.setLittleEndian16(0);	/* Help key */
	data.setLittleEndian16(0);	/* Help index key */
	data.setLittleEndian16(0);	/* Extended help key */
	data.setLittleEndian16(0);	/* Window activate */
    }

    private void sendControlCaps(Packet data) {
	
	data.setLittleEndian16(this.RDP_CAPSET_CONTROL);
	data.setLittleEndian16(this.RDP_CAPLEN_CONTROL);
	
	data.setLittleEndian16(0);	/* Control capabilities */
	data.setLittleEndian16(0);	/* Remote detach */
	data.setLittleEndian16(2);	/* Control interest */
	data.setLittleEndian16(2);	/* Detach interest */
    }
    
    private void sendPointerCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_POINTER);
	data.setLittleEndian16(this.RDP_CAPLEN_POINTER);
	
	data.setLittleEndian16(0);	/* Colour pointer */
	data.setLittleEndian16(20);	/* Cache size */
    }

    private void sendShareCaps(Packet data) {
	
	data.setLittleEndian16(this.RDP_CAPSET_SHARE);
	data.setLittleEndian16(this.RDP_CAPLEN_SHARE);

	data.setLittleEndian16(0);	/* userid */
	data.setLittleEndian16(0);	/* pad */
    }

    private void sendUnknownCaps(Packet data) {

	data.setLittleEndian16(this.RDP_CAPSET_UNKNOWN);
	data.setLittleEndian16(0x58);

	data.copyFromByteArray(canned_caps,0 ,data.getPosition(), this.RDP_CAPLEN_UNKNOWN - 4);
	data.incrementPosition(this.RDP_CAPLEN_UNKNOWN - 4);
    }

    private void sendSynchronize() throws RdesktopException, IOException, CryptoException {
	Packet data = this.initData(4);

	data.setLittleEndian16(1); // type
	data.setLittleEndian16(1002);

	data.markEnd();
	this.sendData(data, this.RDP_DATA_PDU_SYNCHRONISE);
    }

    private void sendControl(int action) throws RdesktopException, IOException, CryptoException {

	Packet data = this.initData(8);

	data.setLittleEndian16(action);
	data.setLittleEndian16(0); // userid
	data.setLittleEndian32(0); // control id

	data.markEnd();
	this.sendData(data, this.RDP_DATA_PDU_CONTROL);
    }

    public void sendInput(int time, int message_type, int device_flags, int param1, int param2) {
	Packet data = null;
	try {
	    data = this.initData(16);
	} catch(RdesktopException p) {
	    Debug.out.println("Bei der Uebertragung ist ein Fehler aufgetreten!");
	    Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + p.getMessage());
	    frame.setVisible(false);
	    //frame.dispose();
	    this.disconnect();
	    System.exit(0);
	}

	data.setLittleEndian16(1);	/* number of events */
	data.setLittleEndian16(0);	/* pad */

	data.setLittleEndian32(time);
	data.setLittleEndian16(message_type);
	data.setLittleEndian16(device_flags);
	data.setLittleEndian16(param1);
	data.setLittleEndian16(param2);

	data.markEnd();
	try {
	    this.sendData(data, this.RDP_DATA_PDU_INPUT);
	} catch(CryptoException l) {
	    Debug.out.println("Es konnte kein Schluessel registriert werden!");
	    Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + l.getMessage());
	    frame.setVisible(false);
	    //frame.dispose();
	    this.disconnect();
	    System.exit(0);
	} catch(IOException n) {
	    Debug.out.println("Bei der Uebertragung ist ein Fehler aufgetreten!");
	    Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + n.getMessage());
	    frame.setVisible(false);
	    //frame.dispose();
	    System.exit(0);
	} catch(RdesktopException o) {
	    Debug.out.println("Bei der Uebertragung ist ein Fehler aufgetreten!");
	    Debug.out.println("Der genaue Wortlaut der Fehlermeldung ist: " + o.getMessage());
	    frame.setVisible(false);
	    //frame.dispose();
	    this.disconnect();
	    System.exit(0);
	}
    }

    private void sendFonts(int seq) throws RdesktopException, IOException, CryptoException {

	Packet data = this.initData(8);

	data.setLittleEndian16(0);	/* number of fonts */
	data.setLittleEndian16(0x3e);	/* unknown */
	data.setLittleEndian16(seq);	/* unknown */
	data.setLittleEndian16(0x32);	/* entry size */

	data.markEnd();
	this.sendData(data, this.RDP_DATA_PDU_FONT2);
    }
 
    private void processPointer(Packet data)throws RdesktopException {
	int message_type = 0;
	int x = 0, y = 0, width = 0, height = 0, cache_idx = 0, masklen = 0, datalen = 0;
	byte[] mask = null, pixel = null;
	MSCursor cursor = null;

	message_type = data.getLittleEndian16();
	data.incrementPosition(2);
	switch(message_type) {

	case (Rdp.RDP_POINTER_MOVE):
	    x = data.getLittleEndian16();
	    y = data.getLittleEndian16();

	    if(data.getPosition() <= data.getEnd()) {
		surface.movePointer(x, y);
	    }
	    break;
	    
	case (Rdp.RDP_POINTER_COLOR):
	    cache_idx = data.getLittleEndian16();
	    x = data.getLittleEndian16();
	    y = data.getLittleEndian16();
	    width = data.getLittleEndian16();
	    height = data.getLittleEndian16();
	    masklen = data.getLittleEndian16();
	    datalen = data.getLittleEndian16();
	    mask = new byte[masklen];
	    pixel = new byte[datalen];
	    data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
	    data.incrementPosition(datalen);
	    data.copyToByteArray(mask, 0, data.getPosition(), masklen);
	    data.incrementPosition(masklen);
	    cursor = surface.createCursor(x, y, width, height, mask, pixel); 
	    //surface.setCursor(cursor);
	    cache.putCursor(cache_idx, cursor);
	    break;

	case (Rdp.RDP_POINTER_CACHED):
	    cache_idx = data.getLittleEndian16();
	    //surface.setCursor(cache.getCursor(cache_idx));
	    break;

	default:
	    break;
	}
    }

    private void processBitmapUpdates(Packet data) throws RdesktopException {
	int n_updates=0;
	int left=0, top=0, right=0, bottom=0, width=0, height=0;
	int cx=0, cy=0, bitsperpixel=0, compression=0, buffersize=0, size=0;
	byte[] compressed_pixel=null, pixel=null;

	n_updates = data.getLittleEndian16();

	for(int i=0; i < n_updates; i++) {

	    left = data.getLittleEndian16();
	    top = data.getLittleEndian16();
	    right = data.getLittleEndian16();
	    bottom = data.getLittleEndian16();
	    width = data.getLittleEndian16();
	    height = data.getLittleEndian16();
	    bitsperpixel = data.getLittleEndian16();
	    compression = data.getLittleEndian16();
	    buffersize = data.getLittleEndian16();

	    cx = right - left+1;
	    cy = bottom - top+1;
	    
	    pixel = new byte[width*height];

	    if(compression == 0){
		for(int y = 0; y < height; y++) {
		    data.copyToByteArray(pixel, (height - y-1)*width, data.getPosition(), width);
		    data.incrementPosition(width);
		}

		surface.displayImage(pixel, width, height, left, top, cx, cy);
		continue;
	    }
	    
	    data.incrementPosition(2); //pad
	    size = data.getLittleEndian16();

	    data.incrementPosition(4); //line size, final size
	    compressed_pixel = new byte[size];
	    data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
	    data.incrementPosition(size);
	    pixel = this.decompress(compressed_pixel, width, height, size);
	    surface.displayImage(pixel, width, height, left, top, cx, cy);
	    	    
	}
    }

    private void processPalette(Packet data) {
	int n_colors=0;
	//IndexColorModel cm = null;
	byte[] palette = null;
	
	byte[] red = null;
	byte[] green = null;
	byte[] blue = null;
	int j = 0;
	
	data.incrementPosition(2); //pad
	n_colors = data.getLittleEndian16(); // Number of Colors in Palette
	data.incrementPosition(2); //pad
	palette = new byte[n_colors*3];
	red = new byte[n_colors];
	green = new byte[n_colors];
	blue = new byte[n_colors];
	data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
	data.incrementPosition(palette.length);
	for(int i = 0; i < n_colors; i++) {
	    red[i] = palette[j];
	    green[i] = palette[j+1];
	    blue[i] = palette[j+2];
	    j+=3;
	}
	//cm = new IndexColorModel(8, n_colors, red, green ,blue);
	//surface.registerPalette(cm);
    }

    public void registerDrawingSurface(RdesktopFrame fr) {
	RdesktopCanvas ds = fr.getCanvas();
	this.surface=ds;
	orders.registerDrawingSurface(ds);
    }

    private byte[] decompress(byte[] data, int width, int height, int size) throws RdesktopException {
	int previous=0, line=0;
	int input = 0, output = 0, end = size;
	int opcode = 0, count = 0, offset = 0, x = width;
	int lastopcode = -1, fom_mask = 0;
	int code = 0, color1 = 0, color2 = 0; 
	byte mixmask = 0, mask = 0, mix = (byte)0xff;

	boolean insertmix = false, bicolor = false, isfillormix = false;

	byte[] pixel = new byte[width*height];
	while(input < end) {
	    
	    fom_mask = 0;
	    code = (data[input++]&0x000000ff);
	    opcode = code >> 4;

	    /* Handle different opcode forms */
	    switch (opcode) {
	    case 0xc:
	    case 0xd:
	    case 0xe:
		opcode -= 6;
		count = code & 0xf;
		offset = 16;
		break;
		
	    case 0xf:
		opcode = code & 0xf;
		if (opcode < 9) {
		    count = (data[input++]&0x000000ff);
		    count |= ((data[input++]&0x000000ff) << 8);
		}
		else {
		    count = (opcode < 0xb) ? 8 : 1;
		}
		offset = 0;
		break;
		
	    default:
		opcode >>= 1;
		count = code & 0x1f;
		offset = 32;
		break;
	    }
	    
	    
	    /* Handle strange cases for counts */
	    if (offset != 0) {
		isfillormix = ((opcode == 2) || (opcode == 7));
		
		if (count == 0) {
		    if (isfillormix)
			count = (data[input++]&0x000000ff) + 1;
		    else
			count = (data[input++]&0x000000ff) + offset;
		}
		else if (isfillormix) {
		    count <<= 3;
		}
	    }
	    
	    switch (opcode) {
	    case 0:	/* Fill */
		if ((lastopcode == opcode) && !((x == width) && (previous == 0)))
		    insertmix = true;
		break;
	    case 8:	/* Bicolor */
		color1 = (data[input++]&0x000000ff);
	    case 3:	/* Color */
		color2 = (data[input++]&0x000000ff);
		break;
	    case 6:	/* SetMix/Mix */
	    case 7:	/* SetMix/FillOrMix */
		mix = data[input++];
		opcode -= 5;
		break;
	    case 9:	/* FillOrMix_1 */
		mask = 0x03;
		opcode = 0x02;
		fom_mask = 3;
		break;
	    case 0x0a:	/* FillOrMix_2 */
		mask = 0x05;
		opcode = 0x02;
		fom_mask = 5;
		break;
		
	    }
	    
	    lastopcode = opcode;
	    mixmask = 0;
	    
	    /* Output body */
	    while (count > 0) {
		if (x >= width) {
		    if (height <= 0)
			throw new RdesktopException();
		    
		    x = 0;
		    height--;
		    
		    previous = line;
		    line = height * width;
		}
		
		switch (opcode) {
		case 0:	/* Fill */
		    if (insertmix==true) {
			if (previous == 0)
			    pixel[line+x] = (byte)mix;
			else
			    pixel[line+x] = (byte)(pixel[previous+x] ^ (byte)mix);
			
			insertmix = false;
			count--;
			x++;
		    }
		    
		    if (previous == 0) {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) { 
				pixel[line+x] = 0;
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) { 
			    pixel[line+x] = 0;
			    count--; 
			    x++;
			} 
			    
		    }
		    else {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) { 
				pixel[line+x] = pixel[previous+x];
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) { 
			    pixel[line+x] = pixel[previous+x];
			    count--; 
			    x++;
			} 
		    }
		    break;
		    
		case 1:	/* Mix */
		    if (previous == 0) {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) { 
				pixel[line+x] = (byte)mix;
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) { 
			    pixel[line+x] = (byte)mix;
			    count--; 
			    x++;
			} 
		    }
		    else {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) { 
				pixel[line+x] = (byte)(pixel[previous+x] ^ (byte)mix);
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) { 
			    pixel[line+x] = (byte)(pixel[previous+x] ^ (byte)mix);
			    count--; 
			    x++;
			} 
			
		    }
		    break;
		    
		case 2:	/* Fill or Mix */
		    if (previous == 0) {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) {
			       	mixmask <<= 1;
				if (mixmask == 0) {
				    mask = (fom_mask != 0)? (byte)fom_mask : data[input++];
				    mixmask = 1;
				}
				if ((mask & mixmask) !=0)
				    pixel[line+x] = (byte)mix;
				else
				    pixel[line+x] = 0;
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) {
			    mixmask <<= 1;
			    if (mixmask == 0) {
				mask = (fom_mask !=0) ? (byte)fom_mask : data[input++];
				mixmask = 1;
			    }
			    if ((mask & mixmask)!=0)
				pixel[line+x] = (byte)mix;
			    else
				pixel[line+x] = 0;
			    count--; 
			    x++;
			} 
		    }
		    else {
			while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			    for(int i=0; i<8; i++) {
				mixmask <<= 1;
				if (mixmask == 0) {
				    mask = (fom_mask !=0) ? (byte)fom_mask : data[input++];
				    mixmask = 1;
				}
				if ((mask & mixmask)!=0)
				    pixel[line+x] = (byte)(pixel[previous+x] ^ (byte)mix);
				else
				    pixel[line+x] = pixel[previous+x];
				count--;
				x++;
			    }
			}
			while((count > 0) && (x < width)) {
			    mixmask <<= 1;
			    if (mixmask == 0) {
				mask = (fom_mask!=0) ? (byte)fom_mask : data[input++];
				mixmask = 1;
			    }
			    if ((mask & mixmask)!=0)
				pixel[line+x] = (byte)(pixel[previous+x] ^ (byte)mix);
			    else
				pixel[line+x] = pixel[previous+x];
			    count--; 
			    x++;
			} 
			
		    }
		    break;
		    
		case 3:	/* Color */
		    while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			for(int i=0; i<8; i++) { 
			    pixel[line+x] = (byte)color2;
			    count--;
			    x++;
			}
		    }
		    while((count > 0) && (x < width)) { 
			pixel[line+x] = (byte)color2;
			count--; 
			x++;
		    } 
		    
		    break;
		    
		case 4:	/* Copy */
		    while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			for(int i=0; i<8; i++) {
			    pixel[line+x] = data[input++];
			    count--;
			    x++;
			}
		    }
		    while((count > 0) && (x < width)) { 
			pixel[line+x] = data[input++];
			count--; 
			x++;
		    } 
		    
		    break;
		    
		case 8:	/* Bicolor */
		    while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			for(int i=0; i<8; i++) { 
			    if (bicolor) {
				pixel[line+x] = (byte)color2;
				bicolor = false;
			    }
			    else {
				pixel[line+x] = (byte)color1;
				bicolor = true; count++;
			    }
			    count--;
			    x++;
			}
		    }
		    while((count > 0) && (x < width)) { 
			 if (bicolor) {
			    pixel[line+x] = (byte)color2;
			    bicolor = false;
			}
			else {
			    pixel[line+x] = (byte)color1;
			    bicolor = true; count++;
			}
			count--; 
			x++;
		    } 
		    
		    break;
		
		case 0xd:	/* White */
		    while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			for(int i=0; i<8; i++) { 
			    pixel[line+x] = (byte)0xff;
			    count--;
			    x++;
			}
		    }
		    while((count > 0) && (x < width)) { 
			pixel[line+x] = (byte)0xff;
			count--; 
			x++;
		    } 
		    break;
		    
		case 0xe:	/* Black */
		    while(((count & ~0x7)!= 0) && ((x+8) < width)) {
			for(int i=0; i<8; i++) { 
			    pixel[line+x] = (byte)0x00;
			    count--;
			    x++;
			}
		    }
		    while((count > 0) && (x < width)) { 
			pixel[line+x] = (byte)0x00;
			count--; 
			x++;
		    } 
		    
		    break;
		    
		default:
		    throw new RdesktopException("Unimplemented!");;
		}
	    }
	}
	return pixel;
    }
}
