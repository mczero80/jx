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

class ConsoleInputStream extends InputStream {
    VirtualConsoleImpl console;;
    ConsoleInputStream(VirtualConsoleImpl c) { console = c; }
    public int read() throws IOException {
	return console.getc();
    }
}
