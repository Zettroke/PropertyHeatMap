package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;

import java.util.Map;

public class Jsonizer {
    public static JsonObject toJson(Way way, boolean points) {
        JsonObject answer = new JsonObject();
        JsonObject data = new JsonObject();
        for (Map.Entry<String, String> p : way.data.entrySet()) {
            data.add(p.getKey(), p.getValue());
        }
        answer.add("data", data);
        answer.add("id", way.id);
        if (points) {
            JsonArray pointsArr = new JsonArray();
            for (MapPoint p : way.nodes) {
                JsonArray point = new JsonArray();
                point.add(p.x);
                point.add(p.y);
                pointsArr.add(point);
            }
            answer.add("points", pointsArr);
        }
        return answer;
    }
}
