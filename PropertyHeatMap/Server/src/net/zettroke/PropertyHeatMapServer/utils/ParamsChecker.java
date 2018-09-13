package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ParamsChecker {
    public class ParamsCheckerException extends Exception{
        public ParamsCheckerException(String s){super(s);}
    }

    private class Param{
        String name;
        CheckValueType type = StringType;
        Range range = new Range(true);

        public Param(String name){this.name = name;}
    }

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

    private boolean currently_adding = false;
    private Param current_param;
    private ArrayList<Param> params = new ArrayList<>();
    private ArrayList<ParamsChecker> checkers = new ArrayList<>();


    private ArrayList<String> miss_name = new ArrayList<>();
    private ArrayList<String> incorrect_type = new ArrayList<>();
    private ArrayList<String> out_of_range = new ArrayList<>();
    private ArrayList<JsonObject> failed_checkers = new ArrayList<>();

    public ParamsChecker addParam(String name){
        //if (currently_adding){
        //    throw new ParamsCheckerException("Can't add new while not finished previous. Call finish() first.");
        //}
        currently_adding = true;
        current_param = new Param(name);
        return this;
    }

    public ParamsChecker range(Object start, Object end){
        //if (!currently_adding){
        //    throw new ParamsCheckerException("Can't set range because there is no param. Call addParam() first.");
        //}
        current_param.range = new Range(start, end);
        return this;
    }

    public ParamsChecker type(CheckValueType type){
        //if (!currently_adding){
        //    throw new ParamsCheckerException("Can't set range because there is no param. Call addParam() first.");
        //}
        current_param.type = type;
        return this;
    }

    public ParamsChecker finish(){
        //if (!currently_adding){
        //    throw new ParamsCheckerException("Can't finish because there is no param to finish. Call addParam() first.");
        //}
        currently_adding = false;
        params.add(current_param);

        return this;
    }

    public ParamsChecker or(ParamsChecker checker){
        this.checkers.add(checker);
        return this;
    }

    public boolean isValid(QueryStringDecoder decoder){
        boolean correct = true;
        miss_name.clear();incorrect_type.clear();out_of_range.clear();failed_checkers.clear();
        for (int i=0; i<params.size(); i++){
            Param par = params.get(i);
            CheckValueType curr = par.type;
            if (decoder.parameters().containsKey(par.name)){
                String val = decoder.parameters().get(par.name).get(0);

                if (!curr.isValid(val)){
                    correct = false;
                    incorrect_type.add(par.name + "=" + val + "(need " + curr.getName() + ")");
                }else{
                    Range currR = par.range;
                    if (!(currR.isDummy || (curr.compare(val, currR.begin) >= 0 && 0 >= curr.compare(val, currR.end)))){
                        correct = false;
                        out_of_range.add(par.name + "=" + val + " [" + currR.begin.toString() + ";" + currR.end.toString() + "]");
                    }
                }
            }else{
                miss_name.add(par.name);
                correct = false;
            }
        }
        if (checkers.size() != 0) {
            boolean or_check = false;
            for (ParamsChecker ch : checkers) {
                if (!ch.isValid(decoder)) {
                    failed_checkers.add(ch.getErrorMessage());
                } else {
                    or_check = true;
                }

            }
            correct &= or_check;
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

        if (failed_checkers.size() != 0){
            JsonArray p = new JsonArray();

            for (JsonObject o: failed_checkers){
                p.add(o);
            }
            err.add("At least one of follow should be correct", p);
        }

        return err;
    }

    public JsonObject createErrorAnswer(){
        JsonObject ans = new JsonObject();
        ans.add("status", "error");
        ans.add("error", this.getErrorMessage());
        return ans;
    }
}


