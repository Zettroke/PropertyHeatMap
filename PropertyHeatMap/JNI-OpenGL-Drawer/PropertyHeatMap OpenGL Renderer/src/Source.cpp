#define GLEW_STATIC
#define FREEGLUT_STATIC
#include "GL\glew.h"
#include "GL\freeglut.h"
#include "png.h"
#include <cstdio>
#include <Windows.h>
#include "shared_data_types.h"
#include <cmath>


#define max(x, y)(x > y?x:y)
#define min(x, y)(x < y?x:y)

HANDLE evt;
HANDLE mapfile;
unsigned char* out_buff;
GLuint fbo, render_buf, fbo_res, render_buf_res;
int *params;
long long params_len;
unsigned char *data = new unsigned char[256 * 256 * 4];
int dist1, dist2, width, zoom_level;
float dx1, dy1, dx2, dy2;
void RenderFunction(int);
int samples = 1;
float divider = 1;
int max_dist = 0;

char debug[256];

void HSV_to_RGB(float &r, float &g, float &b, float h, float s, float v) {
	if (s == 0) {
		r = g = b = v;	
	}
	else {
		if (h == 360) h = 0;
		h /= 60;
		int i = (int)h;
		float f = h - i;
		float p = v * (1 - s);
		float q = v * (1 - s * f);
		float t = v * (1 - s * (1 - f));
		switch (i) {
		case 0:
			r = v;
			g = t;
			b = p;
			break;
		case 1:
			r = q;
			g = v;
			b = p;
			break;
		case 2:
			r = p;
			g = v;
			b = t;
			break;
		case 3:
			r = p;
			g = q;
			b = v;
			break;
		case 4:
			r = t;
			g = p;
			b = v;
			break;
		case 5:
		default:
			r = v;
			g = p;
			b = q;
			break;

		}

	}
}

void getNodeColor(float &r, float &g, float &b, int dist, int max_dist) {
	float target_low_b = 0.6f;
	if (dist <= max_dist) {
		float bf = 0.93f;
		float hue = pow((1 - (dist / (double)max_dist)), 0.7);
		if (hue < 0.15) {
			bf = target_low_b + hue * (1 - target_low_b) / 0.15f;
		}
		HSV_to_RGB(r, g, b, hue * 120.0f, 1, bf);
	}
	else {
		HSV_to_RGB(r, g, b, 0, 1, target_low_b);
	}
}

struct write_bundle{
	unsigned char* data = new unsigned char[200*1024];
	long long buff_len = 200 * 1024;
	long long len = 0;
};
void write_func(png_structp  png_ptr, png_bytep data, png_size_t length) {
	write_bundle* bundle = (write_bundle *)png_get_io_ptr(png_ptr);
	if (bundle->len + length < bundle->buff_len) {
		memcpy(bundle->data + bundle->len, data, length);
	}
	else {
		if (bundle->buff_len * 3 / 2 > bundle->len + length) {
			bundle->data = (unsigned char *)realloc(bundle->data, bundle->buff_len * 3 / 2);
		}
		else {
			bundle->data = (unsigned char *)realloc(bundle->data, bundle->buff_len + length);
		}
		
		memcpy(bundle->data + bundle->len, data, length);
	}
	bundle->len += length;
}

void MyMainLoop(int v) {
	
	while (true) {

		long long ret = WaitForSingleObject(evt, INFINITE);

		tile_data td = ((tile_data*)out_buff)[0];
		params_len = td.len;
		divider = td.divider;
		zoom_level = td.zoom_level;
		max_dist = td.max_dist;
		params = (int*)(out_buff + sizeof(tile_data));
		if (params_len == -1) {
			break;
		}
		RenderFunction(0);

		png_structp png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
		if (!png_ptr) {
			printf("No main struct!'n");

		}
		png_infop info_ptr = png_create_info_struct(png_ptr);
		if (!info_ptr) {
			printf("No info struct!'n");
		}
		png_set_IHDR(png_ptr, info_ptr, 256, 256, 8, PNG_COLOR_TYPE_RGBA, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
		unsigned char* curr;
		unsigned char** rows = new unsigned char*[256];
		for (int i = 0; i < 256; i++) {
			rows[i] = data + (256 * 4)*i;
		}
		png_set_rows(png_ptr, info_ptr, rows);

		write_bundle bundle;

		png_set_write_fn(png_ptr, &bundle, write_func, NULL);

		png_write_png(png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY, NULL);

		//printf("png done in %f millis.\n", stch::duration_cast<stch::microseconds>(stch::high_resolution_clock::now() - start).count() / 1000.0);
		((int*)out_buff)[0] = bundle.len;
		memcpy(out_buff + 4, bundle.data, bundle.len);

		/*FILE *f = fopen("test.png", "wb");
		fwrite(bundle.data, 1, bundle.len, f);
		fflush(f);
		fclose(f);*/

		delete[] bundle.data;
		delete[] rows;
		png_destroy_write_struct(&png_ptr, &info_ptr);

		SetEvent(evt);
	}

	CloseHandle(evt);
	UnmapViewOfFile(out_buff);
	CloseHandle(mapfile);
	glDeleteFramebuffers(1, &fbo);
	glDeleteRenderbuffers(1, &render_buf);
	glutLeaveMainLoop();
}

void RenderFunction(int v)
{
	//auto start = stch::high_resolution_clock::now();
	glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	glDrawBuffer(GL_COLOR_ATTACHMENT0);
	glClear(GL_COLOR_BUFFER_BIT);
	float r, g, b;
	for (int i = 0; i < params_len; i += 7) {
		dx1 = (params[i + 0] / divider - 128) / 128.0f;
		dy1 = (params[i + 1] / divider - 128) / 128.0f;
		dist1 = params[i + 2];
		dx2 = (params[i + 3] / divider - 128) / 128.0f;
		dy2 = (params[i + 4] / divider - 128) / 128.0f;
		dist2 = params[i + 5];
		width = params[i + 6];
		if (width < 20) {
			continue;
		}
		if (zoom_level <= 13) {
			width = width * (1 << (13-zoom_level));
		}
		
		glLineWidth(width / 100.0f);
		
		glBegin(GL_LINES);
		getNodeColor(r, g, b, dist1, max_dist);
		glColor4f(r, g, b, 0.8f);
		glVertex2f(dx1, dy1);
		getNodeColor(r, g, b, dist2, max_dist);
		glColor4f(r, g, b, 0.8f);
		glVertex2f(dx2, dy2);

		glEnd();
	}
	glFlush();

	glReadBuffer(GL_COLOR_ATTACHMENT0);
	glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
	glDrawBuffer(GL_COLOR_ATTACHMENT1);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo_res);

	glBlitFramebuffer(0, 0, 256, 256, 0, 0, 256, 256, GL_COLOR_BUFFER_BIT, GL_LINEAR);

	glBindFramebuffer(GL_FRAMEBUFFER, fbo_res);
	glReadBuffer(GL_COLOR_ATTACHMENT1);

	glReadPixels(0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, data);
	/*for (int i = 0; i < 256; i++) {
		for (int j = 0; j < 256; j++) {
			unsigned char* curr = data + (i * 256 + j) * 4;
			//curr[0] = curr[0] * 9 / 8;
			//curr[1] = curr[1] * 9 / 8;
			//curr[2] = curr[2] * 9 / 8;
			curr[3] = curr[3] * 15 / 16;
		}
	}*/
}

void start_up(int v) {
	glDrawBuffer(GL_COLOR_ATTACHMENT0);
	glClear(GL_COLOR_BUFFER_BIT);
	glBegin(GL_QUADS);
	glVertex2f(1, 1);
	glVertex2f(-1, 1);
	glVertex2f(-1, -1);
	glVertex2f(1, -1);
	glEnd();
	glFlush();

	glReadBuffer(GL_COLOR_ATTACHMENT0);
	glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
	glDrawBuffer(GL_COLOR_ATTACHMENT1);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo_res);

	glBlitFramebuffer(0, 0, 256 * samples, 256 * samples, 0, 0, 256, 256, GL_COLOR_BUFFER_BIT, GL_LINEAR);

	glBindFramebuffer(GL_FRAMEBUFFER, fbo_res);
	glReadBuffer(GL_COLOR_ATTACHMENT1);

	glReadPixels(0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, data);
}

int main(int argc, char** argv) {
	memset(data, 42, 256 * 256 * 4);
	char shared_memory_name[256];
	char event_name[256];
	strcpy(shared_memory_name, argv[1]);
	strcpy(event_name, argv[2]);


	evt = OpenEvent(EVENT_ALL_ACCESS, TRUE, event_name);
	if (evt == NULL) {
		printf("fuck %d", GetLastError());
		getchar();
		exit(1);
	}

	mapfile = OpenFileMapping(FILE_MAP_ALL_ACCESS, FALSE, shared_memory_name);
	if (mapfile == NULL) {
		printf("Fuck! %d", GetLastError());
		getchar();
		exit(1);
	}
	out_buff = (unsigned char*)MapViewOfFile(mapfile, // handle to map object
		FILE_MAP_ALL_ACCESS,  // read/write permission
		0,
		0,
		1024*1024*10);
	if (out_buff == NULL) {
		printf("Fuck! %d", GetLastError());
		getchar();
		exit(1);
	}
	
	
	glutInit(&argc, argv);
	glutSetOption(GLUT_ACTION_ON_WINDOW_CLOSE, GLUT_ACTION_CONTINUE_EXECUTION);
	
	glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA);
	glutCreateWindow("PropertyHeatMap OpenGL Renderer");
	GLenum err = glewInit();

	glGenFramebuffers(1, &fbo);
	glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	glGenRenderbuffers(1, &render_buf);
	glBindRenderbuffer(GL_RENDERBUFFER, render_buf);
	//glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, 256, 256);
	glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_RGBA, 256, 256);
	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, render_buf);

	glGenFramebuffers(1, &fbo_res);
	glBindFramebuffer(GL_FRAMEBUFFER, fbo_res);
	glGenRenderbuffers(1, &render_buf_res);
	glBindRenderbuffer(GL_RENDERBUFFER, render_buf_res);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, 256, 256);
	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_RENDERBUFFER, render_buf_res);

	glBindFramebuffer(GL_FRAMEBUFFER, fbo);

	glutTimerFunc(10, start_up, 0);
	glutTimerFunc(20, start_up, 0);
	glutTimerFunc(30, MyMainLoop, 0);
	glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
	glEnable(GL_LINE_SMOOTH);
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    //glBlendFunc(GL_SRC_ALPHA, GL_CONSTANT_ALPHA);
	glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
	glViewport(0, 0, 256, 256);
	glutHideWindow();
	glutMainLoop();
}
