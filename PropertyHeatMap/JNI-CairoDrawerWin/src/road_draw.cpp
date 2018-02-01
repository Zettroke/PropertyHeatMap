#include "cairo.h"
#include <string>
//#include <cstdio>

#include "jni.h"
#include "net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer.h"

#define INITIAL_BUFF_SIZE 32768



struct data_bundle {
	unsigned int curr;
	jbyte *data;
	unsigned int len;
	unsigned int buff_size;
};

cairo_status_t my_write_func(void* closure, const unsigned char* data, unsigned int length) {
	data_bundle *data_bndl = (data_bundle*)closure;
	if (data_bndl->curr + length > data_bndl->buff_size) {
		data_bndl->data = (jbyte *)realloc(data_bndl->data, data_bndl->buff_size + length);
		data_bndl->buff_size = data_bndl->buff_size + length;
	}
	memcpy(data_bndl->data + data_bndl->curr, data, length);
	data_bndl->len += length;
	data_bndl->curr += length;

	return CAIRO_STATUS_SUCCESS;
}

JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_drawNativeCall(JNIEnv *env, jobject obj, jintArray arr, jint len) {
	/*const int r = 0xFF0000;
	const int g = 0x00FF00;
	const int b = 0x0000FF;*/

	cairo_surface_t *surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 256, 256);
	cairo_t *cr = cairo_create(surface);
	
	cairo_set_source_rgba(cr, 1, 1, 1, 0);
	cairo_rectangle(cr, 0, 0, 256, 256);
	cairo_fill(cr);
	cairo_set_line_width(cr, 10);
	cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
	cairo_set_line_join(cr, CAIRO_LINE_JOIN_ROUND);
	int x1, y1, x2, y2, color1, color2, width;
	jboolean copy = false;
	jint* params = env->GetIntArrayElements(arr, &copy);
	for (int i = 0; i < len; i += 7) {
		x1 = params[i + 0];
		y1 = params[i + 1];
		color1 = params[i + 2];
		x2 = params[i + 3];
		y2 = params[i + 4];
		color2 = params[i + 5];
		width = params[i + 6];
		

		cairo_pattern_t *gradient = cairo_pattern_create_linear(x1, y1, x2, y2);
		cairo_pattern_add_color_stop_rgb(gradient, 0, (color1 >> 16 & 0xFF) / 255.0f, (color1 >> 8 & 0xFF) / 255.0f, (color1 & 0xFF) / 255.0f);
		cairo_pattern_add_color_stop_rgb(gradient, 1, (color2 >> 16 & 0xFF) / 255.0f, (color2 >> 8 & 0xFF) / 255.0f, (color2 & 0xFF) / 255.0f);
		cairo_set_source(cr, gradient);
		cairo_set_line_width(cr, width / 100.0f);
		cairo_move_to(cr, x1, y1);
		cairo_line_to(cr, x2, y2);
		cairo_stroke(cr);

		cairo_pattern_destroy(gradient);
		
	}
	
	cairo_surface_flush(surface);

	unsigned char *c = cairo_image_surface_get_data(surface);
	for (int j = 0; j < 262144; j += 1024) {
		for (int i = 0; i < 1024; i += 4) {
			c[j + i] =     (int)(c[j + i] * 0.8f);
			c[j + i + 1] = (int)(c[j + i + 1] * 0.8f);
			c[j + i + 2] = (int)(c[j + i + 2] * 0.8f);
			c[j + i + 3] = (int)(c[j + i + 3] * 0.8f);
		}
	}

	cairo_surface_mark_dirty(surface);
	
	data_bundle data{0, new jbyte[INITIAL_BUFF_SIZE], 0, INITIAL_BUFF_SIZE};

	cairo_surface_write_to_png_stream(surface, my_write_func, &data);

	jbyteArray res = env->NewByteArray(data.len);
	env->SetByteArrayRegion(res, 0, data.len, data.data);
	
	delete[] data.data;

	cairo_destroy(cr);

	cairo_surface_destroy(surface);

	env->ReleaseIntArrayElements(arr, params, JNI_ABORT);

	return res;
}

void print_data(unsigned char *c, unsigned int len) {
	for (unsigned int i = 0; i < len; i++){
		printf("%02X ", c[i]);		
	}
	printf("\n");
	for (unsigned int i = 0; i < len; i++) {
		printf("%02X ", c[i+1024]);
	}
	printf("\n");
}

std::string show_data(unsigned char *c, unsigned int off, unsigned int len) {
	std::string res;
	for (unsigned int i = off; i < off+len; i++) {
		res.append(std::to_string(c[i]));
		res.append(" ");
	}
	return res;
}

int main() {
	cairo_surface_t *surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 256, 256);
	cairo_t *cr = cairo_create(surface);

	cairo_surface_t *surface2 = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 256, 256);
	cairo_t *cr2 = cairo_create(surface2);


	cairo_set_source_rgba(cr, 1, 0, 0, 1);
	cairo_rectangle(cr, 0, 0, 256, 256);
	cairo_fill(cr);

	cairo_set_line_width(cr, 10);
	cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
	cairo_set_line_join(cr, CAIRO_LINE_JOIN_ROUND);

	cairo_pattern_t *gradient = cairo_pattern_create_linear(100, 100, 200, 128);
	cairo_pattern_add_color_stop_rgb(gradient, 0, 1, 0, 0);
	cairo_pattern_add_color_stop_rgb(gradient, 1, 0, 0, 1);
	cairo_set_source(cr, gradient);
	cairo_move_to(cr, 100, 100);
	cairo_line_to(cr, 200, 128);
	cairo_stroke(cr);
	cairo_pattern_destroy(gradient);


	cairo_pattern_t *gradient2 = cairo_pattern_create_linear(200, 128, 20, 200);
	cairo_pattern_add_color_stop_rgb(gradient2, 0, 0, 0, 1);
	cairo_pattern_add_color_stop_rgb(gradient2, 1, 0, 1, 0);
	cairo_set_source(cr, gradient2);
	cairo_move_to(cr, 200, 128);
	cairo_line_to(cr, 20, 320);
	cairo_stroke(cr);
	cairo_pattern_destroy(gradient2);

	cairo_surface_flush(surface);

	//cairo_surface_t surface3 = cairo_surface_copy
	cairo_set_source_rgba(cr2, 1, 1, 1, 0);
	cairo_rectangle(cr2, 0, 0, 256, 256);
	cairo_fill(cr2);

	cairo_set_source_surface(cr2, surface, 0, 0);
	cairo_paint_with_alpha(cr2, 0.5f);

	unsigned char* c = cairo_image_surface_get_data(surface);
	for (int j = 0; j < 262144; j+=1024) {
		for (int i = 0; i < 1024; i+=4) {
			c[j + i] >>= 1;
			c[j + i + 1] >>= 1;
			c[j + i + 2] >>= 1;
			c[j + i + 3] >>= 1;
		}
	}
	cairo_surface_flush(surface2);
	unsigned char* c2 = cairo_image_surface_get_data(surface2);
	printf("%d", memcmp(c2, c, 1024));
	cairo_surface_mark_dirty(surface);
	
	cairo_surface_write_to_png(surface2, "file2.png");
	cairo_surface_write_to_png(surface, "file3.png");

	cairo_destroy(cr);
	cairo_surface_destroy(surface);
	cairo_destroy(cr2);
	cairo_surface_destroy(surface2);

	int t;
	//scanf("%d", &t);
}