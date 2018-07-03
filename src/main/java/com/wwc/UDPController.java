package com.wwc;

import com.wwc.Protocol.Inbound;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class UDPController extends AbstractVerticle {
    private static Logger log = LoggerFactory.getLogger(UDPController.class);

    private String controllerName;
    private HashMap<String,Object> config;

    private DatagramSocket udpSocket;

    private int port;
    private String listenAddr;

    public UDPController(String controllerName, HashMap<String,Object> config){
        this.controllerName = controllerName;
        this.config = config;
        this.port = (Integer)config.get("port");
        this.listenAddr = (String)config.get("listen");
    }

    @Override
    public void start(Future<Void> startFuture){
        initUdpSocket();
        startFuture.complete();
    }

    private void initUdpSocket(){
        udpSocket = Main.getVertx().createDatagramSocket(getUdpSocketConfig());
        udpSocket.listen(port,listenAddr,(result) ->{
            DatagramSocket sock = result.result();
            if(result.succeeded()){
                try {
                    Inbound inbound = Main.instance.dispatcher.dispatchToInbound((String)config.get("sendto"));
//                    inbound.process(socket);
                } catch (Exception e) {
                    log.debug("[{}]",e);
                    e.printStackTrace();
                }
            }else{
                log.debug("failed: [{}]",result.cause());
            }

        });
    }

    private DatagramSocketOptions getUdpSocketConfig(){
        DatagramSocketOptions option = new DatagramSocketOptions()
                .setLogActivity(true)
                .setReuseAddress(true)
                .setReusePort(true);
        return option;

    }
}
