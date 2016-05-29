#ifndef _BDT_H_
#define _BDT_H_

/* support for binary data transmission (bdt)*/

#include "types.h"

int bdtStart();
int bdtStartChunk(const char *id, u4_t size, u4_t nelem,
		  char sendCompressed);
int bdtSend(const char *data, u4_t size, u4_t nelem);
int bdtStop();

#endif				/* _BDT_H_ */
