package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ParamsChecker {
    private class Range{
        Object begin;
        Object end;
        boolean isDummy = false;

        public Range(Object begin, Object end) {
            this.begin = begin;
            this.end = end;

        }

        public Range(boolean isDummy) {
            this.isDummy = isDummy;
        }
    }

    private static class IntegerTypeClass implements CheckValueType{
        @Override
        public boolean isValid(String s) {
            try{
                Integer.parseInt(s);
                return true;
            }catch (Exception e){
                return false;
            }
        }

        @Override
        public String getName() {
            return "Integer";
        }

        @Override
        public int compare(String o1, Object o2) {
            return Integer.compare(Integer.parseInt(o1), (Integer)o2);
        }
    }

    public static IntegerTypeClass IntegerType = new IntegerTypeClass();

    private static class LongTypeClass implements CheckValueType{
        @Override
        public boolean isValid(String s) {
            try{
                Long.parseLong(s);
                return true;
            }catch (Exception e){
                return false;
            }
        }

        @Override
        public int compare(String o1, Object o2) {
            return Long.compare(Long.parseLong(o1), (Long)o2);
        }

        @Override
        public String getName() {
            return "Long";
        }
    }

    public static LongTypeClass LongType = new LongTypeClass();

    private static class BooleanTypeClass implements CheckValueType{
        @Override
        public boolean isValid(String s) {
            return s.toLowerCase().equals("true") || s.toLowerCase().equals("false");
        }

        @Override
        public int compare(String o1, Object o2) {
            return Boolean.compare(Boolean.parseBoolean(o1), (Boolean)o2);
        }

        @Override
        public String getName() {
            return "Boolean";
        }
    }

    public static BooleanTypeClass BooleanType = new BooleanTypeClass();

    private static class DoubleTypeClass implements CheckValueType{
        @Override
        public boolean isValid(String s) {
            try{
                Double.parseDouble(s);
                return true;
            }catch (Exception e){
                return false;
            }
        }

        @Override
        public int compare(String o1, Object o2) {
            return Double.compare(Double.parseDouble(o1), (Double)o2);
        }

        @Override
        public String getName() {
            return "Double";
        }
    }

    public static DoubleTypeClass DoubleType = new DoubleTypeClass();

    private static class StringTypeClass implements CheckValueType{
        @Override
        public boolean isValid(String s) {
            return true;
        }

        @Override
        public int compare(String o1, Object o2) {
            return o1.compareTo((String)o2);
        }

        @Override
        public String getName() {
            return "String";
        }
    }

    public static StringTypeClass StringType = new StringTypeClass();

    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<CheckValueType> types = new ArrayList<>();
    private ArrayList<Range> ranges = new ArrayList<>();
    //private HashMap<String, Integer> indexes;

    private ArrayList<String> miss_name = new ArrayList<>();
    private ArrayList<String> incorrect_type = new ArrayList<>();
    private ArrayList<String> out_of_range = new ArrayList<>();

    public ParamsChecker addName(String name){
        names.add(name);
        return this;
    }

    public ParamsChecker addRange(Object start, Object end){
        ranges.add(new Range(start, end));
        return this;
    }

    public ParamsChecker addNoRange(){
        ranges.add(new Range(true));
        return this;
    }

    public ParamsChecker addType(CheckValueType type){
        types.add(type);
        return this;
    }

    public boolean isValid(QueryStringDecoder decoder){
        boolean correct = true;
        miss_name.clear();incorrect_type.clear();out_of_range.clear();
        for (int i=0; i<names.size(); i++){
            CheckValueType curr = types.get(i);
            if (decoder.parameters().containsKey(names.get(i))){
                String val = decoder.parameters().get(names.get(i)).get(0);
                if (i < types.size()){
                    if (!curr.isValid(val)){
                        correct = false;
                        incorrect_type.add(names.get(i) + "=" + val + "(need " + curr.getName() + ")");
                    }else{
                        if (i < ranges.size()){
                            Range currR = ranges.get(i);
                            if (!(ranges.get(i).isDummy || (curr.compare(val, currR.begin) >= 0 && 0 >= curr.compare(val, currR.end)))){
                                correct = false;
                                out_of_range.add(names.get(i) + "=" + val + " [" + currR.begin.toString() + ";" + currR.end.toString() + "]");
                            }
                        }
                    }
                }
            }else{
                miss_name.add(names.get(i));
                correct = false;
            }
        }


        return correct;
    }

    public JsonObject getErrorMessage(){
        JsonObject err = new JsonObject();
        String divid = "";
        if (miss_name.size() != 0){
            JsonArray p = new JsonArray();

            for (String s: miss_name){
                p.add(s);
            }
            err.add("Missing params", p);
        }

        if (incorrect_type.size() != 0){
            JsonArray p = new JsonArray();

            for (String s: incorrect_type){
                p.add(s);
            }
            err.add("Incorrect types", p);
        }

        if (out_of_range.size() != 0){
            JsonArray p = new JsonArray();

            for (String s: out_of_range){
                p.add(s);
            }
            err.add("Out of range", p);
        }

        return err;
    }
}


