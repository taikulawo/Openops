package com.wwc.Protocol.Openops;

import com.wwc.Crypto.EncryptorFactory;
import com.wwc.Crypto.IEncryptor;
import com.wwc.Main;
import com.wwc.Protocol.Inbound;
import com.wwc.Protocol.Outbound;
import com.wwc.Socket.SocketCallback;
import com.wwc.Utils.ByteCircularBuffer;
import com.wwc.Utils.ConfigManager;
import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import static com.wwc.Protocol.Openops.OpenopsConstVar.*;
import static com.wwc.Protocol.Openops.ProtocolImpl.firstEncrypt;
import static com.wwc.Protocol.Openops.ProtocolImpl.runningEncrypt;
import static com.wwc.Utils.Common.*;


//
// +------+-------------+---------+--------+
// | len  | header tag  | payload | tag    |
// +------+-------------+---------+--------+
// |<--------------AAE-------------------->|
//

//len 只包含 tcp payload 与 tag(16bytes)的长度
public final class OutboundHandler extends SocketCallback implements Outbound {
    private Logger log = LoggerFactory.getLogger(OutboundHandler.class);

    //这两个是给Outbound用的，所以没有必要放到OpenopsConstVar里面
    private static final int RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_INTERNAL = 500;
    private static final int MAX_TIMEOUT = 300;//seconds, 5 mins

    private NetClient client ;

    private ByteCircularBuffer ringBuffer = new ByteCircularBuffer();

    private boolean requestSend = false;
    private boolean remoteConnected = false;
    private boolean isDestroyed = false;

    private ConfigManager configManager = Main.instance.configManager;
    private HashMap<String,Object> config;

    private String password;
    private String method;

    private HashMap<String,Object> outboundConfig = new HashMap<>();

    private SocketAddress dst;
    private SocketAddress proxyServerDst;

    private Inbound in;

    private IEncryptor encryptor;
    private IEncryptor decryptor;

    /**
     * we use this handler send data to downstream.
     */
    private Handler<Buffer> handler;

    public OutboundHandler(){
        super("Openops/OutboundHandler");
        configuringNetClient();

    }

    private void connectToRemote(){
        this.client = Main.getVertx().createNetClient(getNetClientOptions());
        this.client.connect(proxyServerDst,res ->{
            if(res.succeeded()){
                remoteConnected = true;
                socket = res.result();
                registerCallbackToSocket();
                log.info("Connected to [{}:{}], Accept in [{}:{}], remote Address: [{}:{}]",
                        proxyServerDst.host(),proxyServerDst.port(),
                        socket.localAddress().host(),socket.localAddress().port()
                        ,socket.remoteAddress().host(),socket.remoteAddress().port());
                writeToSocket(null);
            }else{
                log.debug("Failed to connect: [{}]",res.cause());
            }
        });
    }

    private NetClientOptions getNetClientOptions(){
        return new NetClientOptions()
                .setTcpKeepAlive(true)
                .setTcpNoDelay(true)
                .setReconnectAttempts(3)
                .setReconnectInterval(1000);
    }

    private void registerCallbackToSocket(){

        socket.endHandler(this::handleOnEnd)
                .exceptionHandler(this::handleOnException)
                .handler(this::handleOnRead)
                .drainHandler(this::handleOnDrain);
    }

    @Override
    protected void handleOnRead(Buffer data){
        try{
            ringBuffer.put(data.getBytes());
            log.debug("recv from remote, size: [{}],ByteCircularBuffer available: [{}]",data.length(),ringBuffer.getAvailableBytes());
            if(ringBuffer.getAvailableBytes() < AUTH_HEADER){
                log.debug("no available Bytes, return , available: [{}], need: [{}]",ringBuffer.getAvailableBytes(), AUTH_HEADER);
                return;
            }
            byte[] authHeader = new byte[AUTH_HEADER];
            ringBuffer.peek(authHeader);
            byte[] lenBytes = new byte[LENGTH_FIELD_LEN];

            decryptor.decrypt(authHeader,0,AUTH_HEADER,lenBytes,0, (Object) null);

            int len = getUnsignedshortFromBytesArray(lenBytes,0);

            int available = ringBuffer.getAvailableBytes();
            if(available < len + AUTH_HEADER){
                log.debug("waitting for more data, current: [{}], need: [{}]",available,len + AUTH_HEADER);
                return;
            }
            ringBuffer.skip(AUTH_HEADER);
            decryptor.incrementIv(false);
            byte[] beforeDecrypted = new byte[len];
            byte[] afterDecrypted = new byte[len - TAG_LEN];
            ringBuffer.get(beforeDecrypted);

            decryptor.decrypt(beforeDecrypted,0,len,afterDecrypted,0);
            decryptor.incrementIv(false);
            handler.handle(Buffer.buffer(afterDecrypted));
            log.debug("ByteCircularBuffer available: [{}], before decrypt size: [{}], after: [{}]",ringBuffer.getAvailableBytes(),len,len-TAG_LEN);
        }catch(Exception e ){
            log.error("",e);
            close();
        }


    }


    @Override
    protected void handleOnEnd(Void v){

        //可以在这里处理事件，最后tell inbound事件的发生。

        //通知Inbound end事件发生
        in.tell(END_ACTION,v);
        close();
    }

    @Override
    protected void handleOnException(Throwable t){
        log.debug("onException: [{}]",t);
        in.tell(EXCEPTION_ACTION,t);
        close();
    }

    @Override
    protected void handleOnClose(Void v) {
        in.tell(CLOSE_ACTION,v);
        close();
    }


    private void configuringNetClient(){
        NetClientOptions option = new NetClientOptions()
                            .setReconnectAttempts(RECONNECT_ATTEMPTS)
                .setReconnectInterval(RECONNECT_INTERNAL)
                .setLogActivity(true)
                .setTcpKeepAlive(true)
                .setTcpNoDelay(true)
                .setIdleTimeout(MAX_TIMEOUT);

        client  = Main.getVertx().createNetClient(option);
    }

    @Override
    public void process(Buffer data, SocketAddress addr, Handler<Buffer> handler, Inbound in) {
        try{
            if(!requestSend){
                requestSend = true;
                this.in = in;
                this.dst = addr;
                this.config = configManager.getSpecOutboundFromTag("openops");
                String host = (String) config.get("server");
                int port = (int) config.get("port");

                this.proxyServerDst = SocketAddress.inetSocketAddress(port,host);
                init(handler);
                connectToRemote();
                ArrayList<Buffer> list = processFirstRequest(data);


                queue.addAll(list);
                writeToSocket(null);
                return;
            }
            processEncryptRunning(data);
        }catch(Exception e ){
            log.error("",e);
            close();
        }
    }

    private void processEncryptRunning(Buffer data)
            throws Exception {

        ArrayList<Buffer> list = runningEncrypt(data,encryptor);

        queue.addAll(list);
        writeToSocket(null);

    }

    private void init(Handler<Buffer> handler){
        this.handler = handler;
        this.outboundConfig = configManager.getSpecOutboundFromTag("openops");
        this.password = (String)outboundConfig.get("password");
        this.method = (String)outboundConfig.get("method");

        try {
            encryptor = EncryptorFactory.getEncryptor(this.method,this.password);
            decryptor = EncryptorFactory.getEncryptor(this.method,this.password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //len -> 2bytes
    //tag -> 16bytes
    // +--------+-------+
    // | len    | tag   |
    // +--------+-------+
    // len只包含tcp payload本身长度，排除认证头长度。
    //we don't need to use SecureRandom, it spend a lot time, not necessary. we just use Random.
    //iv only need random not unpredictable


    // 对于第一次的请求包中tcp payload,仿照socks握手handshake格式
    //+-------+----------+-------+-----------------+
    //| type  | variable |  port | tcp payload tag |
    //+-------+----------+-------+-----------------+
    //|<-------tcp payload------>|<-----tag------->|

    private ArrayList<Buffer> processFirstRequest(Buffer data)
            throws Exception {

        byte[] header = processRequestHeader();
        return firstEncrypt(data,header,encryptor);
    }

    private byte[] processRequestHeader(){
        String host = dst.host();
        byte[] hostBytes = host.getBytes(Charset.forName("UTF-8"));
        byte[] port = getBytesArrayOfShort(dst.port());

        byte[] header;
        ByteBuffer d;

        if(isIp(host)){
            if(isIpv4(host)){
                header = new byte[1 + 4 + 2];

                d = ByteBuffer.wrap(header);
                d.put((byte)ATYP_IPV4);
            }else{ //ipv6
                header = new byte[1 + 16 + 2];

                d = ByteBuffer.wrap(header);
                d.put((byte)ATYP_IPV6);
            }
        }else{
            int len = host.length();
            header = new byte[1 + 1 + len + 2];

            d = ByteBuffer.wrap(header);
            d.put((byte)ATYP_DOMAIN)
                    .put((byte)len);
        }

        d.put(hostBytes).put(port);
        return header;
    }

    @Override
    public void close() {
        if(socket != null)
            socket.close();
    }



}

