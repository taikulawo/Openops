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

    private Inbound in;
    private Handler<Buffer> handler;

    private boolean isDestroyed = false;

    public OutboundHandler() {
        super("Direct/OutboundHandler");
    }

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

        writeToSocket(data);
    }

    private void init(){
        client = Main.getVertx().createNetClient(getNetClientOptions());
        client.connect(dst,res ->{
           if(res.succeeded()){
               isConnected = true;
               socket = res.result();
               log.info("Connected to [{}:{}], Accept in [{}:{}], remote Address: [{}:{}]",
                       dst.host(),dst.port(),
                       socket.localAddress().host(),socket.localAddress().port()
                       ,socket.remoteAddress().host(),socket.remoteAddress().port());

               socket.handler(this::handleOnRead)
                       .exceptionHandler(this::handleOnException)
                       .endHandler(this::handleOnEnd)
                       .drainHandler(this::handleOnDrain)
                       .closeHandler(this::handleOnClose);


               writeToSocket(null);

           }else{
               log.debug("cannot connect to [{}:{}]",dst.host(),dst.port(),res.cause());
               tell(EXCEPTION_ACTION,res.cause());
               close();
           }
        });
    }

    @Override
    public void close() {
        if(socket!=null)
            socket.close();
        isDestroyed = true;
    }

    private NetClientOptions getNetClientOptions(){
        NetClientOptions option = new NetClientOptions();
        option.setLogActivity(true)
                .setTcpKeepAlive(true)
                .setReconnectAttempts(3)
                .setReconnectInterval(1000);;
        return option;
    }

    @Override
    protected void handleOnRead(Buffer data) {
        log.debug("recv from remote, data length: [{}]",data.length());
        handler.handle(data);
    }

    @Override
    protected void handleOnEnd(Void v) {
        log.debug("remote end");
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
        log.debug("remote closed");
        in.tell(IBound.CLOSE_ACTION,v);
        close();
    }

}
