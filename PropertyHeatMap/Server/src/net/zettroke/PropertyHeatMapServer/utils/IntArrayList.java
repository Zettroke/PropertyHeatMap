package net.zettroke.PropertyHeatMapServer.utils;

import java.util.Arrays;
import java.util.Iterator;

// Сохраняет память, т.к. Integer весит 20 байт а int 4 байта. Также из-за отсутствия проверок увеличилась скорость инициализации.
public class IntArrayList implements Iterable<Integer>{
    private int[] container;

    private int size = 0;

    public void add(int i){
        if (size < container.length){
            container[size] = i;
        }else{
            grow(size+1);
            container[size] = i;
        }
        size++;
    }

    public void addAll(int... ints){
        if (container.length > size + ints.length){
            System.arraycopy(ints, 0, container, size, ints.length);
            size += ints.length;
        }else{
            for (int i: ints){
                add(i);
            }
        }

    }

    private void grow(int minSize){
        int newSize = container.length + (container.length >> 1);
        if (newSize < minSize){
            newSize = minSize;
        }
        container = Arrays.copyOf(container, newSize);
    }

    public void shrink(){
        container = Arrays.copyOf(container, size);
    }

    public int size(){
        return size;
    }

    public Iterator<Integer> iterator(){
        return new Iterator<Integer>() {
            int ind = 0;
            @Override
            public boolean hasNext() {
                return ind < size;
            }

            @Override
            public Integer next() {
                return container[ind++];
            }
        };
    }

    public int get(int ind){
        return container[ind];
    }

    public int[] toArray(){
        return Arrays.copyOf(container, size);
    }

    public IntArrayList(){
        this(10);
    }

    public IntArrayList(int capacity){
        container = new int[capacity];
    }


}
