#include "all.h"

#define debugz(x)

/*
 * Ports DEP
 */
/* slow down by writing to the nonexistent port 0x80 */
#define SLOW_DOWN __asm__ __volatile__("outb %%al, $0x80": :)

void outb_p(u2_t port, u1_t v)
{
	outb(0x80, 0);
	outb(port, v);
}


static inline unsigned int pinb(unsigned short int port)
{
	unsigned char ret;
	asm volatile ("inb %1,%0":"=a" (ret):"d"(port));
	return ret;
}

static inline void poutb(unsigned short int port, unsigned char val)
{
	asm volatile ("outb %0,%1"::"a" (val), "d"(port));
}


static inline unsigned long inl(unsigned short int port)
{
	unsigned long ret;
	asm volatile ("inl %1,%0":"=a" (ret):"d"(port));
	return ret;
}

static inline void outl(unsigned short port, unsigned long val)
{
	asm volatile ("outl %0,%1"::"a" (val), "d"(port));
}

static inline unsigned short inw(unsigned short int port)
{
	unsigned short ret;
	asm volatile ("inw %1,%0":"=a" (ret):"d"(port));
	return ret;
}

static inline void outw(unsigned short port, unsigned short val)
{
	asm volatile ("outw %0,%1"::"a" (val), "d"(port));
}

void ports_outb(ObjectDesc * self, jint port, jbyte value)
{
#ifdef KERNEL
	poutb(port, value);
#else
	debugz(("PORT outb(%lx, %d)\n", port, value));
#endif
}

jbyte ports_inb(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	jbyte ret = pinb(port);
	//    printf("inb %p -> %d\n", port, ret);
	return ret;
#else
	debugz(("PORT in(%lx\n", port));
	return 0xff;
#endif
}

void ports_outb_p(ObjectDesc * self, jint port, jbyte value)
{
#ifdef KERNEL
	poutb(port, value);
	SLOW_DOWN;
#else
	debugz(("PORT outb_p(%lx, %d)\n", port, value));
#endif
}

jbyte ports_inb_p(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	jbyte ret = pinb(port);
	SLOW_DOWN;
	return ret;
#else
	debugz(("PORT inb_p(%lx\n", port));
	return 0xff;
#endif
}

void ports_outl(ObjectDesc * self, jint port, jint value)
{
#ifdef KERNEL
	outl(port, value);
#else
	debugz(("PORT outl(%lx, %ld)\n", port, value));
#endif
}

void ports_outl_p(ObjectDesc * self, jint port, jint value)
{
#ifdef KERNEL
	/*printf("PORT outl_p(0x%lx, %lx)\n", port, value); */
	outl(port, value);
	SLOW_DOWN;
#else
	debugz(("PORT outl(%lx, %ld)\n", port, value));
#endif
}

jint ports_inl(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	u4_t ret = inl(port);
	// printf("RET=%d\n", ret);
	return ret;
#else
	debugz(("PORT inl(%lx\n", port));
	return 0xff;
#endif
}

jint ports_inl_p(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	jint ret = inl(port);
	SLOW_DOWN;
	return ret;
#else
	debugz(("PORT inl_p(%lx\n", port));
	return 0xff;
#endif
}

void ports_outw(ObjectDesc * self, jint port, jshort value)
{
#ifdef KERNEL
	outw(port, value);
#else
	debugz(("PORT outl(%lx, %ld)\n", port, value));
#endif
}

void ports_outw_p(ObjectDesc * self, jint port, jshort value)
{
#ifdef KERNEL
	outw(port, value);
	SLOW_DOWN;
#else
	debugz(("PORT outl(%lx, %ld)\n", port, value));
#endif
}

jshort ports_inw(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	return inw(port);
#else
	debugz(("PORT inl(%lx\n", port));
	return 0xff;
#endif
}

jshort ports_inw_p(ObjectDesc * self, jint port)
{
#ifdef KERNEL
	jshort ret = inw(port);
	SLOW_DOWN;
	/*printf("PORT inl_p(0x%lx) ->  0x%lx\n", port, ret); */
	return ret;
#else
	debugz(("PORT inl_p(%lx\n", port));
	return 0xff;
#endif
}

MethodInfoDesc portsMethods[] = {
	{"outb", "", ports_outb}
	,
	{"inb", "", ports_inb}
	,
	{"outb_p", "", ports_outb_p}
	,
	{"inb_p", "", ports_inb_p}
	,
	{"outl", "", ports_outl}
	,
	{"outl_p", "", ports_outl_p}
	,
	{"inl", "", ports_inl}
	,
	{"inl_p", "", ports_inl_p}
	,
	{"outw", "", ports_outw}
	,
	{"outw_p", "", ports_outw_p}
	,
	{"inw", "", ports_inw}
	,
	{"inw_p", "", ports_inw_p}
	,
};


void init_ports_portal()
{
	init_zero_dep_without_thread("jx/zero/Ports", "Ports", portsMethods, sizeof(portsMethods), "<jx/zero/Ports>");
}
