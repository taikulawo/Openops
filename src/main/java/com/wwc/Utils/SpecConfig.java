package com.wwc.Utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * one inbound to all of outbounds
 */
public class SpecConfig {

    public HashMap<String,Object> inbound;
    public ArrayList<HashMap<String,Object>> outbounds;

    public SpecConfig(HashMap inbound, ArrayList outbounds){
        this.inbound = inbound;
        this.outbounds = outbounds;
    }
}
