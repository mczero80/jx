#include "all.h"

void executeForallClasses(DomainDesc * domain, classexec_f func)
{
	jint k;
	jint i;
	for (k = 0; k < domain->numberOfLibs; k++) {
		LibDesc *lib = domain->libs[k];
		for (i = 0; i < lib->numberOfClasses; i++) {
			func(&(lib->allClasses[i]));
		}
	}
}
