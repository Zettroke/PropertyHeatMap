package net.zettroke.PropertyHeatMapServer.utils;

public class Apartment {
    public int price;
    public double area;
    public int floor;
    public int max_floor;

    public Apartment(int price, double area, int floor, int max_floor) {
        this.price = price;
        this.area = area;
        this.floor = floor;
        this.max_floor = max_floor;
    }
}
