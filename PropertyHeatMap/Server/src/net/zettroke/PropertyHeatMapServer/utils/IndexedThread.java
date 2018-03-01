package net.zettroke.PropertyHeatMapServer.utils;

public class IndexedThread extends Thread{
    public int index;
    IndexedThread(Runnable r){
        super(r);
    }
}
