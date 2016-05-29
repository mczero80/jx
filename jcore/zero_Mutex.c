#include "all.h"


/*
 *
 * Mutex Portal
 *
 */
static ClassDesc *mutexClass;

static int Mutex_lock(ObjectDesc * self)
{
	spin_lock(&(self->data));
	return 1;
}
static int Mutex_trylock(ObjectDesc * self)
{
	return spin_trylock(&(self->data));
}
static int Mutex_unlock(ObjectDesc * self)
{
	spin_unlock(&(self->data));
	return 1;
}
static int Mutex_destroy(ObjectDesc * self)
{
	return 1;
}

MethodInfoDesc mutexMethods[] = {
	{"lock", "()I", (code_t) Mutex_lock}
	,
	{"trylock", "()I", (code_t) Mutex_trylock}
	,
	{"unlock", "()I", (code_t) Mutex_unlock}
	,
	{"destroy", "()I", (code_t) Mutex_destroy}
	,
};


static jbyte mutexTypeMap[] = { 0 };

void init_mutex_portal()
{
	mutexClass =
	    init_zero_class("jx/zero/Mutex", mutexMethods, sizeof(mutexMethods), (sizeof(spinlock_t)) >> 2, mutexTypeMap,
			    "<jx/zero/Mutex>");
}
