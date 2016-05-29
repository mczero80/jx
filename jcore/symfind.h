#ifndef SYMFIND_H
#define SYMFIND_H

char *findCoreSymbol(jint addr);
u4_t sizeCoreSymbol(const char *name);
char *addrCoreSymbol(const char *name);
void printCoreSymbolInformation(char *name);

#endif				/* SYMFIND_H */
