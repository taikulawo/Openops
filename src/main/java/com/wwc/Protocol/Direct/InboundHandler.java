package com.wwc.Protocol.Direct;

import com.wwc.Main;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Socket.SocketCallback;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;

public class InboundHandler extends SocketCallback implements Inbound {
    private HashMap<String,Object> config = Main.instance.configManager.getSpecInboundFromTag("direct");
    private Outbound outbound;
    private String sendTo;

    public InboundHandler() {
        super("Direct/InboundHandler");
    }


    @Override
    public void process(NetSocket socket) {
        this.socket = socket;
        socket.handler(this::handleOnRead)
                .exceptionHandler(this::handleOnException)
                .endHandler(this::handleOnEnd)
                .drainHandler(this::handleOnDrain)
                .closeHandler(this::handleOnClose);
        sendTo = (String)config.get("sendTo");
        try {
            outbound = Main.instance.dispatcher.dispatchToOutbound(sendTo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

    }

    @Override
    protected void handleOnRead(Buffer data) {
        //outbound.process(data);
    }


    @Override
    protected void handleOnEnd(Void v) {

    }

    @Override
    protected void handleOnException(Throwable t) {

    }

    @Override
    protected void handleOnClose(Void v) {

    }
}
