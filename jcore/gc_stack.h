#ifndef GC_STACK_H
#define GC_STACK_H
#ifdef ENABLE_GC

jboolean find_stackmap(MethodDesc * method, u4_t * eip, u4_t * ebp,
		       jbyte * stackmap, u4_t maxslots, u4_t * nslots);
void list_stackmaps(MethodDesc * method);
void walkStack(DomainDesc * domain, ThreadDesc * thread,
	       HandleReference_t handleReference);

#endif				/* ENABLE_GC */
#endif				/* GC_STACK_H */
