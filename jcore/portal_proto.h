/********************************************************************************
 * Portal handling
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifndef PORTAL_PROTO_H
#define PORTAL_PROTO_H

#ifndef ASSEMBLER

#include "load.h"
#include "thread.h"
#include "domain.h"
#include "load.h"
#include "classes.h"

Proxy *createDEPProxy(ClassDesc * depClass, DEPDesc * target);
void installVtables(DomainDesc * domain, ClassDesc * c,
		    MethodInfoDesc * methods, int numMethods,
		    ClassDesc * cl);
ClassDesc *createSubClassDesc(DomainDesc * domain, ClassDesc * cl,
			      MethodInfoDesc * methods, int numMethods,
			      char *name);
DEPDesc *findDEPByName(char *name, jint namelen);


jint send_msg(DEPDesc ** dep);

/*
ThreadDesc *recv_msg(DEPDesc *dep);
void repl_msg(ThreadDesc *replyTo, DEPDesc *dep, jint value);
*/


#ifdef NEW_PORTALCALL
struct ServiceThreadPool_s;
//void receive_portalcall(struct ServiceThreadPool_s* pool);
void receive_portalcall(u4_t poolIndex);
#else
void receive_portalcall(u4_t serviceIndex);
#endif
jint send_portalcall(jint methodIndex, jint numParams, jint ** paramlist);


#endif				/*ASSEMBLER */

#endif
