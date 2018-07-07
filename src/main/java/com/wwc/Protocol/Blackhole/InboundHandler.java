package com.wwc.Protocol.Blackhole;

import com.wwc.Protocol.Inbound;
import io.vertx.core.net.NetSocket;

public class InboundHandler implements Inbound {
    private NetSocket socket;
    @Override
    public void process(NetSocket socket) {
        //drop everything
        this.socket = socket;
    }

    @Override
    public void close() {
        socket.close();
        socket = null;
    }
}
