package jx.rdp;

import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;

public class HexDump {

    public HexDump() {
    }

    public void encode(byte[] data, PrintStream out) {
	int count = 0;
	String index;
	String number;

	while(count < data.length) {
	    index = Integer.toHexString(count);
	    switch(index.length()) {
	    case(1):
		index = "0000000".concat(index);
		break;
	    case(2):
		index = "000000".concat(index);
		break;
	    case(3):
		index = "00000".concat(index);
		break;
	    case(4):
		index = "0000".concat(index);
		break;
	    case(5):
		index = "000".concat(index);
		break;
	    case(6):
		index = "00".concat(index);
		break;
	    case(7):
		index = "0".concat(index);
		break;
	    case(8):
		break;
	    default:
		return;
	    }
	    out.print(index + ": ");
	    for(int i = 0; i < 16; i++) {
		if(count >= data.length) {
		    break;
		}
		number = Integer.toHexString((data[count]&0x000000ff));
		switch(number.length()) {
		case(1):
		    number = "0".concat(number);
		    break;
		case(2):
		    break;
		default:
		    out.println("");
		    return;
		}   
		out.print(number + " ");
		count++;
	    }
	    out.println("");
	}
	
    }

    public void encode(byte[] data, DebugPrintStream out) {
	int count = 0;
	String index;
	String number;
	
	while(count < data.length) {
	    index = Integer.toHexString(count);
	    switch(index.length()) {
	    case(1):
		index = "0000000".concat(index);
		break;
	    case(2):
		index = "000000".concat(index);
		break;
	    case(3):
		index = "00000".concat(index);
		break;
	    case(4):
		index = "0000".concat(index);
		break;
	    case(5):
		index = "000".concat(index);
		break;
	    case(6):
		index = "00".concat(index);
		break;
	    case(7):
		index = "0".concat(index);
		break;
	    case(8):
		break;
	    default:
		return;
	    }
	    out.print(index + ": ");
	    for(int i = 0; i < 16; i++) {
		if(count >= data.length) {
		    break;
		}
		number = Integer.toHexString((data[count]&0x000000ff));
		switch(number.length()) {
		case(1):
		    number = "0".concat(number);
		    break;
		case(2):
		    break;
		default:
		    out.println("");
		    return;
		}   
		out.print(number + " ");
		count++;
	    }
	    out.println("");
	}
	
    }
}
