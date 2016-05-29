#ifdef ENABLE_GC
#include "all.h"
#if defined(PROFILE_AGING) || defined(ZSTORE)
#include "gc_pa.h"
#include "minilzo.h"
#include "symfind.h"

#ifdef KERNEL
#include "serialdbg.h"
#else
#include <termios.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#endif

#include "gc_org_int.h"

#ifdef PROFILE_SAMPLE_PMC0
extern jint n_pmc0_samples;
extern jlong *pmc0_samples;
#endif
#ifdef PROFILE_SAMPLE_PMC1
extern jint n_pmc1_samples;
extern jlong *pmc1_samples;
#endif

extern char _start[], end[];

static const char *refType2String(paRefType refType)
{
	switch (refType) {
	case OBJECT:
		return "object";
	case ARRAY:
		return "array";
	case SERVICE:
		return "service";
	case ATOM:
		return "atom";
	case PROXY:
		return "proxy";
	case MEMORY:
		return "memory";
	case FOREIGN:
		return "foreign";
	}
	sys_panic("unknown reftype: %d", refType);
	return "";		// just keep the compiler happy
}

#define MAX_PAPOOL (1*1024*1024)
#define MAX_PAPLAIN (1*1024*1024)
#define MAX_PAPLAINSTRING 1000
#define MAX_PAPLAINDOMAIN 10
#define MAX_PAPLAINTHREADS 50
#define MAX_PAPLAINEIP 2000
#define MAX_PAPLAINEIP2 2000
#define IN_LEN		(64*1024L)
#define OUT_LEN		(IN_LEN + IN_LEN / 64 + 16 + 3)

static char paPool[MAX_PAPOOL];
static char *paNext = &paPool[0];
static char *paEnd = &(paPool[MAX_PAPOOL]);

static char paPlain[MAX_PAPLAIN];
static char *paPlainNext = &paPlain[0];
static char *paPlainEnd = &(paPlain[MAX_PAPLAIN]);

static int ddcmp(const paDomainDesc * a, const paDomainDesc * b, int n)
{
	return (u4_t) a->domain - (u4_t) b->domain;
}

static int eipcmp(const paEIP_t * a, const paEIP_t * b, int n)
{
	int i = (u4_t) a->eip - (u4_t) b->eip;
	if (i == 0)
		i = (u4_t) a->cn - (u4_t) b->cn;
	return i;
}

static int thrcmp(const paThread_t * a, const paThread_t * b, int n)
{
	int i = (u4_t) a->ptr - (u4_t) b->ptr;
	if (i == 0)
		i = (u4_t) a->name - (u4_t) b->name;
	return i;
}

static int paStrings[MAX_PAPLAINSTRING + PA_TABOFF] = { MAX_PAPLAINSTRING, 0, (int) strncmp, -1, (int) "paStrings" };

static int paDomains[MAX_PAPLAINDOMAIN + PA_TABOFF] = { MAX_PAPLAINDOMAIN, 0, (int) ddcmp, sizeof(paDomainDesc),
	(int) "paDomains"
};

static int paEIPs[MAX_PAPLAINEIP + PA_TABOFF] = { MAX_PAPLAINEIP, 0, (int) eipcmp, sizeof(paEIP_t), (int) "paEIPs" };

static int paEIP2s[MAX_PAPLAINEIP2 + PA_TABOFF] = { MAX_PAPLAINEIP2, 0, (int) eipcmp, sizeof(paEIP_t), (int) "paEIP2s" };

static int paThreads[MAX_PAPLAINTHREADS + PA_TABOFF] = { MAX_PAPLAINTHREADS, 0, (int) thrcmp, sizeof(paThread_t),
	(int) "paThreads"
};

static lzo_byte in[IN_LEN];
static int ini = 0;
static char lzo_is_init = 0;

/* Work-memory needed for compression. Allocate memory in units
 * of `long' (instead of `char') to make sure it is properly aligned.
 */

#define HEAP_ALLOC(var,size) \
	long __LZO_MMODEL var [ ((size) + (sizeof(long) - 1)) / sizeof(long) ]

static HEAP_ALLOC(wrkmem, LZO1X_1_MEM_COMPRESS);

static void paStore(const lzo_byte * ptr, int len)
{
	if (len <= 0)
		return;
	if (len + ini > IN_LEN) {
		int l = IN_LEN - ini;
		memcpy(in + ini, ptr, l);
		ini += l;

		if (paNext + 3 + OUT_LEN > paEnd) {
			ini -= l;
			sys_panic("paPool overflow");
		}

		if ((!lzo_is_init) && (lzo_init() != LZO_E_OK))
			sys_panic("lzo_init() failed");

		lzo_is_init = 1;
		if (lzo1x_1_compress(in, IN_LEN, paNext + 4, (int *) paNext, wrkmem) != LZO_E_OK)
			sys_panic("lzo1x_1_compress() failed");

		paNext += 4 + *(int *) paNext;
		ini = 0;
		paStore(ptr + l, len - l);
	} else {
		memcpy(in + ini, ptr, len);
		ini += len;
	}
}

static void paFlush()
{
	if (paNext + 4 + OUT_LEN > paEnd)
		sys_panic("paPool overflow");

	if ((!lzo_is_init) && (lzo_init() != LZO_E_OK))
		sys_panic("lzo_init() failed");

	lzo_is_init = 1;
	if (lzo1x_1_compress(in, ini, paNext + 4, (int *) paNext, wrkmem)
	    != LZO_E_OK)
		sys_panic("lzo1x_1_compress() failed");

	paNext += 4 + *(int *) paNext;
	ini = 0;
}

typedef int (*cmp_t) (const void *s1, const void *s2, size_t n);

static int binsearch(int *tab, int l, int r, const char *s, cmp_t cmp, int len)
{
	if (l == r) {
		return -l - 1;
	} else {
		int n = l + (r - l) / 2;
		int cr = cmp(s, paPlain + tab[n], len);
		if (cr == 0)
			return n;
		else if (cr < 0)
			return binsearch(tab, l, n, s, cmp, len);
		else
			return binsearch(tab, n + 1, r, s, cmp, len);
	}
}

static int paInsert(int *tab, const char *s)
{
	int len = tab[3];
	int n;

	if (s == NULL)
		s = "(null)";
	if (len < 0)
		len = strlen(s) + 1;
	n = binsearch(tab + PA_TABOFF, 0, tab[1], s, (cmp_t) tab[2], len);
	if (n < 0) {
		char *ptr = paPlainNext;
		n = -n - 1;

		if (tab[0] == tab[1])
			sys_panic("paTab %s overflow", (char *) (tab[4]));
		paPlainNext += len;

		if (paPlainNext > paPlainEnd) {
			paPlainNext -= len;
			sys_panic("paPlain overflow by from paTab %s", (char *) (tab[4]));
		}
		memcpy(ptr, s, len);
#ifdef KERNEL
		lzo_memmove(tab + PA_TABOFF + n + 1, tab + PA_TABOFF + n, sizeof(int) * tab[1] - n);
#else
		memmove(tab + PA_TABOFF + n + 1, tab + PA_TABOFF + n, sizeof(int) * tab[1] - n);
#endif
		tab[PA_TABOFF + n] = (u4_t) ptr - (u4_t) paPlain;
		tab[1]++;
	}
/*
 {
    int i;
    printf("TAB: %s, (%d/%d)\n", (char*)(tab[4]), tab[1], tab[0]);
    for (i = 0; i < tab[1]; i++)
      printf("%d: %s\n", i, paPlain + tab[PA_TABOFF+i]);
  }
  */
	return tab[PA_TABOFF + n];
}

u4_t paDomainIndex(DomainDesc * domain)
{
	paDomainDesc paDD;

	paDD.domain = domain;
	paDD.name = paInsert(paStrings, domain->domainName);
	paDD.heap = GCM_ORG(domain).heap;
	paDD.heapBorder = GCM_ORG(domain).heapBorder;
	paDD.heap2 = GCM_ORG(domain).heap2;
	paDD.heapBorder2 = GCM_ORG(domain).heapBorder2;

	return paInsert(paDomains, (char *) &paDD);
}

void paNew(DomainDesc * domain, jint size, jint * ptr)
{
	paNew_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) NEW;
	pa.di = paDomainIndex(domain);
	pa.size = size;
	pa.ptr = ptr;
	pa.tsc = get_tsc();

	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

void paMove(paRefType refType, jint * from, jint * to)
{
#ifndef PROFILE_AGING_CREATION_ONLY
	paMove_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) MOVE;
	pa.refType = (u1_t) refType;
	pa.from = from;
	pa.to = to;
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
#endif
}

void paGCStart(DomainDesc * domain, int memTime)
{
	paGCStart_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) GCSTART;
	pa.di = paDomainIndex(domain);
	pa.memTime = memTime;
	pa.tsc = get_tsc();
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

void paGCStop(DomainDesc * domain)
{
	paGCStop_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) GCSTOP;
	pa.di = paDomainIndex(domain);
	pa.tsc = get_tsc();
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

/* FIXME jgbauman 
void print_eip_info(char *addr) {
  ClassDesc *classInfo;
  MethodDesc *method;
  jint bytecodePos, lineNumber;
  if (addr == NULL) {
    printf("(null)");
    return;
                      }
  if (findMethodAtAddr(addr, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
     printf("(%s::%s%s (0x%lx) at byteocode %ld, line %ld)",
     classInfo->name, method->name, method->signature, method->code, bytecodePos, lineNumber);
     } else {
     char *cname = findCoreSymbol(addr);
     if (cname != NULL) {
     printf("(core:%s)",cname);
     } else {
      // look for proxy code 
      char *meth, *sig;
      if(findProxyCode(addr, &meth, &sig, &classInfo) == 0) {
      printf("(proxy:%s:%s%s)", classInfo->name, meth, sig);
     } else {
      printf("(???)");
     }
     } 
     }
}
 */

int findProxyCode(char *addr, char **method, char **sig, ClassDesc ** classInfo);
code_t getCaller(u4_t nup);

u4_t paEIP(const char *cn, u4_t eip)
{
	paEIP_t eipt;
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;
	int i, j;

	DISABLE_IRQ;
	memset(&eipt, 0, sizeof(paEIP_t));
	eipt.eip = eip;
	eipt.cn = paInsert(paStrings, cn);
	j = paEIPs[1];
	i = paInsert(paEIPs, (char *) &eipt);
	if (j != paEIPs[1]) {
		paEIP_t *eipp = (paEIP_t *) (paPlain + i);
		if ((eipp->eip != eipt.eip) || (eipp->cn != eipt.cn))
			sys_panic("wrong paEIP_t");
		eipp->pln = PA_NO_LINE;
		if (eip != 0) {
			if (findMethodAtAddr((u1_t *) eip, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
				eipp->pcn = paInsert(paStrings, classInfo->name);
				eipp->pmn = paInsert(paStrings, method->name);
				eipp->pms = paInsert(paStrings, method->signature);
				eipp->pln = lineNumber;
			} else {
				char *cname = findCoreSymbol(eip);
				if (cname != NULL) {
					eipp->pmn = paInsert(paStrings, cname);
				} else {
					char *meth, *sig;
					if (findProxyCode((char *) eip, &meth, &sig, &classInfo) == 0) {
						eipp->pcn = paInsert(paStrings, classInfo->name);
						eipp->pmn = paInsert(paStrings, meth);
						eipp->pms = paInsert(paStrings, sig);
					} else {

					}
				}
			}
		}
	}
	RESTORE_IRQ;
	return i;
}

u4_t paEIP2(u4_t eip)
{
	paEIP_t eipt;
	ClassDesc *classInfo;
	MethodDesc *method;
	jint bytecodePos, lineNumber;
	int i, j;

	DISABLE_IRQ;
	memset(&eipt, 0, sizeof(paEIP_t));
	eipt.eip = eip;
	eipt.cn = paInsert(paStrings, "(null)");
	j = paEIPs[1];
	i = paInsert(paEIP2s, (char *) &eipt);
	if (j != paEIP2s[1]) {
		paEIP_t *eipp = (paEIP_t *) (paPlain + i);
		if ((eipp->eip != eipt.eip))
			sys_panic("wrong paEIP_t");
		eipp->pln = PA_NO_LINE;
		if (eip != 0) {
			if (findMethodAtAddr((u1_t *) eip, &method, &classInfo, &bytecodePos, &lineNumber) == 0) {
				eipp->pcn = paInsert(paStrings, classInfo->name);
				eipp->pmn = paInsert(paStrings, method->name);
				eipp->pms = paInsert(paStrings, method->signature);
				eipp->pln = lineNumber;
			} else {
				char *cname = findCoreSymbol(eip);
				if (cname != NULL) {
					eipp->pmn = paInsert(paStrings, cname);
				} else {
					char *meth, *sig;
					if (findProxyCode((char *) eip, &meth, &sig, &classInfo) == 0) {
						eipp->pcn = paInsert(paStrings, classInfo->name);
						eipp->pmn = paInsert(paStrings, meth);
						eipp->pms = paInsert(paStrings, sig);
					} else {

					}
				}
			}
		}
	}
	RESTORE_IRQ;
	return i;
}

void paObj(jint * obj, ClassDesc * c)
{
	paObj_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TOBJ;
	pa.ptr = obj;
	pa.ci = paEIP(c->name, (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

char buf[1024];
void paArray(jint * array, ClassDesc * c, jint size)
{
	paArray_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TARRAY;
	pa.ptr = (jint *) array;
	buf[0] = '[';
	strcpy(buf + 1, c->name);
	pa.ci = paEIP(buf, (u4_t) getCaller(5));
	pa.size = size;
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

void paProxy(jint * obj, ClassDesc * c, DomainDesc * targetDomain, int depIndex)
{
	paProxy_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TPROXY;
	pa.ptr = obj;
	pa.ci = paEIP((c != NULL) ? c->name : "(c=null)", (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

/** THIS FUNCTION MUST NOT BE INTERRUPTED BY A GC */
void paMemProxy(jint * obj, ClassDesc * c)
{
	paMemProxy_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TMEMPROXY;
	pa.ptr = obj;
	pa.ci = paEIP(c->name, (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;

}

void paCSP(jint * obj, ClassDesc * c, void *cpuState)
{
	paCSP_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TCSP;
	pa.ptr = obj;
	pa.ci = paEIP(c->name, (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;

}

void paAVP(jint * obj, ClassDesc * c)
{
	paAVP_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TAVP;
	pa.ptr = obj;
	pa.ci = paEIP(c->name, (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;

}

void paDEP(jint * obj)
{
	paDEP_t pa;

	DISABLE_IRQ;
	pa.type = (u1_t) TDEP;
	pa.ptr = obj;
	pa.ci = paEIP(":DEP:", (u4_t) getCaller(3));
	paStore((const lzo_byte *) &pa, sizeof(pa));
	RESTORE_IRQ;
}

void paThread(ThreadDesc * d, char *name)
{
	paThread_t thr;

	DISABLE_IRQ;
	thr.ptr = (u4_t) d;
	thr.name = 0;
	if (name != NULL)
		thr.name = paInsert(paStrings, name);
	thr.domain = paDomainIndex(d->domain);

	paInsert(paThreads, (char *) &thr);
	RESTORE_IRQ;
}

#ifndef KERNEL
#define MAXSTRING 1024*1024
void pty_dump(const char *ptr, unsigned int size)
{
	u4_t s;
	struct termios ts1, ts2;

	tcgetattr(1, &ts1);
	ts2 = ts1;
	ts2.c_oflag &= ~ONLCR;
	tcsetattr(1, TCSADRAIN, &ts2);
	while (size > 0) {
		s = size < MAXSTRING ? size : MAXSTRING;
		{
			int r;
			do {
				r = s;
				if ((s = write(1, ptr, r)) < 0) {
					tcsetattr(1, TCSADRAIN, &ts1);
					printf("\nMonitor: ");
					sys_panic("printProfileAging: write error %s", strerror(errno));
				}
				r -= s;
			} while (r > 0);
		}
		ptr += s;
		size -= s;
	}
	tcsetattr(1, TCSADRAIN, &ts1);
}
#endif				/* KERNEL */

#ifdef KERNEL
#define DUMP(ptr, l) ser_dump(debug_port, ptr, l)
#else
#define DUMP(ptr, l) pty_dump(ptr, l)
#endif

static void libraries(int core)
{
	SharedLibDesc *sharedLib;
	int i = 0;
	u4_t n[3];

	sharedLib = sharedLibs;
	while (sharedLib != NULL) {
		i++;
		sharedLib = sharedLib->next;
	}
	if (core)
		i++;
	i *= sizeof(n);
	DUMP((char *) &i, sizeof(i));
	if (core) {
		n[0] = paInsert(paStrings, "(core)");
		n[1] = (u4_t) (_start);
		n[2] = (u4_t) (end);
		DUMP((char *) n, sizeof(n));
	}
	sharedLib = sharedLibs;
	while (sharedLib != NULL) {
		n[0] = paInsert(paStrings, sharedLib->name);
		n[1] = (u4_t) (sharedLib->code);
		n[2] = (u4_t) ((char *) sharedLib->code + sharedLib->codeBytes);
		DUMP((char *) n, sizeof(n));
		sharedLib = sharedLib->next;
	}
}

void printProfileAging()
{
	int l, m;

	DISABLE_IRQ;
	if (paNext + 4 + OUT_LEN > paEnd);
	else
		paFlush();
	RESTORE_IRQ;

	libraries(0);

	l = (u4_t) paPlainNext - (u4_t) & (paPlain[0]);
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) paPlain, l);

	l = paStrings[1];
	DUMP((char *) paStrings, sizeof(int) * (l + PA_TABOFF));

	l = paDomains[1];
	DUMP((char *) paDomains, sizeof(int) * (l + PA_TABOFF));

	l = paEIPs[1];
	DUMP((char *) paEIPs, sizeof(int) * (l + PA_TABOFF));

	l = (u4_t) paNext - (u4_t) & (paPool[0]);
	m = l + sizeof(u4_t) + ini;
	DUMP((char *) &m, sizeof(m));
	DUMP((char *) paPool, l);
	m = ini;
	m = -m;
	DUMP((char *) &m, sizeof(m));
	DUMP((char *) in, ini);
}

void rswitches()
{
#if 0
#ifdef PROFILE_EVENT_THREADSWITCH
	u4_t i, j, nthr, l;
	ThreadDesc *allthr[MAX_PAPLAINTHREADS];
	char **ips;
	u4_t nips = 0;
	nthr = 0;


	libraries(1);

	for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
		for (j = 0; j < nthr; j++) {
			if (profile_event_threadswitch_samples[i].to == allthr[j])
				break;
		}
		if (j == nthr) {
			allthr[nthr] = profile_event_threadswitch_samples[i].to;
			paThread(allthr[nthr], allthr[nthr]->name);
			if (++nthr == MAX_PAPLAINTHREADS)
				sys_panic("PROFILE_EVENT_THREADSWITCH:  #threads > MAX_PAPLAINTHREADS (=%d)", MAX_PAPLAINTHREADS);
		}
	}

	ips = jxmalloc(sizeof(char *) * profile_event_threadswitch_n_samples * 2 MEMTYPE_PROFILING);
	memset(ips, 0, sizeof(char *) * profile_event_threadswitch_n_samples * 2);
	for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
		for (j = 0; j < nips; j++) {
			if (profile_event_threadswitch_samples[i].ip_from == ips[j])
				break;
		}
		if (j == nips) {
			ips[nips] = profile_event_threadswitch_samples[i].ip_from;
			nips++;
		}
	}
	for (i = 0; i < profile_event_threadswitch_n_samples; i++) {
		for (j = 0; j < nips; j++) {
			if (profile_event_threadswitch_samples[i].ip_to == ips[j])
				break;
		}
		if (j == nips) {
			ips[nips] = profile_event_threadswitch_samples[i].ip_to;
			nips++;
		}
	}

	for (i = 0; i < nips; i++) {
		paEIP2((u4_t) ips[i]);
	}
	l = (u4_t) paPlainNext - (u4_t) & (paPlain[0]);
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) paPlain, l);

	l = paEIP2s[1];
	DUMP((char *) paEIP2s, sizeof(int) * (l + PA_TABOFF));

	l = paThreads[1];
	DUMP((char *) paThreads, sizeof(int) * (l + PA_TABOFF));

	l = paDomains[1];
	DUMP((char *) paDomains, sizeof(int) * (l + PA_TABOFF));

	l = profile_event_threadswitch_n_samples;
	l *= sizeof(struct profile_event_threadswitch_s);
	DUMP((char *) &l, sizeof(l));
	/*{
	   char *ptr=(char*)profile_event_threadswitch_samples;
	   while (l>1000) {
	   DUMP(ptr, 1000);
	   l -= 1000;
	   ptr += 1000;
	   }
	   DUMP(ptr, l);

	   } */
	DUMP((char *) profile_event_threadswitch_samples, l);
	DUMP("XXXX", 4);

#ifdef PROFILE_SAMPLE_PMC0

	l = n_pmc0_samples * sizeof(u8_t);
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) pmc0_samples, l);
#else
	l = 0;
	DUMP((char *) &l, sizeof(l));
#endif				/* PROFILE_SAMPLE_PMC0 */
	DUMP("YYYY", 4);
#ifdef PROFILE_SAMPLE_PMC1
	l = n_pmc1_samples * sizeof(u8_t);
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) pmc1_samples, l);
#else
	l = 0;
	DUMP((char *) &l, sizeof(l));
#endif				/* PROFILE_SAMPLE_PMC1 */

#ifdef EVENT_LOG
	rlogts();
#else
	l = 0;
	DUMP((char *) &l, sizeof(l));
#endif				/* EVENT_LOG */

#endif
#endif				/*0 */
}

#ifdef EVENT_LOG
void rlogts()
{
	u4_t l;
	l = sizeof(l) + sizeof(eventTypes);
	DUMP((char *) &n_event_types, sizeof(l));
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) eventTypes, l);
	l = n_events * sizeof(EventLog);
	DUMP((char *) &l, sizeof(l));
	DUMP((char *) events, l);
}
#endif				/*EVENT_LOG */

void printProfileAgingASCII()
{
	char *ptr = &paPool[0];
	// FIXME
	printf("paStart\n");

	while (ptr < paNext) {
		switch ((paType) * (u1_t *) ptr) {
		case NEW:{
				//paNew_t *pa = (paNew_t*)ptr;
				ptr += sizeof(paNew_t);
				/*printf("new %p %s %ld %p\n", paDD[pa->di].domain, paDD[pa->di].name,
				   pa->size, pa->ptr); */
				break;
			}
		case MOVE:{
				paMove_t *pa = (paMove_t *) ptr;
				ptr += sizeof(paMove_t);
				printf("move %s %p %p\n", refType2String(pa->refType), pa->from, pa->to);
				break;
			}
		case GCSTART:{
				//paGCStart_t *pa = (paGCStart_t*)ptr;
				ptr += sizeof(paGCStart_t);
/*      printf("gcstart %p %s %d %p %p %p %p\n", paDD[pa->di].domain, 
             paDD[pa->di].name, pa->memTime, paDD[pa->di].heap, 
             paDD[pa->di].heapBorder, paDD[pa->di].heap2, 
             paDD[pa->di].heapBorder2);
             */
				break;
			}
		case GCSTOP:{
				//paGCStop_t *pa = (paGCStop_t*)ptr;
				ptr += sizeof(paGCStop_t);
				//printf("gcstop %p %s\n", paDD[pa->di].domain, paDD[pa->di].name);
				break;
			}
		default:
			sys_panic("unknown paType: %d", *(paType *) ptr);
		}
	}

	printf("paStop\n");
}
#endif

#endif				/* ENABLE_GC */
