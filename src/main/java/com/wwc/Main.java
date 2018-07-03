package com.wwc;

import com.wwc.Utils.ConfigManager;
import com.wwc.Utils.SpecConfig;
import io.vertx.core.Vertx;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static Logger log = LoggerFactory.getLogger(Main.class);
    private static boolean isLocal ;

    private static Vertx vertx;

    private static HashMap<String,Object> params = new HashMap<>();

    public static ConfigManager configManager;
    public static Instance instance;

    public static void main(String[] argv){
        PropertyConfigurator.configure("resources/log4j.properties");

        parseParams(argv);
        configManager = new ConfigManager(params);
        vertx = Vertx.vertx();

        instance = new Instance(vertx,configManager);

        ArrayList<HashMap<String,Object>> inbounds = configManager.getInbounds();

        for(HashMap<String,Object> map : inbounds){
            String tl = (String)map.get("tl");
            if(tl.equals("tcp")){
                startTCPController(map);
            }
        }
    }

    private static void startTCPController(HashMap config){
        TCPController controller = new TCPController(config);
        instance.addMainController(controller);

        vertx.deployVerticle(controller);
    }

    private static void startUDPController(String actualName, SpecConfig config){

    }

    private static void parseParams(String [] p){
            String configPath = null;
        for(int i = 0 ; i < p.length ; ++i){
            if(p[i].equals("--config")){
                params.put("--config",p[i + 1]);
                continue;
            }
            if(p[i].equals("--isLocal")){
                boolean isLocal = p[i+1].equals("true");
                params.put("--isLocal",isLocal);
            }

        }


        String path = (String)params.get("--config");
        if(path == null){
            log.error("cannot find config file");
            System.exit(-1);
        }
    }

    public static Vertx getVertx(){
        return vertx;
    }

}
