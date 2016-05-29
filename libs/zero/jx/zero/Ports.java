package jx.zero;

public interface Ports extends Portal {
    public void outb(int addr, byte b);
    public void outb_p(int addr, byte b);
    public byte inb(int addr);
    public byte inb_p(int addr);
    public void outl(int addr, int b);
    public void outl_p(int addr, int b);
    public int inl(int addr);
    public int inl_p(int addr);
    public void outw(int addr, short b);
    public void outw_p(int addr, short b);
    public short inw(int addr);
    public short inw_p(int addr);
}
