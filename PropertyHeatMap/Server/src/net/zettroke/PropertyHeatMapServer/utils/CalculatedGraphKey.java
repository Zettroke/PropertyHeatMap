package net.zettroke.PropertyHeatMapServer.utils;

public class CalculatedGraphKey {
    long id;
    int max_dist;
    boolean foot;

    public CalculatedGraphKey(long id, boolean foot, int max_dist) {
        this.id = id;
        this.max_dist = max_dist;
    }

    @Override
    public int hashCode() {
        return (int)((id + max_dist ^ id) ^ (foot?0:1) << 31);
    }

    @Override
    public boolean equals(Object obj) {
        return id == ((CalculatedGraphKey)obj).id && max_dist == ((CalculatedGraphKey)obj).max_dist && ((CalculatedGraphKey)obj).foot == foot;
    }
}
