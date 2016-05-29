package metaxa.os.devices.net;

class ManufacturingData {
    
    // Bits [4:0] - Tag von 1 bis 31
    private byte Day;
    // Bits [8:5] - Monat von 1 bis 12
    private byte Month;
    // Bits [15:9] - letzten zwei Digits des Jahres (0 bis 99)
    private byte Year;

    ManufacturingData() {
	Day = 0;
	Month = 0;
	Year = 0;
    }

    ManufacturingData(byte day, byte month, byte year) throws WrongDate {
	if (day < 1 || day > 31 || month < 1 || month > 12 || year < 0 || year > 99) {
	    System.out.println("ManufacturingData: falsche Parameter im Konstruktor!");
	    throw new WrongDate();
	}
	else {
	    Day = day;
	    Month = month;
	    Year = year;
	}
    }

    public void set_Day(byte day) throws WrongDate {
	if (day < 1 || day > 31)
	    throw new WrongDate();
	Day = day;
    }
    public void set_Month(byte month) throws WrongDate {
	if (month < 1 || month > 12)
	    throw new WrongDate();
	Month = month;
    } 
    public void set_Year(byte year) throws WrongDate {
	if (year < 0 || year > 99)
	    throw new WrongDate();
	Year = year;
    }

    public byte get_Day() {
	return Day;
    }
    public byte get_Month() {
	return Month;
    }
    public byte get_Year() {
	return Year;
    }

} 
