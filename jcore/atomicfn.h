
ATOMICFN(ObjectHandle, registerObject,
	 (DomainDesc * domain, ObjectDesc * o));
ATOMICFN(ObjectDesc *, unregisterObject,
	 (DomainDesc * domain, ObjectHandle o));
ATOMICFN(ClassDesc *, handle2ClassDesc, (ObjectDesc ** handle));
//ATOMICFN(void,   vm_checkcast,           (ObjectDesc *obj, ClassDesc *c));

#ifdef ATOMIC_MEMORY
#if ! defined(MEMORY_USE_NEW) && ! defined(MEMORY_USE_SHARED)
/*
ATOMICFN(jint,         memory_get32,               (ObjectDesc *self, jint where));
ATOMICFN(void,         memory_set32,               (ObjectDesc *self, jint where, jint what));
*/
//ATOMICFN(ObjectDesc*,  memory_extendRange,         (ObjectDesc* self, jint atbegin, jint atend));
ATOMICFN(int, memoryIsValid, (MemoryProxyHandle handle));
ATOMICFN(char *, memoryGetMem, (MemoryProxyHandle handle));
ATOMICFN(jint, memoryGetSize, (MemoryProxyHandle handle));
ATOMICFN(jint, memoryGetValid, (MemoryProxyHandle handle));
ATOMICFN0(void, memorySetValid, (MemoryProxyHandle handle, jint v));
//ATOMICFN0(void,              createDZMemoryInstance,  (MemoryProxyHandle mr));
#endif
#endif

ATOMICFN0(ObjectDesc *, allocObjectInDomain,
	  (DomainDesc * domain, ClassDesc * c));
