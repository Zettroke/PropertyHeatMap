package net.zettroke.PropertyHeatMapServer.utils;

import net.zettroke.PropertyHeatMapServer.map.Way;

import java.util.ArrayList;
import java.util.HashMap;

public class StringPredictor {

    HashMap<Character, HashMap> container = new HashMap<>();
    private int size = 0;

    public int size(){
        return size;
    }

    public ArrayList<String> predict(String segment, int number){
        ArrayList<String> res = new ArrayList<>();
        HashMap<Character, HashMap> curr = container;
        char[] val = segment.toLowerCase().toCharArray();

        boolean found = true;
        for (char c: val){
            if (curr.containsKey(c)){
                curr = curr.get(c);
            }else{
                found = false;
                break;
            }
        }
        if (found){
            rec_string_search(segment, curr, res, number);
        }
        return res;
    }

    private void rec_string_search(String seg, HashMap<Character, HashMap> m, ArrayList<String> arr, int max){
        for (char c: m.keySet()){
            if (c == 0){
                arr.add(seg);
            }else if (arr.size() < max){

                rec_string_search(seg+c, m.get(c), arr, max);
            }
        }
    }

    public void add(String s, Way way){
        char[] val = s.toLowerCase().toCharArray();
        HashMap<Character, HashMap> curr = container;
        for (char c: val){
            if (curr.containsKey(c)){
                curr = curr.get(c);
            }else{
                HashMap<Character, HashMap> temp = new HashMap<>();

                curr.put(c, temp);
                curr = temp;
            }
        }
        if (!curr.containsKey((char)0)){
            size++;
        }
        HashMap<Character, Way> m = new HashMap<>();
        m.put((char)0, way);
        curr.put((char)0, m);
    }
}
