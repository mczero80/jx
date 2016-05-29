#ifndef CLASSES_H
#define CLASSES_H

#include "load.h"

typedef void (*classexec_f) (Class * cl);

#include "domain.h"

void executeForallClasses(DomainDesc * domain, classexec_f func);

#endif
