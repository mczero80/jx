all: serial

include ../jcore/settings.makefile

serial: serial.c ../jcore/settings.makefile
	$(CC) $(DEFINES) -DLINUX -DUSE_LITTLE_ENDIAN -I../jcore -o $(@F) $<
serialsun: serial.c ../jcore/settings.makefile
	$(CC) $(DEFINES) -DSOLARIS -DUSE_BIG_ENDIAN -I../jcore -o $(@F) serial.c
