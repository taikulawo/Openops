package com.wwc.Dispatch.Factory;


import com.wwc.Main;
import com.wwc.Protocol.Outbound;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wwc.Utils.Common.getNow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;


//暂时缺少Outbound的超时控制
public class OutboundManager {
    private static Logger log = LoggerFactory.getLogger(OutboundManager.class);
    private static final int CLEAN_TIMEOUT = 5 * 60 * 1000; //5 minutes

    private HashMap<Integer,OutboundInfo> handlers = new HashMap<>();
    private Queue<Outbound> needRemovedHandlers = new LinkedList<>();
    private Vertx vertx = Main.getVertx();

    private OutboundFactory factory = new OutboundFactory();

    public OutboundManager(){
        vertx.setTimer(CLEAN_TIMEOUT,this::handleTimeout);
    }

    public void removeHandler(int hashCode){
        handlers.remove(hashCode);
    }

    public void addHandler(Outbound outbound){
        handlers.put(outbound.hashCode(),new OutboundInfo(outbound));
    }

    public Outbound create(String sendTo)
            throws Exception {

        Outbound out = factory.getInstance(sendTo);
        addHandler(out);
        return out;
    }


    private void handleTimeout(Long l){
        sweepTimeout();
        vertx.setTimer(CLEAN_TIMEOUT,this::handleTimeout);
    }

    private void sweepTimeout(){
        log.debug("start clean outbounds");
    }

    private class OutboundInfo{
        public OutboundInfo(Outbound out){
            this.outbound = out;
        }

        public Outbound outbound;
        public long lastActiveTime = getNow();
    }
}
