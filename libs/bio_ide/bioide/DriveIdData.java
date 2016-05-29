package bioide;

import jx.zero.*;

/**
 * Access to drive information (command IDENTIFY)
 * (according to ATA4 spezifikation).
 * @author Michael Golm
 * @author Andreas Weissel
 */
class DriveIdData {

    private Memory  b_data;

    public DriveIdData() {
	b_data = Env.memoryManager.alloc(512);
    }

    // configuration
    public short  config()            { return b_data.get16(0>>1); }

    // physikalische Zahl der Zylinder
    public short  cyls()              { return b_data.get16(2>>1); }
    public void   cyls(short v)       { b_data.set16(2>>1, v); }

    // physikalische Zahl der Koepfe
    public short  heads()             { return b_data.get16(6>>1); }

    // Zahl der physikalischen Sektoren je Spur
    public short  sectors()           { return b_data.get16(12>>1); }

    // ASCII-Modellnummer, 0 = keine Angabe
    public String model()             { return DataFormat.readString(b_data, 54, 40); }
    public void   model(String v)     { DataFormat.writeString(b_data, 54, v, 40); }

    // Bit 0: DMA, 1: LBA, 2: IORDYsw, 3: IORDYsup
    public byte   capability()        { return b_data.get8(99); }

    // Bit 0: cur_xx gueltig, 1: eide_xx gueltig, 2: dma_ultra gueltig
    public short  field_valid()       { return b_data.get16(106>>1); }

    // logische Zahl der Zylinder
    public short  cur_cyls()          { return b_data.get16(108>>1); } 

    // logische Zahl der Koepfe
    public short  cur_heads()         { return b_data.get16(110>>1); }

    // logische Zahl der Sektoren je Spur
    public short  cur_sectors()       { return b_data.get16(112>>1); }

    // adressierbare Sektoren im LBA-Modus
    public int    lba_capacity()      { return b_data.get32(120>>2); }
    public void   lba_capacity(int v) { b_data.set32(120>>2, v); }

    // Mehrfach-DMA: unterstuetzte Modi, aktiver Modus
    public short  dma_mword()         { return b_data.get16(126>>1); }

    // PIO-Modi: Bit 0: mode3, 1: mode4
    public short  eide_pio_modes()    { return b_data.get16(128>>1); }

    // Ultra-DMA: unterstuetzte Modi, aktiver Modus
    public short  dma_ultra()         { return b_data.get16(172>>1); }


    Memory getData() { return b_data; }
}
