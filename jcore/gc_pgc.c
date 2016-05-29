#ifdef PROFILE_GC
#include "all.h"
#include "types.h"
#include "gc_pgc.h"

#include "gc_org_int.h"

#define PGC_GC_USE 1
#define PGC_MOVE_USE 1

struct pgcTscCtr_s {
	u8_t val, start;
	enum { STARTED, STOPPED } mode;
};

typedef struct pgcTscCtr_s pgcTscCtr;

char *pgcCtr2char(enum pgcCtrIds_e t)
{
	switch (t) {
	case PGC_STACK1:
		return "stack1";
	case PGC_STACK2:
		return "stack2";
	case PGC_STACK3:
		return "stack3";
	case PGC_STACK4:
		return "stack4";
	case PGC_STACKMAP:
		return "stackmap";
	case PGC_GC:
		return "gc";
	case PGC_MOVE:
		return "move";
	case PGC_STACK:
		return "stack";
	case PGC_SPECIAL:
		return "special";
	case PGC_REGISTERED:
		return "registered";
	case PGC_HEAP:
		return "heap";
	case PGC_INTR:
		return "intr";
	case PGC_SCAN:
		return "scan";
	case PGC_PROTECT:
		return "protect";
	case PGC_STATIC:
		return "static";
	case PGC_SERVICE:
		return "service";
	case MAX_PGC_CTR_ID:
	}
	return "unknown";
}

// FIXME
#define MAX_PADD 100


struct paDomainDesc_s {
	DomainDesc *domain;
	char name[100];
	jint *heap;
	jint *heapBorder;
	jint *heap2;
	jint *heapBorder2;
};
typedef struct paDomainDesc_s paDomainDesc;
paDomainDesc paDD[MAX_PADD];
int paDDsize = 0;
u1_t paDomianIndex(DomainDesc * domain)
{
	int i;
	for (i = 0; i < paDDsize; i++)
		if (paDD[i].domain == domain)
			break;
	if (i == MAX_PADD)
		sys_panic("paDD overflow");
	if (i == paDDsize)
		paDDsize++;
	paDD[i].domain = domain;
	if (domain->domainName)
		strncpy(paDD[i].name, domain->domainName, 100);
	else
		strcpy(paDD[i].name, "(null)");
	paDD[i].name[99] = 0;
	paDD[i].heap = GCM_ORG(domain).heap;
	paDD[i].heapBorder = GCM_ORG(domain).heapBorder;
	paDD[i].heap2 = GCM_ORG(domain).heap2;
	paDD[i].heapBorder2 = GCM_ORG(domain).heapBorder2;
	return i;
}

// 
struct pgcGCInfo_s {
	int di;
	pgcTscCtr ctr[MAX_PGC_CTR_ID];
};
typedef struct pgcGCInfo_s pgcGCInfo;
#define MAX_PGC_RUNS 170
pgcGCInfo pgcGCInfos[MAX_PGC_RUNS];
int pgcIndex = -1;



void pgcInitCtr(pgcTscCtr * ctr)
{
	ctr->val = 0;
	ctr->start = 0;
	ctr->mode = STOPPED;
}

void pgcStartCtr(pgcTscCtr * ctr)
{
	if (ctr->mode == STARTED)
		sys_panic("starting started TscCtr");
	ctr->mode = STARTED;
	rdtsc(ctr->start);
}

void pgcStopCtr(pgcTscCtr * ctr)
{
	u8_t stop;

	rdtsc(stop);
	if (ctr->mode == STOPPED)
		sys_panic("stopping stopped TscCtr");
	ctr->mode = STOPPED;
	if (stop > ctr->start)
		ctr->val += stop - ctr->start;
	else
		ctr->val -= ctr->start - stop;
}

void pgcEndRun()
{
	int i;
	for (i = 0; i < MAX_PGC_CTR_ID; i++)
		if (pgcGCInfos[pgcIndex].ctr[i].mode != STOPPED)
			sys_panic("pgcGCInfos[%d].ctr[%d] (%s) not stopped", pgcIndex, i, pgcCtr2char(i));
}

void pgcNewRun(DomainDesc * domain)
{
	int i;
	if (pgcIndex != -1)
		pgcEndRun();
	if (++pgcIndex >= MAX_PGC_RUNS)
		sys_panic("pgcCtrs overflow");
	pgcGCInfos[pgcIndex].di = paDomianIndex(domain);
	for (i = 0; i < MAX_PGC_CTR_ID; i++)
		pgcInitCtr(&(pgcGCInfos[pgcIndex].ctr[i]));
}

void pgcBegin(int i)
{
	pgcStartCtr(&(pgcGCInfos[pgcIndex].ctr[i]));
}

void pgcEnd(int i)
{
	pgcStopCtr(&(pgcGCInfos[pgcIndex].ctr[i]));
}

void printProfileGC()
{
	int i, j;
	printf("pgcStart\n");
	printf("%d %d\n", pgcIndex + 1, MAX_PGC_CTR_ID);
	for (i = 0; i < MAX_PGC_CTR_ID; i++)
		printf("%s\n", pgcCtr2char((enum pgcCtrIds_e) i));
	for (j = 0; j <= pgcIndex; j++) {
		printf("%s\n", paDD[pgcGCInfos[j].di].name);
		for (i = 0; i < MAX_PGC_CTR_ID; i++)
			printf("0x%08x%08x\n", *(((int *) &(pgcGCInfos[j].ctr[i].val)) + 1),
			       *(((int *) &(pgcGCInfos[j].ctr[i].val)) + 0));
	}
	printf("pgcStop\n");
}
#endif				/* PROFILE_GC */
