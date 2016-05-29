package metaxa.os.devices.net;

import metaxa.os.*;


/* Here are the classes for DpdList und UpdList - These are the lists which handle the descriptors for downloads to 
 * the NIC (this means, that the NIC should send the data downloaded onto) and the descriptors for uploads, which 
 * describe where to put the received data 
 * the concept for this lists is following:
 * for the DPDs and UPDs we use Memory_Objects, which themselves contain all the information, the NIC needs to do his
 * work, e.g. the DnNextPtr, the FrameStartHeader and the FragmentAddresses with their respective counts
 * the FragmentAddresses themselves point to the address of the memory which used to store the data to be sent or 
 * which has been received 
 * additonal these DpdListEntries and UpdListEntries are used to manage those memory objects and to hold information
 * about the corresponding memory objects 
 */

class ListEntry {
 
    final static int MAXIMUM_SCATTER_GATHER_LIST = 0x10;
    
    public int get_MAXGatherList() {
	return MAXIMUM_SCATTER_GATHER_LIST;
    }
}
