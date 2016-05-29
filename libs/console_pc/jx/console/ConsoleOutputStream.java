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

class ConsoleOutputStream extends OutputStream {
    VirtualConsoleImpl console;;
    ConsoleOutputStream(VirtualConsoleImpl c) { console = c; }
    public void write(int b) throws IOException {
	console.putc((char)b);
    }
}
