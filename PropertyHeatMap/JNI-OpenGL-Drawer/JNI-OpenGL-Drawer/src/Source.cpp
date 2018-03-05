#include <stdio.h>
#include <Windows.h>


#include "jni.h"
#include "net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer.h"
#include "shared_data_types.h"

int number_of_process;
HANDLE* mapfiles;
HANDLE* events;
unsigned char** out_buffers;


JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_initOpenGLRenderer
(JNIEnv *env, jobject obj, jint num_proc) {
	mapfiles = new HANDLE[num_proc];
	events = new HANDLE[num_proc];
	out_buffers = new unsigned char*[num_proc];
	char shared_memory_name[256];
	char event_name[256];
	number_of_process = num_proc;
	for (int i = 0; i < num_proc; i++) {
		sprintf(shared_memory_name, "PHM_shared_memory%d", i);
		sprintf(event_name, "PHM_event%d", i);
		mapfiles[i] = CreateFileMapping(
			INVALID_HANDLE_VALUE,    
			NULL,                   
			PAGE_READWRITE,			 
			0,                       
			1024 * 1024 * 10,            
			shared_memory_name);

		out_buffers[i] = (unsigned char *)MapViewOfFile(mapfiles[i], FILE_MAP_ALL_ACCESS, 0, 0, 1024 * 1024 * 10);

		SECURITY_DESCRIPTOR sd = { 0 };
		InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION);
		SetSecurityDescriptorDacl(&sd, TRUE, 0, FALSE);
		SECURITY_ATTRIBUTES sa = { 0 };
		sa.nLength = sizeof(SECURITY_ATTRIBUTES);
		sa.lpSecurityDescriptor = &sd;

		events[i] = CreateEvent(&sa, false, false, event_name);
	}
	
	char name[1024];
	STARTUPINFO info = { 42 };
	PROCESS_INFORMATION processInfo;
	for (int i = 0; i < num_proc; i++) {
		sprintf(shared_memory_name, "PHM_shared_memory%d", i);
		sprintf(event_name, "PHM_event%d", i);
		sprintf(name, "\"PropertyHeatMap OpenGL Renderer.exe\" %s %s", shared_memory_name, event_name);
		CreateProcess(NULL, name, NULL, NULL, false, 0, NULL, NULL, &info, &processInfo);
	}
	
}

JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_drawOpenGLCall
(JNIEnv *env, jobject obj, jintArray arr, jint len, jint divider, jint zoom_level, jint proc_ind, jint max_dist) {
	jboolean copy = false;
	jint *params = env->GetIntArrayElements(arr, &copy);
	tile_data td{len, divider, zoom_level, max_dist};
	((tile_data*)out_buffers[proc_ind])[0] = td;
	memcpy(out_buffers[proc_ind] + sizeof(tile_data), params, len * 4);
	SetEvent(events[proc_ind]);
	WaitForSingleObject(events[proc_ind], INFINITE);
	int out_len = ((int*)out_buffers[proc_ind])[0];
	jbyteArray out_arr = env->NewByteArray(out_len);
	env->SetByteArrayRegion(out_arr, 0, out_len, (jbyte *)(out_buffers[proc_ind] + 4));
	env->ReleaseIntArrayElements(arr, params, JNI_ABORT);
	return out_arr;
}

JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_closeOpenGLRenderer
(JNIEnv *env, jobject obj) {

}


int main() {
	int params_len;
	int* params;
	

	FILE *f = fopen("data", "rb");
	fseek(f, 0, SEEK_END);
	params_len = ftell(f) / 4;
	fseek(f, 0, SEEK_SET);
	params = new int[params_len]();
	fread(params, 4, params_len, f);
	fclose(f);

	
	//LPCTSTR pBuf;
	HANDLE mapfile;
	mapfile = CreateFileMapping(
		INVALID_HANDLE_VALUE,    // use paging file
		NULL,                    // default security
		PAGE_READWRITE,			 // read/write access
		0,                       // maximum object size (high-order DWORD)
		1024*1024*10,            // maximum object size (low-order DWORD)
		"MyFileMappingObject");

	if (mapfile == NULL) {
		printf("Shiiit!1 %d\n", GetLastError());
		getchar();
		exit(1);
	}
	unsigned char* p = (unsigned char *)MapViewOfFile(mapfile, FILE_MAP_ALL_ACCESS, 0, 0, 1024 * 1024 * 10);
	if (p == NULL) {
		printf("Shiiit!2\n");
		getchar();
		exit(1);
	}
	SECURITY_DESCRIPTOR sd = { 0 };

	InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION);

	SetSecurityDescriptorDacl(&sd, TRUE, 0, FALSE);

	SECURITY_ATTRIBUTES sa = { 0 };

	sa.nLength = sizeof(SECURITY_ATTRIBUTES);

	sa.lpSecurityDescriptor = &sd;


	HANDLE evt = CreateEvent(&sa, false, false, "PropertyHeatMapEvent1");
	ResetEvent(evt);
	if (evt == NULL) {
		printf("Shiiit!3 %d\n", GetLastError());
		getchar();
		exit(1);
	}
	printf("waiting for input\n");
	char c[256];
	while (true) {
		scanf("%255s", c);
		if (strcmp(c, "exit") == 0) {
			break;
		}
		((int *)p)[0] = params_len;
		memcpy(p + 4, params, params_len * 4);


		SetEvent(evt);	
		WaitForSingleObject(evt, INFINITE);


		int l = ((int *)p)[0];

		FILE *fout = fopen("lel.png", "wb");
		fwrite(p + 4, 1, l, fout);
		fflush(fout);
		fclose(fout);
	}
	((int *)p)[0] = -1;
	SetEvent(evt);
	//WaitForSingleObject(evt, INFINITE);
	getchar();
	
	
	printf("%d\n", mapfile);
	UnmapViewOfFile(p);
	CloseHandle(evt);
	CloseHandle(mapfile);

}