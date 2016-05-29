/********************************************************************************
 * JVM support functions
 * Copyright 1998-2002 Michael Golm
 * Copyright 2001-2002 Christian Wawersich
 *******************************************************************************/

#include "all.h"

#define debugf(x)
#define debugs(x)
#ifdef DBG_LOAD
#define debugc(x) printf x
#else				/* DBG_LOAD */
#define debugc(x)
#endif				/* DBG_LOAD */

static jint vm_spinlock = 1;

jboolean check_assign(ClassDesc * variable_class, ClassDesc * reference_class);
jboolean implements_interface(ClassDesc * c, ClassDesc * ifa);
jboolean is_interface(ClassDesc * c);

/* new vmsupport functions */

jboolean vm_instanceof(ObjectDesc * obj, ClassDesc * c)
{
	ClassDesc *oclass;
	ASSERTCLASSDESC(c);

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(obj, 20);

	if (obj == NULL)
		return JNI_TRUE;

	/* nonatomic_handle2ClassDesc() */
	oclass = handle2ClassDesc(&obj);
	ASSERTCLASSDESC(oclass);

	if (oclass == c)
		return JNI_TRUE;

	return check_assign(c, oclass);
}

/*
 * in use 
 * obj is checked by gc -> gc save
 */
void vm_checkcast(ObjectDesc * obj, ClassDesc * c)
{
	ClassDesc *oclass;

	ASSERTCLASSDESC(c);

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(obj, 20);

	if (obj == NULL)
		return;		/* null reference can be casted to every type */

	ASSERTOBJECT(obj);

	oclass = handle2ClassDesc(&obj);
	if (check_assign(c, oclass) != JNI_TRUE) {
		ObjectDesc *ex = createExceptionInDomain(curdom(), "java/lang/ClassCastException", NULL);
#ifdef DEBUG
		printf("cast %s to %s failed\n", oclass->name, c->name);
#endif
		throw_exception(ex, ((u4_t *) & obj - 2));
	}

	return;
}

void vm_arraycopy(ObjectDesc * src, unsigned int soff, ObjectDesc * dst, unsigned int doff, unsigned int count)
{
	//ObjectDesc **sh,**dh;
	ClassDesc *sclass, *dclass;
	u4_t *sdata, *ddata;
	jint ssize, dsize;

	CHECK_NULL_PTR(src);
	CHECK_NULL_PTR(dst);

	ASSERTOBJECT(src);
	ASSERTOBJECT(dst);

	sclass = obj2ClassDesc(src);
	if (!isArrayClass(sclass)) {
		printf("sclass %s is not an array class (%s:%d)\n", sclass->name, __FILE__, __LINE__);
		if (strcmp(sclass->name, "<Array>") != 0)
			exceptionHandler(THROW_RuntimeException);
	}
	dclass = obj2ClassDesc(dst);
	if (!isArrayClass(dclass)) {
		printf("dclass %s is not an array class (%s:%d)\n", dclass->name, __FILE__, __LINE__);
		if (strcmp(dclass->name, "<Array>") != 0)
			exceptionHandler(THROW_RuntimeException);
	}
	if (!check_assign(dclass, sclass)) {
		printf("dclass %s is not an assignable to sclass %s (%s:%d)\n", dclass->name, sclass->name, __FILE__, __LINE__);
		exceptionHandler(THROW_RuntimeException);
	}
#ifndef ALL_ARRAYS_32BIT
#ifdef DEBUG
	printf("error: wrong array type (%s:%i)\n", __FILE__, __LINE__);
	exceptionHandler(THROW_RuntimeException);
#endif
#endif

	/* HACK-WARNING: This is not SMP-save and long arraycopies may lock the system */
#ifdef KERNEL
	DISABLE_IRQ;
#endif

	ssize = ((ArrayDesc *) src)->size;
	sdata = ((ArrayDesc *) src)->data;
	dsize = ((ArrayDesc *) dst)->size;
	ddata = ((ArrayDesc *) dst)->data;

	if ((doff + count > dsize) || (soff + count > ssize)) {
#ifdef KERNEL
#define PLAIN_RESTORE_IRQ  setEFlags(oldflags);
		PLAIN_RESTORE_IRQ;
#endif
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
	}

	if ((sdata == ddata) && (doff > soff)) {
		int i;
		for (i = 0; i < count; ++i) {
			ddata[--doff] = sdata[--soff];
		}
	} else {
		jxwordcpy(sdata + soff, ddata + doff, count);
	}

#ifdef KERNEL
	RESTORE_IRQ;
#endif
}

void vm_arraycopy_right(ObjectDesc * src, unsigned int soff, ObjectDesc * dst, unsigned int doff, unsigned int count)
{
	ClassDesc *sclass, *dclass;
	u4_t *sdata, *ddata;
	jint ssize, dsize;

	CHECK_NULL_PTR(src);
	CHECK_NULL_PTR(dst);

	ASSERTOBJECT(src);
	ASSERTOBJECT(dst);

	sclass = obj2ClassDesc(src);
	dclass = obj2ClassDesc(dst);
	if (!check_assign(dclass, sclass))
		exceptionHandler(THROW_RuntimeException);

#ifndef ALL_ARRAYS_32BIT
#ifdef DEBUG
	printf("error: wrong array type (%s:%i)\n", __FILE__, __LINE__);
	exceptionHandler(THROW_RuntimeException);
#endif
#endif

	/* HACK-WARNING: This is not SMP-save and long arraycopies may lock the system */
#ifdef KERNEL
	DISABLE_IRQ;
#endif

	ssize = ((ArrayDesc *) src)->size;
	sdata = ((ArrayDesc *) src)->data;
	dsize = ((ArrayDesc *) dst)->size;
	ddata = ((ArrayDesc *) dst)->data;

	if ((doff + count > dsize) || (soff + count > ssize)) {
#ifdef KERNEL
#define PLAIN_RESTORE_IRQ  setEFlags(oldflags);
		PLAIN_RESTORE_IRQ;
#endif
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
	}

	jxwordcpy(sdata + soff, ddata + doff, count);

#ifdef KERNEL
	RESTORE_IRQ;
#endif
}

void vm_arraycopy_left(ObjectDesc * src, unsigned int soff, ObjectDesc * dst, unsigned int doff, unsigned int count)
{
	printf("not implemented yet!\n");
	exceptionHandler(THROW_RuntimeException);
}

void test_static(ClassDesc * c, void *f, int offset)
{
#ifdef DEBUG
	Class *cl;
	int off;

	cl = classDesc2Class(curdom(), c);
	off = (int) f - (int) (cl->staticFields);

	if ((off < 0) || (off >= (c->staticFieldsSize * sizeof(jint *)))) {
		sys_panic("static field out of range %s %d %d", c->name, off, offset);
	}
#endif
}

void vm_test_cinit(ClassDesc * c)
{
	Class *cl;
	//if (!curdom()->initialized) {
	cl = classDesc2Class(curdom(), c);
	if (cl->state != CLASS_READY) {
		printf("call class constructor for %s\n", cl->classDesc->name);
		callClassConstructor(cl);
	}
	//}
	return;
}


/*
 * gc save
 */
jint vm_getStaticsAddr(ClassDesc * c)
{
#ifndef USE_LIB_INDEX
	sys_panic("not impl.");
#endif
	ASSERTCLASSDESC(c);
	return curdom()->sfields;
}

jint vm_getStaticsAddr2(ClassDesc * c)
{
#ifndef USE_LIB_INDEX
	sys_panic("not impl.");
#endif
	ASSERTCLASSDESC(c);
	return (curdom()->sfields[c->definingLib->ndx]);
}


//#define BARRIER_CHECK 1

void blubb()
{
}

void vm_put_field32(ObjectDesc * obj, jint offset, jint value)
{
#ifdef DEBUG
	ClassDesc *c;
#endif
	jint *o = (jint *) obj;
	ASSERTOBJECT(obj);
#ifdef DEBUG
	c = obj2ClassDesc(obj);
	ASSERTCLASSDESC(c);
	if (offset < 0 || offset > c->instanceSize)
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
#endif
#ifdef BARRIER_CHECK
	if (((u4_t) o & 0xffffff00) == ((u4_t) value & 0xffffff00)) {
		blubb(o);
	}
#endif
	o[offset] = value;
}

void vm_put_static_field32(ClassDesc * c, jint offset, jint value)
{
	jint *s;

	ASSERTCLASSDESC(c);

#ifdef DEBUG
	printf("%s(%p)->data[%i] = %i\n", c->name, curdom()->sfields[c->definingLib->ndx], offset, value);
#endif

	s = (curdom()->sfields[c->definingLib->ndx]);

	s[offset] = value;
}

void vm_put_array_field32(ArrayDesc * arr, jint index, jint value)
{
#ifndef ALL_ARRAYS_32BIT
#ifdef DEBUG
	ClassDesc *c = arr->arrayClass;
	ASSERTCLASSDESC(c);
	if (ARRAY_8BIT(c) || (ARRAY_16BIT(c))) {
		printf("error: wrong array type (%s:%i)\n", __FILE__, __LINE__);
		exceptionHandler(THROW_RuntimeException);
	}
#endif
#endif
	ASSERTOBJECT(arr);
	if (index < arr->size) {
		arr->data[index] = value;
	} else {
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
	}
}


#ifdef ENABLE_MAPPING
void vm_map_put32(ObjectDesc * obj, jint offset, jint value)
{
#ifdef DEBUG
	ClassDesc *c;
#endif
	MappedMemoryProxy *o = (MappedMemoryProxy *) obj;
	ASSERTOBJECT(obj);
#ifdef DEBUG
	c = obj2ClassDesc(obj);
	ASSERTCLASSDESC(c);
/* use mapped size!!
	if (offset < 0 || offset > c->instanceSize)
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
*/
#endif
	*((u4_t *) ((u1_t *) o->mem + offset)) = value;
}

jint vm_map_get32(ObjectDesc * obj, jint offset)
{
#ifdef DEBUG
	ClassDesc *c;
#endif
	jint *o = (jint *) obj;
	ASSERTOBJECT(obj);
#ifdef DEBUG
	c = obj2ClassDesc(obj);
	ASSERTCLASSDESC(c);
	if (offset < 0 || offset > c->instanceSize)
		exceptionHandler(THROW_ArrayIndexOutOfBounds);
#endif
	return o[offset];
}
#endif				/* ENABLE_MAPPING */


/*
 * gc save
 */
jint vm_breakpoint(jint a)
{
#ifdef SMP
	printf("CPU%d: BREAKPOINT:\n", get_processor_id());
#else
	printf("BREAKPOINT:\n");
#endif
	asm("int $3");
}

/*
 * unsave
 */
jint vm_getclassname(ObjectDesc * ref)
{
	Class *cl;
	CHECK_NULL_PTR(ref);
	ASSERTOBJECT(ref);
	cl = obj2class(ref);
	ASSERTCLASS(cl);
	/* printf("classname %s\n",cl->classDesc->name); */
	return newStringFromClassname(curdom(), cl->classDesc->name);
}

/*
 * unused
 */
jint vm_getinstancesize(ClassDesc * c)
{
	exceptionHandler(THROW_UnsupportedByteCode);
	return c->instanceSize;
}

/*
 * unused
 */
jint vm_isprimitive(ClassDesc * c)
{
	exceptionHandler(THROW_UnsupportedByteCode);
	return c->classType == CLASSTYPE_PRIMITIVE;
}

/*
 * usnused
 */
void vm_monitorenter(ObjectDesc * o)
{
	printf("MONTITORENTER");
}

/*
 * unused
 */
void vm_monitorexit(ObjectDesc * o)
{
	printf("MONTITOREXIT");
}

/*
 * helper funktions
 */

jboolean isArrayClass(ClassDesc * c)
{
	ASSERTCLASSDESC(c);
	return *c->name == '[';
}

ClassDesc *get_element_class(ClassDesc * c)
{
	return ((ArrayClassDesc *) c)->elementClass;
}

jboolean is_subclass_of(ClassDesc * subclass, ClassDesc * superclass)
{
	ASSERTCLASSDESC(subclass);
	ASSERTCLASSDESC(superclass);
	if (subclass == superclass)
		return JNI_TRUE;
	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(subclass, 10);
	while ((subclass = subclass->superclass) != NULL) {
		ASSERTCLASSDESC(subclass);
		if (subclass == superclass)
			return JNI_TRUE;
	}

	return JNI_FALSE;
}

jboolean is_subinterface_of(ClassDesc * subif, ClassDesc * superif)
{
	ASSERTCLASSDESC(subif);
	ASSERTCLASSDESC(superif);
	if (subif == superif)
		return JNI_TRUE;
	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(subif, 10);
	while ((subif = subif->superclass) != NULL) {
		ASSERTCLASSDESC(subif);
		if (subif == superif)
			return JNI_TRUE;
		if (implements_interface(subif, superif))
			return JNI_TRUE;
	}

	return JNI_FALSE;
}

jboolean implements_interface(ClassDesc * c, ClassDesc * ifa)
{
	int i;
	ASSERTCLASSDESC(c);
	ASSERTCLASSDESC(ifa);

	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(c, 124);

	if (is_subinterface_of(c, ifa))
		return JNI_TRUE;

	for (i = 0; i < c->numberOfInterfaces; i++) {
		if (implements_interface(c->interfaces[i], ifa))
			return JNI_TRUE;
		if (is_subinterface_of(c->interfaces[i], ifa))
			return JNI_TRUE;
	}

	return JNI_FALSE;
}

jboolean is_interface(ClassDesc * c)
{
	ASSERTCLASSDESC(c);
	return c->classType == CLASSTYPE_INTERFACE;
}

jboolean check_assign(ClassDesc * variable_class, ClassDesc * reference_class)
{
	ASSERTCLASSDESC(reference_class);
	ASSERTCLASSDESC(variable_class);
	if (variable_class == reference_class)
		return JNI_TRUE;
	/* check if sufficient stack space exist to run this function */
	CHECK_STACK_SIZE(variable_class, 20);
	//  if (reference_class == class_Array && isArrayClass(variable_class)) return JNI_TRUE;
	if (isArrayClass(reference_class)) {
		/* class must be java.lang.Object or
		 * the element class of obj must be a subclass of the element class of class
		 */
		if (variable_class == java_lang_Object)
			return JNI_TRUE;
		if (!isArrayClass(variable_class))
			return JNI_FALSE;
		if (check_assign(get_element_class(variable_class), get_element_class(reference_class)))
			return JNI_TRUE;
		return JNI_FALSE;
	}
	if (is_subclass_of(reference_class, variable_class))
		return JNI_TRUE;
	if (is_interface(variable_class)
	    && implements_interface(reference_class, variable_class))
		return JNI_TRUE;
	return JNI_FALSE;
}

ObjectDesc *vm_getnaming(ObjectDesc * src)
{
	return getInitialNaming();
}

void vm_unsupported()
{
	sys_panic("vmfkt not supported");
}

#ifdef FAST_MEMORY_CALLS
void vm_get8();
void vm_get32();
void vm_getLittleEndian32();
#endif
#ifdef EVENT_LOG
void vm_event();
void vm_event()
{
	u4_t event;
	/* MUST BE THE FIRST INSTRUCTION IN THIS FUNCTION!! */
	asm volatile ("movl %%ebx, %0":"=r" (event));
	RECORD_EVENT(event);
}
#endif
vm_fkt_table_t vmsupport[] = {
	{"vm_unsupported", 0, (code_t) vm_unsupported},
	{"vm_instanceof", 0, (code_t) vm_instanceof},
	{"nil", 0, (code_t) vm_unsupported},	//vm_dep_send,
	{"vm_breakpoint", 0, (code_t) vm_breakpoint},
	{"vm_getclassname", 0, (code_t) vm_getclassname},
	{"vm_getinstancesize", 0, (code_t) vm_getinstancesize},
	{"vm_isprimitive", 0, (code_t) vm_isprimitive},
	{"vm_monitorenter", 0, (code_t) vm_monitorenter},
	{"vm_monitorexit", 0, (code_t) vm_monitorexit},
	{"vm_checkcast", 0, (code_t) vm_checkcast},
	{"vm_test_cinit", 0, (code_t) vm_test_cinit},
	{"vm_getStaticsAddr", 0, (code_t) vm_getStaticsAddr},
	{"vm_getStaticsAddr2", 0, (code_t) vm_getStaticsAddr2},
	{"vm_spinlock", 0, (code_t) & vm_spinlock},
	{"vm_arraycopy", 0, (code_t) vm_arraycopy},
	{"vm_arraycopy_right", 0, (code_t) vm_arraycopy_right},
	{"vm_arraycopy_left", 0, (code_t) vm_arraycopy_left},
	{"vm_getnaming", 0, (code_t) vm_getnaming},
	{"vm_stackoverflow", 0, (code_t) throw_StackOverflowError},
	{"vm_arrindex", 0, (code_t) throw_ArrayIndexOutOfBounds},
	{"vm_nullchk", 0, (code_t) throw_NullPointerException},
	{"vm_arith", 0, (code_t) throw_ArithmeticException},
#ifdef EVENT_LOG
	{"vm_event", 0, (code_t) vm_event},
#endif
#ifdef FAST_MEMORY_CALLS
	{"vm_get8", 0, (code_t) vm_get8},
	{"vm_get32", 0, (code_t) vm_get32},
	{"vm_getLittleEndian32", 0, (code_t) vm_getLittleEndian32},
#endif
	{"vm_put_field32", 0, (code_t) vm_put_field32},
	{"vm_put_array_field32", 0, (code_t) vm_put_array_field32},
	{"vm_put_static_field32", 0, (code_t) vm_put_static_field32},
#ifdef ENABLE_MAPPING
	{"vm_map_get32", 0, (code_t) vm_map_get32},
	{"vm_map_put32", 0, (code_t) vm_map_put32},
#endif				/* ENABLE_MAPPING */

};
jint numberVMOperations = sizeof(vmsupport) / sizeof(vm_fkt_table_t);
