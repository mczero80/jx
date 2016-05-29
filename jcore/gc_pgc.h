#ifndef GC_PGC_H
#define GC_PGC_H

#ifdef PROFILE_GC
void pgcNewRun();
void pgcEndRun();
void pgcBegin(int i);
void pgcEnd(int i);

void printProfileGC();

enum pgcCtrIds_e {
	PGC_GC,
	PGC_MOVE,
	PGC_STACK,
	PGC_SPECIAL,
	PGC_REGISTERED,
	PGC_HEAP,
	PGC_INTR,
	PGC_SCAN,
	PGC_PROTECT,
	PGC_STATIC,
	PGC_STACKMAP,
	PGC_STACK1,
	PGC_STACK2,
	PGC_STACK3,
	PGC_STACK4,
	PGC_SERVICE,
	MAX_PGC_CTR_ID
};

#define PGC_GC_USE   1
#define PGC_MOVE_USE 1
#define PGC_STACK_USE 1
#define PGC_SPECIAL_USE 1
#define PGC_REGISTERED_USE 1
#define PGC_HEAP_USE 1
#define PGC_INTR_USE 1
#define PGC_SCAN_USE 1
#define PGC_PROTECT_USE 1
#define PGC_STATIC_USE 1
#define PGC_SERVICE_USE 1
#define PGC_STACKMAP_USE 1
#define PGC_STACK1_USE 1
#define PGC_STACK2_USE 1
#define PGC_STACK3_USE 1
#define PGC_STACK4_USE 1


#define PGCB(x) if (PGC_##x##_USE) pgcBegin(PGC_##x);
#define PGCE(x) if (PGC_##x##_USE) pgcEnd(PGC_##x);

#else				/* PROFILE_GC */

#define PGCB(x)
#define PGCE(x)

#endif				/* PROFILE_GC */

#endif				/* GC_PGC_H */
