package metaxa.os.devices.net;

/* Ethernet-Adresse mit Zugriffsfunktionen */

class EthernetAdress {
    final static int ETH_ADDR_SIZE = 6; 

    private byte Addr[];

    EthernetAdress(byte[] array) throws WrongEthernetAdressFormat {
	if (array.length != ETH_ADDR_SIZE) throw new WrongEthernetAdressFormat();
	Addr = new byte[ETH_ADDR_SIZE];
	for (int i=0; i<6; i++) 
	    Addr[i] = array[i];
    }

    /* 
     * gibt die Ethernet-Adresse als Byte-Array zurück, allerdings als Kopie des internen Arrays,
     * das nicht von außen geändert werden können soll
     */

    public byte[] get_Addr() {
	byte feld[] = (byte[])Addr.clone();
	return feld;
    }

    /* 
     * erzeugt einen String aus der Ethernet.Adresse im gewohnten Format als 6 Werte zwischen 0 und 255
     * dazu muss jedoch getrickst werden, da Java Byte-Werte vorzeichenbehaftet interpretiert und alle 
     * Werte ab 1xxxxxxx (binär) als negativ angesehen werden
     * deswegen verwende ich die eine Funktion, die die Byte-Werte unsigned in Dezimalzahlen umrechnet
     * es ist somit nicht sonderlich schnell, aber diese Funktion ist sowieso nur für Kontrollzwecke gedacht
     */

    public String print_Addr() {
	BitPosition bs = new BitPosition();
	return "" + bs.byte_to_unsigned((byte)Addr[5]) + "." + bs.byte_to_unsigned((byte)Addr[4]) + "." 
	    + bs.byte_to_unsigned((byte)Addr[3]) + "." + bs.byte_to_unsigned((byte)Addr[2]) + "." 
	    + bs.byte_to_unsigned((byte)Addr[1]) + "." + bs.byte_to_unsigned((byte)Addr[0]);
    }
}
	
