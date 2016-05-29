#include "all.h"
#include "gc_memcpy.h"

#ifdef GC_FAST_MEMCPY
void gc_fast_memcpy(void *b, void *a, unsigned int s)
{
	//if ((s%8)!=0) sys_panic("memcpy: %d", s);
	//printf("memcpy %p %p %d\n", b, a, s);
#ifdef GC_USE_MMX
	if (!(s & 7)) {
		asm volatile ("movl $128,%%eax \n" "cmpl %%eax,%2 \n" "jb adB8_6 \n" "ja adB8_2 \n"
			      // move 128
			      "adB8_m128: \n" "movq 120(%1),%%mm0 \n" "movq %%mm0,120(%0) \n" "adB8_m120: \n"
			      "movq 112(%1),%%mm1 \n" "movq %%mm1,112(%0) \n" "adB8_m112: \n" "movq 104(%1),%%mm2 \n"
			      "movq %%mm2,104(%0) \n" "adB8_m104: \n" "movq 96(%1),%%mm3 \n" "movq %%mm3,96(%0) \n"
			      // move 96
			      "adB8_m96: \n" "movq 88(%1),%%mm4 \n" "movq %%mm4,88(%0) \n" "adB8_m88: \n" "movq 80(%1),%%mm5 \n"
			      "movq %%mm5,80(%0) \n" "adB8_m80: \n" "movq 72(%1),%%mm6 \n" "movq %%mm6,72(%0) \n" "adB8_m72: \n"
			      "movq 64(%1),%%mm7 \n" "movq %%mm7,64(%0) \n"
			      // move 64
			      "adB8_m64: \n" "movq 56(%1),%%mm0 \n" "movq %%mm0,56(%0) \n" "adB8_m56: \n" "movq 48(%1),%%mm1 \n"
			      "movq %%mm1,48(%0) \n" "adB8_m48: \n" "movq 40(%1),%%mm2 \n" "movq %%mm2,40(%0) \n" "adB8_m40: \n"
			      "movq 32(%1),%%mm3 \n" "movq %%mm3,32(%0) \n"
			      // move 32
			      "adB8_m32: \n" "movq 24(%1),%%mm4 \n" "movq %%mm4,24(%0) \n" "adB8_m24: \n" "movq 16(%1),%%mm5 \n"
			      "movq %%mm5,16(%0) \n" "adB8_m16: \n" "movq 8(%1),%%mm6 \n" "movq %%mm6,8(%0) \n" "adB8_m8: \n"
			      "movq (%1),%%mm7 \n" "movq %%mm7,(%0) \n" "jmp adB8_end \n"
			      //<128,64
			      "adB8_6: \n" "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adB8_m64 \n" "ja adB8_7 \n"
			      //<64,32
			      "adB8_1: \n" "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adB8_m32 \n" "ja adB8_3 \n"
			      //<32,16
			      "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adB8_m16 \n" "ja adB8_m24 \n" "jmp adB8_m8 \n"
			      //>32,48
			      "adB8_3: \n" "add $16,%%eax \n" "cmpl %%eax,%2 \n" "ja adB8_m56 \n" "je adB8_m48 \n"
			      "jmp adB8_m40 \n"
			      //>64,96
			      "adB8_7: \n" "add $32,%%eax \n" "cmpl %%eax,%2 \n" "je adB8_m96 \n" "ja adB8_8 \n"
			      //<96,80
			      "sub $16,%%eax \n" "cmpl %%eax,%2 \n" "ja adB8_m88 \n" "je adB8_m80 \n" "jmp adB8_m72 \n"
			      //>96,112
			      "adB8_8: \n" "add $16,%%eax \n" "cmpl %%eax,%2 \n" "ja adB8_m120 \n" "je adB8_m112 \n"
			      "jmp adB8_m104 \n"
			      // move all
			      "adB8_2: \n" "shrl $2,%2 \n" "cld \n" "rep \n" "movsl \n" "adB8_end: \n"::"D" (b), "S"(a),
			      "c"(s):"eax");
		return;
	}
#endif
	asm volatile ("movl $128,%%eax \n" "cmpl %%eax,%2 \n" "jb adb4_6 \n" "ja adb4_2 \n"
		      // move 128
		      "adb4_m128: \n" "movl 124(%1),%%ecx \n" "movl %%ecx,124(%0) \n" "adb4_m124: \n" "movl 120(%1),%%ecx \n"
		      "movl %%ecx,120(%0) \n" "adb4_m120: \n" "movl 116(%1),%%ecx \n" "movl %%ecx,116(%0) \n" "adb4_m116: \n"
		      "movl 112(%1),%%ecx \n" "movl %%ecx,112(%0) \n" "adb4_m112: \n" "movl 108(%1),%%ecx \n"
		      "movl %%ecx,108(%0) \n" "adb4_m108: \n" "movl 104(%1),%%ecx \n" "movl %%ecx,104(%0) \n" "adb4_m104: \n"
		      "movl 100(%1),%%ecx \n" "movl %%ecx,100(%0) \n" "adb4_m100: \n" "movl 96(%1),%%ecx \n"
		      "movl %%ecx,96(%0) \n"
		      // move 96
		      "adb4_m96: \n" "movl 92(%1),%%ecx \n" "movl %%ecx,92(%0) \n" "adb4_m92: \n" "movl 88(%1),%%ecx \n"
		      "movl %%ecx,88(%0) \n" "adb4_m88: \n" "movl 84(%1),%%ecx \n" "movl %%ecx,84(%0) \n" "adb4_m84: \n"
		      "movl 80(%1),%%ecx \n" "movl %%ecx,80(%0) \n" "adb4_m80: \n" "movl 76(%1),%%ecx \n" "movl %%ecx,76(%0) \n"
		      "adb4_m76: \n" "movl 72(%1),%%ecx \n" "movl %%ecx,72(%0) \n" "adb4_m72: \n" "movl 68(%1),%%ecx \n"
		      "movl %%ecx,68(%0) \n" "adb4_m68: \n" "movl 64(%1),%%ecx \n" "movl %%ecx,64(%0) \n"
		      // move 64
		      "adb4_m64: \n" "movl 60(%1),%%ecx \n" "movl %%ecx,60(%0) \n" "adb4_m60: \n" "movl 56(%1),%%ecx \n"
		      "movl %%ecx,56(%0) \n" "adb4_m56: \n" "movl 52(%1),%%ecx \n" "movl %%ecx,52(%0) \n" "adb4_m52: \n"
		      "movl 48(%1),%%ecx \n" "movl %%ecx,48(%0) \n" "adb4_m48: \n" "movl 44(%1),%%ecx \n" "movl %%ecx,44(%0) \n"
		      "adb4_m44: \n" "movl 40(%1),%%ecx \n" "movl %%ecx,40(%0) \n" "adb4_m40: \n" "movl 36(%1),%%ecx \n"
		      "movl %%ecx,36(%0) \n" "adb4_m36: \n" "movl 32(%1),%%ecx \n" "movl %%ecx,32(%0) \n"
		      // move 32
		      "adb4_m32: \n" "movl 28(%1),%%ecx \n" "movl %%ecx,28(%0) \n" "adb4_m28: \n" "movl 24(%1),%%ecx \n"
		      "movl %%ecx,24(%0) \n" "adb4_m24: \n" "movl 20(%1),%%ecx \n" "movl %%ecx,20(%0) \n" "adb4_m20: \n"
		      "movl 16(%1),%%ecx \n" "movl %%ecx,16(%0) \n" "adb4_m16: \n" "movl 12(%1),%%ecx \n" "movl %%ecx,12(%0) \n"
		      "adb4_m12: \n" "movl 8(%1),%%ecx \n" "movl %%ecx,8(%0) \n" "adb4_m8: \n" "movl 4(%1),%%ecx \n"
		      "movl %%ecx,4(%0) \n" "adb4_m4: \n" "movl (%1),%%ecx \n" "movl %%ecx,(%0) \n" "jmp adb4_end \n"
		      //<128
		      "adb4_6: \n" "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adb4_m64 \n" "ja adb4_7 \n"
		      //<64
		      "adb4_1: \n" "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adb4_m32 \n" "ja adb4_3 \n"
		      //<32
		      "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "je adb4_m16 \n" "ja adb4_4 \n"
		      //<16
		      "shrl $1,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m12 \n" "je adb4_m8 \n" "jmp adb4_m4 \n"
		      //>16
		      "adb4_4: \n" "add $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m28 \n" "je adb4_m24 \n" "jmp adb4_m20 \n"
		      //>32
		      "adb4_3: \n" "add $16,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_5 \n" "je adb4_m48 \n"
		      //<48
		      "sub $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m44 \n" "je adb4_m40 \n" "jmp adb4_m36 \n"
		      //>48
		      "adb4_5: \n" "add $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m60 \n" "je adb4_m56 \n" "jmp adb4_m52 \n"
		      //>64
		      "adb4_7: \n" "add $32,%%eax \n" "cmpl %%eax,%2 \n" "je adb4_m96 \n" "ja adb4_8 \n"
		      //<96
		      "sub $16,%%eax \n" "cmpl %%eax,%2 \n" "je adb4_m80 \n" "ja adb4_10 \n"
		      //<80
		      "sub $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m76 \n" "je adb4_m72 \n" "jmp adb4_m68 \n"
		      //>80
		      "adb4_10: \n" "add $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m92 \n" "je adb4_m88 \n" "jmp adb4_m84 \n"
		      //>96
		      "adb4_8: \n" "add $16,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_9 \n" "je adb4_m112 \n"
		      //<112
		      "sub $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m108 \n" "je adb4_m104 \n" "jmp adb4_m100 \n"
		      //>112
		      "adb4_9: \n" "add $8,%%eax \n" "cmpl %%eax,%2 \n" "ja adb4_m124 \n" "je adb4_m120 \n" "jmp adb4_m116 \n"
		      // move all
		      "adb4_2: \n" "shrl $2,%2 \n" "cld \n" "rep \n" "movsl \n" "adb4_end: \n"::"D" (b), "S"(a), "c"(s):"eax");
}

#endif				/* GC_FAST_MEMCPY */
