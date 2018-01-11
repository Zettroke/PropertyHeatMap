package net.zettroke.PropertyHeatMapServer.utils;

import java.util.HashMap;

public enum RoadType {
    FOOTWAY,
    SECONDARY,
    LIVING_STREET,
    RESIDENTIAL,
    SERVICE,
    CONSTRUCTION,
    TERTIARY,
    PRIMARY,
    PATH,
    TRUNK,
    DEFAULT;


    public static RoadType getType(HashMap<String, String> data){
        if (data.containsKey("living_street")){
            return RoadType.LIVING_STREET;
        }
        String s = data.get("highway");
        switch (s) {
            case "secondary":
                return SECONDARY;

            case "secondary_link":
                return SECONDARY;

            case "footway":
                return FOOTWAY;

            case "construction":
                return CONSTRUCTION;

            case "residential":
                return RESIDENTIAL;

            case "service":
                return SERVICE;

            case "tertiary":
                return TERTIARY;
            case "unclassified":
                return TERTIARY;
            case "tertiary_link":
                return TERTIARY;

            case "primary":
                return PRIMARY;
            case "primary_link":
                return PRIMARY;

            case "path":
                return PATH;

            case "trunk":
                return TRUNK;
            case "trunk_link":
                return TRUNK;

            default:
                return DEFAULT;
        }
    }
}