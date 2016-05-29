#ifndef ZIP_H
#define ZIP_H
typedef struct {
	char filename[80];
	jint uncompressed_size;
	jint compression_method;
	char *data;
	jint isDirectory;
} zipentry;

/* Prototypes */
void zip_init(char *zipstart, jint ziplen);

#endif
