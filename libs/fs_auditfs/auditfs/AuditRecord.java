package auditfs;

import jx.zero.*;

/**
   Format of a record: 8bytes time + 3 varsize strings 
   string length is first 32 bit word of string
*/

     
class AuditRecord {
    CycleTime time;
    String systemName;
    String domainName;
    String message;


    public AuditRecord() {}
    public AuditRecord(Clock clock, String systemName, String domainName, String message) {
	this.systemName = systemName;
	this.domainName = domainName;
	this.message = message;
	time = new CycleTime();
	if (clock==null) throw new Error();
	clock.getCycles(time);
    }

    public int getSize() {
	return 8 
	    + systemName.length()+4
	    + domainName.length()+4
	    + message.length()+4;
    }

    public void addToBuffer(Memory buf, int pos) {
	buf.setLittleEndian32(pos, time.low);
	buf.setLittleEndian32(pos+4, time.high);
	pos += 8;
	pos = addString(buf, pos, systemName);
	pos = addString(buf, pos, domainName);
	pos = addString(buf, pos, message);
    }

    public int parseFromBuffer(Memory buf, int pos) {
	time = new CycleTime();
	time.low = buf.getLittleEndian32(pos);
	time.high = buf.getLittleEndian32(pos+4);
	pos += 8;
	systemName = parseString(buf, pos); pos += 4 + systemName.length();
	domainName = parseString(buf, pos); pos += 4 + domainName.length();
	message = parseString(buf, pos); pos += 4 + message.length();
	return pos;
    }

    private int addString(Memory buf, int pos, String str) {
	buf.setLittleEndian32(pos, str.length());
	for(int i=0; i<str.length(); i++) {
	    buf.set8(pos+i+4, (byte)str.charAt(i));
	}
	return pos+str.length()+4;
    }

    private String parseString(Memory buf, int pos) {
	int len = buf.getLittleEndian32(pos);
	byte b[] = new byte[len];
	for(int i=0; i<len; i++) {
	    b[i] = buf.get8(pos+i+4);
	}
	return new String(b);
    }
}
