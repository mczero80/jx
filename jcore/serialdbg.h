#ifndef __SERIALDBG_H
#define __SERIALDBG_H
extern int debug_port;

void ser_putc(int x, char c);
void dbg_print(int port, char *s);
void check_serial();
void ser_dump(int port, const char *ptr, unsigned int size);
int ser_trygetchar(int port);
#ifdef LOG_PRINTF
void init_log_space();
void mem_putc(int x, char c);
#endif

#ifdef KERNEL
#define DUMP(ptr, l) ser_dump(debug_port, ptr, l)
#else
#define DUMP(ptr, l) pty_dump(ptr, l)
#endif

#endif				/* __SERIALDBG_H */
