#define GLEW_STATIC
#define FREEGLUT_STATIC
#include "GL\glew.h"
#include "GL\freeglut.h"
#include "png.h"
#include <chrono>
#include <cstdio>

#include "net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer.h"
#include "jni.h"

namespace stch = std::chrono;

void MyMainLoop(int v);
long long frame_count = 0;
GLuint fbo, render_buf, fbo_res, render_buf_res;
int *params;
long long params_len;
bool flag = false;
unsigned char *data = new unsigned char[256 * 256 * 4];
int color1, color2, width;
float dx1, dy1, dx2, dy2;
void RenderFunction(int);
int samples = 1;

struct write_bundle{
	unsigned char* data = new unsigned char[200*1024];
	long long buff_len = 200 * 1024;
	long long len = 0;
};
void leave_mainloop(int v) {
	printf("leave main loop");
	glutLeaveMainLoop();
}
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
	char in[256];
	while (true) {
		scanf("%256s", in);
		if (strcmp(in, "exit") == 0) {
			printf("exiting!\n");
			break;
		}
		else {
			printf("drawing\n");
			if (strcmp(in, "abc") == 0) {
				for (int i1 = 0; i1 < params_len; i1+=7){
					params[i1 + 6] = params[i1+6] / 3 * 2;
				}
				printf("color change!");
			}
			glBindFramebuffer(GL_FRAMEBUFFER, fbo);
			auto start = stch::high_resolution_clock::now();
			RenderFunction(0);
			png_structp png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
			
			if (!png_ptr) {
				printf("No main struct!'n");
				continue;
			}

			png_infop info_ptr = png_create_info_struct(png_ptr);

			if (!info_ptr) {
				printf("No info struct!'n");
				continue;
			}

			png_set_IHDR(png_ptr, info_ptr, 256, 256, 8, PNG_COLOR_TYPE_RGBA, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);

			unsigned char** rows = new unsigned char*[256];
			for (int i = 0; i < 256; i++) {
				rows[i] = data + (256 * 4)*i;
			}

			png_set_rows(png_ptr, info_ptr, rows);

			write_bundle bundle;

			png_set_write_fn(png_ptr, &bundle, write_func, NULL);

			png_write_png(png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY, NULL);

			
			printf("png done in %f millis.\n", stch::duration_cast<stch::microseconds>(stch::high_resolution_clock::now() - start).count() / 1000.0);
			//png_init_io(png_ptr, fp);
			FILE *f = fopen("out.png", "wb");
			fwrite(bundle.data, 1, bundle.len, f);
			fflush(f);
			fclose(f);
			delete[] bundle.data;
			delete[] rows;
			png_destroy_write_struct(&png_ptr, &info_ptr);
			
		}
	}
	glDeleteFramebuffers(1, &fbo);
	glDeleteRenderbuffers(1, &render_buf);

	

}

void RenderFunction(int v)
{
	auto start = stch::high_resolution_clock::now();
	glDrawBuffer(GL_COLOR_ATTACHMENT0);
	glClear(GL_COLOR_BUFFER_BIT);
	
	for (int i = 0; i < params_len; i += 7) {
		dx1 = (params[i + 0] - 128) / 128.0f;
		dy1 = (params[i + 1] - 128) / 128.0f;
		color1 = params[i + 2];
		dx2 = (params[i + 3] - 128) / 128.0f;
		dy2 = (params[i + 4] - 128) / 128.0f;
		color2 = params[i + 5];
		width = params[i + 6];
		glLineWidth(width / 100.0f*samples*2);
		
		glBegin(GL_LINES);
		glColor3f(1, 0, 0);
		glColor4f((color1 >> 16 & 0xFF) / 255.0f, (color1 >> 8 & 0xFF) / 255.0f, (color1 & 0xFF) / 255.0f, 1.5f);
		glVertex2f(dx1, dy1);
		glColor4f((color2 >> 16 & 0xFF) / 255.0f, (color2 >> 8 & 0xFF) / 255.0f, (color2 & 0xFF) / 255.0f, 1.5f);
		glVertex2f(dx2, dy2);
		glEnd();
	}
	glFlush();

	glReadBuffer(GL_COLOR_ATTACHMENT0);
	glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
	glDrawBuffer(GL_COLOR_ATTACHMENT1);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo_res);

	glBlitFramebuffer(0, 0, 256*samples, 256*samples, 0, 0, 256, 256, GL_COLOR_BUFFER_BIT, GL_LINEAR);

	glBindFramebuffer(GL_FRAMEBUFFER, fbo_res);
	glReadBuffer(GL_COLOR_ATTACHMENT1);

	glReadPixels(0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, data);
	printf("begin:\n");
	for (int i = 0; i < 8; i++) {
		printf("%d ", data[i]);
	}
	printf("\n");
	printf("end:\n");
	for (int i = 256 * 256 * 4 - 8; i < 256 * 256 * 4; i++) {
		printf("%d ", data[i]);
	}
	printf("\n\n");
	printf("frame done in %f millis.\n", stch::duration_cast<stch::microseconds>(stch::high_resolution_clock::now() - start).count() / 1000.0);
	

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
	FILE *f = fopen("data", "rb");
	fseek(f, 0, SEEK_END);
	params_len = ftell(f) / 4;
	fseek(f, 0, SEEK_SET);
	params = new int[params_len]();
	fread(params, 4, params_len, f);
	fclose(f);

	glutInit(&argc, argv);
	glutSetOption(GLUT_ACTION_ON_WINDOW_CLOSE, GLUT_ACTION_CONTINUE_EXECUTION);
	glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA);
	glutCreateWindow("PropertyHeatMap OpenGL Renderer");
	GLenum err = glewInit();

	glGenFramebuffers(1, &fbo);
	glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	glGenRenderbuffers(1, &render_buf);
	glBindRenderbuffer(GL_RENDERBUFFER, render_buf);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, 256*samples, 256*samples);
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
	glutTimerFunc(500, leave_mainloop, 0);
	glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
	glEnable(GL_LINE_SMOOTH);
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
	glViewport(0, 0, 256 * samples, 256 * samples);
	glutHideWindow();
	glutMainLoop();
	MyMainLoop(0);
}



JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_initOpenGL
(JNIEnv *env, jobject obj) {
	int argc = 1;
	char *c = "Something";
	char *argv[1] = { c };
	glutInit(&argc, argv);
	
	glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA);
	glutCreateWindow("PropertyHeatMap OpenGL Renderer");
	GLenum err = glewInit();

	glGenFramebuffers(1, &fbo);
	glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	glGenRenderbuffers(1, &render_buf);
	glBindRenderbuffer(GL_RENDERBUFFER, render_buf);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, 256 * samples, 256 * samples);
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
	glutTimerFunc(30, leave_mainloop, 0);
	glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
	glEnable(GL_LINE_SMOOTH);
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
	glViewport(0, 0, 256 * samples, 256 * samples);
	glutHideWindow();
	glutSetOption(GLUT_ACTION_ON_WINDOW_CLOSE, GLUT_ACTION_CONTINUE_EXECUTION);
	glutMainLoop();
}

/*
* Class:     net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer
* Method:    drawOpenGLCall
* Signature: ([III)[B
*/
JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_drawOpenGLCall
(JNIEnv *env, jobject obj, jintArray arr, jint len, jint divider) {
	jint *params = env->GetIntArrayElements(arr, false);

	glBindFramebuffer(GL_FRAMEBUFFER, fbo);

	glDrawBuffer(GL_COLOR_ATTACHMENT0);
	glClear(GL_COLOR_BUFFER_BIT);

	for (int i = 0; i < len; i += 7) {
		dx1 = (params[i + 0] - 128) / 128.0f / divider;
		dy1 = (params[i + 1] - 128) / 128.0f / divider;
		color1 = params[i + 2];
		dx2 = (params[i + 3] - 128) / 128.0f / divider;
		dy2 = (params[i + 4] - 128) / 128.0f / divider;
		color2 = params[i + 5];
		width = params[i + 6];
		glLineWidth(width / 100.0f*samples * 2);

		glBegin(GL_LINES);
		glColor3f(1, 0, 0);
		glColor4f((color1 >> 16 & 0xFF) / 255.0f, (color1 >> 8 & 0xFF) / 255.0f, (color1 & 0xFF) / 255.0f, 1.5f);
		glVertex2f(dx1, dy1);
		glColor4f((color2 >> 16 & 0xFF) / 255.0f, (color2 >> 8 & 0xFF) / 255.0f, (color2 & 0xFF) / 255.0f, 1.5f);
		glVertex2f(dx2, dy2);
		glEnd();
	}
	glLineWidth(10);
	glBegin(GL_LINES);
	glVertex2f(0, 0);
	glVertex2f(1, 1);
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

	png_structp png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);

	if (!png_ptr) {
		printf("No main struct!'n");
		return env->NewByteArray(12);
	}

	png_infop info_ptr = png_create_info_struct(png_ptr);

	if (!info_ptr) {
		printf("No info struct!'n");
		return env->NewByteArray(12);
	}

	png_set_IHDR(png_ptr, info_ptr, 256, 256, 8, PNG_COLOR_TYPE_RGBA, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);

	unsigned char** rows = new unsigned char*[256];
	for (int i = 0; i < 256; i++) {
		rows[i] = data + (256 * 4)*i;
	}
	png_set_rows(png_ptr, info_ptr, rows);

	write_bundle bundle;

	png_set_write_fn(png_ptr, &bundle, write_func, NULL);
	png_write_png(png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY, NULL);

	jbyteArray res = env->NewByteArray(bundle.len);
	env->SetByteArrayRegion(res, 0, bundle.len, (jbyte *)bundle.data);
	FILE *f = fopen("out.png", "wb");
	fwrite(bundle.data, 1, bundle.len, f);
	fflush(f);
	fclose(f);

	env->ReleaseIntArrayElements(arr, params, JNI_ABORT);
	delete[] bundle.data;
	delete[] rows;
	png_destroy_write_struct(&png_ptr, &info_ptr);

	return res;
}
