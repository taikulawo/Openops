package com.wwc.Protocol.Openops;

import com.wwc.Crypto.EncryptorFactory;
import com.wwc.Crypto.IEncryptor;
import com.wwc.Main;
import com.wwc.Protocol.IBound;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import static com.wwc.Protocol.Openops.OpenopsConstVar.*;
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

    private String password;
    private String method;

    private HashMap<String,Object> outboundConfig = new HashMap<>();

    private SocketAddress dst;

    private Inbound in;

    private Queue<Buffer> queue = new LinkedList<>();

    private IEncryptor encryptor;
    private IEncryptor decryptor;

    /**
     * we use this handler send data to downstream.
     */
    private Handler<Buffer> handler;

    public OutboundHandler(){
        configuringNetClient();
    }

    private void connectToRemote(){
        this.client.connect(dst,res ->{
            if(res.succeeded()){
                remoteConnected = true;
                socket = res.result();
                registerCallbackToSocket();

                queue.forEach(d ->{
                   socket.write(d);
                });
                queue.clear();

            }else{
                log.debug("Failed to connect: [{}]",res.cause());
            }
        });
    }

    private void registerCallbackToSocket(){

        socket.endHandler(this::handleOnEnd)
                .exceptionHandler(this::handleOnException)
                .handler(this::handleOnRead)
                .drainHandler(this::handleOnDrain);
    }

    @Override
    protected void handleOnRead(Buffer data){
        ringBuffer.put(data.getBytes());

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
        writeToSocket(Buffer.buffer(afterDecrypted));

    }

    protected void writeToSocket(Buffer data){
        if(!remoteConnected){
            queue.offer(data);
            return;
        }
        socket.write(data);
    }

    @Override
    protected void handleOnDrain(Void v){

    }

    @Override
    protected void handleOnEnd(Void v){

        //可以在这里处理事件，最后tell inbound事件的发生。

        //通知Inbound end事件发生
        in.tell(END_ACTION,v);
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
        if(!requestSend){
            requestSend = true;
            this.in = in;
            init(handler);
            connectToRemote();
            Buffer d = processFirstRequest(data);

            writeToSocket(d);
            return;
        }
        processEncryptRunning(data);
    }

    private void processEncryptRunning(Buffer data){
        int dataLen = data.length();

        byte[] decrypted = new byte[LENGTH_FIELD_LEN + TAG_LEN * 2 + dataLen];

        encryptor.encrypt(getBytesArrayOfShort(dataLen+ TAG_LEN ),0,Short.BYTES,decrypted,0,null);
        encryptor.encrypt(data.getBytes(),0,Short.BYTES,decrypted,Short.BYTES + TAG_LEN,null);

        writeToSocket(Buffer.buffer(decrypted));
    }

    private void init(Handler<Buffer> handler){
        this.handler = handler;
        this.outboundConfig = configManager.getSpecOutboundFromTag("openops");
        this.password = (String)outboundConfig.get("password");
        this.method = (String)outboundConfig.get("method");

        try {
            encryptor = EncryptorFactory.getEncryptor(this.method,this.password);
            decryptor = EncryptorFactory.getEncryptor(password,method);
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

    private Buffer processFirstRequest(Buffer data){
        int len = data.length();

        byte[] header = processRequestHeader();

        byte[] encrypted = new byte[LENGTH_FIELD_LEN + TAG_LEN * 2 + len + header.length];

        encryptor.encrypt(getBytesArrayOfShort(len + TAG_LEN + header.length),0,Short.BYTES,encrypted,0);

        encryptor.encryptUpdate(header,0,header.length);
        encryptor.encrypt(data.getBytes(), 0 , len,encrypted,Short.BYTES + TAG_LEN, (Object[]) null);

        return Buffer.buffer(encrypted);
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

    }

    @Override
    public void tell(int action, Object o) {
        switch(action){
            case IBound.END_ACTION: {

            }
            case IBound.CLOSE_ACTION:{

            }
            case IBound.EXCEPTION_ACTION:{
                log.debug("[{}]",(Throwable)o);
            }
            close();
        }
    }

    private void destroyHandler(){
        if(isDestroyed){
            log.debug("already destroyed");
            return;
        }
        in.close();
        close();
    }


    //iv + tag + tcp payload
    private Handler<byte[]> syncRingBufferHandler = data ->{

        byte[] after = new byte[data.length - TAG_LEN];
        decryptor.decrypt(data,0,data.length,after,0, (Object) null);
        handler.handle(Buffer.buffer(after));
    };

}

