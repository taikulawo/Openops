package com.wwc.Protocol;

import com.wwc.Utils.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;

/**
 * Outbound只有这一个接口，如何被调用应该由Outbound的编写者来规定参数。
 */

//Inbound通过process来传递数据，但是如何传递？
public interface Outbound extends IBound{

    default Outbound getInstance(){
        try {
            return this.getClass().newInstance();
        } catch (IllegalAccessException
                | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 这里的Inbound和Outbound并不一定真的是Inbound,Outbound，又可能是另一个实现了Processable接口的类比如
     * InboundHandler，
     * Inbound对于每一个连接应该只使用process一次，后面的数据传递应该通过Processable p来进行
     * 调用process来向Outbound传递消息，这时Outbound应该保存这三个参数
     * @param data
     * @param addr
     * @param handler  */
    void process(Buffer data, SocketAddress addr, Handler<Buffer> handler, Inbound in);
}
