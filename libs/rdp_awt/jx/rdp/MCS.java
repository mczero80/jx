package jx.rdp;

import java.io.*;
import java.net.*;
import jx.net.*;
import jx.zero.*;
import jx.zero.Debug;

public class MCS {

    private ISO IsoLayer=null;
    private int McsUserID;

    /* this for the MCS Layer */
    private static final int CONNECT_INITIAL = 0x7f65;
    private static final int CONNECT_RESPONSE= 0x7f66;
    
    private static final int BER_TAG_BOOLEAN = 1;
    private static final int BER_TAG_INTEGER = 2;
    private static final int BER_TAG_OCTET_STRING = 4;
    private static final int BER_TAG_RESULT = 10;
    private static final int TAG_DOMAIN_PARAMS = 0x30;

    private static final int MCS_GLOBAL_CHANNEL =1003;

    private static final int EDRQ = 1;		/* Erect Domain Request */
    private static final int DPUM = 8;		/* Disconnect Provider Ultimatum */
    private static final int AURQ = 10;		/* Attach User Request */
    private static final int AUCF = 11;		/* Attach User Confirm */
    private static final int CJRQ = 14;		/* Channel Join Request */
    private static final int CJCF = 15;		/* Channel Join Confirm */
    private static final int SDRQ = 25;		/* Send Data Request */
    private static final int SDIN = 26;		/* Send Data Indication */


    public MCS() {
	IsoLayer = new ISO();
    }
    
    public void connect(IPAddress host, int port, Packet data)  throws IOException, RdesktopException, SocketException {
	IsoLayer.connect(host, port);
	this.sendConnectInitial(data);
	this.receiveConnectResponse(data);

	send_edrq();
	send_aurq();

	this.McsUserID=receive_aucf();
	send_cjrq(this.McsUserID+1001);
	receive_cjcf();
	send_cjrq(this.MCS_GLOBAL_CHANNEL);
	receive_cjcf();

    }
    /*
    public void connect(String host, int port, Packet data) throws UnknownHostException, IOException, RdesktopException, SocketException {
	this.connect(InetAddress.getByName(host), port, data);
	}*/
    
    public void connect(IPAddress host, Packet data) throws IOException, RdesktopException, SocketException {
	this.connect(host, Constants.PORT, data);
    }
    /*
    public void connect(String host, Packet data) throws UnknownHostException, IOException, RdesktopException, SocketException {
	this.connect(InetAddress.getByName(host), Constants.PORT, data);
	}*/

    public void disconnect() {
	IsoLayer.disconnect();
	//in=null;
	//out=null;
    }

    public Packet init(int length) throws RdesktopException {
	Packet data = IsoLayer.init(length+8);
	data.setHeader(Packet.MCS_HEADER);
	data.incrementPosition(8);
	data.setStart(data.getPosition());
	return data;
    }
	
    public void send(Packet buffer) throws RdesktopException, IOException {
	int length=0;
	buffer.setPosition(buffer.getHeader(Packet.MCS_HEADER));

	length=buffer.getEnd()-buffer.getHeader(Packet.MCS_HEADER)-8;
	length|=0x8000;
	
	buffer.set8((this.SDRQ << 2));
	buffer.setBigEndian16(this.McsUserID);
	buffer.setBigEndian16(this.MCS_GLOBAL_CHANNEL);
	buffer.set8(0x70); //Flags
	buffer.setBigEndian16(length);
	IsoLayer.send(buffer);
    }

    public Packet receive() throws IOException, RdesktopException {
	int opcode=0, appid=0, length=0;
	Packet buffer=IsoLayer.receive();
	Packet data =null;
	buffer.setHeader(Packet.MCS_HEADER);
	opcode = buffer.get8();

	appid = opcode>>2;

	if (appid != this.SDIN) {
	    if (appid != this.DPUM) {
		throw new RdesktopException("Expected data got" + opcode);
	    }
	    throw new EOFException("Ende der Uebertragung!");
	}

	buffer.incrementPosition(5); // Skip UserID, ChannelID, Flags
	length=buffer.get8();
	
	if((length&0x80)!=0) {
	    buffer.incrementPosition(1);
	}
	buffer.setStart(buffer.getPosition());
	return buffer;
    }
    /**
    * send an Integer encoded according to the ISO ASN.1 Basic Encoding Rules
    */
    public void sendBerInteger(Packet buffer, int value) {
	
	sendBerHeader(buffer, this.BER_TAG_INTEGER, 2);
	buffer.setBigEndian16(value);
    }

    /** 
     * send a Header encoded accordibg to the ISO ASN.1 Basic Encoding rules
     */
    public void sendBerHeader(Packet buffer, int tagval, int length) {
	if (tagval > 0xff) {
	    buffer.setBigEndian16(tagval);
	} else {
	    buffer.set8(tagval);
	}
	
	if (length >= 0x80) {
	    buffer.set8(0x82);
	    buffer.setBigEndian16(length);
	} else {
	    buffer.set8(length);
	}
    }

    /**
     * send a DOMAIN_PARAMS structure encoded according to the ISO ASN.1
     * Basic Encoding rules
     */
    public void sendDomainParams(Packet buffer, int max_channels, int max_users, int max_tokens, int max_pdusize) {
	
	sendBerHeader(buffer,  this.TAG_DOMAIN_PARAMS, 32);
	sendBerInteger(buffer, max_channels);
	sendBerInteger(buffer, max_users);
	sendBerInteger(buffer, max_tokens);
	
	sendBerInteger(buffer, 1);
	sendBerInteger(buffer, 0);
	sendBerInteger(buffer, 1);

	sendBerInteger(buffer, max_pdusize);
	sendBerInteger(buffer, 2);
    }

    /**
     * send a MCS_CONNECT_INITIAL message (encoded as ASN.1 Ber)
     */
    public void sendConnectInitial(Packet data) throws IOException, RdesktopException {
	int length = 7 + (3 *34) + 4 + data.getEnd();
	Packet buffer = IsoLayer.init(length+5);
	
	sendBerHeader(buffer, this.CONNECT_INITIAL, length);
	sendBerHeader(buffer, this.BER_TAG_OCTET_STRING, 0); //calling domain
	sendBerHeader(buffer, this.BER_TAG_OCTET_STRING, 0); // called domain

	sendBerHeader(buffer, this.BER_TAG_BOOLEAN, 1);
	buffer.set8(255); //upward flag

	sendDomainParams(buffer, 2, 2, 0, 0xffff); //target parameters
	sendDomainParams(buffer, 1, 1, 1, 0x420); // minimun parameters
	sendDomainParams(buffer, 0xffff, 0xfc17, 0xffff, 0xffff); //maximum parameters

	sendBerHeader(buffer, this.BER_TAG_OCTET_STRING, data.getEnd());

	data.copyToPacket(buffer, 0, buffer.getPosition(), data.getEnd());
	buffer.incrementPosition(data.getEnd());
	buffer.markEnd();
	IsoLayer.send(buffer);
    }

    public void receiveConnectResponse(Packet data) throws IOException, RdesktopException {
	int result=0;
	int length=0;
	
	Packet buffer = IsoLayer.receive();
       
	length=berParseHeader(buffer, this.CONNECT_RESPONSE);
	length=berParseHeader(buffer, this.BER_TAG_RESULT);

	result=buffer.get8();
	if (result != 0) {
	    throw new RdesktopException("MCS Connect failed! result was " + result);
	}
	length=berParseHeader(buffer, this.BER_TAG_INTEGER);
	length=buffer.get8(); //connect id
	parseDomainParams(buffer);
	length=berParseHeader(buffer, this.BER_TAG_OCTET_STRING);

	if (length > data.size()) {
	    Debug.out.println("MCS Datalength exceeds size!"+length);
	    length=data.size();
	}
	data.copyFromPacket(buffer, buffer.getPosition(), 0, length);
	data.setPosition(0);
	data.markEnd(length);
	buffer.incrementPosition(length);
	
	if (buffer.getPosition() != buffer.getEnd()) {
	    throw new RdesktopException();
	}
    }

    public void send_edrq() throws IOException, RdesktopException {
	Packet buffer = IsoLayer.init(5);
	buffer.set8(this.EDRQ << 2);
	buffer.setBigEndian16(1); //height
	buffer.setBigEndian16(1); //interval
	buffer.markEnd();
	IsoLayer.send(buffer);
    }
    
    public void send_cjrq(int channelid) throws IOException, RdesktopException {
	Packet buffer = IsoLayer.init(5);
	buffer.set8(this.CJRQ << 2);
	buffer.setBigEndian16(this.McsUserID); //height
	buffer.setBigEndian16(channelid); //interval
	buffer.markEnd();
	IsoLayer.send(buffer);
    }

    public void send_aucf() throws IOException, RdesktopException {
	Packet buffer = IsoLayer.init(2);
	
	buffer.set8(this.AUCF << 2);
	buffer.set8(0);
	buffer.markEnd();
	IsoLayer.send(buffer);
    }

    public void send_aurq() throws IOException, RdesktopException {
	Packet buffer = IsoLayer.init(1);

	buffer.set8(this.AURQ <<2);
	buffer.markEnd();
	IsoLayer.send(buffer);
    }

    public void receive_cjcf() throws IOException, RdesktopException {
	int opcode=0, result=0;
	Packet buffer = IsoLayer.receive();
	
	opcode=buffer.get8();
	if ((opcode >>2) != this.CJCF) {
	    throw new RdesktopException("Expected CJCF got" + opcode);
	}

	result=buffer.get8();
	if (result!=0) {
	    throw new RdesktopException("Expected CJRQ got" + result);
	}
	
	buffer.incrementPosition(4); //skip userid, req_channelid

	if ((opcode&2)!=0) {
	    buffer.incrementPosition(2); // skip join_channelid
	}

	if (buffer.getPosition() != buffer.getEnd()){
	    throw new RdesktopException();
	}
    }

    public int receive_aucf() throws IOException, RdesktopException {
	int opcode=0, result=0, UserID=0;
	Packet buffer = IsoLayer.receive();
	
	opcode=buffer.get8();
	if ((opcode >>2) != this.AUCF) {
	    throw new RdesktopException("Expected AUCF got" + opcode);
	}

	result=buffer.get8();
	if (result!=0) {
	    throw new RdesktopException("Expected AURQ got" + result);
	}

	if ((opcode&2)!=0) {
	    UserID=buffer.getBigEndian16();
	}

	if (buffer.getPosition() != buffer.getEnd()){
	    throw new RdesktopException();
	}
	return UserID;
    }

    public int berParseHeader(Packet data, int tagval) throws RdesktopException {
	int tag=0;
	int length=0;
	int len;

	if (tagval > 0x000000ff) {
	    tag = data.getBigEndian16();
	} else {
	    tag = data.get8();
	}

	if (tag !=tagval) {
	    throw new RdesktopException("Unexpected tag got " + tag + " expected " +tagval);
	}
    
	len=data.get8();
	
	if ((len&0x00000080)!=0) { 
	    len &= ~0x00000080; // subtract 128
	    length = 0;
	    while(len--!=0){
		length=(length << 8)+data.get8();
	    }
	} else {
	    length=len;
	}

	return length;
    }

    public void parseDomainParams(Packet data) throws RdesktopException {
	int length;

	length = this.berParseHeader(data, this.TAG_DOMAIN_PARAMS);
	data.incrementPosition(length);

	if (data.getPosition() > data.getEnd()) {
	    throw new RdesktopException();
	}
    }

    public Packet getMemory(int size) {
	return IsoLayer.getMemory(size);
    }
    
    public int getUserID() {
	return this.McsUserID;
    }
}
