package com.wwc.Protocol.Socks;

import com.wwc.Crypto.IEncryptor;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;

public class OutboundHandler implements Outbound {

    private IEncryptor encryptor;
    private IEncryptor decryptor;
    public OutboundHandler(){   }


    @Override
    public void close() {

    }



    @Override
    public void process(Buffer data, SocketAddress addr, Handler<Buffer> handler, Inbound in) {

    }
}
