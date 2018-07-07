package com.wwc.Protocol.Openops;

import com.wwc.Crypto.EncryptorFactory;
import com.wwc.Crypto.IEncryptor;
import com.wwc.Main;
import com.wwc.Protocol.IBound;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Socket.SocketCallback;
import com.wwc.Utils.ByteCircularBuffer;
import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import static com.wwc.Protocol.Openops.Common.processATYP;
import static com.wwc.Protocol.Openops.OpenopsConstVar.LENGTH_FIELD_LEN;
import static com.wwc.Protocol.Openops.OpenopsConstVar.TAG_LEN;
import static com.wwc.Protocol.Openops.ProtocolImpl.runningEncrypt;
import static com.wwc.Utils.Common.getBytesArrayOfShort;
import static com.wwc.Utils.Common.getUnsignedshortFromBytesArray;

public class InboundHandler extends SocketCallback implements Inbound {

    private boolean firstReceived = false;

    private IEncryptor encryptor ;
    private IEncryptor decryptor;

    private ByteCircularBuffer ringBuffer = new ByteCircularBuffer(65535);

    private HashMap<String,Object> config ;

    private static final int Stream_Init = 0;
    private static final int Stream_Running = 1;
    private int stage = Stream_Init ;

    private SocketAddress dst;
    private Outbound outbound;

    private String sendTo;

    public InboundHandler(){
        super("Openops/InboundHandler");
    }

    private Handler<Buffer> handler  = data->{
        ArrayList<Buffer> list = null;
        try {
            list = runningEncrypt(data,encryptor);
            queue.addAll(list);

            writeToSocket(null);
        } catch (Exception e) {
            log.error("",e);
            close();
        }


    };

    @Override
    public void process(NetSocket socket) {
        this.socket = socket;
        init();
    }

    private void init(){
        socket.handler(this::handleOnRead)
                .endHandler(this::handleOnEnd)
                .exceptionHandler(this::handleOnException)
                .drainHandler(this::handleOnDrain);
        config = Main.instance.configManager.getSpecInboundFromTag("openops");
        String passwd = (String) config.get("password");
        String method = (String) config.get("method");

        try {
            encryptor = EncryptorFactory.getEncryptor(method,passwd);
            decryptor = EncryptorFactory.getEncryptor(method,passwd);
        } catch (Exception e) {
            log.debug("error in init encryptor and decryptor ",e);
            e.printStackTrace();
        }

        this.sendTo = (String)config.get("sendTo");
        try {
            this.outbound = Main.instance.dispatcher.dispatchToOutbound(sendTo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if(socket != null)
            socket.close();
        socket = null;
    }

    @Override
    protected void handleOnRead(Buffer data){
        try {
            ringBuffer.put(data.getBytes());
            if(!firstReceived){
                int authHeader = LENGTH_FIELD_LEN + TAG_LEN;
                if(ringBuffer.getAvailableBytes() < authHeader){
                    log.debug("current: [{}], waitting for more data",ringBuffer.getAvailableBytes());
                    return;

                }
                processHeader(authHeader);
                return;
            }
            processRunning();
        } catch (Exception e) {
            log.error("",e);
            close();
        }
    }
    private void processRunning()
            throws Exception {

        int available = ringBuffer.getAvailableBytes();
       int authHeader = LENGTH_FIELD_LEN + TAG_LEN;
       if(available < authHeader){
           log.debug("waitting for more data,current: [{}], need: [{}]",available,authHeader);
           return;
       }
       byte[] len = new byte[LENGTH_FIELD_LEN + TAG_LEN];
       byte[] after = new byte[LENGTH_FIELD_LEN];
       ringBuffer.peek(len);
       decryptor.decrypt(len,0,len.length,after,0);
       int chunkLen = getUnsignedshortFromBytesArray(after,0);
       if(available < chunkLen + authHeader){
           log.debug("waitting for more data");
           return;
       }
       ringBuffer.skip(authHeader);
       decryptor.incrementIv(false);
       byte[] encryptedPayload = new byte[chunkLen];
       byte[] payload = new byte[chunkLen - TAG_LEN];
       ringBuffer.get(encryptedPayload);
       decryptor.decrypt(encryptedPayload,0,chunkLen,payload,0);
       decryptor.incrementIv(false);
       log.debug("send data to outbound through process(), size: [{}]",payload.length);
       outbound.process(Buffer.buffer(payload),dst,handler,this);
    }

    private void processHeader(int authHeaderLen)
            throws Exception {

        int available = ringBuffer.getAvailableBytes();
        byte[] authHeader = ringBuffer.peek(authHeaderLen);
        byte[] len = new byte[LENGTH_FIELD_LEN];
        decryptor.decrypt(authHeader,0,authHeader.length,len,0);
        int realChunkLen = getUnsignedshortFromBytesArray(len,0);
        if(available < realChunkLen + authHeaderLen){
            log.debug("need: [{}], actual: [{}], waitting for more data",realChunkLen,available);
            return;
        }
        decryptor.incrementIv(false);

        ringBuffer.skip(authHeaderLen);
        byte[] encryptedPayload = new byte[realChunkLen];
        ringBuffer.get(encryptedPayload);

        byte[] payload = new byte[realChunkLen - TAG_LEN];
        decryptor.decrypt(encryptedPayload,0,realChunkLen,payload,0);
        decryptor.incrementIv(false);

        Object[] os = processATYP(payload,0);
        try {
            outbound = Main.instance.dispatcher.dispatchToOutbound(sendTo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SocketAddress dst = (SocketAddress)os[0];
        int finalPosition = (int)os[1];
        Buffer send = Buffer.buffer(payload);
        send = send.slice(finalPosition,send.length());
        log.debug("send data to outbound through process(), size: [{}]",send.length());
        outbound.process(send,dst,handler,this);
        firstReceived = true;
    }


    @Override
    protected void handleOnEnd(Void v) {
        log.debug("socket end");
        close();
        outbound.tell(END_ACTION,v);
    }

    @Override
    protected void handleOnException(Throwable t) {
        log.debug("",t);
        outbound.close();
        close();
    }

    @Override
    protected void handleOnClose(Void v) {
        log.debug("socket closed");
        outbound.tell(CLOSE_ACTION,v);
        close();
    }

}
