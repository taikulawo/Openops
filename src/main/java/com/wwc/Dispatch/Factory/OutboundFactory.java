package com.wwc.Dispatch.Factory;

import com.wwc.Main;
import com.wwc.Protocol.Outbound;

import java.util.ArrayList;
import java.util.HashMap;

public class OutboundFactory {
    private HashMap<String,HashMap<String,Object>> configs  = new HashMap<>();
    private HashMap<String,Class<?>> classObjects = new HashMap<>();

    private String packageUrl = "com.wwc.Protocol.";

    private HashMap<String,ProtocolInfo> registered = new HashMap<>();


    public OutboundFactory(){
        ArrayList<HashMap<String,Object>> configLists = Main.configManager.getOutbounds();
        for(HashMap<String,Object> config : configLists){
            String tag = (String)config.get("tag");
            configs.put(tag.toUpperCase(),config);
        }

        initRegisteredProtocol();
    }

    private void initRegisteredProtocol(){
        ProtocolInfo socks = new ProtocolInfo("socks","Socks","com.wwc.Protocol.Socks.OutboundHandler");
        ProtocolInfo direct = new ProtocolInfo("direct","Direct","com.wwc.Protocol.Direct.OutboundHandler");
        ProtocolInfo openops = new ProtocolInfo("openops","Openops","com.wwc.Protocol.Openops.OutboundHandler");
        registered.put("SOCKS",socks);
        registered.put("DIRECT",direct);
        registered.put("OPENOPS",openops);
    }

    private Outbound createFromRegistered(ProtocolInfo info){
        String url = info.completedPackageUrl;
        try {
            Class<?> clazz = Class.forName(url);
            classObjects.put(info.tag.toUpperCase(),clazz);
            return (Outbound)clazz.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Outbound getInstance(String tag)
            throws Exception {

        String up = tag.toUpperCase();

        assert configs.containsKey(up) : "cannot find config!";

        if(classObjects.containsKey(up)){
            Class<?> clazz = classObjects.get(up);
            return createInstance(clazz);
        }else{
            ProtocolInfo info = registered.get(up);
            if(info != null){
                return createFromRegistered(info);
            }
            HashMap<String,Object> specConfig = configs.get(up);

            String packageName = (String)specConfig.get("name");
            String main = (String)specConfig.get("main");
            StringBuilder builder = new StringBuilder();
            builder.append(packageUrl).append(packageName).append("." + main);

            Class<?> clazz = Class.forName(builder.toString());
            classObjects.put(up,clazz);
            return createInstance(clazz);
        }

    }

    private Outbound createInstance(Class<?> clazz)
            throws Exception {

        try {
            return (Outbound)clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    private class ProtocolInfo{
        String tag ;
        String main;
        String completedPackageUrl;

        public ProtocolInfo(String t, String m, String c){
            this.tag = t;
            this.main = m;
            this.completedPackageUrl = c;
        }
    }
}
