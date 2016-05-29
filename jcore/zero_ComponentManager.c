#include "all.h"

#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy->domain)

static jint componentManager_load(ObjectDesc * self, ObjectDesc * libnameObj)
{
	char value[128];
	LibDesc *lib;
	DomainDesc *sourceDomain = CALLERDOMAIN;
	if (libnameObj == 0)
		return 0;
	stringToChar(libnameObj, value, sizeof(value));
	lib = load(sourceDomain, value);
	return (jint) lib;	// HACK
}

static void componentManager_registerLib(ObjectDesc * self, ObjectDesc * libnameObj, ObjectDesc * memObj)
{
}

static void componentManager_setInheritThread(ObjectDesc * self, ObjectDesc * classnameObj)
{
	char value[128];
	ClassDesc *cl;
	DomainDesc *sourceDomain = CALLERDOMAIN;
	if (classnameObj == 0)
		return;
	stringToChar(classnameObj, value, sizeof(value));
	cl = findClassDesc(value);
	cl->inheritServiceThread = JNI_TRUE;
}

static MethodInfoDesc componentManagerMethods[] = {
	{"load", "", (code_t) componentManager_load},
	{"registerLib", "", (code_t) componentManager_registerLib},
	{"setInheritThread", "", (code_t) componentManager_setInheritThread},
};

void init_componentmanager_portal()
{
	init_zero_dep("jx/zero/ComponentManager", "ComponentManager", componentManagerMethods, sizeof(componentManagerMethods),
		      "<jx/zero/ComponentManager>");
}
