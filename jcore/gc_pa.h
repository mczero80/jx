#ifndef GC_PA_H
#define GC_PA_H

#if defined(PROFILE_AGING) || defined(ZSTORE)

#include "types.h"
#include "domain.h"
#ifdef KERNEL
#include "serialdbg.h"
#endif

# define PA_TABOFF 5
void pty_dump(const char *ptr, unsigned int size);

#ifdef KERNEL
#define DUMP(ptr, l) ser_dump(debug_port, ptr, l)
#else
#define DUMP(ptr, l) pty_dump(ptr, l)
#endif

enum paType_e { NEW, MOVE, GCSTART, GCSTOP, TOBJ, TARRAY, TPROXY, TDEP,
	    TMEMPROXY, TCSP, TAVP };
typedef enum paType_e paType;

# define PA_NO_LINE 0x80000000

enum paRefType_e { OBJECT, ARRAY, SERVICE, ATOM, PROXY, MEMORY, FOREIGN };
typedef enum paRefType_e paRefType;

struct paNew_s {
	u1_t type;
	u4_t di:24;
	jint size;
	jint *ptr;
	u8_t tsc;
};
typedef struct paNew_s paNew_t;

struct paObj_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paObj_s paObj_t;

struct paArray_s {
	u1_t type;
	u4_t ci:24;
	jint size;
	jint *ptr;
};
typedef struct paArray_s paArray_t;

struct paProxy_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paProxy_s paProxy_t;

struct paMemProxy_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paMemProxy_s paMemProxy_t;

struct paCSP_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paCSP_s paCSP_t;

struct paAVP_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paAVP_s paAVP_t;

struct paDEP_s {
	u1_t type;
	u4_t ci:24;
	jint *ptr;
};
typedef struct paDEP_s paDEP_t;

struct paMove_s {
	u1_t type;
	u1_t refType;
	jint *from;
	jint *to;
};
typedef struct paMove_s paMove_t;

struct paGCStart_s {
	u1_t type;
	u4_t di:24;
	u8_t tsc;
	int memTime;
};
typedef struct paGCStart_s paGCStart_t;

struct paGCStop_s {
	u1_t type;
	u4_t di:24;
	u8_t tsc;
};
typedef struct paGCStop_s paGCStop_t;

struct paDomainDesc_s {
	DomainDesc *domain;
	u4_t name;
	jint *heap;
	jint *heapBorder;
	jint *heap2;
	jint *heapBorder2;
};
typedef struct paDomainDesc_s paDomainDesc;

struct paEIP_s {
	u4_t eip;
	u4_t cn;
	u4_t pcn;
	u4_t pmn;
	u4_t pms;
	jint pln;
};
typedef struct paEIP_s paEIP_t;

struct paThread_s {
	u4_t ptr;
	u4_t name;
	u4_t domain;
};
typedef struct paThread_s paThread_t;

void paNew(DomainDesc * domain, jint size, jint * ptr);

void paMove(paRefType refType, jint * from, jint * to);

void paGCStart(DomainDesc * domain, int memTime);

void paGCStop(DomainDesc * domain);

void printProfileAging();

void printProfileAgingASCII();

void paObj(jint * obj, ClassDesc * c);

void paArray(jint * array, ClassDesc * c, jint size);

void paProxy(jint * obj, ClassDesc * c, DomainDesc * targetDomain,
	     int depIndex);

void paMemProxy(jint * obj, ClassDesc * c);

void paCSP(jint * obj, ClassDesc * c, void *cpuState);

void paAVP(jint * obj, ClassDesc * c);

void paDEP(jint * obj);

void rswitches();

void rlogts();
#endif				/* PROFILE_AGING */
#endif				/* GC_PA_H */
