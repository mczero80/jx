package jx.zero;

public interface MappedLittleEndianObject extends MappedObject {
    /*
    Memory memory;

    public MappedLittleEndianObject(Memory m) {
	this.memory = m;
    }

    final protected int readInt(int pos) { 
	int part[] = new int[3];
	for (int i = 0; i < 3; i++) {
	    part[i] = (int)memory.get8(pos+i);
	    if (part[i] < 0) part[i] += 256;
	}

	return (int)(memory.get8(pos+3)<<24  | part[2]<<16 | part[1]<<8 | part[0]);
    }

    public final byte readByte(int pos) {
	return memory.get8(pos);
    }

    final protected short readShort(int pos) {
	short part = (short)memory.get8(pos);
	if (part < 0) part += 256;
	return (short)(memory.get8(pos+1)<<8 | part);
    }


    final protected void writeByte(int pos, byte value) {
	memory.set8(pos, value);
    }

    final protected void writeShort(int pos, short value) {
	memory.set8(pos+1, (byte)((value>>8) & 255));
	memory.set8(pos,   (byte)(value      & 255));
	// oder: memory.set16(...Offset umrechnen..., value);
    }

    final protected void writeInt(int pos, int value) {
	memory.set8(pos+3, (byte)((value>>24) & 255));
	memory.set8(pos+2, (byte)((value>>16) & 255));
	memory.set8(pos+1, (byte)((value>>8)  & 255));
	memory.set8(pos,   (byte)(value       & 255));
    }

    final protected void writeLong(int pos, long value) {
	writeInt(pos+4, (int)(value & 0xffffffff));
	writeInt(pos, (int)((value >> 32) & 0xffffffff));
    }

    final protected void writeBytes(int pos, byte[] value, int size) {
    }
    final protected byte[] readBytes(int pos, int size) {
	return null;
    }

    */
}
