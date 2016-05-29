package jx.console;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import jx.console.Console;
import jx.console.VirtualConsole;
import jx.devices.Screen;
import jx.devices.Keyboard;

import jx.zero.debug.*;
import jx.zero.*;

public class VirtualConsoleImplColor extends VirtualConsoleImpl {
/*
0:  schwarz(grau)
1:  blau
2:  gruen
3:  cyan
4:  rot
5:  mangenta
6:  braun(gelb)
7:  grau(weiss)
8:  hell+...
*/

    public static final String BLACK = "\234";
    static final int BLACKv = ((int)(byte)'\234')&0xffff;
    public static final String BLUE  = "\235";
    public static final String GREEN = "\236";
    public static final String CYAN  = "\237";
    public static final String RED   = "\240";
    public static final String MAG   = "\241";
    public static final String YELL  = "\242";
    public static final String GREY  = "\243";
    static final int GREYv = ((int)(byte)'\243')&0xffff;
    
//     public static final String LIGHT = "\244";
//     public static final String DARK  = "\245";
 
//     public static final String BLINK = "\246";
//     public static final String NORM  = "\247";
    

    byte color = (byte)0x0f;
    byte light = 0;
    byte blink = 0;
    final Naming naming = (Naming) InitialNaming.getInitialNaming();
   final SMPCPUManager SMPcpuManager = (SMPCPUManager) naming.lookup("SMPCPUManager");
 
    public VirtualConsoleImplColor(MemoryManager memMgr, ConsoleImpl cons, Screen screen, Keyboard keyboard) {
	super(memMgr, cons, screen, keyboard);
    }
    
    public void putc(int c) {
	if (SMPcpuManager.getNumCPUs()>1){	
	    if (SMPcpuManager.getMyCPU() != SMPcpuManager.getCPU(0)) {
		light = 8;
//		color = (byte)0x01;
	    }   else {
		light = 0;
//		color = (byte)0x02;
	    }
	}
	
	if(c >= BLACKv && c <= GREYv) {
	    color = (byte)(c-BLACKv);
	}
// 	else if(c == LIGHT)
// 	    light = 8;
// 	else if(c == DARK)
// 	    light = 0;
// 	else if(c == BLINK)
// 	    blink = 16;
// 	else if(c == NORM)
// 	    blink = 0;
	else
	    switch (c) {
	    case '\n':
		if (y==24) {
		    buffer.copy(80*2, 0, 80*24*2);
		    buffer.fill16((short)0x0f00, 80*24, 80);
		} else {
		    y++;
		}
		/* fall through... */
	    case '\r':
		x = 0;
		break;
	    case '\b':
		if (x > 0) x--;
	    break;
	    case '\t':
		do {
		    putc(' ');
		} while ((x & 7) != 0);
		break;
	    default:
		/* Wrap if we reach the end of a line.  */
		if (x >= 80) {
		    putc('\n');
		}
		
		/* Stuff the character into the video buffer. */
		buffer.set8((80*y + x) * 2,  (byte)c);
		buffer.set8((80*y + x) * 2 + 1, (byte)(light+color+blink));
		x++;
		break;
	    }
	if (active) cons.moveCursorTo(x,y);
    }
    
}

