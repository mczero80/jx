#ifndef TYPES_H
#define TYPES_H

typedef signed char jbyte;
typedef signed short jshort;
typedef signed long jint;
typedef signed long long jlong;
typedef int jboolean;

typedef unsigned long long u8_t;
typedef unsigned long u4_t;
typedef unsigned char u1_t;
typedef unsigned short u2_t;

typedef signed long s4_t;
typedef signed short s2_t;

typedef void *addr_t;
#ifdef KERNEL
typedef u4_t size_t;
#endif

#ifndef NULL
#define NULL ((void*)0)
#endif

#define JNI_FALSE 0
#define JNI_TRUE 1

typedef void (*code_t) ();
typedef int (*int_code_t) ();
typedef jlong(*longop_t) (jlong a, jlong b);

#endif				/* TYPES_H */
