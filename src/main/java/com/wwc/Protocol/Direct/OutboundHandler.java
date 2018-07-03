package com.wwc.Protocol.Direct;

import com.wwc.Main;
import com.wwc.Protocol.IBound;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Socket.SocketCallback;
import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class OutboundHandler extends SocketCallback implements Outbound {
    private Logger log = LoggerFactory.getLogger(OutboundHandler.class);

    private NetClient client;
    private boolean isConnected = false;
    private SocketAddress dst;
    private boolean isFirstRequest = true;
    private Queue<Buffer> queue = new LinkedList<>();
    private NetSocket socket ;

    private Inbound in;
    private Handler<Buffer> handler;

    private boolean isDestroyed = false;

    @Override
    public void process(Buffer data, SocketAddress addr, Handler<Buffer> handler, Inbound in) {
        if(isFirstRequest){
            isFirstRequest = false;
            this.in = in;
            this.handler = handler;
            dst = addr;
            init();
        }

        if(!isConnected){
            queue.offer(data);
            return;
        }

        socket.write(data);
    }

    private void init(){
        client = Main.getVertx().createNetClient(getNetClientOptions());
        client.connect(dst,res ->{
           if(res.succeeded()){
               isConnected = true;
               log.info("Connected to [{}:{}]",dst.host(),dst.port());
               socket = res.result();
               socket.handler(this::handleOnRead)
                       .exceptionHandler(this::handleOnException)
                       .endHandler(this::handleOnEnd)
                       .drainHandler(this::handleOnDrain)
                       .closeHandler(this::handleOnClose);


               queue.forEach( data ->{
                   socket.write(data);
               });
               queue.clear();

           }else{
               log.debug("cannot connect to [{}:{}]",dst.host(),dst.port(),res.cause());
           }
        });
    }

    @Override
    public void close() {
        socket.close();
        isDestroyed = true;
    }

    private NetClientOptions getNetClientOptions(){
        NetClientOptions option = new NetClientOptions();
        option.setLogActivity(true)
                .setReconnectAttempts(2)
                .setTcpKeepAlive(true)
                .setReuseAddress(true)
                .setIdleTimeout(300)
                .setReusePort(true);
        return option;
    }


    private void destroyHandler(){
        if(isDestroyed){
           log.debug("already  been destroyed");
            return;
        }
        in.close();
        close();
    }

    @Override
    protected void handleOnRead(Buffer data) {
        handler.handle(data);
    }

    @Override
    protected void handleOnDrain(Void v) {
        log.debug("DrainHandler");
        close();
    }

    @Override
    protected void handleOnEnd(Void v) {
        in.tell(END_ACTION,v);
        close();
    }

    @Override
    protected void handleOnException(Throwable t) {
        log.debug("[{}]",t);
        close();
        in.tell(EXCEPTION_ACTION,t);
    }

    @Override
    protected void handleOnClose(Void v) {
        tell(IBound.CLOSE_ACTION,v);
        close();
    }
}
