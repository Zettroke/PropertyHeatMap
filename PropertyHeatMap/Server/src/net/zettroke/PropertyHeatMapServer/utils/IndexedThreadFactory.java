package net.zettroke.PropertyHeatMapServer.utils;

import java.util.concurrent.ThreadFactory;

public class IndexedThreadFactory implements ThreadFactory{
    private int index = 0;
    @Override
    public Thread newThread(Runnable r) {
        IndexedThread t = new IndexedThread(r);
        t.index = index++;
        return t;
    }
}
