package net.zettroke.PropertyHeatMapServer.utils;

public interface CheckValueType {
    boolean isValid(String s);

    int compare(String o1, Object o2);

    String getName();

}
