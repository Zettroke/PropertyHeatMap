package net.zettroke.PropertyHeatMapServer;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(sc.nextLine());
        long start = System.nanoTime();
        PropertyMapLoaderOSM propertyMapLoaderOSM = new PropertyMapLoaderOSM("C:/PropertyHeatMap/map.osm");
        propertyMapLoaderOSM.load();
        System.out.println("done in " + (System.nanoTime()-start)/1000000000.0 + "sec.");
        System.out.println(sc.nextLine());


    }
}
