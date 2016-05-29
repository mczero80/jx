package jx.rdp;
import java.io.*;
import java.util.*;
import java.net.*;
import jx.net.*;
import java.math.*;
import jx.zero.*;
import jx.zero.Debug;
import jx.rdp.crypto.*;

public class Secure {

    /* constants for the secure layer */
    public static final int SEC_ENCRYPT = 0x0008;
    public static final int SEC_LOGON_INFO = 0x0040;

    private static final int SEC_RANDOM_SIZE = 32;
    private static final int SEC_MODULUS_SIZE = 64;
    private static final int SEC_PADDING_SIZE = 8;
    private static final int SEC_EXPONENT_SIZE = 4;

    private static final int SEC_CLIENT_RANDOM = 0x0001;
    private static final int SEC_LICENCE_NEG = 0x0080;

    private static final int SEC_TAG_SRV_INFO = 0x0c01;
    private static final int SEC_TAG_SRV_CRYPT = 0x0c02;
    private static final int SEC_TAG_SRV_3 = 0x0c03;

    private static final int SEC_TAG_CLI_INFO = 0xc001;
    private static final int SEC_TAG_CLI_CRYPT = 0xc002;

    private static final int SEC_TAG_PUBKEY = 0x0006;
    private static final int SEC_TAG_KEYSIG = 0x0008;

    private static final int SEC_RSA_MAGIC = 0x31415352; /* RSA1 */


    /* constants for the licence negotiation */

    private static final int LICENCE_TOKEN_SIZE = 10;
    private static final int LICENCE_HWID_SIZE = 20;
    private static final int LICENCE_SIGNATURE_SIZE = 16;

    private static final int LICENCE_TAG_DEMAND = 0x0201;
    private static final int LICENCE_TAG_AUTHREQ	= 0x0202;
    private static final int LICENCE_TAG_ISSUE = 0x0203;
    private static final int LICENCE_TAG_REQUEST = 0x0213;
    private static final int LICENCE_TAG_AUTHRESP = 0x0215;
    private static final int LICENCE_TAG_RESULT = 0x02ff;

    private static final int LICENCE_TAG_USER = 0x000f;
    private static final int LICENCE_TAG_HOST = 0x0010;
    

    private MCS McsLayer=null;
    private String hostname=null;
    private String username=null;
    private boolean licenceIssued = false;
    private RC4 rc4_enc = null;
    private RC4 rc4_dec = null;
    private RC4 rc4_update = null;
    private BlockMessageDigest sha1 = null;
    private BlockMessageDigest md5 = null;
    private int keylength = 0;
    private int enc_count = 0;
    private int dec_count = 0;
    
    private byte[] sec_sign_key = null;

    private byte[] sec_decrypt_key = null;

    private byte[] sec_encrypt_key = null;

    private byte[] sec_decrypt_update_key = null;

    private byte[] sec_encrypt_update_key = null;
    private byte[] sec_crypted_random=null;

    private byte[] exponent = null;
    private byte[] modulus = null;
    private byte[] server_random = null;
    private byte[] client_random = new byte[this.SEC_RANDOM_SIZE];

    private byte[] licence_key = null;
    private byte[] licence_sign_key = null;
    private byte[] in_token = null, in_sig = null;

    private static final byte[] pad_54 = {
	54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
	54, 54, 54,
	54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
	54, 54, 54
    };
    
    private static final byte[] pad_92 = {
	92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
	92, 92, 92, 92, 92, 92, 92,
	92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
	92, 92, 92, 92, 92, 92, 92
    };
    
    public Secure() {
	McsLayer = new MCS();
	rc4_dec = new RC4();
	rc4_enc = new RC4();
	rc4_update = new RC4();
	sha1 = new SHA1();
	md5 = new MD5();
	sec_sign_key = new byte[8];
	sec_decrypt_key = new byte[16];
	sec_encrypt_key = new byte[16];
	sec_decrypt_update_key = new byte[8];
	sec_encrypt_update_key = new byte[8];
	sec_crypted_random = new byte[64];
	licence_key = new byte[16];
	licence_sign_key = new byte[16];
    }
    
    public void connect(String username, IPAddress host, int port) throws UnknownHostException, IOException, RdesktopException, SocketException, CryptoException {
	InetAddress localhost = InetAddress.getLocalHost();
	String name = localhost.getHostName();
	StringTokenizer tok = new StringTokenizer(name, ".");
	this.hostname = tok.nextToken();
	this.hostname.trim();
	this.username=username;
	Packet mcs_data = this.sendMcsData();
	McsLayer.connect(host, port, mcs_data);
	
	this.processMcsData(mcs_data);

	if (Constants.getEncryptionStatus()==true) {
	    this.establishKey();  
	}
    }
    
    /*
    public void connect(String username, String host, int port) throws UnknownHostException, IOException, RdesktopException, SocketException,  NoSuchAlgorithmException {
	this.connect(username, InetAddress.getByName(host), port);
	}*/
    
    public void connect(String username, IPAddress host) throws UnknownHostException, IOException, RdesktopException, SocketException, CryptoException {
	this.connect(username, host, Constants.PORT);
    }
    /*
    public void connect(String username, String host) throws UnknownHostException, IOException, RdesktopException, SocketException,  NoSuchAlgorithmException {
	this.connect(username, InetAddress.getByName(host), Constants.PORT);
	}*/
    
    public void disconnect() {
	McsLayer.disconnect();
	//in=null;
	//out=null;
    }  

    public Packet getMemory(int size) {
	return McsLayer.getMemory(size);
    }

    public Packet sendMcsData() {
	Packet buffer=this.getMemory(512);

	int hostlen = 2 * (this.hostname==null ? 0 : this.hostname.length());
	
	if (hostlen > 30) { hostlen=30;}

	buffer.setBigEndian16(5);	/* unknown */
	buffer.setBigEndian16(0x14);
	buffer.set8(0x7c);
	buffer.setBigEndian16(1);

	buffer.setBigEndian16(158 | 0x8000);	/* remaining length */

	buffer.setBigEndian16(8);	/* length? */
	buffer.setBigEndian16(16);
	buffer.set8(0);
	buffer.setLittleEndian16(0xc001);
	buffer.set8(0);

	buffer.setLittleEndian32(0x61637544);	/* "Duca" ?! */
	buffer.setBigEndian16(144 | 0x8000);	/* remaining length */

	/* Client information */
	buffer.setLittleEndian16(this.SEC_TAG_CLI_INFO);
	buffer.setLittleEndian16(136);	/* length */
	buffer.setLittleEndian16(1);
	buffer.setLittleEndian16(8);
	buffer.setLittleEndian16(Constants.width); /* FIXME */
	buffer.setLittleEndian16(Constants.height); /* FIXME */
	buffer.setLittleEndian16(0xca01);
	buffer.setLittleEndian16(0xaa03);
	buffer.setLittleEndian32(Constants.keylayout);
	buffer.setLittleEndian32(419);	/* client build? we are 419 compatible :-) */

	/* Unicode name of client, padded to 32 bytes */
	Rdp.sendUnicodeString(buffer, this.hostname, hostlen);
	buffer.incrementPosition(30 - hostlen);

	buffer.setLittleEndian32(4);
	buffer.setLittleEndian32(0);
	buffer.setLittleEndian32(12);
	buffer.incrementPosition(64);	/* reserved? 4 + 12 doublewords */

	buffer.setLittleEndian16(0xca01);
	buffer.setLittleEndian16(0);

	/* Client encryption settings */
	buffer.setLittleEndian16(this.SEC_TAG_CLI_CRYPT);
	buffer.setLittleEndian16(8);	/* length */
	buffer.setLittleEndian32(Constants.getEncryptionStatus() ? 1 : 0);	/* encryption enabled */
	buffer.markEnd();
	return buffer;
    }

    public void processMcsData(Packet mcs_data) throws RdesktopException, CryptoException {
	int tag=0, len=0, length=0, nexttag=0;
	
	mcs_data.incrementPosition(21);
	len=mcs_data.get8();

	if ((len&0x00000080)!=0) {
	    len=mcs_data.get8();
	}

	while (mcs_data.getPosition() < mcs_data.getEnd()) {
	    tag=mcs_data.getLittleEndian16();
	    length=mcs_data.getLittleEndian16();

	    if (length <=4) return;
	    
	    nexttag=mcs_data.getPosition() + length -4;
	    
	    switch(tag) {
	    case (Secure.SEC_TAG_SRV_INFO):
	    case (Secure.SEC_TAG_SRV_3): break;
	    case (Secure.SEC_TAG_SRV_CRYPT):
		this.processCryptInfo(mcs_data);
		break;

	    default: throw new RdesktopException("Not implemented! Tag:"+tag+"not recognized!");
	    }

	    mcs_data.setPosition(nexttag);
	}
    }

    public void establishKey()throws RdesktopException, IOException, CryptoException {
	int length = this.SEC_MODULUS_SIZE + this.SEC_PADDING_SIZE;
	int flags = this.SEC_CLIENT_RANDOM;
	Packet buffer = this.init(flags, 76);

	buffer.setLittleEndian32(length);

	buffer.copyFromByteArray(this.sec_crypted_random, 0, buffer.getPosition(), this.SEC_MODULUS_SIZE);
	buffer.incrementPosition(this.SEC_MODULUS_SIZE);
	buffer.incrementPosition(this.SEC_PADDING_SIZE);
	buffer.markEnd();
	this.send(buffer, flags);
	
    }

    public void processCryptInfo(Packet data) throws RdesktopException, CryptoException {
	int rc4_key_size=0;
	    
	rc4_key_size = this.parseCryptInfo(data);
	if (rc4_key_size == 0) {
	    return;
	}
	
	this.generateRandom();
	this.RSAEncrypt(this.SEC_RANDOM_SIZE);
	this.generateKeys(rc4_key_size);
	
    }

    public Packet init(int flags, int length) throws RdesktopException {
	int headerlength=0;
	Packet buffer;

	if (this.licenceIssued ==false) 
	    headerlength = ((flags & this.SEC_ENCRYPT)!=0) ? 12 : 4;
	else 
	     headerlength = ((flags & this.SEC_ENCRYPT)!=0) ? 12 : 0;
	buffer=McsLayer.init(length+headerlength);
	buffer.setHeader(Packet.SECURE_HEADER);
	buffer.incrementPosition(headerlength);
	buffer.setStart(buffer.getPosition());
	return buffer;
    }

    public void send(Packet sec_data, int flags) throws RdesktopException, IOException, CryptoException {
	int datalength=0;
	byte[] signature = null;
	byte[] data;
	byte[] buffer;

	sec_data.setPosition(sec_data.getHeader(Packet.SECURE_HEADER));
	
	if (this.licenceIssued == false || (flags & this.SEC_ENCRYPT) !=0) {
	    sec_data.setLittleEndian32(flags);
	}
	if ((flags & this.SEC_ENCRYPT) !=0) {
	    flags &= ~this.SEC_ENCRYPT;
	    datalength = sec_data.getEnd() - sec_data.getPosition() - 8;
	    data = new byte[datalength];
	    buffer = null;
	    sec_data.copyToByteArray(data, 0, sec_data.getPosition()+8, datalength); 
	    signature = this.sign(this.sec_sign_key, 8, data, datalength);
	   
	    buffer = this.encrypt(data, datalength);
	    
	    sec_data.copyFromByteArray(signature, 0, sec_data.getPosition(), 8);
	    sec_data.copyFromByteArray(buffer, 0, sec_data.getPosition()+8, datalength);
	   
	}
	McsLayer.send(sec_data);
    }

    public byte[] sign(byte[] session_key, int length, byte[] data, int datalength) throws CryptoException {
	byte[] shasig = new byte[20];
	byte[] md5sig = new byte[16];
	byte[] lenhdr = new byte[4];
	byte[] signature = new byte[length];

	this.setLittleEndian32(lenhdr, datalength);
	
	sha1.engineReset();
	sha1.engineUpdate(session_key, 0, length);
	sha1.engineUpdate(pad_54, 0, 40);
	sha1.engineUpdate(lenhdr, 0, 4);
	sha1.engineUpdate(data, 0, datalength);
	shasig = sha1.engineDigest();
	sha1.engineReset();

	md5.engineReset();
	md5.engineUpdate(session_key, 0, length);
	md5.engineUpdate(pad_92, 0, 48);
	md5.engineUpdate(shasig, 0, 20);
	md5sig = md5.engineDigest();
	md5.engineReset();

	System.arraycopy(md5sig, 0, signature, 0, length);
	return signature;
    }

    public byte[] encrypt(byte[] data, int length) throws CryptoException {
	byte[] buffer = null;
	if (this.enc_count==4096) {
	    this.update(this.sec_encrypt_key, this.sec_encrypt_update_key);
	    byte[] key = new byte[this.keylength];
	    System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
	    this.rc4_enc.engineInitEncrypt(key);
	    this.enc_count=0;
	}
	//this.rc4.engineInitEncrypt(this.rc4_encrypt_key);
	buffer = this.rc4_enc.crypt(data, 0, length);
	this.enc_count++;
	return buffer;
    }
    
    public byte[] encrypt(byte[] data) throws CryptoException {
	byte[] buffer = null;
	if (this.enc_count==4096) {
	    this.update(this.sec_encrypt_key, this.sec_encrypt_update_key);
	    byte[] key = new byte[this.keylength];
	    System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
	    this.rc4_enc.engineInitEncrypt(key);
	    this.enc_count=0;
	}
	//this.rc4.engineInitEncrypt(this.rc4_encrypt_key);
	buffer = this.rc4_enc.crypt(data);
	this.enc_count++;
	return buffer;
    }

    public byte[] decrypt(byte[] data, int length) throws CryptoException {
	byte[] buffer = null;
	if (this.dec_count==4096) {
	    this.update(this.sec_decrypt_key, this.sec_decrypt_update_key);
	    byte[] key = new byte[this.keylength];
	    System.arraycopy(this.sec_decrypt_key, 0, key, 0, this.keylength);
	    this.rc4_dec.engineInitDecrypt(key);
	    this.dec_count=0;
	}
	//this.rc4.engineInitDecrypt(this.rc4_decrypt_key);
	buffer = this.rc4_dec.crypt(data, 0, length);
	this.dec_count++;
	return buffer;
    }

    public byte[] decrypt(byte[] data) throws CryptoException {
	byte[] buffer = null;
	if (this.dec_count==4096) {
	    this.update(this.sec_decrypt_key, this.sec_decrypt_update_key);
	    byte[] key = new byte[this.keylength];
	    System.arraycopy(this.sec_decrypt_key, 0, key, 0, this.keylength);
	    this.rc4_dec.engineInitDecrypt(key);
	    this.dec_count=0;
	}
	//this.rc4.engineInitDecrypt(this.rc4_decrypt_key);
	buffer = this.rc4_dec.crypt(data);
	this.dec_count++;
	return buffer;
    }
    
    public int parseCryptInfo(Packet data) throws RdesktopException {
	int encryption_level=0, random_length=0, RSA_info_length=0;
	int tag=0, length=0;
	int next_tag=0, end=0; 
	int rc4_key_size=0;

	rc4_key_size = data.getLittleEndian32(); // 1 = 40-Bit 2 = 128 Bit
	encryption_level = data.getLittleEndian32(); // 1 = low, 2 = medium, 3 = high
	/*if (encryption_level==0) { // no encryption
	    return 0;
	    }*/
	random_length = data.getLittleEndian32();
	RSA_info_length = data.getLittleEndian32();

	if ( random_length != this.SEC_RANDOM_SIZE) {
	    throw new RdesktopException("Wrong Size of Random! Got" + random_length + "expected" + this.SEC_RANDOM_SIZE);
	}
	this.server_random = new byte[random_length];
	data.copyToByteArray(this.server_random, 0, data.getPosition(), random_length);
	data.incrementPosition(random_length);

	end = data.getPosition() + RSA_info_length;

	if (end > data.getEnd()) {
	    Debug.out.println("Reached end of crypt info prematurely ");
	    return 0;
	}

	data.incrementPosition(12); // unknown bytes

	while (data.getPosition() < data.getEnd()) {
	    tag=data.getLittleEndian16();
	    length=data.getLittleEndian16();

	    next_tag = data.getPosition() + length;

	    switch (tag) {

	    case (Secure.SEC_TAG_PUBKEY):
		
		if (!parsePublicKey(data)) {
		    return 0;
		}
		
		break;
	    case (Secure.SEC_TAG_KEYSIG):
		// Microsoft issued a key but we don't care
		break;

	    default:
		throw new RdesktopException("Unimplemented");
	    }
	    data.setPosition(next_tag);
	}

	if (data.getPosition() == data.getEnd()) {
	    return rc4_key_size;
	} else {
	    Debug.out.println("End not reached!");
	    return 0;
	}
    }

    public void generateRandom() {
	//SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
	//random.nextBytes(this.client_random);
    }
	
	
    public void RSAEncrypt(int length) throws RdesktopException {
	byte[] inr = new byte[length];
	int outlength = 0;
	BigInteger mod = null;
	BigInteger exp = null;
	BigInteger x   = null;

	this.reverse(this.exponent);
	this.reverse(this.modulus);
	System.arraycopy(this.client_random, 0, inr, 0, length);
	this.reverse(inr);

	if ((this.modulus[0] & 0x80) != 0) {
	    byte[] temp = new byte [this.modulus.length+1];
	    System.arraycopy(this.modulus, 0, temp, 1, this.modulus.length);
	    temp[0]=0;
	    mod = new BigInteger(temp);
	} else {
	    mod = new BigInteger(this.modulus);
	}
	if ((this.exponent[0] & 0x80) != 0) {
	    byte[] temp = new byte [this.exponent.length+1];
	    System.arraycopy(this.exponent, 0, temp, 1, this.exponent.length);
	    temp[0]=0;
	    exp = new BigInteger(temp);
	} else {
	    exp = new BigInteger(this.exponent);
	}
	if ((inr[0] & 0x80) != 0) {
	    byte[] temp = new byte [inr.length+1];
	    System.arraycopy(inr, 0, temp, 1, inr.length);
	    temp[0]=0;
	    x = new BigInteger(temp);
	} else {
	    x   = new BigInteger(inr);
	}
	
	BigInteger y = x.modPow(exp, mod);
	this.sec_crypted_random = y.toByteArray();

	if ((this.sec_crypted_random[0] & 0x80) != 0) {
	    throw new RdesktopException("Wrong Sign! Expected positive Integer!");
	}
	
	if (this.sec_crypted_random.length > this.SEC_MODULUS_SIZE) {
	    Debug.out.println("Zu gross"); /* FIXME */
	}
	this.reverse(this.sec_crypted_random);

	byte[] temp = new byte[this.SEC_MODULUS_SIZE];

	if (this.sec_crypted_random.length < this.SEC_MODULUS_SIZE) {
	    System.arraycopy(this.sec_crypted_random, 0, temp, 0, this.sec_crypted_random.length);
	    for (int i = this.sec_crypted_random.length; i < temp.length; i++) {
		temp[i] = 0;
	    }
	    this.sec_crypted_random = temp;
	    
	}

    }

    public void generateKeys(int rc4_key_size) throws CryptoException {
	byte[] session_key = new byte[48];
	byte[] temp_hash = new byte[48];
	byte[] input = new byte[48];
	
	System.arraycopy(this.client_random, 0, input, 0, 24);
	System.arraycopy(this.server_random, 0, input, 24, 24);

	temp_hash = this.hash48(input, this.client_random, this.server_random, 65);
	session_key = this.hash48(temp_hash, this.client_random, this.server_random, 88);

	System.arraycopy(session_key, 0, this.sec_sign_key, 0, 8);
	
	this.sec_decrypt_key = this.hash16(session_key, this.client_random, this.server_random, 16);
	this.sec_encrypt_key = this.hash16(session_key, this.client_random, this.server_random, 32);
	
	if (rc4_key_size == 1) {
	    Debug.out.println("40 Bit Encryption enabled!");
	    this.make40bit(this.sec_sign_key);
	    this.make40bit(this.sec_decrypt_key);
	    this.make40bit(this.sec_encrypt_key);
	    this.keylength = 8;
	} else {
	    Debug.out.println("128 Bit Encryption enabled!");
	    this.keylength = 16;
	}
	
	System.arraycopy(this.sec_decrypt_key, 0, this.sec_decrypt_update_key, 0, 8);
	System.arraycopy(this.sec_encrypt_key, 0, this.sec_encrypt_update_key, 0, 8);

	
	byte[] key = new byte[this.keylength];
	System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
	rc4_enc.engineInitEncrypt(key);
	System.arraycopy(this.sec_decrypt_key, 0, key, 0, this.keylength);
	rc4_dec.engineInitDecrypt(key);
    }

    
    public boolean parsePublicKey(Packet data) throws RdesktopException {
	int magic=0, modulus_length=0;

	magic = data.getLittleEndian32();

	if (magic != this.SEC_RSA_MAGIC) {
	    throw new RdesktopException("Wrong magic! Expected" + this.SEC_RSA_MAGIC + "got:" + magic);
	}

	modulus_length = data.getLittleEndian32();
	
	if (modulus_length != this.SEC_MODULUS_SIZE + this.SEC_PADDING_SIZE) {
	    throw new RdesktopException("Wrong modulus size! Expected" + this.SEC_MODULUS_SIZE + "+" + this.SEC_PADDING_SIZE + "got:" + modulus_length);
	}
	
	data.incrementPosition(8); //unknown modulus bits
	this.exponent = new byte[this.SEC_EXPONENT_SIZE];
	data.copyToByteArray(this.exponent, 0, data.getPosition(), this.SEC_EXPONENT_SIZE);	
	data.incrementPosition(this.SEC_EXPONENT_SIZE);
	this.modulus = new byte[this.SEC_MODULUS_SIZE];
	data.copyToByteArray(this.modulus, 0, data.getPosition(), this.SEC_MODULUS_SIZE);
	data.incrementPosition(this.SEC_MODULUS_SIZE);
	data.incrementPosition(this.SEC_PADDING_SIZE);

	if (data.getPosition() <= data.getEnd()) {
	    return true;
	} else {
	    return false;
	}
    }
    
    public void reverse(byte[] data) {
	int i=0, j=0;
	byte temp = 0;

	for(i=0, j=data.length-1; i < j; i++, j--) {
	    temp = data[i];
	    data[i] = data[j];
	    data[j] = temp;
	}
    }
       
     public void reverse(byte[] data, int length) {
	 int i=0, j=0;
	byte temp = 0;
	
	for(i=0, j=length-1; i < j; i++, j--) {
	    temp = data[i];
	    data[i] = data[j];
	    data[j] = temp;
	}
     }

    public byte[] hash48(byte[] in, byte[] salt1, byte[] salt2, int salt)throws CryptoException {
	byte[] shasig = new byte[20];
	byte[] pad = new byte[4];
	byte[] out = new byte[48];
	int i = 0;
	
	for(i=0; i< 3; i++) {
	    for(int j=0; j <=i; j++) {
		pad[j]=(byte)(salt+i);
	    }
	    sha1.engineUpdate(pad, 0, i + 1);
	    sha1.engineUpdate(in, 0, 48);
	    sha1.engineUpdate(salt1, 0, 32);
	    sha1.engineUpdate(salt2, 0, 32);
	    shasig = sha1.engineDigest();
	    sha1.engineReset();

	    md5.engineUpdate(in, 0, 48);
	    md5.engineUpdate(shasig, 0, 20);
	    System.arraycopy(md5.engineDigest(), 0, out, i*16, 16);
	}

	return out;
    }
	
    public byte[] hash16(byte[] in, byte[] salt1, byte[] salt2, int in_position) throws CryptoException {
	
	md5.engineUpdate(in, in_position, 16);
	md5.engineUpdate(salt1, 0, 32);
	md5.engineUpdate(salt2, 0, 32);
	return md5.engineDigest();
    }

    public void make40bit(byte[] key) {
	key[0] = (byte)0xd1;
	key[1] = (byte)0x26;
	key[2] = (byte)0x9e;
    }

    public byte[] update(byte[] key, byte[] update_key) throws CryptoException {
	byte[] shasig = new byte[20];
	byte[] update = new byte[this.keylength];;

	sha1.engineUpdate(update_key, 0, 8);
	sha1.engineUpdate(pad_54, 0, 40);
	sha1.engineUpdate(key,0 , 8);
	shasig = sha1.engineDigest();
	sha1.engineReset();

	md5.engineUpdate(update_key,0 ,8);
	md5.engineUpdate(pad_92, 0, 48);
	md5.engineUpdate(shasig, 0, 20);
	key = md5.engineDigest();
	md5.engineReset();

	System.arraycopy(key, 0, update, 0, this.keylength);
	rc4_update.engineInitDecrypt(update);
	rc4_update.crypt(key, 0, this.keylength);

	if( this.keylength == 8) {
	    this.make40bit(key);
	}
	
	return key;
    }

    public void setLittleEndian32(byte[] data, int value) {

	data[3] = (byte)((value >>> 24) & 0xff); 
	data[2] = (byte)((value >>> 16) & 0xff); 
	data[1] = (byte)((value >>> 8) & 0xff); 
	data[0] = (byte)(value & 0xff); 
    }

    public Packet receive() throws RdesktopException, IOException, CryptoException {
	int sec_flags=0;
	Packet buffer=null;
	while(true) {
	    
	    buffer=McsLayer.receive();
	    buffer.setHeader(Packet.SECURE_HEADER);
	    if ((Constants.getEncryptionStatus() == true) || (this.licenceIssued == false)) {
	
		sec_flags=buffer.getLittleEndian32();

		if ((sec_flags & this.SEC_LICENCE_NEG) != 0) {
		    this.process(buffer);
		    continue;
		}
		if ((sec_flags & this.SEC_ENCRYPT) != 0) {
		    buffer.incrementPosition(8); //signature
		    byte[] data = new byte[buffer.size() - buffer.getPosition()];
		    buffer.copyToByteArray(data, 0, buffer.getPosition(), data.length);
		    byte[] packet = this.decrypt(data);
		    
		    buffer.copyFromByteArray(packet, 0, buffer.getPosition(), packet.length);

		    buffer.setStart(buffer.getPosition());
		    return buffer;
		}
	    }
	    
	    buffer.setStart(buffer.getPosition());
	    return buffer;
	}
    }

    public void process(Packet data) throws RdesktopException, IOException, CryptoException {
	int tag = 0;
	tag = data.getLittleEndian16();
	data.incrementPosition(2);

	switch(tag) {
	    
	case (Secure.LICENCE_TAG_DEMAND):
	    this.processDemand(data);
	    break;
	    
	case (Secure.LICENCE_TAG_AUTHREQ):
	    this.processAuthreq(data);
	    break;
	    
	case (Secure.LICENCE_TAG_ISSUE):
	    this.processIssue(data);
	    break;
	    
	case (Secure.LICENCE_TAG_RESULT):
	    break;
	    
	default:
	    Debug.out.println("got licence tag: " + tag);
	}
	
	
    }

    public void processDemand(Packet data) throws UnsupportedEncodingException, RdesktopException, IOException, CryptoException {
	byte[] null_data = new byte[this.SEC_MODULUS_SIZE];
	byte[] server_random = new byte[this.SEC_RANDOM_SIZE];
	byte[] host = this.hostname.getBytes("US-ASCII");
	byte[] user = this.username.getBytes("US-ASCII");

	/*retrieve the server random */
	data.copyToByteArray(server_random, 0, data.getPosition(), server_random.length);
	data.incrementPosition(server_random.length);

	/* Null client keys are currently used */
	this.generateKeysLicence(null_data, server_random, null_data);
     
	this.sendRequest(null_data, null_data, user, host);
    }

    public void generateKeysLicence(byte[] client_key, byte[] server_key, byte[] client_rsa)throws CryptoException {
	byte[] session_key = new byte[48];
	byte[] temp_hash = new byte[48];
	
	temp_hash = this.hash48(client_rsa, client_key, server_key, 65);
	session_key = this.hash48(temp_hash, server_key, client_key, 65);

	System.arraycopy(session_key, 0, this.licence_sign_key, 0, 16);

	this.licence_key = this.hash16(session_key, client_key, server_key, 16);
    }

    public void sendRequest(byte[] client_random, byte[] rsa_data, byte[] username, byte[] hostname) throws RdesktopException, IOException, CryptoException {
	int sec_flags = this.SEC_LICENCE_NEG;
	int userlen = (username.length == 0 ? 0 : username.length+1);
	int hostlen = (hostname.length == 0 ? 0 : hostname.length+1);
	int length = 128 + userlen + hostlen;

	Packet buffer = this.init(sec_flags, length);

	buffer.setLittleEndian16(this.LICENCE_TAG_REQUEST);
	buffer.setLittleEndian16(length);
	
	buffer.setLittleEndian32(1);
	buffer.setLittleEndian32(0xff010000);
	
	buffer.copyFromByteArray(client_random, 0, buffer.getPosition(), this.SEC_RANDOM_SIZE);
	buffer.incrementPosition(this.SEC_RANDOM_SIZE);
	buffer.setLittleEndian16(0);

	buffer.setLittleEndian16(this.SEC_MODULUS_SIZE + this.SEC_PADDING_SIZE);
	buffer.copyFromByteArray(rsa_data, 0, buffer.getPosition(), this.SEC_MODULUS_SIZE);
	buffer.incrementPosition(this.SEC_MODULUS_SIZE);

	buffer.incrementPosition(this.SEC_PADDING_SIZE);

	buffer.setLittleEndian16(this.LICENCE_TAG_USER);
	buffer.setLittleEndian16(userlen);

	if (username.length !=0) {
	    buffer.copyFromByteArray(username, 0, buffer.getPosition(), userlen-1);
	} else {
	     buffer.copyFromByteArray(username, 0, buffer.getPosition(), userlen);
	}
	
	buffer.incrementPosition(userlen);

	buffer.setLittleEndian16(this.LICENCE_TAG_HOST);
	buffer.setLittleEndian16(hostlen);
	
	if (hostname.length !=0) {
	    buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen-1);
	} else {
	    buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen);
	}
	buffer.incrementPosition(hostlen);
	buffer.markEnd();
	this.send(buffer, sec_flags);
    }
    
    public void processIssue(Packet data)throws CryptoException {
	int length = 0;
	int check = 0;
	RC4 rc4_licence = new RC4();
	byte[] key = new byte[this.licence_key.length];
	System.arraycopy(this.licence_key, 0, key, 0, this.licence_key.length);
	
	data.incrementPosition(2); //unknown
	length = data.getLittleEndian16();
	
	if ( data.getPosition() + length > data.getEnd()) {
	    return;
	}
	
	rc4_licence.engineInitDecrypt(key);
	byte[] buffer = new byte[length];
	data.copyToByteArray(buffer, 0, data.getPosition(), length);
	rc4_licence.crypt(buffer, 0, length, buffer, 0);
	data.copyFromByteArray(buffer, 0, data.getPosition(), length);

	check = data.getLittleEndian16();
	if (check!=0) {
	    return;
	}
	this.licenceIssued = true;
	Debug.out.println("Server issued Licence!");
    }
	
    public void processAuthreq(Packet data) throws RdesktopException,  UnsupportedEncodingException, IOException, CryptoException{

	byte[] out_token = new byte[this.LICENCE_TOKEN_SIZE];
	byte[] decrypt_token = new byte[this.LICENCE_TOKEN_SIZE];
	byte[] hwid = new byte[this.LICENCE_HWID_SIZE];
	byte[] crypt_hwid = new byte[this.LICENCE_HWID_SIZE];
	byte[] sealed_buffer = new byte[this.LICENCE_TOKEN_SIZE + this.LICENCE_HWID_SIZE];
	byte[] out_sig = new byte[this.LICENCE_SIGNATURE_SIZE];
	RC4 rc4_licence = new RC4();
	byte[] crypt_key = null;

	/* parse incoming packet and save encrypted token */
	if (parseAuthreq(data)!=true) {
	    throw new RdesktopException("Authentication Request was corrupt!");
	}
	System.arraycopy(this.in_token, 0, out_token, 0, this.LICENCE_TOKEN_SIZE);
	
	/* decrypt token. It should read TEST in Unicode */
	crypt_key = new byte[this.licence_key.length];
	System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
	rc4_licence.engineInitDecrypt(crypt_key);
	rc4_licence.crypt(this.in_token, 0, this.LICENCE_TOKEN_SIZE, decrypt_token, 0);

	/*construct HWID */
	this.setLittleEndian32(hwid, 2);System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
	byte[] name = this.hostname.getBytes("US-ASCII");

	if (name.length > this.LICENCE_HWID_SIZE-4) {System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
	    System.arraycopy(name, 0, hwid, 4, this.LICENCE_HWID_SIZE-4);
	} else {
	    System.arraycopy(name, 0, hwid, 4, name.length);
	}
	
	/* generate signature for a buffer of token and HWId */
	System.arraycopy(decrypt_token, 0, sealed_buffer, 0, this.LICENCE_TOKEN_SIZE);
	System.arraycopy(hwid, 0, sealed_buffer, this.LICENCE_TOKEN_SIZE, this.LICENCE_HWID_SIZE);
	
	out_sig = this.sign(this.licence_sign_key, 16, sealed_buffer, sealed_buffer.length);
	
	/* deliberately break signature if licencing disabled */
	if (Constants.getLicenceStatus() == false) {
	    out_sig = new byte[this.LICENCE_SIGNATURE_SIZE]; // set to 0
	}

	/*now crypt the hwid */
	System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
	rc4_licence.engineInitEncrypt(crypt_key);
	rc4_licence.crypt(hwid, 0, this.LICENCE_HWID_SIZE, crypt_hwid, 0);

	this.sendAuthresp(out_token, crypt_hwid, out_sig);
    }

    public boolean parseAuthreq(Packet data)throws RdesktopException {

	int tokenlen=0;

	data.incrementPosition(6); //unknown
	
	tokenlen = data.getLittleEndian16();

	if (tokenlen != this.LICENCE_TOKEN_SIZE) {
	    throw new RdesktopException("Wrong Tokenlength!");
	}
	this.in_token = new byte[tokenlen];
	data.copyToByteArray(this.in_token, 0, data.getPosition(), tokenlen);
	data.incrementPosition(tokenlen);
	this.in_sig = new byte[this.LICENCE_SIGNATURE_SIZE];
	data.copyToByteArray(this.in_sig, 0, data.getPosition(), this.LICENCE_SIGNATURE_SIZE);
	data.incrementPosition(this.LICENCE_SIGNATURE_SIZE);
	
	if (data.getPosition() == data.getEnd()) {
	    return true;
	} else {
	    return false;
	}
    }
    
    public void sendAuthresp(byte[] token, byte[] crypt_hwid, byte[] signature) throws RdesktopException, IOException, CryptoException {
	int sec_flags = this.SEC_LICENCE_NEG;
	int length = 58;
	Packet data = null;

	data = this.init(sec_flags, length + 2);

	data.setLittleEndian16(this.LICENCE_TAG_AUTHRESP);
	data.setLittleEndian16(length);

	data.setLittleEndian16(1);
	data.setLittleEndian16(this.LICENCE_TOKEN_SIZE);
	data.copyFromByteArray(token, 0, data.getPosition(), this.LICENCE_TOKEN_SIZE);
	data.incrementPosition(this.LICENCE_TOKEN_SIZE);

	data.setLittleEndian16(1);
	data.setLittleEndian16(this.LICENCE_HWID_SIZE);
	data.copyFromByteArray(crypt_hwid, 0, data.getPosition(), this.LICENCE_HWID_SIZE);
	data.incrementPosition(this.LICENCE_HWID_SIZE);
	
	data.copyFromByteArray(signature, 0, data.getPosition(), this.LICENCE_SIGNATURE_SIZE);
	data.incrementPosition(this.LICENCE_SIGNATURE_SIZE);
	data.markEnd();
	this.send(data, sec_flags);
    }
    
    public int getUserID() {
	return McsLayer.getUserID();
    }
    
}
