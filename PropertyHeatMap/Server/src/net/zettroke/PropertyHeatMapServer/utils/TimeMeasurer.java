package net.zettroke.PropertyHeatMapServer.utils;

public class TimeMeasurer {
    public static void printMeasure(long t1, String format){
        System.out.println(format.replace("%t", "" + ((System.nanoTime()-t1)/1000000.0)));

    }
}
