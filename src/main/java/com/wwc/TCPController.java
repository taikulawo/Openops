package com.wwc;

/*
*           ------ outbound
*
* inbound   ------ outbound
*
*           ------ outbound
* */


import com.wwc.Protocol.Inbound;
import com.wwc.Utils.ConfigManager;
import com.wwc.Utils.SpecConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TCPController extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(TCPController.class);
    private static final int MAX_CONNECTIONS = 1024;

    private Vertx vertx ;
    private Instance instance;

    private NetServer netServer;
    private ConfigManager configManager;

    private HashMap<String,Object> params ;
    private HashMap<String,Object> config;

    private String sendTo;
    private int port;
    private String inboundName;

    private Inbound inbound = null;


    public TCPController( HashMap c){
        this.instance = Main.instance;
        this.config = c;
        this.vertx = instance.vertx;
        this.netServer = vertx.createNetServer(getNetServerOptions());
        this.configManager = instance.configManager;
        this.params = this.instance.configManager.params;

        this.sendTo = (String)config.get("sendTo");
        this.port = (int) config.get("port");
        this.inboundName = (String)config.get("tag");

    }

    @Override
    public void start(Future<Void> startFuture){
        netServer.connectHandler(netSocket ->{
            if(inbound == null){
                try {
                    inbound = instance.dispatcher.dispatchToInbound(inboundName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            Inbound in = inbound.getInstance();

            assert in != inbound;
            in.process(netSocket);
        }).listen(port,"127.0.0.1",ar ->{
            if(ar.succeeded()){
                log.info("TCPServer listen at: [{}]",port);
            }else{
                log.error("error when listen at port: [{}], Exception",port,ar.cause());
            }
        });
    }

    private NetServerOptions getNetServerOptions(){
        return new NetServerOptions().setTcpNoDelay(true)
                .setReuseAddress(true)
                .setReusePort(true)
                .setLogActivity(true)
                .setReceiveBufferSize(2048);
    }


}
