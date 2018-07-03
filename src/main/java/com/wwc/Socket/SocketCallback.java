package com.wwc.Socket;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public abstract class SocketCallback {
    protected NetSocket socket;

    public SocketCallback(){
    }
    abstract protected void handleOnRead(Buffer data);


    abstract protected void handleOnDrain(Void v);

    abstract protected void handleOnEnd(Void v);

    abstract protected void handleOnException(Throwable t);

    abstract protected void handleOnClose(Void v);
}
