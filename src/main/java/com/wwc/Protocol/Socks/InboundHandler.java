package com.wwc.Protocol.Socks;

import com.wwc.Main;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Socket.SocketCallback;
import com.wwc.Utils.ByteCircularBuffer;
import com.wwc.Utils.Handler;
import com.wwc.Utils.SpecConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.wwc.Utils.Common.byteArrayIPToString;
import static com.wwc.Utils.Common.bytePortToUnsignedInt;

public class InboundHandler extends SocketCallback implements Inbound{

    private DnsClient dnsClient ;
    private Stage stage = Stage.STREAM_START;

    private boolean isClosed = false;

    private SpecConfig config;
    private HashMap<String,Object> inboundConfig ;

    /**
     * this used only for socks handshake, when handshake completed, set it to null,
     * and InboundHandler just transfer data to outbound.
     */
    private ByteCircularBuffer ringBuffer = new ByteCircularBuffer(100);

    // only support socks5 and no authorization.
    private static final int SOCKS_VERSION = 0x05;

    private static final int CMD_CONNECT = 0x01;
    private static final int CMD_BIND = 0x02;
    private static final int CMD_UDP_ASSOCIATE = 0x03;

    private static final int ATYP_IPV4 = 0x01;
    private static final int ATYP_DOMAIN_NAME = 0x03;
    private static final int ATYP_IPV6 = 0x04;

    private SocketAddress dst;
    //
    private Outbound outbound;

    public InboundHandler(){
        super("Socks/InboundHandler");
    }

    @Override
    protected void handleOnRead(Buffer data) {
        if(stage == Stage.STREAM_RUNNING){
            processStreamRunning(data);
            return;
        }

        ringBuffer.put(data.getBytes());
        if(stage == Stage.STREAM_ADDR_CONNECTING){

            socket.write(Buffer.buffer(new byte[]
                    {0x05,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x10,0x10}));

            processStreamAddrConnecting();
            connectToOutbound();

            stage = Stage.STREAM_RUNNING;
        }else if(stage == Stage.STREAM_START){
            processStreamStart();
            stage = Stage.STREAM_ADDR_CONNECTING;
        }

        assert stage == Stage.STREAM_RUNNING
                || stage == Stage.STREAM_ADDR_CONNECTING
                || stage == Stage.STREAM_START;
    }

    @Override
    protected void handleOnEnd(Void v) {
        log.debug("local socket has been end");
        destroyHandler(v);
    }

    @Override
    protected void handleOnException(Throwable t) {
        log.debug("exception occurred on Local, [{}]",t);
        destroyHandler();
    }

    @Override
    protected void handleOnClose(Void v) {
        if(stage == Stage.STREAM_DESTROYED){
            log.debug("already been destroyed");
            return;
        }
        destroyHandler();
    }

    private void processStreamStart(){
        int available = ringBuffer.getAvailableBytes();
        if(available < 3){//waitting for more data come.
            return;
        }
        byte[] data = ringBuffer.get(3);//get first socks handshake package,
        int version = data[0];
        if(version != SOCKS_VERSION){
            log.debug("current socks version: [{}],we only support socks 5",version);
            destroyHandler();
            return;
        }
        socket.write(Buffer.buffer(new byte[]{0x05,0x00}));


        handleOnRead(Buffer.buffer(new byte[0]));//recheck whether have data to process.
    }

    private void processStreamRunning(Buffer data){
        //for socks. we don't need to control package length,
        //so in there, we transfer all data we recv from socket.
        log.debug("Send to OutboundHandler through process(), size: [{}]",data.length());
        outbound.process(data,dst,this.handler,this);

    }


    private void processStreamAddrConnecting(){
        int available = ringBuffer.getAvailableBytes();
        if(available < 3 ){//VER + CMD + RSV
            return;
        }
        byte[] halfHeader = ringBuffer.get(3);
        int version = halfHeader[0];
        if(version != 5){
            log.debug("bad socks, only support socks5,current: [{}]",version);
        }
        int cmd = halfHeader[1];
        if(cmd == CMD_CONNECT){
            processCmdConnect();
        }else if(cmd == CMD_BIND){
            processCmdBind();
        }else if(cmd == CMD_UDP_ASSOCIATE){
            processCmdUdpAssociate();
        }else{
            log.debug("bad CMD field, current: [{}]",cmd);
            destroyHandler();
            return;
        }
    }

    /**
     * we assume socks header receive completed.
     */
    private void processCmdConnect(){
        int atyp = ringBuffer.get();
        if(atyp == ATYP_IPV4){
            processATYPIp(4);
        }else if(atyp == ATYP_DOMAIN_NAME){
            processAddr();
        }else if(atyp == ATYP_IPV6){
            processATYPIp(16);
        }else {
            log.debug("bad socks, cannot find ATYP, current: [{}]",atyp);
            destroyHandler();
            return;
        }
    }

    private void processCmdBind(){

    }

    private void processATYPIp(int length){
        //length  + port(2 bytes)
        byte[] ip = new byte[length];
        ringBuffer.get(ip);
        try {
            String host = byteArrayIPToString(ip);
            byte[] portBytes = ringBuffer.get(2);
            int port = bytePortToUnsignedInt(portBytes);
            log.debug("Socks parse completed, dst: [{}:{}]",host,port);
            dst = SocketAddress.inetSocketAddress(port,host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void processCmdUdpAssociate(){

    }

    private void processAddr(){
        int length = ringBuffer.get();
        byte[] host = ringBuffer.get(length);
        String strHost = new String(host, Charset.forName("UTF-8"));
        int port = bytePortToUnsignedInt(ringBuffer.get(2));
        dst = SocketAddress.inetSocketAddress(port,strHost);
    }

    private void connectToOutbound(){
        String sendTo = (String)inboundConfig.get("sendTo");

        try {
            outbound = Main.instance.dispatcher.dispatchToOutbound(sendTo);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    public void destroyHandler(Object o){
        if(stage == Stage.STREAM_DESTROYED){
            log.debug("already destroyed");
            return;
        }
        stage = Stage.STREAM_DESTROYED;

        if(outbound != null)
            outbound.tell(END_ACTION,o);
        close();
    }

    public void destroyHandler(){
        destroyHandler(new Object());
    }

    @Override
    public void process(NetSocket socket) {
        this.socket = socket;
        socket.handler(this::handleOnRead)
                .closeHandler(this::handleOnClose)
                .endHandler(this::handleOnClose)
                .exceptionHandler(this::handleOnException)
                .drainHandler(this::handleOnDrain);
        this.config = Main.instance.configManager.getSpecConfig("socks");
        this.inboundConfig = config.inbound;
    }

    @Override
    public void close() {
        if(isClosed){
            return;
        }
        isClosed = true;
        socket.close();
    }

    private Handler<Buffer> handler = data ->{
        log.debug("Socks#InboundHandler, write to socket, size: [{}]",data.length());
        writeToSocket(data);
    };


    private enum Stage{
        STREAM_START,
        STREAM_ADDR_CONNECTING,
        STREAM_RUNNING,
        STREAM_DESTROYED;
    }
}
