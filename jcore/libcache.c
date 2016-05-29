#include "libcache.h"
#include "all.h"

static libcache_entry *global_jll_cache = NULL;

libcache_entry *libcache_new_entry(ObjectDesc * string_obj, ObjectDesc * memory_obj)
{
	libcache_entry *new_entry;

	if ((new_entry = jxmalloc(sizeof(libcache_entry) MEMTYPE_OTHER)) == NULL) {
		return NULL;
	}

	new_entry->name = string_obj;
	new_entry->codefile = memory_obj;
	new_entry->next = NULL;

	return new_entry;
}

void libcache_init()
{
	global_jll_cache = NULL;
}

char *libcache_lookup_jll(const char *name, jint * size)
{
	libcache_entry *entry;
	ObjectDesc *str;
	char e_str[80];

	for (entry = global_jll_cache; entry != NULL; entry = entry->next) {

		str = entry->name;
		stringToChar(str, e_str, 80);

		if (strcmp(name, e_str) == 0) {

			if (size != NULL)
				*size = memory_size(entry->codefile);

			return (char *) memory_getStartAddress(entry->codefile);
		}

	}

	if (size != NULL)
		*size = 0;
	return NULL;
}

void libcache_register_jll(ObjectDesc * self, ObjectDesc * string_obj, ObjectDesc * memory_obj)
{
	libcache_entry *newjll;
	char e_str[80];

	stringToChar(string_obj, e_str, 80);

	printf("register: %s\n", e_str);

	if ((newjll = libcache_new_entry(string_obj, memory_obj)) == NULL) {
		sys_panic("no memory for libcache");
	}

	newjll->next = global_jll_cache;
	global_jll_cache = newjll;

	return;
}
