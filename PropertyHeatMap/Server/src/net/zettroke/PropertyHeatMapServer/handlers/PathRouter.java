package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import net.zettroke.PropertyHeatMapServer.map.PropertyMap;
import sun.reflect.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PathRouter {
    private static ArrayList<Class<? extends ShittyHttpHandler>> handlers = new ArrayList<>();
    private HashMap<String, PathRouter> routes = new HashMap<>();
    private ShittyHttpHandler handler = null;
    private static ShittyHttpHandler error_handler = null;

    public static PropertyMap propertyMap;

    private void addPath(ShittyHttpHandler handler){
        String path = handler.getPath();
        if (path.length() > 0 && path.charAt(0) == '/'){
            path = path.substring(1);
        }
        String[] l = path.split("/");

        addPath0(Arrays.asList(l), handler);


    }

    private void addPath0(List<String> l, ShittyHttpHandler handler){
        if (l.size() == 0){
            this.handler = handler;
        }else{
            PathRouter pr;
            if (!routes.containsKey(l.get(0))){
                pr = new PathRouter();
                routes.put(l.get(0), pr);

            }else{
                pr = routes.get(l.get(0));
            }
            pr.addPath0(l.subList(1, l.size()), handler);
        }
    }

    public static void setErrorHandler(ErrorHandler handler){
       error_handler = handler;
    }

    public ShittyHttpHandler getHandler(String path){
        if (path.charAt(0) == '/'){
            path = path.substring(1);
        }
        int ind = path.lastIndexOf('?');
        if (ind != -1){
            path = path.substring(0, ind);
        }
        String[] l = path.split("/");
        return getHandler0(Arrays.asList(l));
    }

    private ShittyHttpHandler getHandler0(List<String> l){
        if (l.size() == 0 || l.get(0).isEmpty()){
            if (handler != null){
                return handler;
            }else{
                return error_handler;
            }
        }else{
            PathRouter pt = routes.get(l.get(0));
            if (pt != null){
                return pt.getHandler0(l.subList(1, l.size()));
            }else{
                return error_handler;
            }
        }
    }

    public static PathRouter getPathRouter(PropertyMap propertyMap){
        PathRouter pr = new PathRouter();
        pr.addPath(new MapPointSearchHandler(propertyMap));
        pr.addPath(new DrawerHandler());
        pr.addPath(new MapCircleSearchHandler(propertyMap));
        pr.addPath(new PriceTileHandler(propertyMap));
        pr.addPath(new RoadGraphTileHandler(propertyMap));
        pr.addPath(new StringSearchHandler(propertyMap));
        setErrorHandler(new ErrorHandler());

        return pr;

    }


}
