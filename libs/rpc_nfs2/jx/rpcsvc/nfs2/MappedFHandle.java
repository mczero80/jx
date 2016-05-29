package jx.rpcsvc.nfs2;

//import jx.zero.*;

/**
 * Mapping of file handle from byte array to object.
 * @author Michael Golm
 */
public class MappedFHandle {
    private byte[] data;

    public int identifier;
    public int deviceIdentifier;
    public int generation;

//    public static int counter;
    
    public MappedFHandle() {
//	counter++;
//	Debug.out.println("MappedFHandle-Counter: " + counter);
    }

    public MappedFHandle(byte[] data) {
	this();
	this.data = data;
	identifier = readInt(0);
	deviceIdentifier = readInt(4);
	generation = readInt(8);
    }

    public MappedFHandle(int deviceIdentifier, int identifier, int generation) {
	this();
	this.deviceIdentifier = deviceIdentifier;
	this.identifier = identifier;
	this.generation = generation;
	data = new byte[32];
	writeInt(0, identifier);
	writeInt(4, deviceIdentifier);
	writeInt(8, generation);
    }
    public MappedFHandle(FHandle fh) {
	this(fh.data);
    }

    public void renew(int deviceIdentifier, int identifier, int generation) {
	this.deviceIdentifier = deviceIdentifier;
	this.identifier = identifier;
	this.generation = generation;
	data = new byte[32];
	writeInt(0, identifier);
	writeInt(4, deviceIdentifier);
	writeInt(8, generation);
    }	
    public void renew(byte[] data) {
	this.data = data;
	identifier = readInt(0);
	deviceIdentifier = readInt(4);
	generation = readInt(8);
    }

    public byte[] getData() { return data; }
    public FHandle getFHandle() { return new FHandle(data); }

    protected int readInt(int o) { 
	int d = 0;
	d += unsignedByteToInt(readByte(o)) << 24;
	d += unsignedByteToInt(readByte(o+1)) << 16;
	d += unsignedByteToInt(readByte(o+2)) << 8;
	d += unsignedByteToInt(readByte(o+3)) << 0;
	return d;
    }

  protected byte readByte(int o) { 
      return data[o];
  }

  protected void writeInt(int o, int d) { 
    data[o+0] = (byte)( (d>>24) & 0xff);
    data[o+1] = (byte)((d>>16)  & 0xff);
    data[o+2] = (byte)((d>>8) & 0xff);
    data[o+3] = (byte)(d & 0xff);
  }

    public int unsignedByteToInt(byte b) {
	return (int) b & 0xFF;
    }

}
