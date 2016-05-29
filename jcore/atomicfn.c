#define INATOMICFN 1
#include "all.h"
#include "symbols.h"

#undef ATOMICFN
#undef ATOMICFN0
#define ATOMICFN(_r_, _n_, _s_) _r_ (* _n_) _s_;
#define ATOMICFN0(_r_, _n_, _s_) _r_ (* _n_) _s_;
#include "atomicfn.h"


#undef ATOMICFN
#undef ATOMICFN0
#define ATOMICFN(_r_, _n_, _s_) _r_ nonatomic_##_n_ _s_;
#define ATOMICFN0(_r_, _n_, _s_) _r_ nonatomic_##_n_ _s_;
#include "atomicfn.h"

void atomicfn_init()
{
	printf("atomicfn init\n");
#undef ATOMICFN
#undef ATOMICFN0
#ifdef FKTSIZE_EMPTY
#define ATOMICFN(_r_, _n_, _s_) _n_ = NULL;
#define ATOMICFN0(_r_, _n_, _s_)
	sys_panic("FKTSIZE UNKNOWN!!");
#else
#ifdef NOPREEMPT

#define ATOMICFN(_r_, _n_, _s_) printf("%s: ", #_n_); _n_ = (_r_ (*) _s_) nopreempt_register((code_t)nonatomic_##_n_, FKTSIZE_nonatomic_##_n_);
#define ATOMICFN0(_r_, _n_, _s_) _n_ = (_r_ (*) _s_)  (code_t)nonatomic_##_n_;
#else
#define ATOMICFN(_r_, _n_, _s_) _n_ = (_r_ (*) _s_)  (code_t)nonatomic_##_n_;
#define ATOMICFN0(_r_, _n_, _s_) _n_ = (_r_ (*) _s_) (code_t)nonatomic_##_n_;
#endif				/* NOPREEMPT */
#endif				/*FKTSIZE_EMPTY */
#include "atomicfn.h"
	printf("atomicfn init completed\n");
}
