/*

  The libcache is used by the online compiler to insert
  new jll-files into the system.

*/


#ifndef LIBCACHE
#define LIBCACHE 1

#include "types.h"
#include "object.h"

typedef struct libcache_entry_s {
	ObjectDesc *name;	/* Java String object */
	ObjectDesc *codefile;	/* Java Memory object */
	struct libcache_entry_s *next;
} libcache_entry;

void libcache_init();

char *libcache_lookup_jll(const char *name, jint * size);

void libcache_register_jll(ObjectDesc * self,
			   ObjectDesc * string_obj,
			   ObjectDesc * memory_obj);

#endif
