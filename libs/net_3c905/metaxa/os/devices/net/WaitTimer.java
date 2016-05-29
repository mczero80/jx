package metaxa.os.devices.net;

import metaxa.os.*;
import jx.timer.*;
import jx.zero.*;

class WaitTimer extends WaitCases implements TimerHandler {

    Ports ports;
    TimerManager timerManager;

    WaitTimer(Ports ports, TimerManager timerManager) {
	this.ports = ports;
	this.timerManager = timerManager;
    }

    public void timer(Object arg) {
	
	WaitTimerArg help = (WaitTimerArg)arg;
	NicInformation Adapter = help.get_NicInfo();	
	WaitTimerGlobal waitglobal = WaitTimerGlobal.instance();
	Befehl befehl = new Befehl(ports, timerManager);
	Mii mii = new Mii(ports, timerManager);
	int NextTick = 0;

	// helper variables
	DpdListEntry dpd;
	int portValue;
	short MediaStatus;
	boolean PhyResponding;
	int DownListPointer;
	int UpListPointer;
	int dmaControl;
	short PhyStatus = 0;

	//Debug.out.println("In WaitTimer");

	if(help.get_TimeOut() > timerManager.getCurrentTime()) {

	switch (Adapter.WaitCases)
	{
	case CHECK_DOWNLOAD_STATUS:
	    Debug.out.println("CHECK_DOWNLOAD_STATUS");
		dpd = help.get_TestDPD();
		// changed 28.4. : in DPDPhysicalAddress muesste schon das richtige stehen, aber zur Sicherheit
		Memory mem = dpd.DpdMemory();
		if(waitglobal.get_PortV() ==  (dpd.DpdMemory()).getStartAddress()) {
		    portValue = befehl.NicReadPortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER());
		    waitglobal.set_PortV(portValue);
		    NextTick = 10;
		}
		break;

	case CHECK_UPLOAD_STATUS:
	    //Debug.out.println("CHECK_UPLOAD_STATUS");
		if(waitglobal.get_PortV() == Adapter.HeadUPD.UPDPhysicalAddress()) {
		    Debug.out.println("Waitglobal: " + waitglobal.get_PortV() + " HeadUPD: " +  Adapter.HeadUPD.UPDPhysicalAddress());
		    portValue = befehl.NicReadPortLong(Adapter, Register.UP_LIST_POINTER_REGISTER());
		    waitglobal.set_PortV(portValue);
		    NextTick = 10;
		}
		break;

	case CHECK_DC_CONVERTER:
	    Debug.out.println("CHECK_DC_CONVERTER");
		if ( ((waitglobal.get_DCConverterEnabledState()) && 
		      !((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0) ) ||
		     ((!waitglobal.get_DCConverterEnabledState()) && 
		      ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0) ))
		{	
		    MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());
		    waitglobal.set_MediaStatus(MediaStatus);
		    NextTick = 10;
		}
		break;

	case CHECK_PHY_STATUS:
	    Debug.out.println("CHECK_PHY_STATUS");
	    if(!((waitglobal.get_PhyStatus() & Mii.MII_STATUS_AUTO_DONE) > 0) )
		{
		    PhyResponding = true;
		    try {
			PhyStatus = mii.ReadMIIPhy(Adapter, Mii.MII_PHY_STATUS);
		    }
		    catch (ReadMIIException e) {
			PhyResponding = false;
		    }
		    waitglobal.set_PhyStatus(PhyStatus);
		    waitglobal.set_PhyResponding(PhyResponding);
		    // um es zu verschnellern, machen wir es nur alle 200ms - Marcus 17.5.00 
		    NextTick = 500;//10;
		}
	    break;
	
	case CHECK_TRANSMIT_IN_PROGRESS:
	    Debug.out.println("CHECK_TRANSMIT_IN_PROGRESS");
		if((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0)
		{
		    MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());
		    waitglobal.set_MediaStatus(MediaStatus);
		    NextTick = 10;
		}
		break;

	case CHECK_DOWNLOAD_SELFDIRECTED:
	    Debug.out.println("CHECK_DOWNLOAD_SELFDIRECTED");
		dpd = help.get_TestDPD();
		if(waitglobal.get_DownListPointer() == dpd.DPDPhysicalAddress())
		{	
		    DownListPointer = befehl.NicReadPortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER());
		    waitglobal.set_DownListPointer(DownListPointer);
		    NextTick = 10;
		}
		break;

	case AUTONEG_TEST_PACKET:
	    Debug.out.println("AUTONEG_TEST_PACKET");
		if(waitglobal.get_UpListPointer() == Adapter.HeadUPD.UPDPhysicalAddress())
		    {	
			UpListPointer = befehl.NicReadPortLong(Adapter, Register.UP_LIST_POINTER_REGISTER());
			waitglobal.set_UpListPointer(UpListPointer);
			NextTick = 10;
		    }
		break;

	case CHECK_DMA_CONTROL:
	    Debug.out.println("CHECK_DMA_CONTROL");
		if((waitglobal.get_dmaControl() & Register.DMA_CONTROL_DOWN_IN_PROGRESS()) > 0)
		{	
		    dmaControl = befehl.NicReadPortLong(Adapter, Register.DMA_CONTROL_REGISTER());
		    waitglobal.set_dmaControl(dmaControl);
		    // um es zu verschnellern, machen wir es nur alle 200ms - Marcus 17.5.00 
		    NextTick = 10;
		}
		break;

	case CHECK_CARRIER_SENSE:
	    Debug.out.println("CHECK_CARRIER_SENSE");
		if((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_CARRIER_SENSE()) > 0)
		{
		    MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());
		    waitglobal.set_MediaStatus(MediaStatus);
		    NextTick = 10;
		} 
		break;		

	default:
		break;
	}
	if(NextTick != 0) {
	    timerManager.addMillisTimer(NextTick * 10, this, help);
	}

	} // end if TimeOutCount...
	Debug.out.println("Out WaitTimer");
    }
    
    public boolean equals(Object obj) {
	return (obj instanceof WaitTimer);
    }
}
