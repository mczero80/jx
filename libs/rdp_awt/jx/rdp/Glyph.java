package jx.rdp;

import java.awt.*;
import java.awt.image.*;

public class Glyph {

    private int font = 0;
    private int character = 0;
    private int offset = 0;
    private int baseline = 0;
    private int width = 0;
    private int height = 0;
    private byte[] fontdata = null;

    public Glyph(int font, int character, int offset, int baseline, int width, int height, byte[] fontdata) {
	this.font = font;
	this.character = character;
	this.offset = offset;
	this.baseline = baseline;
	this.width = width;
	this.height = height;
	this.fontdata = fontdata;
    }
    
    public int getFont() {
	return this.font;
    }
    
    public int getBaseLine() {
	return this.baseline;
    }

    public int getCharacter() {
	return this.character;
    }

    public int getOffset() {
	return this.offset;
    }

    public int getWidth() {
	return this.width;
    }

    public int getHeight() {
	return this.height;
    }

    public byte[] getFontData() {
	return this.fontdata;
    }
}
