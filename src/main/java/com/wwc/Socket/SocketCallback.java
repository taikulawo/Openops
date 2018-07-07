package com.wwc.Socket;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public abstract class SocketCallback {
    private Logger log = LoggerFactory.getLogger(SocketCallback.class);
    protected NetSocket socket;
    protected Queue<Buffer> queue = new LinkedList<>();

    private String id;

    public SocketCallback(String id){
        this.id = id;
    }
    abstract protected void handleOnRead(Buffer data);


    protected void handleOnDrain(Void v){
        log.debug("[{}],handleOnDrainCalled,writeToSocket()",id);
        writeToSocket(null);
    }

    abstract protected void handleOnEnd(Void v);

    abstract protected void handleOnException(Throwable t);

    abstract protected void handleOnClose(Void v);

    protected void writeToSocket(Buffer data){
        if(data != null){
            queue.offer(data);
        }
        while(true){
            Buffer d = queue.peek();
            if(d == null || socket == null || socket.writeQueueFull()){
                break;
            }
            log.debug("[{}],In queue, size: [{}],Write to remote socket, length: [{}]",id,queue.size(),d.length());
            socket.write(d);
            queue.remove();
        }
    }
}