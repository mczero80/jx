#ifdef NETWORK_EMULATION

jint netemulation_open(ObjectDesc * self)
{
	return 0;
}

MethodInfoDesc fbemulationMethods[] = {
	{"open", "", netemulation_open}
	,
};

void init_network_emulation_portal()
{
	init_zero_dep("jx/zero/NetEmulation", "NetEmulation", netemulationMethods, sizeof(netemulationMethods),
		      "<jx/zero/NetEmulation>");
}

#endif				/* NETWORK_EMULATION */
