package net.zettroke.PropertyHeatMapServer.utils;

public class TimeMeasurer {
    public static void printMeasure(long t1, String format){
        double t = (System.nanoTime()-t1)/1000000.0;
        if (format.contains("%t")) {
            System.out.println(format.replace("%t", "" + t));
        }else{
            System.out.println(format + t);
        }

    }

    public static void printMeasure(String format, long t1){
        printMeasure(t1, format);
    }
}
