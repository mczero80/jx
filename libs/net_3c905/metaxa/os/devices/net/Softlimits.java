package metaxa.os.devices.net;

class Softlimits {

final static int NIC_STATUS_SUCCESS = 0x1;
final static int NIC_STATUS_FAILURE = 0x2;

//
// Software limits defined here
//

final static int NIC_DEFAULT_SEND_COUNT = 0x40;
final static int NIC_DEFAULT_RECEIVE_COUNT = 0x40;
final static int NIC_MINIMUM_SEND_COUNT	= 0x2;
final static int NIC_MAXIMUM_SEND_COUNT	= 0x80;
final static int NIC_MINIMUM_RECEIVE_COUNT = 0x2;
final static int NIC_MAXIMUM_RECEIVE_COUNT = 0x80;

final static int LINK_SPEED_100 = 100000000;
final static int LINK_SPEED_10 = 10000000;

final static int ETH_ADDR_SIZE = 6;
final static int ETH_MULTICAST_BIT = 1;

}
