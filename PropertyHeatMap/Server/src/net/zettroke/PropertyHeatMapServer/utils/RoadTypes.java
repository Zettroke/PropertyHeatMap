package net.zettroke.PropertyHeatMapServer.utils;

import java.util.HashMap;

public enum RoadTypes{
    FOOTWAY,
    SECONDARY,
    LIVING_STREET,
    RESIDENTIAL,
    SERVICE,
    CONSTRUCTION,
    TERTIARY,
    DEFAULT;


    public static RoadTypes getType(HashMap<String, String> data){
        if (data.containsKey("living_street")){
            return RoadTypes.LIVING_STREET;
        }
        String s = data.get("highway");
        switch (s) {
            case "footway":
                return FOOTWAY;

            case "construction":
                return CONSTRUCTION;

            case "residential":
                return RESIDENTIAL;

            case "service":
                return SERVICE;

            case "secondary":
                return SECONDARY;

            case "tertiary":
                return TERTIARY;

            default:
                return DEFAULT;
        }
    }
}