/********************************************************************************
 * Garbage collector
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Joerg Baumann
 *******************************************************************************/

#if ENABLE_GC
#include "all.h"

#include "gc_memcpy.h"
#include "gc_move.h"
#include "gc_pa.h"
#include "gc_pgc.h"

/* FIXME prototypes */
int eip_in_last_stackframe(u4_t eip);
ObjectHandle(*registerObject) (DomainDesc * domain, ObjectDesc * o);
// FIXME jgbauman remove
extern unsigned char callnative_special_end[], callnative_special_portal_end[], callnative_static_end[], thread_exit_end[];
void return_from_java0(ThreadDesc * next, ContextDesc * restore);
void return_from_java1(long param, ContextDesc * restore, ThreadDesc * next);
void return_from_java2(long param1, long param2, ThreadDesc * next, ContextDesc * restore);
extern unsigned char return_from_javaX_end[], never_return_end[];
void never_return(void);
jint cpuManager_receive(ObjectDesc * self, Proxy * portal);
extern unsigned char cpuManager_receive_end[];

jboolean find_stackmap(MethodDesc * method, u4_t * eip, u4_t * ebp, jbyte * stackmap, u4_t maxslots, u4_t * nslots)
{

	SymbolDescStackMap *sym;
	jbyte *addr;
	char *symip, *symippre;
	int j, k;
	jint numSlots;
	jbyte b = 0;

	PGCB(STACKMAP);
	for (j = 0; j < method->numberOfSymbols; j++) {
		if ((method->symbols[j]->type != 16) && (method->symbols[j]->type != 17))
			continue;
		sym = (SymbolDescStackMap *) method->symbols[j];
		symip = (char *) method->code + (jint) (sym->immediateNCIndex);
		symippre = (char *) method->code + (jint) (sym->immediateNCIndexPre);

#if 0
		printf("STACKMAP at ip=%p (%d): ", (method->code + (jint) (sym->immediateNCIndex)), sym->n_bytes);
		addr = sym->map;
		b = 0;
		for (k = 0; k < sym->n_bits; k++) {
			if (k % 8 == 0)
				b = *addr++;
			printf("%d", b & 1);
			b >>= 1;
		}
		printf("\n");
#endif

		if ((u4_t *) symip == eip || (u4_t *) symippre == eip) {
			//jint *s=ebp;
			//printf("Map found at NCIndex %ld\n", sym->immediateNCIndex);
			//s--; /* ebp, ... */

			addr = sym->map;
			numSlots = sym->n_bits;
			if (numSlots > maxslots)
				sys_panic("stack frame too large");
			*nslots = numSlots;
			for (k = 0; k < sym->n_bits; k++) {
				if (k % 8 == 0)
					b = *addr++;
				//printf("%d %p %p\n", b&1, s , (char*)*s);
				//s--;
				stackmap[k] = b & 1;
				b >>= 1;
			}
			PGCE(STACKMAP);
			return JNI_TRUE;
		}
	}
	PGCE(STACKMAP);
	return JNI_FALSE;
}

void list_stackmaps(MethodDesc * method)
{
	SymbolDescStackMap *sym;
	jbyte *addr;
	char *symip;
	int j, k;
	//jint numSlots;
	jbyte b = 0;

	for (j = 0; j < method->numberOfSymbols; j++) {

		if ((method->symbols[j]->type != 16) && (method->symbols[j]->type != 17))
			continue;

		sym = (SymbolDescStackMap *) method->symbols[j];
		symip = (char *) method->code + (jint) (sym->immediateNCIndex);

		printf("   stackmap at: %p  (IPpre: %p)\n", symip, (char *) method->code + (jint) (sym->immediateNCIndexPre));

		addr = sym->map;
		for (k = 0; k < sym->n_bits; k++) {
			if (k % 8 == 0)
				b = *addr++;
			printf("%d", b & 1);
			b >>= 1;
		}

		printf("\n");
	}
}

//#define DBG_STACKMAP 1
//#define DBG_STACKMAP_SP 1
#define MOVE_ONLY_IF_INHEAP 1
void walkStack(DomainDesc * domain, ThreadDesc * thread, HandleReference_t handleReference)
{
	int i, k;
	u4_t *ebp, *eip, *sp, *s, *prevSP;
	ClassDesc *classInfo;
	MethodDesc *method, *prevMethod;
	char *sig;
	jint bytecodePos, lineNumber;
	jint numSlots;
	jbyte stackmap[128];
	extern char _start[], end[];

#if defined(DBG_STACKMAP) || defined(DBG_STACKMAP_SP)
	printf("Thread %d.%d tcb=%p stack=%p stackTop=%p\n", TID(thread), thread, thread->stack, thread->stackTop);
#endif

	prevMethod = NULL;
	prevSP = NULL;

	ebp = (u4_t *) thread->context[PCB_EBP];
	sp = (u4_t *) thread->context[PCB_ESP];
	eip = (u4_t *) thread->context[PCB_EIP];

	while (sp != NULL && sp < thread->stackTop) {

		prevMethod = method;
		//printf("start=%p end=%p\n", _start, end);
		if (eip >= (u4_t *) _start && eip <= (u4_t *) end) {
			PGCB(STACK1);
			/* our own text segment */
			/*printf("NATIVE "); print_eip_info(eip); printf("\n"); */
#ifdef DBG_STACKMAP
			printf("NATIVE\n");
#endif
			if (eip_in_last_stackframe((u4_t) eip)) {
				PGCE(STACK1);
				if (*(eip - 1) == 0xfb)
					sys_panic("FIXME: interrupted before call in callnative_special_portal");
#ifdef DBG_STACKMAP
				printf("  Last interesting frame on this stack\n");
#endif
				break;	// no more stack frames
			}
			if ((eip >= (u4_t *) callnative_special && eip <= (u4_t *) callnative_special_end)
			    || (eip >= (u4_t *) callnative_static && eip <= (u4_t *) callnative_static_end)
			    || (eip >= (u4_t *) callnative_special_portal && eip <= (u4_t *) callnative_special_portal_end)
//#ifdef JAVASCHEDULER
			    || (eip >= (u4_t *) return_from_java0 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) return_from_java1 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) return_from_java2 && eip <= (u4_t *) return_from_javaX_end)
			    || (eip >= (u4_t *) never_return && eip <= (u4_t *) never_return_end)
//#endif
			    ) {
				/* C -> Java */
				/*  scan parameters */

#ifdef DBG_STACKMAP
				if (prevMethod)
					printf("  C -> Java control transfer; previous method %s %s  numArgs=%d\n",
					       prevMethod->name, prevMethod->signature, (int) prevMethod->numberOfArgs);
				else
					printf("  C -> Java control transfer; unknown previous method \n");

				printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
				printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
#endif

				if (prevMethod != NULL) {
					u4_t *s = sp;
					s += 2;
					/* callnative_static  does not put an ObjectDesc onto the stack */
					if (!(eip >= (u4_t *) callnative_static && eip <= (u4_t *)
					      callnative_static_end)) {
#ifdef DBG_STACKMAP
						printf("  self:   %p %p ", s, (void *) *s);
#endif
						if (handleReference) {
							handleReference(domain, (ObjectDesc **) s);
#ifdef DBG_STACKMAP
							printf("  moved to %p", (void *) *s);
#endif
						}
#ifdef DBG_STACKMAP
						printf("\n");
#endif
						s++;
					}
					for (i = 1; i < prevMethod->numberOfArgs + 1; i++) {
						if (isRef(prevMethod->argTypeMap, prevMethod->numberOfArgs, i - 1)) {
#ifdef DBG_STACKMAP
							printf("  %d:   1  %p %p ", i, s, (void *) *s);
#endif
							if (handleReference) {
								handleReference(domain, (ObjectDesc **)
										s);
#ifdef DBG_STACKMAP
								printf("  moved to %p", (void *)
								       *s);
#endif
							}
						} else {
#ifdef DBG_STACKMAP
							if (domain->gc.isInHeap(domain, *s)) {
								printf(" maybe reference");
							}
							printf("  %d:   0  %p %p ", i, s, (void *) *s);
							printf("  not moved");
#endif
						}
						s++;
#ifdef DBG_STACKMAP
						printf("\n");
#endif
					}

#ifdef DBG_STACKMAP
					for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
						char *ptr = (char *) *s;
						printf("  %p:   %p   (ebp=%p)\n", s, ptr, ebp);
					}
#endif
				}
				// break; /* no more Java stack frames; NOT TRUE: newString, executeSpecial, ... */
//#ifdef JAVASCHEDULER
				/* no more Java stack frames for these functions: */
				if ((eip >= (u4_t *) return_from_java0 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) return_from_java1 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) return_from_java2 && eip <= (u4_t *) return_from_javaX_end)
				    || (eip >= (u4_t *) never_return && eip <= (u4_t *) never_return_end)
				    )
					break;
//#endif

			} else {
#ifdef DBG_STACKMAP
				char *cname = findCoreSymbol((jint) eip);
				if (cname != NULL) {
					printf("Skipping core frame:%s\n", cname);
#if 0
					if (!(eip >= (u4_t *) thread_exit && eip <= (u4_t *) (char *) thread_exit + 48)) {	/* last frame; ebp and ret are invalid */
						printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
						printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
						for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
							char *ptr = (char *) *s;
							printf("  %p:   %p\n", s, ptr);
						}
					}
#endif
				} else {
					printf("Unknown code at address 0x%lx\n", (u4_t) eip);
					printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
					printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
					for (s = (u4_t *) (ebp - 1), k = 0; s > (sp + 1); s--, k++) {
						char *ptr = (char *) *s;
						printf("  %p:   %p\n", s, ptr);
					}
					sys_panic("");
				}
#endif
			}


			PGCE(STACK1);
		} else {
			int q;
#ifdef PROFILE_GC
			PGCB(STACK3);
#endif
			q = findMethodAtAddrInDomain(domain, (char *) eip, &method, &classInfo, &bytecodePos, &lineNumber);
#ifdef PROFILE_GC
			PGCE(STACK3);
#endif
			if (q == 0) {
				PGCB(STACK2);
#ifdef DBG_STACKMAP
				printf("%s::%s%s at BC=%ld LINE=%d (codestart=%p:eip=%p)\n", classInfo->name, method->name,
				       method->signature, bytecodePos, (int) lineNumber, method->code, eip);
#endif

				if (!find_stackmap(method, eip, ebp, stackmap, sizeof(stackmap), &numSlots)) {
					printf("No stackmap for this frame! at %p; thread=%p\n", eip, thread);
					list_stackmaps(method);
					sys_panic("No stackmap for this frame!");
				}
#ifdef DBG_STACKMAP
				printf("  %p:  x %8p RET\n", ebp + 1, (void *) *(ebp + 1));
				printf("  %p:  x %8p EBP\n", ebp, (void *) *ebp);
#endif
				for (s = ebp - 1, k = 0; s > (sp + 1); s--, k++) {
					//char* sm;
#ifdef DBG_STACKMAP
					char *ptr = (char *) *s;
#endif
					if (k >= numSlots) {
#ifdef DBG_STACKMAP
						printf("  %p:  ? %8p", s, ptr);
#endif
					} else {
#ifdef DBG_STACKMAP
						printf("  %p:  %d %8p", s, stackmap[k], ptr);
						if (!stackmap[k]) {
							printf(" %d", (int) ptr);
						}
#endif
					}
#ifdef DBG_STACKMAP
					if (domain->gc.isInHeap(domain, ptr)) {
						printf(" HEAP ");

#ifdef USE_QMAGIC
						if (getObjMagic(ptr) == MAGIC_OBJECT) {
							ClassDesc *oclass = obj2ClassDesc(ptr);
							printf("%s", oclass->name);
						}
#endif
					} else {
						if (!stackmap[k]) {
							printf(" %d", (int) ptr);
						} else {
							if (ptr == (char *)
							    getInitialNaming()) {
								printf(" InitialNaming");
							}
						}
					}

#endif				/* DBG_STACKMAP */
					if (handleReference) {
						if (stackmap[k]) {	/* found reference */
#ifdef MOVE_ONLY_IF_INHEAP
							if (domain->gc.isInHeap(domain, *s)) {
								handleReference(domain, (ObjectDesc **) s);
							}
#else
							handleReference(domain, (ObjectDesc **) s);
#endif				/* MOVE_ONLY_IF_INHEAP */

#ifdef DBG_STACKMAP
							printf("  moved to %p", (void *) *s);
#endif
						} else {
#ifdef DBG_STACKMAP
							if (domain->gc.isInHeap(domain, *s)) {
								printf(" maybe reference");
							}
							printf("  not moved");
#endif
						}
					}
#ifdef DBG_STACKMAP
					printf("\n");
#endif
				}
				PGCE(STACK2);
			} else {
				int q;
#ifdef PROFILE_GC
				PGCB(STACK4);
#endif
				q = findProxyCodeInDomain(domain, eip, &method, &sig, &classInfo);
#ifdef PROFILE_GC
				PGCE(STACK4);
#endif
				if (q == 0) {
#ifdef DBG_STACKMAP
					printf("PROXY\n");
					printf("  %p:  x %p RET\n", ebp + 1, (void *) *(ebp + 1));
					printf("  %p:  x %p EBP\n", ebp, (void *) *ebp);
#endif
				} else {
					printf("walkStack: Warning: Strange eip thread %d.%d (%p) eip=%p esp=%p ebp=%p\n",
					       TID(thread), thread, eip, sp, ebp);
					//sys_panic("Strange instruction pointer");
				}
			}
		}
#if defined(DBG_STACKMAP) || defined(DBG_STACKMAP_SP)
		printf("SP=%p EBP=%p\n", sp, ebp);
#endif

		prevSP = sp;

		sp = ebp;
		if (sp == NULL)
			break;
		ebp = (u4_t *) * sp;
		eip = (u4_t *) * (sp + 1);
	}
}
#endif				/* ENABLE_GC */
