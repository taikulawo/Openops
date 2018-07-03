package com.wwc;

import com.wwc.Dispatch.Dispatcher;
import com.wwc.Utils.ConfigManager;
import com.wwc.Utils.Timer;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;

import java.util.ArrayList;

public class Instance {
    public ConfigManager configManager = null;
    public DnsClient dnsClient ;
    public Vertx vertx;
    public Dispatcher dispatcher ;
    public ArrayList<TCPController> controllers = new ArrayList<>();
    public Timer timer;
    public Instance(Vertx vertx, ConfigManager configManager){
        this.vertx = vertx;
        this.dnsClient = vertx.createDnsClient();
        this.configManager = configManager;
        this.timer = new Timer(vertx);
        this.dispatcher =  new Dispatcher();
    }

    public void addMainController(TCPController c){
        this.controllers.add(c);
    }


}
