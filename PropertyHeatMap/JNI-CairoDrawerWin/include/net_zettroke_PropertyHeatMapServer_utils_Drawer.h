/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class net_zettroke_PropertyHeatMapServer_utils_Drawer */

#ifndef _Included_net_zettroke_PropertyHeatMapServer_utils_Drawer
#define _Included_net_zettroke_PropertyHeatMapServer_utils_Drawer
#ifdef __cplusplus
extern "C" {
#endif
#undef net_zettroke_PropertyHeatMapServer_utils_Drawer_zoom_level
#define net_zettroke_PropertyHeatMapServer_utils_Drawer_zoom_level 13L
/*
 * Class:     net_zettroke_PropertyHeatMapServer_utils_Drawer
 * Method:    drawGraphCairoCall
 * Signature: ([III)[B
 */
JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawGraphCairoCall
  (JNIEnv *, jobject, jintArray, jint, jint);

/*
 * Class:     net_zettroke_PropertyHeatMapServer_utils_Drawer
 * Method:    drawBuildingCairoCall
 * Signature: ([II[III)[B
 */
JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawBuildingCairoCall
  (JNIEnv *, jobject, jintArray, jint, jintArray, jint, jint);

/*
 * Class:     net_zettroke_PropertyHeatMapServer_utils_Drawer
 * Method:    initOpenGLRenderer
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_initOpenGLRenderer
  (JNIEnv *, jobject, jint);

/*
 * Class:     net_zettroke_PropertyHeatMapServer_utils_Drawer
 * Method:    drawGraphOpenGLCall
 * Signature: ([IIIIII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawGraphOpenGLCall
  (JNIEnv *, jobject, jintArray, jint, jint, jint, jint, jint);

/*
 * Class:     net_zettroke_PropertyHeatMapServer_utils_Drawer
 * Method:    drawBuildingOpenGLCall
 * Signature: ([II[IIIII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_Drawer_drawBuildingOpenGLCall
  (JNIEnv *, jobject, jintArray, jint, jintArray, jint, jint, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
