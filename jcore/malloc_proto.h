
LibDesc *malloc_libdesc(DomainDesc * domain);
ClassDesc *malloc_classdesc(DomainDesc * domain, u4_t namelen);
PrimitiveClassDesc *malloc_primitiveclassdesc(DomainDesc * domain,
					      u4_t namelen);
Class *malloc_class(DomainDesc * domain);
Class *malloc_classes(DomainDesc * domain, u4_t number);
MethodDesc *malloc_methoddesc(DomainDesc * domain);
char **malloc_vtableSym(DomainDesc * domain, u4_t vtablelen);
MethodDesc *malloc_methods(DomainDesc * domain, u4_t number);
MethodDesc **malloc_methodVtable(DomainDesc * domain, u4_t number);
code_t *malloc_vtable(DomainDesc * domain, u4_t number);
SharedLibDesc *malloc_sharedlibdesc(DomainDesc * domain, u4_t namelen);
char *malloc_string(DomainDesc * domain, u4_t len);
char *malloc_staticsmap(DomainDesc * domain, u4_t size);
char *malloc_objectmap(DomainDesc * domain, u4_t size);
char *malloc_argsmap(DomainDesc * domain, u4_t size);
SymbolDesc **malloc_symboltable(DomainDesc * domain, u4_t len);
SymbolDesc *malloc_symbol(DomainDesc * domain, u4_t size);
char *malloc_stackmap(DomainDesc * domain, u4_t size);
ByteCodeDesc *malloc_bytecodetable(DomainDesc * domain, u4_t len);
SourceLineDesc *malloc_sourcelinetable(DomainDesc * domain, u4_t len);
u4_t *malloc_staticfields(DomainDesc * domain, u4_t number);
struct nameValue_s *malloc_domainzero_namevalue();
char *malloc_proxycode(DomainDesc * domain, u4_t size);
char *malloc_cpudesc(DomainDesc * domain, u4_t size);
SharedLibDesc **malloc_sharedlibdesctable(DomainDesc * domain,
					  u4_t number);
ClassDesc *malloc_classdescs(DomainDesc * domain, u4_t number);
ArrayClassDesc *malloc_arrayclassdesc(DomainDesc * domain, u4_t namelen);
u4_t *malloc_threadstack(DomainDesc * domain, u4_t size, u4_t align);
u1_t *malloc_nativecode(DomainDesc * domain, u4_t size);
