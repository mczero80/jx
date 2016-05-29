#ifndef ATOMIC_H
#define ATOMIC_H

void nopreempt_init();
code_t nopreempt_register(code_t original, u4_t size);

void atomicfn_init();

#endif				/* ATOMIC_H */
