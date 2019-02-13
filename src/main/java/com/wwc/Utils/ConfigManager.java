package com.wwc.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;



public class ConfigManager {

    //client's or server side's all inbounds and outbounds
    private ArrayList<HashMap<String,Object>> inbounds = new ArrayList<>();
    private ArrayList<HashMap<String,Object>> outbounds = new ArrayList<>();

    private HashMap<String,HashMap<String,Object>> nameToInbounds = new HashMap<>();
    private HashMap<String,HashMap<String,Object>> nameToOutbounds = new HashMap<>();

    //key 是每一个inbound的tag,区分大小写，SpecConfig一个inbound对应全部的outbound.
    private HashMap<String,SpecConfig> specMainControllersConfig = new HashMap<>();

    private boolean isLocal ;

    public HashMap<String,Object> params;


    public ConfigManager(HashMap<String,Object> params){
        this.params = params;
        load();

        inbounds.forEach(action ->{
            nameToInbounds.put((String) action.get("tag"),action);
        });

        outbounds.forEach(action ->{
           nameToOutbounds.put((String)action.get("tag"),action);
        });
    }

    public HashMap getInboundsWithTags(){
        return nameToInbounds;
    }

    public HashMap getOutboundsWithTags(){
        return nameToOutbounds;
    }

    public HashMap getAllSpecConfigs(){
        return this.specMainControllersConfig;
    }

    public void load(){
        String path = (String)params.get("--config");
        File file = new File(path);
        JSONObject ConfigJsonObject = null;
        try {
            String configString = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            ConfigJsonObject = new JSONObject(configString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert ConfigJsonObject != null;
        HashMap jsonMap = (HashMap<String,Object>)ConfigJsonObject.toMap();
        boolean local = (Boolean)jsonMap.get("isLocal");

        if(params.containsKey("--isLocal")){
            isLocal = (Boolean)params.get("--isLocal");
        }

        HashMap side = (HashMap<String,Object>)jsonMap.get(isLocal ? "client" : "server");
        inbounds = (ArrayList)side.get("inbounds");
        outbounds = (ArrayList<HashMap<String,Object>>)side.get("outbounds");


        //Outbounds都是一个实例，更改会影响到其余SpecConfig中的Outbound
        //name must change when pass one special config to TCPController, name use name of TCPController.
        for(HashMap map: inbounds){
            String tag = (String)map.get("tag");
            specMainControllersConfig.put(tag,new SpecConfig(map,outbounds));
        }


    }

    public ArrayList<HashMap<String,Object>> getInbounds(){
        return this.inbounds;
    }

    public ArrayList<HashMap<String,Object>> getOutbounds(){
        return this.outbounds;
    }

    /**
     * get specConfig, if not exist, null will be return
     * @param tag
     * @return
     */
    public SpecConfig getSpecConfig(String tag){
        if(!specMainControllersConfig.containsKey(tag)){
            return null;
        }
        return specMainControllersConfig.get(tag);
    }

    public HashMap getSpecOutboundFromTag(String tag){
        return getHashMap(tag, outbounds);
    }

    private HashMap getHashMap(String tag, ArrayList<HashMap<String, Object>> outbounds) {
        for(HashMap<String,Object> config : outbounds){
            String name = (String)config.get("tag");
            if(name.equals(tag)){
                return config;
            }
        }
        return null;
    }

    public HashMap getSpecInboundFromTag(String tag){
        return getHashMap(tag, inbounds);
    }
}
