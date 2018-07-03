package com.wwc.Dispatch.Factory;

import com.wwc.Main;
import com.wwc.Protocol.Inbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;


/**
 * InboundFactory应该让实现Inbound接口的类(A)去创建Inbound，但A的编写者可能只想要一个A的实例，
 * 所以InboundFactory应该保留一个Inbound的实例，然后通过Inbound#getInstance()去获得对象
 * InboundFactory与OutboundFactory应该保证只使用一次new操作符来创建仅一个实例。
 */
public class InboundFactory extends Factory{
    private static Logger log = LoggerFactory.getLogger(InboundFactory.class);

    /**
     * all inbounds configs read from config file.
     */
    private HashMap<String,HashMap<String,Object>> configs;

    private HashMap<String,Class<?>> classObjects = new HashMap<>();
    private HashMap<String,ProtocolInfo> registered  = new HashMap<>();

    private String packageUrl = "com.wwc.Protocol.";

    public InboundFactory(){
        initRegisteredProtocols();
    }

    private void initRegisteredProtocols(){
        ProtocolInfo socks = new ProtocolInfo("socks","Socks","com.wwc.Protocol.Socks.InboundHandler");
        ProtocolInfo direct = new ProtocolInfo("direct","Direct","com.wwc.Protocol.Direct.InboundHandler");
        ProtocolInfo openops = new ProtocolInfo("openops","Openops","com.wwc.Protocol.Openops.InboundHandler");
        registered.put("socks",socks);
        registered.put("direct",direct);
        registered.put("openops",openops);
        configs = Main.configManager.getInboundsWithTags();
    }

    private Inbound createFromRegistered(ProtocolInfo info){
        String url = info.completedPackageUrl;
        try {
            Class<?> clazz = Class.forName(url);
            classObjects.put(info.tag,clazz);
            return (Inbound)clazz.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Inbound getInstance(String tag)
        throws Exception{

        String up = tag;


        assert configs.containsKey(up) : "cannot find config!";

        if(classObjects.containsKey(up)){
            Class<?> clazz = classObjects.get(up);
            Inbound in = create(clazz);
            return in;
        }else{
            ProtocolInfo info = registered.get(up);
            if(info != null){
                return createFromRegistered(info);
            }

            HashMap<String,Object> specConfig = configs.get(up);
            String main = (String)specConfig.get("main");
            String name = (String)specConfig.get("name");
            StringBuilder builder = new StringBuilder();
            builder.append(packageUrl).append(name).append("." + main);
            Class<?> clazz = Class.forName(builder.toString());
            Inbound in =  create(clazz);
            return in;
        }

    }

    private static Inbound create(Class<?> clazz)
            throws Exception {

        try {
            return (Inbound)clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    //tag: ID in configuration.
    //name : package name.
    //completedPackageUrl: used by reflection.
    private class ProtocolInfo{
        public String tag ;
        public String name;
        public String completedPackageUrl;
        public ProtocolInfo(String t,String n,String c){
            this.tag = t;
            this.name = n;
            this.completedPackageUrl = c;
        }
    }

}
