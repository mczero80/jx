#ifndef _CRC32_H_
#define _CRC32_H_

#include "types.h"


typedef u4_t crc32_t;

crc32_t crc32start();
crc32_t crc32end(crc32_t crc32);
crc32_t crc32(crc32_t crc, const u1_t * buf, u4_t len);
crc32_t crc32single(crc32_t crc, char ch);

#endif				/* _CRC32_H_ */
