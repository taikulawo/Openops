package com.wwc.Dispatch;

import com.wwc.Dispatch.Factory.InboundFactory;
import com.wwc.Dispatch.Factory.InboundManager;
import com.wwc.Dispatch.Factory.OutboundFactory;
import com.wwc.Dispatch.Factory.OutboundManager;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//dispatcher can dispatch inbound and outbound

public class Dispatcher {
    private static Logger log = LoggerFactory.getLogger(Dispatcher.class);

    public InboundManager inManager = new InboundManager();
    public OutboundManager outManager = new OutboundManager();

    //all inbound or outbounds will create through InboundFactory or OutboundFactory
    public Inbound dispatchToInbound(String sendTo)
            throws Exception {
        log.debug("Dispatcher to Inbound");
        return inManager.create(sendTo);
    }


    public Outbound dispatchToOutbound(String sendTo)
            throws Exception {

        return outManager.create(sendTo);
    }

}
