typedef struct gc_move_common_mem_s {
	u4_t* (*allocHeap2) (struct DomainDesc_s * domain, u4_t size);
	void (*walkHeap2) (struct DomainDesc_s * domain,
			  HandleObject_t handleObject,
			  HandleObject_t handleArray,
			  HandleObject_t handlePortal,
			  HandleObject_t handleMemory,
			  HandleObject_t handleService,
			  HandleObject_t handleCAS,
			  HandleObject_t handleAtomVar,
			  HandleObject_t handleDomainProxy,
			   HandleObject_t handleCPUStateProxy,
			   HandleObject_t handleServicePool,
			  HandleObject_t handleStack);
} gc_move_common_mem_t;

u4_t *gc_common_move_reference(DomainDesc * domain, ObjectDesc ** refPtr);

#define GCM_MOVE_COMMON(domain) (*(gc_move_common_mem_t*)(&domain->gc.untypedMemory))
