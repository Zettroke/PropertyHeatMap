#include <stdio.h>
#include <Windows.h>
#include <vector>

#include "jni.h"
#include "net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer.h"
#include "shared_data_types.h"
enum RoadType{
	FOOTWAY,
	SECONDARY,
	LIVING_STREET,
	RESIDENTIAL,
	SERVICE,
	CONSTRUCTION,
	TERTIARY,
	PRIMARY,
	PATH,
	TRUNK,
	INVISIBLE,
	SUBWAY,
	BUS,
	TRAM,
	TROLLEYBUS,
	DEFAULT
};
struct MapPoint {
	int x, y;
	int* dist;
};
struct RoadGraphLine {
	MapPoint *p1, *p2;
	RoadType type;
};
class QuadTreeNode {
	QuadTreeNode* nw;
	QuadTreeNode* ne;
	QuadTreeNode* sw;
	QuadTreeNode* se;
public:
	int* bounds;
	

	struct iterator {
		QuadTreeNode** p;
		bool operator!=(iterator rhs) { return p != rhs.p; }
		QuadTreeNode*& operator*() { return *p; }
		void operator++() { ++p; }
	};

	iterator begin() { return iterator{ &nw }; }
	iterator end() { return iterator{ (&se) + 4 }; }

	std::vector<RoadGraphLine> lines;
	
	bool inBounds(MapPoint* p) {
		return (p->x >= bounds[0] && p->x <= bounds[2] && p->y >= bounds[1] && p->y <= bounds[3]);
	}

	bool intersec_with_quad(int bounds[4]) {
		return !((this->bounds[0] < bounds[0] && this->bounds[2] < bounds[0]) || (this->bounds[0] > bounds[2] && this->bounds[2] > bounds[2]) ||
			(this->bounds[1] < bounds[1] && this->bounds[3] < bounds[1]) || (this->bounds[1] > bounds[3] && this->bounds[3] > bounds[3]));
	}

	bool contained_in_quad(int bounds[4]) {
		return this->bounds[0] >= bounds[0] && this->bounds[2] <= bounds[2] && this->bounds[1] >= bounds[1] && this->bounds[3] <= bounds[3];
	}

	static bool HorzCross(int horz, int x1, int x2, MapPoint* p1, MapPoint* p2) {
		if ((p1->y > horz && p2->y > horz) || (p1->y < horz && p2->y < horz)) {
			return false;
		}
		else {
			float x = p1->x + ((horz - p1->y) / static_cast<float>(p2->y - p1->y))*(p2->x - p1->x);
			if (x >= min(x1, x2) && x <= max(x1, x2)) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	static bool VertCross(int vert, int y1, int y2, MapPoint* p1, MapPoint* p2) {
		if ((p1->x > vert && p2->x > vert) || (p1->x < vert && p2->x < vert)) {
			return false;
		}
		else {
			double y = p1->y + ((vert - p1->x) / static_cast<float>(p2->x - p1->x))*(p2->y - p1->y);
			if (y >= min(y1, y2) && y <= max(y1, y2)) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	bool isEndNode = true;

	void add(RoadGraphLine line) {
		if (this->isEndNode) {
			if (inBounds(line.p1) || inBounds(line.p2)) {
				lines.push_back(line);
			}
			else {
				if (HorzCross(bounds[1], bounds[0], bounds[2], line.p1, line.p2) ||
					HorzCross(bounds[3], bounds[0], bounds[2], line.p1, line.p2) ||
					VertCross(bounds[0], bounds[1], bounds[3], line.p1, line.p2) ||
					VertCross(bounds[2], bounds[1], bounds[3], line.p1, line.p2)) 
				{
					lines.push_back(line);
				}
			}
			if (lines.size() > 1000) {
				split();
			}
		}
		else {
			for (int i = 0; i < 4; i++) {
				QuadTreeNode* t = (&(this->nw))[i];
				if (t->inBounds(line.p1) || t->inBounds(line.p2)) {
					t->add(line);
				}
				else {
					if (HorzCross(t->bounds[1], t->bounds[0], t->bounds[2], line.p1, line.p2) ||
						HorzCross(t->bounds[3], t->bounds[0], t->bounds[2], line.p1, line.p2) ||
						VertCross(t->bounds[0], t->bounds[1], t->bounds[3], line.p1, line.p2) ||
						VertCross(t->bounds[2], t->bounds[1], t->bounds[3], line.p1, line.p2))
					{
						t->add(line);
					}
				}
			}
		}
	}
	void split() {
		isEndNode = false;
		int hx = (bounds[0] + bounds[2]) / 2;
		int hy = (bounds[1] + bounds[3]) / 2;

		nw = new QuadTreeNode(bounds[0], bounds[1], hx, hy);
		ne = new QuadTreeNode(hx, bounds[1], bounds[2], hy);
		sw = new QuadTreeNode(bounds[0], hy, hx, bounds[3]);
		se = new QuadTreeNode(hx, hy, bounds[2], bounds[3]);

		for (RoadGraphLine l : lines) {
			add(l);
		}
		lines.clear();
	}

	QuadTreeNode(int b1, int b2, int b3, int b4) {
		bounds = new int[4]{ b1, b2, b3, b4 };
	}
};

const int zoom_level = 13;
int number_of_process;
HANDLE* mapfiles;
HANDLE* events;
unsigned char** out_buffers;
int server_zoom;

std::vector<MapPoint*> points;
QuadTreeNode* root;


void rec_search(int bounds[4], QuadTreeNode* n, std::vector<RoadGraphLine> &result) {
	if (n->isEndNode) {
		if (n->contained_in_quad(bounds)) {
			result.insert(result.end(), n->lines.begin(), n->lines.end());
		}
		else {
			for (RoadGraphLine &line : n->lines) {
				if (n->inBounds(line.p1) || n->inBounds(line.p2)) {
					n->add(line);
				}
				else {
					if (QuadTreeNode::HorzCross(n->bounds[1], n->bounds[0], n->bounds[2], line.p1, line.p2) ||
						QuadTreeNode::HorzCross(n->bounds[3], n->bounds[0], n->bounds[2], line.p1, line.p2) ||
						QuadTreeNode::VertCross(n->bounds[0], n->bounds[1], n->bounds[3], line.p1, line.p2) ||
						QuadTreeNode::VertCross(n->bounds[2], n->bounds[1], n->bounds[3], line.p1, line.p2))
					{
						result.push_back(line);
					}
				}
			}
		}
	}
	else {
		for (QuadTreeNode* t : *n) {
			if (t->intersec_with_quad(bounds)) {
				rec_search(bounds, t, result);
			}
		}
	}
}

std::vector<RoadGraphLine> getLinesInSquare(int bounds[4], QuadTreeNode* n) {
	std::vector<RoadGraphLine> result;
	rec_search(bounds, n, result);
	return result;
}


JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_initOpenGLRenderer
(JNIEnv *env, jobject obj, jint cache_size, jint server_zoom, jintArray roadGraphNodes, jint len1, jintArray roadGraphLines, jint len2, jintArray bounds, jint num_proc) {
	server_zoom = server_zoom;
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
	jboolean copy = false;
	jint* bnds = env->GetIntArrayElements(bounds, &copy);
	root = new QuadTreeNode(bnds[0], bnds[1], bnds[2], bnds[3]);
	jint* nodes = env->GetIntArrayElements(roadGraphNodes, &copy);
	for (int i = 0; i < len1; i += 2) {
		points.push_back(new MapPoint{ nodes[i], nodes[i + 1], new int[cache_size] });
	}
	env->ReleaseIntArrayElements(roadGraphNodes, nodes, JNI_ABORT);
	jint* lines = env->GetIntArrayElements(roadGraphLines, &copy);
	for (int i = 0; i < len2; i += 3) {
		root->add(RoadGraphLine{ points[lines[i]], points[lines[i + 1]], RoadType(lines[i + 2]) });
	}
	env->ReleaseIntArrayElements(roadGraphLines, lines, JNI_ABORT);
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

JNIEXPORT jbyteArray JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_drawOpenGLTile
(JNIEnv *env, jobject obj, jint x, jint y, jint z, jint max_dist, jint ind, jint proc_ind) {
	int mult = 1 << (server_zoom - z);
	std::vector<RoadGraphLine> rgls = getLinesInSquare(new int[4]{ x * 256 * mult, y * 256 * mult, (x + 1) * 256 * mult, (y + 1) * 256 * mult }, root);
	std::vector<int> bundle;
	int secondary_stroke =		(int)(65.0f / mult * 100 + 0.5f);
	int primary_stroke =		(int)(75.0f / mult * 100 + 0.5f);
	int tertiary_stroke =		(int)(60.0f / mult * 100 + 0.5f);
	int service_stroke =		(int)(25.0f / mult * 100 + 0.5f);
	int residential_stroke =	(int)(50.0f / mult * 100 + 0.5f);
	int living_stroke =			(int)(25.0f / mult * 100 + 0.5f);
	int default_stroke =		(int)(20.0f / mult * 100 + 0.5f);
	int unknown_stroke =		(int)(10.0f / mult * 100 + 0.5f);

	int stroke = 100;
	bool dont_draw = false;
	int offx = x * mult * 256;
	int offy = y * mult * 256;

	for (RoadGraphLine &rgl : rgls) {
		switch (rgl.type) {
		case SECONDARY:
			if (z > zoom_level) {
				stroke = secondary_stroke;
			}
			else {
				stroke = (int)(160.0f / mult * 100);
			}
			break;
		case RESIDENTIAL:
			if (z > zoom_level) {
				stroke = residential_stroke;
			}
			else {
				dont_draw = true;
			}
			break;
		case SERVICE:
			if (z > zoom_level) {
				stroke = service_stroke;
			}
			else {
				dont_draw = true;
			}
			break;
		case TERTIARY:
			if (z > zoom_level) {
				stroke = tertiary_stroke;
			}
			else {
				stroke = tertiary_stroke;
			}
			break;
		case PRIMARY:
			if (z > zoom_level) {
				stroke = primary_stroke;
			}
			else {
				stroke = (int)(160.0f / mult * 100);
			}
			break;
		case TRUNK:
			stroke = (int)(160.0f / mult * 100);
			break;
		case DEFAULT:
			if (z > zoom_level) {
				stroke = default_stroke;
			}
			else {
				dont_draw = true;
			}
			break;
		case LIVING_STREET:
			if (z > zoom_level) {
				stroke = living_stroke;
			}
			else {
				dont_draw = true;
			}
			break;
		case SUBWAY:
			dont_draw = true;
			break;
		case TRAM:
			dont_draw = true;
			break;
		case BUS:
			dont_draw = true;
			break;
		case TROLLEYBUS:
			dont_draw = true;
			break;
		case INVISIBLE:
			dont_draw = true;
			break;
		default:
			if (z > zoom_level) {
				stroke = unknown_stroke;
			}
			else {
				dont_draw = true;
			}
			break;
		}
		if (!dont_draw) {
			bundle.push_back(rgl.p1->x - offx); 
			bundle.push_back(rgl.p1->y - offy);
			bundle.push_back(rgl.p1->dist[ind]);
			bundle.push_back(rgl.p2->x - offx);
			bundle.push_back(rgl.p2->y - offy);
			bundle.push_back(rgl.p2->dist[ind]);
			bundle.push_back(stroke);
			//lines++;
		}
		else {
			dont_draw = false;
		}

	}

	tile_data td{ bundle.size(), mult, z, max_dist };
	((tile_data*)out_buffers[proc_ind])[0] = td;
	memcpy(out_buffers[proc_ind] + sizeof(tile_data), &bundle[0], bundle.size() * 4);
	SetEvent(events[proc_ind]);
	WaitForSingleObject(events[proc_ind], INFINITE);
	int out_len = ((int*)out_buffers[proc_ind])[0];
	jbyteArray out_arr = env->NewByteArray(out_len);
	env->SetByteArrayRegion(out_arr, 0, out_len, (jbyte *)(out_buffers[proc_ind] + 4));
	return env->NewByteArray(42);
}

JNIEXPORT void JNICALL Java_net_zettroke_PropertyHeatMapServer_utils_RoadGraphDrawer_updateOpenGLDistances
(JNIEnv *env, jobject obj, jintArray arr, jint len, jint ind) {
	jboolean copy = false;
	jint* dsts = env->GetIntArrayElements(arr, &copy);
	for (int i = 0; i < points.size(); i++) {
		points[i]->dist[ind] = dsts[i];
	}
	env->ReleaseIntArrayElements(arr, dsts, JNI_ABORT);
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