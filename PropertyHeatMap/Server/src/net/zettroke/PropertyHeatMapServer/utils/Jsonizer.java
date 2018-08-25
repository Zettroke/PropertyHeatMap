package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.zettroke.PropertyHeatMapServer.map.MapPoint;
import net.zettroke.PropertyHeatMapServer.map.Node;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import net.zettroke.PropertyHeatMapServer.map.Way;

import java.util.Map;

public class Jsonizer {
    public static JsonObject toJson(Way way, boolean points, boolean latlon, PropertyMap context) {
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
                if (!latlon) {
                    point.add(p.x);
                    point.add(p.y);
                }else{
                    double[] ltln = context.inverse_mercator(p.x, p.y);
                    point.add(ltln[1]);
                    point.add(ltln[0]);
                }
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
        if (latlon){
            double[] c = context.inverse_mercator(center.x, center.y);
            answer.add("center", new JsonArray().add(c[1]).add(c[0]));
        }else {
            answer.add("center", new JsonArray().add(center.x).add(center.y));
        }


        return answer;
    }
    public static JsonObject toJson(Way way, boolean points){
        return toJson(way, points, false, null);
    }

    public static JsonObject toJson(Node n){
        JsonObject res = new JsonObject();

        res.add("id", n.id);
        res.add("lat", n.lat);
        res.add("lon", n.lon);

        JsonObject data = new JsonObject();
        if (n.data != null) {
            for (Map.Entry<String, String> p : n.data.entrySet()) {
                data.add(p.getKey(), p.getValue());
            }
        }

        res.add("data", data);

        res.add("x", n.x);
        res.add("y", n.y);

        return res;

    }
}
