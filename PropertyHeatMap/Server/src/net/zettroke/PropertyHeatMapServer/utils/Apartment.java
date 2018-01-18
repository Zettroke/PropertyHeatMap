package net.zettroke.PropertyHeatMapServer.utils;

import com.eclipsesource.json.JsonObject;

public class Apartment {
    public int price;
    public double area;
    public int floor;
    public int max_floor;

    JsonObject data = new JsonObject();

    public Apartment(int price, double area, int floor, int max_floor, JsonObject data) {
        this.price = price;
        this.area = area;
        this.floor = floor;
        this.max_floor = max_floor;
        this.data = data;
    }
}
