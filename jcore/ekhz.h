#ifndef EKHZ_H
# define EKHZ_H

# ifdef USE_EKHZ

typedef struct CPUFrequency_s {
	u4_t khz;
	u4_t mhz;
} CPUFrequency;

void getCPUFrequency(CPUFrequency * freqs);

# endif				/* USE_EKHZ */

#endif				/* EKHZ_H */
