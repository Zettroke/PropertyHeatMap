package net.zettroke.PropertyHeatMapServer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class BoolArrayPool {
    static int array_max_size = 0;

    final static ArrayList<boolean[]> arrays = new ArrayList<>();

    public static boolean[] getArray(int size){
        synchronized (arrays) {
            if (array_max_size < size || arrays.size() == 0) {
                boolean[] arr = new boolean[size];
                return arr;
            } else {
                boolean[] arr;
                if (arrays.size() == 1) {
                    arr = arrays.get(0);
                    arrays.remove(0);
                    return arr;
                } else {
                    boolean[] curr;
                    for (int i = 0; i < arrays.size(); i++) {
                        curr = arrays.get(i);
                        if (curr.length < size) {
                            if (i - 1 == 0) {
                                array_max_size = arrays.get(1).length;
                            }
                            arr = arrays.get(i - 1);
                            arrays.remove(i - 1);
                            return arr;
                        }else if(curr.length == size){
                            if (i == 0) {
                                array_max_size = arrays.get(1).length;
                            }
                            arr = arrays.get(i);
                            arrays.remove(i);
                            return arr;
                        }
                    }
                }
            }
            return new boolean[size];
        }
    }

    public static void returnArray(boolean[] arr){
        Arrays.fill(arr, false);
        synchronized (arrays) {
            if (arr.length > array_max_size){
                array_max_size = arr.length;
                arrays.add(0, arr);
            }else{
                arrays.add(arr);
                arrays.sort(new Comparator<boolean[]>() {
                    @Override
                    public int compare(boolean[] o1, boolean[] o2) {
                        return o2.length - o1.length;
                    }
                });
            }
        }
    }

}
