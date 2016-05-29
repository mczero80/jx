/* necessary defines:
   CHECK_BEFORE
   CHECK_AFTER
   CHECK_BEFORE_SIZE1: perform rangecheck; where is byte offset
   CHECK_BEFORE_SIZE2: perform rangecheck; where is 2-byte offset
   CHECK_BEFORE_SIZE3: perform rangecheck; where is 4-byte offset
   CHECK_BEFORE_RANGE1: perform rangecheck; where is 1-byte offset
   CHECK_BEFORE_RANGE2: perform rangecheck; where is 2-byte offset
   CHECK_BEFORE_RANGE3: perform rangecheck; where is 4-byte offset
   CHECK_BEFORE_EXTERN(stmt): perform rangecheck using boolean expression stmt
*/

/*******************************************
 *            METHODS OF THE MEMORY PORTAL
 *******************************************/

void memory_set8(MemoryProxy * self, jint where, jbyte what)
{
	CHECK_BEFORE_SIZE1;
	((jbyte *) self->mem)[where] = what;
	CHECK_AFTER;
}

jbyte memory_get8(MemoryProxy * self, jint where)
{
	jbyte ret;
	CHECK_BEFORE_SIZE1;
	ret = ((jbyte *) self->mem)[where];
	CHECK_AFTER;
	return ret;
}

void memory_set16(MemoryProxy * self, jint where, jshort what)
{
	CHECK_BEFORE_SIZE2;
	((jshort *) self->mem)[where] = what;
	CHECK_AFTER;
}

jshort memory_get16(MemoryProxy * self, jint where)
{
	jshort ret;
	CHECK_BEFORE_SIZE2;
	ret = ((jshort *) self->mem)[where];
	CHECK_AFTER;
	return ret;
}

void memory_set32(MemoryProxy * self, jint where, jint what)
{
	CHECK_BEFORE_SIZE3;
	((jint *) self->mem)[where] = what;
	CHECK_AFTER;
}

jint memory_get32(MemoryProxy * self, jint where)
{
	jint ret;
	CHECK_BEFORE_SIZE3;
	ret = ((jint *) self->mem)[where];
	CHECK_AFTER;
	return ret;
}

extern void jxwmemset(void *, jshort, jint);

void memory_fill16(MemoryProxy * self, jshort what, jint where, jint length)
{
	CHECK_BEFORE_RANGE2;
	jxwmemset((jshort *) self->mem + where, what, length << 1);
	CHECK_AFTER;
}

void memory_fill32(MemoryProxy * self, jint what, jint where, jint length)
{
	CHECK_BEFORE_RANGE3;
	jxwmemset((jint *) self->mem + where, what, length << 2);
	CHECK_AFTER;
}

void memory_clear(MemoryProxy * self)
{
	CHECK_BEFORE;
	jxwmemset((jint *) self->mem, 0, self->size >> 2);
	CHECK_AFTER;
}

void memory_move(MemoryProxy * self, jint __dst, jint __src, jint count)
{
	jint size = self->size;
	jbyte *dst = (jbyte *) self->mem + __dst;
	jbyte *src = (jbyte *) self->mem + __src;
	int d0, d1, d2, d3;

	CHECK_BEFORE;

	if ((__dst + count) > size || (__src + count) > size) {
		exceptionHandler(THROW_MemoryIndexOutOfBounds);
	}


	if (dst < src) {
		__asm__ __volatile__("cld\n\t" "shrl $1,%%ecx\n\t" "jnc 1f\n\t" "movsb\n" "1:\tshrl $1,%%ecx\n\t" "jnc 2f\n\t"
				     "movsw\n" "2:\trep\n\t" "movsl":"=&c"(d0), "=&D"(d1), "=&S"(d2)
				     :"0"(count), "1"((long) dst), "2"((long) src)
				     :"memory");
	} else {
		__asm__ __volatile__("std\n\t" "shrl $1,%%ecx\n\t" "jnc 1f\n\t" "movb 3(%%esi),%%al\n\t" "movb %%al,3(%%edi)\n\t"
				     "decl %%esi\n\t" "decl %%edi\n" "1:\tshrl $1,%%ecx\n\t" "jnc 2f\n\t" "movw 2(%%esi),%%ax\n\t"
				     "movw %%ax,2(%%edi)\n\t" "decl %%esi\n\t" "decl %%edi\n\t" "decl %%esi\n\t" "decl %%edi\n"
				     "2:\trep\n\t" "movsl\n\t" "cld":"=&c"(d0), "=&D"(d1), "=&S"(d2), "=&a"(d3)
				     :"0"(count), "1"(count - 4 + (long) dst), "2"(count - 4 + (long) src)
				     :"memory");
	}
	CHECK_AFTER;
}



void memory_copy(MemoryProxy * self, jint from, jint to, jint len)
{
	jint size, i;
	char *mem;
	CHECK_BEFORE_EXTERN(to + len > self->size || from + len > self->size);
	size = self->size;
	mem = (jbyte *) self->mem;
	for (i = 0; i < len; i++) {
		mem[to + i] = mem[from + i];
	}
	CHECK_AFTER;
}

void memory_copyToByteArray(MemoryProxy * self, ArrayDesc * bytes, jint array_offset, jint mem_offset, jint len)
{
	jint dstSize, srcSize;
	char *src;
	jint *dst;		/* element of byte array is 32 bits ! */
	jint i;

	CHECK_BEFORE_EXTERN((bytes == NULL)
			    || (mem_offset + len > self->size)
			    || (array_offset + len > bytes->size));


	srcSize = self->size;
	dstSize = bytes->size;

	src = self->mem;
	dst = bytes->data;
	src += mem_offset;
	dst += array_offset;
	for (i = 0; i < len; i++) {
		*dst++ = *src++;
	}
	CHECK_AFTER;
}

void memory_copyFromByteArray(MemoryProxy * self, ArrayDesc * bytes, jint array_offset, jint mem_offset, jint len)
{
	jint dstSize, srcSize;
	char *dst;
	jint *src;		/* element of byte array is 32 bits ! */
	jint i;

	CHECK_BEFORE_EXTERN((bytes == NULL)
			    || (mem_offset + len > self->size)
			    || (array_offset + len > bytes->size));

	dstSize = self->size;
	srcSize = bytes->size;

	dst = self->mem;
	src = bytes->data;
	dst += mem_offset;
	src += array_offset;
	for (i = 0; i < len; i++) {
		*dst++ = *src++;
	}
	CHECK_AFTER;
}

void memory_copyToMemory(MemoryProxy * self, MemoryProxy * dst, jint srcOffset, jint dstOffset, jint len)
{
	jbyte *mem, *smem;
	jint dstSize, srcSize;
	jint i;

	CHECK_BEFORE_EXTERN((dst == NULL) || (dstOffset + len > dst->size)
			    || (srcOffset + len > self->size));

	dstSize = dst->size;
	srcSize = self->size;
	mem = (jbyte *) dst->mem + dstOffset;
	smem = (jbyte *) self->mem + srcOffset;
	if ((((u4_t) smem | (u4_t) mem | (u4_t) len) & 0x3) == 0) {
		jxwordcpy(smem, mem, len >> 2);
	} else {
		jxbytecpy(smem, mem, len);
	}
	CHECK_AFTER;
}

void memory_copyFromMemory(MemoryProxy * self, MemoryProxy * src, jint srcOffset, jint dstOffset, jint len)
{
	jbyte *mem, *smem;
	jint i;
	jint dstSize, srcSize;

	CHECK_BEFORE_EXTERN((src == NULL) || (dstOffset + len > self->size)
			    || (srcOffset + len > src->size));

	smem = (jbyte *) src->mem + srcOffset;
	mem = (jbyte *) self->mem + dstOffset;
	dstSize = self->size;
	srcSize = src->size;
	if ((((u4_t) smem | (u4_t) mem | (u4_t) len) & 0x3) == 0) {
		jxwordcpy(smem, mem, len >> 2);
	} else {
		jxbytecpy(smem, mem, len);
	}
	CHECK_AFTER;
}

jint memory_getLittleEndian32(MemoryProxy * self, jint offset)
{
	jint ret;
	CHECK_BEFORE_EXTERN(offset + 4 > self->size || offset < 0);
	ret = *(jint *) (((jbyte *) self->mem) + offset);
	CHECK_AFTER;
	return ret;
}

void memory_setLittleEndian32(MemoryProxy * self, jint offset, jint value)
{
	CHECK_BEFORE_EXTERN(offset + 4 > self->size || offset < 0);
	*(jint *) (((jbyte *) self->mem) + offset) = value;
	CHECK_AFTER;
}

jshort memory_getLittleEndian16(MemoryProxy * self, jint offset)
{
	jshort ret;
	CHECK_BEFORE_EXTERN(offset + 2 > self->size || offset < 0);
	ret = *(jshort *) (((jbyte *) self->mem) + offset);
	CHECK_AFTER;
	return ret;
}

void memory_setLittleEndian16(MemoryProxy * self, jint offset, jshort value)
{
	CHECK_BEFORE_EXTERN(offset + 2 > self->size || offset < 0);
	*(jshort *) (((jbyte *) self->mem) + offset) = value;
	CHECK_AFTER;
}

jint memory_getBigEndian32(MemoryProxy * self, jint offset)
{
	jint data;

	CHECK_BEFORE_EXTERN((offset + 4 > self->size || offset < 0));

	data = *(jint *) (((jbyte *) self->mem) + offset);
	asm volatile ("bswap %%eax":"=r" (data));

	CHECK_AFTER;
	return data;
}

void memory_setBigEndian32(MemoryProxy * self, jint offset, jint value)
{
	CHECK_BEFORE_EXTERN(offset + 4 > self->size || offset < 0);
	asm volatile ("bswap %%eax":"=r" (value));
	*(jint *) (((jbyte *) self->mem) + offset) = value;
	CHECK_AFTER;
}

jshort memory_getBigEndian16(MemoryProxy * self, jint offset)
{
	jshort ret;
	jint value;

	CHECK_BEFORE_EXTERN(offset + 2 > self->size || offset < 0);
	value = *(jshort *) (((jbyte *) self->mem) + offset);
	ret = ((value & 0x00ff) << 8) | ((value & 0xff00) >> 8);
	CHECK_AFTER;
	return ret;
}

void memory_setBigEndian16(MemoryProxy * self, jint offset, jshort value)
{
	CHECK_BEFORE_EXTERN(offset + 2 > self->size || offset < 0);
	*(jshort *) (((jbyte *) self->mem) + offset) = ((value & 0x00ff) << 8) | ((value & 0xff00) >> 8);
	CHECK_AFTER;
}

/*******************************************
 *            MISC
 *******************************************/

jint memory_size(MemoryProxy * self)
{
	jint ret;
	CHECK_BEFORE;
	ret = self->size;
	CHECK_AFTER;
	return ret;
}

jint memory_getStartAddress(MemoryProxy * self)
{
	jint ret;
	CHECK_BEFORE;
	ret = (jint) self->mem;
	CHECK_AFTER;
	return ret;
}

u4_t memory_sizeof_proxy()
{
	return sizeof(struct MemoryProxy_s);
}

ClassDesc *mem_getDeviceMemoryClass()
{
	return deviceMemoryClass;
}

u4_t memory_getMem(MemoryProxy * self)
{
	u4_t ret;
	CHECK_BEFORE;
	ret = self->mem;
	CHECK_AFTER;
	return ret;
}
