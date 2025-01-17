#include "cairo.h"
#include <memory>
#include <chrono>
#include <fstream>

#include "jni.h"
#include "net_zettroke_PropertyHeatMapServer_utils_Drawer.h"

#define INITIAL_BUFF_SIZE 32768

namespace stch = std::chrono;

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

JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawGraphCairoCall
(JNIEnv *env, jobject obj, jintArray arr, jint len , jint divider) {

	//printf("drawing %d lines\n", len / 14);
	auto start = stch::high_resolution_clock::now();
	cairo_surface_t *surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 256, 256);
	cairo_t *cr = cairo_create(surface);
	
	cairo_set_source_rgba(cr, 1, 1, 1, 0);
	cairo_rectangle(cr, 0, 0, 256, 256);
	cairo_fill(cr);
	cairo_set_line_width(cr, 10);
	cairo_set_line_cap(cr, CAIRO_LINE_CAP_ROUND);
	cairo_set_line_join(cr, CAIRO_LINE_JOIN_ROUND);
	cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
	int x1, y1, x2, y2, color1, color2, width;
	jboolean copy = false;
	jint* params = env->GetIntArrayElements(arr, &copy);
	for (int i = 0; i < len; i += 7) {
		x1 = params[i + 0] / divider;
		y1 = params[i + 1] / divider;
		color1 = params[i + 2];
		x2 = params[i + 3] / divider;
		y2 = params[i + 4] / divider;
		color2 = params[i + 5];
		width = params[i + 6];
		

		cairo_pattern_t *gradient = cairo_pattern_create_linear(x1, y1, x2, y2);
		cairo_pattern_add_color_stop_rgba(gradient, 0, (color1 >> 16 & 0xFF) / 255.0, (color1 >> 8 & 0xFF) / 255.0, (color1 & 0xFF) / 255.0, 0.85);
		cairo_pattern_add_color_stop_rgba(gradient, 1, (color2 >> 16 & 0xFF) / 255.0, (color2 >> 8 & 0xFF) / 255.0, (color2 & 0xFF) / 255.0, 0.85);
		cairo_set_source(cr, gradient);
		cairo_set_line_width(cr, width / 100.0f);
		cairo_move_to(cr, x1, y1);
		cairo_line_to(cr, x2, y2);
		cairo_stroke(cr);

		cairo_pattern_destroy(gradient);
		
	}
	cairo_surface_flush(surface);
	double t1 = stch::duration_cast<stch::microseconds>(stch::high_resolution_clock::now() - start).count() / 1000.0;
	//printf("draw done in %f millis.\n", t1);

	start = stch::high_resolution_clock::now();
	data_bundle data{0, new jbyte[INITIAL_BUFF_SIZE], 0, INITIAL_BUFF_SIZE};

	cairo_surface_write_to_png_stream(surface, my_write_func, &data);
	double t2 = stch::duration_cast<stch::microseconds>(stch::high_resolution_clock::now() - start).count() / 1000.0;
	//printf("png done in %f millis.\n", t2);
	jbyteArray res = env->NewByteArray(data.len);
	env->SetByteArrayRegion(res, 0, data.len, data.data);
	
	delete[] data.data;

	cairo_destroy(cr);

	cairo_surface_destroy(surface);

	env->ReleaseIntArrayElements(arr, params, JNI_ABORT);
	std::ofstream of("timing.txt");
	of << t1 << " " << t2;
	of.close();
	return res;
}

JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawBuildingCairoCall
(JNIEnv *env, jobject obj, jintArray arrPoints, jint len, jintArray arrPolyLens, jint len2, jint divider) {
	cairo_surface_t *surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 256, 256);
	cairo_t *cr = cairo_create(surface);

	cairo_set_line_width(cr, 1);
	cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
	cairo_set_source_rgba(cr, 1, 1, 1, 0);
	cairo_rectangle(cr, 0, 0, 256, 256);
	cairo_fill(cr);
	jboolean isCopy = false;
	jint* points = env->GetIntArrayElements(arrPoints, &isCopy);
	jint* poly_lens = env->GetIntArrayElements(arrPolyLens, &isCopy);

	unsigned int point_ind_off = 0;
	unsigned int poly_ind = 0;
	double d = divider;
	double x, y;
	for (; poly_ind < len2; poly_ind++) {
		x = points[point_ind_off]/d;
		y = points[point_ind_off + 1]/d;
		cairo_move_to(cr, x, y);
		for (int i = 0; i < poly_lens[poly_ind]; i++) {
			x = points[point_ind_off + i * 2]/d;
			y = points[point_ind_off + i * 2+1]/d;
			cairo_line_to(cr, (int)x, (int)y);
		}
		int color = points[point_ind_off + poly_lens[poly_ind] * 2];
		

		cairo_set_source_rgba(cr, (color >> 16 & 0xFF) / 255.0, (color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0, 0.85);
		cairo_fill(cr);

		x = points[point_ind_off] / d;
		y = points[point_ind_off + 1] / d;
		cairo_move_to(cr, x, y);
		for (int i = 0; i < poly_lens[poly_ind]; i++) {
			x = points[point_ind_off + i * 2] / d;
			y = points[point_ind_off + i * 2 + 1] / d;
			cairo_line_to(cr, (int)x, (int)y);
		}
		cairo_set_source_rgba(cr, (color >> 16 & 0xFF) / 255.0, (color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0, 1);
		cairo_stroke(cr);
		
		point_ind_off += poly_lens[poly_ind] * 2 + 1;
	}

	data_bundle data{ 0, new jbyte[INITIAL_BUFF_SIZE], 0, INITIAL_BUFF_SIZE };

	cairo_surface_write_to_png_stream(surface, my_write_func, &data);
	jbyteArray res = env->NewByteArray(data.len);
	env->SetByteArrayRegion(res, 0, data.len, data.data);

	delete[] data.data;

	cairo_destroy(cr);

	cairo_surface_destroy(surface);

	env->ReleaseIntArrayElements(arrPoints, points, JNI_ABORT);
	env->ReleaseIntArrayElements(arrPolyLens, poly_lens, JNI_ABORT);

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

/*std::string show_data(unsigned char *c, unsigned int off, unsigned int len) {
	std::string res;
	for (unsigned int i = off; i < off+len; i++) {
		res.append(std::to_string(c[i]));
		res.append(" ");
	}
	return res;
}*/

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