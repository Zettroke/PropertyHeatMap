package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.Way;

import java.util.Map;

public class Jsonizer {
    public static JsonObject toJson(Way way, boolean points) {
        JsonObject answer = new JsonObject();
        JsonObject data = new JsonObject();
        for (Map.Entry<String, String> p : way.data.entrySet()) {
            data.add(p.getKey(), p.getValue());
        }

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
        if (way.apartments != null) {
            JsonArray apartments = new JsonArray();
            for (Apartment apartment : way.apartments){
                JsonObject apartObj = new JsonObject();
                apartObj.add("price", apartment.price);
                apartObj.add("area", apartment.area);
                apartObj.add("floor", apartment.floor);
                apartObj.add("max_floor", apartment.max_floor);
                apartObj.add("full data", apartment.data);
                apartments.add(apartObj);
            }
            data.add("apartments", apartments);
        }
        answer.add("data", data);
        MapPoint center = way.getCenter();
        answer.add("center", new JsonArray().add(center.x).add(center.y));


        return answer;
    }
}
