/********************************************************************************
 * Garbage collector object allocation
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef GC_ALLOC_H
#define GC_ALLOC_H

//FIXME
//ObjectDesc*  allocObjectInDomain(DomainDesc *domain, ClassDesc *c);

CPUStateProxy *allocCPUStateProxyInDomain(DomainDesc * domain,
					  ClassDesc * c,
					  ThreadDesc * cpuState);
Proxy *allocProxyInDomain(DomainDesc * domain, ClassDesc * c,
			  struct DomainDesc_s *targetDomain,
			  u4_t targetDomainID, u4_t index);
CredentialProxy *allocCredentialProxyInDomain(DomainDesc * domain,
					      ClassDesc * c,
					      u4_t signerDomainID);
DEPDesc *allocServiceDescInDomain(DomainDesc * domain);

ArrayDesc *allocArrayInDomain(DomainDesc * domain, ClassDesc * elemClass,
			      jint size);

DomainProxy *allocDomainProxyInDomain(DomainDesc * domain, DomainDesc * domainValue, u4_t domainID);
ThreadDescProxy *allocThreadDescProxyInDomain(DomainDesc * domain, ClassDesc * c);
ThreadDescForeignProxy *allocThreadDescForeignProxyInDomain(DomainDesc * domain, ThreadDescProxy * src);
#ifdef ENABLE_MAPPING
MappedMemoryProxy *allocMappedMemoryProxyInDomain(DomainDesc * domain, char *mem, ClassDesc *cl);
#endif /* ENABLE_MAPPING */

CPUDesc *specialAllocCPUDesc();
DomainDesc *specialAllocDomainDesc();

#endif				/* GC_ALLOC_H */
