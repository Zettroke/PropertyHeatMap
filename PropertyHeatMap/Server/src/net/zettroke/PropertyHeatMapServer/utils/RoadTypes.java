package net.zettroke.PropertyHeatMapServer.utils;

import java.util.HashMap;

public enum RoadTypes{
    FOOTWAY,
    SECONDARY,
    LIVING_STREET,
    RESIDENTIAL,
    SERVICE,
    CONSTRUCTION,
    DEFAULT;


    public static RoadTypes getType(HashMap<String, String> data){
        if (data.containsKey("living_street")){
            return RoadTypes.LIVING_STREET;
        }
        String s = data.get("highway");
        if (s.equals("residential")){
            System.out.println();
        }
        switch (s) {
            case "footway":
                return RoadTypes.FOOTWAY;

            case "construction":
                return RoadTypes.CONSTRUCTION;

            case "residential":
                return RoadTypes.RESIDENTIAL;

            case "service":
                return RoadTypes.SERVICE;

            case "secondary":
                return RoadTypes.SECONDARY;

            default:
                return RoadTypes.DEFAULT;
        }
    }
}