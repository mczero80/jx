#include "all.h"

static char *zipstart = NULL;
static char *zipend = NULL;

#ifdef LOG_PRINTF
int printf2mem = 0;
#endif

#define phystokv(x) x

struct multiboot_module *multiboot_get_module()
{
	struct multiboot_module *m = (struct multiboot_module *) boot_info.mods_addr;
	unsigned i;

	if (!(boot_info.flags & MULTIBOOT_MODS))
		return NULL;

	if (boot_info.mods_count != 1) {
		printf("mods_count != 1\n");
		for (i = 0; i < boot_info.mods_count; i++) {
			printf("Module: %s\n", m[i].string);
		}
		return NULL;
	}

	return &m[0];
}


/*
 *
 */
int atexit(void (*__function) (void))
{
}


int write(int fd, char *buf, int len)
{

}



/*
 *
 */

int gettimeofday(struct timeval *tp, void *x)
{
	return 0;
}

int open(const char *path, int oflag)
{
}

int fstat()
{
}

int read()
{
}

int close()
{
}


/*
 *
 */
#define LONG_MIN (-2147483647-1)
#define LONG_MAX 2147483647
#define	isascii(c)	(((c) & ~0x7F) == 0)
#define isalpha(c) ((c>='A' && c<='Z') || (c>='a' && c<='z'))
#define isupper(c) (c>='A' && c<='Z')


long strtol(nptr, endptr, base)
const char *nptr;
char **endptr;
register int base;
{
	register const char *s = nptr;
	register unsigned long acc;
	register unsigned char c;
	register unsigned long cutoff;
	register int neg = 0, any, cutlim;

	/*
	 * Skip white space and pick up leading +/- sign if any.
	 * If base is 0, allow 0x for hex and 0 for octal, else
	 * assume decimal; if base is already 16, allow 0x.
	 */
	do {
		c = *s++;
	} while (isspace(c));
	if (c == '-') {
		neg = 1;
		c = *s++;
	} else if (c == '+')
		c = *s++;
	if ((base == 0 || base == 16) && c == '0' && (*s == 'x' || *s == 'X')) {
		c = s[1];
		s += 2;
		base = 16;
	}
	if (base == 0)
		base = c == '0' ? 8 : 10;

	/*
	 * Compute the cutoff value between legal numbers and illegal
	 * numbers.  That is the largest legal value, divided by the
	 * base.  An input number that is greater than this value, if
	 * followed by a legal input character, is too big.  One that
	 * is equal to this value may be valid or not; the limit
	 * between valid and invalid numbers is then based on the last
	 * digit.  For instance, if the range for longs is
	 * [-2147483648..2147483647] and the input base is 10,
	 * cutoff will be set to 214748364 and cutlim to either
	 * 7 (neg==0) or 8 (neg==1), meaning that if we have accumulated
	 * a value > 214748364, or equal but the next digit is > 7 (or 8),
	 * the number is too big, and we will return a range error.
	 *
	 * Set any if any `digits' consumed; make it negative to indicate
	 * overflow.
	 */
	cutoff = neg ? -(unsigned long) LONG_MIN : LONG_MAX;
	cutlim = cutoff % (unsigned long) base;
	cutoff /= (unsigned long) base;
	for (acc = 0, any = 0;; c = *s++) {
		if (!isascii(c))
			break;
		if (isdigit(c))
			c -= '0';
		else if (isalpha(c))
			c -= isupper(c) ? 'A' - 10 : 'a' - 10;
		else
			break;
		if (c >= base)
			break;
		if (any < 0 || acc > cutoff || (acc == cutoff && c > cutlim))
			any = -1;
		else {
			any = 1;
			acc *= base;
			acc += c;
		}
	}
	if (any < 0) {
		acc = neg ? LONG_MIN : LONG_MAX;
		/* range error */
	} else if (neg)
		acc = -acc;
	if (endptr != 0)
		*endptr = (char *) (any ? s - 1 : nptr);
	return (acc);
}

double strtod(const char *s, char **p)
{
	sys_panic("strtod not available");
}

size_t strlen(const char *string)
{
	const char *ret = string;
	while (*string++);
	return string - 1 - ret;
}

char *strcpy(char *to, const char *from)
{
	register char *ret = to;

	while ((*to++ = *from++) != 0);

	return ret;
}


char *strcat(char *s, const char *add)
{
	register char *ret = s;

	while (*s)
		s++;

	while ((*s++ = *add++) != 0);

	return ret;
}







char *getenv(const char *name)
{
	return NULL;
}

#ifndef NO_PRINTF
int printf(const char *fmt, ...)
{
	va_list args;
	int err;

	DISABLE_IRQ;

	va_start(args, fmt);
	err = vprintf(fmt, args);
	va_end(args);

	RESTORE_IRQ;

	return err;
}
#endif

int sprintf(char *str, char const *fmt, ...)
{
	sys_panic("sprintf not implemented.");
}

extern int do_sampling;
int strcmp(const char *s1, const char *s2)
{
#ifdef SAMPLE_FASTPATH
	if (do_sampling)
		printStackTrace("SLOWOPERATION-STRCMP ", curthr(), &s1 - 2);
#endif
	if (s1 == s2)
		return 0;
	while (*s1 == *s2++)
		if (*s1++ == 0)
			return (0);
	return (*(const unsigned char *) s1 - *(const unsigned char *) (s2 - 1));
}

int strncmp(s1, s2, n)
register const char *s1, *s2;
register size_t n;
{

	if (n == 0)
		return (0);
	do {
		if (*s1 != *s2++)
			return (*(const unsigned char *) s1 - *(const unsigned char *) (s2 - 1));
		if (*s1++ == 0)
			break;
	} while (--n != 0);
	return (0);
}

char *strchr(const char *s, int c)
{
	while (1) {
		if (*s == c)
			return (char *) s;
		if (*s == 0)
			return 0;
		s++;
	}
}
char *strncpy(char *to, const char *from, size_t count)
{
	register char *ret = to;

	while (count > 0) {
		count--;
		if ((*to++ = *from++) == '\0')
			break;
	}

	while (count > 0) {
		count--;
		*to++ = '\0';
	}

	return ret;
}


static long no_idt[2] = { 0, 0 };

void pc_reset()
{
	asm volatile ("lidt %0"::"m" (no_idt));
	asm volatile ("sti");
	asm volatile ("int3");
}

void pc_reset0()
{
	unsigned char v;

	v = inb(0xcf9);
	v |= 2;
	outb(0xcf9, v);
	v |= 4;
	outb(0xcf9, v);

	//pc_reset0();
}


void exit(int code)
{
	printf("Exited with return code %d.\nRebooting.\n", code);

	pc_reset();

	for (;;);
}


void *memset(void *tov, int c, size_t len)
{
	register char *to = tov;

	while (len-- > 0)
		*to++ = c & 0xff;

	return tov;
}

/* length in words !*/
void *memset16(void *tov, int c, size_t len)
{
	register short *to = tov;

	while (len-- > 0)
		*to++ = c & 0xffff;

	return tov;
}

#if FAST_MEMCPY
void *memcpy(void *to, const void *from, size_t n)
{
	int d0, d1, d2;
	__asm__ __volatile__("rep ; movsl\n\t" "testb $2,%b4\n\t" "je 1f\n\t" "movsw\n" "1:\ttestb $1,%b4\n\t" "je 2f\n\t"
			     "movsb\n" "2:":"=&c"(d0), "=&D"(d1), "=&S"(d2)
			     :"0"(n / 4), "q"(n), "1"((long) to), "2"((long) from)
			     :"memory");
	return (to);
}
#else
void *memcpy(void *tov, const void *fromv, size_t length)
{
	register char *to = tov;
	register char *from = fromv;

	while (length-- > 0) {
		*to++ = *from++;
	}

	return tov;
}
#endif

#define FALSE 0
#define TRUE 1
#define _doprnt_truncates  FALSE
#define Ctod(c) ((c) - '0')
#define MAXBUF (sizeof(long int) * 8)	/* enough for binary */

static void printnum(u4_t u, int base, void (*putc) (), char *putc_arg)
{
	char buf[MAXBUF];	/* build number here */
	register char *p = &buf[MAXBUF - 1];
	static char digs[] = "0123456789abcdef";

	do {
		*p-- = digs[u % base];
		u /= base;
	} while (u != 0);

	while (++p != &buf[MAXBUF])
		(*putc) (putc_arg, *p);
}
void printstr(char *str, u4_t u, int base)
{
	char buf[MAXBUF];	/* build number here */
	register char *p = &buf[MAXBUF - 1];
	static char digs[] = "0123456789abcdef";

	do {
		*p-- = digs[u % base];
		u /= base;
	} while (u != 0);

	while (++p != &buf[MAXBUF])
		*str++ = *p;
	*str++ = '\0';
}


#ifndef INCLUDE_VPRINTF
int vprintf(const char *fmt, va_list args)
{
}

#else				/* INCLUDE_VPRINTF */
int vsprintf(char *buf, const char *fmt, va_list args)
{
}

int vprintf(const char *fmt, va_list args)
{
	int length;
	int prec;
	bool_t ladjust;
	char padc;
	long n;
	unsigned long u;
	int plus_sign;
	int sign_char;
	bool_t altfmt, truncate;
	int base;
	char c;
#ifdef DOPRNT_FLOATS
	int float_hack;
	char *p;
#endif
	int radix = 0;		/* default radix - for '%r' */
	void (*putc) () = ser_putc;	/* character output */
	char *putc_arg = NULL;	/* argument for putc */

#ifdef SMP
	static volatile unsigned long active = 0;	// make reentrant    
	unsigned int oldbit;
#endif

	if (debug_port == -1)
		return 0;
#ifdef LOG_PRINTF
	if (printf2mem)
		putc = mem_putc;
#endif
#ifdef SMP
	// make reentrant
	oldbit = 1;
	while (oldbit) {
		while (active);	//wait here
		oldbit = test_and_set_bit(0, &active);
	}
#endif

#ifdef DOPRNT_FLOATS
	/* Copy the fmt so we can modify it in immoral ways. */
	p = alloca(strlen(fmt) + 1);
	strcpy(p, fmt);
	fmt = p;
#endif

	while (*fmt != '\0') {
		if (*fmt != '%') {
			(*putc) (putc_arg, *fmt++);
			continue;
		}

		fmt++;

		length = 0;
		prec = -1;
		ladjust = FALSE;
		padc = ' ';
		plus_sign = 0;
		sign_char = 0;
		altfmt = FALSE;

		while (TRUE) {
			if (*fmt == '#') {
				altfmt = TRUE;
				fmt++;
			} else if (*fmt == '-') {
				ladjust = TRUE;
				fmt++;
			} else if (*fmt == '+') {
				plus_sign = '+';
				fmt++;
			} else if (*fmt == ' ') {
				if (plus_sign == 0)
					plus_sign = ' ';
				fmt++;
			} else
				break;
		}

		if (*fmt == '0') {
			padc = '0';
			fmt++;
		}

		if (isdigit(*fmt)) {
			while (isdigit(*fmt))
				length = 10 * length + Ctod(*fmt++);
		} else if (*fmt == '*') {
			length = va_arg(args, int);
			fmt++;
			if (length < 0) {
				ladjust = !ladjust;
				length = -length;
			}
		}

		if (*fmt == '.') {
			fmt++;
			if (isdigit(*fmt)) {
				prec = 0;
				while (isdigit(*fmt))
					prec = 10 * prec + Ctod(*fmt++);
			} else if (*fmt == '*') {
				prec = va_arg(args, int);
				fmt++;
			}
		}

		if (*fmt == 'l')
			fmt++;	/* need it if sizeof(int) < sizeof(long) */

		truncate = FALSE;
#ifdef DOPRNT_FLOATS
		float_hack = FALSE;
#endif

		switch (*fmt) {
		case 'b':
		case 'B':
			{
				register char *p;
				bool_t any;
				register int i;

				u = va_arg(args, unsigned long);
				p = va_arg(args, char *);
				base = *p++;
				printnum(u, base, putc, putc_arg);

				if (u == 0)
					break;

				any = FALSE;
				while ((i = *p++) != 0) {
					/* NOTE: The '32' here is because ascii space */
					if (*p <= 32) {
						/*
						 * Bit field
						 */
						register int j;
						if (any)
							(*putc) (putc_arg, ',');
						else {
							(*putc) (putc_arg, '<');
							any = TRUE;
						}
						j = *p++;
						for (; (c = *p) > 32; p++)
							(*putc) (putc_arg, c);
						printnum((unsigned)
							 ((u >> (j - 1)) & ((2 << (i - j)) - 1)), base, putc, putc_arg);
					} else if (u & (1 << (i - 1))) {
						if (any)
							(*putc) (putc_arg, ',');
						else {
							(*putc) (putc_arg, '<');
							any = TRUE;
						}
						for (; (c = *p) > 32; p++)
							(*putc) (putc_arg, c);
					} else {
						for (; *p > 32; p++)
							continue;
					}
				}
				if (any)
					(*putc) (putc_arg, '>');
				break;
			}

		case 'c':
			c = va_arg(args, int);
			(*putc) (putc_arg, c);
			break;

		case 's':
			{
				register char *p;
				register char *p2;

				if (prec == -1)
					prec = 0x7fffffff;	/* MAXINT */

				p = va_arg(args, char *);

				if (p == (char *) 0)
					p = "";

				if (length > 0 && !ladjust) {
					n = 0;
					p2 = p;

					for (; *p != '\0' && n < prec; p++)
						n++;

					p = p2;

					while (n < length) {
						(*putc) (putc_arg, ' ');
						n++;
					}
				}

				n = 0;

				while (*p != '\0') {
					if (++n > prec)
						break;

					(*putc) (putc_arg, *p++);
				}

				if (n < length && ladjust) {
					while (n < length) {
						(*putc) (putc_arg, ' ');
						n++;
					}
				}

				break;
			}

		case 'o':
			truncate = _doprnt_truncates;
		case 'O':
			base = 8;
			goto print_unsigned;

		case 'd':
		case 'i':
			truncate = _doprnt_truncates;
		case 'D':
			base = 10;
			goto print_signed;

#ifdef DOPRNT_FLOATS
			/*
			 * This is horrible and needs to be redone sometime.
			 */
		case 'f':
		case 'F':
		case 'g':
		case 'G':
		case 'e':
		case 'E':
			truncate = _doprnt_truncates;
			base = 10;
			{
				static int first = 1;
				static int oldprec = 6;
				static char oldchar;
				double x;
				int i;

				/* we don't do left adjustment, sorry */
				ladjust = 0;

				if (first) {
					double dd = 1;

					x = va_arg(args, double);
					n = (int) x;
					sign_char = n >= 0 ? plus_sign : '-';
					u = n >= 0 ? n : -n;

					/* default prec is six digits */
					if (prec == -1)
						prec = 6;
					oldprec = prec;

					length -= (prec + 1);

					/* rewind args and fmt */
					args -= __va_size(double);

					/* uh-oh! we write the format string... */
					/* But now it's in an array we allocated.  */
					oldchar = fmt[-1];
					fmt[-1] = '%';
					fmt -= 2;

					/* stick fraction on arg */
					/* compute multiplier */
					for (i = 0; i < prec; i++)
						dd *= 10.0;

					x -= (double) ((int) x);
					x *= dd;
					*((double *) args) = x;

				} else {
					/* restore old stuff */
					fmt[-1] = oldchar;

					x = va_arg(args, double);
					n = (int) x;
					sign_char = '.';
					u = n >= 0 ? n : -n;;

					length = oldprec + 1;	/* Need to account for the . */
					float_hack = 1;
					ladjust = 0;
					padc = '0';
				}
				first = !first;
			}
			goto print_num;
#endif				/* DOPRNT_FLOATS */

		case 'u':
			truncate = _doprnt_truncates;
		case 'U':
			base = 10;
			goto print_unsigned;

		case 'p':
			padc = '0';
			length = 8;
			/* 
			 * We do this instead of just setting altfmt to TRUE
			 * because we want 0 to have a 0x in front, and we want
			 * eight digits after the 0x -- not just 6.
			 */
			(*putc) (putc_arg, '0');
			(*putc) (putc_arg, 'x');
		case 'x':
			truncate = _doprnt_truncates;
		case 'X':
			base = 16;
			goto print_unsigned;

		case 'z':
			truncate = _doprnt_truncates;
		case 'Z':
			base = 16;
			goto print_signed;

		case 'r':
			truncate = _doprnt_truncates;
		case 'R':
			base = radix;
			goto print_signed;

		case 'n':
			truncate = _doprnt_truncates;
		case 'N':
			base = radix;
			goto print_unsigned;

		      print_signed:
			n = va_arg(args, long);
			if (n >= 0) {
				u = n;
				sign_char = plus_sign;
			} else {
				u = -n;
				sign_char = '-';
			}
			goto print_num;

		      print_unsigned:
			u = va_arg(args, unsigned long);
			goto print_num;

		      print_num:
			{
				char buf[MAXBUF];	/* build number here */
				register char *p = &buf[MAXBUF - 1];
				static char digits[] = "0123456789abcdef";
				char *prefix = 0;

				if (truncate)
					u = (long) ((int) (u));

				if (u != 0 && altfmt) {
					if (base == 8)
						prefix = "0";
					else if (base == 16)
						prefix = "0x";
				}

				do {
					*p-- = digits[u % base];
					u /= base;
					prec--;
				} while (u != 0);

				length -= (&buf[MAXBUF - 1] - p);
				if (sign_char)
					length--;
				if (prefix)
					length -= strlen(prefix);

				if (prec > 0)
					length -= prec;
				if (padc == ' ' && !ladjust) {
					/* blank padding goes before prefix */
					while (--length >= 0)
						(*putc) (putc_arg, ' ');
				}
				if (sign_char)
					(*putc) (putc_arg, sign_char);
				if (prefix)
					while (*prefix)
						(*putc) (putc_arg, *prefix++);

				while (--prec >= 0)
					(*putc) (putc_arg, '0');

				if (padc == '0') {
					/* zero padding goes after sign and prefix */
					while (--length >= 0)
						(*putc) (putc_arg, '0');
				}
				while (++p != &buf[MAXBUF])
					(*putc) (putc_arg, *p);

#ifdef DOPRNT_FLOATS
				if (ladjust) {
					while (--length >= 0)
						(*putc) (putc_arg, float_hack ? '0' : ' ');
				}
				float_hack = 0;
#else
				if (ladjust) {
					while (--length >= 0)
						(*putc) (putc_arg, ' ');
				}
#endif
				break;
			}

		case '\0':
			fmt--;
			break;

		default:
			(*putc) (putc_arg, *fmt);
		}
		fmt++;
	}

#ifdef SMP
	active = 0;
#endif
}
#endif				/* PRODUCTION */

int snprintf(char *str, int n, const char *fmt, ...)
{
	sys_panic("not implemented");
}

long atol(const char *str)
{
	long n = 0;
	while (isdigit(*str)) {
		n = (n * 10) + *str - '0';
		str++;
	}
	return n;
}
void panic(const char *__format, ...)
{
	sys_panic("");
}

int isdigit(int d)
{
	return ((d) >= '0' && (d) <= '9');
}

int getchar(void)
{
	return -1;
}

char *strtok(s, delim)
register char *s;
register const char *delim;
{
	register char *spanp;
	register int c, sc;
	char *tok;
	static char *last;


	if (s == NULL && (s = last) == NULL)
		return (NULL);

	/*
	 * Skip (span) leading delimiters (s += strspn(s, delim), sort of).
	 */
      cont:
	c = *s++;
	for (spanp = (char *) delim; (sc = *spanp++) != 0;) {
		if (c == sc)
			goto cont;
	}

	if (c == 0) {		/* no non-delimiter characters */
		last = NULL;
		return (NULL);
	}
	tok = s - 1;

	/*
	 * Scan token (scan for delimiters: s += strcspn(s, delim), sort of).
	 * Note that delim must have one NUL; we stop if we see that, too.
	 */
	for (;;) {
		c = *s++;
		spanp = (char *) delim;
		do {
			if ((sc = *spanp++) == c) {
				if (c == 0)
					s = NULL;
				else
					s[-1] = 0;
				last = s;
				return (tok);
			}
		} while (sc != 0);
	}
	/* NOTREACHED */
}


char **environ;

unsigned long strtoul(const char *p, char **out_p, int base)
{
	unsigned long v = 0;

	while (isspace(*p))
		p++;
	if (((base == 16) || (base == 0)) && ((*p == '0') && ((p[1] == 'x') || (p[1] == 'X')))) {
		p += 2;
		base = 16;
	}
	if (base == 0) {
		if (*p == '0')
			base = 8;
		else
			base = 10;
	}
	while (1) {
		char c = *p;
		if ((c >= '0') && (c <= '9') && (c - '0' < base))
			v = (v * base) + (c - '0');
		else if ((c >= 'a') && (c <= 'z') && (c - 'a' + 10 < base))
			v = (v * base) + (c - 'a' + 10);
		else if ((c >= 'A') && (c <= 'Z') && (c - 'A' + 10 < base))
			v = (v * base) + (c - 'A' + 10);
		else
			break;
		p++;
	}

	if (out_p)
		*out_p = (char *) p;
	return v;
}

int isspace(int c)
{
	return ((c) == ' ') || ((c) == '\f')
	    || ((c) == '\n') || ((c) == '\r')
	    || ((c) == '\t') || ((c) == '\v');
}









static inline char *med3(char *, char *, char *, int (*)());
static inline void swapfunc(char *, char *, int, int);

#define min(a, b)	(a) < (b) ? a : b

/*
 * Qsort routine from Bentley & McIlroy's "Engineering a Sort Function".
 */
#define swapcode(TYPE, parmi, parmj, n) { 		\
	long i = (n) / sizeof (TYPE); 			\
	register TYPE *pi = (TYPE *) (parmi); 		\
	register TYPE *pj = (TYPE *) (parmj); 		\
	do { 						\
		register TYPE	t = *pi;		\
		*pi++ = *pj;				\
		*pj++ = t;				\
        } while (--i > 0);				\
}

#define SWAPINIT(a, es) swaptype = ((char *)a - (char *)0) % sizeof(long) || \
	es % sizeof(long) ? 2 : es == sizeof(long)? 0 : 1;

static inline void swapfunc(a, b, n, swaptype)
char *a, *b;
int n, swaptype;
{
	if (swaptype <= 1) {
		swapcode(long, a, b, n);
	} else {
		swapcode(char, a, b, n);
	}
}

#define swap(a, b)					\
	if (swaptype == 0) {				\
		long t = *(long *)(a);			\
		*(long *)(a) = *(long *)(b);		\
		*(long *)(b) = t;			\
	} else						\
		swapfunc(a, b, es, swaptype)

#define vecswap(a, b, n) 	if ((n) > 0) swapfunc(a, b, n, swaptype)

static inline char *med3(a, b, c, cmp)
char *a, *b, *c;
int (*cmp) ();
{
	return cmp(a, b) < 0 ? (cmp(b, c) < 0 ? b : (cmp(a, c) < 0 ? c : a))
	    : (cmp(b, c) > 0 ? b : (cmp(a, c) < 0 ? a : c));
}












/*
 * Freebsd C lib: multiprecision
 */

/*****
 * multiprecision arithmetic
 * from FreeBSD
 */
#ifndef FREEBSD_C
typedef long long quad_t;
typedef unsigned long long u_quad_t;


union uu {
	quad_t q;		/* as a (signed) quad */
	quad_t uq;		/* as an unsigned quad */
	long sl[2];		/* as two signed longs */
	u4_t ul[2];		/* as two unsigned longs */
};

/*
 * Define high and low longwords.
 */
#define	H		_QUAD_HIGHWORD
#define	L		_QUAD_LOWWORD

#define _QUAD_HIGHWORD 1
#define _QUAD_LOWWORD 0

/*
 * Total number of bits in a quad_t and in the pieces that make it up.
 * These are used for shifting, and also below for halfword extraction
 * and assembly.
 */
#define CHAR_BIT 8
#define	QUAD_BITS	(sizeof(quad_t) * CHAR_BIT)
#define	LONG_BITS	(sizeof(long) * CHAR_BIT)
#define	HALF_BITS	(sizeof(long) * CHAR_BIT / 2)

/*
 * Extract high and low shortwords from longword, and move low shortword of
 * longword to upper half of long, i.e., produce the upper longword of
 * ((quad_t)(x) << (number_of_bits_in_long/2)).  (`x' must actually be u4_t.)
 *
 * These are used in the multiply code, to split a longword into upper
 * and lower halves, and to reassemble a product as a quad_t, shifted left
 * (sizeof(long)*CHAR_BIT/2).
 */
#define	HHALF(x)	((x) >> HALF_BITS)
#define	LHALF(x)	((x) & ((1 << HALF_BITS) - 1))
#define	LHUP(x)		((x) << HALF_BITS)

quad_t __divdi3(quad_t a, quad_t b);
quad_t __moddi3(quad_t a, quad_t b);
u_quad_t __qdivrem(u_quad_t u, u_quad_t v, u_quad_t * rem);
u_quad_t __udivdi3(u_quad_t a, u_quad_t b);
u_quad_t __umoddi3(u_quad_t a, u_quad_t b);

typedef unsigned int qshift_t;



/*
 * Multiprecision divide.  This algorithm is from Knuth vol. 2 (2nd ed),
 * section 4.3.1, pp. 257--259.
 */

#define	B	(1 << HALF_BITS)	/* digit base */

/* Combine two `digits' to make a single two-digit number. */
#define	COMBINE(a, b) (((u4_t)(a) << HALF_BITS) | (b))

/* select a type for digits in base B: use unsigned short if they fit */
#if ULONG_MAX == 0xffffffff && USHRT_MAX >= 0xffff
typedef unsigned short digit;
#else
typedef u4_t digit;
#endif

/*
 * Shift p[0]..p[len] left `sh' bits, ignoring any bits that
 * `fall out' the left (there never will be any such anyway).
 * We may assume len >= 0.  NOTE THAT THIS WRITES len+1 DIGITS.
 */
static void shl(register digit * p, register int len, register int sh)
{
	register int i;

	for (i = 0; i < len; i++)
		p[i] = LHALF(p[i] << sh) | (p[i + 1] >> (HALF_BITS - sh));
	p[i] = LHALF(p[i] << sh);
}

/*
 * __qdivrem(u, v, rem) returns u/v and, optionally, sets *rem to u%v.
 *
 * We do this in base 2-sup-HALF_BITS, so that all intermediate products
 * fit within u4_t.  As a consequence, the maximum length dividend and
 * divisor are 4 `digits' in this base (they are shorter if they have
 * leading zeros).
 */
u_quad_t __qdivrem(uq, vq, arq)
u_quad_t uq, vq, *arq;
{
	union uu tmp;
	digit *u, *v, *q;
	register digit v1, v2;
	u4_t qhat, rhat, t;
	int m, n, d, j, i;
	digit uspace[5], vspace[5], qspace[5];

	/*
	 * Take care of special cases: divide by zero, and u < v.
	 */
	if (vq == 0) {
		/* divide by zero. */
		static volatile const unsigned int zero = 0;

		tmp.ul[H] = tmp.ul[L] = 1 / zero;
		if (arq)
			*arq = uq;
		return (tmp.q);
	}
	if (uq < vq) {
		if (arq)
			*arq = uq;
		return (0);
	}
	u = &uspace[0];
	v = &vspace[0];
	q = &qspace[0];

	/*
	 * Break dividend and divisor into digits in base B, then
	 * count leading zeros to determine m and n.  When done, we
	 * will have:
	 *      u = (u[1]u[2]...u[m+n]) sub B
	 *      v = (v[1]v[2]...v[n]) sub B
	 *      v[1] != 0
	 *      1 < n <= 4 (if n = 1, we use a different division algorithm)
	 *      m >= 0 (otherwise u < v, which we already checked)
	 *      m + n = 4
	 * and thus
	 *      m = 4 - n <= 2
	 */
	tmp.uq = uq;
	u[0] = 0;
	u[1] = HHALF(tmp.ul[H]);
	u[2] = LHALF(tmp.ul[H]);
	u[3] = HHALF(tmp.ul[L]);
	u[4] = LHALF(tmp.ul[L]);
	tmp.uq = vq;
	v[1] = HHALF(tmp.ul[H]);
	v[2] = LHALF(tmp.ul[H]);
	v[3] = HHALF(tmp.ul[L]);
	v[4] = LHALF(tmp.ul[L]);
	for (n = 4; v[1] == 0; v++) {
		if (--n == 1) {
			u4_t rbj;	/* r*B+u[j] (not root boy jim) */
			digit q1, q2, q3, q4;

			/*
			 * Change of plan, per exercise 16.
			 *      r = 0;
			 *      for j = 1..4:
			 *              q[j] = floor((r*B + u[j]) / v),
			 *              r = (r*B + u[j]) % v;
			 * We unroll this completely here.
			 */
			t = v[2];	/* nonzero, by definition */
			q1 = u[1] / t;
			rbj = COMBINE(u[1] % t, u[2]);
			q2 = rbj / t;
			rbj = COMBINE(rbj % t, u[3]);
			q3 = rbj / t;
			rbj = COMBINE(rbj % t, u[4]);
			q4 = rbj / t;
			if (arq)
				*arq = rbj % t;
			tmp.ul[H] = COMBINE(q1, q2);
			tmp.ul[L] = COMBINE(q3, q4);
			return (tmp.q);
		}
	}

	/*
	 * By adjusting q once we determine m, we can guarantee that
	 * there is a complete four-digit quotient at &qspace[1] when
	 * we finally stop.
	 */
	for (m = 4 - n; u[1] == 0; u++)
		m--;
	for (i = 4 - m; --i >= 0;)
		q[i] = 0;
	q += 4 - m;

	/*
	 * Here we run Program D, translated from MIX to C and acquiring
	 * a few minor changes.
	 *
	 * D1: choose multiplier 1 << d to ensure v[1] >= B/2.
	 */
	d = 0;
	for (t = v[1]; t < B / 2; t <<= 1)
		d++;
	if (d > 0) {
		shl(&u[0], m + n, d);	/* u <<= d */
		shl(&v[1], n - 1, d);	/* v <<= d */
	}
	/*
	 * D2: j = 0.
	 */
	j = 0;
	v1 = v[1];		/* for D3 -- note that v[1..n] are constant */
	v2 = v[2];		/* for D3 */
	do {
		register digit uj0, uj1, uj2;

		/*
		 * D3: Calculate qhat (\^q, in TeX notation).
		 * Let qhat = min((u[j]*B + u[j+1])/v[1], B-1), and
		 * let rhat = (u[j]*B + u[j+1]) mod v[1].
		 * While rhat < B and v[2]*qhat > rhat*B+u[j+2],
		 * decrement qhat and increase rhat correspondingly.
		 * Note that if rhat >= B, v[2]*qhat < rhat*B.
		 */
		uj0 = u[j + 0];	/* for D3 only -- note that u[j+...] change */
		uj1 = u[j + 1];	/* for D3 only */
		uj2 = u[j + 2];	/* for D3 only */
		if (uj0 == v1) {
			qhat = B;
			rhat = uj1;
			goto qhat_too_big;
		} else {
			u4_t n = COMBINE(uj0, uj1);
			qhat = n / v1;
			rhat = n % v1;
		}
		while (v2 * qhat > COMBINE(rhat, uj2)) {
		      qhat_too_big:
			qhat--;
			if ((rhat += v1) >= B)
				break;
		}
		/*
		 * D4: Multiply and subtract.
		 * The variable `t' holds any borrows across the loop.
		 * We split this up so that we do not require v[0] = 0,
		 * and to eliminate a final special case.
		 */
		for (t = 0, i = n; i > 0; i--) {
			t = u[i + j] - v[i] * qhat - t;
			u[i + j] = LHALF(t);
			t = (B - HHALF(t)) & (B - 1);
		}
		t = u[j] - t;
		u[j] = LHALF(t);
		/*
		 * D5: test remainder.
		 * There is a borrow if and only if HHALF(t) is nonzero;
		 * in that (rare) case, qhat was too large (by exactly 1).
		 * Fix it by adding v[1..n] to u[j..j+n].
		 */
		if (HHALF(t)) {
			qhat--;
			for (t = 0, i = n; i > 0; i--) {	/* D6: add back. */
				t += u[i + j] + v[i];
				u[i + j] = LHALF(t);
				t = HHALF(t);
			}
			u[j] = LHALF(u[j] + t);
		}
		q[j] = qhat;
	} while (++j <= m);	/* D7: loop on j. */

	/*
	 * If caller wants the remainder, we have to calculate it as
	 * u[m..m+n] >> d (this is at most n digits and thus fits in
	 * u[m+1..m+n], but we may need more source digits).
	 */
	if (arq) {
		if (d) {
			for (i = m + n; i > m; --i)
				u[i] = (u[i] >> d) | LHALF(u[i - 1] << (HALF_BITS - d));
			u[i] = 0;
		}
		tmp.ul[H] = COMBINE(uspace[1], uspace[2]);
		tmp.ul[L] = COMBINE(uspace[3], uspace[4]);
		*arq = tmp.q;
	}

	tmp.ul[H] = COMBINE(qspace[1], qspace[2]);
	tmp.ul[L] = COMBINE(qspace[3], qspace[4]);
	return (tmp.q);
}

quad_t __divdi3(quad_t a, quad_t b)
{
	u_quad_t ua, ub, uq;
	int neg;

	if (a < 0)
		ua = -(u_quad_t) a, neg = 1;
	else
		ua = a, neg = 0;
	if (b < 0)
		ub = -(u_quad_t) b, neg ^= 1;
	else
		ub = b;
	uq = __qdivrem(ua, ub, (u_quad_t *) 0);
	return (neg ? -uq : uq);
}


u_quad_t __udivdi3(u_quad_t a, u_quad_t b)
{

	return __qdivrem(a, b, (u_quad_t *) 0);
}

quad_t __moddi3(quad_t a, quad_t b)
{
	u_quad_t ua, ub, ur;
	int neg;

	if (a < 0)
		ua = -(u_quad_t) a, neg = 1;
	else
		ua = a, neg = 0;
	if (b < 0)
		ub = -(u_quad_t) b, neg ^= 1;
	else
		ub = b;
	(void) __qdivrem(ua, ub, &ur);
	return (neg ? -ur : ur);
}

void libc_sendsig_init()
{
}

/* A PART OF THIS FILE IS COVERED BY THE FOLLOWING LICENSE: */

/*
 * Copyright (c) 1988, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */


#endif				/* FREEBSD_C */
