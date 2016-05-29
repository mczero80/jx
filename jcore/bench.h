#ifndef BENCH_H
#define BENCH_H

struct benchtime_s {
	u4_t a;
	u4_t b;
	u4_t c;
	u4_t d;
};

void bench_empty(struct benchtime_s *x);
void bench_load(struct benchtime_s *x);

#endif				/* BENCH_H */
