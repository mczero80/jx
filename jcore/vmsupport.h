#ifndef VMSUPPORT_H
#define VMSUPPORT_H

#define VMSUPPORT(_ndx_) vmsupport[vmsupport[_ndx_].index]

typedef struct {
	char *name;
	int index;
	code_t fkt;
} vm_fkt_table_t;

extern jint numberVMOperations;
extern vm_fkt_table_t vmsupport[];

void vm_unsupported();

ClassDesc *get_element_class(ClassDesc * c);
jboolean is_interface(ClassDesc * c);
jboolean implements_interface(ClassDesc * c, ClassDesc * ifa);

#endif
