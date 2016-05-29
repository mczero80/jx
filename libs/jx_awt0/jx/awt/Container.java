package jx.awt;

import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import java.awt.datatransfer.Clipboard;

public class Container extends jx.awt.Component implements java.awt.peer.ContainerPeer {
    Insets insets;

    Container(jx.awt.Toolkit toolkit) {
	super(toolkit);
	insets = new Insets(0,0,0,0);
    }

    public Insets insets() {
	return insets;
    }

    public Insets getInsets(){throw new Error("not implemented");}

    public void beginValidate() {
	Dbg.msg("Container.beginValidate "+this);
    }

    public void endValidate() {
	Dbg.msg("Container.endValidate "+this);
    }

}

