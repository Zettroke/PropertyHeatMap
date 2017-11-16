package net.zettroke.PropertyHeatMapServer.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PathRouter {
    private HashMap<String, PathRouter> routes = new HashMap<>();
    private ShittyHttpHandler handler = null;
    private ShittyHttpHandler error_handler = null;

    public void addPath(String path, ShittyHttpHandler handler){
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
            if (!routes.containsKey(l.get(0))){
                PathRouter pr = new PathRouter();
                pr.setErrorHandler(error_handler);
                routes.put(l.get(0), pr);
                pr.addPath0(l.subList(1, l.size()), handler);
            }
        }
    }

    public void setErrorHandler(ShittyHttpHandler handler){
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


}
