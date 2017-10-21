package net.zettroke.PropertyHeatMapServer;

import java.io.FileOutputStream;
import java.util.Scanner;
//-XX:+UnlockCommercialFeatures -XX:+FlightRecorder

public class Main {

    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);
        System.out.println(sc.nextLine());
        long start = System.nanoTime();
        PropertyMap m = new PropertyMap();
        PropertyMapLoaderOSM.load(m, "C:/PropertyHeatMap/map.osm");
        System.out.println("done in " + (System.nanoTime()-start)/1000000000.0 + " sec.");
        System.out.println(sc.nextLine());

    }
}
