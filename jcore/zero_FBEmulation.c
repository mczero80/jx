/********************************************************************************
 * DomainZero Framebuffer Emulation
 * Copyright 1998-2002 Michael Golm
 *******************************************************************************/

#ifdef FRAMEBUFFER_EMULATION




/***************************************************************************/
/***************************************************************************/
/***************************************************************************/

#ifdef KERNEL

#include "all.h"

#include "realmode.h"

#define REALMODE_MODULE "realmode"

struct vesa_info {
	char signature[4];	/* 'VESA' */
	u2_t version;
	u4_t oemPtr;
	u1_t capabilities[4];
	u4_t modePtr;		/* modelist is terminated with 0xffff */
	u1_t reserved[238];
} __attribute__ ((packed));

/* The VESA mode info structure. */
struct vesa_mode_info {
	u2_t mode_attributes;
	u1_t window_attributes_a;
	u1_t window_attributes_b;
	u2_t window_granularity;
	u2_t window_size;
	u2_t window_a_segment;
	u2_t window_b_segment;
	u4_t window_posititioning_function;
	u2_t bytes_per_scan_line;
	u2_t width_in_pixels;
	u2_t height_in_pixels;
	u1_t character_cell_width;
	u1_t character_cell_height;
	u1_t memory_planes;
	u1_t bits_per_pixel;
	u1_t banks;
	u1_t memory_model_type;
	u1_t memory_bank_size;
	u1_t image_pages;
	u1_t reserved;
	u1_t red_mask_size;
	u1_t red_field_position;
	u1_t green_mask_size;
	u1_t green_field_size;
	u1_t blue_mask_size;
	u1_t blue_field_size;
	u1_t reserved_mask_size;
	u1_t reserved_mask_position;
	u1_t direct_color_mode_info;
	u4_t linear_frame_buffer_address;
	u4_t offscreen_memory_address;
	u2_t offscreen_memory_size;
	u1_t reserved2[206];
} __attribute__ ((packed));

static u4_t vesaModes[30];
static u4_t vesaModesWidth[30];
static int numberModes = 0;

#define SEG2LIN(x) ((((u4_t)(x))/ 65536) * 16 + (((u4_t)(x)) % 65536))

typedef int (*vesa2_get_mode_info_t) (int mode, struct vesa_mode_info * vesa_mode_info);
vesa2_get_mode_info_t extern_vesa2_get_mode_info = (vesa2_get_mode_info_t) FKTADDR_vesa2_get_mode_info;

typedef int (*vesa2_set_mode_t) (int mode);
vesa2_set_mode_t extern_vesa2_set_mode = (vesa2_set_mode_t) FKTADDR_vesa2_set_mode;

typedef int (*vesa2_detect_t) (void);
vesa2_detect_t extern_vesa2_detect = (vesa2_detect_t) FKTADDR_vesa2_detect;

//#define DBG_VESA 1
//#define TEST_FRAMEBUFFER 1

#define REALCODE   0x9000
#define STACKOFF   0x8000
#define SCRATCHMEM 0x7000
#define OTHERMEM   0x6000

/* end of first 64kB is 0x10000 */

static struct vesa_info *info = (struct vesa_info *) SCRATCHMEM;
static struct vesa_mode_info *modeinfo = (struct vesa_mode_info *) OTHERMEM;


static u1_t *framebuffer = NULL;
static jint width, height, bytesPerLine, bitsPerPixel;

struct pseudo_descriptor {
	short pad;
	u2_t limit;
	unsigned long linear_base;
};
#define INITPIC 1
jboolean fbemulation_open(ObjectDesc * self, int mode)
{
	jboolean status = JNI_FALSE;
	int i;
	char modeStr[80];

	DISABLE_IRQ;

	printstr(modeStr, mode, 10);

	console(1, "SET VESA MODE:");
	console(2, modeStr);

#ifdef DBG_VESA
	printf("Init VESA mode %x\n", mode);
#endif

#ifdef INITPIC
	pic_init_rmode();
#endif
	/* Load the real-mode IDT.  */
	{
		struct pseudo_descriptor pdesc;

		pdesc.limit = 0xffff;
		pdesc.linear_base = 0;
		set_idt(&pdesc);
	}
      retry:
	if (extern_vesa2_set_mode(mode | 0x4000) != 0) {
		printf("FAILED TO SET VESA MODE\n");
		console(0, "FAILED TO SET VESA MODE");
		/* try to find supported mode */
		if (numberModes > 0) {
			mode = vesaModes[0];
			goto retry;
		}
		goto finished;
	}

	if (extern_vesa2_get_mode_info((mode | 0x4000), modeinfo) != 0) {
		console(0, "FAILED TO GET VESA MODE INFO");
		goto finished;
	}

	framebuffer = (u1_t *) modeinfo->linear_frame_buffer_address;
	width = modeinfo->width_in_pixels;
	height = modeinfo->height_in_pixels;
	bytesPerLine = modeinfo->bytes_per_scan_line;
	bitsPerPixel = modeinfo->bits_per_pixel;
	status = JNI_TRUE;

	printf("VESA: %ldx%ld, %ld bytesPerLine, %ld bitsPerPixel\n", width, height, bytesPerLine, bitsPerPixel);
#ifdef TEST_FRAMEBUFFER
	for (i = 0; i < 0xff; i += 10) {
		jxmemset(framebuffer, i, bytesPerLine * height);
	}
#endif

      finished:
	idt_load();
#ifdef INITPIC
	pic_init_pmode();	/* config interrupt controller */
#endif

	RESTORE_IRQ;

	ASSERTSTI;

	printf("return from fbemulation_open\n");

	return status;
}

u4_t fbemulation_getVideoMemory(ObjectDesc * self)
{
	if (framebuffer == NULL)
		return NULL;
	return memoryManager_allocDeviceMemory(NULL, framebuffer, bytesPerLine * height);
}

jint fbemulation_getWidth(ObjectDesc * self)
{
	return width;
}

jint fbemulation_getHeight(ObjectDesc * self)
{
	return height;
}

jint fbemulation_getBytesPerLine(ObjectDesc * self)
{
	return bytesPerLine;
}

jint fbemulation_getBitsPerPixel(ObjectDesc * self)
{
	return bitsPerPixel;
}

void fbemulation_update(ObjectDesc * self)
{
}

jboolean fbemulation_inputDevicesAvailable(ObjectDesc * self)
{
	return JNI_FALSE;
}

jboolean fbemulation_checkEvent(ObjectDesc * self, ObjectDesc * event)
{
	return JNI_FALSE;
}





void init_realmode()
{
	zipentry entry;
	int ret;
	u2_t mode;
	zip_reset();
	while ((ret = zip_next_entry(&entry)) != -1) {
		if (strcmp(entry.filename, REALMODE_MODULE) == 0) {
			memcpy(REALCODE, entry.data + 0x1000, entry.uncompressed_size - 0x1000);
			break;
		}
	}
	if (ret == -1) {
		sys_panic("no realmode module found in zipfile");
	}

	DISABLE_IRQ;

	pic_init_rmode();

	/* Load the real-mode IDT.  */
	{
		struct pseudo_descriptor pdesc;

		pdesc.limit = 0xffff;
		pdesc.linear_base = 0;
		set_idt(&pdesc);
	}


	{
		u2_t *list;
		u2_t *modes = jxmalloc(sizeof(u2_t) * 100 MEMTYPE_OTHER);
		int nmodes;
		int i, j;
		int ret;

		ret = extern_vesa2_detect();
		if (ret != 0) {
			console(0, "VESA NOT SUPPORTED");
			sys_panic("VESA NOT SUPPORTED");
		}
#ifdef DBG_VESA
		printf("HAVE VESA: ret=%d %c %c %c %c\n", ret, info->signature[0], info->signature[1], info->signature[2],
		       info->signature[3]);
		printf("   VESA-Version: 0x%x\n", info->version);
		printf("   Mode-Ptr: seg=%p lin=%p\n", info->modePtr, SEG2LIN(info->modePtr));
		printf("   OEM-Ptr: seg=%p lin=%p\n", info->oemPtr, SEG2LIN(info->oemPtr));
#endif
		list = (u2_t *) SEG2LIN(info->modePtr);

		for (i = 0; list[i] != 0xffff; i++) {
#ifdef DBG_VESA
			printf("   Mode[%d] %p\n", i, list[i]);
#endif
			modes[i] = list[i];
		}
		nmodes = i;

		for (i = 0; i < nmodes; i++) {
#ifdef DBG_VESA
			printf("   Mode[%d] %p\n", i, modes[i]);
#endif
			ret = extern_vesa2_get_mode_info(modes[i] | (1 << 14), modeinfo);
#ifdef DBG_VESA
			printf("      ret=%d\n", ret);
			for (j = 0; j < sizeof(struct vesa_mode_info); j++) {
				printf("%02x", ((u1_t *) modeinfo /*OTHERMEM*/)[j] & 0xff);
			}
			printf("\n");
			printf("      %dx%d\n", modeinfo->width_in_pixels, modeinfo->height_in_pixels);
			printf("      depth=%d\n", modeinfo->bits_per_pixel);
#endif
			if (modeinfo->mode_attributes & 0x80) {
#ifdef DBG_VESA
				printf("      Info.linear_frame_buffer_address %p\n", modeinfo->linear_frame_buffer_address);
#endif
				if (modeinfo->bits_per_pixel == 16 /*|| modeinfo->bits_per_pixel == 32 */ ) {
					if (numberModes < 30) {
						if (modeinfo->width_in_pixels > vesaModesWidth[0]) {	/* use highest resolution */
							vesaModes[numberModes] = vesaModes[0];
							vesaModesWidth[numberModes] = vesaModesWidth[0];
							vesaModes[0] = modes[i];
							vesaModesWidth[0] = modeinfo->width_in_pixels;
						} else {
							vesaModes[numberModes] = modes[i];
							vesaModesWidth[numberModes] = modeinfo->width_in_pixels;
						}
						numberModes++;
					}
				}
			} else {
#ifdef DBG_VESA
				printf("      Linear framebuffer not supported attributes=0x%x\n", modeinfo->mode_attributes);
#endif
			}
		}

	}

	idt_load();
	pic_init_pmode();	/* config interrupt controller */

	RESTORE_IRQ;

	printf("OK\n");
}





MethodInfoDesc fbemulationMethods[] = {
	{"open", "", fbemulation_open}
	,
	{"getVideoMemory", "", fbemulation_getVideoMemory}
	,
	{"getWidth", "", fbemulation_getWidth}
	,
	{"getHeight", "", fbemulation_getHeight}
	,
	{"getBytesPerLine", "", fbemulation_getBytesPerLine}
	,
	{"getBitsPerPixel", "", fbemulation_getBitsPerPixel}
	,
	{"update", "", fbemulation_update}
	,
	{"inputDevicesAvailable", "", fbemulation_inputDevicesAvailable}
	,
	{"checkEvent", "", fbemulation_checkEvent}
	,
};

void init_framebuffer_emulation(void)
{
}

void init_fbemulation_portal()
{
	init_zero_dep("jx/zero/FBEmulation", "FBEmulation", fbemulationMethods, sizeof(fbemulationMethods),
		      "<jx/zero/FBEmulation>");
}



/***************************************************************************/
/***************************************************************************/
/***************************************************************************/

#else				/* KERNEL */

#include <sys/ipc.h>
#include <sys/shm.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XShm.h>

#include <stdlib.h>
#include <stdio.h>

#include "all.h"

#ifdef FRAMEBUFFER_EMULATION_ENFORCE_SHM
static int use_shm = 1;
#else
static int use_shm = 0;
#endif

static int mitshm_major_code;
static int mitshm_minor_code;

static int mitshm_handler(Display * d, XErrorEvent * ev)
{
	sys_panic("shm error");
}

#define WIN_W	800
#define WIN_H	600

static Display *d;
static Window win;
static GC gc;
static XImage *img = NULL;
static XVisualInfo vis;
static int screen;
static XShmSegmentInfo shminfo;
int display_fd;

static char *noshm_mem = NULL;
static u4_t noshm_mem_size = 0;

void draw_pixel(int x, int y, int color)
{
	char *a = shminfo.shmaddr;
	a[x * (img->bits_per_pixel >> 3) + y * img->bytes_per_line] = 0xff;
}

/* Portal */

/*
 * FBEmulationDEP
 */
jboolean fbemulation_open(ObjectDesc * self)
{
	return open_framebuffer();
}

u4_t fbemulation_getVideoMemory(ObjectDesc * self)
{
	if (use_shm) {
		return memoryManager_allocDeviceMemory(NULL, shminfo.shmaddr, img->bytes_per_line * img->height);
	} else {
		return memoryManager_allocDeviceMemory(NULL, noshm_mem, noshm_mem_size);
	}
}

jint fbemulation_getWidth(ObjectDesc * self)
{
	return WIN_W;
}

jint fbemulation_getHeight(ObjectDesc * self)
{
	return WIN_H;
}


jint fbemulation_getBytesPerLine(ObjectDesc * self)
{
	return img->bytes_per_line;
}

jint fbemulation_getBitsPerPixel(ObjectDesc * self)
{
	return img->bits_per_pixel;
}

void fbemulation_update(ObjectDesc * self)
{
	DISABLE_IRQ;
	if (!use_shm) {
		XPutImage(d, win, gc, img, 0, 0, 0, 0, WIN_W, WIN_H);
	} else {
		XShmPutImage(d, win, gc, img, 0, 0, 0, 0, WIN_W, WIN_H, True);
	}

	RESTORE_IRQ;
}

jboolean fbemulation_inputDevicesAvailable(ObjectDesc * self)
{
	return JNI_TRUE;
}

Bool pred(Display * display, XEvent * event, XPointer arg)
{
	printf("%d\n", event->type);
	if (event->type == MotionNotify)
		return True;
	if (event->type == Expose)
		return True;
	if (event->type == ButtonPress)
		return True;
	if (event->type == ButtonRelease)
		return True;
	if (event->type == KeyRelease)
		return True;
	if (event->type == KeyPress)
		return True;
	if (event->type == 84)
		return True;
	if (event->type > LASTEvent)
		sys_panic("");
	return False;
}

static int ecount = 0;

jboolean fbemulation_checkEvent(ObjectDesc * self, ObjectDesc * event)
{
	XEvent ev;
	int ret, n;
	if (event == NULL) {
		printf("warning: event is NULL\n");
		return JNI_FALSE;
	}
#if 0
	ret =
	    XCheckMaskEvent(d,
			    ButtonPressMask | ButtonReleaseMask | PointerMotionMask | KeyPressMask | KeyReleaseMask |
			    ExposureMask, &ev);
	/*printf(".");
	   fflush(stdout); */
	if (ret != True)
		return JNI_FALSE;
#endif
#if 1
	n = XEventsQueued(d, QueuedAfterFlush);
	/*XFlush(d);
	   n = XPending(d); */
	if (n == 0) {
		return JNI_FALSE;
	}
	//printf("---%d--\n", n);
	//ret = XCheckIfEvent(d, &ev, pred, NULL);
	XNextEvent(d, &ev);
	/*printf(".");
	   fflush(stdout);
	   if (ret != True)
	   return JNI_FALSE; */
#endif

#if 0
	{
		int res;
		fd_set readfds;
		struct timeval timeout;
		timeout.tv_sec = 0;
		timeout.tv_usec = 1000;	/* 1/1000 s */
		FD_ZERO(&readfds);
		FD_SET(display_fd, &readfds);

		while (XEventsQueued(d, QueuedAfterFlush) == 0) {
			res = select(display_fd + 1, &readfds, NULL, NULL, &timeout);

			switch (res) {
			case -1:	/* select() error - should not happen */
				perror("XPeekEventTimeout: select() failure");
				return (False);
			case 0:	/* timeout */
				return (False);
			}
		}
		printf("NEXT\n");

		XNextEvent(d, &ev);
		//   XPeekEvent(d, &ev); 

	}
#endif

	event->data[0] = 0;
	printf("EVENT%d %d %x\n", ecount++, ev.type, ev.type);
	switch (ev.type) {
	case KeyPress:
		event->data[0] = 1 << 0;
		event->data[3] = ev.xkey.keycode;
		break;
	case KeyRelease:
		event->data[0] = 1 << 1;
		event->data[3] = ev.xkey.keycode;
		break;
	case ButtonPress:
		event->data[0] = 1 << 2;
		event->data[1] = ev.xbutton.x;
		event->data[2] = ev.xbutton.y;
		event->data[4] = ev.xbutton.button;
		break;
	case ButtonRelease:
		event->data[0] = 1 << 3;
		event->data[1] = ev.xbutton.x;
		event->data[2] = ev.xbutton.y;
		event->data[4] = ev.xbutton.button;
		break;
	case MotionNotify:
		printf("MOTION\n");
		{
/*
			XEvent nev;
			while (XCheckMaskEvent(d, PointerMotionMask, &nev)
			       == True) {
				ev = nev;
			}
*/
			event->data[0] = 1 << 4;
			event->data[1] = ev.xmotion.x;
			event->data[2] = ev.xmotion.y;
			event->data[5] = 0;
			if (ev.xmotion.state & 256)
				event->data[5] |= 1 << 0;
			if (ev.xmotion.state & 512)
				event->data[5] |= 2 << 0;
			if (ev.xmotion.state & 1024)
				event->data[5] |= 3 << 0;
			if (ev.xmotion.state & 1)
				event->data[5] |= 4 << 0;
			//printf("STATE: %d\n", ev.xmotion.state);
			printf("X,Y: %d,%d\n", ev.xmotion.x, ev.xmotion.y);
			break;
		}
	case Expose:
		XShmPutImage(d, win, gc, img, 0, 0, 0, 0, WIN_W, WIN_H, True);
		return JNI_FALSE;	/* only internal */
	default:
		return JNI_FALSE;
	}
	return JNI_TRUE;
}




/******/

void init_framebuffer_emulation(void)
{
}

static jboolean open_framebuffer(void)
{
	XVisualInfo *vlist;
	int nitems;
	int i;
	int shared_pixmaps;	/* dummy */
	int (*handler) (Display *, XErrorEvent *);
	XEvent report;

	d = XOpenDisplay(NULL);

	if (!d)
		return JNI_FALSE;

	screen = DefaultScreen(d);
	gc = DefaultGC(d, screen);

	/* Find a visual */

	vis.screen = screen;
	vlist = XGetVisualInfo(d, VisualScreenMask, &vis, &nitems);

	//vis.depth = 16;
	//vlist = XGetVisualInfo(d, VisualDepthMask, &vis, &nitems);

	if (!vlist) {
		printf("No matched visuals\n");
		return JNI_FALSE;
	}

	for (i = 0; i < nitems; i++) {
		printf("%d: depth=%d\n", i, vlist[i].depth);
		if (vlist[i].class == TrueColor && vlist[i].depth > 8) {
			vis = vlist[i];
			XFree(vlist);
			break;
		}
	}
	if (i == nitems) {
		printf("NO TRUECOLOR VISUAL\n");
		return JNI_FALSE;
	}


	if (!use_shm) {
		noshm_mem_size = WIN_W * WIN_H * 4;
		noshm_mem = jxmalloc(noshm_mem_size MEMTYPE_OTHER);
		img = XCreateImage(d, vis.visual, vis.depth, ZPixmap, 0, noshm_mem, WIN_W, WIN_H, 8, 0);
	} else {
		/* Find out if MITSHM is supported and useable */

		if (!XShmQueryVersion(d, &mitshm_major_code, &mitshm_minor_code, &shared_pixmaps)) {
			printf("no shm");
			return JNI_FALSE;
		}


		img = XShmCreateImage(d, vis.visual, vis.depth, XShmPixmapFormat(d), NULL, &shminfo, WIN_W, WIN_H);
		shminfo.shmid = shmget(IPC_PRIVATE, img->bytes_per_line * img->height, IPC_CREAT | 0777);
		shminfo.shmaddr = img->data = shmat(shminfo.shmid, 0, 0);

		handler = XSetErrorHandler(mitshm_handler);
		XShmAttach(d, &shminfo);	/* Tell the server to attach */
		XSync(d, 0);
		XSetErrorHandler(handler);

		shmctl(shminfo.shmid, IPC_RMID, 0);
	}

	win = XCreateSimpleWindow(d, DefaultRootWindow(d), 0, 0, WIN_W, WIN_H, 0, WhitePixel(d, screen), BlackPixel(d, screen));

	XSelectInput(d, win,
		     ExposureMask | KeyPressMask | KeyReleaseMask | ButtonPressMask | ButtonReleaseMask | StructureNotifyMask |
		     PointerMotionMask | EnterWindowMask | LeaveWindowMask);
/*
	XSelectInput(d, win,
		     ButtonPressMask | ButtonReleaseMask | PointerMotionMask | KeyPressMask | KeyReleaseMask | ExposureMask);
*/
	XMapWindow(d, win);
	XSync(d, 0);

	while (1) {
		XNextEvent(d, &report);
		if (report.type == MapNotify)
			break;
	}

	XFlush(d);

	display_fd = XConnectionNumber(d);
}

void fb_shutdown()
{
	if (shminfo.shmid) {
		XShmDetach(d, &shminfo);
		XDestroyImage(img);
		shmdt(shminfo.shmaddr);
	}

	if (d) {
		XDestroyWindow(d, win);
		XCloseDisplay(d);
	}
}

MethodInfoDesc fbemulationMethods[] = {
	{"open", "", fbemulation_open}
	,
	{"getVideoMemory", "", fbemulation_getVideoMemory}
	,
	{"getWidth", "", fbemulation_getWidth}
	,
	{"getHeight", "", fbemulation_getHeight}
	,
	{"getBytesPerLine", "", fbemulation_getBytesPerLine}
	,
	{"getBitsPerPixel", "", fbemulation_getBitsPerPixel}
	,
	{"update", "", fbemulation_update}
	,
	{"inputDevicesAvailable", "", fbemulation_inputDevicesAvailable}
	,
	{"checkEvent", "", fbemulation_checkEvent}
	,
};

void init_fbemulation_portal()
{
	init_zero_dep("jx/zero/FBEmulation", "FBEmulation", fbemulationMethods, sizeof(fbemulationMethods),
		      "<jx/zero/FBEmulation>");
}

#endif				/* KERNEL */
#endif				/* FRAMEBUFFER_EMULATION */
