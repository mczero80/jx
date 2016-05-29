#ifndef GC_ORG_INT_H
#define GC_ORG_INT_H

typedef struct gc_org_mem_s {
	jint *heapBorder;	/* pointer to border of heap (last allocated word  + 1) */
	jint *heap;		/* all objects life here */
	jint *heapFreePtr;	/* start of allocated (not aligned) memory */
	jint *heapTop;		/* pointer to free heap space */
	jint *heapBorder2;	/* pointer to border of heap (last allocated word  + 1) */
	jint *heap2;		/* all objects life here */
	jint *heap2FreePtr;	/* start of allocated (not aligned) memory */
	jint *heapTop2;		/* pointer to free heap space */
} gc_org_mem_t;

#define GCM_ORG(domain) (*(gc_org_mem_t*)(&domain->gc.untypedMemory))

u4_t *gc_org_move_reference(DomainDesc * domain, ObjectDesc ** refPtr);
u4_t *gc_org_move_service(DomainDesc * domain, ObjectDesc ** refPtr,
			  jboolean onlyContents);
u4_t *gc_org_move_array(DomainDesc * domain, ArrayDesc ** refPtr,
			jboolean onlyContents);
u4_t *gc_org_move_portal(DomainDesc * domain, ObjectDesc ** refPtr);
u4_t *gc_org_move_object(DomainDesc * domain, ObjectDesc ** refPtr,
			 jboolean onlyContents);
u4_t *gc_org_move_memory(DomainDesc * domain, ObjectDesc ** refPtr,
			 jboolean onlyContents);
u4_t *gc_org_move_atomvar(DomainDesc * domain, ObjectDesc ** refPtr,
			  jboolean onlyContents);
u4_t *gc_org_move_cas(DomainDesc * domain, ObjectDesc ** refPtr,
		      jboolean onlyContents);

#endif				/* GC_ORG_H_INT */
