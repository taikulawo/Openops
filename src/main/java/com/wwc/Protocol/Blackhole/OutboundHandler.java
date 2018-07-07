package com.wwc.Protocol.Blackhole;

import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;

public class OutboundHandler implements Outbound {
    @Override
    public void process(Buffer data, SocketAddress addr, Handler<Buffer> handler, Inbound in) {
        //drop everything
    }

    @Override
    public void close() {

    }
}
