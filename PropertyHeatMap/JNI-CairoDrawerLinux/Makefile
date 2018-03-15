CXX=g++

all: JNI-CairoDrawer

JNI-CairoDrawer: main.o
	g++ draw.o -o JNI-CairoDrawer.so -lcairo -lpixman-1 -lpng -lz -lpthread -lfreetype -shared -lc 

main.o: 
	g++ -Iinclude -I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux -std=c++11 -c draw.cpp -fPIC -o draw.o
