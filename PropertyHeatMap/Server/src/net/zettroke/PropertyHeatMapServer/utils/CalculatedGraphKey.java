package net.zettroke.PropertyHeatMapServer.utils;

import java.util.Objects;

public class CalculatedGraphKey{
    long id;
    int max_dist;
    boolean foot;

    public CalculatedGraphKey(long id, boolean foot, int max_dist) {
        this.id = id;
        this.max_dist = max_dist;
        this.foot = foot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculatedGraphKey that = (CalculatedGraphKey) o;
        return id == that.id &&
                max_dist == that.max_dist &&
                foot == that.foot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, max_dist, foot);
    }

}
