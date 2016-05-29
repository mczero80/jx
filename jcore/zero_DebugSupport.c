#include "all.h"

#define CALLERDOMAIN (curthr()->mostRecentlyCalledBy?curthr()->mostRecentlyCalledBy->domain:domainZero)

void debugsupport_dumpDomain(ObjectDesc * self, ObjectDesc * domainObj)
{
	DomainDesc *domain = obj2domainDesc(domainObj);
	dumpDomainInfo(domain);
}

void debugsupport_dumpObject(ObjectDesc * self, ObjectDesc * obj)
{
	dumpObject(obj);
}

void debugsupport_sendBinary(ObjectDesc * self, ObjectDesc * name, struct MemoryProxy_s *data, jint size)
{
#if (defined(DEBUGSUPPORT_DUMP) || defined(MONITOR)) && defined(KERNEL)
	char value[128];
	if (name == 0 || data == 0)
		return;
	stringToChar(name, value, sizeof(value));
	ASSERT(size <= memory_size(data));
	send_binary(value, memory_getMem(data), size);
#endif
}

void debugsupport_registerMonitorCommand(ObjectDesc * self, ObjectDesc * name, ObjectDesc * cmd)
{
#if defined(MONITOR)
#if defined(JAVA_MONITOR_COMMANDS)
	char value[128];
	Proxy *proxy;
	u4_t quota = 1000;
	DomainDesc *domain = CALLERDOMAIN;
	if (name == 0 || cmd == 0)
		return;
	stringToChar(name, value, sizeof(value));

	DISABLE_IRQ;
#ifdef COPY_TO_DOMAINZERO
	proxy = cmd;
#else
	proxy = copy_reference(domain, curdom(), cmd, &quota);
#endif
	RESTORE_IRQ;

	register_command(value, registerObject(curdom(), proxy), domain);
#endif
#endif				/* MONITOR */
}

void debugsupport_breakpoint(ObjectDesc * self)
{
	asm("int $3");
}

MethodInfoDesc debugsupportMethods[] = {
	{"dumpDomain", "(Ljx/zero/Domain;)V", debugsupport_dumpDomain},
	{"dumpObject", "(Ljava/lang/Object;)V", debugsupport_dumpObject},
	{"sendBinary", "(Ljx/zero/Memory;I)V", debugsupport_sendBinary},
	{"registerMonitorCommand",
	 "(Ljava/lang/String;Ljx/zero/debug/MonitorCommand;)V",
	 debugsupport_registerMonitorCommand},
	{"breakpoint", "()V", debugsupport_breakpoint},
};

void init_debugsupport_portal()
{
	init_zero_dep("jx/zero/DebugSupport", "DebugSupport", debugsupportMethods, sizeof(debugsupportMethods),
		      "<jx/zero/DebugSupport>");
}
