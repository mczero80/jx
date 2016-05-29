package jx.console;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import jx.console.Console;
import jx.console.VirtualConsole;
import jx.devices.Screen;
import jx.devices.Keyboard;
import jx.devices.KeyListener;

import jx.keyboard.Scancodes;

import jx.zero.debug.*;
import jx.zero.*;

public class ConsoleImpl implements Console {
    private Screen screen;
    private Keyboard keyboard;

    DebugPrintStream out;
    MemoryManager memMgr;
    DeviceMemory video;
    VirtualConsole current;
    VirtualConsole cons[] = new VirtualConsoleImpl[10];

    public ConsoleImpl(Naming naming, Screen screen, Keyboard keyboard) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	memMgr = (MemoryManager) naming.lookup("MemoryManager");

	this.screen = screen;
	this.keyboard = keyboard;
	screen.clear();

	//current = createVirtualConsole();
	//current.activate();

    }

    public static void init(Naming naming) {
	final jx.screen.ScreenImpl screen = new jx.screen.ScreenImpl(naming);
	new ConsoleImpl(naming, screen, null);
    }

    public VirtualConsole createVirtualConsole() {
	VirtualConsole v = new VirtualConsoleImpl(memMgr, this, screen, keyboard);
	return v;
    }

    public void switchTo(VirtualConsole cons) {
	if (current != null) current.deactivate();
	current = cons;
	current.activate();
    }

    void moveCursorTo(int x, int y) {    
	screen.moveCursorTo(x,y);
    }
    /*
    public InputStream getInputStream() {
	return in;
    }
    public OutputStream getOutputStream() {
	return out;
    }
    public OutputStream getErrorStream() {
	return err;
    }

    public void write(int c) {
	dout.println("CONSOLE::write " + c);
	screen.putc((char)c);
    }
    */
}

