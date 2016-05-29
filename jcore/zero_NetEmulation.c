/********************************************************************************
 * DomainZero Network Emulation
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#include "config.h"
#ifdef NET_EMULATION
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/if_ether.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <netpacket/packet.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>

#include "all.h"

extern Class *class_B;

static int sock = -1;
static struct ifreq ifr;

//static unsigned char macaddr[] = {  NETEMULATION_ETHERADDR };

static u1_t macaddr[6];

jint netemulation_open(ObjectDesc * self, ObjectDesc * deviceName, ArrayDesc * etheraddr)
{
	int i, j;

	struct in_addr in;
	struct iphdr *ip;

	struct packet_mreq mr;
	struct sockaddr_ll sll;	/* packet(7) */

	char device[128];
	if (deviceName == 0)
		return 0;
	stringToChar(deviceName, device, sizeof(device));

	if (etheraddr->size != 6)
		sys_panic("etherAddr must be byte array of dimension 6");

	printf("Using ether address ");
	for (i = 0; i < 6; i++) {
		macaddr[i] = etheraddr->data[i];
		printf("%x", macaddr[i]);
		if (i < 5)
			printf(":");
	}
	printf("\n");

	printf(" ====%s \n", (char *) device);

	if ((sock = socket(PF_PACKET, SOCK_RAW, htons(ETH_P_ALL))) < 0) {
		perror("socket");
		sys_panic("Error in netemulation_open \n");
	}

	fcntl(sock, F_SETFL, O_NONBLOCK);


	ifr.ifr_ifindex = 0;
	strcpy(ifr.ifr_name, device);
	if (ioctl(sock, SIOGIFINDEX, &ifr) < 0)
		sys_panic("Error in ioctl \n");
	/*printf("ifr.ifr_ifindex %d \n",ifr.ifr_ifindex); */

	memset(&mr, 0, sizeof(mr));
	mr.mr_ifindex = ifr.ifr_ifindex;
	mr.mr_type = PACKET_MR_PROMISC;
	if (setsockopt(sock, SOL_PACKET, PACKET_ADD_MEMBERSHIP, (char *) &mr, sizeof(mr)) < 0) {
		sys_panic("Error in setsockopt \n");
	}

	ifr.ifr_ifindex = 0;
	if (ioctl(sock, SIOCGIFINDEX, &ifr) < 0)
		sys_panic("Error in ioctl SIOCGIFINDEX\n");

	/*printf("ifr.ifr_ifindex %d \n",ifr.ifr_ifindex); */
	memset(&sll, 0, sizeof(sll));
	sll.sll_family = AF_PACKET;
	sll.sll_ifindex = ifr.ifr_ifindex;
	sll.sll_protocol = htons(ETH_P_ALL);

	if (bind(sock, (struct sockaddr *) &sll, sizeof(sll)) < 0) {
		sys_panic("===bind error %d \n", errno);
	}

	return 0;
}


jint netemulation_receive(ObjectDesc * self, struct MemoryProxy_s * mem)
{
	int fromlen;
	struct sockaddr_ll from, sll;
	int size;
	char *buffer;
	int i;

	CHECK_NULL_PTR(mem);
	ASSERTMEMORY(mem);

	if (sock == -1)
		sys_panic("Socket not initialized");
	fromlen = sizeof(from);
	size = recvfrom(sock, memory_getMem(mem), memory_size(mem), 0, (struct sockaddr *) &from, &fromlen);
	if (size == -1 && errno == EWOULDBLOCK)
		return 0;
	if (size == -1) {
		perror("recvfrom");
		sys_panic("error in netemulation_receive");
	}
	// check our ether address or broadcast
	//for(i=0;i<12;i++) printf("%02x",((char*)mem->mem)[i]&0xff); printf("\n");
	for (i = 0; i < 6; i++)
		if ((((char *) memory_getMem(mem))[i] & 0xff) != macaddr[i]
		    && (((char *) memory_getMem(mem))[i] & 0xff) != 0xff)
			return 0;

	//  printf("RECEIVED PACKET\n");
	return size;
}

jint netemulation_send(ObjectDesc * self, struct MemoryProxy_s * mem, jint offset, jint size)
{
	int fromlen, ret, i;
	struct sockaddr_ll from;

	CHECK_NULL_PTR(mem);
	ASSERTMEMORY(mem);

	from.sll_family = AF_PACKET;
	from.sll_ifindex = ifr.ifr_ifindex;
	fromlen = sizeof(from);

	//printf("Offset=%d size=%d\n", offset, size);
	//for(i=0;i<64;i++) printf("%02x",((char*)mem->mem)[i]&0xff); printf("\n");
	ret = sendto(sock, ((u1_t *) (memory_getMem(mem))) + offset, size, 0, (struct sockaddr *) &from, fromlen);
	return ret;
}

jint netemulation_getMTU(ObjectDesc * self)
{
	return 1514;
}

jint netemulation_close(ObjectDesc * self)
{
}

ArrayDesc *netemulation_getMACAddress(ObjectDesc * self)
{
	ArrayDesc *arr;
	jint size = 6;
	arr = allocArrayInDomain(curdom(), class_B->classDesc, size);
	copyIntoByteArray(arr, macaddr, size);

	return arr;
}

MethodInfoDesc netemulationMethods[] = {
	{"open", "", netemulation_open}
	,
	{"receive", "", netemulation_receive}
	,
	{"send", "", netemulation_send}
	,
	{"getMTU", "", netemulation_getMTU}
	,
	{"getMACAddress", "", netemulation_getMACAddress}
	,
	{"close", "", netemulation_close}
	,
};

void init_net_emulation_portal()
{
	init_zero_dep("jx/zero/NetEmulation", "NetEmulation", netemulationMethods, sizeof(netemulationMethods),
		      "<jx/zero/NetEmulation>");
}

#endif				/* NET_EMULATION */
