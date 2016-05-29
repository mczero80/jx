#ifdef MEMORY_USE_NEW
#include "zero_Memory_new.c"
#endif

#ifdef MEMORY_USE_SHARED
#include "zero_Memory_shared.c"
#endif

#ifdef MEMORY_USE_ORG
#include "zero_Memory_org.c"
#endif
